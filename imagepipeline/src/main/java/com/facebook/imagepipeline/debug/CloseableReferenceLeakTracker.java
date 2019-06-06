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
    void onCloseableReferenceLeak(SharedReference<Object> reference);
  }

  void trackCloseableReferenceLeak(SharedReference<Object> reference);

  void setListener(@Nullable Listener listener);
}
