/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.impl;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.SystemClock;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.TypedValue;

import com.facebook.common.logging.FLog;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableCachingBackend;
import com.facebook.imagepipeline.animated.base.AnimatedDrawableDiagnostics;
import com.facebook.imagepipeline.animated.util.AnimatedDrawableUtil;

/**
 * Implementation of {@link AnimatedDrawableDiagnostics} that logs extra information and draws
 * a debug overlay..
 */
public class AnimatedDrawableDiagnosticsImpl implements AnimatedDrawableDiagnostics {

  private static final Class<?> TAG = AnimatedDrawableDiagnostics.class;

  private final AnimatedDrawableUtil mAnimatedDrawableUtil;
  private final DisplayMetrics mDisplayMetrics;
  private final TextPaint mDebugTextPaint;
  private final StringBuilder sbTemp;
  private final RollingStat mDroppedFramesStat;
  private final RollingStat mDrawnFrames;

  private AnimatedDrawableCachingBackend mAnimatedDrawableBackend;

  private long mLastTimeStamp;

  public AnimatedDrawableDiagnosticsImpl(
      AnimatedDrawableUtil animatedDrawableUtil,
      DisplayMetrics displayMetrics) {
    mAnimatedDrawableUtil = animatedDrawableUtil;
    mDisplayMetrics = displayMetrics;
    mDroppedFramesStat = new RollingStat();
    mDrawnFrames = new RollingStat();
    sbTemp = new StringBuilder();
    mDebugTextPaint = new TextPaint();
    mDebugTextPaint.setColor(Color.BLUE);
    mDebugTextPaint.setTextSize(convertDpToPx(14));
  }

  @Override
  public void setBackend(AnimatedDrawableCachingBackend animatedDrawableBackend) {
    mAnimatedDrawableBackend = animatedDrawableBackend;
  }

  @Override
  public void onStartMethodBegin() {
    mLastTimeStamp = SystemClock.elapsedRealtime();
  }

  @Override
  public void onStartMethodEnd() {
    long elapsedMs = SystemClock.elapsedRealtime() - mLastTimeStamp;
    if (elapsedMs > 3) {
      FLog.v(TAG, "onStart took %d", elapsedMs);
    }
  }

  @Override
  public void onNextFrameMethodBegin() {
    mLastTimeStamp = SystemClock.elapsedRealtime();
  }

  @Override
  public void onNextFrameMethodEnd() {
    long elapsedMs = SystemClock.elapsedRealtime() - mLastTimeStamp;
    if (elapsedMs > 3) {
      FLog.v(TAG, "onNextFrame took %d", elapsedMs);
    }
  }

  @Override
  public void incrementDroppedFrames(int droppedFrames) {
    mDroppedFramesStat.incrementStats(droppedFrames);
    if (droppedFrames > 0) {
      FLog.v(TAG, "Dropped %d frames", droppedFrames);
    }
  }

  @Override
  public void incrementDrawnFrames(int drawnFrames) {
    mDrawnFrames.incrementStats(drawnFrames);
  }

  @Override
  public void onDrawMethodBegin() {
    mLastTimeStamp = SystemClock.elapsedRealtime();
  }

  @Override
  public void onDrawMethodEnd() {
    long elapsedMs = SystemClock.elapsedRealtime() - mLastTimeStamp;
    FLog.v(TAG, "draw took %d", elapsedMs);
  }

  public void drawDebugOverlay(Canvas canvas, Rect destRect) {
    // Running percentage of frames shown (i.e. drop rate).
    int droppedFrame10 = mDroppedFramesStat.getSum(10);
    int drawnFrames10 = mDrawnFrames.getSum(10);
    int totalFrames = drawnFrames10 + droppedFrame10;
    int leftMargin = convertDpToPx(10);
    int x = leftMargin;
    int y = convertDpToPx(20);
    int spacingBetweenTextPx = convertDpToPx(5);
    if (totalFrames > 0) {
      int percentage = drawnFrames10 * 100 / totalFrames;
      sbTemp.setLength(0);
      sbTemp.append(percentage);
      sbTemp.append("%");
      canvas.drawText(sbTemp, 0, sbTemp.length(), x, y, mDebugTextPaint);
      x += mDebugTextPaint.measureText(sbTemp, 0, sbTemp.length());
      x += spacingBetweenTextPx;
    }

    // Memory usage.
    int bytesUsed = mAnimatedDrawableBackend.getMemoryUsage();
    sbTemp.setLength(0);
    mAnimatedDrawableUtil.appendMemoryString(sbTemp, bytesUsed);
    float textWidth = mDebugTextPaint.measureText(sbTemp, 0, sbTemp.length());
    if (x + textWidth > destRect.width()) {
      x = leftMargin;
      y += mDebugTextPaint.getTextSize() + spacingBetweenTextPx;
    }
    canvas.drawText(sbTemp, 0, sbTemp.length(), x, y, mDebugTextPaint);
    x += textWidth;
    x += spacingBetweenTextPx;

    // Options
    sbTemp.setLength(0);
    mAnimatedDrawableBackend.appendDebugOptionString(sbTemp);
    textWidth = mDebugTextPaint.measureText(sbTemp, 0, sbTemp.length());
    if (x + textWidth > destRect.width()) {
      x = leftMargin;
      y += mDebugTextPaint.getTextSize() + spacingBetweenTextPx;
    }
    canvas.drawText(sbTemp, 0, sbTemp.length(), x, y, mDebugTextPaint);
  }

  private int convertDpToPx(int dips) {
    return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dips, mDisplayMetrics);
  }
}
