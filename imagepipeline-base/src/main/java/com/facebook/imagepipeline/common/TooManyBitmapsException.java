/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.common;

import com.facebook.infer.annotation.Nullsafe;

/** Thrown if a bitmap pool cap or other limit on the number of bitmaps is exceeded. */
@Nullsafe(Nullsafe.Mode.STRICT)
public class TooManyBitmapsException extends RuntimeException {

  public TooManyBitmapsException() {
    super();
  }

  public TooManyBitmapsException(String detailMessage) {
    super(detailMessage);
  }
}
