package noox.bzr.provider

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.LocationManager
import android.net.Uri
import android.os.CancellationSignal
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.fragment.app.activityViewModels
import noox.bzr.customer.C19ChatView
import noox.bzr.customer.C32NotificationsView
import noox.bzr.links.openNotificationSettings
import noox.bzr.customer.CustomerPhase
import noox.bzr.customer.CustomerUiState
import noox.bzr.customer.CustomerMediaUpload
import noox.bzr.gallery.R
import noox.bzr.maps.NativeGoogleMapController
import noox.bzr.screens.ScreenFragment

abstract class ProviderFragment<V : ProviderScreenView>(private val destination: Int) : ScreenFragment<ProviderUiState, V>() {
    protected val viewModel: ProviderViewModel by activityViewModels()
    override val states get() = viewModel.stateFlow
    override fun accepts(state: ProviderUiState) = ProviderRoutes.destination(state) == destination
    override fun V.render(state: ProviderUiState) = render(state)

    protected fun imagePicker(fallbackName: String, upload: (CustomerMediaUpload) -> Unit) =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult
            val resolver = requireContext().contentResolver
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@registerForActivityResult
            if (bytes.size > MAX_IMAGE_BYTES) return@registerForActivityResult
            upload(CustomerMediaUpload(uri.lastPathSegment ?: fallbackName, resolver.getType(uri) ?: "image/jpeg", bytes))
        }

    protected fun V.bindBack() { onBack = viewModel::goBack }

    private companion object { const val MAX_IMAGE_BYTES = 10 * 1024 * 1024 }
}

class P01IntroFragment : ProviderFragment<P01IntroView>(R.id.scr_p01) {
    override fun create() = P01IntroView(requireContext())
    override fun P01IntroView.bind() { bindBack(); onAction = viewModel::handleAction }
}

class P02ProfileFragment : ProviderFragment<P02ProfileView>(R.id.scr_p02) {
    private val photo = imagePicker("provider-profile.jpg", viewModel::uploadProfilePhoto)
    override fun create() = P02ProfileView(requireContext())
    override fun P02ProfileView.bind() {
        bindBack()
        onExperienceChange = { viewModel.updateProfile(experience = it) }
        onBioChange = { viewModel.updateProfile(about = it) }
        onPickPhoto = { photo.launch("image/*") }
        onNext = viewModel::openIdentity
    }
}

class P03IdentityFragment : ProviderFragment<P03IdentityView>(R.id.scr_p03) {
    private val front = imagePicker("id-front.jpg") { viewModel.uploadIdentity(true, it) }
    private val back = imagePicker("id-back.jpg") { viewModel.uploadIdentity(false, it) }
    override fun create() = P03IdentityView(requireContext())
    override fun P03IdentityView.bind() {
        bindBack(); onPickFront = { front.launch("image/*") }; onPickBack = { back.launch("image/*") }; onNext = viewModel::loadCatalog
    }
}

class P04CatalogFragment : ProviderFragment<P04CatalogView>(R.id.scr_p04) {
    override fun create() = P04CatalogView(requireContext())
    override fun P04CatalogView.bind() {
        bindBack(); onCategory = viewModel::toggleCategory; onSpecialty = viewModel::toggleSpecialty; onNext = viewModel::loadCities
    }
}

class P05AreasFragment : ProviderFragment<P05AreasView>(R.id.scr_p05) {
    override fun create() = P05AreasView(requireContext())
    override fun P05AreasView.bind() { bindBack(); onCity = viewModel::selectCity; onArea = viewModel::toggleArea; onNext = viewModel::loadPayout }
}

class P06PayoutFragment : ProviderFragment<P06PayoutView>(R.id.scr_p06) {
    override fun create() = P06PayoutView(requireContext())
    override fun P06PayoutView.bind() {
        bindBack(); onMethod = viewModel::selectPayout; onDetailsChange = viewModel::updatePayoutDetails; onSubmit = viewModel::submit
    }
}

class P07StatusFragment : ProviderFragment<P07StatusView>(R.id.scr_p07) {
    override fun create() = P07StatusView(requireContext())
    override fun P07StatusView.bind() { bindBack(); onAction = viewModel::handleAction }
}

class P08HomeFragment : ProviderFragment<P08HomeView>(R.id.scr_p08) {
    override fun create() = P08HomeView(requireContext())
    override fun P08HomeView.bind() {
        bindBack()
        onAvailabilityChange = viewModel::toggleAvailability
        onAction = viewModel::handleAction
        onRequest = viewModel::openMarketRequest
    }
}

class P09AvailableRequestFragment : ProviderFragment<P09AvailableRequestView>(R.id.scr_p09) {
    override fun create() = P09AvailableRequestView(requireContext())
    override fun P09AvailableRequestView.bind() { bindBack(); onAction = viewModel::handleMarketRequestAction }
}

class P10OfferFragment : ProviderFragment<P10OfferView>(R.id.scr_p10) {
    override fun create() = P10OfferView(requireContext())
    override fun P10OfferView.bind() {
        bindBack()
        onEtaSelect = viewModel::selectOfferEta
        onDeductibleSelect = viewModel::selectOfferDeductible
        onPriceChange = viewModel::updateOfferPrice
        onIncludesChange = viewModel::updateOfferIncludes
        onNoteChange = viewModel::updateOfferNote
        onSubmit = viewModel::submitMarketOffer
    }
}

class P11OffersFragment : ProviderFragment<P11OffersView>(R.id.scr_p11) {
    override fun create() = P11OffersView(requireContext())
    override fun P11OffersView.bind() {
        bindBack()
        onOfferAction = viewModel::handleOfferListAction
        onConfirmWithdraw = viewModel::confirmOfferWithdrawal
        onCancelWithdraw = viewModel::cancelOfferWithdrawal
    }
}

abstract class ProviderActiveOrderFragment<V : ProviderActiveOrderView>(destination: Int) : ProviderFragment<V>(destination) {
    private var pendingLocationAction: String? = null
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) pendingLocationAction?.let(::resolveLocation)
        pendingLocationAction = null
    }

    protected fun V.bindOrderActions() {
        bindBack()
        onAction = { action ->
            when (action) {
                "start_trip", "mark_arrived" -> requestLocation(action)
                "navigate" -> navigate()
                "call" -> call()
                else -> viewModel.handleOrderAction(action)
            }
        }
    }

    private fun requestLocation(action: String) {
        val context = requireContext()
        val granted = listOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            .any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }
        if (granted) {
            resolveLocation(action)
        } else {
            pendingLocationAction = action
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }

    private fun resolveLocation(action: String) {
        val context = requireContext()
        val finePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        val coarsePermission = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (finePermission != PackageManager.PERMISSION_GRANTED && coarsePermission != PackageManager.PERMISSION_GRANTED) return
        val manager = context.getSystemService(LocationManager::class.java)
        val provider = when {
            manager.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            manager.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            else -> null
        } ?: return
        try {
            LocationManagerCompat.getCurrentLocation(
                manager,
                provider,
                CancellationSignal(),
                ContextCompat.getMainExecutor(context),
            ) { location ->
                location?.let { viewModel.performLocationAction(action, it.latitude, it.longitude) }
            }
        } catch (_: SecurityException) {
            return
        }
    }

    private fun navigate() {
        val (latitude, longitude) = viewModel.activeDestination() ?: return
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:$latitude,$longitude?q=$latitude,$longitude"))
        if (intent.resolveActivity(requireContext().packageManager) != null) startActivity(intent)
    }

    private fun call() {
        val phone = viewModel.activeCustomerPhone() ?: return
        val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone"))
        if (intent.resolveActivity(requireContext().packageManager) != null) startActivity(intent)
    }
}

class P12ConfirmedOrderFragment : ProviderActiveOrderFragment<P12ConfirmedOrderView>(R.id.scr_p12) {
    override fun create() = P12ConfirmedOrderView(requireContext())
    override fun P12ConfirmedOrderView.bind() = bindOrderActions()
}

class P13OnTheWayFragment : ProviderActiveOrderFragment<P13OnTheWayView>(R.id.scr_p13) {
    private var mapController: NativeGoogleMapController? = null
    override fun create() = P13OnTheWayView(requireContext())
    override fun P13OnTheWayView.bind() {
        bindOrderActions()
        val controller = NativeGoogleMapController(requireContext(), selectable = false)
        mapController = controller
        lifecycle.addObserver(controller)
        attachNativeMap(controller.view) { controller.centerOnCurrentLocation(requireContext()) }
        val destination = viewModel.activeDestination()
        controller.showTracking(null, destination?.let { com.google.android.gms.maps.model.LatLng(it.first, it.second) })
    }
}

class P14ArrivedFragment : ProviderActiveOrderFragment<P14ArrivedView>(R.id.scr_p14) {
    override fun create() = P14ArrivedView(requireContext())
    override fun P14ArrivedView.bind() = bindOrderActions()
}

class P15InProgressFragment : ProviderActiveOrderFragment<P15InProgressView>(R.id.scr_p15) {
    override fun create() = P15InProgressView(requireContext())
    override fun P15InProgressView.bind() = bindOrderActions()
}

class P16AwaitingPaymentFragment : ProviderActiveOrderFragment<P16AwaitingPaymentView>(R.id.scr_p16) {
    override fun create() = P16AwaitingPaymentView(requireContext())
    override fun P16AwaitingPaymentView.bind() = bindOrderActions()
}

class P17CustomerRatingFragment : ProviderFragment<P17CustomerRatingView>(R.id.scr_p17) {
    override fun create() = P17CustomerRatingView(requireContext())
    override fun P17CustomerRatingView.bind() {
        bindBack()
        onRate = viewModel::selectCustomerRating
        onSubmit = viewModel::submitCustomerRating
    }
}

class P18EarningsFragment : ProviderFragment<P18EarningsView>(R.id.scr_p18) {
    override fun create() = P18EarningsView(requireContext())
    override fun P18EarningsView.bind() = bindBack()
}

class P19ProfileFragment : ProviderFragment<P19ProfileView>(R.id.scr_p19) {
    private val customerViewModel: noox.bzr.customer.CustomerViewModel by activityViewModels()
    private val avatar = imagePicker("provider-avatar.jpg", viewModel::uploadWorkspaceAvatar)
    private val portfolio = imagePicker("provider-portfolio.jpg", viewModel::addWorkspacePortfolio)
    override fun create() = P19ProfileView(requireContext())
    override fun P19ProfileView.bind() {
        bindBack()
        onExperienceChange = { viewModel.updateWorkspaceProfile(experience = it) }
        onBioChange = { viewModel.updateWorkspaceProfile(about = it) }
        onSpecialty = viewModel::toggleWorkspaceSpecialty
        onArea = viewModel::toggleWorkspaceArea
        onCaptionChange = { viewModel.updateWorkspaceProfile(caption = it) }
        onAvatar = { avatar.launch("image/*") }
        onPortfolio = { portfolio.launch("image/*") }
        onDeletePortfolio = viewModel::deleteWorkspacePortfolio
        onSupport = {
            viewModel.closeFlow()
            customerViewModel.loadHelp()
        }
        onSave = viewModel::saveWorkspaceProfile
    }
}

/** SCR-C32 in provider mode (DEC-058): the customer view, provider-mode data. */
class ProviderNotificationsFragment : ScreenFragment<ProviderUiState, C32NotificationsView>() {
    private val viewModel: ProviderViewModel by activityViewModels()
    override val states get() = viewModel.stateFlow
    override fun create() = C32NotificationsView(requireContext())
    override fun C32NotificationsView.bind() {
        onBack = viewModel::goBack
        onOpen = viewModel::openNotification
        onOpenSettings = { openNotificationSettings(requireContext()) }
    }
    override fun C32NotificationsView.render(state: ProviderUiState) = render(viewModel.notificationsState)
    override fun accepts(state: ProviderUiState) = state.screen == "SCR-C32"
}

class ProviderChatFragment : ScreenFragment<ProviderUiState, C19ChatView>() {
    private val viewModel: ProviderViewModel by activityViewModels()
    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        val resolver = requireContext().contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@registerForActivityResult
        if (bytes.size <= 10 * 1024 * 1024) {
            viewModel.sendChatPhoto(CustomerMediaUpload(uri.lastPathSegment ?: "chat-image.jpg", resolver.getType(uri) ?: "image/jpeg", bytes))
        }
    }
    override val states get() = viewModel.stateFlow
    override fun create() = C19ChatView(requireContext())
    override fun C19ChatView.bind() {
        onBack = viewModel::goBack
        onDraftChange = viewModel::updateChatDraft
        onSend = viewModel::sendChatMessage
        onAddPhoto = { picker.launch("image/*") }
    }
    override fun C19ChatView.render(state: ProviderUiState) = render(state.asCustomerChatState())
    override fun accepts(state: ProviderUiState) = state.screen == "SCR-C19"
}

private fun ProviderUiState.asCustomerChatState() = CustomerUiState(
    screen = screen,
    phase = when (phase) {
        ProviderPhase.Empty -> CustomerPhase.Empty
        ProviderPhase.Loading -> CustomerPhase.Loading
        ProviderPhase.Content -> CustomerPhase.Content
        ProviderPhase.Error -> CustomerPhase.Error
    },
    canContinue = canContinue,
    messageKey = messageKey,
    items = items,
    itemDetails = itemDetails,
    itemStates = itemStates,
    options = options,
    fieldValues = fieldValues,
    isBusy = isBusy,
)

class P20ProposalFragment : ProviderFragment<P20ProposalView>(R.id.scr_p20) {
    private val photo = imagePicker("proposal.jpg", viewModel::uploadProposalPhoto)
    override fun create() = P20ProposalView(requireContext())
    override fun P20ProposalView.bind() {
        bindBack()
        onTypeSelect = viewModel::selectProposalType
        onAmountChange = viewModel::updateProposalAmount
        onReasonChange = viewModel::updateProposalReason
        onOutsideReasonChange = viewModel::updateOutsidePriceGuideReason
        onPickPhoto = { photo.launch("image/*") }
        onDeletePhoto = viewModel::deleteProposalPhoto
        onSubmit = viewModel::submitProposal
    }
}

class P21UnableFragment : ProviderFragment<P21UnableView>(R.id.scr_p21) {
    override fun create() = P21UnableView(requireContext())
    override fun P21UnableView.bind() {
        bindBack()
        onReasonSelect = viewModel::selectUnableReason
        onNoteChange = viewModel::updateUnableNote
        onConfirm = viewModel::confirmUnable
    }
}
