/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.references;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;


import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Closeables;
import com.facebook.common.logging.FLog;

/**
 * A smart pointer-like class for Java.
 *
 * <p>This class allows reference-counting semantics in a Java-friendlier way. A single object
 * can have any number of CloseableReferences pointing to it. When all of these have been closed,
 * the object either has its {@link Closeable#close} method called, if it implements
 * {@link Closeable}, or its designated {@link ResourceReleaser#release},
 * if it does not.
 *
 * <p>Callers can construct a CloseableReference wrapping a {@link Closeable} with:
 * <pre>
 * Closeable foo;
 * CloseableReference c = CloseableReference.of(foo);
 * </pre>
 * <p>Objects that do not implement Closeable can still use this class, but must supply a
 * {@link ResourceReleaser}:
 * <pre>
 * {@code
 * Object foo;
 * ResourceReleaser<Object> fooReleaser;
 * CloseableReference c = CloseableReference.of(foo, fooReleaser);
 * }
 * </pre>
 * <p>When making a logical copy, callers should call {@link #clone}:
 * <pre>
 * CloseableReference copy = c.clone();
 * </pre>
 * <p>
 * When each copy of CloseableReference is no longer needed, close should be called:
 * <pre>
 * copy.close();
 * c.close();
 * </pre>
 *
 * <p>As with any Closeable, try-finally semantics may be needed to ensure that close is called.
 * <p>Do not rely upon the finalizer; the purpose of this class is for expensive resources to
 * be released without waiting for the garbage collector. The finalizer will log an error if
 * the close method has not bee called.
 */
public final class CloseableReference<T> implements Cloneable, Closeable {

  private static Class<CloseableReference> TAG = CloseableReference.class;

  private static final ResourceReleaser<Closeable> DEFAULT_CLOSEABLE_RELEASER =
      new ResourceReleaser<Closeable>() {
        @Override
        public void release(Closeable value) {
          try {
            Closeables.close(value, true);
          } catch (IOException ioe) {
            // This will not happen, Closeable.close swallows and logs IOExceptions
          }
        }
      };


  @GuardedBy("this")
  private boolean mIsClosed = false;

  private final SharedReference<T> mSharedReference;

  /**
   * The caller should guarantee that reference count of sharedReference is not decreased to zero,
   * so that the reference is valid during execution of this method.
   */
  private CloseableReference(SharedReference<T> sharedReference) {
    mSharedReference = Preconditions.checkNotNull(sharedReference);
    sharedReference.addReference();
  }

  private CloseableReference(T t, ResourceReleaser<T> resourceReleaser) {
    // Ref-count pre-set to 1
    mSharedReference = new SharedReference<T>(t, resourceReleaser);
  }

  /**
   * Constructs a CloseableReference.
   *
   * <p>Returns null if the parameter is null.
   */
  public static @Nullable <T extends Closeable> CloseableReference<T> of(@Nullable T t) {
    if (t == null) {
      return null;
    } else {
      return new CloseableReference<T>(t, (ResourceReleaser<T>) DEFAULT_CLOSEABLE_RELEASER);
    }
  }

  /**
   * Constructs a CloseableReference (wrapping a SharedReference) of T with provided
   * ResourceReleaser<T>. If t is null, this will just return null.
   */
  public static @Nullable <T> CloseableReference<T> of(
      @Nullable T t,
      ResourceReleaser<T> resourceReleaser) {
    if (t == null) {
      return null;
    } else {
      return new CloseableReference<T>(t, resourceReleaser);
    }
  }

  /**
   * Closes this CloseableReference.
   *
   * <p>Decrements the reference count of the underlying object. If it is zero, the object
   * will be released.
   *
   * <p>This method is idempotent. Calling it multiple times on the same instance has no effect.
   */
  @Override
  public void close() {
    synchronized (this) {
      if (mIsClosed) {
        return;
      }
      mIsClosed = true;
    }

    mSharedReference.deleteReference();
  }

  /**
   * Returns the underlying Closeable if this reference is not closed yet.
   * Otherwise IllegalStateException is thrown.
   */
  public synchronized T get() {
    Preconditions.checkState(!mIsClosed);
    return mSharedReference.get();
  }

  /**
   * Returns a new CloseableReference to the same underlying SharedReference. The SharedReference
   * ref-count is incremented.
   */
  @Override
  public synchronized CloseableReference<T> clone() {
    Preconditions.checkState(isValid());
    return new CloseableReference<T>(mSharedReference);
  }

  public synchronized CloseableReference<T> cloneOrNull() {
    return isValid() ? new CloseableReference<T>(mSharedReference) : null;
  }

  /**
   * Checks if this closable-reference is valid i.e. is not closed.
   * @return true if the closeable reference is valid
   */
  public synchronized boolean isValid() {
    return !mIsClosed;
  }

  @Override
  protected void finalize() throws Throwable {
    try {
      // We put synchronized here so that lint doesn't warn about accessing mIsClosed, which is
      // guarded by this. Lint isn't aware of finalize semantics.
      synchronized (this) {
        if (mIsClosed) {
          return;
        }
      }

      FLog.w(TAG, "Finalized without closing: %x %x (type = %s)",
          System.identityHashCode(this),
          System.identityHashCode(mSharedReference),
          mSharedReference.get().getClass().getSimpleName());

      close();
    } finally {
      super.finalize();
    }
  }

  /**
   * A test-only method to get the underlying references.
   *
   * <p><b>DO NOT USE in application code.</b>
   */
  @VisibleForTesting
  public synchronized SharedReference<T> getUnderlyingReferenceTestOnly() {
    return mSharedReference;
  }

  /**
   * Method used for tracking Closeables pointed by CloseableReference.
   * Use only for debugging and logging.
   */
  public synchronized int getValueHash() {
    return isValid() ? System.identityHashCode(mSharedReference.get()) : 0;
  }

  /**
   * Checks if the closable-reference is valid i.e. is not null, and is not closed.
   * @return true if the closeable reference is valid
   */
  public static boolean isValid(@Nullable CloseableReference<?> ref) {
    return ref != null && ref.isValid();
  }

  /**
   * Returns the cloned reference if valid, null otherwise.
   *
   * @param ref the reference to clone
   */
  @Nullable
  public static <T> CloseableReference<T> cloneOrNull(@Nullable CloseableReference<T> ref) {
    return (ref != null) ? ref.cloneOrNull() : null;
  }

  /**
   * Clones a collection of references and returns a list. Returns null if the list is null. If
   * the list is non-null, clones each reference. If a reference cannot be cloned due to already
   * being closed, the list will contain a null value in its place.
   *
   * @param refs the references to clone
   * @return the list of cloned references or null
   */
  public static <T> List<CloseableReference<T>> cloneOrNull(
      Collection<CloseableReference<T>> refs) {
    if (refs == null) {
      return null;
    }
    List<CloseableReference<T>> ret = new ArrayList<>(refs.size());
    for (CloseableReference<T> ref : refs) {
      ret.add(CloseableReference.cloneOrNull(ref));
    }
    return ret;
  }

  /**
   * Closes the reference handling null.
   *
   * @param ref the reference to close
   */
  public static void closeSafely(@Nullable CloseableReference<?> ref) {
    if (ref != null) {
      ref.close();
    }
  }

  /**
   * Closes the references in the iterable handling null.
   *
   * @param references the reference to close
   */
  public static void closeSafely(@Nullable Iterable<? extends CloseableReference<?>> references) {
    if (references != null) {
      for (CloseableReference<?> ref : references) {
        closeSafely(ref);
      }
    }
  }
}
