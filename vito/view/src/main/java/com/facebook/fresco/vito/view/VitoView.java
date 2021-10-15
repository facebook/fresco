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
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.source.ImageSource;
import com.facebook.fresco.vito.source.ImageSourceProvider;
import com.facebook.fresco.vito.view.impl.VitoViewImpl2;
import com.facebook.infer.annotation.Nullsafe;

/** Load images into an ImageView or a plain View. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class VitoView {

  private VitoView() {}

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
    VitoViewImpl2.show(imageSource, ImageOptions.defaults(), null, null, target);
  }

  /*
   * Display an image with default image options and a caller context.
   */
  public static void show(ImageSource imageSource, Object callerContext, View target) {
    VitoViewImpl2.show(imageSource, ImageOptions.defaults(), callerContext, null, target);
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
    VitoViewImpl2.show(imageSource, imageOptions, null, null, target);
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
    VitoViewImpl2.show(imageSource, imageOptions, callerContext, null, target);
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
    VitoViewImpl2.show(imageSource, imageOptions, callerContext, imageListener, target);
  }
}
