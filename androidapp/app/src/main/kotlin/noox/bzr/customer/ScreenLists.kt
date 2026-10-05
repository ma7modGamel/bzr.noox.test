package noox.bzr.customer

import android.graphics.Rect
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

/**
 * RecyclerView + ListAdapter/DiffUtil for the screen lists (DEC-047). Rows are immutable data classes,
 * so DiffUtil compares them by value; [key] tells it which rows are the same item.
 */
class RowAdapter<T : Any, V : View>(
    private val create: (ViewGroup) -> V,
    private val bind: (view: V, row: T, position: Int) -> Unit,
    key: (T) -> Any = { it },
) : ListAdapter<T, RowAdapter.Holder<V>>(Diff(key)) {
    class Holder<V : View>(val view: V) : RecyclerView.ViewHolder(view)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder<V> = Holder(create(parent).also { view ->
        if (view.layoutParams == null) {
            view.layoutParams = RecyclerView.LayoutParams(RecyclerView.LayoutParams.MATCH_PARENT, RecyclerView.LayoutParams.WRAP_CONTENT)
        }
    })

    override fun onBindViewHolder(holder: Holder<V>, position: Int) = bind(holder.view, getItem(position), position)

    private class Diff<T : Any>(private val key: (T) -> Any) : DiffUtil.ItemCallback<T>() {
        override fun areItemsTheSame(oldItem: T, newItem: T) = key(oldItem) == key(newItem)

        @android.annotation.SuppressLint("DiffUtilEquals")
        override fun areContentsTheSame(oldItem: T, newItem: T) = oldItem == newItem
    }
}

/**
 * Spacing between rows (Compose spacedBy): [gap] between items, never after the last. With [columns] > 1 the
 * gap is shared by neighbouring cells so both columns keep the same width (TwoColumns). Start / end follow RTL.
 */
class GapDecoration(private val gap: Int, private val columns: Int = 1, private val horizontal: Boolean = false) : RecyclerView.ItemDecoration() {
    override fun getItemOffsets(outRect: Rect, view: View, parent: RecyclerView, state: RecyclerView.State) {
        val position = parent.getChildAdapterPosition(view).takeIf { it != RecyclerView.NO_POSITION } ?: return
        val rtl = parent.layoutDirection == View.LAYOUT_DIRECTION_RTL
        if (horizontal) {
            if (position > 0) if (rtl) outRect.right = gap else outRect.left = gap
            return
        }
        if (position >= columns) outRect.top = gap
        if (columns > 1) {
            val column = position % columns
            val start = gap * column / columns
            val end = gap - gap * (column + 1) / columns
            if (rtl) outRect.set(end, outRect.top, start, 0) else outRect.set(start, outRect.top, end, 0)
        }
    }
}

/** A vertical (or [columns]-wide grid, or horizontal) list inside the screen column, drawn in full like Compose. */
fun RecyclerView.rows(adapter: RecyclerView.Adapter<*>, gap: Int, columns: Int = 1, horizontal: Boolean = false) {
    layoutManager = when {
        horizontal -> LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false)
        columns > 1 -> GridLayoutManager(context, columns)
        else -> LinearLayoutManager(context)
    }
    itemAnimator = null
    addItemDecoration(GapDecoration(gap, columns, horizontal))
    this.adapter = adapter
}

/** A RadioCard per option (reasons, payment methods, channels); [enabled] gates selection as in Compose. */
class RadioOptions(
    list: RecyclerView,
    gap: Int,
    private val body: String,
    private val onSelect: (Int) -> Unit,
) {
    private data class Option(val index: Int, val label: String, val selected: Boolean, val enabled: Boolean)

    private val adapter = RowAdapter<Option, noox.bzr.design.views.RadioCardView>(
        create = { parent -> noox.bzr.design.views.RadioCardView(parent.context) },
        bind = { card, option, _ ->
            card.title = option.label
            card.body = body
            card.selected = option.selected
            card.setOnClickListener(if (option.enabled) View.OnClickListener { onSelect(option.index) } else null)
            card.isClickable = option.enabled
        },
        key = { it.index },
    )

    init {
        list.rows(adapter, gap)
    }

    fun submit(options: List<String>, selectedIndex: Int, enabled: Boolean = true) {
        adapter.submitList(options.mapIndexed { index, label -> Option(index, label, selectedIndex == index, enabled) })
    }
}
