/*
 * Copyright (c) 2017-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.drawee.backends.pipeline.info;

import static com.facebook.drawee.backends.pipeline.info.ImageOrigin.DISK;
import static com.facebook.drawee.backends.pipeline.info.ImageOrigin.MEMORY_BITMAP;
import static com.facebook.drawee.backends.pipeline.info.ImageOrigin.MEMORY_ENCODED;
import static com.facebook.drawee.backends.pipeline.info.ImageOrigin.NETWORK;
import static com.facebook.drawee.backends.pipeline.info.ImageOrigin.UNKNOWN;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import android.support.annotation.IntDef;
import java.lang.annotation.Retention;

/**
 * Image origin that indicates whether an image has been loaded from cache, network or other source.
 */
@Retention(SOURCE)
@IntDef({
  UNKNOWN,
  NETWORK,
  DISK,
  MEMORY_BITMAP,
  MEMORY_ENCODED,
})
public @interface ImageOrigin {

  int UNKNOWN = -1;
  int NETWORK = 0;
  int DISK = 1;
  int MEMORY_BITMAP = 2;
  int MEMORY_ENCODED = 3;
}
