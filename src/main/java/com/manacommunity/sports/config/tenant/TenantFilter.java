package com.manacommunity.sports.config.tenant;

import com.manacommunity.common.enums.*;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Servlet filter that extracts the tenant identifier from the
 * {@code X-Tenant-ID} HTTP header and stores it in {@link TenantContext}
 * for the duration of the request.
 * <p>
 * If the header is absent, the default schema ({@code manacommunity}) is used,
 * preserving backward compatibility with single-tenant deployments.
 * <p>
 * Runs at {@link Order} {@code 1} so it executes before Spring Security filters
 * and the JPA session is opened with the correct schema.
 */
@Component
@Order(1)
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-ID";

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
            throws ServletException, IOException {

        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId != null && !tenantId.isBlank()) {
            TenantContext.setTenantId(tenantId.trim().toLowerCase());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}

