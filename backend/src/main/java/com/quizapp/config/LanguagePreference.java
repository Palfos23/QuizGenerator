package com.quizapp.config;

import com.quizapp.repository.ContentTranslationRepository;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * "Prefer the player's language" for random picks of game boards. A board counts as being in a language when an
 * admin has written a version of any of its text in that language (see {@link com.quizapp.model.ContentTranslation});
 * everything else is still eligible, just picked only after the matching boards run out - so a game never fails
 * to find enough boards because too few are translated.
 */
@Component
public class LanguagePreference {

    private final ContentTranslationRepository repository;

    public LanguagePreference(ContentTranslationRepository repository) {
        this.repository = repository;
    }

    /** The language of the current request: "en" or "no". */
    public static String requestLanguage() {
        return RequestLang.isEnglish() ? "en" : "no";
    }

    /** Which of these ids have a version in the request's language. */
    public Set<Long> translatedIds(String type, Collection<Long> ids) {
        if (ids.isEmpty()) return Collections.emptySet();
        return new HashSet<>(repository.findTranslatedIds(type, requestLanguage(), ids));
    }

    /**
     * Same ids, matching boards first. Stable, so an already-shuffled list stays random within each group - call
     * this after the shuffle and before taking the first N.
     */
    public List<Long> preferTranslated(String type, List<Long> ids) {
        Set<Long> translated = translatedIds(type, ids);
        if (translated.isEmpty() || translated.size() == ids.size()) return ids;
        List<Long> out = new ArrayList<>(ids.size());
        for (Long id : ids) if (translated.contains(id)) out.add(id);
        for (Long id : ids) if (!translated.contains(id)) out.add(id);
        return out;
    }
}
