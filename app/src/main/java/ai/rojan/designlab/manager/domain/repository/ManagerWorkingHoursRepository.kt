package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.domain.repository.SalonWorkingHours
import ai.rojan.designlab.domain.repository.TimeInterval

/**
 * Owner-scoped Working Hours read/write (First Salon Pilot, Owner Salon
 * Profile Completion — Android-only, per
 * `ROJAN_PhaseA_Salon_Identity_Readiness_Report_v1.md` §7-8: the backend
 * `GET/PUT/DELETE /api/v1/salons/{salonId}/working-hours/{dayOfWeek}`
 * endpoints already exist and work, the only gap was Android write UI).
 * Reuses [SalonWorkingHours]/[TimeInterval] (the same domain types
 * [ai.rojan.designlab.domain.repository.WorkingHoursRepository] already
 * returns for Customer-side reads) rather than introducing a second,
 * overlapping shape — same "reuse, don't duplicate" precedent
 * [ManagerSalonRepository] already establishes for [ai.rojan.designlab.manager.domain.dashboard.ManagerSalonSummary].
 */
interface ManagerWorkingHoursRepository {

    suspend fun getWorkingHours(salonId: String): Result<List<SalonWorkingHours>>

    suspend fun setWorkingHours(
        salonId: String,
        dayOfWeek: String,
        intervals: List<TimeInterval>,
    ): Result<SalonWorkingHours>

    suspend fun removeWorkingHours(salonId: String, dayOfWeek: String): Result<Unit>
}
