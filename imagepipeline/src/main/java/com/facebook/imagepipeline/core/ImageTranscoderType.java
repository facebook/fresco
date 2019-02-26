/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.core;

import static com.facebook.imagepipeline.core.ImageTranscoderType.JAVA_TRANSCODER;
import static com.facebook.imagepipeline.core.ImageTranscoderType.NATIVE_TRANSCODER;
import static java.lang.annotation.RetentionPolicy.SOURCE;

import androidx.annotation.IntDef;
import java.lang.annotation.Retention;

/**
 * Image transcoder type that indicates which kind of image transcoder implementation will be used.
 */
@Retention(SOURCE)
@IntDef({
  NATIVE_TRANSCODER,
  JAVA_TRANSCODER,
})
public @interface ImageTranscoderType {
  int NATIVE_TRANSCODER = 0;
  int JAVA_TRANSCODER = 1;
}
