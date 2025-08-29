/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.references;

import android.graphics.Bitmap;
import androidx.annotation.IntDef;
import androidx.annotation.VisibleForTesting;
import com.facebook.common.internal.Closeables;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;
import com.facebook.infer.annotation.FalseOnNull;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.infer.annotation.PropagatesNullable;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

/**
 * A smart pointer-like class for Java.
 *
 * <p>This class allows reference-counting semantics in a Java-friendlier way. A single object can
 * have any number of CloseableReferences pointing to it. When all of these have been closed, the
 * object either has its {@link Closeable#close} method called, if it implements {@link Closeable},
 * or its designated {@link ResourceReleaser#release}, if it does not.
 *
 * <p>Callers can construct a CloseableReference wrapping a {@link Closeable} with:
 *
 * <pre>
 * Closeable foo;
 * CloseableReference c = CloseableReference.of(foo);
 * </pre>
 *
 * <p>Objects that do not implement Closeable can still use this class, but must supply a {@link
 * ResourceReleaser}:
 *
 * <pre>{@code
 * Object foo;
 * ResourceReleaser<Object> fooReleaser;
 * CloseableReference c = CloseableReference.of(foo, fooReleaser);
 * }</pre>
 *
 * <p>When making a logical copy, callers should call {@link #clone}:
 *
 * <pre>
 * CloseableReference copy = c.clone();
 * </pre>
 *
 * <p>When each copy of CloseableReference is no longer needed, close should be called:
 *
 * <pre>
 * copy.close();
 * c.close();
 * </pre>
 *
 * <p>As with any Closeable, try-finally semantics may be needed to ensure that close is called.
 *
 * <p>Do not rely upon the finalizer; the purpose of this class is for expensive resources to be
 * released without waiting for the garbage collector. The finalizer will log an error if the close
 * method has not been called.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public abstract class CloseableReference<T> implements Cloneable, Closeable {

  @IntDef({REF_TYPE_DEFAULT, REF_TYPE_FINALIZER, REF_TYPE_REF_COUNT, REF_TYPE_NOOP})
  public @interface CloseableRefType {}

  public static final int REF_TYPE_DEFAULT = 0;
  public static final int REF_TYPE_FINALIZER = 1;
  public static final int REF_TYPE_REF_COUNT = 2;
  public static final int REF_TYPE_NOOP = 3;

  private static final Class<CloseableReference> TAG = CloseableReference.class;

  private static @CloseableRefType int sBitmapCloseableRefType = REF_TYPE_DEFAULT;

  public static void setDisableCloseableReferencesForBitmaps(
      @CloseableRefType int bitmapCloseableRefType) {
    sBitmapCloseableRefType = bitmapCloseableRefType;
  }

  @GuardedBy("this")
  protected boolean mIsClosed = false;

  protected final SharedReference<T> mSharedReference;
  protected final @Nullable LeakHandler mLeakHandler;
  protected final @Nullable Throwable mStacktrace;

  public interface LeakHandler {
    void reportLeak(SharedReference<Object> reference, @Nullable Throwable stacktrace);

    /**
     * Indicate whether the {@link #reportLeak(SharedReference, Throwable)} method expects a
     * stacktrace. This is expensive and should only be used sparingly.
     */
    boolean requiresStacktrace();
  }

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

  private static final LeakHandler DEFAULT_LEAK_HANDLER =
      new LeakHandler() {
        @Override
        public void reportLeak(SharedReference<Object> reference, @Nullable Throwable stacktrace) {
          final Object ref = reference.get();
          FLog.w(
              TAG,
              "Finalized without closing: %x %x (type = %s)",
              System.identityHashCode(this),
              System.identityHashCode(reference),
              ref == null ? null : ref.getClass().getName());
        }

        @Override
        public boolean requiresStacktrace() {
          return false;
        }
      };

  protected CloseableReference(
      SharedReference<T> sharedReference,
      @Nullable LeakHandler leakHandler,
      @Nullable Throwable stacktrace) {
    mSharedReference = Preconditions.checkNotNull(sharedReference);
    sharedReference.addReference();
    mLeakHandler = leakHandler;
    mStacktrace = stacktrace;
  }

  protected CloseableReference(
      T t,
      @Nullable ResourceReleaser<T> resourceReleaser,
      @Nullable LeakHandler leakHandler,
      @Nullable Throwable stacktrace,
      boolean keepAlive) {
    mSharedReference = new SharedReference<T>(t, resourceReleaser, keepAlive);
    mLeakHandler = leakHandler;
    mStacktrace = stacktrace;
  }

  /**
   * Constructs a CloseableReference.
   *
   * <p>Returns null if the parameter is null.
   */
  public static <T extends Closeable> CloseableReference<T> of(@PropagatesNullable T t) {
    return of(t, (ResourceReleaser<T>) DEFAULT_CLOSEABLE_RELEASER);
  }

  /**
   * Constructs a CloseableReference (wrapping a SharedReference) of T with provided
   * ResourceReleaser<T>. If t is null, this will just return null.
   */
  public static <T> CloseableReference<T> of(
      @PropagatesNullable T t, ResourceReleaser<T> resourceReleaser) {
    return of(t, resourceReleaser, DEFAULT_LEAK_HANDLER);
  }

  /**
   * Constructs a CloseableReference with a custom {@link LeakHandler} that's run if a reference is
   * not closed when the finalizer is called.
   *
   * <p>Returns null if the parameter is null.
   */
  public static <T extends Closeable> CloseableReference<T> of(
      @PropagatesNullable @Nullable T t, LeakHandler leakHandler) {
    if (t == null) {
      return null;
    } else {
      return of(
          t,
          (ResourceReleaser<T>) DEFAULT_CLOSEABLE_RELEASER,
          leakHandler,
          leakHandler.requiresStacktrace() ? new Throwable() : null);
    }
  }

  public static <T> CloseableReference<T> of(
      @PropagatesNullable T t, ResourceReleaser<T> resourceReleaser, LeakHandler leakHandler) {
    if (t == null) {
      return null;
    } else {
      return of(
          t,
          resourceReleaser,
          leakHandler,
          leakHandler.requiresStacktrace() ? new Throwable() : null);
    }
  }

  /**
   * Constructs a CloseableReference (wrapping a SharedReference) of T with provided
   * ResourceReleaser<T> and a custom handler that's run if a leak is detected in the finalizer. If
   * t is null, this will just return null.
   */
  public static <T> CloseableReference<T> of(
      @PropagatesNullable T t,
      ResourceReleaser<T> resourceReleaser,
      LeakHandler leakHandler,
      @Nullable Throwable stacktrace) {
    if (t == null) {
      return null;
    } else {
      if (t instanceof Bitmap || t instanceof HasBitmap) {
        switch (sBitmapCloseableRefType) {
          case REF_TYPE_FINALIZER:
            return new FinalizerCloseableReference<>(t, resourceReleaser, leakHandler, stacktrace);
          case REF_TYPE_REF_COUNT:
            return new RefCountCloseableReference<>(t, resourceReleaser, leakHandler, stacktrace);
          case REF_TYPE_NOOP:
            return new NoOpCloseableReference<>(t);
          case REF_TYPE_DEFAULT:
            // return default
        }
      }

      return new DefaultCloseableReference<>(t, resourceReleaser, leakHandler, stacktrace);
    }
  }

  /**
   * Returns the underlying Closeable if this reference is not closed yet. Otherwise
   * IllegalStateException is thrown.
   */
  public synchronized T get() {
    Preconditions.checkState(!mIsClosed);
    return Preconditions.checkNotNull(mSharedReference.get());
  }

  /**
   * Returns a new CloseableReference to the same underlying SharedReference. The SharedReference
   * ref-count is incremented.
   */
  public abstract CloseableReference<T> clone();

  public synchronized @Nullable CloseableReference<T> cloneOrNull() {
    if (isValid()) {
      return clone();
    }
    return null;
  }

  /**
   * Checks if this closable-reference is valid i.e. is not closed.
   *
   * @return true if the closeable reference is valid
   */
  public synchronized boolean isValid() {
    return !mIsClosed;
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
   * Method used for tracking Closeables pointed by CloseableReference. Use only for debugging and
   * logging.
   */
  public int getValueHash() {
    return isValid() ? System.identityHashCode(mSharedReference.get()) : 0;
  }

  /**
   * Closes this CloseableReference.
   *
   * <p>Decrements the reference count of the underlying object. If it is zero, the object will be
   * released.
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
   * Checks if the closable-reference is valid i.e. is not null, and is not closed.
   *
   * @return true if the closeable reference is valid
   */
  @FalseOnNull
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
   * Clones a collection of references and returns a list. Returns null if the list is null. If the
   * list is non-null, clones each reference. If a reference cannot be cloned due to already being
   * closed, the list will contain a null value in its place.
   *
   * @param refs the references to clone
   * @return the list of cloned references or null
   */
  public static <T> List<CloseableReference<T>> cloneOrNull(
      @PropagatesNullable Collection<CloseableReference<T>> refs) {
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
