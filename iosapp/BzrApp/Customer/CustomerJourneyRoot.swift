import BzrCore
import DesignSystem
import SwiftUI
import UIKit

struct CustomerJourneyRoot: View {
    @Bindable var viewModel: CustomerViewModel
    let onLogout: () -> Void
    @Environment(\.openURL) private var openURL

    var body: some View {
        Group {
            switch viewModel.state.screen {
            case "SCR-C02": C02AccountView(state: viewModel.state, onOpen: open)
            case "SCR-C03":
                C03ProblemView(
                    state: viewModel.state,
                    onProblemSelect: {
                        viewModel.show(
                            screen: "SCR-C03",
                            input: [
                                "event": .text("validate"), "problem_type_id": .text("selected"),
                                "description": .text(""),
                            ])
                    }, onOpen: open)
            case "SCR-C04": C04TimingView(state: viewModel.state, onOpen: open)
            case "SCR-C05":
                C05ReviewView(
                    state: viewModel.state,
                    onTermsChange: {
                        viewModel.show(
                            screen: "SCR-C05",
                            input: [
                                "event": .text("validate"),
                                "operating_mode": .text(viewModel.state.showPricing ? "marketplace" : "staff"),
                                "terms_accepted": .bool(true),
                            ])
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
                    C06OffersView(state: viewModel.state)
                }
            case "SCR-C07":
                C07ProviderView(state: viewModel.state) { action in
                    if action == "report_provider" { viewModel.openProviderReport() }
                }
            case "SCR-C08": C08OfferDetailsView(state: viewModel.state)
            case "SCR-C09": tracking
            case "SCR-C14":
                C14AddressesView(
                    state: viewModel.state, onSelect: viewModel.confirmAddress,
                    onEdit: { index in Task { await viewModel.openEditAddress(index: index) } },
                    onDelete: viewModel.requestDeleteAddress,
                    onCancelDelete: viewModel.cancelDeleteAddress,
                    onConfirmDelete: { Task { await viewModel.confirmDeleteAddress() } },
                    onAdd: { Task { await viewModel.openNewAddress() } })
            case "SCR-C15":
                C15AddressFormView(
                    state: viewModel.state, onAreaSelect: viewModel.selectArea,
                    onFieldChange: viewModel.updateAddressField,
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
                    onOpen: { index in Task { await viewModel.openNotification(index: index) } })
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
                    }, onFieldChange: viewModel.updateAccountField)
            case "SCR-C34": C34TermsView(state: viewModel.state)
            case "SCR-C35":
                C35NoOffersView(
                    state: viewModel.state,
                    onAction: { action in Task { await viewModel.noOffersAction(action) } })
            default: C01HomeView(state: viewModel.state, onOpen: open)
            }
        }
        .task { await viewModel.loadHome() }
    }

    private var tracking: some View {
        C09TrackingView(state: viewModel.state) { action in
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
        case "SCR-C09": Task { await viewModel.openCurrentOrder() }
        case "SCR-C10": onLogout()
        case "SCR-C14": Task { await viewModel.loadAddresses(selectionMode: viewModel.state.screen == "SCR-C04") }
        case "SCR-C16":
            Task {
                await viewModel.loadSlots(
                    dayLabels: slotDayLabels, morning: bzrString("format.morning.short"),
                    evening: bzrString("format.evening.short"))
            }
        case "SCR-C17": viewModel.openMedia()
        case "SCR-C18": Task { await viewModel.loadConversations() }
        case "SCR-C25": Task { await viewModel.loadOrders(tabIndex: 0) }
        case "SCR-C29": Task { await viewModel.loadHelp() }
        case "SCR-C32": Task { await viewModel.loadNotifications() }
        case "SCR-C33": Task { await viewModel.loadAccount() }
        case "SCR-C34": Task { await viewModel.loadTerms() }
        default: viewModel.show(screen: screen, input: defaultInput(screen))
        }
    }

    private func defaultInput(_ screen: String) -> [String: CustomerInputValue] {
        switch screen {
        case "SCR-C03": ["event": .text("validate"), "problem_type_id": .text(""), "description": .text("")]
        case "SCR-C04": ["event": .text("validate"), "operating_mode": .text("marketplace"), "timing_type": .text("now"), "now_available": .bool(true), "address_id": .text("12")]
        case "SCR-C05": ["event": .text("validate"), "operating_mode": .text("marketplace"), "terms_accepted": .bool(false)]
        case "SCR-C09": ["event": .text("loaded"), "status": .text("CONFIRMED"), "display_status": .text(""), "stepper": .bool(true)]
        case "SCR-C14": ["event": .text("loaded"), "selection_mode": .bool(true), "address_labels": .strings([]), "address_details": .strings([])]
        case "SCR-C16": ["event": .text("loaded"), "day_labels": .strings([]), "slot_labels": .strings([])]
        case "SCR-C17": ["event": .text("validate"), "media_kinds": .strings([]), "media_states": .strings([])]
        default: ["event": .text("loaded")]
        }
    }

    private var slotDayLabels: [String] {
        [
            "slot.day.today", "slot.day.tomorrow", "slot.day.saturday", "slot.day.sunday",
            "slot.day.monday", "slot.day.tuesday", "slot.day.wednesday", "slot.day.thursday",
        ].map(bzrString)
    }
}
