package ai.rojan.designlab.data.demo

import ai.rojan.designlab.ui.assets.SampleImageProvider
import ai.rojan.designlab.ui.theme.RojanAquaMint
import ai.rojan.designlab.ui.theme.RojanBlushPink
import ai.rojan.designlab.ui.theme.RojanPearlPink
import ai.rojan.designlab.ui.theme.RojanSoftLavender

object DemoSpecialistRepository {

    val specialists: List<DemoSpecialist> = listOf(
        DemoSpecialist(
            id = "specialist_01",
            name = "سارا احمدی",
            title = "متخصص مو و رنگ",
            bio = "بیش از ۸ سال تجربه در کوتاهی، رنگ و استایل مو با تمرکز بر تکنیک‌های روز اروپا.",
            experienceYears = 8,
            rating = "4.9",
            reviewCount = 312,
            completedAppointments = 1840,
            skills = listOf("کوتاهی مو", "رنگ مو", "بالیاژ", "کراتینه"),
            languages = listOf("فارسی", "انگلیسی"),
            salonId = "salon_01",
            colorSeed = RojanSoftLavender,
            assetRes = SampleImageProvider.forSpecialist("specialist_01"),
        ),
        DemoSpecialist(
            id = "specialist_02",
            name = "نگین رضایی",
            title = "متخصص پوست و اسپا",
            bio = "کارشناس ارشد زیبایی پوست، فعال در حوزه پاکسازی، لیزر و مراقبت‌های ضدپیری.",
            experienceYears = 6,
            rating = "4.8",
            reviewCount = 201,
            completedAppointments = 1120,
            skills = listOf("پاکسازی پوست", "لیزر", "مزوتراپی", "ماساژ صورت"),
            languages = listOf("فارسی"),
            salonId = "salon_03",
            colorSeed = RojanAquaMint,
            assetRes = SampleImageProvider.forSpecialist("specialist_02"),
        ),
        DemoSpecialist(
            id = "specialist_03",
            name = "مریم کریمی",
            title = "متخصص آرایش",
            bio = "آرایشگر حرفه‌ای عروس و مجالس، با سابقه همکاری با مجلات مد داخلی.",
            experienceYears = 10,
            rating = "5.0",
            reviewCount = 428,
            completedAppointments = 2260,
            skills = listOf("آرایش عروس", "آرایش مجلسی", "میکاپ دائم"),
            languages = listOf("فارسی", "انگلیسی", "ترکی استانبولی"),
            salonId = "salon_02",
            colorSeed = RojanBlushPink,
            assetRes = SampleImageProvider.forSpecialist("specialist_03"),
        ),
        DemoSpecialist(
            id = "specialist_04",
            name = "الناز موسوی",
            title = "متخصص ناخن",
            bio = "طراح ناخن با تخصص در ژلیش، کاشت و طراحی‌های هنری اختصاصی.",
            experienceYears = 5,
            rating = "4.7",
            reviewCount = 167,
            completedAppointments = 940,
            skills = listOf("کاشت ناخن", "ژلیش", "طراحی ناخن"),
            languages = listOf("فارسی"),
            salonId = "salon_04",
            colorSeed = RojanPearlPink,
            assetRes = SampleImageProvider.forSpecialist("specialist_04"),
        ),
    )

    fun findById(id: String): DemoSpecialist? = specialists.find { it.id == id }
    fun findBySalonId(salonId: String): List<DemoSpecialist> = specialists.filter { it.salonId == salonId }
}
