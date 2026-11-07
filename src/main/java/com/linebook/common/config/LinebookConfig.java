package com.linebook.common.config;

import com.linebook.auth.service.AuthService;
import com.linebook.auth.service.TokenService;
import com.linebook.auth.web.AuthFilter;
import com.linebook.common.web.RequestIdFilter;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

@Configuration
public class LinebookConfig {

    /** The one clock for "today", the current billing cycle and receipt dates; fixed in tests. */
    @Bean
    Clock clock(LinebookProperties props) {
        return Clock.system(ZoneId.of(props.zone()));
    }

    /** Runs first, so browser pre-flight (OPTIONS) requests are answered here. */
    @Bean
    FilterRegistrationBean<CorsFilter> corsFilter(LinebookProperties props) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(props.cors().allowedOrigins());
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setExposedHeaders(List.of(RequestIdFilter.HEADER));
        config.setMaxAge(props.cors().maxAge());

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        FilterRegistrationBean<CorsFilter> registration = new FilterRegistrationBean<>(new CorsFilter(source));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return registration;
    }

    @Bean
    FilterRegistrationBean<RequestIdFilter> requestIdFilter() {
        FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>(new RequestIdFilter());
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
        return registration;
    }

    /**
     * Checks the Authorization header. Runs after CORS (so pre-flight still works) and after the
     * request-id filter (so a rejected request still gets a request id in its log line).
     */
    @Bean
    FilterRegistrationBean<AuthFilter> authFilter(TokenService tokens, AuthService auth) {
        FilterRegistrationBean<AuthFilter> registration =
                new FilterRegistrationBean<>(new AuthFilter(tokens, auth));
        registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 20);
        return registration;
    }
}
