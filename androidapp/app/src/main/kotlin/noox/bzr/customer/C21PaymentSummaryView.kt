package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import android.widget.LinearLayout
import noox.bzr.design.BzrFormat
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.R
import noox.bzr.design.views.PrimaryButtonView
import noox.bzr.design.views.RadioCardView
import noox.bzr.gallery.databinding.ScreenC21PaymentSummaryBinding

/**
 * SCR-C21 — Figma «إنهاء الخدمة والدفع» 3002:4134 (DEC-076): hero, provider card, invoice, the server's payment methods
 * and «ادفع الآن» when electronic payment is open. `items`: labor, materials, total.
 */
class C21PaymentSummaryView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC21PaymentSummaryBinding.inflate(inflater, content)
    private val blocks = FinishServiceBlocks(context)
    var loadImage: (String, (android.graphics.Bitmap?) -> Unit) -> Unit = { _, ready -> ready(null) }
    var onMethodSelect: (Int) -> Unit = {}
    var onPay: () -> Unit = {}
    var onAction: (String) -> Unit = {}

    private val warrantyBanner = noox.bzr.design.views.InfoBannerView(context)

    init {
        content.addView(warrantyBanner, 0)
        pinAction(binding.dock)
    }

    override fun title(state: CustomerUiState) = string(R.string.completion_title)

    override fun renderContent(state: CustomerUiState) {
        warrantyBanner.isVisible = state.warrantyCoverage.isNotBlank()
        warrantyBanner.text = state.warrantyCoverage
        val values = state.fieldValues
        val total = state.items.getOrElse(2) { "" }
        binding.body.removeAllViews()
        binding.body.addView(photoSection(context, string(R.string.repair_photos_before), state.beforePhotoUrls, loadImage))
        binding.body.addView(photoSection(context, string(R.string.repair_photos_after), state.afterPhotoUrls, loadImage))
        binding.body.addView(blocks.hero(values.getOrElse(0) { "" }))
        binding.body.addView(blocks.providerCard(values.getOrElse(0) { "" }, state.providerVerified, values.getOrElse(1) { "" }, values.getOrElse(2) { "" }))
        binding.body.addView(blocks.invoice(state.items.getOrElse(0) { "" }, state.items.getOrElse(1) { "" }, total))
        binding.body.addView(blocks.heading(string(R.string.confirm_payment)))
        val canSwitch = "change_payment_method" in state.visibleActions
        state.options.forEachIndexed { index, label ->
            val code = state.optionCodes.getOrElse(index) { "" }
            binding.body.addView(RadioCardView(context).apply {
                title = label
                body = when (code) {
                    "CASH" -> string(R.string.completion_method_cash)
                    "ELECTRONIC" -> string(R.string.completion_method_electronic)
                    else -> ""
                }
                icon = if (code == "CASH") R.drawable.ic_payments else R.drawable.ic_receipt
                selected = index == state.selectedOptionIndex
                isEnabled = canSwitch && !state.isBusy
                setOnClickListener { if (canSwitch) onMethodSelect(index) }
            }, LinearLayout.LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        }
        binding.dock.removeAllViews()
        if (("pay_electronic" in state.visibleActions || "submit_instapay_transfer" in state.visibleActions)) {
            binding.dock.addView(PrimaryButtonView(context).apply {
                text = BzrFormat.fill(string(R.string.completion_pay), "amount" to total)
                icon = R.drawable.ic_check
                this.state = when {
                    state.isBusy -> ButtonVisualState.Loading
                    state.canContinue -> ButtonVisualState.Normal
                    else -> ButtonVisualState.Disabled
                }
                onClick = { onPay() }
            })
        }
        if ("open_dispute" in state.visibleActions) {
            binding.dock.addView(blocks.link(string(R.string.completion_problem), R.drawable.ic_info) { onAction("open_dispute") })
        }
    }
}
