/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

import com.facebook.common.logging.FLog;
import com.facebook.infer.annotation.Nullsafe;

@Nullsafe(Nullsafe.Mode.LOCAL)
public abstract class DefaultCloseableImage extends BaseCloseableImage {
  private static final String TAG = "CloseableImage";

  /** Ensures that the underlying resources are always properly released. */
  @Override
  protected void finalize() throws Throwable {
    if (isClosed()) {
      return;
    }
    FLog.w(
        TAG,
        "finalize: %s %x still open.",
        this.getClass().getSimpleName(),
        System.identityHashCode(this));
    try {
      close();
    } finally {
      super.finalize();
    }
  }
}
