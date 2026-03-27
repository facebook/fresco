/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

import android.graphics.Bitmap;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/** {@link CloseableImage} that wraps a bitmap. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface CloseableBitmap extends CloseableImage {

  /**
   * Gets the underlying bitmap. Note: care must be taken because subclasses might be more
   * sophisticated than that. For example, animated bitmap may have many frames and this method will
   * only return the first one.
   *
   * @return the underlying bitmap, or null if the image has been closed
   */
  @Nullable
  Bitmap getUnderlyingBitmap();
}
