package com.deska.evolvelog.i18n;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

/**
 * Request-scoped bean that holds the resolved locale for the current HTTP request.
 * Populated by {@link LocaleInterceptor} before any service method runs.
 */
@Component
@RequestScope
public class LocaleContextHolder {

    private String locale = "en";

    public String getLocale() {
        return locale;
    }

    public void setLocale(String locale) {
        this.locale = locale;
    }
}
