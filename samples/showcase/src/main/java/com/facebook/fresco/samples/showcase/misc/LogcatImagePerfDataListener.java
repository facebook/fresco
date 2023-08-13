/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.misc;

import android.util.Log;
import com.facebook.fresco.ui.common.ImageLoadStatus;
import com.facebook.fresco.ui.common.ImagePerfData;
import com.facebook.fresco.ui.common.ImagePerfDataListener;
import com.facebook.fresco.ui.common.VisibilityState;
import java.util.Locale;

public class LogcatImagePerfDataListener implements ImagePerfDataListener {

  private static final String TAG = "ImagePerf";

  @Override
  public void onImageLoadStatusUpdated(
      ImagePerfData imagePerfData, ImageLoadStatus imageLoadStatus) {
    Log.d(
        TAG,
        String.format(
            (Locale) null,
            "status=%s, data=%s",
            imageLoadStatus.toString(),
            imagePerfData.createDebugString()));
  }

  @Override
  public void onImageVisibilityUpdated(ImagePerfData imagePerfData, VisibilityState visibility) {
    Log.d(
        TAG,
        String.format(
            (Locale) null,
            "visibility=%s, data=%s",
            imagePerfData.getVisibilityState(),
            imagePerfData.createDebugString()));
  }
}
