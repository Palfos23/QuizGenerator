package com.quizapp.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.quizapp.config.LanguagePreference;
import com.quizapp.dto.FlashbackYearDto;
import com.quizapp.dto.FlashbackYearRequest;
import com.quizapp.model.ContentTranslation;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

// Random picks favour boards that have a version in the player's language - but never at the price of not
// finding enough boards: the rest are still eligible once the matching ones run out.
@SpringBootTest
class LanguagePreferenceTest {

    @Autowired
    private LanguagePreference languagePreference;
    @Autowired
    private ContentTranslationService translations;
    @Autowired
    private FlashbackAdminService flashbackAdminService;
    @Autowired
    private FlashbackPlayService flashbackPlayService;
    @Autowired
    private com.quizapp.repository.FlashbackYearRepository flashbackYearRepository;

    @AfterEach
    void clear() {
        RequestContextHolder.resetRequestAttributes();
    }

    private void requestIn(String language) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Accept-Language", language);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private Long newYear() {
        FlashbackYearRequest request = new FlashbackYearRequest();
        request.setTitle("Language preference test " + System.nanoTime());
        request.setYear(1999);
        request.setHints(List.of("A clue"));
        return flashbackAdminService.create(request).getId();
    }

    @Test
    void matchingBoardsComeFirstAndTheRestKeepTheirOrder() {
        Long a = newYear(), b = newYear(), c = newYear();
        translations.save(ContentTranslation.TYPE_FLASHBACK, c, Map.of("hints#0", Map.of("en", "An English clue")));

        requestIn("en");
        assertThat(languagePreference.preferTranslated(ContentTranslation.TYPE_FLASHBACK, new ArrayList<>(List.of(a, b, c))))
                .containsExactly(c, a, b);
    }

    @Test
    void theOrderIsUntouchedWhenNothingMatchesTheLanguage() {
        Long a = newYear(), b = newYear(), c = newYear();
        translations.save(ContentTranslation.TYPE_FLASHBACK, c, Map.of("hints#0", Map.of("en", "An English clue")));

        requestIn("nb"); // c only has an English version
        assertThat(languagePreference.preferTranslated(ContentTranslation.TYPE_FLASHBACK, new ArrayList<>(List.of(a, b, c))))
                .containsExactly(a, b, c);
    }

    @Test
    void aRoundOfChoicesTakesTheMatchingBoardFirstButStillFillsUpWithOthers() {
        Long translated = newYear();
        translations.save(ContentTranslation.TYPE_FLASHBACK, translated, Map.of("hints#0", Map.of("en", "An English clue")));
        newYear();
        newYear();

        requestIn("en");
        // other tests share this database and may have translated boards of their own - whichever ones have an
        // English version are the only valid picks
        java.util.Set<Long> inEnglish = languagePreference.translatedIds(ContentTranslation.TYPE_FLASHBACK,
                new ArrayList<>(flashbackYearRepository.findEligibleIds()));
        assertThat(inEnglish).contains(translated);
        for (int i = 0; i < 5; i++) { // the shuffle is random - a matching board must win every time
            List<FlashbackYearDto> one = flashbackPlayService.getRoundChoices(1, List.of());
            assertThat(one).hasSize(1);
            assertThat(inEnglish).contains(one.get(0).getId());
        }
        assertThat(flashbackPlayService.getRoundChoices(3, List.of())).hasSize(3);
    }
}
