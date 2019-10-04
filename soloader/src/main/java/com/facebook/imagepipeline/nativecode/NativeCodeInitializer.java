/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.nativecode;

import android.content.Context;
import com.facebook.common.internal.DoNotStrip;
import com.facebook.soloader.SoLoader;
import java.io.IOException;

/** Delegate to properly set and initialize NativeLoader and SoLoader. */
@DoNotStrip
public class NativeCodeInitializer {

  /** Initialize NativeLoader and SoLoader */
  @DoNotStrip
  public static void init(Context context) throws IOException {
    SoLoader.init(context, 0);
  }
}
