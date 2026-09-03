package ai.rojan.designlab.reception.data

import ai.rojan.designlab.data.remote.ReceptionInviteApi
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.reception.domain.repository.ReceptionInviteRepository
import ai.rojan.designlab.reception.domain.repository.SalonInviteAcceptance
import ai.rojan.designlab.reception.domain.repository.SalonInvitePreview

/** Real backend-backed [ReceptionInviteRepository] — see that interface's own doc comment for the confirmed `InviteController` contract this implements. */
class BackendReceptionInviteRepository(
    private val receptionInviteApi: ReceptionInviteApi,
) : ReceptionInviteRepository {

    override suspend fun getInvite(token: String): Result<SalonInvitePreview> =
        safeApiCall { receptionInviteApi.getInvite(token) }
            .map { SalonInvitePreview(salonName = it.salonName, role = it.role.name) }

    override suspend fun acceptInvite(token: String): Result<SalonInviteAcceptance> =
        safeApiCall { receptionInviteApi.acceptInvite(token) }
            .map { SalonInviteAcceptance(salonId = it.salonId, role = it.role.name) }
}
