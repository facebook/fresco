/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imagepipeline.core;

import com.facebook.common.logging.FLog;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.common.references.SharedReference;
import com.facebook.imagepipeline.debug.CloseableReferenceLeakTracker;
import java.io.Closeable;

public class CloseableReferenceFactory {

  private final CloseableReference.LeakHandler mLeakHandler;

  public CloseableReferenceFactory(
      final CloseableReferenceLeakTracker closeableReferenceLeakTracker) {
    mLeakHandler =
        new CloseableReference.LeakHandler() {
          @Override
          public void reportLeak(SharedReference<Object> reference) {
            closeableReferenceLeakTracker.trackCloseableReferenceLeak(reference);
            FLog.w(
                "Fresco",
                "Finalized without closing: %x %x (type = %s)",
                System.identityHashCode(this),
                System.identityHashCode(reference),
                reference.get().getClass().getName());
          }
        };
  }

  public <U extends Closeable> CloseableReference<U> create(U u) {
    return CloseableReference.of(u, mLeakHandler);
  }

  public <T> CloseableReference<T> create(T t, ResourceReleaser<T> resourceReleaser) {
    return CloseableReference.of(t, resourceReleaser, mLeakHandler);
  }
}
