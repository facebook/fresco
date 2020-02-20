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

public class DefaultFrescoContextProvider implements FrescoContextProvider.Implementation {
  @Override
  public FrescoContext getFrescoContext(Context context, @Nullable Object callerContext) {
    return DefaultFrescoContext.get(context.getResources());
  }
}
