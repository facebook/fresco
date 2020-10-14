/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.instrumentation;

import androidx.annotation.Nullable;
import com.facebook.infer.annotation.Nullsafe;

/**
 * Utility class that provides hooks to capture execution of different units of work. Client code
 * can specify a custom {@link Instrumenter} that will receive ad-hoc updates when work that has to
 * be executed across threads gets moved around.
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public final class FrescoInstrumenter {

  /** Allows to capture unit of works across different threads. */
  public interface Instrumenter {

    /**
     * Allows to know in advance if the custom instrumenter desires to receive continuation updates.
     * This can be used to avoid un-necessary work if the subscriber will not use the information
     * provided.
     *
     * @return true to specify interest in handling the updates, false otherwise.
     */
    boolean isTracing();

    /**
     * Called before scheduling a new unit work.
     *
     * @param tag name.
     * @return a token object that allows to track work units.
     */
    @Nullable
    Object onBeforeSubmitWork(String tag);

    /**
     * Captures the beginning of the continuation for stolen work.
     *
     * @param token returned by {@link Instrumenter#onBeforeSubmitWork}.
     * @param tag optional name.
     */
    @Nullable
    Object onBeginWork(Object token, @Nullable String tag);

    /**
     * Captures the end of the continuation for stolen work.
     *
     * @param token returned by {@link Instrumenter#onBeginWork}.
     */
    @Nullable
    void onEndWork(Object token);

    /**
     * Reports a failure while executing work.
     *
     * <p><note>{@link Instrumenter#onEndWork(Object)} still needs to be invoked.
     *
     * @param token returned by {@link Instrumenter#onBeginWork(Object, String)}.
     * @param th containing the failure.
     */
    void markFailure(Object token, Throwable th);

    /**
     * Called when a unit of work is about to be scheduled.
     *
     * @param runnable that will be executed.
     * @param tag name.
     * @return the wrapped input runnable or just the input one.
     */
    @Nullable
    Runnable decorateRunnable(Runnable runnable, String tag);
  }

  @Nullable private static volatile Instrumenter sInstance;

  /**
   * Allows to provide a {@link Instrumenter} that will receive units of work updates.
   *
   * @param instrumenter to be notified or null to reset.
   */
  public static void provide(@Nullable Instrumenter instrumenter) {
    sInstance = instrumenter;
  }

  public static boolean isTracing() {
    final Instrumenter instrumenter = sInstance;
    if (instrumenter == null) {
      return false;
    }
    return instrumenter.isTracing();
  }

  @Nullable
  public static Object onBeforeSubmitWork(@Nullable String tag) {
    final Instrumenter instrumenter = sInstance;
    if (instrumenter == null || tag == null) {
      return null;
    }
    return instrumenter.onBeforeSubmitWork(tag);
  }

  @Nullable
  public static Object onBeginWork(@Nullable Object token, @Nullable String tag) {
    final Instrumenter instrumenter = sInstance;
    if (instrumenter == null || token == null) {
      return null;
    }
    return instrumenter.onBeginWork(token, tag);
  }

  public static void onEndWork(@Nullable Object token) {
    final Instrumenter instrumenter = sInstance;
    if (instrumenter == null || token == null) {
      return;
    }
    instrumenter.onEndWork(token);
  }

  public static void markFailure(@Nullable Object token, Throwable th) {
    final Instrumenter instrumenter = sInstance;
    if (instrumenter == null || token == null) {
      return;
    }
    instrumenter.markFailure(token, th);
  }

  @Nullable
  public static Runnable decorateRunnable(@Nullable Runnable runnable, @Nullable String tag) {
    final Instrumenter instrumenter = sInstance;
    if (instrumenter == null || runnable == null || tag == null) {
      return runnable;
    }
    return instrumenter.decorateRunnable(runnable, tag);
  }
}
