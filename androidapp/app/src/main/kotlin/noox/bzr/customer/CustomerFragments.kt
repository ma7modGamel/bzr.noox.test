package noox.bzr.customer

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.getSystemService
import androidx.fragment.app.activityViewModels
import noox.bzr.design.R as DesignR
import noox.bzr.gallery.R
import noox.bzr.screens.ScreenFragment

// One Fragment per customer screen (DEC-047). Each wires its view to CustomerViewModel exactly as
// CustomerJourneyFlow wired the Compose screen; no decision is taken here.

abstract class CustomerFragment<V : CustomerScreenView>(private val destination: Int) : ScreenFragment<CustomerUiState, V>() {
    protected val viewModel: CustomerViewModel by activityViewModels()
    override val states get() = viewModel.stateFlow
    override fun accepts(state: CustomerUiState) = CustomerRoutes.destination(state) == destination
    override fun V.render(state: CustomerUiState) = render(state)

    protected fun dayLabels() = listOf(
        DesignR.string.slot_day_today, DesignR.string.slot_day_tomorrow, DesignR.string.slot_day_saturday, DesignR.string.slot_day_sunday,
        DesignR.string.slot_day_monday, DesignR.string.slot_day_tuesday, DesignR.string.slot_day_wednesday, DesignR.string.slot_day_thursday,
    ).map(::getString)

    protected fun morning() = getString(DesignR.string.format_morning_short)
    protected fun evening() = getString(DesignR.string.format_evening_short)

    /** Picks an image and hands its bytes to [upload] (chat and dispute photos). */
    protected fun photoPicker(fallbackName: String, upload: (CustomerMediaUpload) -> Unit) =
        registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            uri ?: return@registerForActivityResult
            val resolver = requireContext().contentResolver
            val bytes = resolver.openInputStream(uri)?.use { it.readBytes() } ?: return@registerForActivityResult
            upload(CustomerMediaUpload(uri.lastPathSegment ?: fallbackName, resolver.getType(uri) ?: "image/jpeg", bytes))
        }
}

internal fun routeInput(screen: String): Map<String, Any?> = when (screen) {
    "SCR-C03" -> mapOf("event" to "validate", "problem_type_id" to "", "description" to "")
    "SCR-C04" -> mapOf("event" to "validate", "operating_mode" to "marketplace", "timing_type" to "now", "now_available" to true, "address_id" to "12")
    "SCR-C05" -> mapOf("event" to "validate", "operating_mode" to "marketplace", "terms_accepted" to false)
    "SCR-C09" -> mapOf("event" to "loaded", "status" to "CONFIRMED", "display_status" to "", "stepper" to true, "available_actions" to emptyList<String>())
    "SCR-C14" -> mapOf("event" to "loaded", "selection_mode" to true, "address_labels" to emptyList<String>(), "address_details" to emptyList<String>())
    "SCR-C16" -> mapOf("event" to "loaded", "day_labels" to emptyList<String>(), "slot_labels" to emptyList<String>())
    "SCR-C17" -> mapOf("event" to "validate", "media_kinds" to emptyList<String>(), "media_states" to emptyList<String>())
    else -> mapOf("event" to "loaded")
}

class C01HomeFragment : CustomerFragment<C01HomeView>(R.id.scr_c01) {
    override fun create() = C01HomeView(requireContext())
    override fun C01HomeView.bind() {
        onOpen = { route ->
            when (route) {
                "SCR-C09" -> viewModel.openOrder()
                "SCR-C14" -> viewModel.loadAddresses()
                "SCR-C18" -> viewModel.loadConversations()
                "SCR-C25" -> viewModel.loadOrders()
                "SCR-C29" -> viewModel.loadHelp()
                "SCR-C32" -> viewModel.loadNotifications()
                "SCR-C33" -> viewModel.loadAccount()
                "SCR-C34" -> viewModel.loadTerms()
                else -> viewModel.show(route, routeInput(route))
            }
        }
    }
}

class C02AccountFragment : CustomerFragment<C02AccountView>(R.id.scr_c02) {
    override fun create() = C02AccountView(requireContext())
    override fun C02AccountView.bind() {
        onOpen = { route ->
            when (route) {
                "SCR-C14" -> viewModel.loadAddresses(selectionMode = false)
                "SCR-C29" -> viewModel.loadHelp()
                "SCR-C32" -> viewModel.loadNotifications()
                "SCR-C33" -> viewModel.loadAccount()
                "SCR-C10" -> viewModel.logout()
                else -> viewModel.show(route, mapOf("event" to "loaded", "is_verified" to true))
            }
        }
    }
}

class C03ProblemFragment : CustomerFragment<C03ProblemView>(R.id.scr_c03) {
    override fun create() = C03ProblemView(requireContext())
    override fun C03ProblemView.bind() {
        onProblemSelect = { viewModel.show("SCR-C03", mapOf("event" to "validate", "problem_type_id" to "selected", "description" to "")) }
        onOpen = { route -> if (route == "SCR-C17") viewModel.openMedia() else viewModel.show(route, routeInput(route)) }
    }
}

class C04TimingFragment : CustomerFragment<C04TimingView>(R.id.scr_c04) {
    override fun create() = C04TimingView(requireContext())
    override fun C04TimingView.bind() {
        onOpen = { route ->
            when (route) {
                "SCR-C14" -> viewModel.loadAddresses()
                "SCR-C16" -> viewModel.loadSlots(dayLabels(), morning(), evening())
                else -> viewModel.show(route, routeInput(route))
            }
        }
    }
}

class C05ReviewFragment : CustomerFragment<C05ReviewView>(R.id.scr_c05) {
    override fun create() = C05ReviewView(requireContext())
    override fun C05ReviewView.bind() {
        onTermsChange = {
            val pricing = viewModel.state.showPricing
            viewModel.show("SCR-C05", mapOf("event" to "validate", "operating_mode" to if (pricing) "marketplace" else "staff", "terms_accepted" to true))
        }
        onPublish = viewModel::publishDraft
    }
}

class C06OffersFragment : CustomerFragment<C06OffersView>(R.id.scr_c06) {
    override fun create() = C06OffersView(requireContext())
    override fun C06OffersView.bind() = Unit
}

class C07ProviderFragment : CustomerFragment<C07ProviderView>(R.id.scr_c07) {
    override fun create() = C07ProviderView(requireContext())
    override fun C07ProviderView.bind() {
        onAction = { action -> if (action == "report_provider") viewModel.openProviderReport() }
    }
}

class C08OfferDetailsFragment : CustomerFragment<C08OfferDetailsView>(R.id.scr_c08) {
    override fun create() = C08OfferDetailsView(requireContext())
    override fun C08OfferDetailsView.bind() = Unit
}

class C09TrackingFragment : CustomerFragment<C09TrackingView>(R.id.scr_c09) {
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
    override fun create() = C15AddressFormView(requireContext())
    override fun C15AddressFormView.bind() {
        onAreaSelect = viewModel::selectArea
        onFieldChange = viewModel::updateAddressField
        onDefaultChange = viewModel::toggleAddressDefault
        onSave = viewModel::saveAddress
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
    override fun create() = C17MediaView(requireContext())
    override fun C17MediaView.bind() {
        onDelete = viewModel::deleteMedia
        onDone = viewModel::completeMediaSelection
    }
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

class C30ExecutionQuoteFragment : CustomerFragment<ProposalDecisionView>(R.id.scr_c30) {
    override fun create() = ProposalDecisionView(requireContext(), "SCR-C30")
    override fun ProposalDecisionView.bind() {
        onAction = viewModel::decideProposal
    }
}

class C31AdditionalCostFragment : CustomerFragment<ProposalDecisionView>(R.id.scr_c31) {
    override fun create() = ProposalDecisionView(requireContext(), "SCR-C31")
    override fun ProposalDecisionView.bind() {
        onAction = viewModel::decideProposal
    }
}

class C32NotificationsFragment : CustomerFragment<C32NotificationsView>(R.id.scr_c32) {
    override fun create() = C32NotificationsView(requireContext())
    override fun C32NotificationsView.bind() {
        onOpen = viewModel::openNotification
    }
}

class C33AccountSettingsFragment : CustomerFragment<C33AccountSettingsView>(R.id.scr_c33) {
    override fun create() = C33AccountSettingsView(requireContext())
    override fun C33AccountSettingsView.bind() {
        onAction = viewModel::accountAction
        onFieldChange = viewModel::updateAccountField
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
