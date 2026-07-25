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

    @DrawableRes
    fun forSalon(salonId: String): Int = defaultSampleImage

    @DrawableRes
    fun forSpecialist(specialistId: String): Int = defaultSampleImage

    @DrawableRes
    fun forService(serviceId: String): Int = defaultSampleImage

    @DrawableRes
    fun forProduct(productId: String): Int = defaultSampleImage

    @DrawableRes
    fun forGallery(id: String): Int = defaultSampleImage
}
