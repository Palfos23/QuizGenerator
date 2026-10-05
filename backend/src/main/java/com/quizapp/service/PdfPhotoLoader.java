package com.quizapp.service;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.attributes.ViewBox;
import com.github.weisj.jsvg.parser.SVGLoader;
import com.lowagie.text.Image;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Locale;

/**
 * Turns a question's photo URL into something OpenPDF can embed.
 *
 * Photo URLs are whatever an admin pasted - any host, any format - so this has to cope
 * with the ways a plain "open the URL and read an image" fails in practice:
 *  - Hosts that refuse a non-browser client: Wikimedia (where most team/company logos
 *    come from) answers Java's default User-Agent with 403 Forbidden, so requests go out
 *    with a browser-style one.
 *  - Redirects (including http -> https), which are followed.
 *  - Formats OpenPDF can't embed: SVG is rasterized, WebP is read through ImageIO, and
 *    PNG/JPEG/GIF go straight in.
 * Only http(s) is accepted - anything else (e.g. file:) is rejected rather than fetched.
 */
@Component
public class PdfPhotoLoader {

    private static final long MAX_BYTES = 10L * 1024 * 1024;
    // Long edge of a rasterized SVG - well above the ~220pt it's printed at, so it stays sharp.
    private static final int SVG_RENDER_PX = 800;
    private static final String USER_AGENT =
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) "
                    + "Chrome/124.0 Safari/537.36 QuizzesPdf/1.0 (+https://quizzes.no)";

    private final HttpClient httpClient = HttpClient.newBuilder()
            .followRedirects(HttpClient.Redirect.ALWAYS)
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public Image load(String photoUrl) throws Exception {
        return toPdfImage(download(photoUrl));
    }

    private byte[] download(String photoUrl) throws IOException, InterruptedException {
        URI uri = URI.create(photoUrl.trim());
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase(Locale.ROOT);
        if (!scheme.equals("http") && !scheme.equals("https")) {
            throw new IOException("only http(s) photo links are supported");
        }
        HttpRequest request = HttpRequest.newBuilder(uri)
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", USER_AGENT)
                .header("Accept", "image/*,*/*;q=0.8")
                .GET()
                .build();
        HttpResponse<InputStream> response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        try (InputStream body = response.body()) {
            if (response.statusCode() != 200) {
                throw new IOException("HTTP " + response.statusCode());
            }
            byte[] bytes = body.readNBytes((int) MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) {
                throw new IOException("image is larger than " + (MAX_BYTES / 1024 / 1024) + " MB");
            }
            return bytes;
        }
    }

    private Image toPdfImage(byte[] bytes) throws Exception {
        if (looksLikeSvg(bytes)) {
            return Image.getInstance(rasterizeSvg(bytes));
        }
        try {
            return Image.getInstance(bytes); // PNG, JPEG, GIF, BMP, TIFF
        } catch (Exception notNativelySupported) {
            BufferedImage decoded = ImageIO.read(new ByteArrayInputStream(bytes)); // e.g. WebP, via the plugin
            if (decoded == null) {
                throw new IOException("unsupported image format");
            }
            return Image.getInstance(toPng(decoded));
        }
    }

    private static boolean looksLikeSvg(byte[] bytes) {
        String head = new String(bytes, 0, Math.min(bytes.length, 2048), StandardCharsets.UTF_8).toLowerCase(Locale.ROOT);
        return head.contains("<svg");
    }

    private static byte[] rasterizeSvg(byte[] bytes) throws IOException {
        SVGDocument document = new SVGLoader().load(new ByteArrayInputStream(bytes));
        if (document == null) {
            throw new IOException("could not parse SVG");
        }
        float srcW = Math.max(document.size().width, 1f);
        float srcH = Math.max(document.size().height, 1f);
        float scale = SVG_RENDER_PX / Math.max(srcW, srcH);
        int w = Math.max(1, Math.round(srcW * scale));
        int h = Math.max(1, Math.round(srcH * scale));

        BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = image.createGraphics();
        try {
            // White backing: the page is white anyway, and a logo drawn in white-on-transparent
            // would otherwise vanish into a black raster background.
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, w, h);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            document.render(null, g, new ViewBox(0, 0, w, h));
        } finally {
            g.dispose();
        }
        return toPng(image);
    }

    private static byte[] toPng(BufferedImage image) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
