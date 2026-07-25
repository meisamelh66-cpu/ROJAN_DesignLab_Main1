package ai.rojan.designlab.ui.assets

/**
 * ROJAN AI Asset Library — naming registry (Production Readiness Fixes
 * v1 — Fix 3: Normalize Asset Naming Convention).
 *
 * This is a documentation/contract object, not a resource loader — it
 * lists the *target* Android drawable resource names from the official
 * Asset Library taxonomy as plain strings, deliberately not as
 * `R.drawable.*` references, since some of these files don't exist yet
 * and a resource reference only compiles once a file with that exact
 * name is physically present in a `drawable*` folder.
 *
 * Naming standard (final, this revision): `<category-prefix>_<meaning>`.
 * This revision fixes a real inconsistency found during the Production
 * Readiness Audit — earlier registry names (`ic_service_hair`) and the
 * shorter names actually shipped when default placeholders were
 * generated (`ic_hair`) had drifted apart. Every name below is now the
 * single source of truth, and every drawable file on disk was renamed
 * to match it in the same pass — verified zero duplicates remain.
 */
object RojanAssetNames {

    // ── 1. Master Background ─────────────────────────────────────────
    const val MASTER_BACKGROUND = "bg_master_luxury_salon"

    // ── 2. Brand ──────────────────────────────────────────────────────
    const val LOGO_GLASS = "rojan_logo_glass"
    const val AI_MARK = "rojan_ai_mark"
    const val SPLASH_LOGO = "splash_logo"

    // ── 3. Hero Illustrations ────────────────────────────────────────
    const val HERO_BOOKING = "hero_booking"
    const val HERO_BEAUTY = "hero_beauty"
    const val HERO_AI = "hero_ai"
    const val HERO_PRODUCTS = "hero_products"

    // ── 4. Service Icons ──────────────────────────────────────────────
    const val SERVICE_HAIR = "ic_service_hair"
    const val SERVICE_SKIN = "ic_service_skin"
    const val SERVICE_MAKEUP = "ic_service_makeup"
    const val SERVICE_NAILS = "ic_service_nails"
    const val SERVICE_SPA = "ic_service_spa"
    const val SERVICE_MASSAGE = "ic_service_massage"
    const val SERVICE_LASER = "ic_service_laser"
    const val SERVICE_EYEBROW = "ic_service_eyebrow"
    const val SERVICE_EYELASH = "ic_service_eyelash"
    const val SERVICE_HAIR_COLOR = "ic_service_hair_color"

    // ── 5. Navigation Icons ───────────────────────────────────────────
    const val NAV_HOME = "ic_nav_home"
    const val NAV_SEARCH = "ic_nav_search"
    const val NAV_BOOKING = "ic_nav_booking"
    const val NAV_FAVORITE = "ic_nav_favorite"
    const val NAV_PROFILE = "ic_nav_profile"
    const val NAV_NOTIFICATION = "ic_nav_notification"
    const val NAV_AI_ASSISTANT = "ic_nav_ai_assistant"
    const val NAV_BACK = "ic_nav_back"
    const val NAV_MORE = "ic_nav_more"
    const val NAV_FILTER = "ic_nav_filter"

    // ── 6. AI Assets (use only in AI-related sections) ──────────────
    const val AI_ORB = "ic_ai_orb"
    const val AI_WAVE = "ic_ai_wave"
    const val AI_SPARKLE = "ic_ai_sparkle"

    // ── 7. Specialist Avatars ────────────────────────────────────────
    const val AVATAR_01 = "avatar_01"
    const val AVATAR_02 = "avatar_02"
    const val AVATAR_03 = "avatar_03"
    const val AVATAR_04 = "avatar_04"
    const val AVATAR_PLACEHOLDER = "avatar_placeholder"

    // ── 8. Salon Images ───────────────────────────────────────────────
    const val SALON_01 = "salon_01"
    const val SALON_02 = "salon_02"
    const val SALON_03 = "salon_03"
    const val SALON_04 = "salon_04"
    const val SALON_PLACEHOLDER = "salon_placeholder"

    // ── 9. Product Assets (marketplace/product sections only) ───────
    const val PRODUCT_01 = "product_01"
    const val PRODUCT_02 = "product_02"
    const val PRODUCT_03 = "product_03"
    const val PRODUCT_PLACEHOLDER = "product_placeholder"

    // ── 10. Status Assets (empty/loading/success/error states only) ─
    const val STATUS_EMPTY = "empty_state"
    const val STATUS_LOADING = "loading_state"
    const val STATUS_NO_BOOKING = "no_booking"
    const val STATUS_NO_NOTIFICATION = "no_notification"
    const val STATUS_SUCCESS = "success"
    const val STATUS_ERROR = "error"

    // ── 11. Decorative Assets ─────────────────────────────────────────
    const val DECOR_GLASS_NOISE = "glass_noise"
    const val DECOR_LIGHT_GLOW = "light_glow"
    const val DECOR_PURPLE_BLOOM = "purple_bloom"
    const val DECOR_ROSE_GOLD_GLOW = "rose_gold_glow"
    const val DECOR_GLASS_REFLECTION = "glass_reflection"

    // ── 12. Authentication Assets (no current screen consumer yet) ──
    const val AUTH_WELCOME_HERO = "welcome_hero"
    const val AUTH_LOGIN_HERO = "login_hero"
    const val AUTH_REGISTER_HERO = "register_hero"

    // ── 13. Profile Assets ────────────────────────────────────────────
    const val PROFILE_DEFAULT_AVATAR = "default_avatar"
    const val PROFILE_VIP_BADGE = "vip_badge"
    const val PROFILE_PREMIUM_BADGE = "premium_badge"
    const val PROFILE_VERIFIED_BADGE = "verified_badge"

    // ── 14. Role Icons (added this revision — were unregistered before) ─
    const val ROLE_CUSTOMER = "ic_role_customer"
    const val ROLE_MANAGER = "ic_role_manager"
    const val ROLE_SPECIALIST = "ic_role_specialist"
}
