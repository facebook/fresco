/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

import com.facebook.common.references.HasBitmap;
import com.facebook.fresco.middleware.HasExtraData;
import com.facebook.infer.annotation.Nullsafe;
import java.io.Closeable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public interface CloseableImage extends Closeable, ImageInfo, HasBitmap, HasExtraData {
  /**
   * @return size in bytes of the bitmap(s)
   */
  int getSizeInBytes();

  /** Closes this instance and releases the resources. */
  @Override
  void close();

  /** Returns whether this instance is closed. */
  boolean isClosed();

  boolean isStateful();

  /**
   * @return width of the image
   */
  int getWidth();

  /**
   * @return height of the image
   */
  int getHeight();

  /**
   * @return quality information for the image
   */
  QualityInfo getQualityInfo();

  ImageInfo getImageInfo();
}
