package ai.rojan.designlab.components.roles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * UX Refactor Phase 2: the Customer card is removed — Welcome is now a
 * business-only ("business login") entry point per Phase 1, and a
 * customer no longer has any reason to tap a role card at all now that
 * real authenticated identity (not a card tap) decides Customer routing.
 * See [ai.rojan.designlab.navigation.RojanNavGraph]'s `startDestination`
 * for the current customer-routing logic.
 *
 * UX Refactor Phase 3: tapping either remaining card starts real
 * phone/OTP login instead of persisting a role — the actual dashboard
 * (Manager vs Specialist vs denied) is decided afterward from real
 * PersonRole assignments, not by which card was tapped. UX Refactor
 * Phase 4: [onRoleSelected] reflects that directly — it no longer takes
 * a role type at all, since one was never meaningfully used past Phase 3.
 */
@Composable
fun RoleRow(
    modifier: Modifier = Modifier,
    onRoleSelected: () -> Unit
) {

    Row(

        modifier = modifier
            .fillMaxWidth()
            .height(170.dp),

        horizontalArrangement = Arrangement.spacedBy(16.dp) // Global Design System Refinement (Phase 1): 12dp -> 16dp, "increase breathing room"

    ) {


        RoleCard3D(

            title = "مدیر سالن",

            subtitle = "مدیریت سالن، کارکنان و گزارش‌ها",

            modifier = Modifier.weight(1f),

            onClick = onRoleSelected

        )



        RoleCard3D(

            title = "متخصص",

            subtitle = "خدمات و مدیریت نوبت‌ها",

            modifier = Modifier.weight(1f),

            onClick = onRoleSelected

        )

    }
}