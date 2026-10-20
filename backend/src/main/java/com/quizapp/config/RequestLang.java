package com.quizapp.config;

import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * The language the current request asked for. The frontend sends "Accept-Language: en" or "nb" on every call
 * (see services/api.js); anything else - including no request at all, e.g. a scheduled job - means Norwegian,
 * the app's default.
 */
public final class RequestLang {
    private RequestLang() {}

    public static boolean isEnglish() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs)) return false;
        String header = attrs.getRequest().getHeader("Accept-Language");
        return header != null && header.trim().toLowerCase().startsWith("en");
    }

    /** Norwegian text for most callers, English when the request asked for it. */
    public static String t(String norwegian, String english) {
        return isEnglish() ? english : norwegian;
    }
}
