/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.view;

import android.net.Uri;
import android.view.View;
import androidx.annotation.Nullable;
import com.facebook.common.logging.FLog;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.fresco.vito.source.ImageSourceProvider;
import com.facebook.infer.annotation.Nullsafe;

/** You must initialize this class before use by calling {@link #init(Implementation)}. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class VitoView {

  public interface Implementation {
    void show(
        ImageSource imageSource,
        ImageOptions imageOptions,
        @Nullable Object callerContext,
        @Nullable ImageListener imageListener,
        final View target);
  }

  private static final Class<?> TAG = VitoView.class;
  private static volatile boolean sIsInitialized = false;

  private static @Nullable Implementation sImplementation;

  private VitoView() {}

  public static synchronized void init(@Nullable Implementation implementation) {
    if (implementation == null) {
      sImplementation = null;
      sIsInitialized = false;
      return;
    }
    if (sIsInitialized) {
      FLog.w(TAG, "VitoView has already been initialized!");
      return;
    } else {
      sIsInitialized = true;
    }
    sImplementation = implementation;
  }

  /*
   * Display an image with default image options
   */
  public static void show(@Nullable Uri uri, View target) {
    show(ImageSourceProvider.forUri(uri), target);
  }

  /*
   * Display an image with default image options and a caller context.
   */
  public static void show(@Nullable Uri uri, Object callerContext, View target) {
    show(ImageSourceProvider.forUri(uri), callerContext, target);
  }

  /*
   * Display an image with default image options
   */
  public static void show(ImageSource imageSource, View target) {
    getImplementation().show(imageSource, ImageOptions.defaults(), null, null, target);
  }

  /*
   * Display an image with default image options and a caller context.
   */
  public static void show(ImageSource imageSource, Object callerContext, View target) {
    getImplementation().show(imageSource, ImageOptions.defaults(), callerContext, null, target);
  }

  /*
   * Display an image with the given image options
   */
  public static void show(@Nullable Uri uri, ImageOptions imageOptions, final View target) {
    show(ImageSourceProvider.forUri(uri), imageOptions, target);
  }

  /*
   * Display an image with the given image options
   */
  public static void show(ImageSource imageSource, ImageOptions imageOptions, final View target) {
    getImplementation().show(imageSource, imageOptions, null, null, target);
  }

  /*
   * Display an image
   */
  public static void show(
      @Nullable Uri uri,
      ImageOptions imageOptions,
      @Nullable Object callerContext,
      final View target) {
    show(ImageSourceProvider.forUri(uri), imageOptions, callerContext, target);
  }

  /*
   * Display an image
   */
  public static void show(
      ImageSource imageSource,
      ImageOptions imageOptions,
      @Nullable Object callerContext,
      final View target) {
    getImplementation().show(imageSource, imageOptions, callerContext, null, target);
  }

  /*
   * Display an image
   */
  public static void show(
      @Nullable Uri uri,
      ImageOptions imageOptions,
      @Nullable Object callerContext,
      @Nullable ImageListener imageListener,
      final View target) {
    show(ImageSourceProvider.forUri(uri), imageOptions, callerContext, imageListener, target);
  }

  /*
   * Display an image
   */
  public static void show(
      ImageSource imageSource,
      ImageOptions imageOptions,
      @Nullable Object callerContext,
      @Nullable ImageListener imageListener,
      final View target) {
    getImplementation().show(imageSource, imageOptions, callerContext, imageListener, target);
  }

  public static synchronized Implementation getImplementation() {
    if (sImplementation == null) {
      throw new RuntimeException("Vito View implementation must be set!");
    }
    return sImplementation;
  }
}
