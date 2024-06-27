/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.litho.slideshow

import android.content.Context
import android.content.res.Resources
import android.net.Uri
import android.os.Handler
import android.os.Looper
import com.facebook.common.callercontext.ContextChain
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.provider.FrescoVitoProvider
import com.facebook.fresco.vito.source.ImageSourceProvider
import com.facebook.litho.ComponentContext
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.OnUnmount
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.State
import com.facebook.litho.annotations.TreeProp
import java.util.Timer
import java.util.TimerTask

@MountSpec(isPureRender = true)
object FrescoVitoSlideshowComponentSpec {

  @PropDefault const val isPlaying = true

  @JvmStatic
  @OnCreateInitialState
  internal fun createInitialState(
      c: ComponentContext,
      slideshowIndex: StateValue<Int?>,
      timer: StateValue<Timer?>,
      currentlyPlaying: StateValue<Boolean?>
  ) {
    slideshowIndex.set(0)
    timer.set(Timer("Fresco Vito slideshow timer"))
    currentlyPlaying.set(false)
  }

  @JvmStatic
  @OnCreateMountContent
  fun onCreateMountContent(c: Context?): FrescoVitoSlideshowDrawable<*> =
      FrescoVitoSlideshowDrawable(
          FrescoVitoProvider.getController().createDrawable(),
          FrescoVitoProvider.getController().createDrawable(),
          FrescoVitoProvider.getController().createDrawable())

  @JvmStatic
  @OnMount
  fun onMount(
      c: ComponentContext,
      slideshowDrawable: FrescoVitoSlideshowDrawable<*>,
      @Prop(varArg = "uri") uris: List<Uri?>,
      @Prop photoTransitionMs: Int,
      @Prop fadeTransitionMs: Int,
      @Prop(optional = true) isPlaying: Boolean,
      @Prop(optional = true) imageOptions: ImageOptions?,
      @Prop(optional = true) callerContext: Any?,
      @Prop(optional = true) imageListener: ImageListener?,
      @TreeProp contextChain: ContextChain?,
      @State(canUpdateLazily = true) slideshowIndex: Int,
      @State(canUpdateLazily = true) timer: Timer,
      @State(canUpdateLazily = true) currentlyPlaying: Boolean
  ) {
    // Reset mount content
    val controller = FrescoVitoProvider.getController()
    controller.releaseImmediately(slideshowDrawable.previousImage)
    controller.releaseImmediately(slideshowDrawable.currentImage)
    controller.releaseImmediately(slideshowDrawable.nextImage)
    slideshowDrawable.reset()

    // Configure mount content
    slideshowDrawable.transitionDuration = fadeTransitionMs

    // Load current image
    fetchNextImage(
        c.resources,
        slideshowDrawable,
        uris[slideshowIndex % uris.size],
        imageOptions,
        callerContext,
        contextChain,
        imageListener)
    // Immediately show current image
    slideshowDrawable.fadeToNext()
    slideshowDrawable.finishTransitionImmediately()
    val listSize = uris.size
    if (isPlaying && !currentlyPlaying) {
      // Load next image immediately
      val nextImageIndex = (slideshowIndex + 1) % listSize
      fetchNextImage(
          c.resources,
          slideshowDrawable,
          uris[nextImageIndex],
          imageOptions,
          callerContext,
          contextChain,
          imageListener)

      // Set up task for animating to next image
      val animation: Runnable =
          object : Runnable {
            var currentIndex = nextImageIndex

            override fun run() {
              val nextIndex = (currentIndex + 1) % listSize
              animateToNextImage(
                  c.resources,
                  slideshowDrawable,
                  uris,
                  imageOptions,
                  callerContext,
                  contextChain,
                  nextIndex,
                  imageListener)
              currentIndex = nextIndex
              FrescoVitoSlideshowComponent.lazyUpdateSlideshowIndex(c, currentIndex)
            }
          }
      val handler = Handler(Looper.getMainLooper())
      val timerTask: TimerTask =
          object : TimerTask() {
            override fun run() {
              handler.post(animation)
            }
          }
      slideshowDrawable.timerTask = timerTask
      timer.scheduleAtFixedRate(
          timerTask, photoTransitionMs.toLong(), (photoTransitionMs + fadeTransitionMs).toLong())
    } else if (!isPlaying && currentlyPlaying) {
      val animateTask = slideshowDrawable.timerTask
      animateTask?.cancel()
      FrescoVitoSlideshowComponent.lazyUpdateCurrentlyPlaying(c, false)
    }
  }

  @JvmStatic
  @OnUnmount
  internal fun onUnmount(c: ComponentContext, slideshowDrawable: FrescoVitoSlideshowDrawable<*>) {
    val controller = FrescoVitoProvider.getController()
    controller.releaseImmediately(slideshowDrawable.previousImage)
    controller.releaseImmediately(slideshowDrawable.currentImage)
    controller.releaseImmediately(slideshowDrawable.nextImage)
    slideshowDrawable.reset()
    FrescoVitoSlideshowComponent.lazyUpdateCurrentlyPlaying(c, false)
  }

  private fun animateToNextImage(
      resources: Resources,
      slideshowDrawable: FrescoVitoSlideshowDrawable<*>,
      uris: List<Uri?>,
      options: ImageOptions?,
      callerContext: Any?,
      contextChain: ContextChain?,
      nextIndex: Int,
      listener: ImageListener?
  ) {
    // Do not transition until both current and next images are available
    if (isStillLoading(slideshowDrawable.currentImage) ||
        isStillLoading(slideshowDrawable.nextImage)) {
      return
    }
    // Both images are available -> we can fade
    slideshowDrawable.fadeToNext()
    // Fetch the next image ahead of time
    fetchNextImage(
        resources,
        slideshowDrawable,
        uris[nextIndex % uris.size],
        options,
        callerContext,
        contextChain,
        listener)
  }

  private fun isStillLoading(frescoDrawable: FrescoDrawableInterface): Boolean =
      frescoDrawable.isFetchSubmitted && !frescoDrawable.hasImage()

  private fun fetchNextImage(
      resources: Resources,
      slideshowDrawable: FrescoVitoSlideshowDrawable<*>,
      uri: Uri?,
      options: ImageOptions?,
      callerContext: Any?,
      contextChain: ContextChain?,
      listener: ImageListener?
  ) {
    FrescoVitoProvider.getController()
        .fetch(
            frescoDrawable = slideshowDrawable.nextImage,
            imageRequest =
                FrescoVitoProvider.getImagePipeline()
                    .createImageRequest(resources, ImageSourceProvider.forUri(uri), options),
            callerContext = callerContext,
            contextChain = contextChain,
            listener = listener,
            onFadeListener = null,
            viewportDimensions = null)
  }
}
