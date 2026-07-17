package ai.rojan.designlab.components.roles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

import ai.rojan.designlab.domain.model.RoleType


@Composable
fun RoleRow(
    modifier: Modifier = Modifier,
    onRoleSelected: (RoleType) -> Unit
) {

    Row(

        modifier = modifier
            .fillMaxWidth()
            .height(170.dp),

        horizontalArrangement = Arrangement.spacedBy(16.dp) // Global Design System Refinement (Phase 1): 12dp -> 16dp, "increase breathing room"

    ) {


        RoleCard3D(

            title = "مشتری",

            subtitle = "رزرو نوبت و استفاده از خدمات زیبایی",

            modifier = Modifier.weight(1f),

            onClick = {
                onRoleSelected(
                    RoleType.CUSTOMER
                )
            }

        )



        RoleCard3D(

            title = "مدیر سالن",

            subtitle = "مدیریت سالن، کارکنان و گزارش‌ها",

            modifier = Modifier.weight(1f),

            onClick = {
                onRoleSelected(
                    RoleType.SALON_MANAGER
                )
            }

        )



        RoleCard3D(

            title = "فروشنده",

            subtitle = "فروش و مدیریت محصولات",

            modifier = Modifier.weight(1f),

            onClick = {
                onRoleSelected(
                    RoleType.PRODUCT_SELLER
                )
            }

        )



        RoleCard3D(

            title = "متخصص",

            subtitle = "خدمات و مدیریت نوبت‌ها",

            modifier = Modifier.weight(1f),

            onClick = {
                onRoleSelected(
                    RoleType.BEAUTY_SPECIALIST
                )
            }

        )

    }
}