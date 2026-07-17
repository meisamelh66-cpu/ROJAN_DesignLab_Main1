package ai.rojan.designlab.screens.welcome

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.model.RoleType
import ai.rojan.designlab.components.brand.RojanLogo
import ai.rojan.designlab.components.brand.VersionFooter
import ai.rojan.designlab.components.hero.HeroBookingCard
import ai.rojan.designlab.components.roles.RoleRow
import ai.rojan.designlab.ui.background.PremiumBackground

@Composable
fun WelcomeScreen(
    onRoleSelected: (RoleType) -> Unit,
    onGetStartedClick: () -> Unit
) {

    PremiumBackground {

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    horizontal = 20.dp,
                    vertical = 32.dp // Global Design System Refinement (Phase 1): 24dp -> 32dp, "increase breathing room... calmer and more premium"
                ),

            horizontalAlignment = Alignment.CenterHorizontally,

            verticalArrangement = Arrangement.SpaceBetween
        ) {


            RojanLogo()



            HeroBookingCard(
                onClick = onGetStartedClick
            )



            RoleRow(
                onRoleSelected = onRoleSelected
            )



            VersionFooter()

        }
    }
}