package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC01HomeBinding

/** SCR-C01 (DEC-047): categories, new request, current order, bottom navigation. */
class C01HomeView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC01HomeBinding.inflate(inflater, content)
    var onOpen: (String) -> Unit = {}

    init {
        binding.newRequest.onClick = { onOpen("SCR-C03") }
        binding.order.onClick = { onOpen("SCR-C09") }
        binding.nav.items = listOf(
            R.drawable.ic_home to string(R.string.nav_home),
            R.drawable.ic_orders to string(R.string.nav_orders),
            R.drawable.ic_chat to string(R.string.nav_messages),
            R.drawable.ic_account to string(R.string.nav_account),
        )
        binding.nav.selectedIndex = 0
        binding.nav.onSelect = { index -> listOf("SCR-C01", "SCR-C25", "SCR-C18", "SCR-C02").getOrNull(index)?.let(onOpen) }
    }

    override fun title(state: CustomerUiState) = string(R.string.nav_home)

    override fun renderContent(state: CustomerUiState) {
        binding.empty.isVisible = state.phase == CustomerPhase.Empty
        binding.order.isVisible = state.phase != CustomerPhase.Empty
    }
}
