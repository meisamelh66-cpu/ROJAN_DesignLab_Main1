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
}
