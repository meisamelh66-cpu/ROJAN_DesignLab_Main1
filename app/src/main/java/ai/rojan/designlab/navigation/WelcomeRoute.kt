package ai.rojan.designlab.navigation
import ai.rojan.designlab.domain.model.RoleType
import ai.rojan.designlab.R
import ai.rojan.designlab.components.GlassCard
import ai.rojan.designlab.components.PremiumLoadingBar
import ai.rojan.designlab.presentation.roleselection.RoleSelectionUiState
import ai.rojan.designlab.presentation.roleselection.RoleSelectionViewModel
import ai.rojan.designlab.screens.welcome.WelcomeScreen
import ai.rojan.designlab.ui.theme.RojanTextPrimary
import ai.rojan.designlab.ui.theme.RojanTextSecondary
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController


@Composable
fun WelcomeRoute(
    roleSelectionViewModel: RoleSelectionViewModel,
    navController: NavController,
) {

    val uiState by roleSelectionViewModel.uiState.collectAsStateWithLifecycle()


    LaunchedEffect(Unit) {
        roleSelectionViewModel.navigationEvents.collect { role ->
            navController.navigate(
                RojanDestinations.routeForRole(role)
            ) {
                popUpTo(RojanDestinations.WELCOME) {
                    inclusive = true
                }
                launchSingleTop = true
            }
        }
    }


    Box(
        modifier = Modifier.fillMaxSize()
    ) {


        WelcomeScreen(

            onRoleSelected = { roleType ->
                roleSelectionViewModel.selectRole(
                    roleType.toDomainRole()
                )
            },
        )



        AnimatedVisibility(
            visible = uiState is RoleSelectionUiState.Saving,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically(
                initialOffsetY = { it / 2 }
            ),
            exit = fadeOut() + slideOutVertically(
                targetOffsetY = { it / 2 }
            ),
        ) {


            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = 20.dp,
                        vertical = 16.dp
                    ),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {


                Text(
                    text = stringResource(
                        R.string.status_saving_role
                    ),
                    color = RojanTextSecondary,
                    fontSize = 12.sp,
                )


                Spacer(
                    modifier = Modifier.height(8.dp)
                )


                PremiumLoadingBar(
                    modifier = Modifier.width(120.dp)
                )

            }

        }




        AnimatedVisibility(
            visible = uiState is RoleSelectionUiState.Error,
            modifier = Modifier.align(Alignment.BottomCenter),
            enter = fadeIn() + slideInVertically(
                initialOffsetY = { it / 2 }
            ),
            exit = fadeOut() + slideOutVertically(
                targetOffsetY = { it / 2 }
            ),
        ) {


            GlassCard(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                cornerRadius = 20.dp,
                contentPadding = 16.dp,
            ) {


                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {


                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {


                        Icon(
                            imageVector = Icons.Filled.ErrorOutline,
                            contentDescription = stringResource(
                                R.string.cd_error_icon
                            ),
                            tint = RojanTextPrimary,
                        )


                        Text(
                            text = stringResource(
                                R.string.error_role_selection_generic
                            ),
                            color = RojanTextPrimary,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier.weight(1f),
                        )

                    }



                    Spacer(
                        modifier = Modifier.height(8.dp)
                    )



                    TextButton(
                        onClick = {
                            roleSelectionViewModel.retry()
                        }
                    ) {

                        Text(
                            text = stringResource(
                                R.string.action_retry
                            )
                        )

                    }

                }

            }

        }

    }

}