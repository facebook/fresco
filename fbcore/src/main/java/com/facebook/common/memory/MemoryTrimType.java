/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.memory;

/**
 * Types of memory trim.
 *
 * <p>Each type of trim will provide a suggested trim ratio.
 *
 * <p>A {@link MemoryTrimmableRegistry} implementation sends out memory trim events with this type.
 */
public enum MemoryTrimType {

  /** The application is approaching the device-specific Java heap limit. */
  OnCloseToDalvikHeapLimit(0.5),

  /** The system as a whole is running critically low on memory, and app is in the foreground. */
  OnSystemMemoryCriticallyLowWhileAppInForeground(1),

  /** The system as a whole is running low of memory, and this application is in the foreground. */
  OnSystemLowMemoryWhileAppInForeground(0.5),

  /** The system as a whole is running out of memory, and this application is in the background. */
  OnSystemLowMemoryWhileAppInBackground(1),

  /** This app is moving into the background, usually because the user navigated to another app. */
  OnAppBackgrounded(1);

  private double mSuggestedTrimRatio;

  private MemoryTrimType(double suggestedTrimRatio) {
    mSuggestedTrimRatio = suggestedTrimRatio;
  }

  /** Get the recommended percentage by which to trim the cache on receiving this event. */
  public double getSuggestedTrimRatio () {
    return mSuggestedTrimRatio;
  }
}
