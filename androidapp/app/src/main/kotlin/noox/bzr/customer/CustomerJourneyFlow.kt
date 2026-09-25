package noox.bzr.customer

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.AnnotatedString

@Composable
fun CustomerJourneyFlow(viewModel: CustomerViewModel) {
    LaunchedEffect(Unit) { viewModel.loadHome() }
    val state = viewModel.state
    val dayLabels = listOf(
        stringResource(noox.bzr.design.R.string.slot_day_today),
        stringResource(noox.bzr.design.R.string.slot_day_tomorrow),
        stringResource(noox.bzr.design.R.string.slot_day_saturday),
        stringResource(noox.bzr.design.R.string.slot_day_sunday),
        stringResource(noox.bzr.design.R.string.slot_day_monday),
        stringResource(noox.bzr.design.R.string.slot_day_tuesday),
        stringResource(noox.bzr.design.R.string.slot_day_wednesday),
        stringResource(noox.bzr.design.R.string.slot_day_thursday),
    )
    val morning = stringResource(noox.bzr.design.R.string.format_morning_short)
    val evening = stringResource(noox.bzr.design.R.string.format_evening_short)
    val uriHandler = LocalUriHandler.current
    val clipboard = LocalClipboardManager.current
    when (state.screen) {
        "SCR-C02" -> C02AccountScreen(state) { route ->
            when (route) {
                "SCR-C14" -> viewModel.loadAddresses(selectionMode = false)
                "SCR-C29" -> viewModel.loadHelp()
                "SCR-C32" -> viewModel.loadNotifications()
                "SCR-C33" -> viewModel.loadAccount()
                "SCR-C10" -> viewModel.logout()
                else -> viewModel.show(route, mapOf("event" to "loaded", "is_verified" to true))
            }
        }
        "SCR-C03" -> C03ProblemScreen(
            state,
            onProblemSelect = { viewModel.show("SCR-C03", mapOf("event" to "validate", "problem_type_id" to "selected", "description" to "")) },
            onOpen = { route ->
                if (route == "SCR-C17") viewModel.openMedia() else viewModel.show(route, defaultInput(route))
            },
        )
        "SCR-C04" -> C04TimingScreen(state) { route ->
            when (route) {
                "SCR-C14" -> viewModel.loadAddresses()
                "SCR-C16" -> viewModel.loadSlots(dayLabels, morning, evening)
                else -> viewModel.show(route, defaultInput(route))
            }
        }
        "SCR-C05" -> C05ReviewScreen(
            state,
            onTermsChange = { viewModel.show("SCR-C05", mapOf("event" to "validate", "operating_mode" to if (state.showPricing) "marketplace" else "staff", "terms_accepted" to true)) },
            onPublish = viewModel::publishDraft,
        )
        "SCR-C06" -> if (state.phase == CustomerPhase.Empty) {
            C35NoOffersScreen(CustomerLogic.reduce("SCR-C35", mapOf("event" to "loaded", "available_actions" to state.visibleActions)), viewModel::noOffersAction)
        } else C06OffersScreen(state)
        "SCR-C07" -> C07ProviderScreen(state) { action ->
            if (action == "report_provider") viewModel.openProviderReport()
        }
        "SCR-C08" -> C08OfferDetailsScreen(state)
        "SCR-C09" -> C09TrackingScreen(state) { action ->
            when (action) {
                "cancel" -> viewModel.openCancellation()
                "approve_proposal", "reject_proposal" -> viewModel.loadPendingProposal()
                "change_payment_method", "pay_electronic" -> viewModel.loadPaymentSummary()
                "confirm_completion" -> viewModel.loadCompletionSummary()
                "rate" -> viewModel.openRating()
                "open_dispute" -> viewModel.openDispute()
            }
        }
        "SCR-C14" -> C14AddressesScreen(
            state,
            onSelect = viewModel::confirmAddress,
            onEdit = viewModel::openEditAddress,
            onDelete = viewModel::requestDeleteAddress,
            onCancelDelete = viewModel::cancelDeleteAddress,
            onConfirmDelete = viewModel::confirmDeleteAddress,
            onAdd = viewModel::openNewAddress,
        )
        "SCR-C15" -> C15AddressFormScreen(
            state,
            onAreaSelect = viewModel::selectArea,
            onFieldChange = viewModel::updateAddressField,
            onDefaultChange = viewModel::toggleAddressDefault,
            onSave = viewModel::saveAddress,
        )
        "SCR-C16" -> C16SlotPickerScreen(
            state,
            onDaySelect = { viewModel.selectDay(it, dayLabels, morning, evening) },
            onSlotSelect = viewModel::selectSlot,
            onConfirm = viewModel::confirmSlot,
        )
        "SCR-C17" -> C17MediaScreen(
            state,
            onDelete = viewModel::deleteMedia,
            onDone = viewModel::completeMediaSelection,
        )
        "SCR-C18" -> C18MessagesScreen(state, viewModel::openConversation)
        "SCR-C19" -> C19ChatRoute(state, viewModel)
        "SCR-C20" -> C20CancellationScreen(
            state, viewModel::selectCancellationReason, viewModel::updateCancellationNote,
            viewModel::submitCancellation, viewModel::openOrder,
        )
        "SCR-C21" -> C21PaymentSummaryScreen(
            state, viewModel::selectPaymentMethod, viewModel::openElectronicPayment,
            onAction = { if (it == "open_dispute") viewModel.openDispute() },
        )
        "SCR-C22" -> C22ElectronicPaymentScreen(
            state = state,
            onChannelSelect = viewModel::selectPaymentChannel,
            onCreate = viewModel::createElectronicPayment,
            onOpenCheckout = { state.items.getOrNull(2)?.takeIf(String::isNotBlank)?.let(uriHandler::openUri) },
            onCopyCode = { state.items.getOrNull(1)?.let { clipboard.setText(AnnotatedString(it)) } },
        )
        "SCR-C23" -> C23CompletionScreen(state) { action ->
            if (action == "confirm_completion") viewModel.confirmCompletion()
            else if (action == "open_dispute") viewModel.openDispute()
        }
        "SCR-C24" -> C24RatingScreen(
            state, viewModel::updateRating, viewModel::updateRatingComment, viewModel::submitRating,
        )
        "SCR-C25" -> C25OrdersScreen(state, viewModel::loadOrders, viewModel::selectOrder)
        "SCR-C26" -> C26OrderHistoryScreen(state) { action ->
            when (action) {
                "rate" -> viewModel.openRating()
                "open_dispute" -> viewModel.openDispute()
            }
        }
        "SCR-C27" -> C27DisputeRoute(state, viewModel)
        "SCR-C28" -> C28ProviderReportScreen(
            state,
            viewModel::selectSupportReason,
            viewModel::updateSupportDescription,
            viewModel::submitProviderReport,
            viewModel::openOrder,
        )
        "SCR-C29" -> C29HelpScreen(
            state, viewModel::toggleFaq, { viewModel.updateHelpField(0, it) },
            { viewModel.updateHelpField(1, it) }, viewModel::submitHelpMessage,
        )
        "SCR-C30" -> C30ExecutionQuoteScreen(state, viewModel::decideProposal)
        "SCR-C31" -> C31AdditionalCostScreen(state, viewModel::decideProposal)
        "SCR-C32" -> C32NotificationsScreen(state, viewModel::openNotification)
        "SCR-C33" -> C33AccountSettingsScreen(state, viewModel::accountAction, viewModel::updateAccountField)
        "SCR-C34" -> C34TermsScreen(state)
        "SCR-C35" -> C35NoOffersScreen(state, viewModel::noOffersAction)
        else -> C01HomeScreen(state) { route ->
            when (route) {
                "SCR-C09" -> viewModel.openOrder()
                "SCR-C14" -> viewModel.loadAddresses()
                "SCR-C18" -> viewModel.loadConversations()
                "SCR-C25" -> viewModel.loadOrders()
                "SCR-C29" -> viewModel.loadHelp()
                "SCR-C32" -> viewModel.loadNotifications()
                "SCR-C33" -> viewModel.loadAccount()
                "SCR-C34" -> viewModel.loadTerms()
                else -> viewModel.show(route, defaultInput(route))
            }
        }
    }
}

@Composable
private fun C27DisputeRoute(state: CustomerUiState, viewModel: CustomerViewModel) {
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@rememberLauncherForActivityResult
        viewModel.uploadDisputePhoto(
            CustomerMediaUpload(
                uri.lastPathSegment ?: "dispute-image.jpg",
                context.contentResolver.getType(uri) ?: "image/jpeg",
                bytes,
            ),
        )
    }
    C27DisputeScreen(
        state,
        viewModel::selectSupportReason,
        viewModel::updateSupportDescription,
        onAddPhoto = { photoPicker.launch("image/*") },
        onDeletePhoto = viewModel::removeDisputePhoto,
        onSubmit = viewModel::submitDispute,
    )
}

@Composable
private fun C19ChatRoute(state: CustomerUiState, viewModel: CustomerViewModel) {
    val context = LocalContext.current
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri ?: return@rememberLauncherForActivityResult
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return@rememberLauncherForActivityResult
        viewModel.sendChatImage(
            CustomerMediaUpload(
                uri.lastPathSegment ?: "chat-image.jpg",
                context.contentResolver.getType(uri) ?: "image/jpeg",
                bytes,
            ),
        )
    }
    C19ChatScreen(
        state, viewModel::updateChatDraft, viewModel::sendChatMessage,
        onAddPhoto = { photoPicker.launch("image/*") },
    )
}

private fun defaultInput(screen: String): Map<String, Any?> = when (screen) {
    "SCR-C03" -> mapOf("event" to "validate", "problem_type_id" to "", "description" to "")
    "SCR-C04" -> mapOf("event" to "validate", "operating_mode" to "marketplace", "timing_type" to "now", "now_available" to true, "address_id" to "12")
    "SCR-C05" -> mapOf("event" to "validate", "operating_mode" to "marketplace", "terms_accepted" to false)
    "SCR-C09" -> mapOf("event" to "loaded", "status" to "CONFIRMED", "display_status" to "", "stepper" to true, "available_actions" to emptyList<String>())
    "SCR-C14" -> mapOf("event" to "loaded", "selection_mode" to true, "address_labels" to emptyList<String>(), "address_details" to emptyList<String>())
    "SCR-C16" -> mapOf("event" to "loaded", "day_labels" to emptyList<String>(), "slot_labels" to emptyList<String>())
    "SCR-C17" -> mapOf("event" to "validate", "media_kinds" to emptyList<String>(), "media_states" to emptyList<String>())
    else -> mapOf("event" to "loaded")
}
