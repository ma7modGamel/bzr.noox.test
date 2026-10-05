package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.withStyledAttributes
import com.google.android.material.card.MaterialCardView
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewEtaCardBinding

/** 43 §3 EtaCard: minutes + unit, divider, title/subtitle, car circle (C09). */
class EtaCardView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    MaterialCardView(context, attrs, com.google.android.material.R.attr.materialCardViewStyle) {
    private val binding = ViewEtaCardBinding.inflate(inflater, this)

    var value: String = ""
        set(value) {
            field = value
            binding.value.text = value
        }
    var unit: String = ""
        set(value) {
            field = value
            binding.unit.text = value
        }
    var title: String = ""
        set(value) {
            field = value
            binding.title.text = value
        }
    var subtitle: String = ""
        set(value) {
            field = value
            binding.subtitle.text = value
        }

    init {
        val padding = px(R.dimen.bremo_space_card_padding)
        setContentPadding(padding, padding, padding, padding)
        context.withStyledAttributes(attrs, R.styleable.EtaCardView) {
            value = getString(R.styleable.EtaCardView_bremoValue).orEmpty()
            unit = getString(R.styleable.EtaCardView_unit).orEmpty()
            title = getString(R.styleable.EtaCardView_bremoTitle).orEmpty()
            subtitle = getString(R.styleable.EtaCardView_subtitle).orEmpty()
        }
    }
}
