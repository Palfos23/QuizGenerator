package com.quizapp.service;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ImageStorageServiceTest {

    private HttpServer server;
    private String baseUrl;
    private final AtomicReference<String> path = new AtomicReference<>();
    private final AtomicReference<String> auth = new AtomicReference<>();
    private final AtomicReference<String> contentType = new AtomicReference<>();
    private final AtomicReference<byte[]> body = new AtomicReference<>();
    private volatile int status = 200;

    @BeforeEach
    void startFakeStorage() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            path.set(exchange.getRequestURI().getPath());
            auth.set(exchange.getRequestHeaders().getFirst("Authorization"));
            contentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            body.set(exchange.getRequestBody().readAllBytes());
            byte[] reply = "{}".getBytes();
            exchange.sendResponseHeaders(status, reply.length);
            exchange.getResponseBody().write(reply);
            exchange.close();
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    private static byte[] image(int w, int h, int type, String format) throws Exception {
        BufferedImage img = new BufferedImage(w, h, type);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ImageIO.write(img, format, bos);
        return bos.toByteArray();
    }

    @Test
    void uploadsToTheBucketWithTheServiceKeyAndReturnsThePublicUrl() throws Exception {
        ImageStorageService service = new ImageStorageService(baseUrl + "/", "secret-key", "photos");

        String url = service.upload(image(300, 200, BufferedImage.TYPE_INT_RGB, "png"));

        assertThat(path.get()).startsWith("/storage/v1/object/photos/").endsWith(".jpg");
        assertThat(auth.get()).isEqualTo("Bearer secret-key");
        assertThat(contentType.get()).isEqualTo("image/jpeg");
        assertThat(url).startsWith(baseUrl + "/storage/v1/object/public/photos/").endsWith(".jpg");
        assertThat(ImageIO.read(new ByteArrayInputStream(body.get()))).isNotNull();
    }

    @Test
    void largePicturesAreScaledDownAndTransparentOnesStayPng() throws Exception {
        ImageStorageService.Prepared big = ImageStorageService.prepare(image(4000, 2000, BufferedImage.TYPE_INT_RGB, "png"));
        BufferedImage result = ImageIO.read(new ByteArrayInputStream(big.bytes()));
        assertThat(result.getWidth()).isEqualTo(ImageStorageService.MAX_DIMENSION);
        assertThat(result.getHeight()).isEqualTo(800); // aspect ratio kept

        ImageStorageService.Prepared logo = ImageStorageService.prepare(image(100, 100, BufferedImage.TYPE_INT_ARGB, "png"));
        assertThat(logo.contentType()).isEqualTo("image/png");
        assertThat(logo.extension()).isEqualTo("png");
    }

    @Test
    void rejectsFilesThatAreNotPicturesOrAreEmptyOrTooBig() {
        assertThatThrownBy(() -> ImageStorageService.prepare("<html>not an image</html>".getBytes()))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("picture");
        assertThatThrownBy(() -> ImageStorageService.prepare(new byte[0]))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ImageStorageService.prepare(new byte[(int) ImageStorageService.MAX_UPLOAD_BYTES + 1]))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("too large");
    }

    @Test
    void withoutSettingsUploadsAreUnavailableAndAStoreErrorIsReportedClearly() throws Exception {
        ImageStorageService unconfigured = new ImageStorageService("", "", "photos");
        assertThat(unconfigured.isConfigured()).isFalse();
        assertThatThrownBy(() -> unconfigured.upload(image(10, 10, BufferedImage.TYPE_INT_RGB, "png")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("aren't set up");

        status = 403;
        ImageStorageService rejected = new ImageStorageService(baseUrl, "wrong-key", "photos");
        assertThatThrownBy(() -> rejected.upload(image(10, 10, BufferedImage.TYPE_INT_RGB, "png")))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("403");
    }
}
