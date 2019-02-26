/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info;

import static com.facebook.drawee.backends.pipeline.info.ImageOrigin.DISK;
import static com.facebook.drawee.backends.pipeline.info.ImageOrigin.LOCAL;
import static com.facebook.drawee.backends.pipeline.info.ImageOrigin.MEMORY_BITMAP;
import static com.facebook.drawee.backends.pipeline.info.ImageOrigin.MEMORY_ENCODED;
import static com.facebook.drawee.backends.pipeline.info.ImageOrigin.NETWORK;
import static com.facebook.drawee.backends.pipeline.info.ImageOrigin.UNKNOWN;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;

/**
 * Image origin that indicates whether an image has been loaded from cache, network or other source.
 */
@Retention(SOURCE)
@IntDef({
  UNKNOWN,
  NETWORK,
  DISK,
  MEMORY_ENCODED,
  MEMORY_BITMAP,
  LOCAL,
})
public @interface ImageOrigin {

  int UNKNOWN = 1;
  int NETWORK = 2;
  int DISK = 3;
  int MEMORY_ENCODED = 4;
  int MEMORY_BITMAP = 5;
  int LOCAL = 6;
}
