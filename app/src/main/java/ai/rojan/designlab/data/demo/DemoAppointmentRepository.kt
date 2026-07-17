package ai.rojan.designlab.data.demo

object DemoAppointmentRepository {
    val appointments: List<DemoAppointment> = listOf(
        DemoAppointment("appt_01", "سالن رویا", "کوتاهی و استایل مو", "سارا احمدی", "امروز، ۱۶:۰۰", "16:00", AppointmentStatus.UPCOMING, 690_000, relatedServiceId = "service_01", specialistId = "specialist_01", dateKey = "today"),
        DemoAppointment("appt_02", "سالن بهار", "پاکسازی پوست صورت", "نگین رضایی", "فردا، ۱۰:۳۰", "10:30", AppointmentStatus.UPCOMING, 1_200_000, relatedServiceId = "service_02", specialistId = "specialist_02", dateKey = "tomorrow"),
        DemoAppointment("appt_03", "استودیو luxe", "آرایش مجلسی", "مریم کریمی", "۲۸ خرداد", "18:00", AppointmentStatus.COMPLETED, 1_500_000, relatedServiceId = "service_03", daysAgo = 17),
        DemoAppointment("appt_04", "گلدن تاچ", "کاشت و طراحی ناخن", "الناز موسوی", "۱۵ خرداد", "12:00", AppointmentStatus.COMPLETED, 950_000, relatedServiceId = "service_04", daysAgo = 30),
        DemoAppointment("appt_05", "سالن رویا", "رنگ مو", "سارا احمدی", "۳ خرداد", "14:00", AppointmentStatus.CANCELLED, 850_000, relatedServiceId = null, daysAgo = 42),
    )

    fun findById(id: String): DemoAppointment? = appointments.find { it.id == id }
    fun upcoming(): List<DemoAppointment> = appointments.filter { it.status == AppointmentStatus.UPCOMING }
    fun past(): List<DemoAppointment> = appointments.filter { it.status != AppointmentStatus.UPCOMING }
}
