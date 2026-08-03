package ai.rojan.designlab.auth

import ai.rojan.designlab.data.identity.DemoIdentityProvider
import ai.rojan.designlab.data.identity.DemoSessionProvider
import ai.rojan.designlab.domain.repository.AuthSessionRepository
import ai.rojan.designlab.domain.repository.AuthenticatedUser
import ai.rojan.designlab.domain.repository.BackendAuthRepository
import ai.rojan.designlab.domain.repository.TokenRepository
import ai.rojan.designlab.presentation.auth.AuthViewModel
import ai.rojan.designlab.screens.auth.AuthScreen
import android.graphics.Bitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * Visual-verification aid for the Customer Login (V2) visual refinement —
 * renders the real [AuthScreen] composable (not a re-creation) against
 * demo/in-memory dependencies so it can be screenshotted before/after the
 * visual pass without navigating the live app through the "login only
 * when booking" gate. Not wired into any navigation graph. Safe to remove
 * once reviewed.
 */
@RunWith(AndroidJUnit4::class)
class AuthScreenScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private class NoOpAuthSessionRepository : AuthSessionRepository {
        override suspend fun savePersonId(personId: String) = Unit
        override suspend fun clearPersonId() = Unit
        override fun observePersonId(): Flow<String?> = flowOf(null)
        override suspend fun saveRememberMe(remember: Boolean) = Unit
        override fun observeRememberMe(): Flow<Boolean> = flowOf(true)
    }

    private class NoOpBackendAuthRepository : BackendAuthRepository {
        override suspend fun register(email: String, password: String, fullName: String): Result<AuthenticatedUser> =
            Result.failure(UnsupportedOperationException("not used by this screenshot test"))

        override suspend fun login(email: String, password: String): Result<AuthenticatedUser> =
            Result.failure(UnsupportedOperationException("not used by this screenshot test"))

        override suspend fun currentUser(): Result<AuthenticatedUser> =
            Result.failure(UnsupportedOperationException("not used by this screenshot test"))
    }

    private class NoOpTokenRepository : TokenRepository {
        override fun saveTokens(accessToken: String, refreshToken: String) = Unit
        override fun clearTokens() = Unit
        override fun accessToken(): String? = null
        override fun refreshToken(): String? = null
    }

    @Test
    fun captureAuthScreenLoggedOut() {
        composeTestRule.setContent {
            val identityProvider = DemoIdentityProvider()
            val sessionProvider = DemoSessionProvider(identityProvider)
            val authViewModel = AuthViewModel(
                sessionProvider = sessionProvider,
                identityProvider = identityProvider,
                authSessionRepository = NoOpAuthSessionRepository(),
                backendAuthRepository = NoOpBackendAuthRepository(),
                tokenRepository = NoOpTokenRepository(),
            )

            AuthScreen(
                authViewModel = authViewModel,
                onBackClick = {},
                onExistingUserAuthenticated = {},
            )
        }
        composeTestRule.waitForIdle()

        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "auth_screen_logged_out.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
