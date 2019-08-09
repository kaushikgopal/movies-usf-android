package co.kaush.msusf.genres

import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import co.kaush.msusf.R
import co.kaush.msusf.movies.inflate

class GenreChecklistAdapter(
        private val genreRepo: GenreRepository,
        private val genreToggleListener: (MSGenre) -> Unit
) : ListAdapter<GenreCheckBoxViewState, GenreCheckVH>(GenreCheckDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GenreCheckVH {
        return GenreCheckVH(parent.inflate(R.layout.view_checkbox))
    }

    override fun onBindViewHolder(holder: GenreCheckVH, position: Int) {
        val vs: GenreCheckBoxViewState = getItem(position)
        holder.bind(genreRepo, vs, genreToggleListener)
    }
}

class GenreCheckDiffCallback : DiffUtil.ItemCallback<GenreCheckBoxViewState>() {

    override fun areItemsTheSame(
            oldItem: GenreCheckBoxViewState, newItem: GenreCheckBoxViewState
    ): Boolean = true

    override fun areContentsTheSame(oldItem: GenreCheckBoxViewState, newItem: GenreCheckBoxViewState): Boolean {
        return oldItem.checkboxName == newItem.checkboxName
    }

}

class GenreCheckVH(itemView: View) : RecyclerView.ViewHolder(itemView) {

    private val checkBox: CheckBox = itemView.findViewById(R.id.genre_checkbox)

    fun bind(
            genreRepo: GenreRepository,
            vs: GenreCheckBoxViewState,
            genreToggleListener: (MSGenre) -> Unit
    ) {
        checkBox.text = vs.checkboxName
        checkBox.isChecked = vs.isChecked

        checkBox.setOnCheckedChangeListener { _, _ ->
            val genreToggled = genreRepo.findGenre(vs.checkboxName)
            genreToggleListener.invoke(genreToggled)
        }
    }
}