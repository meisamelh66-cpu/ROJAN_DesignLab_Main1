package ai.rojan.designlab.presentation.beauty

import ai.rojan.designlab.domain.beauty.BeautyProfileRepository
import ai.rojan.designlab.domain.beauty.HairProfile
import ai.rojan.designlab.domain.beauty.NailProfile
import ai.rojan.designlab.domain.beauty.SkinProfile
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

/**
 * Beauty DNA foundation - holds and edits a `CustomerBeautyProfile` via
 * [BeautyProfileRepository]. Deliberately not wired to any AI/recommendation
 * logic ("do not implement AI recommendations yet, only create the
 * foundation").
 */
class BeautyProfileViewModel(
    customerId: String,
    private val repository: BeautyProfileRepository,
) : ViewModel() {

    var profile by mutableStateOf(repository.get(customerId))
        private set

    fun updateHair(hair: HairProfile) {
        profile = profile.copy(hair = hair)
        repository.save(profile)
    }

    fun updateSkin(skin: SkinProfile) {
        profile = profile.copy(skin = skin)
        repository.save(profile)
    }

    fun updateNails(nails: NailProfile) {
        profile = profile.copy(nails = nails)
        repository.save(profile)
    }
}
