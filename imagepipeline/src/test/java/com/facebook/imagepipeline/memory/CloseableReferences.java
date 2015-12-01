/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import java.io.Closeable;

import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.SharedReference;

import org.mockito.*;

import static org.mockito.Matchers.*;

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
