/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode

import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory
import java.lang.reflect.InvocationTargetException

/** Returns the native [ImageTranscoderFactory] if it is instantiable via reflection */
object NativeImageTranscoderFactory {
  @JvmStatic
  fun getNativeImageTranscoderFactory(
      maxBitmapSize: Int,
      useDownSamplingRatio: Boolean,
      ensureTranscoderLibraryLoaded: Boolean
  ): ImageTranscoderFactory =
      try {
        Class.forName("com.facebook.imagepipeline.nativecode.NativeJpegTranscoderFactory")
            .getConstructor(Integer.TYPE, java.lang.Boolean.TYPE, java.lang.Boolean.TYPE)
            .newInstance(maxBitmapSize, useDownSamplingRatio, ensureTranscoderLibraryLoaded)
            as ImageTranscoderFactory
      } catch (e: NoSuchMethodException) {
        throw RuntimeException(
            "Dependency ':native-imagetranscoder' is needed to use the default native image transcoder.",
            e)
      } catch (e: SecurityException) {
        throw RuntimeException(
            "Dependency ':native-imagetranscoder' is needed to use the default native image transcoder.",
            e)
      } catch (e: InstantiationException) {
        throw RuntimeException(
            "Dependency ':native-imagetranscoder' is needed to use the default native image transcoder.",
            e)
      } catch (e: InvocationTargetException) {
        throw RuntimeException(
            "Dependency ':native-imagetranscoder' is needed to use the default native image transcoder.",
            e)
      } catch (e: IllegalAccessException) {
        throw RuntimeException(
            "Dependency ':native-imagetranscoder' is needed to use the default native image transcoder.",
            e)
      } catch (e: IllegalArgumentException) {
        throw RuntimeException(
            "Dependency ':native-imagetranscoder' is needed to use the default native image transcoder.",
            e)
      } catch (e: ClassNotFoundException) {
        throw RuntimeException(
            "Dependency ':native-imagetranscoder' is needed to use the default native image transcoder.",
            e)
      }
}
