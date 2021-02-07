/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.zoomableapp;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import androidx.viewpager.widget.ViewPager;

public class MainActivity extends Activity {

  private MyPagerAdapter mAdapter;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.activity_main);

    ViewPager pager = (ViewPager) findViewById(R.id.pager);
    mAdapter = new MyPagerAdapter(pager.getChildCount());

    pager.setAdapter(mAdapter);
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    MenuInflater inflater = getMenuInflater();
    inflater.inflate(R.menu.menu, menu);
    return super.onCreateOptionsMenu(menu);
  }

  @Override
  public boolean onPrepareOptionsMenu(Menu menu) {
    menu.findItem(R.id.allow_zoomed_swiping).setChecked(mAdapter.allowsSwipingWhileZoomed());
    return super.onPrepareOptionsMenu(menu);
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == R.id.allow_zoomed_swiping) {
      mAdapter.toggleAllowSwipingWhileZoomed();
      item.setChecked(mAdapter.allowsSwipingWhileZoomed());
      mAdapter.notifyDataSetChanged();
    }
    return super.onOptionsItemSelected(item);
  }
}
