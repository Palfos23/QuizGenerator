package com.quizapp.service;

import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.quizapp.dto.QuestionDto;
import com.quizapp.dto.QuizDto;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// Two regressions covered here:
// 1. Layout: a photo used to be added to the page separately from its question, so when
//    it didn't fit in the space left on a page the picture alone jumped to the next page
//    (orphaned from its question) while the answer line stayed behind. Each question's
//    heading, photo and answer now travel together as one block.
// 2. Fetching: some photos silently went missing from the PDF even though they showed
//    fine in the app - Wikimedia (most team/company logos) answers Java's default
//    User-Agent with 403, and its logos are often SVG, which OpenPDF can't embed. The
//    test server below rejects a plain Java client the same way Wikimedia does.
class PdfServiceTest {

    // 1x1 lossless WebP
    private static final String TINY_WEBP_BASE64 = "UklGRhoAAABXRUJQVlA4TA0AAAAvAAAAEAcQERGIiP4HAA==";
    private static final String SVG = "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"300\" height=\"200\" viewBox=\"0 0 300 200\">"
            + "<rect width=\"300\" height=\"200\" fill=\"#fff\"/><circle cx=\"150\" cy=\"100\" r=\"70\" fill=\"#c00\"/></svg>";

    private final PdfPhotoLoader photoLoader = new PdfPhotoLoader();
    private final PdfService pdfService = new PdfService(photoLoader);

    private HttpServer server;
    private final AtomicReference<String> lastUserAgent = new AtomicReference<>();

    private String url(String path) {
        return "http://localhost:" + server.getAddress().getPort() + path;
    }

    private static byte[] pngFor(int n) throws IOException {
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 400; x++) for (int y = 0; y < 400; y++) img.setRGB(x, y, (n * 977 + x * 31 + y * 17) & 0xFFFFFF);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(img, "png", out);
        return out.toByteArray();
    }

    private void respond(HttpExchange ex, int status, String contentType, byte[] body) throws IOException {
        ex.getResponseHeaders().add("Content-Type", contentType);
        ex.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
        if (body.length > 0) ex.getResponseBody().write(body);
        ex.close();
    }

    @BeforeEach
    void startServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", ex -> {
            String ua = ex.getRequestHeaders().getFirst("User-Agent");
            lastUserAgent.set(ua);
            String path = ex.getRequestURI().getPath();
            // Like Wikimedia: refuse anything that identifies as a bare Java client.
            if (ua == null || ua.startsWith("Java")) {
                respond(ex, 403, "text/plain", "Forbidden".getBytes(StandardCharsets.UTF_8));
            } else if (path.startsWith("/img/")) {
                int n = Integer.parseInt(path.substring("/img/".length(), path.length() - ".png".length()));
                respond(ex, 200, "image/png", pngFor(n));
            } else if (path.equals("/redirect")) {
                ex.getResponseHeaders().add("Location", "/img/1.png");
                respond(ex, 302, "text/plain", new byte[0]);
            } else if (path.equals("/logo.svg")) {
                respond(ex, 200, "image/svg+xml", SVG.getBytes(StandardCharsets.UTF_8));
            } else if (path.equals("/logo.webp")) {
                respond(ex, 200, "image/webp", Base64.getDecoder().decode(TINY_WEBP_BASE64));
            } else if (path.equals("/page")) {
                respond(ex, 200, "text/html", "<html>not an image</html>".getBytes(StandardCharsets.UTF_8));
            } else {
                respond(ex, 404, "text/plain", "nope".getBytes(StandardCharsets.UTF_8));
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        server.stop(0);
    }

    // ---- fetching ----

    @Test
    void identifiesAsABrowserSoHostsThatBlockJavaClientsServeTheImage() throws Exception {
        assertThat(photoLoader.load(url("/img/1.png")).getWidth()).isPositive();
        assertThat(lastUserAgent.get()).doesNotStartWith("Java").contains("Mozilla");
    }

    @Test
    void followsRedirects() throws Exception {
        assertThat(photoLoader.load(url("/redirect")).getWidth()).isPositive();
    }

    @Test
    void rasterizesSvgLogos() throws Exception {
        com.lowagie.text.Image image = photoLoader.load(url("/logo.svg"));
        assertThat(image.getWidth()).isPositive();
        assertThat(image.getWidth() / image.getHeight()).as("keeps the SVG's 3:2 shape").isBetween(1.4f, 1.6f);
    }

    @Test
    void readsWebp() throws Exception {
        assertThat(photoLoader.load(url("/logo.webp")).getWidth()).isPositive();
    }

    @Test
    void rejectsDeadLinksNonImagesAndNonHttpSchemes() {
        assertThatThrownBy(() -> photoLoader.load(url("/missing.png"))).hasMessageContaining("404");
        assertThatThrownBy(() -> photoLoader.load(url("/page"))).isInstanceOf(Exception.class);
        assertThatThrownBy(() -> photoLoader.load("file:///etc/hosts")).hasMessageContaining("http");
    }

    // ---- layout ----

    private QuestionDto question(int n, String photoUrl) {
        QuestionDto q = new QuestionDto();
        q.setQuestionText((photoUrl != null ? "PHOTOQ" : "PLAINQ") + n + " which one is this?");
        q.setCategory(photoUrl != null ? "Logo" : "General");
        q.setDifficultyLevel(5);
        q.setAnswer("Answer " + n);
        q.setPhotoUrl(photoUrl);
        return q;
    }

    private int imagesOnPage(PdfReader reader, int page) {
        PdfDictionary resources = reader.getPageN(page).getAsDict(PdfName.RESOURCES);
        PdfDictionary xobjects = resources == null ? null : resources.getAsDict(PdfName.XOBJECT);
        if (xobjects == null) return 0;
        int count = 0;
        for (PdfName name : xobjects.getKeys()) {
            PdfObject obj = PdfReader.getPdfObject(xobjects.get(name));
            if (obj instanceof com.lowagie.text.pdf.PRStream stream
                    && PdfName.IMAGE.equals(stream.getAsName(PdfName.SUBTYPE))) {
                count++;
            }
        }
        return count;
    }

    @Test
    void everyPhotoStaysOnTheSamePageAsItsQuestionAndAnswer() throws Exception {
        QuizDto quiz = new QuizDto();
        quiz.setTitle("Photo layout test");
        List<QuestionDto> questions = new ArrayList<>();
        // A mix that pushes photo questions across several page boundaries.
        for (int n = 1; n <= 16; n++) {
            boolean photo = n >= 5 && n % 2 == 1 || n == 6 || n == 7;
            questions.add(question(n, photo ? url("/img/" + n + ".png") : null));
        }
        quiz.setQuestions(questions);
        int photoQuestions = (int) questions.stream().filter(q -> q.getPhotoUrl() != null).count();

        byte[] pdf = pdfService.renderQuiz(quiz, true);

        PdfReader reader = new PdfReader(pdf);
        PdfTextExtractor extractor = new PdfTextExtractor(reader);
        assertThat(reader.getNumberOfPages()).as("needs to span several pages to be a real test").isGreaterThan(1);

        int totalImages = 0;
        for (int page = 1; page <= reader.getNumberOfPages(); page++) {
            String text = extractor.getTextFromPage(page);
            int images = imagesOnPage(reader, page);

            long photoHeadingsOnPage = text.lines().filter(l -> l.contains("PHOTOQ")).count();
            long photoAnswersOnPage = questions.stream()
                    .filter(q -> q.getPhotoUrl() != null)
                    .filter(q -> text.contains(q.getAnswer()))
                    .count();
            assertThat((long) images)
                    .as("page %d: every photo must sit on the same page as its own question heading", page)
                    .isEqualTo(photoHeadingsOnPage);
            assertThat(photoAnswersOnPage)
                    .as("page %d: every photo question's answer must be on the same page too", page)
                    .isEqualTo(photoHeadingsOnPage);
            totalImages += images;
        }
        assertThat(totalImages).isEqualTo(photoQuestions);
    }

    @Test
    void svgAndWebpPhotosEndUpEmbeddedInThePdf() throws Exception {
        QuizDto quiz = new QuizDto();
        quiz.setTitle("Formats");
        quiz.setQuestions(List.of(question(1, url("/logo.svg")), question(2, url("/logo.webp")), question(3, url("/redirect"))));

        PdfReader reader = new PdfReader(pdfService.renderQuiz(quiz, true));

        int images = 0;
        for (int page = 1; page <= reader.getNumberOfPages(); page++) images += imagesOnPage(reader, page);
        assertThat(images).isEqualTo(3);
    }

    @Test
    void aQuestionWithADeadPhotoLinkStillPrintsAndSaysThePictureIsMissing() throws Exception {
        QuizDto quiz = new QuizDto();
        quiz.setTitle("Dead link test");
        quiz.setQuestions(List.of(question(1, url("/missing.png"))));

        byte[] pdf = pdfService.renderQuiz(quiz, false);

        PdfReader reader = new PdfReader(pdf);
        String text = new PdfTextExtractor(reader).getTextFromPage(1);
        assertThat(text).contains("PHOTOQ1").contains("Picture could not be loaded");
    }
}
