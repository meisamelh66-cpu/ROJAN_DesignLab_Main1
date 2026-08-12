package ai.rojan.designlab.manager.data

import ai.rojan.designlab.data.remote.ManagerCustomerApi
import ai.rojan.designlab.data.remote.dto.CreateCustomerRequestDto
import ai.rojan.designlab.data.remote.dto.CustomerResponseDto
import ai.rojan.designlab.data.remote.dto.NetworkCustomerStatus
import ai.rojan.designlab.data.remote.dto.UpdateCustomerRequestDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.manager.domain.customer.CustomerNote
import ai.rojan.designlab.manager.domain.customer.CustomerServiceHistoryEntry
import ai.rojan.designlab.manager.domain.customer.CustomerTag
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import ai.rojan.designlab.manager.domain.repository.CustomerRepository
import ai.rojan.designlab.manager.domain.repository.ServiceRepository
import ai.rojan.designlab.manager.domain.repository.SpecialistRepository
import java.time.LocalDateTime

/**
 * Real backend-backed [CustomerRepository] (Phase 2, M2) — replaces
 * [InMemoryCustomerRepository]'s hardcoded roster with the real,
 * owner-authenticated `CustomerController` (`ROJAN_Backend`).
 *
 * `getAll()`/`getById()`/`search()` read an in-memory cache populated by
 * [sync] (same "cache populated by an explicit suspend sync, read
 * synchronously" shape as [BackendServiceRepository]/
 * [BackendAppointmentRepository]) — sourced from the bulk
 * `GET .../customers` listing only, which does **not** include per-
 * customer visit history (fetching that per row would be an N+1 call
 * this codebase's Phase 1 "keep list screens lightweight" rule
 * explicitly forbids). [ManagerCustomer.lastVisit]/[ManagerCustomer.totalVisits]/
 * [ManagerCustomer.notes] are placeholders until [loadDetail] runs for
 * that one customer - a single profile view is not an N+1 case, exactly
 * the same distinction Phase 1 drew everywhere else.
 *
 * [ManagerCustomer.loyaltyScore] has no backend equivalent (the real
 * `lifetimeValue` field is a currency total, not a 0-100 score) and is
 * always `0` here rather than a fabricated derivation - no screen
 * currently renders it, so this is an honest gap, not a visible
 * regression.
 *
 * `update()` also writes [ManagerCustomer.tag] back through
 * [CustomerTag.toNetworkStatus] — the DTO supports it and there's no
 * reason to silently drop a tag change a caller explicitly made.
 *
 * Booking dates are formatted Gregorian, with Persian digits - this
 * codebase has no Jalali/Persian-calendar conversion utility (see
 * [ai.rojan.designlab.domain.booking.RollingBookingDates]'s own doc
 * comment for the same disclosed simplification on the Customer side).
 *
 * CRM Foundation, Phase 6 Step 5: [loadDetail] also now keeps every note
 * `GET .../customers/{id}/notes` returns (not just the latest one used
 * for [ManagerCustomer.notes]), exposed via [getNoteHistory] - no extra
 * network call, the full list was already being fetched and discarded.
 * Read-only, same as everything else this class exposes past `create`/
 * `update` - the backend has no note-creation endpoint.
 */
class BackendCustomerRepository(
    private val managerCustomerApi: ManagerCustomerApi,
    private val serviceRepository: ServiceRepository,
    private val specialistRepository: SpecialistRepository,
    private val salonId: String,
) : CustomerRepository {

    private var cache: List<ManagerCustomer> = emptyList()
    private val historyCache = mutableMapOf<String, List<CustomerServiceHistoryEntry>>()
    private val notesHistoryCache = mutableMapOf<String, List<CustomerNote>>()

    /** Fetches this salon's customers from the backend and repopulates the cache. Call before first read, and to refresh. */
    suspend fun sync(): Result<Unit> = safeApiCall {
        managerCustomerApi.list(salonId, page = 0, size = 200)
    }.map { paged ->
        cache = paged.content.map { it.toDomain() }
    }

    override fun getAll(): List<ManagerCustomer> = cache

    override fun getById(id: String): ManagerCustomer? = cache.find { it.id == id }

    override fun search(query: String): List<ManagerCustomer> =
        if (query.isBlank()) {
            cache
        } else {
            cache.filter { it.name.contains(query, ignoreCase = true) || it.phone.contains(query) }
        }

    override suspend fun create(customer: ManagerCustomer): Result<ManagerCustomer> =
        safeApiCall {
            managerCustomerApi.create(
                salonId = salonId,
                request = CreateCustomerRequestDto(
                    fullName = customer.name,
                    phoneNumber = customer.phone.ifBlank { null },
                ),
            )
        }.map { dto ->
            dto.toDomain().also { created -> cache = cache + created }
        }

    override suspend fun update(customer: ManagerCustomer): Result<ManagerCustomer?> =
        safeApiCall {
            managerCustomerApi.update(
                salonId = salonId,
                customerId = customer.id,
                request = UpdateCustomerRequestDto(
                    fullName = customer.name,
                    phoneNumber = customer.phone.ifBlank { null },
                    status = customer.tag.toNetworkStatus(),
                ),
            )
        }.map { dto ->
            dto.toDomain().also { updated ->
                cache = cache.map { if (it.id == updated.id) updated else it }
            }
        }

    override fun getServiceHistory(customerId: String): List<CustomerServiceHistoryEntry> =
        historyCache[customerId] ?: emptyList()

    override fun getNoteHistory(customerId: String): List<CustomerNote> =
        notesHistoryCache[customerId] ?: emptyList()

    override suspend fun loadDetail(customerId: String): Result<Unit> =
        safeApiCall {
            val bookings = managerCustomerApi.bookings(salonId, customerId, page = 0, size = 20, sortDirection = "DESC")
            val notes = managerCustomerApi.notes(salonId, customerId)
            bookings to notes
        }.map { (bookings, notes) ->
            historyCache[customerId] = bookings.content.map { booking ->
                val service = serviceRepository.getById(booking.serviceId)
                val specialist = specialistRepository.getById(booking.specialistId)
                CustomerServiceHistoryEntry(
                    date = formatVisitDate(booking.startTime),
                    service = service?.name ?: "—",
                    specialist = specialist?.name ?: "—",
                    price = service?.let { formatTomanPrice(it.price) } ?: "—",
                )
            }

            val lastVisit = bookings.content.firstOrNull()?.startTime?.let(::formatVisitDate) ?: "—"
            val sortedNotes = notes.sortedByDescending { it.createdAt }
            val latestNote = sortedNotes.firstOrNull()?.text

            notesHistoryCache[customerId] = sortedNotes.map { dto ->
                CustomerNote(id = dto.id, text = dto.text, createdAt = formatVisitDate(dto.createdAt))
            }

            cache = cache.map { existing ->
                if (existing.id == customerId) {
                    existing.copy(
                        lastVisit = lastVisit,
                        totalVisits = bookings.totalElements.toInt(),
                        notes = latestNote,
                    )
                } else {
                    existing
                }
            }
        }

    private fun formatVisitDate(isoStartTime: String): String {
        // startTime is an ISO-8601 *local* date-time with no offset, same shape BackendAppointmentRepository parses.
        val date = LocalDateTime.parse(isoStartTime)
        return "%04d/%02d/%02d".format(date.year, date.monthValue, date.dayOfMonth).toPersianDigits()
    }

    private fun CustomerResponseDto.toDomain() = ManagerCustomer(
        id = id,
        name = fullName,
        phone = phoneNumber.orEmpty(),
        tag = status.toDomainTag(),
        loyaltyScore = 0,
        notes = null,
        lastVisit = "—",
        totalVisits = 0,
    )

    /** 6 backend statuses -> 4 domain tags — see [CustomerResponseDto]'s doc comment on why these are richer than the mobile app's own model. */
    private fun NetworkCustomerStatus.toDomainTag(): CustomerTag = when (this) {
        NetworkCustomerStatus.VIP -> CustomerTag.VIP
        NetworkCustomerStatus.INACTIVE, NetworkCustomerStatus.CHURNED -> CustomerTag.INACTIVE
        NetworkCustomerStatus.LEAD, NetworkCustomerStatus.PROSPECT -> CustomerTag.NEW
        NetworkCustomerStatus.ACTIVE -> CustomerTag.REGULAR
    }

    /** Inverse of [toDomainTag] for writes — lossy (4 -> 6), picks the nearest real status rather than inventing a new one. */
    private fun CustomerTag.toNetworkStatus(): NetworkCustomerStatus = when (this) {
        CustomerTag.VIP -> NetworkCustomerStatus.VIP
        CustomerTag.INACTIVE -> NetworkCustomerStatus.INACTIVE
        CustomerTag.NEW -> NetworkCustomerStatus.LEAD
        CustomerTag.REGULAR -> NetworkCustomerStatus.ACTIVE
    }
}
