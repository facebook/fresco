/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

/**
 * A simple list-based activity showing all Fresco Showcase sample screens. Can be launched from
 * FB4A internal settings or any other host app where Fresco is already initialized.
 */
@SuppressLint(
    "BadSuperClassFragmentActivity",
    "EndpointWithoutSwitchOff",
    "UseOfBasicFragmentClass",
)
class ShowcaseListActivity : AppCompatActivity() {

  companion object {
    private const val FRAGMENT_CONTAINER_ID = 0x00ffffff
  }

  private lateinit var recyclerView: RecyclerView

  private val backCallback =
      object : OnBackPressedCallback(false) {
        override fun handleOnBackPressed() {
          val fm = supportFragmentManager
          val fragment = fm.findFragmentById(FRAGMENT_CONTAINER_ID)
          if (fragment != null) {
            fm.beginTransaction().remove(fragment).commit()
            recyclerView.visibility = View.VISIBLE
            title = "Fresco Showcase"
            isEnabled = false
          }
        }
      }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    ShowcaseProvider.initIfNeeded(this)

    val root = FrameLayout(this)
    root.fitsSystemWindows = true

    recyclerView = RecyclerView(this)
    recyclerView.id = android.R.id.list
    recyclerView.layoutManager = LinearLayoutManager(this)
    root.addView(recyclerView)

    val fragmentContainer = FrameLayout(this)
    fragmentContainer.id = FRAGMENT_CONTAINER_ID
    root.addView(fragmentContainer)

    setContentView(root)

    title = "Fresco Showcase"

    onBackPressedDispatcher.addCallback(this, backCallback)

    val items = buildListItems()
    recyclerView.adapter = ShowcaseListAdapter(items) { exampleItem -> showFragment(exampleItem) }
  }

  private fun buildListItems(): List<ListItem> {
    val items = mutableListOf<ListItem>()
    for (category in ExampleDatabase.examples) {
      items.add(ListItem.Header(category.name))
      for (example in category.examples) {
        items.add(ListItem.Sample(example))
      }
    }
    return items
  }

  private fun showFragment(item: ExampleItem) {
    recyclerView.visibility = View.GONE
    supportFragmentManager
        .beginTransaction()
        .replace(FRAGMENT_CONTAINER_ID, item.createFragment())
        .commit()
    title = item.title
    backCallback.isEnabled = true
  }
}

sealed class ListItem {
  data class Header(val name: String) : ListItem()

  data class Sample(val item: ExampleItem) : ListItem()
}

private class ShowcaseListAdapter(
    private val items: List<ListItem>,
    private val onItemClick: (ExampleItem) -> Unit,
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

  override fun getItemViewType(position: Int): Int =
      when (items[position]) {
        is ListItem.Header -> VIEW_TYPE_HEADER
        is ListItem.Sample -> VIEW_TYPE_SAMPLE
      }

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
    val inflater = LayoutInflater.from(parent.context)
    return when (viewType) {
      VIEW_TYPE_HEADER -> {
        val view =
            inflater.inflate(android.R.layout.simple_list_item_1, parent, false).apply {
              val textView = findViewById<TextView>(android.R.id.text1)
              textView.textSize = 14f
              textView.setTextColor(0xFF757575.toInt())
              val padding = (12 * resources.displayMetrics.density).toInt()
              textView.setPadding(padding, padding * 2, padding, padding / 2)
            }
        object : RecyclerView.ViewHolder(view) {}
      }
      else -> {
        val view = inflater.inflate(android.R.layout.simple_list_item_1, parent, false)
        val holder = object : RecyclerView.ViewHolder(view) {}
        view.setOnClickListener {
          val pos = holder.bindingAdapterPosition
          if (pos != RecyclerView.NO_POSITION) {
            val item = items[pos] as? ListItem.Sample ?: return@setOnClickListener
            onItemClick(item.item)
          }
        }
        holder
      }
    }
  }

  override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
    val textView = holder.itemView.findViewById<TextView>(android.R.id.text1)
    when (val item = items[position]) {
      is ListItem.Header -> textView.text = item.name
      is ListItem.Sample -> textView.text = item.item.title
    }
  }

  override fun getItemCount(): Int = items.size

  companion object {
    private const val VIEW_TYPE_HEADER = 0
    private const val VIEW_TYPE_SAMPLE = 1
  }
}
