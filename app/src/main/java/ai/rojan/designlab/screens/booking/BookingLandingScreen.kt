package ai.rojan.designlab.screens.booking


import ai.rojan.designlab.screens.customer.NearbySalons
import ai.rojan.designlab.ui.background.PremiumBackground
import ai.rojan.designlab.ui.theme.RojanDimens
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp



/**
 * Journey 1 completion. [onSearchClick] fires from a transparent overlay
 * over [BookingSearchBar] and from [BookingHeroCard]/[BookingCategories]'
 * CTAs — Search is the one required gate per
 * [ai.rojan.designlab.domain.booking.rules.BookingStepResolver], so
 * every "start booking" entry point on this landing screen leads there,
 * consistent with the intent-driven architecture rather than each entry
 * point inventing its own destination.
 */
@Composable
fun BookingLandingScreen(
    onSearchClick: () -> Unit,
    onSalonClick: (String) -> Unit,
    onSpecialistClick: (String) -> Unit,
    onHomeClick: () -> Unit = {},
    onBookingsClick: () -> Unit = {},
    onProfileClick: () -> Unit = {},
) {


    PremiumBackground {


        Column(


            modifier = Modifier

                .fillMaxSize()

                .verticalScroll(

                    rememberScrollState()

                ),


            verticalArrangement = Arrangement.spacedBy(

                RojanDimens.SpaceMD

            )


        ) {



            BookingHeader()



            Box(modifier = Modifier.fillMaxWidth()) {
                BookingSearchBar()
                // Transparent tap-catcher: BookingSearchBar's own editable
                // field stays untouched (see its doc comment on why it's
                // preserved rather than converted), this overlay just
                // intercepts taps to route to the full Search screen.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .clickable(onClick = onSearchClick)
                )
            }



            BookingHeroCard(onBookClick = onSearchClick)



            BookingCategories(onCategoryClick = onSearchClick)



            NearbySalons(onSalonClick = onSalonClick)



            RecommendedSpecialists(onSpecialistClick = onSpecialistClick)



            Spacer(

                modifier = Modifier.height(120.dp)

            )


            BookingBottomBar(
                onHomeClick = onHomeClick,
                onBookingsClick = onBookingsClick,
                onProfileClick = onProfileClick,
            )



        }


    }


}