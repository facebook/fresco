/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

import com.facebook.common.references.HasBitmap;
import com.facebook.infer.annotation.Nullsafe;
import java.io.Closeable;
import java.util.Map;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
public interface CloseableImage extends Closeable, ImageInfo, HasBitmap {
  /** @return size in bytes of the bitmap(s) */
  int getSizeInBytes();

  /** Closes this instance and releases the resources. */
  @Override
  void close();

  /** Returns whether this instance is closed. */
  boolean isClosed();

  void setImageExtras(@Nullable Map<String, Object> extras);

  void setImageExtra(String extra, Object value);

  boolean isStateful();
}
