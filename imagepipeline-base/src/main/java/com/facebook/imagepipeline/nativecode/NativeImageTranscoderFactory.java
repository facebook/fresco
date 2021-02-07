/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode;

import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory;
import com.facebook.infer.annotation.Nullsafe;
import java.lang.reflect.InvocationTargetException;

/** Returns the native {@link ImageTranscoderFactory} if it is instantiable via reflection */
@Nullsafe(Nullsafe.Mode.STRICT)
public final class NativeImageTranscoderFactory {

  private NativeImageTranscoderFactory() {}

  public static ImageTranscoderFactory getNativeImageTranscoderFactory(
      final int maxBitmapSize,
      final boolean useDownSamplingRatio,
      final boolean ensureTranscoderLibraryLoaded) {
    try {
      return (ImageTranscoderFactory)
          Class.forName("com.facebook.imagepipeline.nativecode.NativeJpegTranscoderFactory")
              .getConstructor(Integer.TYPE, Boolean.TYPE, Boolean.TYPE)
              .newInstance(maxBitmapSize, useDownSamplingRatio, ensureTranscoderLibraryLoaded);
    } catch (NoSuchMethodException
        | SecurityException
        | InstantiationException
        | InvocationTargetException
        | IllegalAccessException
        | IllegalArgumentException
        | ClassNotFoundException e) {
      throw new RuntimeException(
          "Dependency ':native-imagetranscoder' is needed to use the default native image transcoder.",
          e);
    }
  }
}
