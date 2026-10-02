package noox.bzr.provider

import android.content.Context
import android.view.View.OnClickListener
import androidx.core.view.isVisible
import noox.bzr.customer.RadioOptions
import noox.bzr.customer.RowAdapter
import noox.bzr.customer.rows
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.FieldVisualState
import noox.bzr.design.R
import noox.bzr.design.SelectionState
import noox.bzr.design.views.SelectableChipView
import noox.bzr.gallery.databinding.ScreenP10OfferBinding

class P10OfferView(context: Context) : ProviderScreenView(context) {
    private val binding = ScreenP10OfferBinding.inflate(inflater, content)
    var onEtaSelect: (Int) -> Unit = {}
    var onDeductibleSelect: (Int) -> Unit = {}
    var onPriceChange: (String) -> Unit = {}
    var onIncludesChange: (String) -> Unit = {}
    var onNoteChange: (String) -> Unit = {}
    var onSubmit: () -> Unit = {}

    private data class Eta(val index: Int, val label: String, val selected: Boolean, val enabled: Boolean)
    private val etaAdapter = RowAdapter<Eta, SelectableChipView>(
        create = { SelectableChipView(it.context) },
        bind = { chip, row, _ ->
            chip.text = row.label
            chip.state = if (!row.enabled) SelectionState.Disabled else if (row.selected) SelectionState.Selected else SelectionState.Unselected
            chip.setOnClickListener(if (row.enabled) OnClickListener { onEtaSelect(row.index) } else null)
        },
        key = Eta::index,
    )
    private val deductible = RadioOptions(
        binding.deductible,
        resources.getDimensionPixelSize(R.dimen.bremo_space_m),
        "",
    ) { onDeductibleSelect(it) }

    init {
        binding.eta.rows(etaAdapter, resources.getDimensionPixelSize(R.dimen.bremo_space_s), horizontal = true)
        binding.price.onValueChange = { onPriceChange(it) }
        binding.includes.onValueChange = { onIncludesChange(it) }
        binding.note.onValueChange = { onNoteChange(it) }
        binding.submit.onClick = { onSubmit() }
    }

    override fun title() = string(R.string.provider_offer_title)

    override fun renderContent(state: ProviderUiState) {
        binding.summary.title = string(R.string.provider_offer_summary)
        binding.summary.rows = state.items.map { R.drawable.ic_info to it }
        binding.summary.isVisible = state.items.isNotEmpty()
        binding.price.value = state.fieldValues.getOrElse(0) { "" }
        binding.includes.value = state.fieldValues.getOrElse(1) { "" }
        binding.note.value = state.fieldValues.getOrElse(2) { "" }
        bindFieldErrors(state)
        binding.etaTitle.isVisible = state.showPriceGuide
        binding.eta.isVisible = state.showPriceGuide
        etaAdapter.submitList(state.options.mapIndexed { index, label ->
            Eta(index, string(R.string.format_minutes).replace("{n}", label), index == state.selectedOptionIndex, !state.isBusy)
        })
        binding.deductibleTitle.isVisible = state.showOutsideReason
        binding.deductible.isVisible = state.showOutsideReason
        deductible.submit(state.itemDetails, state.selectedSecondaryIndices.firstOrNull() ?: -1, !state.isBusy)
        binding.net.text = context.getString(R.string.provider_offer_net, state.secondaryOptions.firstOrNull().orEmpty())
        val choiceError = when {
            "provider.offer.eta.required" in state.fieldErrors -> R.string.provider_offer_eta_required
            "provider.offer.deductible.required" in state.fieldErrors -> R.string.provider_offer_deductible_required
            else -> null
        }
        val succeeded = state.messageKey == "provider.offer.success"
        binding.success.isVisible = succeeded
        binding.success.text = string(R.string.provider_offer_success)
        binding.warning.isVisible = (state.messageKey != null && !succeeded) || choiceError != null
        binding.warning.text = string(choiceError ?: R.string.provider_market_request_unavailable)
        binding.submit.isVisible = "submit_offer" in state.visibleActions
        binding.submit.state = when {
            state.isBusy -> ButtonVisualState.Loading
            state.canContinue -> ButtonVisualState.Normal
            else -> ButtonVisualState.Disabled
        }
    }

    private fun bindFieldErrors(state: ProviderUiState) {
        val priceError = "provider.offer.price.invalid" in state.fieldErrors
        binding.price.state = if (priceError) FieldVisualState.Error else FieldVisualState.Filled
        binding.price.error = if (priceError) string(R.string.provider_offer_price_invalid) else null
        val includesError = "provider.offer.includes.invalid" in state.fieldErrors
        binding.includes.state = if (includesError) FieldVisualState.Error else FieldVisualState.Filled
        binding.includes.error = if (includesError) string(R.string.provider_offer_includes_invalid) else null
        val noteError = "provider.offer.note.invalid" in state.fieldErrors
        binding.note.state = if (noteError) FieldVisualState.Error else FieldVisualState.Filled
        binding.note.error = if (noteError) string(R.string.provider_offer_note_invalid) else null
    }
}
