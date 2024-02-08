/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.litho

import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.graphics.Rect
import android.net.Uri
import android.os.Build
import android.view.View
import androidx.core.util.ObjectsCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.facebook.common.callercontext.ContextChain
import com.facebook.datasource.DataSource
import com.facebook.fresco.ui.common.OnFadeListener
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.provider.FrescoVitoProvider
import com.facebook.fresco.vito.source.ImageSource
import com.facebook.fresco.vito.source.ImageSourceProvider
import com.facebook.imagepipeline.listener.RequestListener
import com.facebook.litho.AccessibilityRole
import com.facebook.litho.BoundaryWorkingRange
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLayout
import com.facebook.litho.ContextUtils
import com.facebook.litho.Diff
import com.facebook.litho.Output
import com.facebook.litho.Size
import com.facebook.litho.StateValue
import com.facebook.litho.annotations.CachedValue
import com.facebook.litho.annotations.FromBoundsDefined
import com.facebook.litho.annotations.FromPrepare
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.MountingType
import com.facebook.litho.annotations.OnBind
import com.facebook.litho.annotations.OnBoundsDefined
import com.facebook.litho.annotations.OnCalculateCachedValue
import com.facebook.litho.annotations.OnCreateInitialState
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnEnteredRange
import com.facebook.litho.annotations.OnExitedRange
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.OnPopulateAccessibilityNode
import com.facebook.litho.annotations.OnPrepare
import com.facebook.litho.annotations.OnRegisterRanges
import com.facebook.litho.annotations.OnUnbind
import com.facebook.litho.annotations.OnUnmount
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.ResType
import com.facebook.litho.annotations.ShouldUpdate
import com.facebook.litho.annotations.State
import com.facebook.litho.annotations.TreeProp
import com.facebook.litho.utils.MeasureUtils
import java.util.concurrent.atomic.AtomicReference

/** Fresco Vito component for Litho */
@MountSpec(isPureRender = true, canPreallocate = true, poolSize = 15)
object FrescoVitoImage2Spec {

  @PropDefault const val imageAspectRatio: Float = 1f

  @PropDefault val prefetch: Prefetch = Prefetch.AUTO

  @PropDefault const val mutateDrawables: Boolean = true

  @JvmStatic
  @OnCreateMountContent(mountingType = MountingType.DRAWABLE)
  fun onCreateMountContent(c: Context?): FrescoDrawableInterface =
      FrescoVitoProvider.getController().createDrawable()

  @JvmStatic
  @OnCreateInitialState
  fun onCreateInitialState(
      context: ComponentContext,
      workingRangePrefetchData: StateValue<AtomicReference<DataSource<Void>>>,
  ) {
    if (FrescoVitoProvider.hasBeenInitialized() &&
        FrescoVitoProvider.getConfig().prefetchConfig.prefetchWithWorkingRange()) {
      workingRangePrefetchData.set(AtomicReference())
    }
  }

  @JvmStatic
  @OnMeasure
  fun onMeasure(
      c: ComponentContext,
      layout: ComponentLayout,
      widthSpec: Int,
      heightSpec: Int,
      size: Size,
      @Prop(optional = true, resType = ResType.FLOAT) imageAspectRatio: Float,
  ) {
    MeasureUtils.measureWithAspectRatio(widthSpec, heightSpec, imageAspectRatio, size)
  }

  @JvmStatic
  @OnCalculateCachedValue(name = "requestCachedValue")
  fun onCalculateImageRequest(
      c: ComponentContext,
      @Prop(optional = true) uriString: String?,
      @Prop(optional = true) uri: Uri?,
      @Prop(optional = true) imageSource: ImageSource?,
      @Prop(optional = true) imageOptions: ImageOptions?,
      @Prop(optional = true) logWithHighSamplingRate: Boolean?,
  ): VitoImageRequest? =
      if (imageOptions?.experimentalDynamicSize == true) {
        null
      } else {
        createVitoImageRequest(
            c, imageSource, uri, uriString, imageOptions, logWithHighSamplingRate, null)
      }

  private fun createVitoImageRequest(
      c: ComponentContext,
      imageSource: ImageSource?,
      uri: Uri?,
      uriString: String?,
      imageOptions: ImageOptions?,
      logWithHighSamplingRate: Boolean?,
      viewportRect: Rect?,
  ): VitoImageRequest =
      FrescoVitoProvider.getImagePipeline()
          .createImageRequest(
              c.resources,
              determineImageSource(imageSource, uri, uriString),
              imageOptions,
              logWithHighSamplingRate ?: false,
              viewportRect)

  @JvmStatic
  @OnPrepare
  fun onPrepare(
      c: ComponentContext,
      @Prop(optional = true) callerContext: Any?,
      @Prop(optional = true) prefetch: Prefetch?,
      @Prop(optional = true) prefetchRequestListener: RequestListener?,
      @CachedValue requestCachedValue: VitoImageRequest?,
      prefetchDataSource: Output<DataSource<Void>>,
  ) {
    if (requestCachedValue == null) {
      return
    }
    val config = FrescoVitoProvider.getConfig().prefetchConfig
    if (shouldPrefetchInOnPrepare(prefetch)) {
      prefetchDataSource.set(
          FrescoVitoProvider.getPrefetcher()
              .prefetch(
                  config.prefetchTargetOnPrepare(),
                  requestCachedValue,
                  callerContext,
                  prefetchRequestListener,
                  "OnPrepare"))
    }
  }

  @JvmStatic
  @OnMount
  fun onMount(
      c: ComponentContext,
      frescoDrawable: FrescoDrawableInterface,
      @Prop(optional = true) imageListener: ImageListener?,
      @Prop(optional = true) callerContext: Any?,
      @Prop(optional = true) onFadeListener: OnFadeListener?,
      @Prop(optional = true) mutateDrawables: Boolean,
      @CachedValue requestCachedValue: VitoImageRequest?,
      @FromBoundsDefined requestFromBoundsDefined: VitoImageRequest?,
      @FromPrepare prefetchDataSource: DataSource<Void?>?,
      @FromBoundsDefined viewportDimensions: Rect,
      @State workingRangePrefetchData: AtomicReference<DataSource<Void>>?,
      @TreeProp contextChain: ContextChain?,
  ) {
    val request = requestCachedValue ?: requestFromBoundsDefined
    frescoDrawable.setMutateDrawables(mutateDrawables)
    if (FrescoVitoProvider.getConfig().useBindOnly()) {
      return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
        FrescoVitoProvider.getConfig().enableWindowWideColorGamut()) {
      val activity: Activity? = ContextUtils.findActivityInContext(c.androidContext)
      val window = activity?.window
      if (window != null && window.colorMode != ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT) {
        window.colorMode = ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT
      }
    }

    FrescoVitoProvider.getController()
        .fetch(
            frescoDrawable = frescoDrawable,
            imageRequest = request!!,
            callerContext = callerContext,
            contextChain = contextChain,
            listener = imageListener,
            onFadeListener = onFadeListener,
            viewportDimensions = viewportDimensions)
    frescoDrawable.imagePerfListener.onImageMount(frescoDrawable)
    prefetchDataSource?.close()
    if (FrescoVitoProvider.getConfig().prefetchConfig.cancelPrefetchWhenFetched()) {
      cancelWorkingRangePrefetch(workingRangePrefetchData)
    }
  }

  @JvmStatic
  @OnBind
  fun onBind(
      c: ComponentContext,
      frescoDrawable: FrescoDrawableInterface,
      @Prop(optional = true) imageListener: ImageListener?,
      @Prop(optional = true) onFadeListener: OnFadeListener?,
      @Prop(optional = true) callerContext: Any?,
      @TreeProp contextChain: ContextChain?,
      @CachedValue requestCachedValue: VitoImageRequest?,
      @FromBoundsDefined requestFromBoundsDefined: VitoImageRequest?,
      @FromPrepare prefetchDataSource: DataSource<Void?>?,
      @FromBoundsDefined viewportDimensions: Rect,
      @State workingRangePrefetchData: AtomicReference<DataSource<Void>>?,
  ) {
    val request = requestCachedValue ?: requestFromBoundsDefined
    // We fetch in both mount and bind in case an unbind event triggered a delayed release.
    // We'll only trigger an actual fetch if needed. Most of the time, this will be a no-op.
    FrescoVitoProvider.getController()
        .fetch(
            frescoDrawable = frescoDrawable,
            imageRequest = request!!,
            callerContext = callerContext,
            contextChain = contextChain,
            listener = imageListener,
            onFadeListener = onFadeListener,
            viewportDimensions = viewportDimensions)
    frescoDrawable.imagePerfListener.onImageBind(frescoDrawable)
    prefetchDataSource?.close()
    if (FrescoVitoProvider.getConfig().prefetchConfig.cancelPrefetchWhenFetched()) {
      cancelWorkingRangePrefetch(workingRangePrefetchData)
    }
  }

  @JvmStatic
  @OnUnbind
  fun onUnbind(
      c: ComponentContext,
      frescoDrawable: FrescoDrawableInterface,
      @FromPrepare prefetchDataSource: DataSource<Void>?,
  ) {
    frescoDrawable.imagePerfListener.onImageUnbind(frescoDrawable)
    frescoDrawable.imageListener = null
    if (FrescoVitoProvider.getConfig().useBindOnly()) {
      FrescoVitoProvider.getController().releaseImmediately(frescoDrawable)
    } else {
      FrescoVitoProvider.getController().releaseDelayed(frescoDrawable)
    }
    prefetchDataSource?.close()
  }

  @JvmStatic
  @OnUnmount
  fun onUnmount(
      c: ComponentContext,
      frescoDrawable: FrescoDrawableInterface,
      @FromPrepare prefetchDataSource: DataSource<Void>?,
  ) {
    frescoDrawable.imagePerfListener.onImageUnmount(frescoDrawable)
    frescoDrawable.imageListener = null
    if (FrescoVitoProvider.getConfig().useBindOnly()) {
      return
    }
    FrescoVitoProvider.getController().release(frescoDrawable)
    prefetchDataSource?.close()
  }

  @JvmStatic
  @ShouldUpdate(onMount = true)
  fun shouldUpdate(
      @Prop(optional = true) uri: Diff<Uri>,
      @Prop(optional = true) imageSource: Diff<ImageSource>,
      @Prop(optional = true) imageOptions: Diff<ImageOptions>,
      @Prop(optional = true, resType = ResType.FLOAT) imageAspectRatio: Diff<Float>,
      @Prop(optional = true) imageListener: Diff<ImageListener>,
  ): Boolean =
      !ObjectsCompat.equals(uri.previous, uri.next) ||
          !ObjectsCompat.equals(imageSource.previous, imageSource.next) ||
          !ObjectsCompat.equals(imageOptions.previous, imageOptions.next) ||
          !ObjectsCompat.equals(imageAspectRatio.previous, imageAspectRatio.next) ||
          !ObjectsCompat.equals(imageListener.previous, imageListener.next)

  @JvmStatic
  @OnPopulateAccessibilityNode
  fun onPopulateAccessibilityNode(
      c: ComponentContext,
      host: View,
      node: AccessibilityNodeInfoCompat
  ) {
    node.className = AccessibilityRole.IMAGE
  }

  @JvmStatic
  @OnBoundsDefined
  fun onBoundsDefined(
      c: ComponentContext,
      layout: ComponentLayout,
      viewportDimensions: Output<Rect>,
      requestFromBoundsDefined: Output<VitoImageRequest>,
      @Prop(optional = true) uriString: String?,
      @Prop(optional = true) uri: Uri?,
      @Prop(optional = true) imageSource: ImageSource?,
      @Prop(optional = true) imageOptions: ImageOptions?,
      @Prop(optional = true) logWithHighSamplingRate: Boolean?,
  ) {
    val width = layout.width
    val height = layout.height
    var paddingX = 0
    var paddingY = 0
    if (layout.isPaddingSet) {
      paddingX = layout.paddingLeft + layout.paddingRight
      paddingY = layout.paddingTop + layout.paddingBottom
    }
    val viewportRect = Rect(0, 0, width - paddingX, height - paddingY)
    viewportDimensions.set(viewportRect)
    if (imageOptions != null && imageOptions.experimentalDynamicSize) {
      requestFromBoundsDefined.set(
          createVitoImageRequest(
              c, imageSource, uri, uriString, imageOptions, logWithHighSamplingRate, viewportRect))
    }
  }

  @JvmStatic
  @OnEnteredRange(name = "imagePrefetch")
  fun onEnteredWorkingRange(
      c: ComponentContext,
      @Prop(optional = true) prefetch: Prefetch?,
      @Prop(optional = true) callerContext: Any?,
      @CachedValue requestCachedValue: VitoImageRequest?,
      @FromPrepare prefetchDataSource: DataSource<Void>?,
      @State workingRangePrefetchData: AtomicReference<DataSource<Void>>?,
  ) {
    if (requestCachedValue == null || workingRangePrefetchData == null) {
      return
    }
    cancelWorkingRangePrefetch(workingRangePrefetchData)
    val prefetchConfig = FrescoVitoProvider.getConfig().prefetchConfig
    if (shouldPrefetchWithWorkingRange(prefetch)) {
      workingRangePrefetchData.set(
          FrescoVitoProvider.getPrefetcher()
              .prefetch(
                  prefetchConfig.prefetchTargetWorkingRange(),
                  requestCachedValue,
                  callerContext,
                  null,
                  "OnEnteredRange"))
      if (prefetchDataSource != null &&
          prefetchConfig.cancelOnPreparePrefetchWhenWorkingRangePrefetch()) {
        prefetchDataSource.close()
      }
    }
  }

  @JvmStatic
  @OnExitedRange(name = "imagePrefetch")
  fun onExitedWorkingRange(
      c: ComponentContext,
      @State workingRangePrefetchData: AtomicReference<DataSource<Void>>?,
  ) {
    cancelWorkingRangePrefetch(workingRangePrefetchData)
  }

  @JvmStatic
  @OnEnteredRange(name = "below3")
  fun onEnteredBelow3WorkingRange(
      c: ComponentContext,
      @Prop(optional = true) callerContext: Any?,
      @CachedValue requestCachedValue: VitoImageRequest?,
  ) {
    if (requestCachedValue == null) {
      return
    }
    FrescoVitoProvider.getPrefetcher()
        .setDistanceToViewport(3, callerContext, getUri(requestCachedValue), "FrescoVitoImage2")
  }

  @JvmStatic
  @OnEnteredRange(name = "below2")
  fun onEnteredBelow2WorkingRange(
      c: ComponentContext,
      @Prop(optional = true) callerContext: Any?,
      @CachedValue requestCachedValue: VitoImageRequest?,
  ) {
    if (requestCachedValue == null) {
      return
    }
    FrescoVitoProvider.getPrefetcher()
        .setDistanceToViewport(2, callerContext, getUri(requestCachedValue), "FrescoVitoImage2")
  }

  @JvmStatic
  @OnEnteredRange(name = "below1")
  fun onEnteredBelowWorkingRange(
      c: ComponentContext,
      @Prop(optional = true) callerContext: Any?,
      @CachedValue requestCachedValue: VitoImageRequest?,
  ) {
    if (requestCachedValue == null) {
      return
    }
    FrescoVitoProvider.getPrefetcher()
        .setDistanceToViewport(1, callerContext, getUri(requestCachedValue), "FrescoVitoImage2")
  }

  @JvmStatic
  @OnEnteredRange(name = "visible")
  fun onEnteredVisibleWorkingRange(
      c: ComponentContext,
      @Prop(optional = true) callerContext: Any?,
      @CachedValue requestCachedValue: VitoImageRequest?,
  ) {
    if (requestCachedValue == null) {
      return
    }
    FrescoVitoProvider.getPrefetcher()
        .setDistanceToViewport(0, callerContext, getUri(requestCachedValue), "FrescoVitoImage2")
  }

  @JvmStatic
  @OnEnteredRange(name = "above")
  fun onEnteredAboveWorkingRange(
      c: ComponentContext,
      @Prop(optional = true) callerContext: Any?,
      @CachedValue requestCachedValue: VitoImageRequest?,
  ) {
    if (requestCachedValue == null) {
      return
    }
    FrescoVitoProvider.getPrefetcher()
        .setDistanceToViewport(-1, callerContext, getUri(requestCachedValue), "FrescoVitoImage2")
  }

  private fun determineImageSource(
      imageSource: ImageSource?,
      uri: Uri?,
      uriString: String?,
  ): ImageSource =
      when {
        imageSource != null -> imageSource
        uri != null -> ImageSourceProvider.forUri(uri)
        uriString != null -> ImageSourceProvider.forUri(uriString)
        else -> ImageSourceProvider.emptySource()
      }

  private fun getUri(requestCachedValue: VitoImageRequest): Uri? =
      requestCachedValue.finalImageRequest?.sourceUri

  @JvmStatic
  @OnRegisterRanges
  fun registerWorkingRanges(
      c: ComponentContext,
      @Prop(optional = true) prefetch: Prefetch?,
  ) {
    if (FrescoVitoProvider.hasBeenInitialized()) {
      val prefetchConfig = FrescoVitoProvider.getConfig().prefetchConfig
      if (shouldPrefetchWithWorkingRange(prefetch)) {
        FrescoVitoImage2.registerImagePrefetchWorkingRange(
            c, BoundaryWorkingRange(prefetchConfig.prefetchWorkingRangeSize()))
      }
      if (prefetchConfig.prioritizeWithWorkingRange()) {
        FrescoVitoImage2.registerBelow3WorkingRange(c, BelowViewportWorkingRange(3, Int.MAX_VALUE))
        FrescoVitoImage2.registerBelow2WorkingRange(c, BelowViewportWorkingRange(2, 2))
        FrescoVitoImage2.registerBelow1WorkingRange(c, BelowViewportWorkingRange(1, 1))
        FrescoVitoImage2.registerVisibleWorkingRange(c, InViewportWorkingRange())
        FrescoVitoImage2.registerAboveWorkingRange(c, AboveViewportWorkingRange())
      }
    }
  }

  @JvmStatic
  fun cancelWorkingRangePrefetch(prefetchData: AtomicReference<DataSource<Void>>?) {
    if (prefetchData == null) {
      return
    }
    val dataSource = prefetchData.get()
    dataSource?.close()
    prefetchData.set(null)
  }

  @JvmStatic
  fun shouldPrefetchInOnPrepare(prefetch: Prefetch?): Boolean =
      when (prefetch ?: Prefetch.AUTO) {
        Prefetch.YES -> true
        Prefetch.NO -> false
        else -> FrescoVitoProvider.getConfig().prefetchConfig.prefetchInOnPrepare()
      }

  @JvmStatic
  fun shouldPrefetchWithWorkingRange(prefetch: Prefetch?): Boolean =
      when (prefetch ?: Prefetch.AUTO) {
        Prefetch.YES -> true
        Prefetch.NO -> false
        else -> FrescoVitoProvider.getConfig().prefetchConfig.prefetchWithWorkingRange()
      }

  enum class Prefetch {
    AUTO,
    YES,
    NO;

    companion object {
      @JvmStatic
      fun parsePrefetch(value: Long): Prefetch {
        if (value == 2L) {
          return NO
        }
        return if (value == 1L) {
          YES
        } else {
          AUTO
        }
      }
    }
  }
}
