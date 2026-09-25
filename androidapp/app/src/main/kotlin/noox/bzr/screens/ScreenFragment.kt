package noox.bzr.screens

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * A screen Fragment (DEC-047): builds its XML View, wires the callbacks to the ViewModel, and renders every
 * state the ViewModel publishes while STARTED. It holds no logic; [accepts] only skips states that belong to
 * the next destination while navigation is switching fragments.
 */
abstract class ScreenFragment<S : Any, V : View> : Fragment() {
    protected abstract val states: StateFlow<S>
    protected abstract fun create(): V
    protected abstract fun V.bind()
    protected abstract fun V.render(state: S)
    protected abstract fun accepts(state: S): Boolean

    @Suppress("UNCHECKED_CAST")
    protected val screenView: V get() = requireView() as V

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        create().apply { bind() }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                states.collect { state -> if (accepts(state)) screenView.render(state) }
            }
        }
    }
}
