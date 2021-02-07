/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.samples.showcase.misc;

import android.util.Log;
import com.facebook.drawee.backends.pipeline.info.ImageLoadStatus;
import com.facebook.drawee.backends.pipeline.info.ImagePerfData;
import com.facebook.drawee.backends.pipeline.info.ImagePerfDataListener;
import com.facebook.drawee.backends.pipeline.info.ImagePerfUtils;
import com.facebook.drawee.backends.pipeline.info.VisibilityState;
import java.util.Locale;

public class LogcatImagePerfDataListener implements ImagePerfDataListener {

  private static final String TAG = "ImagePerf";

  @Override
  public void onImageLoadStatusUpdated(
      ImagePerfData imagePerfData, @ImageLoadStatus int imageLoadStatus) {
    Log.d(
        TAG,
        String.format(
            (Locale) null,
            "status=%s, data=%s",
            ImagePerfUtils.toString(imageLoadStatus),
            imagePerfData.createDebugString()));
  }

  @Override
  public void onImageVisibilityUpdated(
      ImagePerfData imagePerfData, @VisibilityState int visibility) {
    Log.d(
        TAG,
        String.format(
            (Locale) null,
            "visibility=%s, data=%s",
            ImagePerfUtils.toString(imagePerfData.getVisibilityState()),
            imagePerfData.createDebugString()));
  }
}
