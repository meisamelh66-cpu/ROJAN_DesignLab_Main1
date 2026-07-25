package ai.rojan.designlab.data.demo

import ai.rojan.designlab.ui.assets.SampleImageProvider
import ai.rojan.designlab.ui.theme.RojanAquaMint
import ai.rojan.designlab.ui.theme.RojanBlushPink
import ai.rojan.designlab.ui.theme.RojanPearlPink
import ai.rojan.designlab.ui.theme.RojanSoftLavender

/**
 * Centralized demo salon data. UI consumes this, never hardcodes salon
 * data directly — per this phase's explicit "create centralized demo
 * repositories" requirement.
 */
object DemoSalonRepository {

    val salons: List<DemoSalon> = listOf(
        DemoSalon(
            id = "salon_01",
            name = "سالن رویا",
            tagline = "زیبایی و آرامش در قلب شهر",
            rating = "4.9",
            reviewCount = 214,
            address = "تهران، ونک، خیابان ملاصدرا، پلاک ۱۲",
            distanceKm = "1.2",
            workingHours = "شنبه تا پنجشنبه: ۹:۰۰ تا ۲۱:۰۰",
            phone = "021-8877-4455",
            facilities = listOf("پارکینگ اختصاصی", "اتاق VIP", "پذیرایی رایگان", "دسترسی معلولین"),
            colorSeed = RojanSoftLavender,
            assetRes = SampleImageProvider.forSalon("salon_01"),
        ),
        DemoSalon(
            id = "salon_02",
            name = "استودیو luxe",
            tagline = "مو و آرایش حرفه‌ای",
            rating = "4.8",
            reviewCount = 176,
            address = "تهران، نیاوران، بلوار آصف، پلاک ۴۵",
            distanceKm = "2.4",
            workingHours = "شنبه تا پنجشنبه: ۱۰:۰۰ تا ۲۰:۰۰",
            phone = "021-2266-7788",
            facilities = listOf("پارکینگ اختصاصی", "کافی‌شاپ داخلی", "وای‌فای رایگان"),
            colorSeed = RojanAquaMint,
            assetRes = SampleImageProvider.forSalon("salon_02"),
        ),
        DemoSalon(
            id = "salon_03",
            name = "سالن بهار",
            tagline = "پوست و اسپا",
            rating = "4.7",
            reviewCount = 98,
            address = "تهران، سعادت‌آباد، خیابان سرو غربی، پلاک ۸",
            distanceKm = "3.1",
            workingHours = "شنبه تا چهارشنبه: ۹:۳۰ تا ۱۹:۳۰",
            phone = "021-2244-9911",
            facilities = listOf("سونا و جکوزی", "اتاق VIP", "پذیرایی رایگان"),
            colorSeed = RojanBlushPink,
            assetRes = SampleImageProvider.forSalon("salon_03"),
        ),
        DemoSalon(
            id = "salon_04",
            name = "گلدن تاچ",
            tagline = "ناخن و زیبایی",
            rating = "4.9",
            reviewCount = 152,
            address = "تهران، جردن، خیابان نلسون ماندلا، پلاک ۲۳",
            distanceKm = "4.0",
            workingHours = "همه روزه: ۱۰:۰۰ تا ۲۲:۰۰",
            phone = "021-8899-1122",
            facilities = listOf("پارکینگ اختصاصی", "وای‌فای رایگان", "دسترسی معلولین"),
            colorSeed = RojanPearlPink,
            assetRes = SampleImageProvider.forSalon("salon_04"),
        ),
    )

    fun findById(id: String): DemoSalon? = salons.find { it.id == id }
}
