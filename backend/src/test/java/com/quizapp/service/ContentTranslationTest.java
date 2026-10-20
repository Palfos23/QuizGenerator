package com.quizapp.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.quizapp.config.TranslationResponseAdvice;
import com.quizapp.dto.FlashbackYearDto;
import com.quizapp.dto.FlashbackYearRequest;
import com.quizapp.model.ContentTranslation;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.List;
import java.util.Map;

// Admin-written Norwegian / English versions of a board's text: stored per field, swapped into player-facing
// responses for the language the request asks for, and never into admin responses.
@SpringBootTest
class ContentTranslationTest {

    @Autowired
    private ContentTranslationService translations;
    @Autowired
    private FlashbackAdminService flashbackAdminService;
    @Autowired
    private TranslationResponseAdvice advice;

    private Long newYear() {
        FlashbackYearRequest request = new FlashbackYearRequest();
        request.setTitle("Translation test " + System.nanoTime());
        request.setYear(1969);
        request.setHints(List.of("Hardest clue", "Easier clue"));
        return flashbackAdminService.create(request).getId();
    }

    private FlashbackYearDto dto(Long id, List<String> hints) {
        FlashbackYearDto dto = new FlashbackYearDto();
        dto.setId(id);
        dto.setTitle("Base title");
        dto.setHints(hints);
        return dto;
    }

    private Object respond(Object body, String path, String language) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", path);
        if (language != null) request.addHeader("Accept-Language", language);
        return advice.beforeBodyWrite(body, null, null, null, new ServletServerHttpRequest(request), null);
    }

    @Test
    void editorListsEveryFieldWithItsBaseTextAndSavedVersions() {
        Long id = newYear();
        translations.save(ContentTranslation.TYPE_FLASHBACK, id, Map.of("hints#0", Map.of("en", "Hardest clue (EN)", "no", "Vanskeligste hint")));

        List<ContentTranslationService.FieldView> fields = translations.getEditor(ContentTranslation.TYPE_FLASHBACK, id);

        assertThat(fields).extracting(ContentTranslationService.FieldView::getKey).containsExactly("hints#0", "hints#1");
        assertThat(fields.get(0).getBase()).isEqualTo("Hardest clue");
        assertThat(fields.get(0).getValues()).containsEntry("en", "Hardest clue (EN)").containsEntry("no", "Vanskeligste hint");
        assertThat(fields.get(1).getValues()).containsEntry("en", "").containsEntry("no", "");
    }

    @Test
    void aBlankTextRemovesThatVersionAgain() {
        Long id = newYear();
        translations.save(ContentTranslation.TYPE_FLASHBACK, id, Map.of("hints#1", Map.of("en", "Easier clue (EN)")));
        translations.save(ContentTranslation.TYPE_FLASHBACK, id, Map.of("hints#1", Map.of("en", "  ")));

        assertThat(translations.getEditor(ContentTranslation.TYPE_FLASHBACK, id).get(1).getValues()).containsEntry("en", "");
    }

    @Test
    void unknownFieldsAndLanguagesAreRejected() {
        Long id = newYear();
        assertThatThrownBy(() -> translations.save(ContentTranslation.TYPE_FLASHBACK, id, Map.of("nonsense", Map.of("en", "x"))))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> translations.save(ContentTranslation.TYPE_FLASHBACK, id, Map.of("hints#0", Map.of("fr", "x"))))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void playerResponsesGetTheVersionForTheRequestedLanguage() {
        Long id = newYear();
        translations.save(ContentTranslation.TYPE_FLASHBACK, id, Map.of(
                "hints#0", Map.of("en", "Hardest clue (EN)", "no", "Vanskeligste hint (NO)")));

        FlashbackYearDto english = (FlashbackYearDto) respond(dto(id, List.of("Hardest clue", "Easier clue")), "/api/flashback/round", "en");
        assertThat(english.getHints()).containsExactly("Hardest clue (EN)", "Easier clue");

        FlashbackYearDto norwegian = (FlashbackYearDto) respond(dto(id, List.of("Hardest clue", "Easier clue")), "/api/flashback/round", "nb");
        assertThat(norwegian.getHints()).containsExactly("Vanskeligste hint (NO)", "Easier clue");

        FlashbackYearDto noHeader = (FlashbackYearDto) respond(dto(id, List.of("Hardest clue", "Easier clue")), "/api/flashback/round", null);
        assertThat(noHeader.getHints().get(0)).isEqualTo("Vanskeligste hint (NO)");
    }

    @Test
    void aFieldWithoutAVersionKeepsItsBaseText() {
        Long id = newYear();
        translations.save(ContentTranslation.TYPE_FLASHBACK, id, Map.of("hints#0", Map.of("no", "Kun norsk")));

        FlashbackYearDto english = (FlashbackYearDto) respond(dto(id, List.of("Hardest clue", "Easier clue")), "/api/flashback/round", "en");

        assertThat(english.getHints()).containsExactly("Hardest clue", "Easier clue");
        assertThat(english.getTitle()).isEqualTo("Base title");
    }

    @Test
    void adminResponsesAreNeverTranslated() {
        Long id = newYear();
        translations.save(ContentTranslation.TYPE_FLASHBACK, id, Map.of("hints#0", Map.of("en", "Hardest clue (EN)")));

        FlashbackYearDto admin = (FlashbackYearDto) respond(dto(id, List.of("Hardest clue", "Easier clue")), "/api/admin/flashback/years/" + id, "en");

        assertThat(admin.getHints()).containsExactly("Hardest clue", "Easier clue");
    }
}
