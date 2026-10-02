package noox.bzr.customer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.app.Activity
import android.net.Uri
import noox.bzr.links.openNotificationSettings
import android.Manifest
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.getSystemService
import androidx.core.content.FileProvider
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import noox.bzr.design.R as DesignR
import noox.bzr.gallery.R
import noox.bzr.maps.NativeGoogleMapController
import noox.bzr.media.AndroidAudioRecorder
import noox.bzr.media.AudioRecordingSpec
import noox.bzr.screens.ScreenFragment
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.InputStream

// One Fragment per customer screen (DEC-047). Each wires its view to CustomerViewModel exactly as
// CustomerJourneyFlow wired the Compose screen; no decision is taken here.

abstract class CustomerFragment<V : CustomerScreenView>(private val destination: Int) : ScreenFragment<CustomerUiState, V>() {
    protected val viewModel: CustomerViewModel by activityViewModels()
    override val states get() = viewModel.stateFlow
    override fun accepts(state: CustomerUiState) = CustomerRoutes.destination(state) == destination
    override fun V.render(state: CustomerUiState) = render(state)

    /** Every customer screen's top-bar back goes through the shared history (6ب). */
    override fun onViewCreated(view: android.view.View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        screenView.onBack = { viewModel.goBack() }
    }

    /** Today, tomorrow, then Monday…Sunday; the ViewModel names each date from these (6ب). */
    protected fun dayLabels() = listOf(
        DesignR.string.slot_day_today, DesignR.string.slot_day_tomorrow, DesignR.string.slot_day_monday, DesignR.string.slot_day_tuesday,
        DesignR.string.slot_day_wednesday, DesignR.string.slot_day_thursday, DesignR.string.slot_day_friday,
        DesignR.string.slot_day_saturday, DesignR.string.slot_day_sunday,
    ).map(::getString)

    protected fun morning() = getString(DesignR.string.format_morning_short)
    protected fun evening() = getString(DesignR.string.format_evening_short)

    /** Picks an image and hands its bytes to [upload] (chat and dispute photos). */
    protected fun photoPicker(fallbackName: String, upload: (CustomerMediaUpload) -> Unit) =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult
            val resolver = requireContext().contentResolver
            val bytes = resolver.openInputStream(uri)?.use { it.readUpTo(MAX_IMAGE_BYTES + 1) } ?: return@registerForActivityResult
            if (bytes.size > MAX_IMAGE_BYTES) return@registerForActivityResult
            upload(CustomerMediaUpload(uri.lastPathSegment ?: fallbackName, resolver.getType(uri) ?: "image/jpeg", bytes))
        }

    protected fun mediaUpload(uri: Uri, fallbackName: String, fallbackMime: String, maxBytes: Int): CustomerMediaUpload? {
        val resolver = requireContext().contentResolver
        val bytes = resolver.openInputStream(uri)?.use { it.readUpTo(maxBytes + 1) } ?: return null
        if (bytes.size > maxBytes) return null
        return CustomerMediaUpload(uri.lastPathSegment ?: fallbackName, resolver.getType(uri) ?: fallbackMime, bytes)
    }

    private companion object {
        const val MAX_IMAGE_BYTES = 10 * 1024 * 1024
    }
}

internal fun routeInput(screen: String): Map<String, Any?> = when (screen) {
    "SCR-C03" -> mapOf("event" to "validate", "problem_type_id" to "", "description" to "")
    "SCR-C09" -> mapOf("event" to "loaded", "status" to "CONFIRMED", "display_status" to "", "stepper" to true, "available_actions" to emptyList<String>())
    "SCR-C14" -> mapOf("event" to "loaded", "selection_mode" to true, "address_labels" to emptyList<String>(), "address_details" to emptyList<String>())
    "SCR-C16" -> mapOf("event" to "loaded", "day_labels" to emptyList<String>(), "slot_labels" to emptyList<String>())
    "SCR-C17" -> mapOf("event" to "validate", "media_kinds" to emptyList<String>(), "media_states" to emptyList<String>())
    else -> mapOf("event" to "loaded")
}

class C01HomeFragment : CustomerFragment<C01HomeView>(R.id.scr_c01) {
    override fun create() = C01HomeView(requireContext())
    override fun C01HomeView.bind() {
        onCategorySelect = viewModel::selectCategory
        onOrderSelect = viewModel::selectOrder
        onOpen = { route ->
            when (route) {
                "SCR-C03" -> viewModel.openProblem()
                "SCR-C09" -> viewModel.openOrder()
                "SCR-C14" -> viewModel.loadAddresses()
                "SCR-C18" -> viewModel.loadConversations()
                "SCR-C25" -> viewModel.loadOrders()
                "SCR-C29" -> viewModel.loadHelp()
                "SCR-C32" -> viewModel.loadNotifications()
                "SCR-C33" -> viewModel.loadAccount()
                "SCR-C34" -> viewModel.loadTerms()
                "SCR-C02" -> viewModel.loadAccountSummary()
                else -> viewModel.show(route, routeInput(route))
            }
        }
    }
}

class C02AccountFragment : CustomerFragment<C02AccountView>(R.id.scr_c02) {
    private val providerViewModel: noox.bzr.provider.ProviderViewModel by activityViewModels()
    override fun create() = C02AccountView(requireContext())
    override fun C02AccountView.bind() {
        onOpen = { route ->
            when (route) {
                "SCR-C14" -> viewModel.loadAddresses(selectionMode = false)
                "SCR-C29" -> viewModel.loadHelp()
                "SCR-C32" -> viewModel.loadNotifications()
                "SCR-C33" -> viewModel.loadAccount()
                "SCR-C10" -> viewModel.logout()
                "SCR-P01" -> providerViewModel.openApplication()
                else -> viewModel.loadAccountSummary()
            }
        }
    }
}

class C03ProblemFragment : CustomerFragment<C03ProblemView>(R.id.scr_c03) {
    override fun create() = C03ProblemView(requireContext())
    override fun C03ProblemView.bind() {
        onCategorySelect = viewModel::selectCategory
        onProblemSelect = viewModel::selectProblem
        onDescriptionChange = viewModel::updateProblemDescription
        onOpen = { route ->
            when (route) {
                "SCR-C17" -> viewModel.openMedia()
                "SCR-C04" -> viewModel.openTiming()
                else -> viewModel.show(route, routeInput(route))
            }
        }
    }
}

class C04TimingFragment : CustomerFragment<C04TimingView>(R.id.scr_c04) {
    override fun create() = C04TimingView(requireContext())
    override fun C04TimingView.bind() {
        onTimingSelect = viewModel::selectTiming
        onMaterialSelect = viewModel::selectMaterial
        onPricingSelect = viewModel::selectPricing
        onBudgetChange = viewModel::updateBudget
        onOpen = { route ->
            when (route) {
                "SCR-C14" -> viewModel.loadAddresses()
                "SCR-C16" -> viewModel.loadSlots(dayLabels(), morning(), evening())
                "SCR-C05" -> viewModel.openReview()
                else -> viewModel.show(route, routeInput(route))
            }
        }
    }
}

class C05ReviewFragment : CustomerFragment<C05ReviewView>(R.id.scr_c05) {
    override fun create() = C05ReviewView(requireContext())
    override fun C05ReviewView.bind() {
        onTermsChange = {
            viewModel.setTermsAccepted(viewModel.state.termsError != null)
        }
        onPublish = viewModel::publishDraft
    }
}

class C06OffersFragment : CustomerFragment<C06OffersView>(R.id.scr_c06) {
    override fun create() = C06OffersView(requireContext())
    override fun C06OffersView.bind() {
        onSort = viewModel::selectOffersSort
        onSelect = viewModel::openOffer
        onProvider = viewModel::openOfferProvider
        onAction = { action ->
            when (action) {
                "cancel" -> viewModel.openCancellation()
                "edit_request", "republish" -> viewModel.noOffersAction(action)
            }
        }
    }
}

class C07ProviderFragment : CustomerFragment<C07ProviderView>(R.id.scr_c07) {
    override fun create() = C07ProviderView(requireContext())
    override fun C07ProviderView.bind() {
        onAction = { action ->
            when (action) {
                "report_provider" -> viewModel.openProviderReport()
                "accept_offer" -> viewModel.openCurrentOffer()
            }
        }
    }
}

class C08OfferDetailsFragment : CustomerFragment<C08OfferDetailsView>(R.id.scr_c08) {
    override fun create() = C08OfferDetailsView(requireContext())
    override fun C08OfferDetailsView.bind() {
        onPaymentSelect = viewModel::selectOfferPayment
        onAction = { action -> if (action == "accept_offer") viewModel.confirmSelectedOffer() }
    }
}

class C09TrackingFragment : CustomerFragment<C09TrackingView>(R.id.scr_c09) {
    private var mapController: NativeGoogleMapController? = null

    override fun create() = C09TrackingView(requireContext())
    override fun C09TrackingView.bind() {
        onAction = { action ->
            when (action) {
                "cancel" -> viewModel.openCancellation()
                "approve_proposal", "reject_proposal" -> viewModel.loadPendingProposal()
                "change_payment_method", "pay_electronic" -> viewModel.loadPaymentSummary()
                "confirm_completion" -> viewModel.loadCompletionSummary()
                "rate" -> viewModel.openRating()
                "open_dispute" -> viewModel.openDispute()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val controller = NativeGoogleMapController(requireContext(), selectable = false)
        mapController = controller
        viewLifecycleOwner.lifecycle.addObserver(controller)
        screenView.attachNativeMap(controller.view) {
            viewModel.refreshTracking { tracking, destination ->
                controller.showTracking(
                    tracking.latitude?.let { latitude -> tracking.longitude?.let { LatLng(latitude, it) } },
                    destination?.let { LatLng(it.first, it.second) },
                )
            }
        }
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.stateFlow
                    .map { it.showMap }
                    .distinctUntilChanged()
                    .collectLatest { showMap ->
                        if (!showMap) return@collectLatest
                        while (true) {
                        viewModel.refreshTracking { tracking, destination ->
                            controller.showTracking(
                                tracking.latitude?.let { latitude -> tracking.longitude?.let { LatLng(latitude, it) } },
                                destination?.let { LatLng(it.first, it.second) },
                            )
                            screenView.updateEta(tracking.etaMinutes, tracking.etaApproximate)
                        }
                            delay(TRACKING_REFRESH_MILLIS)
                        }
                    }
            }
        }
    }

    override fun onDestroyView() {
        mapController = null
        super.onDestroyView()
    }

    private companion object {
        const val TRACKING_REFRESH_MILLIS = 30_000L
    }
}

class C14AddressesFragment : CustomerFragment<C14AddressesView>(R.id.scr_c14) {
    override fun create() = C14AddressesView(requireContext())
    override fun C14AddressesView.bind() {
        onSelect = viewModel::confirmAddress
        onEdit = viewModel::openEditAddress
        onDelete = viewModel::requestDeleteAddress
        onCancelDelete = viewModel::cancelDeleteAddress
        onConfirmDelete = viewModel::confirmDeleteAddress
        onAdd = viewModel::openNewAddress
    }
}

class C15AddressFormFragment : CustomerFragment<C15AddressFormView>(R.id.scr_c15) {
    private var mapController: NativeGoogleMapController? = null
    private val locationPermission = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        if (result.values.any { it }) mapController?.centerOnCurrentLocation(requireContext())
    }

    override fun create() = C15AddressFormView(requireContext())
    override fun C15AddressFormView.bind() {
        onAreaSelect = viewModel::selectArea
        onFieldChange = viewModel::updateAddressField
        onDefaultChange = viewModel::toggleAddressDefault
        onSave = viewModel::saveAddress
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val controller = NativeGoogleMapController(requireContext(), selectable = true, onSelected = viewModel::updateAddressLocation)
        mapController = controller
        viewLifecycleOwner.lifecycle.addObserver(controller)
        screenView.attachNativeMap(controller.view) {
            locationPermission.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        }
        screenView.onMapStateRender = controller::showSelection
    }

    override fun onDestroyView() {
        mapController = null
        super.onDestroyView()
    }
}

class C16SlotPickerFragment : CustomerFragment<C16SlotPickerView>(R.id.scr_c16) {
    override fun create() = C16SlotPickerView(requireContext())
    override fun C16SlotPickerView.bind() {
        onDaySelect = { viewModel.selectDay(it, dayLabels(), morning(), evening()) }
        onSlotSelect = viewModel::selectSlot
        onConfirm = viewModel::confirmSlot
    }
}

class C17MediaFragment : CustomerFragment<C17MediaView>(R.id.scr_c17) {
    private val recorder = AndroidAudioRecorder()
    private var capturedPhoto: Uri? = null

    private val photoChooser = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode != Activity.RESULT_OK) return@registerForActivityResult
        if (!viewModel.canAddMedia("photo")) return@registerForActivityResult
        val uri = result.data?.data ?: capturedPhoto ?: return@registerForActivityResult
        mediaUpload(uri, "bremo-photo.jpg", "image/jpeg", MAX_IMAGE_BYTES)?.let { viewModel.uploadMedia(it, "photo") }
    }
    private val videoPicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@registerForActivityResult
        if (!viewModel.canAddMedia("video")) return@registerForActivityResult
        mediaUpload(uri, "bremo-video.mp4", "video/mp4", MAX_VIDEO_BYTES)?.let { viewModel.uploadMedia(it, "video") }
    }
    private val audioPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startRecording()
    }

    override fun create() = C17MediaView(requireContext())
    override fun C17MediaView.bind() {
        onPickPhoto = ::launchPhotoChooser
        onPickVideo = { videoPicker.launch("video/*") }
        onRecordAudio = ::toggleRecording
        onDelete = viewModel::deleteMedia
        onRetry = viewModel::retryMedia
        onDone = viewModel::completeMediaSelection
    }

    private fun launchPhotoChooser() {
        if (!viewModel.canAddMedia("photo")) return
        val directory = File(requireContext().cacheDir, "captured-media").apply { mkdirs() }
        val output = File.createTempFile("bremo-photo-", ".jpg", directory)
        val outputUri = FileProvider.getUriForFile(requireContext(), "${requireContext().packageName}.files", output)
        capturedPhoto = outputUri
        val gallery = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "image/*"
        }
        val camera = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
            putExtra(MediaStore.EXTRA_OUTPUT, outputUri)
            putExtra(Intent.EXTRA_TITLE, getString(DesignR.string.media_source_camera))
            addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        photoChooser.launch(Intent.createChooser(gallery, getString(DesignR.string.media_source_title)).apply {
            putExtra(Intent.EXTRA_INITIAL_INTENTS, arrayOf(camera))
        })
    }

    private fun toggleRecording() {
        if (recorder.isRecording) {
            stopAndUploadRecording()
        } else if (!viewModel.canAddMedia("audio")) {
            return
        } else if (requireContext().checkSelfPermission(Manifest.permission.RECORD_AUDIO) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            startRecording()
        } else {
            audioPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    private fun startRecording() {
        runCatching {
            recorder.start(requireContext()) {
                activity?.runOnUiThread(::stopAndUploadRecording)
            }
            viewModel.setMediaRecording(true)
        }.onFailure { viewModel.setMediaRecording(false) }
    }

    private fun stopAndUploadRecording() {
        val file = recorder.stop()
        viewModel.setMediaRecording(false)
        if (file == null) return
        val bytes = file.inputStream().use { it.readUpTo(AudioRecordingSpec.maxFileSizeBytes.toInt() + 1) }
        file.delete()
        if (bytes.size > AudioRecordingSpec.maxFileSizeBytes) return
        viewModel.uploadMedia(
            CustomerMediaUpload(file.name, AudioRecordingSpec.mimeType, bytes),
            "audio",
        )
    }

    override fun onDestroyView() {
        if (recorder.isRecording) {
            recorder.cancel()
            viewModel.setMediaRecording(false)
        }
        super.onDestroyView()
    }

    private companion object {
        const val MAX_IMAGE_BYTES = 10 * 1024 * 1024
        const val MAX_VIDEO_BYTES = 50 * 1024 * 1024
    }
}

private fun InputStream.readUpTo(byteLimit: Int): ByteArray {
    val output = ByteArrayOutputStream(minOf(byteLimit, DEFAULT_BUFFER_SIZE))
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var remaining = byteLimit

    while (remaining > 0) {
        val count = read(buffer, 0, minOf(buffer.size, remaining))
        if (count < 0) break
        output.write(buffer, 0, count)
        remaining -= count
    }

    return output.toByteArray()
}

class C18MessagesFragment : CustomerFragment<C18MessagesView>(R.id.scr_c18) {
    override fun create() = C18MessagesView(requireContext())
    override fun C18MessagesView.bind() {
        onSelect = viewModel::openConversation
    }
}

class C19ChatFragment : CustomerFragment<C19ChatView>(R.id.scr_c19) {
    private val picker = photoPicker("chat-image.jpg") { viewModel.sendChatImage(it) }
    override fun create() = C19ChatView(requireContext())
    override fun C19ChatView.bind() {
        onDraftChange = viewModel::updateChatDraft
        onSend = viewModel::sendChatMessage
        onAddPhoto = { picker.launch("image/*") }
    }
}

class C20CancellationFragment : CustomerFragment<C20CancellationView>(R.id.scr_c20) {
    override fun create() = C20CancellationView(requireContext())
    override fun C20CancellationView.bind() {
        onReasonSelect = viewModel::selectCancellationReason
        onNoteChange = viewModel::updateCancellationNote
        onConfirm = viewModel::submitCancellation
        onBackAction = viewModel::openOrder
    }
}

class C21PaymentSummaryFragment : CustomerFragment<C21PaymentSummaryView>(R.id.scr_c21) {
    override fun create() = C21PaymentSummaryView(requireContext())
    override fun C21PaymentSummaryView.bind() {
        onMethodSelect = viewModel::selectPaymentMethod
        onPay = viewModel::openElectronicPayment
        onAction = { if (it == "open_dispute") viewModel.openDispute() }
    }
}

class C22ElectronicPaymentFragment : CustomerFragment<C22ElectronicPaymentView>(R.id.scr_c22) {
    override fun create() = C22ElectronicPaymentView(requireContext())
    override fun C22ElectronicPaymentView.bind() {
        onChannelSelect = viewModel::selectPaymentChannel
        onCreate = viewModel::createElectronicPayment
        onOpenCheckout = {
            viewModel.state.items.getOrNull(2)?.takeIf(String::isNotBlank)?.let { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(it))) }
        }
        onCopyCode = {
            viewModel.state.items.getOrNull(1)?.let { code ->
                requireContext().getSystemService<ClipboardManager>()?.setPrimaryClip(ClipData.newPlainText(code, code))
            }
        }
    }
}

class C23CompletionFragment : CustomerFragment<C23CompletionView>(R.id.scr_c23) {
    override fun create() = C23CompletionView(requireContext())
    override fun C23CompletionView.bind() {
        onAction = { action ->
            if (action == "confirm_completion") viewModel.confirmCompletion() else if (action == "open_dispute") viewModel.openDispute()
        }
    }
}

class C24RatingFragment : CustomerFragment<C24RatingView>(R.id.scr_c24) {
    override fun create() = C24RatingView(requireContext())
    override fun C24RatingView.bind() {
        onRate = viewModel::updateRating
        onCommentChange = viewModel::updateRatingComment
        onSubmit = viewModel::submitRating
    }
}

class C25OrdersFragment : CustomerFragment<C25OrdersView>(R.id.scr_c25) {
    override fun create() = C25OrdersView(requireContext())
    override fun C25OrdersView.bind() {
        onTabSelect = { viewModel.loadOrders(it) }
        onOrderSelect = viewModel::selectOrder
    }
}

class C26OrderHistoryFragment : CustomerFragment<C26OrderHistoryView>(R.id.scr_c26) {
    override fun create() = C26OrderHistoryView(requireContext())
    override fun C26OrderHistoryView.bind() {
        onAction = { action ->
            when (action) {
                "rate" -> viewModel.openRating()
                "open_dispute" -> viewModel.openDispute()
            }
        }
    }
}

class C27DisputeFragment : CustomerFragment<C27DisputeView>(R.id.scr_c27) {
    private val picker = photoPicker("dispute-image.jpg") { viewModel.uploadDisputePhoto(it) }
    override fun create() = C27DisputeView(requireContext())
    override fun C27DisputeView.bind() {
        onReasonSelect = viewModel::selectSupportReason
        onDescriptionChange = viewModel::updateSupportDescription
        onAddPhoto = { picker.launch("image/*") }
        onDeletePhoto = viewModel::removeDisputePhoto
        onSubmit = viewModel::submitDispute
    }
}

class C28ProviderReportFragment : CustomerFragment<C28ProviderReportView>(R.id.scr_c28) {
    override fun create() = C28ProviderReportView(requireContext())
    override fun C28ProviderReportView.bind() {
        onReasonSelect = viewModel::selectSupportReason
        onDescriptionChange = viewModel::updateSupportDescription
        onSubmit = viewModel::submitProviderReport
        onBackAction = viewModel::openOrder
    }
}

class C29HelpFragment : CustomerFragment<C29HelpView>(R.id.scr_c29) {
    override fun create() = C29HelpView(requireContext())
    override fun C29HelpView.bind() {
        onExpand = viewModel::toggleFaq
        onSubjectChange = { viewModel.updateHelpField(0, it) }
        onMessageChange = { viewModel.updateHelpField(1, it) }
        onSubmit = viewModel::submitHelpMessage
    }
}

class C30ExecutionQuoteFragment : CustomerFragment<C30ExecutionQuoteView>(R.id.scr_c30) {
    override fun create() = C30ExecutionQuoteView(requireContext())
    override fun C30ExecutionQuoteView.bind() {
        onAction = viewModel::decideProposal
    }
}

class C31AdditionalCostFragment : CustomerFragment<C31AdditionalCostView>(R.id.scr_c31) {
    override fun create() = C31AdditionalCostView(requireContext())
    override fun C31AdditionalCostView.bind() {
        onAction = viewModel::decideProposal
    }
}

class C32NotificationsFragment : CustomerFragment<C32NotificationsView>(R.id.scr_c32) {
    override fun create() = C32NotificationsView(requireContext())
    override fun C32NotificationsView.bind() {
        onOpen = viewModel::openNotification
        onOpenSettings = { openNotificationSettings(requireContext()) }
    }
}

class C33AccountSettingsFragment : CustomerFragment<C33AccountSettingsView>(R.id.scr_c33) {
    override fun create() = C33AccountSettingsView(requireContext())
    override fun C33AccountSettingsView.bind() {
        onAction = viewModel::accountAction
        onFieldChange = viewModel::updateAccountField
        onRatingRemindersChange = viewModel::setRatingReminders
    }
}

class C34TermsFragment : CustomerFragment<C34TermsView>(R.id.scr_c34) {
    override fun create() = C34TermsView(requireContext())
    override fun C34TermsView.bind() = Unit
}

class C35NoOffersFragment : CustomerFragment<C35NoOffersView>(R.id.scr_c35) {
    override fun create() = C35NoOffersView(requireContext())
    override fun C35NoOffersView.bind() {
        onAction = viewModel::noOffersAction
    }
    override fun C35NoOffersView.render(state: CustomerUiState) = render(CustomerRoutes.noOffersState(state))
}

class C36AssignmentFragment : CustomerFragment<C36AssignmentView>(R.id.scr_c36) {
    override fun create() = C36AssignmentView(requireContext())
    override fun C36AssignmentView.bind() {
        onAction = { action ->
            when (action) {
                "edit_request" -> viewModel.noOffersAction(action)
                "cancel" -> viewModel.openCancellation()
            }
        }
    }
}
