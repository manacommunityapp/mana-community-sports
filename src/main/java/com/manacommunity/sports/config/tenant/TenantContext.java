package com.manacommunity.sports.config.tenant;

import com.manacommunity.common.enums.*;
/**
 * Thread-local holder for the current tenant schema name.
 * Set by TenantFilter on each request, cleared on completion.
 */
public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    public static final String DEFAULT_SCHEMA = "manacommunity";

    private TenantContext() {}

    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static String getTenantId() {
        String tenant = CURRENT_TENANT.get();
        return tenant != null ? tenant : DEFAULT_SCHEMA;
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}

