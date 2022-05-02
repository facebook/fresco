/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.samples.animation2.color;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import com.facebook.fresco.animation.backend.AnimationBackend;
import com.facebook.fresco.animation.drawable.AnimatedDrawable2;
import com.facebook.samples.animation2.R;
import com.facebook.samples.animation2.utils.AnimationBackendUtils;

/**
 * Simple standalone activity that creates a new {@link AnimatedDrawable2} and a custom backend that
 * cycles through colors. Tap the view to start / stop the animation.
 *
 * <p>When the animation is inactive for more than 2 seconds (no new frames drawn), a toast message
 * will be displayed.
 */
public class SimpleColorFragment extends Fragment {

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_simple_container, container, false);
  }

  @Override
  public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
    // Get the animation container
    ImageView animationContainer = (ImageView) view.findViewById(R.id.animation_container);

    // Create the animation backend
    // In addition, we wrap it with an activity check. Tap the view to stop the animation to see
    // an inactivity toast message after 2 seconds.
    // In order to remove the inactivity check, just remove the wrapper method and set it to
    // the backend directly.
    AnimationBackend animationBackend =
        AnimationBackendUtils.wrapAnimationBackendWithInactivityCheck(
            getContext(), ExampleColorBackend.createSampleColorAnimationBackend(getResources()));

    // Create a new animated drawable with the example backend
    final AnimatedDrawable2 animatedDrawable = new AnimatedDrawable2(animationBackend);

    // Set the animation as a background
    animationContainer.setImageDrawable(animatedDrawable);

    // Add a click listener to start / stop the animation
    animationContainer.setOnClickListener(
        new View.OnClickListener() {
          @Override
          public void onClick(View v) {
            if (animatedDrawable.isRunning()) {
              animatedDrawable.stop();
            } else {
              animatedDrawable.start();
            }
          }
        });

    // Start the animation
    animatedDrawable.start();
  }
}
