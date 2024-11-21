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
import android.os.Looper
import android.view.View
import androidx.core.util.ObjectsCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import com.facebook.common.callercontext.ContextChain
import com.facebook.datasource.DataSource
import com.facebook.fresco.middleware.HasExtraData
import com.facebook.fresco.ui.common.OnFadeListener
import com.facebook.fresco.urimod.UriModifierInterface.ModificationResult
import com.facebook.fresco.vito.core.FrescoDrawableInterface
import com.facebook.fresco.vito.core.VitoImageRequest
import com.facebook.fresco.vito.listener.ImageListener
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.provider.FrescoVitoProvider
import com.facebook.fresco.vito.source.ImageSource
import com.facebook.fresco.vito.source.ImageSourceProvider
import com.facebook.imagepipeline.listener.RequestListener
import com.facebook.litho.AccessibilityRole
import com.facebook.litho.ComponentContext
import com.facebook.litho.ComponentLayout
import com.facebook.litho.ContextUtils
import com.facebook.litho.Diff
import com.facebook.litho.Output
import com.facebook.litho.Size
import com.facebook.litho.annotations.CachedValue
import com.facebook.litho.annotations.FromBoundsDefined
import com.facebook.litho.annotations.FromPrepare
import com.facebook.litho.annotations.MountSpec
import com.facebook.litho.annotations.MountingType
import com.facebook.litho.annotations.OnBind
import com.facebook.litho.annotations.OnBoundsDefined
import com.facebook.litho.annotations.OnCalculateCachedValue
import com.facebook.litho.annotations.OnCreateMountContent
import com.facebook.litho.annotations.OnMeasure
import com.facebook.litho.annotations.OnMount
import com.facebook.litho.annotations.OnPopulateAccessibilityNode
import com.facebook.litho.annotations.OnPrepare
import com.facebook.litho.annotations.OnUnbind
import com.facebook.litho.annotations.OnUnmount
import com.facebook.litho.annotations.Prop
import com.facebook.litho.annotations.PropDefault
import com.facebook.litho.annotations.ResType
import com.facebook.litho.annotations.ShouldExcludeFromIncrementalMount
import com.facebook.litho.annotations.ShouldUpdate
import com.facebook.litho.annotations.TreeProp
import com.facebook.litho.utils.MeasureUtils
import java.util.concurrent.TimeUnit

/** Fresco Vito component for Litho */
@MountSpec(isPureRender = true, canPreallocate = true, poolSize = 15)
object FrescoVitoImage2Spec {

  private const val DEFAULT_IMAGE_ASPECT_RATIO = 1f
  @PropDefault const val imageAspectRatio: Float = DEFAULT_IMAGE_ASPECT_RATIO

  @PropDefault val prefetch: Prefetch = Prefetch.AUTO

  @PropDefault const val mutateDrawables: Boolean = true

  @JvmStatic
  @OnCreateMountContent(mountingType = MountingType.DRAWABLE)
  fun onCreateMountContent(c: Context?): FrescoDrawableInterface =
      FrescoVitoProvider.getController().createDrawable("litho")

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
    val resolvedAspectRatio: Float =
        if (!(imageAspectRatio > 0f)) {
          // If the image aspect ratio is not set correctly, we will use the default aspect ratio of
          // 1.0f, we've seen bad inputs like 0.0f and NaN.
          DEFAULT_IMAGE_ASPECT_RATIO
        } else {
          imageAspectRatio
        }
    MeasureUtils.measureWithAspectRatio(widthSpec, heightSpec, resolvedAspectRatio, size)
  }

  @JvmStatic
  @OnCalculateCachedValue(name = "requestCachedValue")
  fun onCalculateImageRequest(
      c: ComponentContext,
      @Prop(optional = true) callerContext: Any?,
      @Prop(optional = true) uriString: String?,
      @Prop(optional = true) uri: Uri?,
      @Prop(optional = true) imageSource: ImageSource?,
      @Prop(optional = true) imageOptions: ImageOptions?,
      @Prop(optional = true) logWithHighSamplingRate: Boolean?,
  ): VitoImageRequest? {
    val shouldCreateRequest =
        if (experimentalDynamicSizeVito2() && !experimentalDynamicSizeWithCacheFallbackVito2()) {
          // we won't do anything with the original URI, so we can just return null
          false
        } else {
          true
        }
    return if (shouldCreateRequest) {
      val imageOptions = ensureImageOptions(imageOptions)
      return createVitoImageRequest(
          c,
          callerContext,
          imageSource,
          uri,
          uriString,
          imageOptions,
          logWithHighSamplingRate,
          viewportRect = null,
          forceKeepOriginalSize = true,
          forLoggingOnly = false)
    } else {
      null
    }
  }

  private fun createVitoImageRequest(
      c: ComponentContext,
      callerContext: Any?,
      imageSource: ImageSource?,
      uri: Uri?,
      uriString: String?,
      imageOptions: ImageOptions?,
      logWithHighSamplingRate: Boolean?,
      viewportRect: Rect?,
      forceKeepOriginalSize: Boolean,
      forLoggingOnly: Boolean,
  ): VitoImageRequest =
      FrescoVitoProvider.getImagePipeline()
          .createImageRequest(
              c.resources,
              determineImageSource(imageSource, uri, uriString),
              imageOptions,
              logWithHighSamplingRate ?: false,
              viewportRect,
              callerContext,
              null,
              forceKeepOriginalSize,
              forLoggingOnly)

  @JvmStatic
  @OnPrepare
  fun onPrepare(
      c: ComponentContext,
      @Prop(optional = true) callerContext: Any?,
      @TreeProp contextChain: ContextChain?,
      @Prop(optional = true) prefetch: Prefetch?,
      @Prop(optional = true) prefetchRequestListener: RequestListener?,
      @CachedValue requestCachedValue: VitoImageRequest?,
      prefetchDataSource: Output<DataSource<Void?>>,
      forceKeepOriginalSize: Output<Boolean>,
  ) {
    if (requestCachedValue == null) {
      forceKeepOriginalSize.set(false)
      return
    }
    if (experimentalDynamicSizeVito2() && experimentalDynamicSizeWithCacheFallbackVito2()) {
      if (Looper.myLooper() == Looper.getMainLooper()) {
        // We don't want to check cache if we are running on the main thread
        // By default uses the original URL
        forceKeepOriginalSize.set(!experimentalDynamicSizeOnPrepareMainThreadVito2())
      } else if (isProductEnabled(callerContext, contextChain) == false) {
        forceKeepOriginalSize.set(true)
      } else if (FrescoVitoProvider.getConfig().experimentalDynamicSizeDisableWhenAppIsStarting() &&
          FrescoVitoProvider.getConfig().isAppStarting()) {
        // App is still starting
        forceKeepOriginalSize.set(true)
      } else {
        val isInDiskCache =
            FrescoVitoProvider.getImagePipeline()
                .isInDiskCacheSync(
                    requestCachedValue,
                    FrescoVitoProvider.getConfig().experimentalDynamicSizeDiskCacheCheckTimeoutMs(),
                    TimeUnit.MILLISECONDS)
        if (isInDiskCache == null) {
          // On timeout, force keep original size by default
          forceKeepOriginalSize.set(
              !FrescoVitoProvider.getConfig().experimentalDynamicSizeUseSfOnDiskCacheTimeout())
        } else {
          forceKeepOriginalSize.set(isInDiskCache)
        }
      }
      if (forceKeepOriginalSize.get() == true) {
        // Tell image pipeline that we're using an unmodified URL so it can ensure we can prefetch
        FrescoVitoProvider.getImagePipeline().hintUnmodifiedUri(requestCachedValue)

        // Prefetch in OnPrepare since no prefetch will happen in OnBoundsDefined
        maybePrefetchInOnPrepare(
            prefetch,
            prefetchDataSource,
            requestCachedValue,
            callerContext,
            contextChain,
            prefetchRequestListener)
      }
    } else {
      forceKeepOriginalSize.set(false)
      maybePrefetchInOnPrepare(
          prefetch,
          prefetchDataSource,
          requestCachedValue,
          callerContext,
          contextChain,
          prefetchRequestListener)
    }
  }

  private fun isProductEnabled(callerContext: Any?, contextChain: ContextChain?): Boolean? {
    val config = FrescoVitoProvider.getConfig()
    if (!config.experimentalDynamicSizeCheckIfProductIsEnabled()) {
      return null
    }
    return config.experimentalDynamicSizeIsProductEnabled(callerContext, contextChain)
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
      @FromBoundsDefined prefetchDataSourceFromBoundsDefined: DataSource<Void?>?,
      @FromBoundsDefined viewportDimensions: Rect,
      @TreeProp contextChain: ContextChain?,
  ) {
    // if requestFromBoundsDefined is not null, we are using SF
    val request = requestFromBoundsDefined ?: requestCachedValue

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
            drawable = frescoDrawable,
            imageRequest = request!!,
            callerContext = callerContext,
            contextChain = contextChain,
            listener = imageListener,
            onFadeListener = onFadeListener,
            viewportDimensions = viewportDimensions)
    frescoDrawable.imagePerfListener.onImageMount(frescoDrawable)
    prefetchDataSource?.close()
    prefetchDataSourceFromBoundsDefined?.close()
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
      @FromBoundsDefined prefetchDataSourceFromBoundsDefined: DataSource<Void?>?,
      @FromBoundsDefined viewportDimensions: Rect,
  ) {
    // if requestFromBoundsDefined is not null, we are using SF
    val request = requestFromBoundsDefined ?: requestCachedValue

    // We fetch in both mount and bind in case an unbind event triggered a delayed release.
    // We'll only trigger an actual fetch if needed. Most of the time, this will be a no-op.
    FrescoVitoProvider.getController()
        .fetch(
            drawable = frescoDrawable,
            imageRequest = request!!,
            callerContext = callerContext,
            contextChain = contextChain,
            listener = imageListener,
            onFadeListener = onFadeListener,
            viewportDimensions = viewportDimensions)
    frescoDrawable.imagePerfListener.onImageBind(frescoDrawable)
    prefetchDataSource?.close()
    prefetchDataSourceFromBoundsDefined?.close()
  }

  @JvmStatic
  @OnUnbind
  fun onUnbind(
      c: ComponentContext,
      frescoDrawable: FrescoDrawableInterface,
      @FromPrepare prefetchDataSource: DataSource<Void?>?,
      @FromBoundsDefined prefetchDataSourceFromBoundsDefined: DataSource<Void?>?,
  ) {
    frescoDrawable.imagePerfListener.onImageUnbind(frescoDrawable)
    if (FrescoVitoProvider.getConfig().useBindOnly()) {
      FrescoVitoProvider.getController().releaseImmediately(frescoDrawable)
    } else {
      FrescoVitoProvider.getController().releaseDelayed(frescoDrawable)
    }
    prefetchDataSource?.close()
    prefetchDataSourceFromBoundsDefined?.close()
  }

  @JvmStatic
  @OnUnmount
  fun onUnmount(
      c: ComponentContext,
      frescoDrawable: FrescoDrawableInterface,
      @FromPrepare prefetchDataSource: DataSource<Void?>?,
      @FromBoundsDefined prefetchDataSourceFromBoundsDefined: DataSource<Void?>?,
  ) {
    frescoDrawable.imagePerfListener.onImageUnmount(frescoDrawable)
    if (FrescoVitoProvider.getConfig().useBindOnly()) {
      return
    }
    FrescoVitoProvider.getController().release(frescoDrawable)
    prefetchDataSource?.close()
    prefetchDataSourceFromBoundsDefined?.close()
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
      @TreeProp contextChain: ContextChain?,
      requestFromBoundsDefined: Output<VitoImageRequest>,
      prefetchDataSourceFromBoundsDefined: Output<DataSource<Void?>>,
      @Prop(optional = true) prefetch: Prefetch?,
      @Prop(optional = true) uriString: String?,
      @Prop(optional = true) uri: Uri?,
      @Prop(optional = true) imageSource: ImageSource?,
      @Prop(optional = true) imageOptions: ImageOptions?,
      @Prop(optional = true) callerContext: Any?,
      @Prop(optional = true) logWithHighSamplingRate: Boolean?,
      @Prop(optional = true) prefetchRequestListener: RequestListener?,
      @CachedValue requestCachedValue: VitoImageRequest?,
      @FromPrepare forceKeepOriginalSize: Boolean
  ) {
    val imageOptions = ensureImageOptions(imageOptions)
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
    if (experimentalDynamicSizeVito2()) {
      val vitoImageRequest =
          createVitoImageRequest(
              c,
              callerContext,
              imageSource,
              uri,
              uriString,
              imageOptions,
              logWithHighSamplingRate,
              viewportRect,
              false,
              forLoggingOnly = forceKeepOriginalSize)
      if (!forceKeepOriginalSize) {
        requestFromBoundsDefined.set(vitoImageRequest)

        val config = FrescoVitoProvider.getConfig().prefetchConfig
        if (shouldPrefetchInBoundsDefinedForDynamicSize(prefetch)) {
          prefetchDataSourceFromBoundsDefined.set(
              FrescoVitoProvider.getPrefetcher()
                  .prefetch(
                      config.prefetchTargetOnBoundsDefined(),
                      vitoImageRequest,
                      callerContext,
                      contextChain,
                      prefetchRequestListener,
                      "FrescoVitoImage2Spec_OnBoundsDefined"))
        }
      } else {
        // Force keep original size so we will not use the OnBoundsDefined URL
        // Compare vitoImageRequest to requestCachedValue to see if original URL was the best URL
        val isBestSize =
            vitoImageRequest.finalImageRequest?.sourceUri ==
                requestCachedValue?.finalImageRequest?.sourceUri
        requestCachedValue?.putExtra(
            HasExtraData.KEY_MODIFIED_URL,
            ModificationResult.FallbackToMbpDiskCache(isBestSize).toString())
      }
    }
  }

  @JvmStatic
  @ShouldExcludeFromIncrementalMount
  fun shouldExcludeFromIM(@Prop(optional = true) shouldExcludeFromIM: Boolean): Boolean =
      shouldExcludeFromIM

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

  private fun maybePrefetchInOnPrepare(
      prefetch: Prefetch?,
      prefetchDataSource: Output<DataSource<Void?>>,
      requestCachedValue: VitoImageRequest,
      callerContext: Any?,
      contextChain: ContextChain?,
      prefetchRequestListener: RequestListener?
  ) {
    val config = FrescoVitoProvider.getConfig().prefetchConfig
    if (shouldPrefetchInOnPrepare(prefetch)) {
      prefetchDataSource.set(
          FrescoVitoProvider.getPrefetcher()
              .prefetch(
                  config.prefetchTargetOnPrepare(),
                  requestCachedValue,
                  callerContext,
                  contextChain,
                  prefetchRequestListener,
                  "FrescoVitoImage2Spec_OnPrepare"))
    }
  }

  @JvmStatic
  fun shouldPrefetchInOnPrepare(prefetch: Prefetch?): Boolean =
      when (prefetch ?: Prefetch.AUTO) {
        Prefetch.YES -> true
        Prefetch.NO -> false
        else -> FrescoVitoProvider.getConfig().prefetchConfig.prefetchInOnPrepare()
      }

  @JvmStatic
  fun shouldPrefetchInBoundsDefinedForDynamicSize(prefetch: Prefetch?): Boolean =
      when (prefetch ?: Prefetch.AUTO) {
        Prefetch.YES ->
            FrescoVitoProvider.getConfig().prefetchConfig.prefetchInOnBoundsDefinedForDynamicSize()
        Prefetch.NO -> false
        else ->
            FrescoVitoProvider.getConfig().prefetchConfig.prefetchInOnBoundsDefinedForDynamicSize()
      }

  private fun ensureImageOptions(imageOptionsProp: ImageOptions?): ImageOptions? {
    if (imageOptionsProp == null &&
        FrescoVitoProvider.getConfig().fallbackToDefaultImageOptions()) {
      return ImageOptions.defaults()
    }
    return imageOptionsProp
  }

  private fun experimentalDynamicSizeVito2(): Boolean =
      FrescoVitoProvider.getConfig().experimentalDynamicSizeVito2()

  private fun experimentalDynamicSizeWithCacheFallbackVito2(): Boolean =
      FrescoVitoProvider.getConfig().experimentalDynamicSizeWithCacheFallbackVito2()

  private fun experimentalDynamicSizeOnPrepareMainThreadVito2(): Boolean =
      FrescoVitoProvider.getConfig().experimentalDynamicSizeOnPrepareMainThreadVito2()

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
