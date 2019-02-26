/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.drawee.backends.pipeline.info;

import static com.facebook.drawee.backends.pipeline.info.ImageLoadStatus.CANCELED;
import static com.facebook.drawee.backends.pipeline.info.ImageLoadStatus.ERROR;
import static com.facebook.drawee.backends.pipeline.info.ImageLoadStatus.INTERMEDIATE_AVAILABLE;
import static com.facebook.drawee.backends.pipeline.info.ImageLoadStatus.ORIGIN_AVAILABLE;
import static com.facebook.drawee.backends.pipeline.info.ImageLoadStatus.REQUESTED;
import static com.facebook.drawee.backends.pipeline.info.ImageLoadStatus.SUCCESS;
import static com.facebook.drawee.backends.pipeline.info.ImageLoadStatus.UNKNOWN;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;

@Retention(SOURCE)
@IntDef({
  UNKNOWN,
  REQUESTED,
  ORIGIN_AVAILABLE,
  INTERMEDIATE_AVAILABLE,
  SUCCESS,
  CANCELED,
  ERROR,
})
public @interface ImageLoadStatus {

  int UNKNOWN = -1;
  int REQUESTED = 0;
  int ORIGIN_AVAILABLE = 1;
  int INTERMEDIATE_AVAILABLE = 2;
  int SUCCESS = 3;
  int CANCELED = 4;
  int ERROR = 5;
}
