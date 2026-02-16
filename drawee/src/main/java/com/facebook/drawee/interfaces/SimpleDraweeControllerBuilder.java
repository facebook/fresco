/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.interfaces;

import android.net.Uri;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/** Interface for simple Drawee controller builders. */
@Nullsafe(Nullsafe.Mode.LOCAL)
public interface SimpleDraweeControllerBuilder {

  /** Sets the caller context. */
  SimpleDraweeControllerBuilder setCallerContext(@Nullable Object callerContext);

  /** Sets the uri. */
  SimpleDraweeControllerBuilder setUri(@Nullable Uri uri);

  /** Sets the uri from a string. */
  SimpleDraweeControllerBuilder setUri(@Nullable String uriString);

  /** Sets the old controller to be reused if possible. */
  SimpleDraweeControllerBuilder setOldController(@Nullable DraweeController oldController);

  /** Builds the specified controller. */
  DraweeController build();
}
