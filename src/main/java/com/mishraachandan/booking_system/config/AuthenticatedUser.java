package com.mishraachandan.booking_system.config;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;

/**
 * Custom authentication principal that carries userId, keycloakId, email, and roles.
 *
 * Set on the SecurityContext by either:
 *   - JwtAuthenticationFilter (for legacy custom JJWT tokens)
 *   - KeycloakJwtConverter (for Keycloak-issued tokens validated via JWKS)
 *
 * Controllers inject it via @AuthenticationPrincipal AuthenticatedUser principal.
 */
public class AuthenticatedUser implements UserDetails {

    private final Long userId;           // Local DB primary key
    private final String keycloakId;     // Keycloak UUID (sub claim) — null for legacy tokens
    private final String email;
    private final Collection<? extends GrantedAuthority> authorities;

    public AuthenticatedUser(Long userId, String email, Collection<? extends GrantedAuthority> authorities) {
        this(userId, null, email, authorities);
    }

    public AuthenticatedUser(Long userId, String keycloakId, String email,
                             Collection<? extends GrantedAuthority> authorities) {
        this.userId = userId;
        this.keycloakId = keycloakId;
        this.email = email;
        this.authorities = authorities;
    }

    public Long getUserId() { return userId; }
    public String getKeycloakId() { return keycloakId; }
    public String getEmail() { return email; }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }

    @Override public String getPassword() { return ""; }
    @Override public String getUsername() { return email; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
