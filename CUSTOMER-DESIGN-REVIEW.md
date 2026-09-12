# ROJAN Customer App — Visual Design Review

**Date:** 2026-09-09 · **Method:** device-first. Reviewed the **real rendered app** on a physical **Samsung Galaxy A72 (SM-A725F), Android 14**, logged in as a real account ("گیتا"). Source code consulted only to confirm token values, never as the primary source of truth.
**Scope:** VISUAL REVIEW ONLY. No source, Compose component, design-system, color, or icon was modified. No redesign implemented. No commit.
**Evidence:** `docs/design-review/customer/` (screenshots captured this session).
**Related internal work (not re-litigated here):** `docs/uiux/ROJAN_UIUX_PRO_MAX_AUDIT_v1.md`, `ROJAN_PREMIUM_UI_POLISH_ROADMAP_v1.md`, and 4 "UI Polish Sprint" reports already exist. This review is device-observed and complements them; where it agrees, it sharpens; where it goes further, it says so.

---

## 0. Verdict up front

The suspicion in the brief is **largely correct**. The app is **not amateur in engineering** — layout is stable, RTL is real, the glass mechanic is technically well-built, nothing is broken. But the **visual language reads as "playful / decorative / young"**, not "premium beauty-tech". The problem is **not one bad screen** — it is **five system-level decisions repeated on every surface**:

1. A **glowing gold/rose-gold border** on every card.
2. **✦ sparkle glints** in the corners of every glass surface (a first-class design token — `RojanTokens.kt` even names a color "reserved for corner sparkle points").
3. **Everything is a large glass card** — including settings rows and empty states.
4. **32dp card radius + 50dp pills + 100dp circular "orb" buttons** — the whole UI is soft-cornered.
5. A **full-saturation pink / lavender / magenta / gold** palette with no neutral rest state, plus an **AI-generated candy-pink salon photograph as the loading screen**.

Individually defensible. Stacked on every screen, they read as **a rewards screen in a mobile game**, not a booking tool a 35-year-old salon client trusts with her money.

**Childishness risk: 68 / 100** (see §6).

---

## 1. Screenshot inventory & per-screen status

| # | Screen / state | Captured | Status | One-line |
|---|---|---|---|---|
| 1 | Splash (system) | `S01_splash.png` | **PROBLEM** | Pure-white system splash → hard cut to dark navy app. 3D-glossy pink "woman + hair" app icon. |
| 2 | App loading / session restore | `S05_home_authed_top.png` | **PROBLEM** | Full-bleed **AI-generated candy-pink salon interior photo** with centered text. |
| 3 | Login (phone entry) | `S02_login_phone.png` | **PROBLEM (mild)** | Single field + one giant gradient pill on a near-empty screen; 🌸 emoji as the only brand mark. |
| 4 | OTP + optional name | `S03_otp.png` | **PROBLEM (mild)** | Two stacked fields + "resend" link + gradient pill; low contrast; code field pre-filled with the phone number (also logged in the smoke test). |
| 5 | Customer Home — guest | `S04_home_guest.png` | **PROBLEM** | "خدمات محبوب" section is 3 identical "به‌زودی" (coming soon) placeholder cards; generic greeting "سلام کاربر جان". |
| 6 | Customer Home — authenticated | `S04b_home_authed.png`, `S05b/c/d` | **PROBLEM** | Real name shown ("سلام گیتا جان") — good. But the feed is placeholder cards ("خدمات محبوب", "فعالیت اخیر"), each a huge glowing card; no section hierarchy. |
| 7 | Search | `S05_search_authed.png`, `S20_auth_required_state.png` | **PARTIAL PASS** | Clean field + "نتایج (N)"; the sign-in-required state is a proper error card with a CTA — better than the app average. |
| 8 | Salon list | `S06_salon_list.png`, `S06b_home_salon_cards.png`, `S06c/d/e` | **PROBLEM** | 2-column grid of storefront-icon cards; real data ("ROJAN AI Pilot Salon", "بانوصبا") but the card is 90% glass + border + icon, name/address cramped at the bottom. |
| 9 | Salon detail | `S07_salon_detail.png`, `S07b_salon_detail_full.png` | **PARTIAL PASS** | Best-composed screen in the app: real name, description, address, phone, working hours, specialists. Still over-decorated (orb back button, sparkles, gold border on the info card). |
| 10 | Specialist selection | — | **NOT TESTED** (could not reach via device navigation; shares the same card/border/sparkle components) |
| 11 | Service selection | — | **NOT TESTED** (same) |
| 12 | Booking — date | — | **NOT TESTED** (same) |
| 13 | Booking — time | — | **NOT TESTED** (same) |
| 14 | Booking — confirmation | — | **NOT TESTED** (same; smoke-test confirmed the flow's data loads, not its visuals) |
| 15 | Booking — success | — | **NOT TESTED** |
| 16 | Bottom navigation | visible in `S04b`, `S05*`, `S07b` | **PROBLEM** | 5-item bar; the center "Home" is an oversized filled purple circle bulging above the bar with its own gold ring — competes with everything. |
| 17 | Profile | `S14_profile.png` | **PROBLEM** | Oversized flat avatar; every row is a glowing sparkle card. |
| 18 | Account / settings menu | `S14b_profile_scrolled.png` | **PROBLEM (worst offender)** | A settings list rendered as ~7 stacked hero cards (Beauty DNA, Appointments, Followed, Favorites, Wallet…), ~120dp each, ~4 visible per screen. |
| 19 | Empty state (appointments) | `S17_empty_appointments.png`, `S13b_nav_appointments.png` | **PROBLEM** | Empty state is *also* a giant sparkle card; icon + both text lines near-invisible (low contrast); **no CTA** ("go back to home" as text only). |
| 20 | Loading indicators (in-screen) | not isolated | **NOT TESTED** (backend was fast; no skeleton observed. The one loading state seen is the full-screen AI photo — item 2.) |
| 21 | Error state | `S20_auth_required_state.png` | **PARTIAL PASS** | The "sign in required" card is clear and has a CTA — the app's best state. |
| 22 | Auth-required / guarded route | `S20_auth_required_state.png`, `S02_login_phone.png` | **PASS (behaviour)** | Guarded tabs cleanly redirect a guest to login; no dead-ends. |

**NOT TESTED total: 6 interior screens** (specialist, service, date, time, confirmation, success) + isolated in-screen loading/skeleton. Design-language findings below apply to them by inheritance (the components are shared and identical everywhere observed), but their **specific composition is unverified**.

---

## 2. Dimension-by-dimension review (rendered app)

### Visual hierarchy — **PROBLEM**
On Home, Profile and the Account list, **every item has the same visual weight**: same glass fill, same glowing border, same sparkles, same size. The eye has nowhere to land. A settings row ("کیف پول" / Wallet) is styled exactly like a primary content card. On Home there is **no single "book an appointment" primary action** visible above the fold — the screen opens on a search bar and two "coming soon" cards.
→ *Reduction filter: 60–70% of the decorative layers can be removed with zero loss of meaning.*

### Typography — **PROBLEM**
`Type.kt` exposes **6 weights (Light 300 → ExtraBold 800)**. On screen, headings, card titles and section titles are all "big + bold white", differentiated mainly by size, and the size steps are small. Body is `16sp / 26sp` (1.6× leading) — loose for dense UI. There is no quiet, secondary, or caption tier that actually recedes; "secondary" text is just a slightly dimmer rose.

### Persian typography — **PARTIAL PASS**
RTL is genuinely implemented (alignment, mirrored layout, Persian digits in places). `Type.kt` has Persian-tuned line metrics — good. **Problems:** (a) mixed numerals — Persian digits for the clock/dates, Latin for the phone number `+989164987585` and counts "نتایج (0)"; (b) the phone number renders LTR inside an RTL card, creating a jagged edge; (c) emoji (🌸) used as a brand element on the login screen is not typography-grade.

### Spacing & rhythm — **PROBLEM**
Section-title → card gap and card → card gap are **both large and roughly equal** (~24–32dp), so nothing groups. On Profile/Account you see ~4 items per screen for what is a flat menu. Empty states place one card at the top and leave 55–65% of the viewport as dead navy.

### Alignment & grid — **PARTIAL PASS**
Cards are edge-aligned to a consistent screen margin; the grid holds. Minor: the floating circular "orb" back button sits outside the content grid, top-left, breaking the left margin on every detail screen.

### Color balance & contrast — **PROBLEM**
- **No neutral.** The palette (`RojanRose`, `RojanBlushPink`, `RojanSoftLavender`, `RojanVividMagenta`, `RojanPearlPink`, gold) is applied at full chroma on a saturated navy-purple ground. There is no calm grey/neutral surface anywhere for the eye to rest.
- **Gold everywhere.** `RojanPremiumBorderGold #D4AF37` + `RojanPremiumBorderRoseGold #E0A67A` glow on every card. Gold used this liberally stops signalling "premium" and starts signalling "trophy / casino / kids' reward".
- **Contrast failures:** the appointments empty-state headline ("هنوز نوبتی ندارید"), placeholder body text, and the empty-state icon are all a muted rose/lavender on translucent glass — they fail WCAG AA against the card and are hard to read on the device in daylight. Placeholder "به‌زودی" text on Home is similarly faint.

### Cards — **PROBLEM (systemic)**
The card is the app's only container. Content card, nav row, empty state, error state, info panel — all the same `PremiumGlassSurface` + `PremiumMetallicBorder` + corner sparkles at 32dp radius. When everything is a hero card, nothing is.

### Buttons — **PROBLEM**
One button style dominates: a **full-width magenta→pink gradient pill** (50dp radius). It appears as the only CTA on login, OTP, the error card, and the salon promo. It's loud, it's identical everywhere, and there is no visible secondary/tertiary button tier, so the UI can't express "this is the main action, that is optional".

### Inputs — **PARTIAL PASS**
Fields are legible, RTL-correct, with a glass fill + gold border. They're over-styled for a text field (glow, sparkles) but functional. Focus state was not clearly visible on device.

### Icons & icon consistency — **PROBLEM**
- Mixed metaphors: **storefront** icon for salons, **leaf** for "Beauty DNA", **bell** for "followed salons" (should be a follow/store/RSS metaphor — a bell means notifications), **wallet**, **heart**, **calendar**, **clock**. The clock is reused as the generic "coming soon" placeholder icon, which then collides with its real meaning (time).
- Weight/fill inconsistency: the profile **avatar** is a filled white person glyph; the Personal-Info **row icon** is a purple line person glyph — same concept, two renderings, adjacent on screen.
- Sizes: bottom-nav center "home" glyph is oversized inside an oversized circle; placeholder icons are ~40–48dp and very faint.

### Corner radii — **PROBLEM**
`RojanRadius.Card = 32.dp`, `Pill = 50.dp`, `Circle = 100.dp`, `Small = 16.dp`. **Nothing in the app is crisp.** 32dp on a full-width card is a large, soft, "friendly" corner; combined with pills and circular orb buttons, the entire silhouette of every screen is rounded blobs. Premium/mature product UIs typically sit at **12–20dp** for cards and reserve pills for chips/tags only.

### Borders — **PROBLEM**
Every card has a 1–1.5px **glowing** gold/rose-gold gradient stroke (`PremiumMetallicBorder`). A glow implies "active / selected / important". When it's on *everything*, it's noise, and it removes the app's ability to actually show a selected state.

### Shadows — **PARTIAL PASS**
The 3-tier shadow scale (`SoftElevation` 8.4 / `FloatingElevation` 18.9 / `PremiumElevation` 25.2 dp) is a reasonable system. On device the shadows are mostly subsumed by the border glow, so they read fine — but they're doing redundant work alongside the glow.

### Glass effects — **PROBLEM (over-applied)**
The `PremiumGlassSurface` mechanic itself is well-built. The issue is **coverage**: ~90% of pixels on Home/Profile are translucent glass over a busy gradient+glow background, which lowers legibility and flattens hierarchy. Glass should be an accent (1–2 elements per screen), not the substrate.

### Gradients — **PROBLEM**
Background gradient + AI-glow radial + per-card glass gradient + gold border gradient + magenta CTA gradient = **5 gradient systems visible simultaneously** on the login and salon-promo screens. The eye can't find a flat reference.

### Bottom navigation — **PROBLEM**
5 items; the center is a large filled purple disc protruding above the bar with a gold ring. It reads as a floating action button, not a nav tab, and it's the loudest thing in the viewport at all times. The other 4 icons are small line glyphs by comparison — inconsistent weight within one component.

### Top bars — **PROBLEM**
No real app bar. Detail screens have a **floating circular glass "orb" back button** (`GlassOrb`) top-left with its own gold ring + sparkle, and a big right-aligned white title below it. The orb is decorative, breaks the margin, and gives no room for actions (favorite, share) which then get scattered into the content.

### Images / illustrations — **PROBLEM**
The only real imagery is (a) the **AI-generated candy-pink salon** loading photo and (b) a similar AI salon photo inside the "رزرو نوبت زیبایی" promo card. Both have the tell-tale over-lit, over-saturated, too-perfect AI-render quality. On a premium product this reads as stock/placeholder, not brand photography.

### Loading states — **PROBLEM / NOT FULLY TESTED**
The one loading state observed is the full-screen AI photo + "در حال بازیابی نشست شما". No content skeletons were seen (backend was fast). `PremiumLoadingBar` / `RojanShimmer` exist in code but weren't triggered on device.

### Empty states — **PROBLEM**
Rendered as a giant sparkle card; low-contrast icon + text; **no action button** (appointments empty state tells the user to "go back to the home page" instead of offering "پیدا کردن سالن").

### Error states — **PARTIAL PASS**
The auth-required card (`S20`) is the app's **best** state: clear icon, clear message, one obvious CTA. It should be the model, not the exception.

### Selected / disabled states — **NOT CLEARLY OBSERVED**
Could not exercise a selection (specialist/service/time) on device. **Risk:** because every card already has a glowing gold border, there is no visual headroom left to express "this one is selected" — a real design-system problem to verify in the booking flow.

### Touch targets — **PARTIAL PASS**
Nav and CTAs are large. Consistent with the smoke test, guarded routes behave well. (Code audit separately flagged sub-48dp icon targets; not re-verified visually here.)

### Visual density & whitespace — **PROBLEM**
Density is **too low**: giant cards + large equal gaps mean 3–4 items per screen and a lot of scrolling for simple lists (Account menu). Simultaneously the *decorative* density is **too high**: sparkles, glow, gradients, orbs. The app manages to feel both empty and noisy.

---

## 3. Premium vs. childish — concrete causes

Ranked by contribution to the "young / decorative / not-premium" read:

| Cause | Where | Why it reads childish |
|---|---|---|
| **✦ corner sparkles on every glass surface** | every card, every screen | Sparkles/glints are a "magic / delight / reward" motif. On a settings row they say "this is a fun toy", not "this is your account". No mature product decorates its containers. |
| **Full-screen AI candy-pink salon photo as the loading screen** | session restore, promo card | Over-saturated pink/lilac AI renders are the visual signature of low-effort beauty content. It's the single most damaging frame in the app. |
| **Glowing gold border on every card** | `PremiumMetallicBorder` everywhere | Gold glow = trophy / casino / mobile-game rarity indicator. Liberal gold cheapens; sparing gold elevates. |
| **32dp+ radii + pills + circular orb buttons** | `RojanRadius` | A fully blob-cornered UI reads soft and juvenile. Mature = tighter geometry (12–20dp), rectangles allowed. |
| **Everything is a hero card** | Profile, Account list, empty states | Turning a flat menu into a stack of glowing cards is the "kids' app" pattern. Menus should be quiet lists. |
| **Magenta→pink gradient pill as the universal button** | login, OTP, errors, promo | One loud gradient button for everything = no hierarchy, and hot-pink gradients skew young. |
| **Oversized protruding nav "home" disc** | bottom nav | An oversized glowing center button is a consumer-game convention (think casual games), not a professional tool. |
| **🌸 emoji as brand mark** | login/OTP header | Emoji in chrome = informal. A wordmark or monogram is expected. |
| **No neutral surface anywhere** | whole app | Full-chroma pink/lavender/gold with nowhere for the eye to rest signals "decorative theme" over "product". |
| **White system splash → dark app flash** | cold start | An unthemed splash is an "unfinished app" tell. |
| **Placeholder "به‌زودی" cards on Home** | Home feed | Shipping visible "coming soon" cards reads as a demo, not a product. |
| **Decorative floating orb back button** | every detail screen | A back button with its own ring + glow + sparkle is decoration masquerading as navigation. |

**What is NOT a problem** (don't "fix" these):
- The dark navy canvas itself — a dark theme is a legitimate premium choice.
- The glass mechanic as an engine — it's well-built; it just needs to be rationed.
- RTL / Persian-first — genuinely done, keep it.
- The salon-detail information architecture — the right fields in a sensible order.
- Feminine direction — feminine is correct for this audience; feminine ≠ childish.

---

## 4. User perspective — the major problems

### 4.1 "Am I in the right app?" — the loading screen
- **What the user sees:** a full-screen, glossy, pink-and-lavender AI image of a fantasy salon, with a thin gold progress line.
- **What the user feels:** "This looks like an ad / a wallpaper / a beauty influencer's story." Mild embarrassment to open it in public.
- **Why it reduces quality:** the first sustained image of the brand is a stock-feeling AI render. It sets the ceiling for how seriously everything after it is taken.
- **What should change:** replace with a calm branded state — dark navy ground, the ROJAN wordmark/monogram centered, a single quiet progress indicator. No photograph.

### 4.2 "Everything is shouting" — hierarchy
- **What the user sees:** on Home and Account, a scroll of near-identical glowing cards with sparkles.
- **What the user feels:** decision fatigue; can't tell what matters; "where do I book?"
- **Why it reduces quality:** a premium product guides the eye. This one distributes attention evenly, which reads as "the designer couldn't decide".
- **What should change:** one primary surface per screen (elevated), everything else demoted to quiet rows/list items. Remove sparkles and border-glow from non-primary elements.

### 4.3 "This is a settings menu, why does it look like a game?" — Account list
- **What the user sees:** Wallet, Appointments, Followed Salons etc. each as a ~120dp glowing sparkle card; 4 fit on screen.
- **What the user feels:** "Overdesigned. I have to scroll a lot for a simple menu."
- **Why it reduces quality:** effort spent decorating a utility list is effort a mature user reads as misplaced priorities.
- **What should change:** a plain grouped list — 56–64dp rows, leading icon, title, chevron, hairline divider. No card, no border, no sparkle.

### 4.4 "I can't read the empty state" — contrast
- **What the user sees:** appointments screen: a faint icon and two lines of low-contrast rose text on a glass card, then a large empty area.
- **What the user feels:** "Is it loading? Is it broken? What do I do now?"
- **Why it reduces quality:** the moment the user needs guidance most (nothing to show), the app is least legible and offers no action.
- **What should change:** high-contrast text (near-white headline, light-grey body), a real illustration or a simple monoline icon, and a primary CTA ("پیدا کردن سالن" → salon list).

### 4.5 "The button is the same everywhere" — CTA hierarchy
- **What the user sees:** the same hot-pink gradient pill on login, OTP, errors, and promos.
- **What the user feels:** nothing stands out; the important action and the optional action look identical.
- **Why it reduces quality:** no hierarchy of intent = the user has to read every button carefully.
- **What should change:** a 3-tier button system — primary (solid, restrained), secondary (outline/tonal), tertiary (text). Reserve any gradient for exactly one primary CTA per screen, max.

---

## 5. Scores (0–100)

| Dimension | Score | Note |
|---|---:|---|
| Overall visual quality | **52** | Competent execution of an over-decorated system. |
| Premium perception | **41** | Gold-glow + sparkles + AI pink photo actively work against "premium". |
| Professionalism | **48** | Reads as a talented solo effort / early startup, not a shipped consumer product. |
| Visual hierarchy | **38** | Everything weighs the same. |
| Typography | **55** | RTL done well; too many weights, weak secondary tier, loose leading. |
| Iconography | **50** | One family, but mixed metaphors (bell=follow), fill/weight inconsistencies, oversized nav glyph. |
| Spacing | **47** | Loose, ungrouped rhythm; low content density; large dead zones. |
| Component consistency | **62** | *Very* consistent — but consistently over-styled. The engine is sound. |
| Color system | **44** | No neutral, gold overused, contrast failures on key states. |
| Navigation | **58** | Behaviour is solid (smoke test); the bottom bar's protruding disc and floating orb back button undermine it visually. |
| UX clarity | **56** | Guarded-route handling is good; Home lacks an obvious primary action; empty states lack CTAs. |
| **Childishness risk** | **68 / 100** | Strongly skews playful/decorative. Driven by sparkles, gold glow, AI pink imagery, blob geometry, hero-card menus, protruding nav disc. |

---

## 6. Issues (full list)

> Format per brief. Severity: **S1** = undermines trust/premium perception app-wide · **S2** = notable per-screen quality loss · **S3** = polish.

---

**Issue ID:** VIS-01
**Screen:** App loading / session restore (`S05_home_authed_top.png`); also the salon promo card
**Severity:** S1
**Category:** Imagery / brand
**Current visual:** Full-bleed AI-generated candy-pink/lavender salon interior photograph filling 100% of the viewport, centered Persian text, thin gold progress line.
**User perception:** "Looks like an ad or a wallpaper." Not a serious product.
**Why it is a problem:** It is the first *sustained* brand image. Over-saturated AI salon renders are the visual cliché of low-effort beauty content and cap perceived quality for everything after.
**Recommended visual direction:** Calm branded loading — dark navy ground (existing `HomeBackgroundTheme`), centered ROJAN wordmark/monogram, one quiet indeterminate progress line in a single accent. No photography anywhere in system chrome.
**Before:** full-screen AI photo + text.
**After:** navy ground · centered monogram (≤96dp) · 2px progress line · caption at 40% opacity.

---

**Issue ID:** VIS-02
**Screen:** Every card, every screen (`PremiumMetallicBorder`)
**Severity:** S1
**Category:** Borders / decoration
**Current visual:** 1–1.5px glowing gold (`#D4AF37`) / rose-gold (`#E0A67A`) gradient stroke on every glass surface.
**User perception:** "Trophy / reward / game rarity." Cheapens rather than elevates.
**Why it is a problem:** A glow reads as "active/important". Applied to everything it becomes noise and removes the app's ability to show real emphasis or a selected state.
**Recommended visual direction:** Remove the glowing border from ~90% of surfaces. Default card = flat fill + a 1px hairline at ~8% white, no glow. Reserve the metallic border for **one** hero element per screen (e.g. the salon promo card), and even there make it static, not glowing.
**Before:** `border = premiumMetallicBorder(glow = true)` on all cards.
**After:** `border = hairline(color = white @ 8%)` default; metallic border on ≤1 element/screen.

---

**Issue ID:** VIS-03
**Screen:** Every glass surface (`RojanTokens.kt` names a color "reserved for corner sparkle points")
**Severity:** S1
**Category:** Decoration
**Current visual:** 2–4 white-gold ✦ glints in the corners of every card, sheet, orb, empty state.
**User perception:** "Cute / magical." Undermines seriousness on utility screens.
**Why it is a problem:** Sparkle motifs signal delight/reward. On a settings row, an OTP field, or an empty state they are pure noise and the strongest single "childish" signal.
**Recommended visual direction:** **Delete corner sparkles globally.** If a moment of delight is wanted, confine it to exactly one place — the booking-success screen — as a brief animation, not a static decoration on containers.
**Before:** sparkles on ~100% of surfaces.
**After:** 0 sparkles in steady-state UI; optional 1 celebratory moment on success only.

---

**Issue ID:** VIS-04
**Screen:** `RojanRadius` — app-wide geometry
**Severity:** S1
**Category:** Corner radii / shape
**Current visual:** `Card = 32.dp`, `Pill = 50.dp`, `Circle = 100.dp`, `Small = 16.dp`. Every surface is soft/blob-cornered; buttons are pills; back buttons are circular orbs.
**User perception:** "Soft, friendly, young."
**Why it is a problem:** Blob geometry across the whole silhouette is a juvenile tell. Mature product UIs use tighter corners and allow rectangles.
**Recommended visual direction:** `Card 16.dp` (from 32), `Button 12.dp` (from 50 pill), keep `Pill 50.dp` only for chips/tags/status, replace circular orb buttons with a standard 40dp back affordance at `8.dp`.
**Before:** Card 32 / Button 50 (pill) / back = 100dp circle.
**After:** Card 16 / Button 12 / back = 40dp square @ 8dp radius.

---

**Issue ID:** VIS-05
**Screen:** Account / settings list (`S14b_profile_scrolled.png`) — **worst offender**
**Severity:** S1
**Category:** Component pattern / hierarchy
**Current visual:** ~7 menu items, each a full-width glass hero card (~110–130dp) with gold glow + sparkles + right-aligned icon + title + subtitle. ~4 visible per screen.
**User perception:** "Overdesigned; too much scrolling for a menu."
**Why it is a problem:** A flat navigation list rendered as trophy cards is a canonical "kids' app" pattern and wastes 60%+ of vertical space.
**Recommended visual direction:** Grouped plain list. 56dp rows, leading 20dp line icon, title (`Body`, near-white), optional trailing value, chevron, 1px divider at 8% white. Section labels as small caps grey with 8dp to first row.
**Before:** 7 × ~120dp glowing cards.
**After:** 7 × 56dp list rows in 2–3 labelled groups; whole menu visible in ~1.3 screens.

---

**Issue ID:** VIS-06
**Screen:** Bottom navigation (visible in most screenshots)
**Severity:** S2
**Category:** Navigation component
**Current visual:** 5 items; center "Home" is an oversized filled purple disc protruding above the bar with a gold ring; the other 4 are small line glyphs.
**User perception:** "The middle button is a special action." Confusing; also very loud.
**Why it is a problem:** Inconsistent icon weight within one component; an oversized glowing center disc is a casual-game convention; it dominates the viewport permanently.
**Recommended visual direction:** 5 equal-weight tabs, 24dp icons, active tab = accent color + label, inactive = 60% grey, no labels or all labels (consistent). Flat bar, 1px top hairline, no protruding disc.
**Before:** center disc ~64dp protruding + gold ring; 4 × ~20dp glyphs.
**After:** 5 × 24dp icons on one baseline; active state via color + small indicator dot.

---

**Issue ID:** VIS-07
**Screen:** All detail screens — floating orb back button (`GlassOrb`)
**Severity:** S2
**Category:** Top bar / navigation
**Current visual:** Circular glass orb top-left with gold ring + sparkle, floating over content, breaking the left margin.
**User perception:** Decorative; not obviously "back".
**Why it is a problem:** Navigation should be the quietest reliable thing on screen. This one is decorated and misaligned, and leaves no room for screen-level actions (favorite/share) which then scatter into the body.
**Recommended visual direction:** A minimal top app bar: 40dp back chevron aligned to the content margin (right side, RTL), screen title centered or leading, action icons trailing. No orb, no ring, no sparkle.
**Before:** floating 100dp glass orb, off-grid.
**After:** 56dp app bar, back chevron on the grid, room for 2 actions.

---

**Issue ID:** VIS-08
**Screen:** Empty states — appointments (`S17`), and by inheritance favorites/followed/waitlist
**Severity:** S2
**Category:** Empty state / contrast / UX
**Current visual:** Giant sparkle card; faint ~44dp icon; headline "هنوز نوبتی ندارید" and body in low-contrast muted rose on glass; **no button**; 55–65% of screen empty.
**User perception:** "Is it broken? Loading? What now?"
**Why it is a problem:** Fails contrast on the exact text the user needs; gives no next action; the container styling implies "content" where there is none.
**Recommended visual direction:** No card — content sits directly on the background. Monoline 48dp icon at full accent; headline `Title` near-white; one line of light-grey body; **primary button** "پیدا کردن سالن" → salon list. Vertically centered in the available space.
**Before:** low-contrast text on a sparkle card, no CTA.
**After:** high-contrast centered stack + 1 primary CTA.

---

**Issue ID:** VIS-09
**Screen:** Buttons — app-wide
**Severity:** S2
**Category:** Buttons / hierarchy
**Current visual:** One style: full-width magenta→pink gradient pill (50dp). Used for login, OTP submit, error-card CTA, promo CTA — no secondary/tertiary tier visible.
**User perception:** Everything is equally urgent; hot-pink skews young.
**Why it is a problem:** No way to express "main vs optional"; the gradient is loud and consumer-playful.
**Recommended visual direction:** 3 tiers — **Primary:** solid accent (a deep, slightly desaturated plum or a single restrained magenta — *not* a gradient), 12dp radius, 52dp height. **Secondary:** transparent + 1px accent border. **Tertiary:** text only. Max one Primary per screen. If a gradient is kept at all, it's for a single marketing CTA (the salon promo), nowhere else.
**Before:** gradient pill everywhere.
**After:** solid Primary / outline Secondary / text Tertiary; gradient retired from transactional screens.

---

**Issue ID:** VIS-10
**Screen:** Color system — app-wide (`RojanTokens.kt`)
**Severity:** S1
**Category:** Color
**Current visual:** Full-chroma pink/lavender/magenta/gold on saturated navy-purple; no neutral surface; gold on every border; key text (empty/placeholder) below AA.
**User perception:** "A theme, not a product." Decorative first, functional second.
**Why it is a problem:** No visual rest; overused accent colors stop being accents; contrast failures on real content.
**Recommended visual direction:** Keep the dark navy identity. Add a **true neutral ramp** (5–6 cool greys) for surfaces, dividers, secondary text. Demote pink/magenta to **one** accent used sparingly (CTAs, active states, links). Demote gold to a **rare** flourish (rating stars, one hero border) — remove it from default card borders. Fix text tokens so headline ≥ 7:1 and body ≥ 4.5:1 on every surface they appear on.
**Before:** ~5 accent families visible per screen, no neutral.
**After:** 1 dark neutral ground + grey ramp + 1 accent + gold as rare flourish.

---

**Issue ID:** VIS-11
**Screen:** Splash (`S01_splash.png`)
**Severity:** S2
**Category:** Launch experience / brand
**Current visual:** White system splash, then a hard cut to the dark navy app. App icon = 3D-glossy rose "R" made of hair + a woman's face silhouette on a purple gradient.
**User perception:** "Unfinished." The icon skews "beauty influencer".
**Why it is a problem:** An unthemed splash + a flash-of-white is a classic "not shipped yet" tell. A literal, glossy, 3D icon dates the product.
**Recommended visual direction:** Android 12+ themed `SplashScreen` — dark navy `windowSplashScreenBackground`, a flat monoline monogram, seamless dissolve into the app (no color flash). Separately (out of scope for this review, flag only): consider a flatter, more abstract app icon.
**Before:** white splash → dark app; 3D glossy icon.
**After:** navy themed splash, no flash; flat monogram.

---

**Issue ID:** VIS-12
**Screen:** Home — guest & authenticated (`S04`, `S04b`, `S05b/c/d`)
**Severity:** S2
**Category:** Content / hierarchy
**Current visual:** Feed of large glowing cards; "خدمات محبوب" and "فعالیت اخیر" are hardcoded "به‌زودی" placeholders; no visible primary "book" action above the fold; generic guest greeting.
**User perception:** "This is a demo." No clear starting point.
**Why it is a problem:** Shipping visible "coming soon" cards and opening on placeholders rather than a primary action is the opposite of premium.
**Recommended visual direction:** Home opens on: a compact greeting, a prominent **"رزرو نوبت" primary CTA** (or a search-first hero), then **only sections that have real data** (nearby salons, your upcoming appointment, followed salons). Remove every placeholder section from the shipping build. One elevated section max; the rest are quiet horizontal rails with real section titles.
*(Wiring/removing sections is a functional change — flagging for the build agent.)*
**Before:** placeholder cards, no primary action.
**After:** greeting + primary CTA + real-data-only rails.

---

**Issue ID:** VIS-13
**Screen:** Salon list cards (`S06b`, `S06c`, salon grid)
**Severity:** S2
**Category:** Cards / content ratio
**Current visual:** 2-up grid; each card is a big glass rectangle mostly filled by a centered storefront icon, with the salon name + address cramped in a strip at the bottom.
**User perception:** "Placeholder." No sense of the actual salon.
**Why it is a problem:** The card's visual budget goes to decoration (glass, border, sparkle, giant icon) instead of information (photo, name, rating, distance, next availability).
**Recommended visual direction:** Content-first card: 16:9 real salon photo (or a tasteful monogram fallback) top, then name (`Body` semibold), then a metadata row (rating · distance · "امروز آزاد"). 12dp radius, hairline border, no sparkle. Single-column list, not 2-up, so each card can carry real info.
**Before:** icon-dominated glass card, name at the bottom.
**After:** photo + name + metadata row, single column.

---

**Issue ID:** VIS-14
**Screen:** Typography system (`Type.kt`)
**Severity:** S2
**Category:** Typography
**Current visual:** 6 weights (300–800); headline/card-title/section-title all "big bold white", small size steps; body `16/26` (loose); secondary text = dim rose only.
**User perception:** Flat hierarchy; hard to skim.
**Why it is a problem:** Too many weights + weak size contrast + no genuine recede tier = the type can't do hierarchy on its own, forcing the decoration to do it.
**Recommended visual direction:** 3 weights (Regular 400 / Medium 500 / Bold 700). A clear scale: Display 28/34 Bold · Title 20/28 Bold · Section 13/18 Medium caps grey · Body 15/24 Regular · Caption 13/18 Regular grey. Secondary text = a real grey token, not tinted rose.
**Before:** 6 weights, ~4sp steps, rose "secondary".
**After:** 3 weights, clear scale, grey secondary.

---

**Issue ID:** VIS-15
**Screen:** Iconography (Profile/Account, nav, placeholders)
**Severity:** S3
**Category:** Icons
**Current visual:** Mixed metaphors (bell = "followed salons"); fill vs line inconsistency (avatar filled, row icon line); clock icon doubles as the "coming soon" placeholder; oversized nav center glyph.
**User perception:** Slightly untidy; "followed = bell" is briefly confusing.
**Why it is a problem:** Metaphor mismatches cost a beat of comprehension; fill/line mixing on adjacent icons looks unpolished.
**Recommended visual direction:** One line-icon set, single 1.5px weight, single 20dp size in lists / 24dp in nav. "Followed salons" → a store or user-plus glyph. Retire the clock as a placeholder (use a neutral box). Avatar = same person glyph family as rows.
**Before:** mixed metaphors + fill/line mix.
**After:** one line set, consistent weight/size, correct metaphors.

---

## 7. TOP 10 visual problems — concrete BEFORE → AFTER

| # | Problem | Before (measured / observed) | After (recommended) | Reason |
|---|---|---|---|---|
| 1 | Corner sparkles everywhere (VIS-03) | ✦ glints on ~100% of surfaces | **0** in steady-state UI; 1 optional celebratory moment on booking success only | Removes the #1 "childish" signal; instantly reads more mature |
| 2 | Glowing gold border on every card (VIS-02) | `premiumMetallicBorder` (glowing) on all cards | Default card border = **1px white @ 8%**, no glow; metallic border on **≤1** hero element per screen | Restores the ability to show emphasis; kills the "trophy" read |
| 3 | AI pink salon photo as loading screen (VIS-01) | full-bleed AI render + text | navy ground + centered monogram (≤96dp) + **2px** progress line | The most damaging brand frame → a calm branded one |
| 4 | Card radius (VIS-04) | **32dp** | **16dp** | Tighter geometry reads premium; 32dp reads "friendly app" |
| 5 | Button radius / style (VIS-09) | **50dp pill**, magenta→pink **gradient**, used for every CTA | **12dp**, **solid** restrained accent, 3-tier system (primary/secondary/text), max 1 primary/screen | Hot-pink gradient pills skew young; solid + tiers = hierarchy |
| 6 | Account menu as hero cards (VIS-05) | 7 × **~120dp** glowing sparkle cards, ~4/screen | 7 × **56dp** plain list rows, grouped, hairline dividers, whole menu in ~1.3 screens | A menu is a list, not a trophy shelf |
| 7 | Bottom-nav center disc (VIS-06) | oversized protruding **~64dp** filled purple disc + gold ring; other icons **~20dp** | 5 × **24dp** equal-weight icons on one baseline; active = accent + indicator dot | Removes a permanent loud element; consistent within the component |
| 8 | Floating orb back button (VIS-07) | **100dp** `GlassOrb`, off-grid, ring + sparkle | **56dp** top app bar, **40dp** back chevron on the content margin, room for 2 actions | Navigation should be the quietest reliable thing on screen |
| 9 | Empty-state contrast + no CTA (VIS-08) | low-contrast rose text on a sparkle card; **no button**; ~60% dead space | no card; near-white headline; grey body; **1 primary CTA** ("پیدا کردن سالن"); vertically centered | The user needs guidance most here and currently gets the least |
| 10 | No neutral in the color system (VIS-10) | ~5 accent families/screen, gold on every border, key text < AA | 1 dark neutral ground + **5–6 cool greys** for surfaces/dividers/secondary text; **1** accent used sparingly; gold = rare flourish; fix text tokens to ≥ 4.5:1 body / ≥ 7:1 headline | Gives the eye somewhere to rest; makes accents mean something again |

---

## 8. Recommended design direction

Move the existing UI toward: **"Quiet luxury / premium beauty-tech, dark, editorial, Persian-first."**

| Keep | Change | Add |
|---|---|---|
| Dark navy/plum canvas as the brand ground | Retire sparkles, border-glow, orb buttons, gradient pills, AI photography | A true neutral grey ramp for surfaces/dividers/secondary text |
| RTL / Persian-first, Persian line metrics | Tighten radii (32→16 cards, 50→12 buttons) | A 3-tier button system and a 3-weight type scale |
| The glass mechanic — as a **rare accent** (1–2 elements/screen) | Demote pink/magenta to a single sparing accent; demote gold to a rare flourish | Real salon/brand photography with a consistent grade (or none) |
| Salon-detail information architecture | Turn menus and empty states into quiet lists/centered stacks | One elevated "primary surface" convention per screen |
| The auth-required error card (use it as the model) | Consistent numerals (Persian throughout, or a documented rule) | A themed Android 12+ splash |

**Adjectives to design against:** elegant, restrained, editorial, confident, calm, trustworthy, feminine-but-serious, expensive.
**Adjectives to design away from:** magical, sparkly, sugary, glowing, rewarding, playful, bubbly.

Feminine stays. The audience is women booking beauty services and spending real money — the product should feel like a high-end salon's own brand (think a refined spa identity: deep tones, generous whitespace, one metal accent, excellent type), **not** like a beauty game.

---

## 9. Reference screen

### Selected: **Salon Detail** (`S07_salon_detail.png` / `S07b`)

**Why this screen:**
1. **It's the pivot of the whole product.** Every booking passes through it; it's where a user decides to trust a salon with her time and money. If it feels premium, the booking feels premium.
2. **It already has the best information architecture in the app** — real name, description, address, phone, working hours, specialists, in a sensible order. The bones are right; only the skin is wrong. That makes it the fastest screen to prove the new direction on.
3. **It exercises every component** that needs fixing: app bar (currently an orb), info card (currently glowing), a list (specialists), a primary CTA (book), section headers, and it will host the entry to the booking flow. Getting it right sets the pattern for specialist/service/date/time/confirmation.
4. **It's content-rich**, so it will honestly show whether the restrained direction still feels "premium beauty" and not "austere/corporate" — a real test, unlike a sparse screen.

Home is more-visited but is a composition problem tangled with unfinished features (placeholder sections). Salon Detail is a **pure design problem** on **real data** — the correct place to anchor.

### Proposed redesign of Salon Detail (visual only — not implemented)

**Layout (top → bottom):**
1. **Top app bar (56dp):** back chevron on the content margin (right, RTL) · salon name centered, `Title` (20/28 Bold), truncating · trailing: favorite (heart outline) + share. Flat, `background` color, 1px bottom hairline on scroll. *(Replaces the floating orb.)*
2. **Hero (ratio 16:9):** real salon photo, full-bleed to screen edges, subtle bottom gradient scrim (navy, 0→60%) so text is readable. If no photo: a flat monogram tile in `surfaceVariant`, no glass, no sparkle. Rating chip bottom-leading over the scrim (`★ 4.8 · 132`), small, solid `surface` @ 80%.
3. **Identity block (24dp screen margin, 16dp below hero):**
   - Salon name — `Display` (28/34 Bold), near-white (`onBackground`).
   - One-line descriptor — `Body` (15/24), `onSurfaceVariant` grey.
   - Metadata row, 8dp gap: `📍 Tehran, Pilot Address` · `·` · `⏱ باز تا ۱۸:۰۰` (open-until, computed from working hours) — all `Caption` grey, icons 16dp line.
4. **Primary CTA (sticky bottom, or inline after identity):** full-width **solid** accent button, `Body` semibold, 52dp, 12dp radius — **"رزرو نوبت"**. This is the only Primary on the screen. No gradient, no glow.
5. **Section — خدمات (Services):** section label `Section` (13/18 Medium, caps, grey), 8dp to content. Horizontal rail of service chips OR a short vertical list: service name (`Body`) · duration + price (`Caption` grey) · chevron. Row height 56dp, 1px divider at 8% white. "مشاهده همه" text-tertiary link if truncated.
6. **Section — متخصصان (Specialists):** same section label. Horizontal rail of specialist cards: 64dp circular avatar (real photo / initials fallback in `surfaceVariant`), name (`Caption`, 2 lines max), 12dp gap between. No card border, no glass — just the avatar + name.
7. **Section — ساعات کاری (Working hours):** a plain 7-row table (day · hours), `Body`/`Caption`, today's row emphasized with a 3px leading accent bar and `onBackground` text; other rows grey. No card.
8. **Section — درباره (About) / تماس:** plain text block + a tappable phone row (`📞 +98 911 405 0112`, LTR-isolated inside an RTL row with proper `unicodeBidi`).
9. Bottom padding = height of the sticky CTA + 16dp.

**Spacing & rhythm:**
- Screen margin: 20dp (from ~16dp).
- Section-label → content: **8dp**. Content block → next section-label: **28dp**. (Asymmetric on purpose — a label belongs to what's below it.)
- Within a list: rows flush, dividers do the separating; no inter-row gap.

**Typography:** 3 weights only (Regular/Medium/Bold). Scale as in VIS-14. Numerals: Persian throughout except the E.164 phone, which stays Latin and is explicitly bidi-isolated.

**Color:**
- Ground: existing dark navy (`HomeBackgroundTheme`).
- Surfaces: a new `surface` / `surfaceVariant` cool-grey-navy pair, **no glass, no border-glow** for standard content. Glass is used **once** — optionally on the rating chip over the hero — and even there without sparkle.
- Accent: one — used on the Primary CTA, today's working-hours bar, and links. No magenta gradient anywhere.
- Gold: **not used** on this screen (or a single hairline on the hero rating chip at most).
- Text: `onBackground` near-white for name/headlines (≥ 7:1); `onSurfaceVariant` mid-grey for body/meta (≥ 4.5:1).

**Components:**
- App bar (new, shared) · content card = flat `surface` @ 16dp, hairline border · list row (56dp, icon + text + chevron + divider) · section label · specialist avatar+name (no container) · primary button (solid, 12dp) · rating chip.
- **Removed from this screen:** `GlassOrb`, `PremiumMetallicBorder` (glowing), corner sparkles, gradient pill.

**Icons:** one 1.5px line set, 16dp inline / 20dp in rows / 24dp in the app bar. Metaphors: `arrow-right` (back, RTL), `heart` (favorite), `share`, `map-pin`, `clock`, `phone`, `scissors`/`sparkle-off` for services, `user` for specialists.

**States:**
- **Loading:** content skeletons matching this layout (hero block, 3 text bars, 2 list-row bars) in `surfaceVariant` with a subtle shimmer — **not** the full-screen AI photo.
- **No services / no specialists:** inline centered stack (20dp icon + one grey line), no card.
- **Favorite toggled:** heart fills accent + a 150ms scale tick + a brief snackbar "به علاقه‌مندی‌ها اضافه شد". (Snackbar is a new shared component — flag for the build agent.)
- **Selected service/specialist (when it feeds the booking flow):** row background → `surfaceVariant`, 3px leading accent bar, check glyph trailing. This is now *possible* because the default border glow is gone.
- **Offline / load error:** the existing auth-error card pattern (icon + message + retry), reused.
- **Booking CTA pressed:** button shows an inline spinner + disables (the ViewModels already expose `isSubmitting` — verified in the smoke test).

**Annotated evidence:** the current-state screenshot is `docs/design-review/customer/S07_salon_detail.png` / `S07b_salon_detail_full.png`. (The design-audit skill does not generate annotated overlays; annotations are described inline above against those files.)

---

## 10. Output classification

### ✅ PASS (keep as-is)
- RTL / Persian-first layout and line metrics.
- Guarded-route behaviour — guests are cleanly redirected to login, no dead-ends (`S20`, `S02`).
- The auth-required **error card** pattern — clear icon, message, one CTA (`S20`). Use as the model.
- Salon-detail **information architecture** (right fields, sensible order).
- The glass **engine** itself (`PremiumGlassSurface`) — technically sound; needs rationing, not rebuilding.
- Layout stability, no clipping, no overflow, portrait-locked — solid.

### ⚠️ PROBLEM (device-observed, evidence attached)
- VIS-01 … VIS-15 above. System-level: sparkles (VIS-03), border-glow (VIS-02), blob geometry (VIS-04), no neutral / contrast failures (VIS-10), AI pink loading imagery (VIS-01), hero-card menus (VIS-05), gradient-pill-everywhere (VIS-09). Per-screen: nav disc (VIS-06), orb back button (VIS-07), empty states (VIS-08), Home placeholders (VIS-12), salon cards (VIS-13), splash (VIS-11), typography (VIS-14), icons (VIS-15).
- **Childishness risk 68/100** — the brief's suspicion is confirmed.

### 🎯 RECOMMENDATION
- Adopt the direction in §8 ("quiet luxury / dark editorial / Persian-first").
- Execute the TOP 10 before→after changes in §7 as the first pass (all are token/style-level, no feature change).
- Prove it on the **reference screen (Salon Detail, §9)** first; get human approval of that single screen before touching anything else.
- Two changes need a build agent (functional): removing/wiring Home placeholder sections (VIS-12), and adding a shared snackbar component (used by VIS-08 and Salon Detail states).

### 🚫 NOT TESTED (visually — must be captured on device before the next phase)
- Specialist selection, Service selection, Booking date, Booking time, Booking confirmation, Booking success (6 screens).
- Isolated in-screen loading / skeleton states.
- Focus states on inputs; selected/disabled states in the booking flow.
- Tablet / landscape (app is portrait-locked by design — N/A).

---

## Appendix — measured facts from the design system (confirmation only)

```
RojanRadius:   Small 16dp · Card 32dp · Pill 50dp · Circle 100dp
Border:        RojanPremiumBorderGold #D4AF37 · RojanPremiumBorderRoseGold #E0A67A  (glowing, applied app-wide via PremiumMetallicBorder)
Sparkle:       RojanTokens.kt — a near-white warm-gold specular color "reserved for corner sparkle points and tight reflection lines"
Palette:       RojanRose · RojanBlushPink · RojanSoftLavender · RojanVividMagenta · RojanPearlPink · RojanRatingGold #FFB020  (no neutral grey ramp)
Type:          6 weights (Light 300 → ExtraBold 800); Body 16sp / lineHeight 26sp
Shadow:        SoftElevation 8.4dp · FloatingElevation 18.9dp · PremiumElevation 25.2dp
Components:    GlassOrb (circular decorative button), PremiumGlassSurface, PremiumMetallicBorder, HeroBookingCard, RojanScaffold
Manifest:      no themed SplashScreen (white system splash observed on device)
```

Device: Samsung SM-A725F · Android 14 · captured 2026-09-09, account "گیتا", live backend `api.rojanai.ir`.
