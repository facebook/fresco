/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.image;

import javax.annotation.concurrent.GuardedBy;
import javax.annotation.concurrent.ThreadSafe;

import java.util.ArrayList;
import java.util.List;

import android.graphics.Bitmap;

import com.facebook.common.internal.Preconditions;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imageutils.BitmapUtil;

/**
 * CloseableImage that contains array of Bitmaps and frame durations.
 */
@ThreadSafe
public class CloseableAnimatedBitmap extends CloseableBitmap {

  // bitmap frames
  @GuardedBy("this")
  private List<CloseableReference<Bitmap>> mBitmapReferences;
  private volatile List<Bitmap> mBitmaps;

  // frame durations
  private volatile List<Integer> mDurations;

  public CloseableAnimatedBitmap(
      List<CloseableReference<Bitmap>> bitmapReferences,
      List<Integer> durations) {
    Preconditions.checkNotNull(bitmapReferences);
    Preconditions.checkState(bitmapReferences.size() >= 1, "Need at least 1 frame!");
    mBitmapReferences = new ArrayList<>();
    mBitmaps = new ArrayList<>();
    for (CloseableReference<Bitmap> bitmapReference : bitmapReferences) {
      mBitmapReferences.add(bitmapReference.clone());
      mBitmaps.add(bitmapReference.get());
    }
    mDurations = Preconditions.checkNotNull(durations);
    Preconditions.checkState(mDurations.size() == mBitmaps.size(), "Arrays length mismatch!");
  }

  /**
   * Creates a new instance of a CloseableStaticBitmap.
   *
   * @param bitmaps the bitmap frames. This list must be immutable.
   * @param durations the frame durations, This list must be immutable.
   * @param resourceReleaser ResourceReleaser to release the bitmaps to
   */
  public CloseableAnimatedBitmap(
      List<Bitmap> bitmaps,
      List<Integer> durations,
      ResourceReleaser<Bitmap> resourceReleaser) {
    Preconditions.checkNotNull(bitmaps);
    Preconditions.checkState(bitmaps.size() >= 1, "Need at least 1 frame!");
    mBitmaps = new ArrayList<>();
    mBitmapReferences = new ArrayList<>();
    for (Bitmap bitmap : bitmaps) {
      mBitmapReferences.add(CloseableReference.of(bitmap, resourceReleaser));
      mBitmaps.add(bitmap);
    }
    mDurations = Preconditions.checkNotNull(durations);
    Preconditions.checkState(mDurations.size() == mBitmaps.size(), "Arrays length mismatch!");
  }

  /**
   * Releases the bitmaps to the pool.
   */
  @Override
  public void close() {
    List<CloseableReference<Bitmap>> bitmapReferences;
    synchronized (this) {
      if (mBitmapReferences == null) {
        return;
      }
      bitmapReferences = mBitmapReferences;
      mBitmapReferences = null;
      mBitmaps = null;
      mDurations = null;
    }
    CloseableReference.closeSafely(bitmapReferences);
  }

  /**
   * Returns whether this instance is closed.
   */
  @Override
  public synchronized boolean isClosed() {
    return mBitmaps == null;
  }

  /**
   * Gets the bitmap frames.
   *
   * @return bitmap frames
   */
  public List<Bitmap> getBitmaps() {
    return mBitmaps;
  }

  /**
   * Gets the frame durations.
   *
   * @return frame durations
   */
  public List<Integer> getDurations() {
    return mDurations;
  }

  /**
   * Gets the first frame.
   *
   * @return the first frame
   */
  @Override
  public Bitmap getUnderlyingBitmap() {
    List<Bitmap> bitmaps = mBitmaps;
    return (bitmaps != null) ? bitmaps.get(0) : null;
  }

  /**
   * @return size in bytes all bitmaps in sum
   */
  @Override
  public int getSizeInBytes() {
    List<Bitmap> bitmaps = mBitmaps;
    if (bitmaps == null || bitmaps.size() == 0) {
      return 0;
    } else {
      return BitmapUtil.getSizeInBytes(bitmaps.get(0)) * bitmaps.size();
    }
  }

  /**
   * @return width of the image
   */
  @Override
  public int getWidth() {
    List<Bitmap> bitmaps = mBitmaps;
    return (bitmaps == null) ? 0 : bitmaps.get(0).getWidth();
  }

  /**
   * @return height of the image
   */
  @Override
  public int getHeight() {
    List<Bitmap> bitmaps = mBitmaps;
    return (bitmaps == null) ? 0 : bitmaps.get(0).getHeight();
  }

}
