package noox.bzr.provider

import noox.bzr.gallery.R

object ProviderRoutes {
    private val destinations = mapOf(
        "SCR-P01" to R.id.scr_p01, "SCR-P02" to R.id.scr_p02, "SCR-P03" to R.id.scr_p03,
        "SCR-P04" to R.id.scr_p04, "SCR-P05" to R.id.scr_p05, "SCR-P06" to R.id.scr_p06,
        "SCR-P07" to R.id.scr_p07, "SCR-P08" to R.id.scr_p08,
        "SCR-P09" to R.id.scr_p09, "SCR-P10" to R.id.scr_p10, "SCR-P11" to R.id.scr_p11,
        "SCR-P12" to R.id.scr_p12, "SCR-P13" to R.id.scr_p13, "SCR-P14" to R.id.scr_p14,
        "SCR-P15" to R.id.scr_p15, "SCR-P16" to R.id.scr_p16,
        "SCR-P17" to R.id.scr_p17, "SCR-P18" to R.id.scr_p18, "SCR-P19" to R.id.scr_p19,
        "SCR-P20" to R.id.scr_p20, "SCR-P21" to R.id.scr_p21,
        "SCR-C19" to R.id.scr_provider_chat,
        "SCR-C32" to R.id.scr_provider_notifications,
    )

    fun destination(state: ProviderUiState): Int = destinations[state.screen] ?: R.id.scr_p01
}
