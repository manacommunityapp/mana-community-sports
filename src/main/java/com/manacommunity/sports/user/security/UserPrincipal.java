package com.manacommunity.sports.user.security;

import com.manacommunity.sports.user.model.AppUser;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * BUG FIX: AuthController and AuctionController used
 * `com.sun.security.auth.UserPrincipal` — an internal JDK class that:
 *   (a) is not part of the public API,
 *   (b) does not have a getId() method returning Long,
 *   (c) will cause ClassCastException at runtime from Spring Security.
 *
 * Replaced with a proper app-level UserPrincipal that implements UserDetails
 * and carries the authenticated user's database ID.
 */
public class UserPrincipal implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final AppUser user;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(Long id, String email, String password,
                         Collection<? extends GrantedAuthority> authorities) {
        this(id, email, password, authorities, null);
    }

    public UserPrincipal(Long id, String email, String password,
                         Collection<? extends GrantedAuthority> authorities,
                         AppUser user) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authorities = authorities;
        this.user = user;
    }

    public Long getId() { return id; }
    public Long getUserId() { return id; }
    public AppUser getUser() { return user; }

    public Long getCommunityId() {
        return user != null && user.getCommunity() != null ? user.getCommunity().getId() : null;
    }

    @Override public String getUsername() { return email; }
    @Override public String getPassword() { return password; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return authorities; }
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
