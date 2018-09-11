/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.backends.volley;

import android.content.res.Resources;
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

  public VolleyDraweeController newController() {
    return new VolleyDraweeController(mResources, mDeferredReleaser, mUiThreadExecutor);
  }
}
