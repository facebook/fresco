/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.animation2.utils;

import static com.facebook.fresco.animation.backend.AnimationBackendDelegateWithInactivityCheck.createForBackend;

import android.content.Context;
import android.widget.Toast;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.time.RealtimeSinceBootClock;
import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.backend.AnimationBackendDelegateWithInactivityCheck;

/** Animation backend utilities shared between multiple examples. */
public final class AnimationBackendUtils {

  /**
   * Wraps the given animation backend with an activity check. When no frame has been drawn for more
   * than 2 seconds, an inactivity toast message will be displayed.
   *
   * @param context the context to be used for displaying the toast message
   * @param animationBackend the backend to wrap with the inactivity check
   * @return the wrapped backend to use
   */
  public static AnimationBackend wrapAnimationBackendWithInactivityCheck(
      final Context context, final AnimationBackend animationBackend) {
    AnimationBackendDelegateWithInactivityCheck.InactivityListener inactivityListener =
        new AnimationBackendDelegateWithInactivityCheck.InactivityListener() {
          @Override
          public void onInactive() {
            // Forward the inactive callback to the backend if needed
            if (animationBackend
                instanceof AnimationBackendDelegateWithInactivityCheck.InactivityListener) {
              ((AnimationBackendDelegateWithInactivityCheck.InactivityListener) animationBackend)
                  .onInactive();
            }
            Toast.makeText(context, "Animation backend inactive.", Toast.LENGTH_SHORT).show();
          }
        };
    return createForBackend(
        animationBackend,
        inactivityListener,
        RealtimeSinceBootClock.get(),
        UiThreadImmediateExecutorService.getInstance());
  }

  private AnimationBackendUtils() {}
}
