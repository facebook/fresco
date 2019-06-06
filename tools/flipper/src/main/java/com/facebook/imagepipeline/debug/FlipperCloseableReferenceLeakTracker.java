/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.debug;

import android.util.Log;
import com.facebook.common.references.SharedReference;
import javax.annotation.Nullable;

public class FlipperCloseableReferenceLeakTracker implements CloseableReferenceLeakTracker {
  @Nullable private Listener mListener;

  @Override
  public void trackCloseableReferenceLeak(SharedReference<Object> reference) {
    if (mListener == null) {
      Log.w("FRESCO", "No Flipper listener registered to track CloseableReference leak.");
      return;
    }

    mListener.onCloseableReferenceLeak(reference);
  }

  @Override
  public void setListener(@Nullable Listener listener) {
    mListener = listener;
  }
}
