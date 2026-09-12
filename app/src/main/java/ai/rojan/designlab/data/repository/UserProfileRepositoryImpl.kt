package ai.rojan.designlab.data.repository

import ai.rojan.designlab.data.remote.UserMediaApi
import ai.rojan.designlab.data.remote.safeApiCall
import ai.rojan.designlab.domain.repository.AuthenticatedUser
import ai.rojan.designlab.domain.repository.UserProfileRepository
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody

/**
 * Backend-backed [UserProfileRepository]. Multipart shape mirrors
 * [ai.rojan.designlab.manager.data.BackendManagerMediaRepository.upload]
 * (a single `file` form-data part), wrapped in `safeApiCall` so a malformed
 * or contract-drifted response is a `Result.failure`, never a crash.
 */
class UserProfileRepositoryImpl(
    private val userMediaApi: UserMediaApi,
) : UserProfileRepository {

    override suspend fun uploadAvatar(fileBytes: ByteArray, fileName: String, mimeType: String): Result<AuthenticatedUser> =
        safeApiCall { userMediaApi.uploadAvatar(filePart(fileBytes, fileName, mimeType)) }
            .map { it.toAuthenticatedUser() }

    override suspend fun uploadCover(fileBytes: ByteArray, fileName: String, mimeType: String): Result<AuthenticatedUser> =
        safeApiCall { userMediaApi.uploadCover(filePart(fileBytes, fileName, mimeType)) }
            .map { it.toAuthenticatedUser() }

    override suspend fun removeAvatar(): Result<AuthenticatedUser> =
        safeApiCall { userMediaApi.deleteAvatar() }.map { it.toAuthenticatedUser() }

    override suspend fun removeCover(): Result<AuthenticatedUser> =
        safeApiCall { userMediaApi.deleteCover() }.map { it.toAuthenticatedUser() }

    private fun filePart(fileBytes: ByteArray, fileName: String, mimeType: String): MultipartBody.Part =
        MultipartBody.Part.createFormData(
            name = "file",
            filename = fileName,
            body = fileBytes.toRequestBody(mimeType.toMediaTypeOrNull()),
        )
}
