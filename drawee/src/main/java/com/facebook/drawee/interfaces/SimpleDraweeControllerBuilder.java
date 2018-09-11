/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.interfaces;

import android.net.Uri;
import javax.annotation.Nullable;

/**
 * Interface for simple Drawee controller builders.
 */
public interface SimpleDraweeControllerBuilder {

  /** Sets the caller context. */
  SimpleDraweeControllerBuilder setCallerContext(Object callerContext);

  /** Sets the uri. */
  SimpleDraweeControllerBuilder setUri(Uri uri);

  /** Sets the uri from a string. */
  SimpleDraweeControllerBuilder setUri(@Nullable String uriString);

  /** Sets the old controller to be reused if possible. */
  SimpleDraweeControllerBuilder setOldController(@Nullable DraweeController oldController);

  /** Builds the specified controller. */
  DraweeController build();
}
