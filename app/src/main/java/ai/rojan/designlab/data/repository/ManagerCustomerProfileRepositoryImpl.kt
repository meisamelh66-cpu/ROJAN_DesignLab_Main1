package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.CustomerApi
import ai.rojan.designlab.data.remote.dto.CustomerNoteDto
import ai.rojan.designlab.data.remote.dto.CustomerRecordDto
import ai.rojan.designlab.data.remote.dto.CustomerTimelineEntryDto
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.ManagerCustomerProfileRepository
import ai.rojan.designlab.manager.domain.customer.CustomerServiceHistoryEntry
import ai.rojan.designlab.manager.domain.customer.ManagerCustomerProfile

class ManagerCustomerProfileRepositoryImpl(
    private val customerApi: CustomerApi,
) : ManagerCustomerProfileRepository {

    override suspend fun loadProfileByAccountId(
        salonId: String,
        accountId: String,
    ): Result<ManagerCustomerProfile?> = safeApiCall {
        val recordId = resolveRecordId(salonId, accountId) ?: return@safeApiCall null

        val detail = customerApi.get(salonId, recordId)
        val notes = customerApi.notes(salonId, recordId)
        val tags = customerApi.tags(salonId, recordId).map { it.label }
        val timeline = customerApi.timeline(salonId, recordId).content
        val visitCount = customerApi.bookings(salonId, recordId).totalElements

        ManagerCustomerProfile(
            fullName = detail.fullName,
            phone = detail.phoneNumber.orEmpty(),
            statusLabel = statusLabel(detail, tags),
            totalVisits = visitCount.toInt(),
            history = timeline.filter { it.type.startsWith("BOOKING") }.map { it.toHistoryEntry() },
            notes = notes.toJoinedText(),
        )
    }

    /**
     * The Customers list navigates with a linked account's `UserId`, but the
     * CRM detail endpoints are keyed on the salon's `CustomerId` and there is
     * no `?userId=` filter - so page through `/customer-records` (backend
     * caps a page at 100) matching on [CustomerRecordDto.userId].
     */
    private suspend fun resolveRecordId(salonId: String, accountId: String): String? {
        var page = 0
        while (true) {
            val response = customerApi.list(salonId, page = page, size = PAGE_SIZE)
            response.content.firstOrNull { it.userId == accountId }?.let { return it.id }
            page++
            if (page >= response.totalPages) return null
        }
    }

    private fun statusLabel(record: CustomerRecordDto, tags: List<String>): String {
        val status = when (record.status.uppercase()) {
            "LEAD" -> "سرنخ"
            "PROSPECT" -> "مشتری بالقوه"
            "ACTIVE" -> "مشتری فعال"
            "VIP" -> "VIP"
            "INACTIVE" -> "غیرفعال"
            "CHURNED" -> "از دست‌رفته"
            else -> record.status
        }
        return if (tags.isEmpty()) status else (listOf(status) + tags).joinToString(" · ")
    }

    private fun CustomerTimelineEntryDto.toHistoryEntry() = CustomerServiceHistoryEntry(
        date = occurredAt.substringBefore('T'),
        service = description,
        specialist = "",
        price = "",
    )

    private fun List<CustomerNoteDto>.toJoinedText(): String? =
        takeIf { it.isNotEmpty() }
            ?.sortedByDescending { it.createdAt }
            ?.joinToString("\n\n") { it.text }

    private companion object {
        const val PAGE_SIZE = 100
    }
}
