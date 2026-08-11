package ai.rojan.designlab.domain.usecase.relationship

import ai.rojan.designlab.domain.repository.CustomerFavoriteSalon
import ai.rojan.designlab.domain.repository.CustomerFollowedSalon
import ai.rojan.designlab.domain.repository.CustomerRelationshipRepository

/**
 * First use-case layer in this codebase (every prior ViewModel calls its
 * repository directly) - introduced here per this feature's own explicit
 * architecture requirement ("UI must never call Retrofit directly", a
 * stricter boundary than the rest of the app currently enforces). Each use
 * case is a thin, single-responsibility pass-through today since the
 * backend already enforces every real business rule (idempotency, tenant
 * isolation, salon existence) - this is the seam future client-side rules
 * (e.g. optimistic-update coordination) attach to without reaching back
 * into the repository or ViewModel.
 */
class FollowSalonUseCase(private val repository: CustomerRelationshipRepository) {
    suspend operator fun invoke(salonId: String): Result<CustomerFollowedSalon> = repository.followSalon(salonId)
}

class UnfollowSalonUseCase(private val repository: CustomerRelationshipRepository) {
    suspend operator fun invoke(salonId: String): Result<Unit> = repository.unfollowSalon(salonId)
}

class GetFollowedSalonsUseCase(private val repository: CustomerRelationshipRepository) {
    suspend operator fun invoke(page: Int = 0, size: Int = 20): Result<List<CustomerFollowedSalon>> =
        repository.getFollowedSalons(page, size)
}

class FavoriteSalonUseCase(private val repository: CustomerRelationshipRepository) {
    suspend operator fun invoke(salonId: String): Result<CustomerFavoriteSalon> = repository.favoriteSalon(salonId)
}

class UnfavoriteSalonUseCase(private val repository: CustomerRelationshipRepository) {
    suspend operator fun invoke(salonId: String): Result<Unit> = repository.unfavoriteSalon(salonId)
}

class GetFavoriteSalonsUseCase(private val repository: CustomerRelationshipRepository) {
    suspend operator fun invoke(page: Int = 0, size: Int = 20): Result<List<CustomerFavoriteSalon>> =
        repository.getFavoriteSalons(page, size)
}
