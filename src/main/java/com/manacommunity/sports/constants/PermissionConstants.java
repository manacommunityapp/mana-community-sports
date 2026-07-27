package com.manacommunity.sports.constants;

import java.util.List;
import java.util.stream.Stream;
import java.util.Collections;

/**
 * PermissionConstants — Single source of truth for all permission keys used in the system.
 * These values are stored in the `role_permissions.permission_key` column.
 *
 * <p>Usage: Import statically where needed.</p>
 * <pre>
 *   import static com.manacommunity.sports.constants.PermissionConstants.*;
 * </pre>
 */
public final class PermissionConstants {

    private PermissionConstants() {
        // Non-instantiable utility class
    }

    // ──── ROLE NAMES ────────────────────────────────────────────────
    public static final String ROLE_SUPER_ADMIN    = "SUPER_ADMIN";
    public static final String ROLE_ADMIN          = "ADMIN";
    public static final String ROLE_COMMUNITY_ADMIN = "COMMUNITY_ADMIN";
    public static final String ROLE_SPORTS_ADMIN   = "SPORTS_ADMIN";
    public static final String ROLE_MEMBER         = "MEMBER";
    public static final String ROLE_VENDOR         = "VENDOR";
    public static final String ROLE_CASHIER        = "CASHIER";
    public static final String ROLE_STAFF          = "STAFF";

    // ──── COMMUNITY FEED ────────────────────────────────────────────
    public static final String VIEW_FEED       = "View Feed";
    public static final String CREATE_POST     = "Create Post";
    public static final String DELETE_POST     = "Delete Post";
    public static final String COMMENT_ON_POST = "Comment on Post";

    // ──── SPORTS — GRANULAR PERMISSIONS ─────────────────────────────
    // Main
    public static final String VIEW_SPORTS_MAIN          = "View Sports Main";
    public static final String CREATE_EDIT_SPORTS_MAIN   = "Create/Edit Sports Main";
    public static final String DELETE_SPORTS_MAIN        = "Delete Sports Main";
    // Sports Menu
    public static final String VIEW_SPORTS_MENU          = "View Sports Menu";
    public static final String CREATE_EDIT_SPORTS_MENU   = "Create/Edit Sports Menu";
    public static final String DELETE_SPORTS_MENU        = "Delete Sports Menu";
    // Auction Configuration
    public static final String VIEW_AUCTION_CONFIG       = "View Auction Configuration";
    public static final String CREATE_EDIT_AUCTION_CONFIG = "Create/Edit Auction Configuration";
    public static final String DELETE_AUCTION_CONFIG      = "Delete Auction Configuration";
    // Live Auction
    public static final String VIEW_LIVE_AUCTION         = "View Live Auction";
    public static final String CREATE_EDIT_LIVE_AUCTION  = "Create/Edit Live Auction";
    public static final String DELETE_LIVE_AUCTION       = "Delete Live Auction";
    // Teams Dashboard
    public static final String VIEW_TEAMS_DASHBOARD      = "View Teams Dashboard";
    public static final String CREATE_EDIT_TEAMS_DASHBOARD = "Create/Edit Teams Dashboard";
    public static final String DELETE_TEAMS_DASHBOARD    = "Delete Teams Dashboard";
    // Player Pool
    public static final String VIEW_PLAYER_POOL          = "View Player Pool";
    public static final String CREATE_EDIT_PLAYER_POOL   = "Create/Edit Player Pool";
    public static final String DELETE_PLAYER_POOL        = "Delete Player Pool";
    // Event Registrations
    public static final String VIEW_EVENT_REGISTRATIONS  = "View Event Registrations";
    public static final String CREATE_EDIT_EVENT_REGISTRATIONS = "Create/Edit Event Registrations";
    public static final String DELETE_EVENT_REGISTRATIONS = "Delete Event Registrations";
    // Auction Results
    public static final String VIEW_AUCTION_RESULTS      = "View Auction Results";
    public static final String CREATE_EDIT_AUCTION_RESULTS = "Create/Edit Auction Results";
    public static final String DELETE_AUCTION_RESULTS    = "Delete Auction Results";

    // ──── MARKETPLACE ───────────────────────────────────────────────
    public static final String VIEW_MARKETPLACE = "View Marketplace";
    public static final String CREATE_LISTING   = "Create Listing";
    public static final String DELETE_LISTING   = "Delete Listing";
    public static final String MANAGE_MARKETPLACE = "Manage Marketplace";

    // ──── VISITOR / GATE PASS ──────────────────────────────────────
    public static final String VIEW_VISITORS      = "View Visitors";
    public static final String CREATE_VISITOR_PASS = "Create Visitor Pass";
    public static final String MANAGE_GATE        = "Manage Gate";

    // ──── AMENITY BOOKING ────────────────────────────────────────────
    public static final String VIEW_AMENITIES    = "View Amenities";
    public static final String BOOK_AMENITY      = "Book Amenity";
    public static final String MANAGE_AMENITIES  = "Manage Amenities";

    // ──── NOTICE BOARD ───────────────────────────────────────────────
    public static final String VIEW_NOTICES   = "View Notices";
    public static final String CREATE_NOTICE  = "Create Notice";
    public static final String DELETE_NOTICE  = "Delete Notice";

    // ──── HELPDESK / COMPLAINTS ────────────────────────────────────
    public static final String VIEW_TICKETS    = "View Tickets";
    public static final String CREATE_TICKET   = "Create Ticket";
    public static final String MANAGE_TICKETS  = "Manage Tickets";

    // ──── POLLING / VOTING ─────────────────────────────────────────
    public static final String VIEW_POLLS   = "View Polls";
    public static final String CREATE_POLL  = "Create Poll";
    public static final String VOTE_POLL    = "Vote Poll";

    // ──── JOBS & REFERRALS ──────────────────────────────────────────
    public static final String VIEW_JOBS  = "View Jobs";
    public static final String CREATE_JOB = "Create Job";
    public static final String APPLY_JOB  = "Apply Job";

    // ──── EVENTS ────────────────────────────────────────────────────
    public static final String VIEW_EVENTS    = "View Events";
    public static final String CREATE_EVENT   = "Create Event";
    public static final String REGISTER_EVENT = "Register Event";


    // ──── SERVICE PLATFORM ────────────────────────────────────────────
    public static final String VIEW_SERVICE_CATALOG     = "View Service Catalog";
    public static final String MANAGE_SERVICE_CATALOG   = "Manage Service Catalog";
    public static final String VIEW_SERVICE_PROVIDERS   = "View Service Providers";
    public static final String MANAGE_SERVICE_PROVIDERS = "Manage Service Providers";
    public static final String CREATE_SERVICE_REQUEST   = "Create Service Request";
    public static final String VIEW_SERVICE_REQUESTS    = "View Service Requests";
    public static final String MANAGE_SERVICE_REQUESTS  = "Manage Service Requests";
    public static final String VIEW_WORK_ORDERS         = "View Work Orders";
    public static final String MANAGE_WORK_ORDERS       = "Manage Work Orders";

    // ──── VENDOR MANAGEMENT SYSTEM ─────────────────────────────────
    public static final String VIEW_VENDOR_MANAGEMENT  = "View Vendor Management";
    public static final String CREATE_VENDOR           = "Create Vendor";
    public static final String MANAGE_VENDORS          = "Manage Vendors";
    public static final String BOOK_VENDOR_SERVICE     = "Book Vendor Service";
    public static final String MANAGE_PROCUREMENT      = "Manage Procurement";
    public static final String MANAGE_CONTRACTS        = "Manage Contracts";
    public static final String MANAGE_VENDOR_PAYMENTS  = "Manage Vendor Payments";
    public static final String RATE_VENDOR             = "Rate Vendor";
    public static final String VIEW_VENDOR_ANALYTICS   = "View Vendor Analytics";


    // ──── FOOD & LIFESTYLE OS ─────────────────────────────────────
    public static final String VIEW_FOOD_PROFILE       = "View Food Profile";
    public static final String MANAGE_FOOD_PROFILE     = "Manage Food Profile";
    public static final String VIEW_FOOD_RESTAURANTS   = "View Food Restaurants";
    public static final String MANAGE_FOOD_RESTAURANTS = "Manage Food Restaurants";
    public static final String VIEW_FOOD_MENU          = "View Food Menu";
    public static final String MANAGE_FOOD_MENU        = "Manage Food Menu";
    public static final String VIEW_FOOD_HOME_CHEFS    = "View Food Home Chefs";
    public static final String MANAGE_FOOD_HOME_CHEFS  = "Manage Food Home Chefs";
    public static final String VIEW_FOOD_ORDERS        = "View Food Orders";
    public static final String MANAGE_FOOD_ORDERS      = "Manage Food Orders";
    public static final String VIEW_FOOD_SUBSCRIPTIONS = "View Food Subscriptions";
    public static final String MANAGE_FOOD_SUBSCRIPTIONS = "Manage Food Subscriptions";
    public static final String VIEW_FOOD_DINING        = "View Food Dining";
    public static final String MANAGE_FOOD_DINING      = "Manage Food Dining";
    public static final String VIEW_FOOD_GROCERY       = "View Food Grocery";
    public static final String MANAGE_FOOD_GROCERY     = "Manage Food Grocery";
    public static final String VIEW_FOOD_RECIPES       = "View Food Recipes";
    public static final String MANAGE_FOOD_RECIPES     = "Manage Food Recipes";
    public static final String VIEW_FOOD_NUTRITION     = "View Food Nutrition";
    public static final String MANAGE_FOOD_NUTRITION   = "Manage Food Nutrition";
    public static final String VIEW_FOOD_DELIVERY      = "View Food Delivery";
    public static final String MANAGE_FOOD_DELIVERY    = "Manage Food Delivery";
    public static final String VIEW_FOOD_COMMUNITY_KITCHEN  = "View Food Community Kitchen";
    public static final String MANAGE_FOOD_COMMUNITY_KITCHEN = "Manage Food Community Kitchen";
    public static final String VIEW_FOOD_CATERING      = "View Food Catering";
    public static final String MANAGE_FOOD_CATERING    = "Manage Food Catering";
    public static final String VIEW_FOOD_CORPORATE     = "View Food Corporate";
    public static final String MANAGE_FOOD_CORPORATE   = "Manage Food Corporate";
    public static final String VIEW_FOOD_EVENTS        = "View Food Events";
    public static final String MANAGE_FOOD_EVENTS      = "Manage Food Events";
    public static final String VIEW_FOOD_PANTRY        = "View Food Pantry";
    public static final String MANAGE_FOOD_PANTRY      = "Manage Food Pantry";
    public static final String VIEW_FOOD_LOYALTY       = "View Food Loyalty";
    public static final String MANAGE_FOOD_LOYALTY     = "Manage Food Loyalty";
    public static final String VIEW_FOOD_ANALYTICS     = "View Food Analytics";
    public static final String VIEW_FOOD_PAYMENTS      = "View Food Payments";
    public static final String MANAGE_FOOD_PAYMENTS    = "Manage Food Payments";
    public static final String VIEW_FOOD_CLOUD_KITCHENS  = "View Food Cloud Kitchens";
    public static final String MANAGE_FOOD_CLOUD_KITCHENS = "Manage Food Cloud Kitchens";

    // ──── ADMIN DASHBOARD ───────────────────────────────────────────
    public static final String VIEW_ADMIN         = "View Admin";
    public static final String VERIFY_KYC         = "Verify KYC";
    public static final String BULK_UPLOAD        = "Bulk Upload";
    public static final String MANAGE_COMMUNITIES = "Manage Communities";
    public static final String MANAGE_ROLES       = "Manage Roles";
    public static final String EDIT_VENUE_TIMING  = "Edit Venue Timing";

    // ──── SPORTS PERMISSION GROUPS ──────────────────────────────────
    /** All 8 View sports permissions */
    public static final List<String> ALL_SPORTS_VIEW_PERMISSIONS = List.of(
            VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU,
            VIEW_AUCTION_CONFIG, VIEW_LIVE_AUCTION,
            VIEW_TEAMS_DASHBOARD, VIEW_PLAYER_POOL,
            VIEW_EVENT_REGISTRATIONS, VIEW_AUCTION_RESULTS
    );

    /** All 24 sports permissions */
    public static final List<String> ALL_SPORTS_PERMISSIONS = List.of(
            VIEW_SPORTS_MAIN, CREATE_EDIT_SPORTS_MAIN, DELETE_SPORTS_MAIN,
            VIEW_SPORTS_MENU, CREATE_EDIT_SPORTS_MENU, DELETE_SPORTS_MENU,
            VIEW_AUCTION_CONFIG, CREATE_EDIT_AUCTION_CONFIG, DELETE_AUCTION_CONFIG,
            VIEW_LIVE_AUCTION, CREATE_EDIT_LIVE_AUCTION, DELETE_LIVE_AUCTION,
            VIEW_TEAMS_DASHBOARD, CREATE_EDIT_TEAMS_DASHBOARD, DELETE_TEAMS_DASHBOARD,
            VIEW_PLAYER_POOL, CREATE_EDIT_PLAYER_POOL, DELETE_PLAYER_POOL,
            VIEW_EVENT_REGISTRATIONS, CREATE_EDIT_EVENT_REGISTRATIONS, DELETE_EVENT_REGISTRATIONS,
            VIEW_AUCTION_RESULTS, CREATE_EDIT_AUCTION_RESULTS, DELETE_AUCTION_RESULTS
    );

    /**
     * Master list of ALL permission keys in the system.
     */
    public static final List<String> ALL_PERMISSIONS = Collections.unmodifiableList(
            Stream.of(
                    List.of(VIEW_FEED, CREATE_POST, DELETE_POST, COMMENT_ON_POST),
                    ALL_SPORTS_PERMISSIONS,
                    List.of(VIEW_MARKETPLACE, CREATE_LISTING, DELETE_LISTING, MANAGE_MARKETPLACE),
                    List.of(VIEW_VISITORS, CREATE_VISITOR_PASS, MANAGE_GATE),
                    List.of(VIEW_AMENITIES, BOOK_AMENITY, MANAGE_AMENITIES),
                    List.of(VIEW_NOTICES, CREATE_NOTICE, DELETE_NOTICE),
                    List.of(VIEW_TICKETS, CREATE_TICKET, MANAGE_TICKETS),
                    List.of(VIEW_POLLS, CREATE_POLL, VOTE_POLL),
                    List.of(VIEW_JOBS, CREATE_JOB, APPLY_JOB),
                    List.of(VIEW_EVENTS, CREATE_EVENT, REGISTER_EVENT),
                    List.of(VIEW_ADMIN, VERIFY_KYC, BULK_UPLOAD, MANAGE_COMMUNITIES, MANAGE_ROLES, EDIT_VENUE_TIMING),

                    List.of(VIEW_SERVICE_CATALOG, MANAGE_SERVICE_CATALOG, VIEW_SERVICE_PROVIDERS,
                            MANAGE_SERVICE_PROVIDERS, CREATE_SERVICE_REQUEST, VIEW_SERVICE_REQUESTS,
                            MANAGE_SERVICE_REQUESTS, VIEW_WORK_ORDERS, MANAGE_WORK_ORDERS),

                    List.of(VIEW_VENDOR_MANAGEMENT, CREATE_VENDOR, MANAGE_VENDORS, BOOK_VENDOR_SERVICE,
                            MANAGE_WORK_ORDERS, MANAGE_PROCUREMENT, MANAGE_CONTRACTS, MANAGE_VENDOR_PAYMENTS,
                            RATE_VENDOR, VIEW_VENDOR_ANALYTICS)

            ).flatMap(List::stream).toList()
    );

    public static final List<String> ADMIN_PERMISSIONS = Collections.unmodifiableList(
            Stream.of(
                    List.of(VIEW_FEED, CREATE_POST, DELETE_POST, COMMENT_ON_POST),
                    ALL_SPORTS_PERMISSIONS,
                    List.of(VIEW_MARKETPLACE, CREATE_LISTING, DELETE_LISTING, MANAGE_MARKETPLACE),
                    List.of(VIEW_VISITORS, CREATE_VISITOR_PASS, MANAGE_GATE),
                    List.of(VIEW_AMENITIES, BOOK_AMENITY, MANAGE_AMENITIES),
                    List.of(VIEW_NOTICES, CREATE_NOTICE, DELETE_NOTICE),
                    List.of(VIEW_TICKETS, CREATE_TICKET, MANAGE_TICKETS),
                    List.of(VIEW_POLLS, CREATE_POLL, VOTE_POLL),
                    List.of(VIEW_JOBS, CREATE_JOB, APPLY_JOB),
                    List.of(VIEW_EVENTS, CREATE_EVENT, REGISTER_EVENT),
                    List.of(VIEW_ADMIN, VERIFY_KYC, BULK_UPLOAD, MANAGE_ROLES, EDIT_VENUE_TIMING),

                    List.of(VIEW_SERVICE_CATALOG, MANAGE_SERVICE_CATALOG, VIEW_SERVICE_PROVIDERS,
                            MANAGE_SERVICE_PROVIDERS, CREATE_SERVICE_REQUEST, VIEW_SERVICE_REQUESTS,
                            MANAGE_SERVICE_REQUESTS, VIEW_WORK_ORDERS, MANAGE_WORK_ORDERS),

                    List.of(VIEW_VENDOR_MANAGEMENT, CREATE_VENDOR, MANAGE_VENDORS, BOOK_VENDOR_SERVICE,
                            MANAGE_WORK_ORDERS, MANAGE_PROCUREMENT, MANAGE_CONTRACTS, MANAGE_VENDOR_PAYMENTS,
                            RATE_VENDOR, VIEW_VENDOR_ANALYTICS)

            ).flatMap(List::stream).toList()
    );

    public static final List<String> SPORTS_ADMIN_PERMISSIONS = Collections.unmodifiableList(
            Stream.of(
                    List.of(VIEW_FEED, CREATE_POST, COMMENT_ON_POST),
                    ALL_SPORTS_PERMISSIONS,
                    List.of(VIEW_MARKETPLACE),
                    List.of(VIEW_VISITORS, CREATE_VISITOR_PASS),
                    List.of(VIEW_AMENITIES, BOOK_AMENITY),
                    List.of(VIEW_NOTICES, CREATE_NOTICE),
                    List.of(VIEW_TICKETS, CREATE_TICKET),
                    List.of(VIEW_POLLS, VOTE_POLL),
                    List.of(VIEW_JOBS),
                    List.of(VIEW_EVENTS, CREATE_EVENT, REGISTER_EVENT),
                    List.of(VIEW_ADMIN, EDIT_VENUE_TIMING)
            ).flatMap(List::stream).toList()
    );

    public static final List<String> MEMBER_PERMISSIONS = List.of(
            VIEW_FEED, CREATE_POST, COMMENT_ON_POST,
            VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU,
            VIEW_AUCTION_CONFIG, VIEW_LIVE_AUCTION,
            VIEW_TEAMS_DASHBOARD, VIEW_PLAYER_POOL,
            VIEW_EVENT_REGISTRATIONS, VIEW_AUCTION_RESULTS,
            VIEW_MARKETPLACE,
            VIEW_VENDOR_MANAGEMENT, BOOK_VENDOR_SERVICE, RATE_VENDOR,
            VIEW_VISITORS, CREATE_VISITOR_PASS,
            VIEW_AMENITIES, BOOK_AMENITY,
            VIEW_NOTICES,
            VIEW_TICKETS, CREATE_TICKET,
            VIEW_POLLS, VOTE_POLL,
            VIEW_JOBS, APPLY_JOB,
            VIEW_EVENTS, REGISTER_EVENT,
            VIEW_SERVICE_CATALOG, VIEW_SERVICE_PROVIDERS,
            CREATE_SERVICE_REQUEST, VIEW_SERVICE_REQUESTS, VIEW_WORK_ORDERS
    );

    public static final List<String> VENDOR_PERMISSIONS = List.of(
            VIEW_FEED, CREATE_POST, COMMENT_ON_POST,
            VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU,
            VIEW_MARKETPLACE, CREATE_LISTING, DELETE_LISTING,
            VIEW_VISITORS,
            VIEW_AMENITIES,
            VIEW_NOTICES,
            VIEW_TICKETS, CREATE_TICKET,
            VIEW_POLLS, VOTE_POLL,
            VIEW_JOBS, CREATE_JOB,
            VIEW_EVENTS, REGISTER_EVENT,
            VIEW_VENDOR_MANAGEMENT, CREATE_VENDOR, MANAGE_VENDORS, MANAGE_VENDOR_PAYMENTS
    );

    public static final List<String> CASHIER_PERMISSIONS = List.of(
            VIEW_FEED, COMMENT_ON_POST,
            VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU,
            VIEW_MARKETPLACE,
            VIEW_VISITORS,
            VIEW_AMENITIES,
            VIEW_NOTICES,
            VIEW_TICKETS,
            VIEW_POLLS, VOTE_POLL,
            VIEW_JOBS,
            VIEW_EVENTS
    );

    public static final List<String> STAFF_PERMISSIONS = List.of(
            VIEW_FEED, COMMENT_ON_POST,
            VIEW_SPORTS_MAIN, VIEW_SPORTS_MENU,
            VIEW_MARKETPLACE,
            VIEW_VISITORS, MANAGE_GATE,
            VIEW_AMENITIES,
            VIEW_NOTICES,
            VIEW_TICKETS, MANAGE_TICKETS,
            VIEW_POLLS, VOTE_POLL,
            VIEW_JOBS,
            VIEW_EVENTS
    );
}
