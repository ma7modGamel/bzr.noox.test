package noox.bzr.customer

import android.content.Context
import android.widget.LinearLayout
import androidx.core.view.isVisible
import noox.bzr.design.R
import noox.bzr.design.SelectionState
import noox.bzr.design.views.RadioCardView
import noox.bzr.design.views.SelectableChipView
import noox.bzr.gallery.databinding.ScreenC04TimingBinding

/** SCR-C04, step 3 of 5 (DEC-071): the service address and when the technician comes. */
class C04TimingView(context: Context) : CustomerScreenView(context) {
    private var nextRoute = "SCR-C38"
    private val binding = ScreenC04TimingBinding.inflate(inflater, content)
    var onOpen: (String) -> Unit = {}
    var onMaterialSelect: (Int) -> Unit = {}
    var onTimingSelect: (Int) -> Unit = {}

    init {
        pinAction(binding.next)
        addRequestProgress(3)
        binding.changeAddress.onClick = { onOpen("SCR-C14") }
        binding.currentLocation.onClick = { onOpen("SCR-C15") }
        binding.map.onMyLocation = { onOpen("SCR-C15") }
        binding.note.text = string(R.string.request_timing_note)
        binding.next.onClick = { onOpen(nextRoute) }
    }

    override fun title(state: CustomerUiState) = string(R.string.request_timing_title)

    override fun renderContent(state: CustomerUiState) {
        binding.note.text = string(if (state.showMaterials) R.string.request_timing_staff_note else R.string.request_timing_note)
        nextRoute = if (state.showMaterials) "SCR-C05" else "SCR-C38"
        binding.next.text = string(if (state.showMaterials) R.string.request_step4_action else R.string.request_step3_action)
        binding.materialsSection.isVisible = state.showMaterials
        if (state.showMaterials) renderMaterialOptions(binding.materials, state, onMaterialSelect)
        binding.refreshAvailability.isVisible = state.messageKey == "request.timing.check_failed"
        binding.refreshAvailability.onClick = { onOpen("SCR-C04") }
        val address = state.fieldValues.firstOrNull().orEmpty()
        binding.map.title = address.ifBlank { string(R.string.request_address_required) }
        renderAddressPills(address)
        binding.message.isVisible = state.messageKey != null
        binding.message.isError = state.messageKey == "request.timing.check_failed"
        state.messageKey?.let {
            binding.message.text = string(
                when (it) {
                    "request.address.required" -> R.string.request_address_required
                    "request.timing.now_unavailable" -> R.string.request_timing_now_unavailable
                    "request.timing.checking" -> R.string.request_timing_checking
                    "request.timing.check_failed" -> R.string.request_timing_check_failed
                    "request.category.unavailable" -> R.string.request_category_unavailable
                    else -> R.string.request_slot_required
                },
            )
        }
        binding.timings.removeAllViews()
        state.options.forEachIndexed { index, label ->
            val code = state.optionCodes.getOrElse(index) { if (index == 0) "NOW" else "SCHEDULED" }
            binding.timings.addView(
                RadioCardView(context).apply {
                    title = label
                    icon = if (code == "NOW") R.drawable.ic_bolt else R.drawable.ic_calendar
                    badge = if (code == "NOW") string(if (state.nowAvailable) R.string.request_timing_fastest else R.string.request_timing_unavailable_badge) else ""
                    // The chosen slot, or a prompt — never a sample date (6ب).
                    body = when (code) {
                        "NOW" -> string(when {
                            state.messageKey == "request.timing.checking" -> R.string.request_timing_checking
                            !state.nowAvailable -> R.string.request_timing_now_unavailable
                            else -> R.string.request_timing_now_eta
                        })
                        else -> state.fieldValues.getOrNull(2).orEmpty().ifBlank { string(R.string.request_timing_body_scheduled) }
                    }
                    isEnabled = code != "NOW" || state.nowAvailable
                    alpha = if (isEnabled) 1f else 0.55f
                    selected = index == state.selectedOptionIndex
                    setOnClickListener { if (code == "NOW") onTimingSelect(index) else onOpen("SCR-C16") }
                },
                LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT),
            )
        }
        binding.next.state = continueState(state)
    }

    /** The chosen address as the selected pill, then «عنوان جديد» (C15). Other saved addresses live in C14. */
    private fun renderAddressPills(address: String) {
        binding.addressPills.removeAllViews()
        val pills = listOfNotNull(address.takeIf(String::isNotBlank)?.substringBefore(" · "), string(R.string.request_address_new))
        pills.forEachIndexed { index, label ->
            val isNew = index == pills.lastIndex
            binding.addressPills.addView(
                SelectableChipView(context).apply {
                    text = label
                    state = if (isNew) SelectionState.Unselected else SelectionState.Selected
                    setOnClickListener { onOpen(if (isNew) "SCR-C15" else "SCR-C14") }
                },
                LinearLayout.LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT),
            )
        }
    }
}
