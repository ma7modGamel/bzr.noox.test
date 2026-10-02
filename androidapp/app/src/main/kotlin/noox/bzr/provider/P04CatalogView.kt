package noox.bzr.provider

import android.content.Context
import android.widget.LinearLayout
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.design.SelectionState
import noox.bzr.design.views.CheckRowView
import noox.bzr.design.views.SelectableChipView
import noox.bzr.gallery.databinding.ScreenP04CatalogBinding

class P04CatalogView(context: Context) : ProviderScreenView(context) {
    private val binding = ScreenP04CatalogBinding.inflate(inflater, content)
    var onCategory: (Int) -> Unit = {}
    var onSpecialty: (Int) -> Unit = {}
    var onNext: () -> Unit = {}

    init { binding.next.onClick = { onNext() } }

    override fun title() = string(R.string.provider_onboarding_categories_title)

    override fun renderContent(state: ProviderUiState) {
        binding.categories.replace(state.options.mapIndexed { index, label ->
            SelectableChipView(context).apply {
                text = label
                this.state = if (index in state.selectedPrimaryIndices) SelectionState.Selected else SelectionState.Unselected
                setOnClickListener { onCategory(index) }
            }
        })
        binding.specialties.replace(state.secondaryOptions.mapIndexed { index, label ->
            CheckRowView(context).apply {
                text = label
                checked = index in state.selectedSecondaryIndices
                setOnClickListener { onSpecialty(index) }
            }
        })
        binding.validation.isVisible = state.phase == ProviderPhase.Empty || state.fieldErrors.isNotEmpty()
        binding.validation.text = string(
            when {
                state.phase == ProviderPhase.Empty -> R.string.provider_onboarding_catalog_empty
                "provider.onboarding.categories.required" in state.fieldErrors -> R.string.provider_onboarding_categories_required
                else -> R.string.provider_onboarding_specialties_required
            },
        )
        binding.next.state = buttonState(state)
    }
}

internal fun LinearLayout.replace(children: List<android.view.View>) {
    removeAllViews()
    children.forEach { addView(it, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)) }
}
