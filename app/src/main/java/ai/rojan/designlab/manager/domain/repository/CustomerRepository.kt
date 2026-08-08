package ai.rojan.designlab.manager.domain.repository

import ai.rojan.designlab.manager.domain.customer.CustomerServiceHistoryEntry
import ai.rojan.designlab.manager.domain.customer.ManagerCustomer

interface CustomerRepository {
    fun getAll(): List<ManagerCustomer>
    fun getById(id: String): ManagerCustomer?
    fun search(query: String): List<ManagerCustomer>
    suspend fun create(customer: ManagerCustomer): Result<ManagerCustomer>
    suspend fun update(customer: ManagerCustomer): Result<ManagerCustomer?>
    fun getServiceHistory(customerId: String): List<CustomerServiceHistoryEntry>

    /**
     * Loads the real per-customer detail (visit history, latest note) a
     * single customer profile view needs but the bulk [getAll]/[search]
     * listing deliberately doesn't fetch (see
     * [ai.rojan.designlab.manager.data.BackendCustomerRepository]'s own
     * doc comment for why - fetching this for every row in a list would
     * be an N+1 call). Call once before reading [getById]/
     * [getServiceHistory] for a specific customer; a no-op for
     * [ai.rojan.designlab.manager.data.InMemoryCustomerRepository], whose
     * data is already fully in memory.
     */
    suspend fun loadDetail(customerId: String): Result<Unit>
}
