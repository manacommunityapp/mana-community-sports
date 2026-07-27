package com.manacommunity.sports.constants;

import java.util.List;

public final class ModuleConstants {

    private ModuleConstants() {}

    public record ModuleDef(String key, String label, int sortOrder) {}

    public static final List<ModuleDef> ALL_MODULES = List.of(
        new ModuleDef("COMMUNITY_FEED",  "Community Feed",    1),
        new ModuleDef("SPORTS",          "Sports",            2),
        new ModuleDef("MARKETPLACE",     "Marketplace",       3),
        new ModuleDef("VISITORS",        "Visitors",          4),
        new ModuleDef("NOTICES",         "Notices",           5),
        new ModuleDef("BOOKINGS",        "Bookings",          6),
        new ModuleDef("HELPDESK",        "Helpdesk",          7),
        new ModuleDef("POLLS",           "Polls",             8),
        new ModuleDef("JOBS",            "Jobs & Referrals",  9),
        new ModuleDef("EVENTS",          "Events",           10),
        new ModuleDef("COMMUNITY_MGMT",  "Community Mgmt",   11),
        new ModuleDef("FINANCE_MGMT",    "Finance Mgmt",     12),
        new ModuleDef("ADMIN_HUB",       "Admin Hub",        13),
        new ModuleDef("FOOD_OS",         "Food & Lifestyle", 14),
        new ModuleDef("VENDOR_MANAGEMENT","Vendor Management",15)
    );
}
