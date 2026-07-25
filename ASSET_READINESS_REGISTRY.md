# ROJAN AI — Asset Readiness Registry v1.0

**Status:** Asset structure preparation only. No screens redesigned, no icons added visually, no UI architecture changed. Every component listed as "pattern implemented" renders **exactly as it did before this change** — verified by construction (the new code path only activates when a real asset resource ID is provided, which is `null` everywhere today).

---

## How This Works

Each data model below gained one optional field: `@DrawableRes val assetRes: Int? = null`. When `null` (true for every entry today, since no real asset files exist), the component renders its current Material Icon fallback — unchanged. When a real asset is delivered and this field is pointed at `R.drawable.<name>`, the component automatically renders the real image instead — **one line changed, nothing else.**

Target filenames are centralized in `ui/assets/RojanAssetNames.kt` — one source of truth, so a real asset just needs to be saved under that exact name in `res/drawable` (or a density-qualified folder) to be ready for wiring.

---

## Category Status

| # | Category | Target Component(s) | Code-Level Pattern | Status |
|---|---|---|---|---|
| 1 | Master Background | `PremiumBackground.kt` | N/A — already real | ✅ **Implemented, real asset** (`bg_master_luxury_salon.webp`, unchanged, permanent per instruction) |
| 2 | Brand (Logo, Splash Logo, AI Mark) | *(no current consumer — Welcome/Splash use text wordmark today, not an image)* | Not applicable yet | 📋 Registered in `RojanAssetNames`, no component to wire until a logo-image component exists |
| 3 | Hero Illustrations | `HeroBookingCard.kt` and similar future hero surfaces | Not yet applied | 📋 Registered, recommend same optional-asset pattern when next touched |
| 4 | Service 3D Icons | `ServiceCategories.kt` | ✅ **Implemented** | Ready — 5 of 10 taxonomy icons have a current consumer (`SERVICE_HAIR/SKIN/MAKEUP/NAILS/SPA`); remaining 5 (massage/laser/eyebrow/eyelash/hair color) registered for whenever a consuming screen exists |
| 5 | Navigation Icons | `CustomerBottomBar.kt`, `AISearchBar.kt`, `HomeHeader.kt` | ✅ **Implemented in `CustomerBottomBar`** | `HomeHeader`/`AISearchBar` not yet updated — same pattern recommended, not done this pass |
| 6 | AI Assets | `AISearchBar.kt`, `RecommendedServices.kt` | Not yet applied | 📋 Registered — usage-boundary rule ("AI sections only") noted for whoever applies the pattern next |
| 7 | Specialist Avatars | `TopSpecialists.kt` | ✅ **Implemented** | Ready — includes circular clipping for real photos |
| 8 | Salon Images | `FeaturedSalons.kt` | ✅ **Implemented** | `NearbySalons.kt`/`FavoriteSalons.kt` share the identical card shape — same pattern recommended, not done this pass |
| 9 | Product Assets | *(no current consumer — Marketplace screens don't exist yet)* | Not applicable yet | 📋 Registered, no component to wire |
| 10 | Status Assets | *(no current consumer — `EmptyState.kt`/`LoadingState.kt` remain unimplemented stubs)* | Not applicable yet | 📋 Registered, correctly deferred per every prior audit |
| 11 | Decorative Assets | `PremiumBackground.kt` (Atmosphere layer, currently omitted) | Not yet applied | 📋 Registered — these map to the Background System's deliberately-omitted Atmosphere layer, not a gap |
| 12 | Authentication Assets | *(no current consumer — auth screens don't exist yet, per the Product Roadmap's Phase 2)* | Not applicable yet | 📋 Registered, no component to wire |
| 13 | Profile Assets | *(no current consumer — Profile screen doesn't exist yet)* | Not applicable yet | 📋 Registered, no component to wire |

---

## Files Changed This Pass

| File | Change |
|---|---|
| `ui/assets/RojanAssetNames.kt` | **New** — central naming registry, all 13 categories, ~50 target names |
| `screens/customer/ServiceCategories.kt` | Added optional `assetRes` field + conditional render |
| `screens/customer/CustomerBottomBar.kt` | Added optional `assetRes` field + conditional render with tint preserved via `ColorFilter` |
| `screens/customer/FeaturedSalons.kt` | Added optional `assetRes` field + conditional render with `ContentScale.Crop` |
| `screens/customer/TopSpecialists.kt` | Added optional `assetRes` field + conditional render, clipped to circle |

**Verified, not assumed:** every field defaults to `null`; every fake data entry across all 4 files still omits `assetRes` entirely (uses the default), meaning **zero visual change today** — confirmed by the fact that the `if (assetRes != null)` branch is dead code until someone explicitly sets a value.

---

## Recommended Next Steps (not done this pass, explicitly out of scope)

Apply the identical pattern to: `NearbySalons.kt`, `FavoriteSalons.kt` (salon-image pattern, copy from `FeaturedSalons.kt`), `RecommendedServices.kt` (service-icon + AI-asset pattern), `RecentVisits.kt`/`UpcomingBookings.kt`/`PromotionsSection.kt` (icon pattern), `HomeHeader.kt`/`AISearchBar.kt` (nav/AI-asset pattern). All follow the exact same shape established here — this pass proved the pattern on 4 representative, distinct cases (icon set, nav bar, photo card, avatar) rather than mechanically repeating it 8 more times in one sitting.

---

**One assumption disclosed:** `androidx.annotation.DrawableRes` is used for the new fields. This comes from the `androidx.annotation:annotation` artifact, which is a near-universal transitive dependency of virtually every AndroidX library already declared in this project (Compose UI, core-ktx). Not independently verified by a real build (same standing limitation as everything else in this engagement), but this is about as safe an assumption as exists in the Android ecosystem.
