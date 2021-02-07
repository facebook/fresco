/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.scrollperf;

import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.facebook.samples.scrollperf.fragments.MainFragment;
import com.facebook.samples.scrollperf.fragments.SettingsFragment;
import com.facebook.samples.scrollperf.util.SizeUtil;

public class MainActivity extends AppCompatActivity {

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    SizeUtil.initSizeData(this);
    setContentView(R.layout.activity_main);
    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    toolbar.setTitle(R.string.app_name);
    setSupportActionBar(toolbar);
    if (savedInstanceState == null) {
      final MainFragment mainFragment = new MainFragment();
      getSupportFragmentManager()
          .beginTransaction()
          .add(R.id.anchor_point, mainFragment, MainFragment.TAG)
          .commit();
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.action_settings) {
      final SettingsFragment settingsFragment = new SettingsFragment();
      getSupportFragmentManager()
          .beginTransaction()
          .replace(R.id.anchor_point, settingsFragment, SettingsFragment.TAG)
          .addToBackStack(SettingsFragment.TAG)
          .commit();
    }
    return super.onOptionsItemSelected(item);
  }
}
