/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.request;

import android.graphics.Bitmap;

/**
 * Use an instance of this class to perform post-process operations on a bitmap.
 *
 * <p>Postprocessors are not supported on Gingerbread and below.
 */
public interface Postprocessor {

  /**
   * Called by the pipeline after completing other steps.
   *
   * @param bitmap A bitmap that will be exclusively owned by the caller of the image pipeline.
   * This bitmap will not be the same object stored in memory cache. The implementation is free
   * to modify this Bitmap in-place. This Bitmap, as modified, will be returned
   * as the output of the pipeline. A new object, unmodified, will be created for every request
   * made to the pipeline.
   */
  void process(Bitmap bitmap);

  /**
   * Returns the name of this postprocessor.
   *
   * <p>Used for logging and analytics.
   */
  String getName();
}
