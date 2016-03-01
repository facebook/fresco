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

import java.util.IdentityHashMap;
import java.util.Map;

import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;

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

  // Keeps references to all live objects so finalization of those Objects always happens after
  // SharedReference first disposes of it. Note, this does not prevent CloseableReference's from
  // being finalized when the reference is no longer reachable.
  @GuardedBy("itself")
  private static final Map<Object, Integer> sLiveObjects = new IdentityHashMap<>();

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
    addLiveReference(value);
  }

  /**
   * Increases the reference count of a live object in the static map. Adds it if it's not
   * being held.
   *
   * @param value the value to add.
   */
  private static void addLiveReference(Object value) {
    synchronized (sLiveObjects) {
      Integer count = sLiveObjects.get(value);
      if (count == null) {
        sLiveObjects.put(value, 1);
      } else {
        sLiveObjects.put(value, count + 1);
      }
    }
  }

  /**
   * Decreases the reference count of live object from the static map. Removes it if it's reference
   * count has become 0.
   *
   * @param value the value to remove.
   */
  private static void removeLiveReference(Object value) {
    synchronized (sLiveObjects) {
      Integer count = sLiveObjects.get(value);
      if (count == null) {
        // Uh oh.
        FLog.wtf(
            "SharedReference",
            "No entry in sLiveObjects for value of type %s",
            value.getClass());
      } else if (count == 1) {
        sLiveObjects.remove(value);
      } else {
        sLiveObjects.put(value, count - 1);
      }
    }
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
      T deleted;
      synchronized (this) {
        deleted = mValue;
        mValue = null;
      }
      mResourceReleaser.release(deleted);
      removeLiveReference(deleted);
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
