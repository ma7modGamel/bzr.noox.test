package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.gallery.databinding.ScreenC34TermsBinding

/** SCR-C34 (DEC-047): current terms version. */
class C34TermsView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC34TermsBinding.inflate(inflater, content)

    override fun title(state: CustomerUiState) = string(R.string.terms_title)

    override fun renderContent(state: CustomerUiState) {
        binding.empty.isVisible = !state.canContinue
        listOf(binding.version, binding.effective, binding.body).forEach { it.isVisible = state.canContinue }
        binding.version.text = "${string(R.string.terms_version)}: ${state.itemDetails.getOrElse(0) { "" }}"
        binding.effective.text = "${string(R.string.terms_effective)}: ${state.itemDetails.getOrElse(1) { "" }}"
        binding.body.text = state.items.firstOrNull().orEmpty()
    }
}
