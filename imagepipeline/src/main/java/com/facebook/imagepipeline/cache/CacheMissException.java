/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import com.facebook.common.util.ExceptionWithNoStacktrace;
import com.facebook.infer.annotation.Nullsafe;

/** Exception to indicate an image was not found in the cache. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class CacheMissException extends ExceptionWithNoStacktrace {

  public CacheMissException(String detailMessage) {
    super(detailMessage);
  }
}
