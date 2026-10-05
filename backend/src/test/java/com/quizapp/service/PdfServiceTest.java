package com.quizapp.service;

import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfObject;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import com.quizapp.dto.QuestionDto;
import com.quizapp.dto.QuizDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

// Regression: a photo used to be added to the page separately from its question, so
// when it didn't fit in the space left on a page the picture alone jumped to the next
// page (orphaned from its question) while the answer line stayed behind. Each
// question's heading, photo and answer now travel together as one block.
class PdfServiceTest {

    private final PdfService pdfService = new PdfService();

    @TempDir
    Path tempDir;

    // A different picture per question, so PDF image de-duplication can't merge them
    // and distort the per-page image count the test relies on.
    private String photoUrl(int n) throws Exception {
        BufferedImage img = new BufferedImage(400, 400, BufferedImage.TYPE_INT_RGB);
        for (int x = 0; x < 400; x++) for (int y = 0; y < 400; y++) img.setRGB(x, y, (n * 977 + x * 31 + y * 17) & 0xFFFFFF);
        File file = tempDir.resolve("logo-" + n + ".png").toFile();
        ImageIO.write(img, "png", file);
        return file.toURI().toURL().toString();
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

    private QuestionDto question(int n, boolean withPhoto, String url) {
        QuestionDto q = new QuestionDto();
        q.setQuestionText((withPhoto ? "PHOTOQ" : "PLAINQ") + n + " which one is this?");
        q.setCategory(withPhoto ? "Logo" : "General");
        q.setDifficultyLevel(5);
        q.setAnswer("Answer " + n);
        if (withPhoto) q.setPhotoUrl(url);
        return q;
    }

    @Test
    void everyPhotoStaysOnTheSamePageAsItsQuestionAndAnswer() throws Exception {
        QuizDto quiz = new QuizDto();
        quiz.setTitle("Photo layout test");
        List<QuestionDto> questions = new ArrayList<>();
        // A mix that pushes photo questions across several page boundaries.
        for (int n = 1; n <= 16; n++) {
            boolean photo = n >= 5 && n % 2 == 1 || n == 6 || n == 7;
            questions.add(question(n, photo, photo ? photoUrl(n) : null));
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

        Files.write(tempDir.resolve("layout.pdf"), pdf);
    }

    @Test
    void aQuestionWithADeadPhotoLinkStillPrintsWithoutThePicture() {
        QuizDto quiz = new QuizDto();
        quiz.setTitle("Dead link test");
        QuestionDto q = question(1, true, "http://localhost:1/does-not-exist.png");
        quiz.setQuestions(List.of(q));

        byte[] pdf = pdfService.renderQuiz(quiz, false);

        assertThat(pdf).isNotEmpty();
    }
}
