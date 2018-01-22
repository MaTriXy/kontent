package com.arranlomas.mvisample.ui.todolist

import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup
import com.arranlomas.mvisample.R
import com.arranlomas.mvisample.models.TodoItem
import com.arranlomas.mvisample.models.TodoItemState
import com.arranlomas.mvisample.utils.inflateLayout
import kotlinx.android.synthetic.main.li_todo_item.view.*

/**
 * Created by arran on 21/12/2017.
 */
class TodoListAdapter : RecyclerView.Adapter<TodoListViewHolder>() {
    lateinit var onItemSelected: (Long, Boolean) -> Unit
    var items = emptyList<TodoItem>()

    override fun onBindViewHolder(holder: TodoListViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TodoListViewHolder {
        val itemView = parent.context.inflateLayout(R.layout.li_todo_item, parent, false)
        return TodoListViewHolder(itemView, onItemSelected)
    }

    override fun getItemCount(): Int = items.size

}

class TodoListViewHolder(itemView: View, private val onItemSelected: (Long, Boolean) -> Unit) : RecyclerView.ViewHolder(itemView) {
    fun bind(item: TodoItem) {
        itemView.todoTitle.text = item.title
        itemView.todoCheckbox.setOnCheckedChangeListener(null)
        when (item.state) {
            TodoItemState.ACTIVE -> itemView.todoCheckbox.isChecked = false
            TodoItemState.COMPLETED -> itemView.todoCheckbox.isChecked = true
        }
        itemView.todoCheckbox.setOnCheckedChangeListener { compoundButton, b ->
            onItemSelected.invoke(item.serverId, b)
        }
    }

}