/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase

import android.graphics.Color
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.facebook.fresco.samples.showcase.databinding.ActivityMainBinding
import com.facebook.fresco.samples.showcase.permissions.StoragePermissionHelper
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar

class MainActivity : AppCompatActivity() {

  private lateinit var binding: ActivityMainBinding
  private val toolbar: Toolbar by lazy { binding.appBarMain.toolbar }
  private val drawerLayout: DrawerLayout by lazy { binding.drawerLayout }
  private val navView: NavigationView by lazy { binding.navView }
  private val contentMain by lazy { binding.appBarMain.contentMain.root }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding = ActivityMainBinding.inflate(layoutInflater)
    setContentView(binding.root)
    setSupportActionBar(toolbar)

    val toggle =
        ActionBarDrawerToggle(
            this,
            drawerLayout,
            toolbar,
            R.string.navigation_drawer_open,
            R.string.navigation_drawer_close)
    drawerLayout.addDrawerListener(toggle)
    toggle.syncState()

    addNavDrawerEntry(navView.menu, ExampleDatabase.welcome)
    for (category in ExampleDatabase.examples) {
      val submenu = navView.menu.addSubMenu(category.name)
      for (item in category.examples) {
        addNavDrawerEntry(submenu, item)
      }
    }
    addNavDrawerEntry(navView.menu.addSubMenu("More"), ExampleDatabase.settings)

    if (savedInstanceState == null) {
      navView.menu.performIdentifierAction(
          PreferenceManager.getDefaultSharedPreferences(this)
              .getInt(KEY_SELECTED_NAVDRAWER_ITEM_ID, ExampleDatabase.welcome.itemId),
          0)
    }
  }

  private fun addNavDrawerEntry(menu: Menu, item: ExampleItem) {
    menu
        .add(Menu.NONE, item.itemId, Menu.NONE, item.title)
        .setCheckable(true)
        .setOnMenuItemClickListener {
          showFragment(item.createFragment(), item.title, item.backstackTag)
          drawerLayout.closeDrawer(GravityCompat.START)
          navView.setCheckedItem(it)
          PreferenceManager.getDefaultSharedPreferences(this)
              .edit()
              .putInt(KEY_SELECTED_NAVDRAWER_ITEM_ID, it.itemId)
              .apply()
          true
        }
  }

  override fun onResume() {
    super.onResume()
    maybeShowUriOverrideReminder()
  }

  override fun onBackPressed() {
    if (drawerLayout.isDrawerOpen(GravityCompat.START)) {
      drawerLayout.closeDrawer(GravityCompat.START)
    } else {
      super.onBackPressed()
    }
  }

  override fun onCreateOptionsMenu(menu: Menu): Boolean {
    menuInflater.inflate(R.menu.main, menu)
    // the support toolbar should probably do this by default
    val styles =
        obtainStyledAttributes(
            R.style.AppTheme_Toolbar, intArrayOf(androidx.appcompat.R.attr.colorControlNormal))
    try {
      val tintColor = styles.getColor(0, Color.BLACK)
      for (i in 0 until menu.size()) {
        val icon = menu.getItem(i).icon
        if (icon != null) {
          DrawableCompat.setTint(icon, tintColor)
        }
      }
    } finally {
      styles.recycle()
    }
    return super.onCreateOptionsMenu(menu)
  }

  override fun onOptionsItemSelected(item: MenuItem): Boolean {
    when (item.itemId) {
      R.id.action_next_image -> ShowcaseApplication.imageSelector.selectNext(this)
      R.id.action_prev_image -> ShowcaseApplication.imageSelector.selectPrevious(this)
      R.id.action_settings -> showFragment(ExampleDatabase.settings)
      R.id.action_edit_image -> showImageOptionsEditor()
    }
    return super.onOptionsItemSelected(item)
  }

  private fun showImageOptionsEditor() {
    supportFragmentManager.let {
      ImageOptionsBottomSheet.newInstance(ShowcaseApplication.imageSelector, Bundle()).apply {
        show(it, tag)
      }
    }
  }

  override fun onRequestPermissionsResult(
      requestCode: Int,
      permissions: Array<out String>,
      grantResults: IntArray
  ) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    StoragePermissionHelper.onRequestPermissionsResult(this, requestCode, permissions, grantResults)
  }

  /**
   * Utility method to display a specific Fragment. If the tag is not null we add a backstack
   *
   * @param fragment The Fragment to add
   */
  private fun showFragment(fragment: Fragment, title: String, backstackTag: String? = null) {
    val fragmentTransaction =
        supportFragmentManager.beginTransaction().replace(R.id.content_main, fragment)
    if (backstackTag != null) {
      fragmentTransaction.addToBackStack(backstackTag)
    }
    fragmentTransaction.commit()

    setTitle(title)
    ShowcaseApplication.imageTracker.reset()
  }

  private fun maybeShowUriOverrideReminder() {
    if (ShowcaseApplication.imageUriProvider.uriOverride == null) {
      return
    }
    Snackbar.make(contentMain, R.string.snackbar_uri_override_reminder_text, Snackbar.LENGTH_LONG)
        .setAction(R.string.snackbar_uri_override_reminder_change_button) {
          showFragment(ExampleDatabase.settings)
        }
        .show()
  }

  private fun showFragment(item: ExampleItem) {
    showFragment(item.createFragment(), item.title, item.backstackTag)
  }

  companion object {
    private const val KEY_SELECTED_NAVDRAWER_ITEM_ID = "selected_navdrawer_item_id"
  }
}
