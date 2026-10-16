import BzrCore
import DesignSystem
import SwiftUI
import UIKit

struct CustomerJourneyRoot: View {
    @Bindable var viewModel: CustomerViewModel
    let onLogout: () -> Void
    let onOpenProvider: () -> Void
    @State private var sharedVisit: CustomerVisitShareLink?
    @Environment(\.openURL) private var openURL

    /// DEC-062: C01, C25, C18, C02 share one bottom navigation under the screen, so its selection animates.
    private static let tabs = ["SCR-C01", "SCR-C25", "SCR-C18", "SCR-C02"]

    var body: some View {
        VStack(spacing: 0) {
            screen.frame(maxHeight: .infinity)
            if let tab = Self.tabs.firstIndex(of: viewModel.state.screen) {
                BottomNav(
                    items: [
                        (.home, bzrString("nav.home")), (.orders, bzrString("nav.orders")),
                        (.chat, bzrString("nav.messages")), (.account, bzrString("nav.account")),
                    ], selectedIndex: tab,
                    onSelect: { index in
                        if index != tab { open(Self.tabs[index]) }
                    })
            }
        }
        .alert(
            bzrString("account.logout.confirm.title"),
            isPresented: Binding(
                get: { viewModel.state.showLogoutConfirmation },
                set: { if !$0 { viewModel.dismissLogoutConfirmation() } }
            )
        ) {
            Button(bzrString("customer.account.logout"), role: .destructive) {
                _ = viewModel.confirmLogout()
                onLogout()
            }
            Button(bzrString("common.cancel"), role: .cancel) { viewModel.dismissLogoutConfirmation() }
        } message: {
            Text(bzrString("account.logout.confirm.body"))
        }
        .environment(\.customerRetry, retryAction)
        .task { await viewModel.loadHome() }
        .sheet(item: $sharedVisit) { link in
            if let url = URL(string: link.url) { VisitActivitySheet(url: url) }
        }
    }

    private var retryAction: (() -> Void)? {
        switch viewModel.state.screen {
        case "SCR-C01", "SCR-C02", "SCR-C09", "SCR-C18", "SCR-C29", "SCR-C32", "SCR-C33", "SCR-C34":
            return { open(viewModel.state.screen) }
        case "SCR-C14":
            return { Task { await viewModel.loadAddresses(selectionMode: viewModel.state.selectionMode) } }
        case "SCR-C25":
            return { Task { await viewModel.loadOrders(tabIndex: viewModel.state.selectedOptionIndex) } }
        default: return nil
        }
    }

    @ViewBuilder private var screen: some View {
        Group {
            switch viewModel.state.screen {
            case "SCR-C02": C02AccountView(state: viewModel.state, onOpen: open)
            case "SCR-C03":
                C03ServiceView(
                    state: viewModel.state,
                    onCategorySelect: viewModel.selectCategory,
                    onSearch: viewModel.searchServices,
                    onOpen: open)
            case "SCR-C37":
                C37ProblemView(
                    state: viewModel.state,
                    onProblemSelect: viewModel.selectProblem,
                    onDescriptionChange: viewModel.updateProblemDescription,
                    onOpen: open)
            case "SCR-C04":
                C04TimingView(state: viewModel.state, onOpen: open, onTimingSelect: viewModel.selectTiming, onMaterialSelect: viewModel.selectMaterial)
            case "SCR-C38":
                C38BudgetView(
                    state: viewModel.state,
                    onMaterialSelect: viewModel.selectMaterial,
                    onPricingSelect: viewModel.selectPricing,
                    onBudgetChange: viewModel.updateBudget,
                    onOpen: open)
            case "SCR-C05":
                C05ReviewView(
                    state: viewModel.state, onEdit: open,
                    onTermsChange: {
                        viewModel.setTermsAccepted(viewModel.state.termsError != nil)
                    },
                    onPublish: { Task { await viewModel.publishDraft() } })
            case "SCR-C06":
                if viewModel.state.phase == .empty {
                    C35NoOffersView(
                        state: CustomerLogic.reduce(
                            screen: "SCR-C35",
                            input: [
                                "event": .text("loaded"),
                                "available_actions": .strings(viewModel.state.visibleActions),
                                "summary_rows": .strings(viewModel.state.itemDetails),
                                "order_icon": .text(viewModel.state.optionIcons.first ?? ""),
                                "timing_type": .text(viewModel.state.showEta ? "now" : "scheduled"),
                            ]),
                        onAction: noOffersAction)
                } else {
                    C06OffersView(
                        state: viewModel.state,
                        onAction: { action in
                            if action == "refresh_arrival_code" {
                                Task { await viewModel.refreshArrivalCode() }
                            } else if action == "cancel" {
                                viewModel.openCancellation()
                            } else {
                                Task { await viewModel.noOffersAction(action) }
                            }
                        },
                        onSort: { index in Task { await viewModel.selectOffersSort(index: index) } },
                        onSelect: viewModel.openOffer,
                        onProvider: { index in Task { await viewModel.openOfferProvider(index: index) } })
                }
            case "SCR-C07":
                C07ProviderView(state: viewModel.state, loadImage: { await viewModel.loadProfileImage(url: $0) }) { action in
                    if action == "report_provider" {
                        viewModel.openProviderReport()
                    } else if action == "accept_offer" {
                        viewModel.openCurrentOffer()
                    }
                }
            case "SCR-C08":
                C08OfferDetailsView(
                    state: viewModel.state,
                    onAction: { action in
                        if action == "accept_offer" {
                            Task { await viewModel.confirmSelectedOffer() }
                        }
                    }, onPaymentSelect: viewModel.selectOfferPayment)
            case "SCR-C09": tracking
            case "SCR-C14":
                C14AddressesView(
                    state: viewModel.state,
                    onSelect: { index in Task { await viewModel.confirmAddress(index: index) } },
                    onEdit: { index in Task { await viewModel.openEditAddress(index: index) } },
                    onDelete: viewModel.requestDeleteAddress,
                    onMakeDefault: { index in Task { await viewModel.makeAddressDefault(index: index) } },
                    onCancelDelete: viewModel.cancelDeleteAddress,
                    onConfirmDelete: { Task { await viewModel.confirmDeleteAddress() } },
                    onAdd: { Task { await viewModel.openNewAddress() } },
                    onRefresh: { Task { await viewModel.loadAddresses(selectionMode: viewModel.state.selectionMode) } })
            case "SCR-C15":
                C15AddressFormView(
                    state: viewModel.state, onAreaSelect: viewModel.selectArea,
                    onFieldChange: viewModel.updateAddressField,
                    onLocationChange: viewModel.updateAddressLocation,
                    onDefaultChange: viewModel.toggleAddressDefault,
                    onSave: { Task { await viewModel.saveAddress() } })
            case "SCR-C16":
                C16SlotPickerView(
                    state: viewModel.state,
                    onDaySelect: { index in
                        Task {
                            await viewModel.selectDay(
                                index: index, dayLabels: slotDayLabels,
                                morning: bzrString("format.morning.short"),
                                evening: bzrString("format.evening.short"))
                        }
                    }, onSlotSelect: viewModel.selectSlot, onConfirm: viewModel.confirmSlot)
            case "SCR-C17":
                C17MediaView(
                    state: viewModel.state,
                    onDelete: { index in Task { await viewModel.deleteMedia(index: index) } },
                    onRetry: { index in Task { await viewModel.retryMedia(index: index) } },
                    onDone: viewModel.completeMediaSelection,
                    onUpload: { upload, kind in Task { await viewModel.uploadMedia(upload, kind: kind) } },
                    onRecordingChange: viewModel.setMediaRecording,
                    onRejected: { viewModel.rejectMedia(reason: $0) })
            case "SCR-C18":
                C18MessagesView(
                    state: viewModel.state,
                    onSelect: { index in Task { await viewModel.openConversation(index: index) } },
                    onOpen: open)
            case "SCR-C19":
                C19ChatView(
                    state: viewModel.state, loadImage: { await viewModel.loadProfileImage(url: $0) }, onDraftChange: viewModel.updateChatDraft,
                    onSend: { Task { await viewModel.sendChatMessage() } },
                    onPhotoSelected: { upload in Task { await viewModel.sendChatImage(upload) } })
            case "SCR-C20":
                C20CancellationView(
                    state: viewModel.state, onReasonSelect: viewModel.selectCancellationReason,
                    onNoteChange: viewModel.updateCancellationNote,
                    onConfirm: { Task { await viewModel.submitCancellation() } },
                    onBack: { Task { await viewModel.openCurrentOrder() } })
            case "SCR-C21":
                C21PaymentSummaryView(
                    state: viewModel.state, loadImage: { await viewModel.loadProfileImage(url: $0) },
                    onMethodSelect: { index in Task { await viewModel.selectPaymentMethod(index: index) } },
                    onPay: viewModel.openElectronicPayment,
                    onAction: { action in
                        if action == "open_dispute" { Task { await viewModel.openDispute() } }
                    })
            case "SCR-C22": electronicPayment
            case "SCR-C23":
                C23CompletionView(state: viewModel.state, loadImage: { await viewModel.loadProfileImage(url: $0) }) { action in
                    if action == "confirm_completion" {
                        Task { await viewModel.confirmCompletion() }
                    } else if action == "open_dispute" {
                        Task { await viewModel.openDispute() }
                    } else if action == "request_warranty" {
                        Task { await viewModel.openWarranty() }
                    }
                }
            case "SCR-C24":
                C24RatingView(
                    state: viewModel.state, onRate: viewModel.updateRating,
                    onOverall: viewModel.updateOverallRating, onTag: viewModel.toggleRatingTag,
                    onCommentChange: viewModel.updateRatingComment,
                    onSubmit: { Task { await viewModel.submitRating() } },
                    onPhotoSelected: { upload in Task { await viewModel.uploadRatingPhoto(upload) } },
                    onRemovePhoto: viewModel.removeRatingPhoto)
            case "SCR-C25":
                C25OrdersView(
                    state: viewModel.state,
                    onTabSelect: { index in Task { await viewModel.loadOrders(tabIndex: index) } },
                    onOrderSelect: { index in Task { await viewModel.selectOrder(index: index) } },
                    onChat: { index in Task { await viewModel.openOrderChat(index: index) } })
            case "SCR-C26":
                C26OrderHistoryView(state: viewModel.state, loadImage: { await viewModel.loadProfileImage(url: $0) }) { action in
                    if action == "rate" {
                        viewModel.openRating()
                    } else if action == "open_dispute" {
                        Task { await viewModel.openDispute() }
                    } else if action == "request_warranty" {
                        Task { await viewModel.openWarranty() }
                    }
                }
            case "SCR-C27":
                C27DisputeView(
                    state: viewModel.state, onReasonSelect: viewModel.selectSupportReason,
                    onDescriptionChange: viewModel.updateSupportDescription,
                    onPhotoSelected: { upload in Task { await viewModel.uploadDisputePhoto(upload) } },
                    onDeletePhoto: { index in Task { await viewModel.removeDisputePhoto(index: index) } },
                    onSubmit: { Task { await viewModel.submitDispute() } },
                    onOpenVisit: { Task { await viewModel.openWarrantyVisit() } })
            case "SCR-C28":
                C28ProviderReportView(
                    state: viewModel.state, onReasonSelect: viewModel.selectSupportReason,
                    onDescriptionChange: viewModel.updateSupportDescription,
                    onSubmit: { Task { await viewModel.submitProviderReport() } },
                    onBack: { Task { await viewModel.returnToProvider() } })
            case "SCR-C29":
                C29HelpView(
                    state: viewModel.state, onExpand: viewModel.toggleFAQ,
                    onSubjectChange: { viewModel.updateHelpField(index: 0, value: $0) },
                    onMessageChange: { viewModel.updateHelpField(index: 1, value: $0) },
                    onSubmit: { Task { await viewModel.submitHelpMessage() } })
            case "SCR-C30":
                C30ExecutionQuoteView(
                    state: viewModel.state, loadImage: { await viewModel.loadProfileImage(url: $0) },
                    onAction: proposalAction)
            case "SCR-C31":
                C31AdditionalCostView(
                    state: viewModel.state, loadImage: { await viewModel.loadProfileImage(url: $0) },
                    onAction: proposalAction)
            case "SCR-C32":
                C32NotificationsView(
                    state: viewModel.state,
                    onOpen: { index in Task { await viewModel.openNotification(index: index) } },
                    onOpenSettings: openNotificationSettings)
            case "SCR-C33":
                C33AccountSettingsView(
                    state: viewModel.state,
                    onAction: { action in
                        if action == "logout" {
                            viewModel.requestLogoutConfirmation()
                        } else {
                            Task {
                                await viewModel.accountAction(action)
                                if viewModel.accountDeleted { onLogout() }
                            }
                        }
                    }, onFieldChange: viewModel.updateAccountField,
                    onRatingRemindersChange: { enabled in
                        Task { await viewModel.setRatingReminders(enabled) }
                    })
            case "SCR-C34": C34TermsView(state: viewModel.state)
            case "SCR-C35":
                C35NoOffersView(
                    state: viewModel.state,
                    onAction: noOffersAction)
            case "SCR-C36":
                C36AssignmentView(
                    state: viewModel.state,
                    onAction: { action in
                        if action == "cancel" {
                            viewModel.openCancellation()
                        } else {
                            Task { await viewModel.noOffersAction(action) }
                        }
                    })
            default:
                C01HomeView(
                    state: viewModel.state, onOpen: open,
                    onCategorySelect: viewModel.selectCategory,
                    onOrderSelect: { index in Task { await viewModel.selectOrder(index: index) } },
                    onUrgent: viewModel.openUrgentRequest)
            }
        }
        .environment(\.customerBack) { Task { await viewModel.goBack() } }
    }

    private var tracking: some View {
        C09TrackingView(
            state: viewModel.state,
            tracking: viewModel.trackingPayload,
            destinationLatitude: viewModel.orderDestinationLatitude,
            destinationLongitude: viewModel.orderDestinationLongitude,
            onRefreshTracking: viewModel.refreshTracking
        ) { action in
            if action == "cancel" {
                viewModel.openCancellation()
            } else if ["approve_proposal", "reject_proposal"].contains(action) {
                Task { await viewModel.loadPendingProposal() }
            } else if ["change_payment_method", "pay_electronic"].contains(action) {
                Task { await viewModel.loadPaymentSummary() }
            } else if action == "confirm_completion" {
                Task { await viewModel.loadCompletionSummary() }
            } else if action == "rate" {
                viewModel.openRating()
            } else if action == "open_dispute" {
                Task { await viewModel.openDispute() }
            } else if action == "report_provider" {
                viewModel.openProviderReport()
            } else if action == "share_visit" {
                Task { sharedVisit = await viewModel.shareVisit() }
            } else if action == "chat" {
                Task { await viewModel.openCurrentOrderChat() }
            } else if action == "call", let phone = viewModel.currentProviderPhone, let url = URL(string: "tel:\(phone)") {
                openURL(url)
            } else if action == "refresh" {
                Task { await viewModel.openCurrentOrder() }
            } else if action == "help" {
                Task { await viewModel.loadHelp() }
            }
        }
    }

    private var electronicPayment: some View {
        C22ElectronicPaymentView(
            state: viewModel.state, onChannelSelect: viewModel.selectPaymentChannel,
            onCreate: { Task { await viewModel.createElectronicPayment() } },
            onOpenCheckout: {
                guard viewModel.state.items.indices.contains(2),
                    let url = URL(string: viewModel.state.items[2])
                else { return }
                openURL(url)
            },
            onCopyCode: {
                guard viewModel.state.items.indices.contains(1) else { return }
                UIPasteboard.general.string = viewModel.state.items[1]
            },
            onReceiptSelected: { upload in Task { await viewModel.uploadPaymentReceipt(upload) } },
            onReferenceChange: viewModel.updateTransferReference,
            onRefresh: { Task { await viewModel.refreshPayment() } })
    }

    // swiftlint:disable:next cyclomatic_complexity
    /// C30 / C31 (Figma 3002:2264 / 3002:4029): chat and dispute leave the decision; the rest decide it.
    private func proposalAction(_ action: String) {
        switch action {
        case "chat": Task { await viewModel.openCurrentOrderChat() }
        case "open_dispute": Task { await viewModel.openDispute() }
        default: Task { await viewModel.decideProposal(action: action) }
        }
    }

    /// C35 (Figma 3002:1519): «متابعة حالة الطلب» reloads the order, cancel opens the sheet, the rest edit or republish.
    private func noOffersAction(_ action: String) {
        switch action {
        case "cancel": viewModel.openCancellation()
        case "follow": Task { await viewModel.openCurrentOrder() }
        default: Task { await viewModel.noOffersAction(action) }
        }
    }

    private func open(_ screen: String) {
        switch screen {
        case "SCR-P01": if viewModel.state.visibleActions.contains("open_provider_mode") { onOpenProvider() }
        case "SCR-C09": Task { await viewModel.openCurrentOrder() }
        case "SCR-C10": viewModel.requestLogoutConfirmation()
        case "SCR-C14":
            Task { await viewModel.loadAddresses(selectionMode: viewModel.state.screen == "SCR-C04") }
        case "SCR-C15": Task { await viewModel.openNewAddress() }
        case "SCR-C16":
            Task {
                await viewModel.loadSlots(
                    dayLabels: slotDayLabels, morning: bzrString("format.morning.short"),
                    evening: bzrString("format.evening.short"))
            }
        case "SCR-C17": viewModel.openMedia()
        case "SCR-C03": viewModel.openProblem()
        case "SCR-C18": Task { await viewModel.loadConversations() }
        case "SCR-C25": Task { await viewModel.loadOrders(tabIndex: 0) }
        case "SCR-C29": Task { await viewModel.loadHelp() }
        case "SCR-C32": Task { await viewModel.loadNotifications() }
        case "SCR-C33": Task { await viewModel.loadAccount() }
        case "SCR-C02": Task { await viewModel.loadAccountSummary() }
        case "SCR-C01": Task { await viewModel.loadHome() }
        case "SCR-C37": viewModel.openProblemDetails()
        case "SCR-C04": viewModel.openTiming()
        case "SCR-C38": viewModel.openBudget()
        case "SCR-C05": viewModel.openReview()
        case "SCR-C34": Task { await viewModel.loadTerms() }
        default: viewModel.show(screen: screen, input: defaultInput(screen))
        }
    }

    private func defaultInput(_ screen: String) -> [String: CustomerInputValue] {
        switch screen {
        case "SCR-C03":
            ["event": .text("validate"), "problem_type_id": .text(""), "description": .text("")]
        case "SCR-C09":
            [
                "event": .text("loaded"), "status": .text("CONFIRMED"), "display_status": .text(""),
                "stepper": .bool(true),
            ]
        case "SCR-C14":
            [
                "event": .text("loaded"), "selection_mode": .bool(true), "address_labels": .strings([]),
                "address_details": .strings([]),
            ]
        case "SCR-C16":
            ["event": .text("loaded"), "day_labels": .strings([]), "slot_labels": .strings([])]
        case "SCR-C17":
            ["event": .text("validate"), "media_kinds": .strings([]), "media_states": .strings([])]
        default: ["event": .text("loaded")]
        }
    }

    private var slotDayLabels: [String] {
        [
            // Today, tomorrow, then Monday…Sunday; the view model names each date from these (6ب).
            "slot.day.today", "slot.day.tomorrow", "slot.day.monday", "slot.day.tuesday", "slot.day.wednesday",
            "slot.day.thursday", "slot.day.friday", "slot.day.saturday", "slot.day.sunday",
        ].map(bzrString)
    }
}

private struct VisitActivitySheet: UIViewControllerRepresentable {
    let url: URL

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: [url], applicationActivities: nil)
    }

    func updateUIViewController(_ controller: UIActivityViewController, context: Context) {}
}
