package ai.rojan.designlab.manager.screens.settings

import ai.rojan.designlab.manager.components.ManagerColors
import ai.rojan.designlab.manager.components.ManagerGlassSurface
import ai.rojan.designlab.manager.components.ManagerPrimaryButton
import ai.rojan.designlab.ui.text.Text
import ai.rojan.designlab.ui.theme.RojanDimens
import ai.rojan.designlab.ui.theme.RojanShapes
import ai.rojan.designlab.ui.theme.RojanTypography
import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapListener
import org.osmdroid.events.ScrollEvent
import org.osmdroid.events.ZoomEvent
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import java.io.File

/** Tehran - the fallback map center when no last-known device location is available and the owner hasn't set coordinates before. */
private const val DEFAULT_MAP_LATITUDE = 35.6892
private const val DEFAULT_MAP_LONGITUDE = 51.3890
private const val DEFAULT_MAP_ZOOM = 15.0
private const val MAP_MIN_ZOOM = 3.0
private const val MAP_MAX_ZOOM = 20.0

/**
 * Manager Location Picker: the in-app map screen itself.
 *
 * Architecture: osmdroid (OpenStreetMap tiles) is the map engine - this
 * project has no Google Maps API key or billing set up anywhere, and
 * none should be introduced for one settings screen; osmdroid needs
 * neither a key nor a Play Services dependency this project doesn't
 * otherwise have. No other map capability exists anywhere in this
 * project to reuse instead (confirmed by inspection before adding this).
 *
 * [initialLatitude]/[initialLongitude] (the salon's already-saved
 * coordinate, if any) take priority for centering; only when neither is
 * set does the map try the device's last-known location (if permission
 * was granted - never a fresh/blocking GPS request, this is a one-shot,
 * best-effort hint only), falling back to a fixed default if neither
 * exists. Wherever the center comes from, the owner can freely pan from
 * there - the fixed pin overlay always marks the exact center of the
 * visible map, and [onConfirm] is called with that same center point
 * ([MapView.getMapCenter]), read live via [MapListener.onScroll] as the
 * owner moves the map. This is the ONLY path [onConfirm] is invoked
 * through - there is no external map app anywhere in this flow, so
 * there is no coordinate to "return" from one.
 *
 * The "موقعیت من" (My Location) button re-centers the map on a fresh
 * last-known-location read, same one-shot/non-blocking/never-authoritative
 * rule as the initial centering above - it only ever moves the camera via
 * [MapView.getController]/`animateTo`, which itself triggers the same
 * [MapListener.onScroll] path, so pressing it still requires the owner to
 * tap "تأیید این موقعیت" afterward; it never calls [onConfirm] itself and
 * never bypasses the pin-confirmation step.
 */
@Composable
fun ManagerLocationPickerMapDialog(
    initialLatitude: Double?,
    initialLongitude: Double?,
    onConfirm: (latitude: Double, longitude: Double) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val startLatitude = initialLatitude ?: lastKnownLatitudeOrNull(context) ?: DEFAULT_MAP_LATITUDE
    val startLongitude = initialLongitude ?: lastKnownLongitudeOrNull(context) ?: DEFAULT_MAP_LONGITUDE

    var centerLatitude by remember { mutableDoubleStateOf(startLatitude) }
    var centerLongitude by remember { mutableDoubleStateOf(startLongitude) }
    var mapViewRef by remember { mutableStateOf<MapView?>(null) }

    fun recenterOnMyLocation() {
        val location = lastKnownLocationOrNull(context) ?: return
        mapViewRef?.controller?.animateTo(GeoPoint(location.latitude, location.longitude))
        centerLatitude = location.latitude
        centerLongitude = location.longitude
    }

    val myLocationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { recenterOnMyLocation() }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(modifier = Modifier.fillMaxSize()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    Configuration.getInstance().apply {
                        userAgentValue = ctx.packageName
                        osmdroidTileCache = File(ctx.cacheDir, "osmdroid")
                    }
                    MapView(ctx).apply {
                        setTileSource(TileSourceFactory.MAPNIK)
                        setMultiTouchControls(true)
                        minZoomLevel = MAP_MIN_ZOOM
                        maxZoomLevel = MAP_MAX_ZOOM
                        controller.setZoom(DEFAULT_MAP_ZOOM)
                        controller.setCenter(GeoPoint(startLatitude, startLongitude))
                        addMapListener(object : MapListener {
                            override fun onScroll(event: ScrollEvent?): Boolean {
                                mapCenter.let {
                                    centerLatitude = it.latitude
                                    centerLongitude = it.longitude
                                }
                                return true
                            }
                            override fun onZoom(event: ZoomEvent?): Boolean = false
                        })
                        mapViewRef = this
                    }
                },
                onRelease = { it.onDetach() },
            )

            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "پین موقعیت",
                tint = ManagerColors.Gold,
                modifier = Modifier
                    .align(Alignment.Center)
                    .size(40.dp),
            )

            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(start = RojanDimens.SpaceMD, end = RojanDimens.SpaceMD, bottom = 96.dp)
                    .size(48.dp)
                    .background(color = ManagerColors.BaseDeep, shape = CircleShape)
                    .clickable {
                        val hasPermission = ContextCompat.checkSelfPermission(
                            context,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                        ) == PackageManager.PERMISSION_GRANTED
                        if (hasPermission) {
                            recenterOnMyLocation()
                        } else {
                            myLocationPermissionLauncher.launch(
                                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION),
                            )
                        }
                    },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Filled.MyLocation,
                    contentDescription = "موقعیت من",
                    tint = ManagerColors.Turquoise,
                    modifier = Modifier.size(24.dp),
                )
            }

            ManagerGlassSurface(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .fillMaxWidth()
                    .padding(RojanDimens.SpaceMD),
                shape = RojanShapes.GlassCard,
            ) {
                Text(
                    text = "نقشه را جابه‌جا کنید تا پین روی موقعیت دقیق قرار گیرد",
                    style = RojanTypography.Caption,
                    color = ManagerColors.TextPrimary,
                    modifier = Modifier.padding(RojanDimens.SpaceSM),
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(RojanDimens.SpaceMD),
                horizontalArrangement = Arrangement.spacedBy(RojanDimens.SpaceSM),
            ) {
                ManagerPrimaryButton(
                    text = "انصراف",
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                )
                ManagerPrimaryButton(
                    text = "تأیید این موقعیت",
                    onClick = { onConfirm(centerLatitude, centerLongitude) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * One-shot, non-blocking hint only - never a fresh GPS request, never
 * what gets saved. `null` whenever permission isn't granted, no provider
 * has ever produced a location, or the device has no location services
 * at all; the map simply falls back to [DEFAULT_MAP_LATITUDE]/
 * [DEFAULT_MAP_LONGITUDE] in that case.
 */
private fun lastKnownLatitudeOrNull(context: Context): Double? = lastKnownLocationOrNull(context)?.latitude

private fun lastKnownLongitudeOrNull(context: Context): Double? = lastKnownLocationOrNull(context)?.longitude

private fun lastKnownLocationOrNull(context: Context): Location? {
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.ACCESS_FINE_LOCATION,
    ) == PackageManager.PERMISSION_GRANTED
    if (!hasPermission) return null

    val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER)
        .mapNotNull { provider -> runCatching { locationManager.getLastKnownLocation(provider) }.getOrNull() }
        .maxByOrNull { it.time }
}
