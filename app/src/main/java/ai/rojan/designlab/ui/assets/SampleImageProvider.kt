package ai.rojan.designlab.ui.assets

import androidx.annotation.DrawableRes

import ai.rojan.designlab.R

/**
 * Production Asset Normalization — the single source every image "card"
 * slot in the app (salon, specialist, service, product, gallery, and any
 * future image card) resolves its picture through, instead of each demo
 * repository/screen referencing a `R.drawable.*` directly. Every
 * category function below currently returns [defaultSampleImage] — the
 * one production-quality sample asset registered so far.
 *
 * This is deliberately the single seam real photography gets wired
 * through later: when a real, category-specific photo is supplied, only
 * that one function's return value changes here. No call site anywhere
 * in the app needs to change, since none of them hold a `R.drawable.*`
 * reference directly anymore — they all go through this object.
 *
 * [forProduct]/[forGallery] have no current UI consumer (no
 * marketplace/gallery screens exist in this app yet), but are included
 * per this pass's explicit requirement to keep every listed category —
 * "any future image card" included — ready before its screen exists.
 */
object SampleImageProvider {

    /**
     * The one real asset file backing every category today —
     * `default_sample_image.png` (square, flat single-color fill, no
     * gradient, with a simple neutral "photo placeholder" glyph). Not a
     * final branded asset; a clearly-temporary stand-in until real
     * photography replaces it category by category.
     */
    @DrawableRes
    val defaultSampleImage: Int = R.drawable.default_sample_image

    /**
     * Customer Journey UI Alignment: real per-salon demo photos —
     * `salon_demo_1..4.png`, one per seeded [ai.rojan.designlab.data.demo.DemoSalon]
     * (`salon_01`..`salon_04`) — so the same salon shows the same photo
     * everywhere it appears (list, details, dashboard, favorites, search,
     * booking confirmation, appointments). Falls back to
     * [defaultSampleImage] for any id outside the 4 seeded demo salons.
     */
    private val salonImages: Map<String, Int> = mapOf(
        "salon_01" to R.drawable.salon_demo_1,
        "salon_02" to R.drawable.salon_demo_2,
        "salon_03" to R.drawable.salon_demo_3,
        "salon_04" to R.drawable.salon_demo_4,
    )

    @DrawableRes
    fun forSalon(salonId: String): Int = salonImages[salonId] ?: defaultSampleImage

    /**
     * Temporary Specialist Avatar pass: every specialist across the app
     * now shows this one real photo (`specialist_female_01.png`) instead
     * of [defaultSampleImage] — a deliberate stand-in, same "one seam"
     * pattern as [forSalon]: when real per-specialist backend photos
     * exist, only this line changes.
     */
    @DrawableRes
    fun forSpecialist(specialistId: String): Int = R.drawable.specialist_female_01

    /**
     * Real per-category service icons — one shipped `ic_service_*` asset
     * per [ai.rojan.designlab.data.demo.DemoService.categoryLabel],
     * replacing the flat [defaultSampleImage] placeholder every service
     * card used to show regardless of category. Categories without a
     * dedicated shipped asset reuse the closest available one instead of
     * falling back to the placeholder: اپیلاسیون and لیزر are both
     * hair-removal services and share [R.drawable.ic_service_laser];
     * ابرو (microblading) reuses [R.drawable.ic_service_eyelash], the
     * closest shipped eye-area asset.
     */
    private val serviceCategoryImages: Map<String, Int> = mapOf(
        "مو" to R.drawable.ic_service_hair,
        "پوست" to R.drawable.ic_service_skin,
        "آرایش" to R.drawable.ic_service_makeup,
        "ناخن" to R.drawable.ic_service_nails,
        "اپیلاسیون" to R.drawable.ic_service_laser,
        "ماساژ" to R.drawable.ic_service_spa,
        "ابرو" to R.drawable.ic_service_eyelash,
        "لیزر" to R.drawable.ic_service_laser,
    )

    @DrawableRes
    fun forService(serviceId: String, categoryLabel: String? = null): Int =
        serviceCategoryImages[categoryLabel] ?: defaultSampleImage

    @DrawableRes
    fun forProduct(productId: String): Int = defaultSampleImage

    @DrawableRes
    fun forGallery(id: String): Int = defaultSampleImage
}
