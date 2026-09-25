package noox.bzr.customer

import android.content.Context

/** The XML View of each customer screen code (DEC-047); fragments and snapshot tests share it. */
fun customerScreenView(context: Context, screen: String): CustomerScreenView = when (screen) {
    "SCR-C01" -> C01HomeView(context)
    "SCR-C02" -> C02AccountView(context)
    "SCR-C03" -> C03ProblemView(context)
    "SCR-C04" -> C04TimingView(context)
    "SCR-C05" -> C05ReviewView(context)
    "SCR-C06" -> C06OffersView(context)
    "SCR-C07" -> C07ProviderView(context)
    "SCR-C08" -> C08OfferDetailsView(context)
    "SCR-C09" -> C09TrackingView(context)
    "SCR-C14" -> C14AddressesView(context)
    "SCR-C15" -> C15AddressFormView(context)
    "SCR-C16" -> C16SlotPickerView(context)
    "SCR-C17" -> C17MediaView(context)
    "SCR-C18" -> C18MessagesView(context)
    "SCR-C19" -> C19ChatView(context)
    "SCR-C20" -> C20CancellationView(context)
    "SCR-C21" -> C21PaymentSummaryView(context)
    "SCR-C22" -> C22ElectronicPaymentView(context)
    "SCR-C23" -> C23CompletionView(context)
    "SCR-C24" -> C24RatingView(context)
    "SCR-C25" -> C25OrdersView(context)
    "SCR-C26" -> C26OrderHistoryView(context)
    "SCR-C27" -> C27DisputeView(context)
    "SCR-C28" -> C28ProviderReportView(context)
    "SCR-C30", "SCR-C31" -> ProposalDecisionView(context, screen)
    else -> error("Unknown screen: $screen")
}
