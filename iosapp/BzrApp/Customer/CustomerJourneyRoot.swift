import BzrCore
import DesignSystem
import SwiftUI
import UIKit

struct CustomerJourneyRoot: View {
    @Bindable var viewModel: CustomerViewModel
    let onLogout: () -> Void
    let onOpenProvider: () -> Void
    @Environment(\.openURL) private var openURL

    var body: some View {
        Group {
            switch viewModel.state.screen {
            case "SCR-C02": C02AccountView(state: viewModel.state, onOpen: open)
            case "SCR-C03":
                C03ProblemView(
                    state: viewModel.state,
                    onCategorySelect: viewModel.selectCategory,
                    onProblemSelect: viewModel.selectProblem,
                    onDescriptionChange: viewModel.updateProblemDescription,
                    onOpen: open)
            case "SCR-C04":
                C04TimingView(
                    state: viewModel.state, onOpen: open,
                    onTimingSelect: viewModel.selectTiming,
                    onMaterialSelect: viewModel.selectMaterial,
                    onPricingSelect: viewModel.selectPricing,
                    onBudgetChange: viewModel.updateBudget)
            case "SCR-C05":
                C05ReviewView(
                    state: viewModel.state,
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
                            ]),
                        onAction: { action in Task { await viewModel.noOffersAction(action) } })
                } else {
                    C06OffersView(
                        state: viewModel.state,
                        onAction: { action in
                            if action == "cancel" {
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
                C07ProviderView(state: viewModel.state) { action in
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
                    onCancelDelete: viewModel.cancelDeleteAddress,
                    onConfirmDelete: { Task { await viewModel.confirmDeleteAddress() } },
                    onAdd: { Task { await viewModel.openNewAddress() } })
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
                    onUpload: { upload, kind in Task { await viewModel.uploadMedia(upload, kind: kind) } },
                    onRecordingChange: viewModel.setMediaRecording,
                    onDone: viewModel.completeMediaSelection)
            case "SCR-C18":
                C18MessagesView(
                    state: viewModel.state,
                    onSelect: { index in Task { await viewModel.openConversation(index: index) } })
            case "SCR-C19":
                C19ChatView(
                    state: viewModel.state, onDraftChange: viewModel.updateChatDraft,
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
                    state: viewModel.state,
                    onMethodSelect: { index in Task { await viewModel.selectPaymentMethod(index: index) } },
                    onPay: viewModel.openElectronicPayment,
                    onAction: { action in
                        if action == "open_dispute" { Task { await viewModel.openDispute() } }
                    })
            case "SCR-C22": electronicPayment
            case "SCR-C23":
                C23CompletionView(state: viewModel.state) { action in
                    if action == "confirm_completion" {
                        Task { await viewModel.confirmCompletion() }
                    } else if action == "open_dispute" {
                        Task { await viewModel.openDispute() }
                    }
                }
            case "SCR-C24":
                C24RatingView(
                    state: viewModel.state, onRate: viewModel.updateRating,
                    onCommentChange: viewModel.updateRatingComment,
                    onSubmit: { Task { await viewModel.submitRating() } })
            case "SCR-C25":
                C25OrdersView(
                    state: viewModel.state,
                    onTabSelect: { index in Task { await viewModel.loadOrders(tabIndex: index) } },
                    onOrderSelect: { index in Task { await viewModel.selectOrder(index: index) } })
            case "SCR-C26":
                C26OrderHistoryView(state: viewModel.state) { action in
                    if action == "rate" {
                        viewModel.openRating()
                    } else if action == "open_dispute" {
                        Task { await viewModel.openDispute() }
                    }
                }
            case "SCR-C27":
                C27DisputeView(
                    state: viewModel.state, onReasonSelect: viewModel.selectSupportReason,
                    onDescriptionChange: viewModel.updateSupportDescription,
                    onPhotoSelected: { upload in Task { await viewModel.uploadDisputePhoto(upload) } },
                    onDeletePhoto: { index in Task { await viewModel.removeDisputePhoto(index: index) } },
                    onSubmit: { Task { await viewModel.submitDispute() } })
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
                    state: viewModel.state,
                    onAction: { action in Task { await viewModel.decideProposal(action: action) } })
            case "SCR-C31":
                C31AdditionalCostView(
                    state: viewModel.state,
                    onAction: { action in Task { await viewModel.decideProposal(action: action) } })
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
                            onLogout()
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
                    onAction: { action in Task { await viewModel.noOffersAction(action) } })
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
                    onOrderSelect: { index in Task { await viewModel.selectOrder(index: index) } })
            }
        }
        .environment(\.customerBack) { Task { await viewModel.goBack() } }
        .task { await viewModel.loadHome() }
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
            })
    }

    // swiftlint:disable:next cyclomatic_complexity
    private func open(_ screen: String) {
        switch screen {
        case "SCR-P01": onOpenProvider()
        case "SCR-C09": Task { await viewModel.openCurrentOrder() }
        case "SCR-C10": onLogout()
        case "SCR-C14":
            Task { await viewModel.loadAddresses(selectionMode: viewModel.state.screen == "SCR-C04") }
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
        case "SCR-C04": viewModel.openTiming()
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
