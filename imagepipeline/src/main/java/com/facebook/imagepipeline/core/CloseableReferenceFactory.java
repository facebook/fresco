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
import com.facebook.infer.annotation.Nullsafe;
import java.io.Closeable;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.STRICT)
public class CloseableReferenceFactory {

  private final CloseableReference.LeakHandler mLeakHandler;

  public CloseableReferenceFactory(
      final CloseableReferenceLeakTracker closeableReferenceLeakTracker) {
    mLeakHandler =
        new CloseableReference.LeakHandler() {
          @Override
          public void reportLeak(
              SharedReference<Object> reference, @Nullable Throwable stacktrace) {
            closeableReferenceLeakTracker.trackCloseableReferenceLeak(reference, stacktrace);
            Object value = reference.get();
            String name = value != null ? value.getClass().getName() : "<value is null>";
            FLog.w(
                "Fresco",
                "Finalized without closing: %x %x (type = %s).\nStack:\n%s",
                System.identityHashCode(this),
                System.identityHashCode(reference),
                name,
                getStackTraceString(stacktrace));
          }

          @Override
          public boolean requiresStacktrace() {
            return closeableReferenceLeakTracker.isSet();
          }
        };
  }

  public <U extends Closeable> CloseableReference<U> create(U u) {
    return CloseableReference.of(u, mLeakHandler);
  }

  public <T> CloseableReference<T> create(T t, ResourceReleaser<T> resourceReleaser) {
    return CloseableReference.of(t, resourceReleaser, mLeakHandler);
  }

  /**
   * Get a loggable stack trace from a Throwable
   *
   * @param tr An exception to log
   */
  private static String getStackTraceString(@Nullable Throwable tr) {
    if (tr == null) {
      return "";
    }
    final StringWriter sw = new StringWriter();
    final PrintWriter pw = new PrintWriter(sw);
    tr.printStackTrace(pw);
    return sw.toString();
  }
}
