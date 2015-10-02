/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.drawee.interfaces;

import javax.annotation.Nullable;

import android.net.Uri;

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
