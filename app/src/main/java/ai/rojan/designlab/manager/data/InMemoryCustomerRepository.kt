package ai.rojan.designlab.manager.data

import ai.rojan.designlab.manager.domain.customer.CustomerServiceHistoryEntry
import ai.rojan.designlab.manager.domain.customer.CustomerTag
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer
import ai.rojan.designlab.manager.domain.repository.CustomerRepository

/**
 * In-memory [CustomerRepository] — same sample roster/service-history/
 * notes previously hardcoded in the now-removed screen-local
 * `ManagerCustomerSampleData.kt`, migrated here per Manager Domain
 * Foundation Phase 1. No database/backend; `loyaltyScore` values are
 * new, estimated from each customer's prior `totalVisits`/tag (no real
 * scoring logic existed anywhere to migrate).
 */
class InMemoryCustomerRepository : CustomerRepository {

    private val customers = mutableListOf(
        ManagerCustomer("c1", "سارا محمدی", "۰۹۱۲۱۲۳۴۵۶۷", CustomerTag.REGULAR, loyaltyScore = 82, notes = "ترجیح می‌دهد وقت‌های عصر رزرو کند. به رنگ‌های طبیعی علاقه دارد.", lastVisit = "امروز", totalVisits = 12),
        ManagerCustomer("c2", "نیلوفر احمدی", "۰۹۱۳۲۳۴۵۶۷۸", CustomerTag.NEW, loyaltyScore = 20, notes = null, lastVisit = "دیروز", totalVisits = 5),
        ManagerCustomer("c3", "پریسا کریمی", "۰۹۱۴۳۴۵۶۷۸۹", CustomerTag.VIP, loyaltyScore = 95, notes = "نسبت به لاک ژل حساسیت داشته، حتماً قبل از رزرو بررسی شود.", lastVisit = "۲ روز پیش", totalVisits = 8),
        ManagerCustomer("c4", "الناز حسینی", "۰۹۱۵۴۵۶۷۸۹۰", CustomerTag.NEW, loyaltyScore = 15, notes = null, lastVisit = "هفته پیش", totalVisits = 3),
        ManagerCustomer("c5", "مینا صادقی", "۰۹۱۶۵۶۷۸۹۰۱", CustomerTag.REGULAR, loyaltyScore = 90, notes = "مشتری قدیمی و همیشگی سالن، به تخفیف‌های فصلی پیام داده شود.", lastVisit = "۲ هفته پیش", totalVisits = 20),
        ManagerCustomer("c6", "شیوا رستمی", "۰۹۱۷۶۷۸۹۰۱۲", CustomerTag.NEW, loyaltyScore = 5, notes = null, lastVisit = "۳ هفته پیش", totalVisits = 1),
        ManagerCustomer("c7", "دنیا فرهادی", "۰۹۱۸۷۸۹۰۱۲۳", CustomerTag.VIP, loyaltyScore = 88, notes = null, lastVisit = "ماه پیش", totalVisits = 15),
        ManagerCustomer("c8", "آیدا مرادی", "۰۹۱۹۸۹۰۱۲۳۴", CustomerTag.REGULAR, loyaltyScore = 60, notes = null, lastVisit = "ماه پیش", totalVisits = 6),
    )

    private val serviceHistory: Map<String, List<CustomerServiceHistoryEntry>> = mapOf(
        "c1" to listOf(
            CustomerServiceHistoryEntry("۱۴۰۴/۰۵/۰۱", "رنگ مو", "سارا کریمی", "۱.۲م"),
            CustomerServiceHistoryEntry("۱۴۰۴/۰۴/۱۰", "کوتاهی مو", "سارا کریمی", "۴۵۰ت"),
            CustomerServiceHistoryEntry("۱۴۰۴/۰۳/۰۲", "میکاپ عروس", "مریم رضایی", "۲.۵م"),
        ),
        "c3" to listOf(
            CustomerServiceHistoryEntry("۱۴۰۴/۰۴/۲۸", "مانیکور", "نگار احمدی", "۳۵۰ت"),
            CustomerServiceHistoryEntry("۱۴۰۴/۰۴/۰۵", "پدیکور", "نگار احمدی", "۴۰۰ت"),
        ),
        "c5" to listOf(
            CustomerServiceHistoryEntry("۱۴۰۴/۰۵/۰۳", "پاکسازی پوست", "مریم رضایی", "۶۰۰ت"),
            CustomerServiceHistoryEntry("۱۴۰۴/۰۳/۲۰", "رنگ مو", "سارا کریمی", "۱.۲م"),
            CustomerServiceHistoryEntry("۱۴۰۴/۰۲/۱۵", "میکاپ", "نگار احمدی", "۸۰۰ت"),
        ),
    )

    /** Default history for customers not explicitly listed above — keeps the profile screen non-empty for every sample id. */
    private val defaultServiceHistory = listOf(
        CustomerServiceHistoryEntry("۱۴۰۴/۰۴/۱۵", "کوتاهی مو", "سارا کریمی", "۴۵۰ت"),
    )

    override fun getAll(): List<ManagerCustomer> = customers.toList()

    override fun getById(id: String): ManagerCustomer? = customers.find { it.id == id }

    override fun search(query: String): List<ManagerCustomer> =
        if (query.isBlank()) {
            customers.toList()
        } else {
            customers.filter { it.name.contains(query, ignoreCase = true) || it.phone.contains(query) }
        }

    override fun create(customer: ManagerCustomer): ManagerCustomer {
        customers.add(customer)
        return customer
    }

    override fun update(customer: ManagerCustomer): ManagerCustomer? {
        val index = customers.indexOfFirst { it.id == customer.id }
        if (index == -1) return null
        customers[index] = customer
        return customer
    }

    override fun getServiceHistory(customerId: String): List<CustomerServiceHistoryEntry> =
        serviceHistory[customerId] ?: defaultServiceHistory
}
