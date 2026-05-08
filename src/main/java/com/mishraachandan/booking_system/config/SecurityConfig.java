package com.mishraachandan.booking_system.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.issuer-uri:http://localhost:8180/realms/booking-system}")
    private String keycloakIssuerUri;

    @Value("${keycloak.enabled:false}")
    private boolean keycloakEnabled;

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final KeycloakJwtConverter keycloakJwtConverter;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          KeycloakJwtConverter keycloakJwtConverter) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.keycloakJwtConverter = keycloakJwtConverter;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Keycloak JWT decoder — validates Keycloak-issued tokens via JWKS endpoint.
     * Only created when keycloak.enabled=true (i.e. Keycloak is actually running).
     * When disabled, the custom JJWT filter handles all authentication.
     */
    @Bean
    @ConditionalOnProperty(name = "keycloak.enabled", havingValue = "true", matchIfMissing = false)
    public JwtDecoder jwtDecoder() {
        return NimbusJwtDecoder
                .withIssuerLocation(keycloakIssuerUri)
                .build();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .cors(cors -> cors.configurationSource(request -> {
                    var corsConfig = new org.springframework.web.cors.CorsConfiguration();
                    corsConfig.setAllowedOrigins(java.util.List.of(
                            "http://localhost:4200",
                            "http://localhost:5173"
                    ));
                    corsConfig.setAllowedMethods(java.util.List.of(
                            "GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
                    // Explicit allow-list instead of "*". Wildcarding allowed-headers
                    // together with allowCredentials=true violates the CORS spec and
                    // is rejected by modern browsers, so we enumerate exactly the
                    // request headers this API accepts.
                    corsConfig.setAllowedHeaders(java.util.List.of(
                            "Authorization",
                            "Content-Type",
                            "Accept",
                            "Origin",
                            "X-Requested-With",
                            "X-Trace-Id"
                    ));
                    corsConfig.setExposedHeaders(java.util.List.of("X-Trace-Id"));
                    corsConfig.setAllowCredentials(true);
                    corsConfig.setMaxAge(3600L);
                    return corsConfig;
                }))
                .authorizeHttpRequests(auth -> auth
                        // Auth endpoints — open (registration, OTP, legacy login)
                        .requestMatchers("/api/auth/**").permitAll()
                        // Swagger / OpenAPI docs — open
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**"
                        ).permitAll()
                        // Public read-only data
                        .requestMatchers(HttpMethod.GET,
                                "/api/v1/shows/**",
                                "/api/v1/movies/**",
                                "/api/v1/addons/**",
                                "/api/v1/cities/**",
                                "/api/cities/**",
                                "/api/categories/**",
                                "/api/cinemas/**"
                        ).permitAll()
                        // Booking & seat-locking — require USER role
                        .requestMatchers(HttpMethod.POST,
                                "/api/bookings/**",
                                "/api/v1/shows/*/seats/lock",
                                "/api/payments/**"
                        ).hasRole("USER")
                        // Payment status reads — authenticated
                        .requestMatchers(HttpMethod.GET, "/api/payments/**").authenticated()
                        // Creating shows — require ADMIN role
                        .requestMatchers(HttpMethod.POST, "/api/v1/shows").hasRole("ADMIN")
                        // Admin analytics + dynamic-pricing management — ADMIN only
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        // Everything else — authenticated
                        .anyRequest().authenticated()
                )
                // ── Dual JWT support ───────────────────────────────────────────────────
                // Custom JJWT filter (HS256) always runs — handles legacy tokens from /api/auth/login.
                // Keycloak OAuth2 resource server only added when keycloak.enabled=true.
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Only wire Keycloak OAuth2 resource server when Keycloak is enabled
        if (keycloakEnabled) {
            http.oauth2ResourceServer(oauth2 -> oauth2
                .jwt(jwt -> jwt.jwtAuthenticationConverter(keycloakJwtConverter))
            );
        }

        return http.build();
    }
}
