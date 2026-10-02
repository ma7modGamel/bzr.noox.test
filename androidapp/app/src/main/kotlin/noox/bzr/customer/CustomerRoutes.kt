package noox.bzr.customer

import noox.bzr.gallery.R

/** Destination of a customer state, exactly as CustomerJourneyFlow switched on state.screen (DEC-047). */
object CustomerRoutes {
    private val destinations = mapOf(
        "SCR-C01" to R.id.scr_c01, "SCR-C02" to R.id.scr_c02, "SCR-C03" to R.id.scr_c03, "SCR-C04" to R.id.scr_c04,
        "SCR-C05" to R.id.scr_c05, "SCR-C06" to R.id.scr_c06, "SCR-C07" to R.id.scr_c07, "SCR-C08" to R.id.scr_c08,
        "SCR-C09" to R.id.scr_c09, "SCR-C14" to R.id.scr_c14, "SCR-C15" to R.id.scr_c15, "SCR-C16" to R.id.scr_c16,
        "SCR-C17" to R.id.scr_c17, "SCR-C18" to R.id.scr_c18, "SCR-C19" to R.id.scr_c19, "SCR-C20" to R.id.scr_c20,
        "SCR-C21" to R.id.scr_c21, "SCR-C22" to R.id.scr_c22, "SCR-C23" to R.id.scr_c23, "SCR-C24" to R.id.scr_c24,
        "SCR-C25" to R.id.scr_c25, "SCR-C26" to R.id.scr_c26, "SCR-C27" to R.id.scr_c27, "SCR-C28" to R.id.scr_c28,
        "SCR-C29" to R.id.scr_c29, "SCR-C30" to R.id.scr_c30, "SCR-C31" to R.id.scr_c31, "SCR-C32" to R.id.scr_c32,
        "SCR-C33" to R.id.scr_c33, "SCR-C34" to R.id.scr_c34, "SCR-C35" to R.id.scr_c35, "SCR-C36" to R.id.scr_c36,
    )

    fun destination(state: CustomerUiState): Int = when {
        // Offers with nothing to show open the no-offers screen (C06 → C35), as the Compose flow did.
        state.screen == "SCR-C06" && state.phase == CustomerPhase.Empty -> R.id.scr_c35
        else -> destinations[state.screen] ?: R.id.scr_c01
    }

    /** The C35 state shown for an empty C06, built by CustomerLogic as in the Compose flow. */
    fun noOffersState(state: CustomerUiState): CustomerUiState =
        if (state.screen == "SCR-C06") CustomerLogic.reduce("SCR-C35", mapOf("event" to "loaded", "available_actions" to state.visibleActions)) else state
}
