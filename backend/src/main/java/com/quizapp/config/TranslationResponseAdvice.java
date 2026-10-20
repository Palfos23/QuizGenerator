package com.quizapp.config;

import com.quizapp.model.ContentTranslation;
import com.quizapp.repository.ContentTranslationRepository;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.PropertyAccessorFactory;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.util.ReflectionUtils;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.*;

/**
 * Swaps admin-written translations (see {@link Translated}) into player-facing responses, just before they're
 * serialised, for the language in the request's Accept-Language header. Admin endpoints are skipped - an admin
 * edits the base text and the translations separately and must always see the base text.
 *
 * <p>Works on the freshly built response DTOs only (never on entities), and replaces list values with copies,
 * so nothing shared or persisted is touched. A field with no version for the requested language is left alone.
 */
@RestControllerAdvice
public class TranslationResponseAdvice implements ResponseBodyAdvice<Object> {

    private static final String DTO_PACKAGE = "com.quizapp.dto";

    private final ContentTranslationRepository repository;

    public TranslationResponseAdvice(ContentTranslationRepository repository) {
        this.repository = repository;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        if (body == null || request.getURI().getPath().contains("/api/admin/")) return body;
        List<String> languages = request.getHeaders().get("Accept-Language");
        String lang = languages != null && !languages.isEmpty() && languages.get(0).trim().toLowerCase().startsWith("en") ? "en" : "no";

        List<Object> targets = new ArrayList<>();
        collect(body, targets, Collections.newSetFromMap(new IdentityHashMap<>()), 0);
        if (targets.isEmpty()) return body;
        try {
            apply(targets, lang);
        } catch (RuntimeException e) {
            // Translations are a nicety - never fail a response (or the game) over them.
        }
        return body;
    }

    private void collect(Object o, List<Object> out, Set<Object> seen, int depth) {
        if (o == null || depth > 8) return;
        if (o instanceof Collection<?> c) {
            for (Object e : c) collect(e, out, seen, depth + 1);
            return;
        }
        if (o instanceof Map<?, ?> m) {
            for (Object e : m.values()) collect(e, out, seen, depth + 1);
            return;
        }
        if (o.getClass().isArray()) {
            if (o.getClass().getComponentType().isPrimitive()) return;
            for (int i = 0; i < Array.getLength(o); i++) collect(Array.get(o, i), out, seen, depth + 1);
            return;
        }
        if (!o.getClass().getName().startsWith(DTO_PACKAGE) || !seen.add(o)) return;
        if (o.getClass().isAnnotationPresent(Translated.class)) out.add(o);
        for (Class<?> c = o.getClass(); c != null && c != Object.class; c = c.getSuperclass()) {
            for (Field f : c.getDeclaredFields()) {
                if (Modifier.isStatic(f.getModifiers()) || f.getType().isPrimitive() || f.getType().isEnum()
                        || f.getType() == String.class || Number.class.isAssignableFrom(f.getType())) continue;
                ReflectionUtils.makeAccessible(f);
                collect(ReflectionUtils.getField(f, o), out, seen, depth + 1);
            }
        }
    }

    private void apply(List<Object> targets, String lang) {
        // one query per content type
        Map<String, Set<Long>> idsByType = new HashMap<>();
        Map<Object, Long> idOf = new IdentityHashMap<>();
        for (Object t : targets) {
            Translated meta = t.getClass().getAnnotation(Translated.class);
            Object id = PropertyAccessorFactory.forBeanPropertyAccess(t).getPropertyValue(meta.id());
            if (id instanceof Long l) {
                idOf.put(t, l);
                idsByType.computeIfAbsent(meta.type(), k -> new HashSet<>()).add(l);
            }
        }
        Map<String, String> found = new HashMap<>(); // "TYPE|id|field" -> text
        for (Map.Entry<String, Set<Long>> e : idsByType.entrySet()) {
            for (ContentTranslation t : repository.findByEntityTypeAndEntityIdInAndLang(e.getKey(), e.getValue(), lang)) {
                found.put(e.getKey() + "|" + t.getEntityId() + "|" + t.getFieldName(), t.getContent());
            }
        }
        if (found.isEmpty()) return;
        Set<String> boardsWithAVersion = new HashSet<>();
        for (String key : found.keySet()) boardsWithAVersion.add(key.substring(0, key.lastIndexOf('|')));
        for (Object t : targets) {
            Long id = idOf.get(t);
            if (id == null) continue;
            Translated meta = t.getClass().getAnnotation(Translated.class);
            BeanWrapper bw = PropertyAccessorFactory.forBeanPropertyAccess(t);
            // Lets a client that picks randomly itself (501's "different category") favour boards in the player's language.
            if (bw.isWritableProperty("inYourLanguage")) {
                bw.setPropertyValue("inYourLanguage", boardsWithAVersion.contains(meta.type() + "|" + id));
            }
            for (String spec : meta.fields()) {
                String property = spec.contains(":") ? spec.substring(0, spec.indexOf(':')) : spec;
                String field = spec.contains(":") ? spec.substring(spec.indexOf(':') + 1) : spec;
                Object current = bw.getPropertyValue(property);
                String prefix = meta.type() + "|" + id + "|";
                if (current instanceof String) {
                    String text = found.get(prefix + field);
                    if (text != null) bw.setPropertyValue(property, text);
                } else if (current instanceof List<?> list) {
                    List<Object> copy = new ArrayList<>(list);
                    boolean changed = false;
                    for (int i = 0; i < copy.size(); i++) {
                        String text = found.get(prefix + field + "#" + i);
                        if (text != null) { copy.set(i, text); changed = true; }
                    }
                    if (changed) bw.setPropertyValue(property, copy);
                }
            }
        }
    }
}
