package co.kaush.msusf.movies

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.CircularProgressDrawable
import coil.load

class MSMovieSearchHistoryAdapter(private val historyClickListener: (MSMovie) -> Unit) :
    ListAdapter<MSMovie, MSMovieSearchVH>(MSMovieSearchDiffCallback()) {
  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MSMovieSearchVH {
    return MSMovieSearchVH(parent.inflate(R.layout.view_movie))
  }

  override fun onBindViewHolder(holder: MSMovieSearchVH, position: Int) {
    holder.bind(getItem(position), historyClickListener)
  }
}

class MSMovieSearchDiffCallback : DiffUtil.ItemCallback<MSMovie>() {
  // only one kind of item
  override fun areItemsTheSame(oldItem: MSMovie, newItem: MSMovie): Boolean = true

  override fun areContentsTheSame(oldItem: MSMovie, newItem: MSMovie): Boolean {
    // this is just lazy!
    return oldItem.posterUrl.equals(newItem.posterUrl, ignoreCase = true)
  }
}

class MSMovieSearchVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

  private val posterView: ImageView = itemView.findViewById(R.id.ms_result_poster)
  private val ratingView: TextView = itemView.findViewById(R.id.ms_result_rating)
  private val spinner: CircularProgressDrawable by lazy {
    val circularProgressDrawable = CircularProgressDrawable(itemView.context)
    circularProgressDrawable.strokeWidth = 5f
    circularProgressDrawable.centerRadius = 30f
    circularProgressDrawable.start()
    circularProgressDrawable
  }

  @SuppressLint("SetTextI18n")
  fun bind(item: MSMovie, historyClickListener: (MSMovie) -> Unit) {

    (item.ratings.first()).let { ratingView.text = it.summary }

    item.posterUrl.takeIf { it.isNotBlank() }?.let { posterView.load(it) { placeholder(spinner) } }
        ?: run { posterView.setImageResource(0) }

    itemView.setOnClickListener {
      historyClickListener.invoke(item)
      posterView.growShrink()
    }
  }
}

// -----------------------------------------------------------------------------------
// helpers

/**
 * Usage: `val view = container?.inflate(R.layout.activity)`
 * `inflater?.inflate(R.layout.fragment_dialog_standard, c)!!`
 */
private fun ViewGroup.inflate(@LayoutRes layoutRes: Int, attachToRoot: Boolean = false): View =
    LayoutInflater.from(context).inflate(layoutRes, this, attachToRoot)
