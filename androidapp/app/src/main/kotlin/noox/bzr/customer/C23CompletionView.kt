package noox.bzr.customer

import android.content.Context
import androidx.core.view.isVisible
import noox.bzr.design.BzrFormat
import noox.bzr.design.ButtonVisualState
import noox.bzr.design.R
import noox.bzr.design.views.InfoBannerView
import noox.bzr.design.views.PrimaryButtonView
import noox.bzr.gallery.databinding.ScreenC23CompletionBinding

/**
 * SCR-C23 — Figma «إنهاء الخدمة والدفع» 3002:4134 without the method picker (DEC-076): hero, provider card, invoice, the
 * method already used, «تأكيد انتهاء الخدمة» and «المشكلة ما زالت موجودة؟». `items`: provider, labor, materials, total, method.
 */
class C23CompletionView(context: Context) : CustomerScreenView(context) {
    private val binding = ScreenC23CompletionBinding.inflate(inflater, content)
    private val blocks = FinishServiceBlocks(context)
    var loadImage: (String, (android.graphics.Bitmap?) -> Unit) -> Unit = { _, ready -> ready(null) }
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
        val name = values.getOrElse(0) { "" }.ifEmpty { state.items.getOrElse(0) { "" } }
        val total = state.items.getOrElse(3) { "" }
        binding.body.removeAllViews()
        binding.body.addView(photoSection(context, string(R.string.repair_photos_before), state.beforePhotoUrls, loadImage))
        binding.body.addView(photoSection(context, string(R.string.repair_photos_after), state.afterPhotoUrls, loadImage))
        binding.body.addView(blocks.hero(name))
        binding.body.addView(blocks.providerCard(name, state.providerVerified, values.getOrElse(1) { "" }, values.getOrElse(2) { "" }))
        binding.body.addView(blocks.invoice(state.items.getOrElse(1) { "" }, state.items.getOrElse(2) { "" }, total))
        values.getOrNull(3)?.takeIf(String::isNotEmpty)?.let { method ->
            binding.body.addView(InfoBannerView(context).apply { text = BzrFormat.fill(string(R.string.completion_method), "method" to method) })
        }
        binding.dock.removeAllViews()
        if ("confirm_completion" in state.visibleActions) {
            binding.dock.addView(PrimaryButtonView(context).apply {
                text = BzrFormat.fill(string(R.string.completion_confirm), "amount" to total)
                icon = R.drawable.ic_check
                this.state = when {
                    state.isBusy -> ButtonVisualState.Loading
                    state.canContinue -> ButtonVisualState.Normal
                    else -> ButtonVisualState.Disabled
                }
                onClick = { onAction("confirm_completion") }
            })
        }
        if ("request_warranty" in state.visibleActions) {
            binding.dock.addView(blocks.link(string(R.string.warranty_request), R.drawable.ic_shield) { onAction("request_warranty") })
        }
        if ("open_dispute" in state.visibleActions) {
            binding.dock.addView(blocks.link(string(R.string.completion_problem), R.drawable.ic_info) { onAction("open_dispute") })
        }
    }
}
