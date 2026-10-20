package com.quizapp.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a response DTO whose text can have an admin-written Norwegian / English version (see
 * {@link com.quizapp.service.ContentTranslationService}). {@link TranslationResponseAdvice} swaps the text in
 * for the language the request asked for, right before the response is written - so no service needs to know
 * about languages, and the base text on the entity is the fallback whenever no version exists.
 *
 * <p>{@code fields} entries are "property" (the DTO property and the translation field share a name) or
 * "property:field" (e.g. {@code gridTitle:title}). A property holding a {@code List<String>} is translated
 * element by element ("hints" -> hints#0, hints#1, ...).
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Translated {
    /** One of {@link com.quizapp.model.ContentTranslation}'s TYPE_* constants. */
    String type();

    /** The DTO property holding the board's id. */
    String id() default "id";

    String[] fields();
}
