/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.ui.common;

import android.net.Uri;
import com.facebook.common.internal.Fn;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public abstract class MultiUriHelper {

  @Nullable
  public static <T> Uri getMainUri(
      @Nullable T mainRequest,
      @Nullable T lowResRequest,
      @Nullable T[] firstAvailableRequest,
      Fn<T, Uri> requestToUri) {

    if (mainRequest != null) {
      Uri uri = requestToUri.apply(mainRequest);
      if (uri != null) return uri;
    }

    if (firstAvailableRequest != null
        && firstAvailableRequest.length > 0
        && firstAvailableRequest[0] != null) {
      Uri uri = requestToUri.apply(firstAvailableRequest[0]);
      if (uri != null) return uri;
    }

    if (lowResRequest != null) return requestToUri.apply(lowResRequest);

    return null;
  }
}
