package noox.bzr.maps

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import android.os.CancellationSignal
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions

/** Native Maps SDK bridge. Screen decisions remain in CustomerViewModel/Core (DEC-056). */
class NativeGoogleMapController(
    private val context: Context,
    private val selectable: Boolean,
    private val onSelected: (Double, Double) -> Unit = { _, _ -> },
) : DefaultLifecycleObserver, OnMapReadyCallback {
    val view = MapView(context)
    private var map: GoogleMap? = null
    private var selectedMarker: Marker? = null
    private var providerMarker: Marker? = null
    private var destinationMarker: Marker? = null
    private var pendingSelection: LatLng? = null
    private var pendingTracking: Pair<LatLng?, LatLng?>? = null

    init {
        view.onCreate(null)
        view.getMapAsync(this)
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap.apply {
            uiSettings.isMapToolbarEnabled = false
            uiSettings.isCompassEnabled = true
            uiSettings.isZoomControlsEnabled = false
            if (selectable) {
                setOnMapClickListener(::select)
            }
        }
        pendingSelection?.let(::select)
        pendingTracking?.let { (provider, destination) -> showTracking(provider, destination) }
        if (pendingSelection == null && pendingTracking == null) {
            googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(DAMIETTA, DEFAULT_ZOOM))
        }
    }

    fun showSelection(latitude: Double?, longitude: Double?) {
        if (latitude == null || longitude == null) return
        val point = LatLng(latitude, longitude)
        pendingSelection = point
        if (map != null) select(point, notify = false)
    }

    fun showTracking(provider: LatLng?, destination: LatLng?) {
        pendingTracking = provider to destination
        val googleMap = map ?: return
        providerMarker?.remove()
        destinationMarker?.remove()
        providerMarker = provider?.let {
            googleMap.addMarker(MarkerOptions().position(it).title(context.getString(noox.bzr.design.R.string.tracking_provider_location)))
        }
        destinationMarker = destination?.let {
            googleMap.addMarker(MarkerOptions().position(it).title(context.getString(noox.bzr.design.R.string.tracking_destination_location)))
        }
        val points = listOfNotNull(provider, destination)
        when (points.size) {
            1 -> googleMap.animateCamera(CameraUpdateFactory.newLatLngZoom(points.first(), TRACKING_ZOOM))
            2 -> googleMap.animateCamera(
                CameraUpdateFactory.newLatLngBounds(
                    LatLngBounds.builder().include(points[0]).include(points[1]).build(),
                    context.resources.getDimensionPixelSize(noox.bzr.design.R.dimen.bremo_space_xxl),
                ),
            )
        }
    }

    fun centerOnCurrentLocation(context: Context, onUnavailable: () -> Unit = {}) {
        val hasPermission = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (!hasPermission) {
            onUnavailable()
            return
        }
        val manager = context.getSystemService(LocationManager::class.java)
        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        }
        if (provider == null) {
            onUnavailable()
            return
        }
        LocationManagerCompat.getCurrentLocation(
            manager,
            provider,
            CancellationSignal(),
            ContextCompat.getMainExecutor(context),
        ) { location ->
            if (location == null) onUnavailable() else select(LatLng(location.latitude, location.longitude))
        }
    }

    private fun select(point: LatLng, notify: Boolean = true) {
        pendingSelection = point
        selectedMarker?.remove()
        selectedMarker = map?.addMarker(MarkerOptions().position(point))
        map?.animateCamera(CameraUpdateFactory.newLatLngZoom(point, SELECTION_ZOOM))
        if (notify) onSelected(point.latitude, point.longitude)
    }

    override fun onStart(owner: LifecycleOwner) = view.onStart()
    override fun onResume(owner: LifecycleOwner) = view.onResume()
    override fun onPause(owner: LifecycleOwner) = view.onPause()
    override fun onStop(owner: LifecycleOwner) = view.onStop()
    override fun onDestroy(owner: LifecycleOwner) = view.onDestroy()
    private companion object {
        val DAMIETTA = LatLng(31.4368, 31.6670)
        const val DEFAULT_ZOOM = 12f
        const val SELECTION_ZOOM = 16f
        const val TRACKING_ZOOM = 15f
    }
}
