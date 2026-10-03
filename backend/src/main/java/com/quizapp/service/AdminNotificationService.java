package com.quizapp.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * Pushes a short message to the admin's phone via ntfy (https://ntfy.sh) - a
 * plain HTTP POST to a topic URL, no account or API key. Does nothing at all
 * until ADMIN_NTFY_URL is set (e.g. https://ntfy.sh/some-long-random-topic),
 * and never throws: a failed notification must not break whatever user action
 * triggered it. Sent after the surrounding transaction commits, so a rolled-
 * back action never produces a notification.
 */
@Service
public class AdminNotificationService {

    private static final Logger log = LoggerFactory.getLogger(AdminNotificationService.class);

    private final String ntfyUrl;
    private final String clickBaseUrl;
    private final HttpClient httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    public AdminNotificationService(@Value("${app.admin-notify.ntfy-url:}") String ntfyUrl,
                                     @Value("${app.frontend-base-url:}") String clickBaseUrl) {
        this.ntfyUrl = ntfyUrl == null ? "" : ntfyUrl.trim();
        this.clickBaseUrl = clickBaseUrl == null ? "" : clickBaseUrl.trim();
    }

    /**
     * @param title    ASCII only - it travels as an HTTP header
     * @param message  free text (UTF-8)
     * @param pagePath admin page to open when the notification is tapped, e.g. "/admin/daily-quiz-review"
     */
    public void notifyAdmin(String title, String message, String pagePath) {
        if (ntfyUrl.isEmpty()) return;
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    sendAsync(title, message, pagePath);
                }
            });
        } else {
            sendAsync(title, message, pagePath);
        }
    }

    private void sendAsync(String title, String message, String pagePath) {
        try {
            HttpRequest.Builder request = HttpRequest.newBuilder(URI.create(ntfyUrl))
                    .timeout(Duration.ofSeconds(10))
                    .header("Title", title)
                    .POST(HttpRequest.BodyPublishers.ofString(message, StandardCharsets.UTF_8));
            if (!clickBaseUrl.isEmpty() && pagePath != null) {
                request.header("Click", clickBaseUrl.replaceAll("/+$", "") + pagePath);
            }
            httpClient.sendAsync(request.build(), HttpResponse.BodyHandlers.discarding())
                    .whenComplete((response, error) -> {
                        if (error != null) {
                            log.warn("Admin notification failed: {}", error.toString());
                        } else if (response.statusCode() >= 300) {
                            log.warn("Admin notification rejected with HTTP {}", response.statusCode());
                        }
                    });
        } catch (Exception e) {
            log.warn("Admin notification could not be sent: {}", e.toString());
        }
    }
}
