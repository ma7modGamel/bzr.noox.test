package noox.bzr.provider

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.design.views.CheckRowView
import noox.bzr.design.views.RadioCardView
import noox.bzr.gallery.databinding.ScreenP05AreasBinding

class P05AreasView(context: Context) : ProviderScreenView(context) {
    private val binding = ScreenP05AreasBinding.inflate(inflater, content)
    var onCity: (Int) -> Unit = {}
    var onArea: (Int) -> Unit = {}
    var onNext: () -> Unit = {}

    init { binding.next.onClick = { onNext() } }

    override fun title() = string(R.string.provider_onboarding_areas_title)

    override fun renderContent(state: ProviderUiState) {
        binding.cities.replace(state.options.mapIndexed { index, label ->
            RadioCardView(context).apply {
                title = label
                selected = index in state.selectedPrimaryIndices
                setOnClickListener { onCity(index) }
            }
        })
        binding.areas.replace(state.secondaryOptions.mapIndexed { index, label ->
            CheckRowView(context).apply {
                text = label
                checked = index in state.selectedSecondaryIndices
                setOnClickListener { onArea(index) }
            }
        })
        binding.validation.isVisible = state.phase == ProviderPhase.Empty || state.fieldErrors.isNotEmpty()
        binding.validation.text = string(
            if (state.phase == ProviderPhase.Empty) R.string.provider_onboarding_areas_empty
            else R.string.provider_onboarding_areas_required,
        )
        binding.next.state = buttonState(state)
    }
}
