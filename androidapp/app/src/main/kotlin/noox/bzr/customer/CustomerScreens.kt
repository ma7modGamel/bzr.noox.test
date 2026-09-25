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
    "SCR-C14" -> C14AddressesView(context)
    "SCR-C15" -> C15AddressFormView(context)
    "SCR-C16" -> C16SlotPickerView(context)
    "SCR-C17" -> C17MediaView(context)
    else -> error("Unknown screen: $screen")
}
