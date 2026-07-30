package ai.rojan.designlab.manager.data

import ai.rojan.designlab.manager.domain.repository.SpecialistRepository
import ai.rojan.designlab.manager.domain.specialist.Specialist

/**
 * In-memory [SpecialistRepository] — new sample roster (no prior
 * Specialist entity existed; reuses the three specialist names already
 * appearing across Calendar/customer-history sample data, so ids
 * `sp1`–`sp3` correspond to the same three people already visible
 * elsewhere in the Manager MVP).
 */
class InMemorySpecialistRepository : SpecialistRepository {

    private val specialists = mutableListOf(
        Specialist(
            id = "sp1",
            name = "سارا کریمی",
            skills = listOf("رنگ مو", "کوتاهی مو"),
            workingHours = "شنبه تا پنجشنبه، ۹ تا ۱۸",
            commissionRate = 0.30,
            active = true,
        ),
        Specialist(
            id = "sp2",
            name = "مریم رضایی",
            skills = listOf("میکاپ عروس", "میکاپ", "پاکسازی پوست"),
            workingHours = "شنبه تا چهارشنبه، ۱۰ تا ۱۹",
            commissionRate = 0.35,
            active = true,
        ),
        Specialist(
            id = "sp3",
            name = "نگار احمدی",
            skills = listOf("مانیکور", "پدیکور"),
            workingHours = "یکشنبه تا پنجشنبه، ۹ تا ۱۷",
            commissionRate = 0.25,
            active = true,
        ),
    )

    override fun getAll(): List<Specialist> = specialists.toList()

    override fun getById(id: String): Specialist? = specialists.find { it.id == id }

    override fun create(specialist: Specialist): Specialist {
        specialists.add(specialist)
        return specialist
    }

    override fun update(specialist: Specialist): Specialist? {
        val index = specialists.indexOfFirst { it.id == specialist.id }
        if (index == -1) return null
        specialists[index] = specialist
        return specialist
    }
}
