/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.memory;

import static org.mockito.Matchers.*;

import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.SharedReference;
import java.io.Closeable;
import org.mockito.*;

/**
 * Utilities for testing {@link CloseableReference}.
 */
public class CloseableReferences {
  private static class CloseableReferenceMatcher<T extends Closeable>
      extends ArgumentMatcher<CloseableReference<T>> {

    private final CloseableReference<T> mCloseableReference;

    public CloseableReferenceMatcher(CloseableReference<T> closeableReference) {
      mCloseableReference = closeableReference;
    }

    @Override
    public boolean matches(Object argument) {
      if (!(argument instanceof CloseableReference)) {
        return false;
      }

      return mCloseableReference.getUnderlyingReferenceTestOnly() ==
          ((CloseableReference) argument).getUnderlyingReferenceTestOnly();
    }
  }

  /**
   * Returns a Mockito ArgumentMatcher that checks that its argument has the same underlying
   * {@link SharedReference}
   */
  public static <T extends Closeable> CloseableReference<T> eqUnderlying(
      CloseableReference<T> closeableReference) {
    return argThat(new CloseableReferenceMatcher<T>(closeableReference));
  }
}
