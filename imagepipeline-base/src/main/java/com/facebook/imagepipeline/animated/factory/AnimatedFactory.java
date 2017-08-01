/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.animated.factory;

import android.content.Context;
import android.graphics.Bitmap;
import com.facebook.imagepipeline.decoder.ImageDecoder;
import com.facebook.imagepipeline.drawable.DrawableFactory;
import javax.annotation.Nullable;
import javax.annotation.concurrent.NotThreadSafe;

@NotThreadSafe
public interface AnimatedFactory {

  @Nullable DrawableFactory getAnimatedDrawableFactory(Context context);

  @Nullable ImageDecoder getGifDecoder(Bitmap.Config config);

  @Nullable ImageDecoder getWebPDecoder(Bitmap.Config config);
}
