/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.zoomableapp;

import android.net.Uri;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import androidx.viewpager.widget.PagerAdapter;
import com.facebook.samples.zoomable.DoubleTapGestureListener;
import com.facebook.samples.zoomable.ZoomableVitoView;

class MyPagerAdapter extends PagerAdapter {

  private static final String[] SAMPLE_URIS = {
    "https://www.gstatic.com/webp/gallery/1.sm.jpg",
    "https://www.gstatic.com/webp/gallery/2.sm.jpg",
    "https://www.gstatic.com/webp/gallery/3.sm.jpg",
    "https://www.gstatic.com/webp/gallery/4.sm.jpg",
    "https://www.gstatic.com/webp/gallery/5.sm.jpg",
  };

  private final int mItemCount;
  private boolean mAllowSwipingWhileZoomed = true;

  public MyPagerAdapter(int itemCount) {
    mItemCount = itemCount;
  }

  public void setAllowSwipingWhileZoomed(boolean allowSwipingWhileZoomed) {
    mAllowSwipingWhileZoomed = allowSwipingWhileZoomed;
  }

  public boolean allowsSwipingWhileZoomed() {
    return mAllowSwipingWhileZoomed;
  }

  public void toggleAllowSwipingWhileZoomed() {
    mAllowSwipingWhileZoomed = !mAllowSwipingWhileZoomed;
  }

  @Override
  public Object instantiateItem(ViewGroup container, int position) {
    FrameLayout page = (FrameLayout) container.getChildAt(position);
    if (page == null) {
      return null;
    }
    ZoomableVitoView zoomableVitoView = (ZoomableVitoView) page.findViewById(R.id.zoomableView);
    zoomableVitoView.setAllowTouchInterceptionWhileZoomed(mAllowSwipingWhileZoomed);
    zoomableVitoView.setIsLongpressEnabled(false);
    zoomableVitoView.setTapListener(new DoubleTapGestureListener(zoomableVitoView));
    Uri uri = Uri.parse(SAMPLE_URIS[position % SAMPLE_URIS.length]);
    zoomableVitoView.loadImage(uri, "ZoomableApp-MyPagerAdapter");
    page.requestLayout();
    return page;
  }

  @Override
  public void destroyItem(ViewGroup container, int position, Object object) {
    FrameLayout page = (FrameLayout) container.getChildAt(position);
    ZoomableVitoView zoomableVitoView = (ZoomableVitoView) page.getChildAt(0);
    zoomableVitoView.releaseImage();
  }

  @Override
  public int getCount() {
    return mItemCount;
  }

  @Override
  public boolean isViewFromObject(View arg0, Object arg1) {
    return arg0 == arg1;
  }

  @Override
  public int getItemPosition(Object object) {
    return POSITION_NONE;
  }
}
