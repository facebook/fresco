/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.provider;

import android.content.Context;
import com.facebook.fresco.vito.core.FrescoContext;
import javax.annotation.Nullable;

public class FrescoContextProvider {

  public interface Implementation {
    FrescoContext getFrescoContext(Context context, @Nullable Object callerContext);
  }

  @Nullable private static Implementation sImplementation = null;

  public static synchronized FrescoContext get(Context context, @Nullable Object callerContext) {
    if (sImplementation == null) {
      throw new RuntimeException("Fresco context provider must be set");
    }
    return sImplementation.getFrescoContext(context, callerContext);
  }

  public static synchronized void setImplementation(Implementation implementation) {
    sImplementation = implementation;
  }
}
