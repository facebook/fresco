/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.animation2.bitmap;

import com.facebook.common.logging.FLog;
import com.facebook.fresco.animation.bitmap.BitmapAnimationBackend;

/**
 * {@link com.facebook.fresco.animation.bitmap.BitmapAnimationBackend.FrameListener} that logs
 * animation statistics.
 */
public class DebugBitmapAnimationFrameListener implements BitmapAnimationBackend.FrameListener {

  private static final Class<?> TAG = DebugBitmapAnimationFrameListener.class;

  private int mCachedCount;
  private int mReusedCount;
  private int mCreatedCount;
  private int mFallbackCount;
  private int mUnknownCount;

  private int mDroppedFrameCount;

  private long mLastFrameStart;

  @Override
  public void onDrawFrameStart(BitmapAnimationBackend backend, int frameNumber) {
    mLastFrameStart = System.currentTimeMillis();
    FLog.d(TAG, "Frame: event=start, number=%d", frameNumber);
  }

  @Override
  public void onFrameDrawn(
      BitmapAnimationBackend backend,
      int frameNumber,
      @BitmapAnimationBackend.FrameType int frameType) {
    increaseFrameTypeCount(frameType);
    FLog.d(
        TAG,
        "Frame: event=drawn, number=%d, type=%s, render_time=%d ms",
        frameNumber,
        getFrameTypeName(frameType),
        System.currentTimeMillis() - mLastFrameStart);
    logStatistics();
  }

  @Override
  public void onFrameDropped(BitmapAnimationBackend backend, int frameNumber) {
    mDroppedFrameCount++;
    FLog.d(
        TAG,
        "Frame: event=dropped, number=%d, render_time=%d ms",
        frameNumber,
        System.currentTimeMillis() - mLastFrameStart);
    logStatistics();
  }

  private void logStatistics() {
    FLog.d(
        TAG,
        "Stats: cached=%s, reused=%s, created=%s, fallback=%s, dropped=%s, unknown=%s",
        mCachedCount,
        mReusedCount,
        mCreatedCount,
        mFallbackCount,
        mDroppedFrameCount,
        mUnknownCount);
  }

  private void increaseFrameTypeCount(@BitmapAnimationBackend.FrameType int frameType) {
    switch (frameType) {
      case BitmapAnimationBackend.FRAME_TYPE_CACHED:
        mCachedCount++;
        break;
      case BitmapAnimationBackend.FRAME_TYPE_REUSED:
        mReusedCount++;
        break;
      case BitmapAnimationBackend.FRAME_TYPE_CREATED:
        mCreatedCount++;
        break;
      case BitmapAnimationBackend.FRAME_TYPE_FALLBACK:
        mFallbackCount++;
        break;
      case BitmapAnimationBackend.FRAME_TYPE_UNKNOWN:
      default:
        mUnknownCount++;
        break;
    }
  }

  private static String getFrameTypeName(@BitmapAnimationBackend.FrameType int frameType) {
    switch (frameType) {
      case BitmapAnimationBackend.FRAME_TYPE_CACHED:
        return "cached";
      case BitmapAnimationBackend.FRAME_TYPE_REUSED:
        return "reused";
      case BitmapAnimationBackend.FRAME_TYPE_CREATED:
        return "created";
      case BitmapAnimationBackend.FRAME_TYPE_FALLBACK:
        return "fallback";
      case BitmapAnimationBackend.FRAME_TYPE_UNKNOWN:
      default:
        return "unknown";
    }
  }
}
