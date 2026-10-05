package noox.bzr.provider

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.FieldImeAction
import noox.bzr.design.FieldVisualState
import noox.bzr.design.R
import noox.bzr.design.views.RadioCardView
import noox.bzr.gallery.databinding.ScreenP06PayoutBinding

class P06PayoutView(context: Context) : ProviderScreenView(context) {
    private val binding = ScreenP06PayoutBinding.inflate(inflater, content)
    var onMethod: (Int) -> Unit = {}
    var onDetailsChange: (String) -> Unit = {}
    var onSubmit: () -> Unit = {}

    init {
        binding.details.imeAction = FieldImeAction.Done
        binding.details.onValueChange = { onDetailsChange(it) }
        binding.submit.onClick = { onSubmit() }
    }

    override fun title() = string(R.string.provider_onboarding_payout_title)

    override fun renderContent(state: ProviderUiState) {
        binding.methods.replace(state.options.mapIndexed { index, label ->
            RadioCardView(context).apply {
                title = label
                selected = index in state.selectedPrimaryIndices
                setOnClickListener { onMethod(index) }
            }
        })
        val selected = state.selectedPrimaryIndices.firstOrNull()
        binding.details.label = selected?.let { state.secondaryOptions.getOrNull(it) }.orEmpty()
        binding.details.value = state.fieldValues.firstOrNull().orEmpty()
        val detailsError = "provider.onboarding.payout.details.required" in state.fieldErrors
        binding.details.state = when {
            state.isBusy -> FieldVisualState.Disabled
            detailsError -> FieldVisualState.Error
            else -> FieldVisualState.Filled
        }
        binding.details.error = string(R.string.provider_onboarding_payout_details_required).takeIf { detailsError }
        val icons = listOf(R.drawable.ic_orders, R.drawable.ic_check, R.drawable.ic_location, R.drawable.ic_camera)
        val labels = listOf(
            R.string.provider_onboarding_summary_categories, R.string.provider_onboarding_summary_specialties,
            R.string.provider_onboarding_summary_areas, R.string.provider_onboarding_summary_documents,
        )
        binding.summary.rows = state.items.mapIndexed { index, value -> icons[index] to string(labels[index]).replace("%1\$s", value) }
        binding.validation.isVisible = state.messageKey != null || "provider.onboarding.payout.method.required" in state.fieldErrors
        binding.validation.text = string(
            if (state.messageKey != null) R.string.provider_application_not_editable
            else R.string.provider_onboarding_payout_method_required,
        )
        binding.submit.state = buttonState(state)
        binding.submit.isVisible = "submit_provider_application" in state.visibleActions
    }
}
