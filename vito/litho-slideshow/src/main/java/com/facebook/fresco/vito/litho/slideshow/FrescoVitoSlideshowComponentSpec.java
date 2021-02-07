/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.litho.slideshow;

import android.content.Context;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import com.facebook.fresco.vito.core.FrescoController2;
import com.facebook.fresco.vito.core.FrescoDrawable2;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.fresco.vito.provider.FrescoVitoProvider;
import com.facebook.fresco.vito.source.ImageSourceProvider;
import com.facebook.infer.annotation.Nullsafe;
import com.facebook.litho.ComponentContext;
import com.facebook.litho.StateValue;
import com.facebook.litho.annotations.MountSpec;
import com.facebook.litho.annotations.OnCreateInitialState;
import com.facebook.litho.annotations.OnCreateMountContent;
import com.facebook.litho.annotations.OnMount;
import com.facebook.litho.annotations.OnUnmount;
import com.facebook.litho.annotations.Prop;
import com.facebook.litho.annotations.PropDefault;
import com.facebook.litho.annotations.State;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import javax.annotation.Nullable;

@Nullsafe(Nullsafe.Mode.LOCAL)
@MountSpec(isPureRender = true)
public class FrescoVitoSlideshowComponentSpec {

  @PropDefault static final boolean isPlaying = true;

  @OnCreateInitialState
  protected static void createInitialState(
      ComponentContext c,
      StateValue<Integer> slideshowIndex,
      StateValue<Timer> timer,
      StateValue<Boolean> currentlyPlaying) {
    slideshowIndex.set(0);
    timer.set(new Timer("Fresco Vito slideshow timer"));
    currentlyPlaying.set(false);
  }

  @OnCreateMountContent
  static FrescoVitoSlideshowDrawable onCreateMountContent(Context c) {
    return new FrescoVitoSlideshowDrawable(
        FrescoVitoProvider.getController().createDrawable(),
        FrescoVitoProvider.getController().createDrawable(),
        FrescoVitoProvider.getController().createDrawable());
  }

  @OnMount
  static void onMount(
      final ComponentContext c,
      final FrescoVitoSlideshowDrawable slideshowDrawable,
      final @Prop(varArg = "uri") List<Uri> uris,
      final @Prop int photoTransitionMs,
      final @Prop int fadeTransitionMs,
      final @Prop(optional = true) boolean isPlaying,
      final @Prop(optional = true) @Nullable ImageOptions imageOptions,
      final @Prop(optional = true) @Nullable Object callerContext,
      final @State(canUpdateLazily = true) Integer slideshowIndex,
      final @State(canUpdateLazily = true) Timer timer,
      final @State(canUpdateLazily = true) Boolean currentlyPlaying) {
    // Reset mount content
    FrescoController2 controller = FrescoVitoProvider.getController();
    controller.releaseImmediately(slideshowDrawable.getPrevious());
    controller.releaseImmediately(slideshowDrawable.getCurrent());
    controller.releaseImmediately(slideshowDrawable.getNext());
    slideshowDrawable.reset();

    // Configure mount content
    slideshowDrawable.setTransitionDuration(fadeTransitionMs);

    // Load current image
    fetchNextImage(
        c.getResources(), slideshowDrawable, uris.get(slideshowIndex), imageOptions, callerContext);
    // Immediately show current image
    slideshowDrawable.fadeToNext();
    slideshowDrawable.finishTransitionImmediately();

    final int listSize = uris.size();

    if (isPlaying && !currentlyPlaying) {
      // Load next image immediately
      final int nextImageIndex = (slideshowIndex + 1) % listSize;
      fetchNextImage(
          c.getResources(),
          slideshowDrawable,
          uris.get(nextImageIndex),
          imageOptions,
          callerContext);

      // Set up task for animating to next image
      final Runnable animation =
          new Runnable() {
            int currentIndex = nextImageIndex;

            @Override
            public void run() {
              int nextIndex = (currentIndex + 1) % listSize;
              animateToNextImage(
                  c.getResources(),
                  slideshowDrawable,
                  uris,
                  imageOptions,
                  callerContext,
                  nextIndex);
              currentIndex = nextIndex;
              FrescoVitoSlideshowComponent.lazyUpdateSlideshowIndex(c, currentIndex);
            }
          };

      final Handler handler = new Handler(Looper.getMainLooper());
      TimerTask timerTask =
          new TimerTask() {
            @Override
            public void run() {
              handler.post(animation);
            }
          };
      slideshowDrawable.setTimerTask(timerTask);
      timer.scheduleAtFixedRate(timerTask, photoTransitionMs, photoTransitionMs + fadeTransitionMs);
    } else if (!isPlaying && currentlyPlaying) {
      TimerTask animateTask = slideshowDrawable.getTimerTask();
      if (animateTask != null) {
        animateTask.cancel();
      }
      FrescoVitoSlideshowComponent.lazyUpdateCurrentlyPlaying(c, false);
    }
  }

  @OnUnmount
  protected static void onUnmount(
      ComponentContext c, final FrescoVitoSlideshowDrawable slideshowDrawable) {
    FrescoController2 controller = FrescoVitoProvider.getController();

    controller.releaseImmediately(slideshowDrawable.getPrevious());
    controller.releaseImmediately(slideshowDrawable.getCurrent());
    controller.releaseImmediately(slideshowDrawable.getNext());
    slideshowDrawable.reset();
    FrescoVitoSlideshowComponent.lazyUpdateCurrentlyPlaying(c, false);
  }

  private static void animateToNextImage(
      final Resources resources,
      final FrescoVitoSlideshowDrawable slideshowDrawable,
      List<Uri> uris,
      @Nullable ImageOptions options,
      @Nullable Object callerContext,
      int nextIndex) {
    // Do not transition until both current and next images are available
    if (isStillLoading(slideshowDrawable.getCurrent())
        || isStillLoading(slideshowDrawable.getNext())) {
      return;
    }
    // Both images are available -> we can fade
    slideshowDrawable.fadeToNext();
    // Fetch the next image ahead of time
    fetchNextImage(resources, slideshowDrawable, uris.get(nextIndex), options, callerContext);
  }

  private static boolean isStillLoading(FrescoDrawable2 frescoDrawable) {
    return frescoDrawable.isFetchSubmitted() && !frescoDrawable.hasImage();
  }

  private static void fetchNextImage(
      Resources resources,
      final FrescoVitoSlideshowDrawable slideshowDrawable,
      Uri uri,
      @Nullable ImageOptions options,
      @Nullable Object callerContext) {
    FrescoVitoProvider.getController()
        .fetch(
            slideshowDrawable.getNext(),
            FrescoVitoProvider.getImagePipeline()
                .createImageRequest(resources, ImageSourceProvider.forUri(uri), options),
            callerContext,
            null,
            null,
            null);
  }
}
