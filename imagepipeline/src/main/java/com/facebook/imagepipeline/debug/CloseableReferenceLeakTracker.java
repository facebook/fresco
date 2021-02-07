/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.debug;

import com.facebook.common.references.SharedReference;
import javax.annotation.Nullable;

/**
 * Tracker for leaks that occur when a {@link com.facebook.common.references.CloseableReference} is
 * not closed.
 */
public interface CloseableReferenceLeakTracker {
  interface Listener {
    void onCloseableReferenceLeak(
        SharedReference<Object> reference, @Nullable Throwable stacktrace);
  }

  void trackCloseableReferenceLeak(
      SharedReference<Object> reference, @Nullable Throwable stacktrace);

  void setListener(@Nullable Listener listener);

  /** Indicate whether or not a listener is set. */
  boolean isSet();
}
