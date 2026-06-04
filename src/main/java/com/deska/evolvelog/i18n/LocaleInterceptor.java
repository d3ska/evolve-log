package com.deska.evolvelog.i18n;

import com.deska.evolvelog.domain.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Locale;
import java.util.Set;

/**
 * Resolves the locale for each request and stores it in the request-scoped
 * {@link LocaleContextHolder}.
 *
 * Priority:
 * 1. {@code users.locale} — from the authenticated Spring Security principal
 * 2. {@code Accept-Language} header — first tag only (e.g. "pl-PL" → "pl")
 * 3. Hard default: {@code "en"}
 */
@Component
public class LocaleInterceptor implements HandlerInterceptor {

    private static final Set<String> SUPPORTED = Set.of("en", "pl");

    private final LocaleContextHolder localeContextHolder;

    public LocaleInterceptor(LocaleContextHolder localeContextHolder) {
        this.localeContextHolder = localeContextHolder;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        localeContextHolder.setLocale(resolveLocale(request));
        return true;
    }

    private String resolveLocale(HttpServletRequest request) {
        // 1. Authenticated user preference
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User user) {
            String userLocale = user.getLocale();
            if (userLocale != null && SUPPORTED.contains(userLocale)) {
                return userLocale;
            }
        }

        // 2. Accept-Language header (first tag only)
        Locale acceptLocale = request.getLocale();
        if (acceptLocale != null) {
            String lang = acceptLocale.getLanguage();
            if (SUPPORTED.contains(lang)) {
                return lang;
            }
        }

        // 3. Default
        return "en";
    }
}
