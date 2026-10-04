package noox.bzr.design.views

import android.content.Context
import android.util.AttributeSet
import android.view.Gravity
import android.widget.LinearLayout
import androidx.core.content.withStyledAttributes
import noox.bzr.design.AvatarSize
import noox.bzr.design.R
import noox.bzr.design.databinding.ViewProviderHeaderBinding

/** 43 §3 ProviderHeader: verified avatar (64/72/88), name, optional verified line, rating line. */
class ProviderHeaderView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) : LinearLayout(context, attrs) {
    private val binding: ViewProviderHeaderBinding

    var name: String = ""
        set(value) {
            field = value
            binding.name.text = value
        }
    var rating: String = ""
        set(value) {
            field = value
            binding.ratingLine.bind(value, services)
        }
    var services: String? = null
        set(value) {
            field = value
            binding.ratingLine.bind(rating, value)
        }
    var size: AvatarSize = AvatarSize.Small
        set(value) {
            field = value
            binding.avatar.setSquare(
                when (value) {
                    AvatarSize.Small -> R.dimen.bremo_size_avatar_small
                    AvatarSize.Medium -> R.dimen.bremo_size_avatar_medium
                    AvatarSize.Large -> R.dimen.bremo_size_avatar_large
                },
            )
        }
    var verifiedLabel: String? = null
        set(value) {
            field = value
            binding.verifiedLabel.text = value.orEmpty()
            binding.verified.showIf(value != null)
            binding.avatar.verified = value != null
        }

    init {
        orientation = if (resources.configuration.fontScale > 1.3f) VERTICAL else HORIZONTAL
        gravity = Gravity.CENTER_VERTICAL
        gap(R.drawable.bremo_gap_m)
        binding = ViewProviderHeaderBinding.inflate(inflater, this)
        context.withStyledAttributes(attrs, R.styleable.ProviderHeaderView) {
            name = getString(R.styleable.ProviderHeaderView_name).orEmpty()
            rating = getString(R.styleable.ProviderHeaderView_rating).orEmpty()
            services = getString(R.styleable.ProviderHeaderView_services)
            size = enumValue(R.styleable.ProviderHeaderView_avatarSize, AvatarSize.entries.toTypedArray(), AvatarSize.Small)
            verifiedLabel = getString(R.styleable.ProviderHeaderView_verifiedLabel)
        }
    }
}
