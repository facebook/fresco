/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase

import android.graphics.Color
import android.os.Bundle
import android.preference.PreferenceManager
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.drawable.DrawableCompat
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import androidx.fragment.app.Fragment
import com.facebook.fresco.samples.showcase.settings.SettingsFragment
import com.google.android.material.navigation.NavigationView
import com.google.android.material.snackbar.Snackbar
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.app_bar_main.*
import kotlinx.android.synthetic.main.content_main.*

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val toggle = ActionBarDrawerToggle(
                this,
                drawer_layout,
                toolbar,
                R.string.navigation_drawer_open,
                R.string.navigation_drawer_close)
        drawer_layout.addDrawerListener(toggle)
        toggle.syncState()

        nav_view.setNavigationItemSelectedListener(this)

        if (savedInstanceState == null) {
            val selectedItem = PreferenceManager.getDefaultSharedPreferences(this)
                    .getInt(KEY_SELECTED_NAVDRAWER_ITEM_ID, INITIAL_NAVDRAWER_ITEM_ID)
            handleNavigationItemClick(selectedItem)
            nav_view.setCheckedItem(selectedItem)
        }
    }

    override fun onResume() {
        super.onResume()
        maybeShowUriOverrideReminder()
    }

    override fun onBackPressed() {
        if (drawer_layout.isDrawerOpen(GravityCompat.START)) {
            drawer_layout.closeDrawer(GravityCompat.START)
        } else {
            super.onBackPressed()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        // the support toolbar should probably do this by default
        val styles = obtainStyledAttributes(R.style.AppTheme_Toolbar, intArrayOf(R.attr.colorControlNormal))
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
        if (item.itemId == R.id.action_settings) {
            showFragment(SettingsFragment())
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        handleNavigationItemClick(item.itemId)
        val drawer = findViewById<View>(R.id.drawer_layout) as DrawerLayout
        drawer.closeDrawer(GravityCompat.START)
        return true
    }

    private fun handleNavigationItemClick(itemId: Int) {
        showFragment(Examples.getFragment(itemId))

        // Save the item if it's not the settings fragment
        if (itemId != R.id.nav_action_settings) {
            PreferenceManager.getDefaultSharedPreferences(this)
                    .edit()
                    .putInt(KEY_SELECTED_NAVDRAWER_ITEM_ID, itemId)
                    .apply()
        }
    }

    /**
     * Utility method to display a specific Fragment. If the tag is not null we add a backstack
     *
     * @param fragment The Fragment to add
     */
    private fun showFragment(fragment: ShowcaseFragment) {
        val fragmentTransaction = supportFragmentManager
                .beginTransaction()
                .replace(R.id.content_main, fragment as Fragment)
        if (fragment.backstackTag != null) {
            fragmentTransaction.addToBackStack(fragment.backstackTag)
        }
        fragmentTransaction.commit()

        setTitle(fragment.titleId)
    }

    private fun maybeShowUriOverrideReminder() {
        if (ShowcaseApplication.imageUriProvider.uriOverride == null) {
            return
        }
        Snackbar.make(
                content_main,
                R.string.snackbar_uri_override_reminder_text,
                Snackbar.LENGTH_LONG)
                .setAction(R.string.snackbar_uri_override_reminder_change_button) {
                    showFragment(SettingsFragment())
                }
                .show()
    }

    companion object {
        private const val INITIAL_NAVDRAWER_ITEM_ID = R.id.nav_welcome
        private const val KEY_SELECTED_NAVDRAWER_ITEM_ID = "selected_navdrawer_item_id"
    }
}
