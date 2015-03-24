/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.references;

import javax.annotation.concurrent.GuardedBy;

import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.internal.Preconditions;

/**
 * A shared-reference class somewhat similar to c++ shared_ptr. The underlying value is reference
 * counted, and when the count drops to zero, the underlying value is "disposed"
 * <p>
 * Unlike the c++ implementation, which provides for a bunch of syntactic sugar with copy
 * constructors and destructors, Java does not provide the equivalents. So we instead have the
 * explicit addReference() and deleteReference() calls, and we need to be extremely careful
 * about using these in the presence of exceptions, or even otherwise.
 * <p>
 * Despite the extra (and clunky) method calls, this is still worthwhile in many cases to avoid
 * the overhead of garbage collection.
 * <p>
 * The somewhat clunky rules are
 * 1. If a function returns a SharedReference, it must guarantee that the reference count
 *    is at least 1. In the case where a SharedReference is being constructed and returned,
 *    the SharedReference constructor will already set the ref count to 1.
 * 2. If a function calls another function with a shared-reference parameter,
 *    2.1 The caller must ensure that the reference is valid for the duration of the
 *        invocation.
 *    2.2 The callee *is not* responsible for the cleanup of the reference.
 *    2.3 If the callee wants to keep the reference around even after the call returns (for
 *        example, stashing it away in a map), then it should "clone" the reference by invoking
 *        {@link #addReference()}
 * <p>
 *   Example #1 (function with a shared reference parameter):
 *   void foo(SharedReference r, ...) {
 *     // first assert that the reference is valid
 *     Preconditions.checkArgument(SharedReference.isValid(r));
 *     ...
 *     // do something with the contents of r
 *     ...
 *     // do not increment/decrement the ref count
 *   }
 * <p>
 *   Example #2 (function with a shared reference parameter that keeps around the shared ref)
 *     void foo(SharedReference r, ...) {
 *       // first assert that the reference is valid
 *       Preconditions.checkArgument(SharedReference.isValid(r));
 *       ...
 *       // increment ref count
 *       r.addReference();
 *       // stash away the reference
 *       ...
 *       return;
 *     }
 * <p>
 *   Example #3 (function with a shared reference parameter that passes along the reference to
 *   another function)
 *     void foo(SharedReference r, ...) {
 *       // first assert that the reference is valid
 *       Preconditions.checkArgument(SharedReference.isValid(r));
 *       ...
 *       bar(r, ...); // call to other function
 *       ...
 *     }
 * <p>
 *   Example #4 (function that returns a shared reference)
 *     SharedReference foo(...) {
 *       // do something
 *       ...
 *       // create a new shared reference (refcount automatically at 1)
 *       SharedReference r = new SharedReference(x);
 *       // return this shared reference
 *       return r;
 *     }
 * <p>
 *   Example #5 (function with a shared reference parameter that returns the shared reference)
 *     void foo(SharedReference r, ...) {
 *       // first assert that the reference is valid
 *       Preconditions.checkArgument(SharedReference.isValid(r));
 *       ...
 *       // increment ref count before returning
 *       r.addReference();
 *       return r;
 *     }
 */
@VisibleForTesting
public class SharedReference<T> {
  private static final Class<?> TAG = SharedReference.class;

  @GuardedBy("this")
  private T mValue;
  @GuardedBy("this")
  private int mRefCount;

  private final ResourceReleaser<T> mResourceReleaser;

  /**
   * Construct a new shared-reference that will 'own' the supplied {@code value}.
   * The reference count will be set to 1. When the reference count decreases to zero
   * {@code resourceReleaser} will be used to release the {@code value}
   * @param value non-null value to manage
   * @param resourceReleaser non-null ResourceReleaser for the value
   */
  public SharedReference(T value, ResourceReleaser<T> resourceReleaser) {
    mValue = Preconditions.checkNotNull(value);
    mResourceReleaser = Preconditions.checkNotNull(resourceReleaser);
    mRefCount = 1;
  }

  /**
   * Get the current referenced value. Null if there's no value.
   * @return the referenced value
   */
  public synchronized T get() {
    return mValue;
  }

  /**
   * Checks if this shared-reference is valid i.e. its reference count is greater than zero.
   * @return true if shared reference is valid
   */
  public synchronized boolean isValid() {
    return mRefCount > 0;
  }

  /**
   * Checks if the shared-reference is valid i.e. its reference count is greater than zero
   * @return true if the shared reference is valid
   */
  public static boolean isValid(SharedReference<?> ref) {
    return ref != null && ref.isValid();
  }

  /**
   * Bump up the reference count for the shared reference
   * Note: The reference must be valid (aka not null) at this point
   */
  public synchronized void addReference() {
    ensureValid();
    mRefCount++;
  }

  /**
   * Decrement the reference count for the shared reference. If the reference count drops to zero,
   * then dispose of the referenced value
   */
  public void deleteReference() {
    if (decreaseRefCount() == 0) {
      mResourceReleaser.release(mValue);
      synchronized (this) {
        mValue = null;
      }
    }
  }

  /**
   * Decrements reference count for the shared reference. Returns value of mRefCount after
   * decrementing
   */
  private synchronized int decreaseRefCount() {
    ensureValid();
    Preconditions.checkArgument(mRefCount > 0);

    mRefCount--;
    return mRefCount;
  }

  /**
   * Assert that there is a valid referenced value. Throw a NullReferenceException otherwise
   * @throws NullReferenceException, if the reference is invalid (i.e.) the underlying value is null
   */
  private void ensureValid() {
    if (!isValid(this)) {
      throw new NullReferenceException();
    }
  }

  /**
   * A test-only method to get the ref count
   * DO NOT USE in regular code
   */
  public synchronized int getRefCountTestOnly() {
    return mRefCount;
  }

  /**
   * The moral equivalent of NullPointerException for SharedReference. Indicates that the
   * referenced object is null
   */
  public static class NullReferenceException extends RuntimeException {
    public NullReferenceException() {
      super("Null shared reference");
    }
  }
}
