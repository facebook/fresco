/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.image;

import android.net.Uri;
import javax.annotation.Nullable;

public class OriginalEncodedImageInfo {
  private final Uri mUri;
  private final @Nullable EncodedImageOrigin mOrigin;
  private final @Nullable Object mCallerContext;
  private final int mWidth;
  private final int mHeight;
  private final int mSize;

  public OriginalEncodedImageInfo(
      Uri sourceUri,
      EncodedImageOrigin origin,
      @Nullable Object callerContext,
      int width,
      int height,
      int size) {
    mUri = sourceUri;
    mOrigin = origin;
    mCallerContext = callerContext;
    mWidth = width;
    mHeight = height;
    mSize = size;
  }

  public int getWidth() {
    return mWidth;
  }

  public int getHeight() {
    return mHeight;
  }

  public int getSize() {
    return mSize;
  }

  public Uri getUri() {
    return mUri;
  }

  public @Nullable Object getCallerContext() {
    return mCallerContext;
  }

  public EncodedImageOrigin getOrigin() {
    return mOrigin;
  }
}
