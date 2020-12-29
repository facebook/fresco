/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.drawee.components;

import android.os.Looper;
import com.facebook.infer.annotation.Nullsafe;
import javax.annotation.Nullable;

/**
 * Component that defers {@code release} until after the main Looper has completed its current
 * message. Although we would like for defer {@code release} to happen immediately after the current
 * message is done, this is not guaranteed as there might be other messages after the current one,
 * but before the deferred one, pending in the Looper's queue.
 *
 * <p>onDetach / onAttach events are used for releasing / acquiring resources. However, sometimes we
 * get an onDetach event followed by an onAttach event within the same loop. In order to avoid
 * overaggressive resource releasing / acquiring, we defer releasing. If onAttach happens within the
 * same loop, we will simply cancel corresponding deferred release, avoiding an unnecessary resource
 * release / acquire cycle. If onAttach doesn't happen before the deferred message gets executed,
 * the resources will be released.
 */
@Nullsafe(Nullsafe.Mode.STRICT)
public abstract class DeferredReleaser {
  public interface Releasable {
    void release();
  }

  private static @Nullable DeferredReleaser sInstance = null;

  public static synchronized DeferredReleaser getInstance() {
    if (sInstance == null) {
      sInstance = new DeferredReleaserConcurrentImpl();
    }
    return sInstance;
  }

  /**
   * Schedules deferred release.
   *
   * <p>The object will be released after the current Looper's loop, unless {@code
   * cancelDeferredRelease} is called before then.
   *
   * @param releasable Object to release.
   */
  public abstract void scheduleDeferredRelease(Releasable releasable);

  /**
   * Cancels a pending release for this object.
   *
   * @param releasable Object to cancel release of.
   */
  public abstract void cancelDeferredRelease(Releasable releasable);

  static boolean isOnUiThread() {
    return Looper.getMainLooper().getThread() == Thread.currentThread();
  }
}
