package com.quizapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.UUID;

/**
 * Lets an admin upload their own picture for a question / subject / club instead of hunting for a hosted
 * link. The file is checked, shrunk and re-encoded here (which also drops any camera metadata such as GPS
 * location), then stored in a public Supabase Storage bucket; what comes back is the ordinary public URL,
 * saved in the same photoUrl field a pasted link would go in - so everything that already shows photos
 * (games, PDF export) works unchanged.
 *
 * Needs three settings (see application-prod.properties); without them uploads are simply unavailable.
 */
@Service
public class ImageStorageService {

    private static final Logger log = LoggerFactory.getLogger(ImageStorageService.class);

    /** Larger than this on either side is scaled down - plenty for a question picture, far lighter to load. */
    static final int MAX_DIMENSION = 1600;
    static final long MAX_UPLOAD_BYTES = 8L * 1024 * 1024;

    private final String supabaseUrl;
    private final String serviceKey;
    private final String bucket;
    private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();

    public ImageStorageService(@Value("${app.storage.supabase-url:}") String supabaseUrl,
                               @Value("${app.storage.service-key:}") String serviceKey,
                               @Value("${app.storage.bucket:photos}") String bucket) {
        this.supabaseUrl = supabaseUrl == null ? "" : supabaseUrl.trim().replaceAll("/+$", "");
        this.serviceKey = serviceKey == null ? "" : serviceKey.trim();
        this.bucket = bucket;
    }

    public boolean isConfigured() {
        return !supabaseUrl.isEmpty() && !serviceKey.isEmpty();
    }

    /** Result of preparing an upload: the bytes to store and their content type / file extension. */
    record Prepared(byte[] bytes, String contentType, String extension) {}

    /** Validates, shrinks and re-encodes; throws IllegalArgumentException (shown to the admin) for anything unusable. */
    static Prepared prepare(byte[] original) {
        if (original == null || original.length == 0) throw new IllegalArgumentException("That file is empty.");
        if (original.length > MAX_UPLOAD_BYTES) throw new IllegalArgumentException("That picture is too large - the limit is 8 MB.");
        BufferedImage image;
        try {
            image = ImageIO.read(new ByteArrayInputStream(original)); // decides by content, not by file name
        } catch (IOException e) {
            image = null;
        }
        if (image == null) throw new IllegalArgumentException("That doesn't look like a picture - use a JPG, PNG, WebP or GIF.");

        int w = image.getWidth();
        int h = image.getHeight();
        double scale = Math.min(1.0, (double) MAX_DIMENSION / Math.max(w, h));
        boolean hasAlpha = image.getColorModel().hasAlpha();
        int nw = Math.max(1, (int) Math.round(w * scale));
        int nh = Math.max(1, (int) Math.round(h * scale));
        BufferedImage out = new BufferedImage(nw, nh, hasAlpha ? BufferedImage.TYPE_INT_ARGB : BufferedImage.TYPE_INT_RGB);
        Graphics2D g = out.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            if (!hasAlpha) {
                g.setColor(java.awt.Color.WHITE);
                g.fillRect(0, 0, nw, nh);
            }
            g.drawImage(image, 0, 0, nw, nh, null);
        } finally {
            g.dispose();
        }
        try {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            String format = hasAlpha ? "png" : "jpg"; // transparent logos stay transparent; photos become JPEG
            if (!ImageIO.write(out, format, bos)) throw new IOException("no writer for " + format);
            return new Prepared(bos.toByteArray(), hasAlpha ? "image/png" : "image/jpeg", format);
        } catch (IOException e) {
            throw new IllegalArgumentException("Couldn't process that picture - try a different file.");
        }
    }

    /** Stores the picture and returns its public URL. */
    public String upload(byte[] original) {
        if (!isConfigured()) {
            throw new IllegalStateException("Picture uploads aren't set up yet - add the storage settings (see the README) or paste a link instead.");
        }
        Prepared p = prepare(original);
        String path = UUID.randomUUID() + "." + p.extension();
        HttpRequest request = HttpRequest.newBuilder(URI.create(supabaseUrl + "/storage/v1/object/" + bucket + "/" + path))
                .timeout(Duration.ofSeconds(30))
                .header("Authorization", "Bearer " + serviceKey)
                .header("apikey", serviceKey)
                .header("Content-Type", p.contentType())
                .header("Cache-Control", "max-age=31536000")
                .POST(HttpRequest.BodyPublishers.ofByteArray(p.bytes()))
                .build();
        try {
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() / 100 != 2) {
                log.warn("Supabase storage upload failed: HTTP {} {}", response.statusCode(), response.body());
                throw new IllegalStateException("The picture store rejected the upload (HTTP " + response.statusCode() + "). Check the storage settings and that the bucket exists.");
            }
        } catch (IOException e) {
            log.warn("Supabase storage upload failed", e);
            throw new IllegalStateException("Couldn't reach the picture store - try again in a moment.");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("The upload was interrupted.");
        }
        return supabaseUrl + "/storage/v1/object/public/" + bucket + "/" + path;
    }
}
