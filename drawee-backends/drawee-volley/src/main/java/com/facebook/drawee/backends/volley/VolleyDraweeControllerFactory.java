/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.backends.volley;

import android.content.res.Resources;
import android.graphics.Bitmap;

import com.facebook.common.internal.Supplier;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.components.DeferredReleaser;

import java.util.concurrent.Executor;

/**
 * Factory for Volley Drawee controllers.
 */
public class VolleyDraweeControllerFactory {

  private Resources mResources;
  private DeferredReleaser mDeferredReleaser;
  private Executor mUiThreadExecutor;

  public VolleyDraweeControllerFactory(
      Resources resources,
      DeferredReleaser deferredReleaser,
      Executor uiThreadExecutor) {
    mResources = resources;
    mDeferredReleaser = deferredReleaser;
    mUiThreadExecutor = uiThreadExecutor;
  }

  public VolleyDraweeController newController(
      Supplier<DataSource<Bitmap>> dataSourceSupplier,
      String id,
      Object callerContext) {
    return new VolleyDraweeController(
        mResources,
        mDeferredReleaser,
        mUiThreadExecutor,
        dataSourceSupplier,
        id,
        callerContext);
  }
}
