package noox.bzr.provider

import android.content.Context

fun providerScreenView(context: Context, screen: String): ProviderScreenView = when (screen) {
    "SCR-P01" -> P01IntroView(context)
    "SCR-P02" -> P02ProfileView(context)
    "SCR-P03" -> P03IdentityView(context)
    "SCR-P04" -> P04CatalogView(context)
    "SCR-P05" -> P05AreasView(context)
    "SCR-P06" -> P06PayoutView(context)
    "SCR-P07" -> P07StatusView(context)
    "SCR-P08" -> P08HomeView(context)
    "SCR-P09" -> P09AvailableRequestView(context)
    "SCR-P10" -> P10OfferView(context)
    "SCR-P11" -> P11OffersView(context)
    "SCR-P12" -> P12ConfirmedOrderView(context)
    "SCR-P13" -> P13OnTheWayView(context)
    "SCR-P14" -> P14ArrivedView(context)
    "SCR-P15" -> P15InProgressView(context)
    "SCR-P16" -> P16AwaitingPaymentView(context)
    "SCR-P17" -> P17CustomerRatingView(context)
    "SCR-P18" -> P18EarningsView(context)
    "SCR-P19" -> P19ProfileView(context)
    "SCR-P20" -> P20ProposalView(context)
    "SCR-P21" -> P21UnableView(context)
    else -> error("Unknown provider screen: $screen")
}
