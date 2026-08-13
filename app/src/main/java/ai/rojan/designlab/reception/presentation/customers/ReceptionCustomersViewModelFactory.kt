package ai.rojan.designlab.reception.presentation.customers

import ai.rojan.designlab.reception.data.ReceptionRepositories
import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class ReceptionCustomersViewModelFactory(
    private val appContext: Context,
    private val salonId: String,
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        val repositories = ReceptionRepositories.from(appContext, salonId)
        return ReceptionCustomersViewModel(salonId, repositories.customerRepository) as T
    }
}
