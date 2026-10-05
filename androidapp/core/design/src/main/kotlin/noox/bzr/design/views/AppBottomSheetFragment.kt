package noox.bzr.design.views

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

/** Pattern E host (43 §4): a BottomSheetDialogFragment whose content is built by the screen. */
open class AppBottomSheetFragment : BottomSheetDialogFragment() {
    var content: ((ViewGroup?) -> View)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? =
        content?.invoke(container) ?: AppBottomSheetView(requireContext())
}
