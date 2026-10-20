package com.quizapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.quizapp.config.MessageTranslations;
import com.quizapp.config.RequestLang;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class RequestLanguageTest {

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void requestWithLanguage(String header) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (header != null) request.addHeader("Accept-Language", header);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    @Test
    void defaultsToNorwegianWithoutARequestOrHeader() {
        assertThat(RequestLang.isEnglish()).isFalse();
        requestWithLanguage(null);
        assertThat(RequestLang.t("Hei", "Hello")).isEqualTo("Hei");
        requestWithLanguage("nb");
        assertThat(RequestLang.t("Hei", "Hello")).isEqualTo("Hei");
    }

    @Test
    void englishIsPickedFromTheAcceptLanguageHeader() {
        requestWithLanguage("en");
        assertThat(RequestLang.t("Hei", "Hello")).isEqualTo("Hello");
        requestWithLanguage("en-GB,en;q=0.9");
        assertThat(RequestLang.isEnglish()).isTrue();
    }

    @Test
    void messagesAreTranslatedFragmentByFragment() {
        assertThat(MessageTranslations.toEnglish("Fant ikke grid med id 12")).isEqualTo("No grid found with id 12");
        assertThat(MessageTranslations.toEnglish("Bare verten kan starte spillet.")).isEqualTo("Only the host can start the game.");
        assertThat(MessageTranslations.toEnglish("Noe helt ukjent")).isEqualTo("Noe helt ukjent");
    }
}
