package ai.rojan.designlab.customer

import ai.rojan.designlab.screens.customer.theme.CustomerBackgroundTheme
import ai.rojan.designlab.screens.customer.theme.CustomerColors
import ai.rojan.designlab.screens.customer.theme.CustomerGlassSurface
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import android.graphics.Bitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * One-off visual-verification aid for the Customer glass depth pass —
 * NOT wired into any real screen/navigation. Splash (the only screen
 * currently using the Customer theme) has no card content of its own,
 * so this renders sample [CustomerGlassSurface] cards on
 * [CustomerBackgroundTheme] in isolation to confirm the layered-depth
 * fix actually reads as elevated glass, not a flat pastel box, before
 * any real screen adopts it in a later phase. Safe to remove once
 * reviewed.
 */
@RunWith(AndroidJUnit4::class)
class CustomerThemeScreenshotTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun captureCustomerGlassDepthDemo() {
        composeTestRule.setContent {
            CustomerBackgroundTheme {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp),
                ) {
                    CustomerGlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RojanShapes.GlassCard,
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text(
                                text = "سالن رویان",
                                style = RojanTypography.CardTitle,
                                color = CustomerColors.TextPrimary,
                            )
                            Text(
                                text = "آرایش و زیبایی بانوان",
                                style = RojanTypography.Caption,
                                color = CustomerColors.TextSecondary,
                            )
                        }
                    }

                    CustomerGlassSurface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RojanShapes.Small,
                    ) {
                        Text(
                            text = "نوبت بعدی شما: فردا ساعت ۱۶:۰۰",
                            style = RojanTypography.Body,
                            color = CustomerColors.TextPrimary,
                            modifier = Modifier.padding(16.dp),
                        )
                    }
                }
            }
        }
        composeTestRule.waitForIdle()

        val bitmap = composeTestRule.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File(context.getExternalFilesDir(null), "customer_glass_depth_demo.png")
        file.outputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }
    }
}
