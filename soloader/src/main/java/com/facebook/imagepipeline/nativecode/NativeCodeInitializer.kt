/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode

import android.content.Context
import com.facebook.common.internal.DoNotStrip
import com.facebook.soloader.SoLoader
import java.io.IOException
import kotlin.jvm.JvmStatic

/** Delegate to properly set and initialize NativeLoader and SoLoader. */
@DoNotStrip
object NativeCodeInitializer {
  /** Initialize NativeLoader and SoLoader */
  @JvmStatic
  @DoNotStrip
  @Throws(IOException::class)
  fun init(context: Context?) {
    SoLoader.init(context, 0)
  }
}
