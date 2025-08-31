/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.options

import android.graphics.PointF
import com.facebook.common.internal.Objects
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.imagepipeline.common.ImageDecodeOptions
import com.facebook.imagepipeline.common.ResizeOptions
import com.facebook.imagepipeline.common.RotationOptions
import com.facebook.imagepipeline.core.DownsampleMode
import com.facebook.imagepipeline.request.Postprocessor

open class DecodedImageOptions(builder: Builder<*>) : EncodedImageOptions(builder) {
  val resizeOptions: ResizeOptions? = builder.resizeOptions
  val downsampleOverride: DownsampleMode? = builder.downsampleOverride
  val rotationOptions: RotationOptions? = builder.rotationOptions
  val postprocessor: Postprocessor? = builder.postprocessor
  val imageDecodeOptions: ImageDecodeOptions? = builder.imageDecodeOptions
  val roundingOptions: RoundingOptions? = builder.roundingOptions
  val animatedOptions: AnimatedOptions? = builder.animatedOptions
  val borderOptions: BorderOptions? = builder.borderOptions
  val actualImageScaleType: ScalingUtils.ScaleType = builder.actualImageScaleType
  val actualImageFocusPoint: PointF? = builder.actualFocusPoint
  val localThumbnailPreviewsEnabled: Boolean = builder.localThumbnailPreviewsEnabled
  val loadThumbnailOnly: Boolean = builder.loadThumbnailOnly
  val bitmapConfig: BitmapConfig? = builder.bitmapConfig
  val isProgressiveDecodingEnabled: Boolean? = builder.progressiveDecodingEnabled
  val isFirstFrameThumbnailEnabled: Boolean = builder.isFirstFrameThumbnailEnabled

  fun areLocalThumbnailPreviewsEnabled(): Boolean = localThumbnailPreviewsEnabled

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) return false
    return equalDecodedOptions(other as DecodedImageOptions)
  }

  protected fun equalDecodedOptions(other: DecodedImageOptions): Boolean {
    return if (
        !Objects.equal(resizeOptions, other.resizeOptions) ||
            !Objects.equal(downsampleOverride, other.downsampleOverride) ||
            !Objects.equal(rotationOptions, other.rotationOptions) ||
            !Objects.equal(postprocessor, other.postprocessor) ||
            !Objects.equal(imageDecodeOptions, other.imageDecodeOptions) ||
            !Objects.equal(roundingOptions, other.roundingOptions) ||
            !Objects.equal(animatedOptions, other.animatedOptions) ||
            !Objects.equal(borderOptions, other.borderOptions) ||
            !Objects.equal(actualImageScaleType, other.actualImageScaleType) ||
            !Objects.equal(actualImageFocusPoint, other.actualImageFocusPoint) ||
            localThumbnailPreviewsEnabled != other.localThumbnailPreviewsEnabled ||
            loadThumbnailOnly != other.loadThumbnailOnly ||
            isProgressiveDecodingEnabled !== other.isProgressiveDecodingEnabled ||
            !Objects.equal(bitmapConfig, other.bitmapConfig) ||
            isFirstFrameThumbnailEnabled != other.isFirstFrameThumbnailEnabled
    ) {
      false
    } else equalEncodedOptions(other)
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + (resizeOptions?.hashCode() ?: 0)
    result = 31 * result + (downsampleOverride?.hashCode() ?: 0)
    result = 31 * result + (rotationOptions?.hashCode() ?: 0)
    result = 31 * result + (postprocessor?.hashCode() ?: 0)
    result = 31 * result + (imageDecodeOptions?.hashCode() ?: 0)
    result = 31 * result + (roundingOptions?.hashCode() ?: 0)
    result = 31 * result + (animatedOptions?.hashCode() ?: 0)
    result = 31 * result + (borderOptions?.hashCode() ?: 0)
    result = 31 * result + actualImageScaleType.hashCode()
    result = 31 * result + (actualImageFocusPoint?.hashCode() ?: 0)
    result = 31 * result + if (localThumbnailPreviewsEnabled) 1 else 0
    result = 31 * result + if (loadThumbnailOnly) 1 else 0
    result = 31 * result + (bitmapConfig?.hashCode() ?: 0)
    result = (31 * result + (isProgressiveDecodingEnabled?.hashCode() ?: 0))
    result = 31 * result + if (isFirstFrameThumbnailEnabled) 1 else 0
    return result
  }

  override fun toString(): String = "DecodedImageOptions{" + toStringHelper().toString() + "}"

  override fun toStringHelper(): Objects.ToStringHelper =
      super.toStringHelper()
          .add("resizeOptions", resizeOptions)
          .add("downsampleOverride", downsampleOverride)
          .add("rotationOptions", rotationOptions)
          .add("postprocessor", postprocessor)
          .add("imageDecodeOptions", imageDecodeOptions)
          .add("roundingOptions", roundingOptions)
          .add("animatedOptions", animatedOptions)
          .add("borderOptions", borderOptions)
          .add("actualImageScaleType", actualImageScaleType)
          .add("actualImageFocusPoint", actualImageFocusPoint)
          .add("localThumbnailPreviewsEnabled", localThumbnailPreviewsEnabled)
          .add("loadThumbnailOnly", loadThumbnailOnly)
          .add("bitmapConfig", bitmapConfig)
          .add("progressiveRenderingEnabled", isProgressiveDecodingEnabled)
          .add("isFirstFrameThumbnailEnabled", isFirstFrameThumbnailEnabled)

  open class Builder<T : Builder<T>> : EncodedImageOptions.Builder<T> {
    internal var resizeOptions: ResizeOptions? = null
    internal var downsampleOverride: DownsampleMode? = null
    internal var rotationOptions: RotationOptions? = null
    internal var postprocessor: Postprocessor? = null
    internal var imageDecodeOptions: ImageDecodeOptions? = null
    internal var roundingOptions: RoundingOptions? = null
    internal var animatedOptions: AnimatedOptions? = null
    internal var borderOptions: BorderOptions? = null
    internal var actualImageScaleType: ScalingUtils.ScaleType = ScalingUtils.ScaleType.CENTER_CROP
    internal var actualFocusPoint: PointF? = null
    internal var localThumbnailPreviewsEnabled = false
    internal var loadThumbnailOnly = false
    internal var bitmapConfig: BitmapConfig? = null
    internal var progressiveDecodingEnabled: Boolean? = null
    internal var isFirstFrameThumbnailEnabled: Boolean = false

    constructor() : super()

    constructor(decodedImageOptions: DecodedImageOptions) : super(decodedImageOptions) {
      resizeOptions = decodedImageOptions.resizeOptions
      downsampleOverride = decodedImageOptions.downsampleOverride
      rotationOptions = decodedImageOptions.rotationOptions
      postprocessor = decodedImageOptions.postprocessor
      imageDecodeOptions = decodedImageOptions.imageDecodeOptions
      roundingOptions = decodedImageOptions.roundingOptions
      animatedOptions = decodedImageOptions.animatedOptions
      borderOptions = decodedImageOptions.borderOptions
      actualImageScaleType = decodedImageOptions.actualImageScaleType
      actualFocusPoint = decodedImageOptions.actualImageFocusPoint
      localThumbnailPreviewsEnabled = decodedImageOptions.areLocalThumbnailPreviewsEnabled()
      loadThumbnailOnly = decodedImageOptions.loadThumbnailOnly
      bitmapConfig = decodedImageOptions.bitmapConfig
      progressiveDecodingEnabled = decodedImageOptions.isProgressiveDecodingEnabled
      isFirstFrameThumbnailEnabled = decodedImageOptions.isFirstFrameThumbnailEnabled
    }

    constructor(defaultOptions: ImageOptions) : this(defaultOptions as DecodedImageOptions)

    fun resize(resizeOptions: ResizeOptions?): T = modify { this.resizeOptions = resizeOptions }

    /**
     * Custom downsample override for this request. null -> use default pipeline's setting.
     *
     * @param downsampleOverride
     * @return the builder
     */
    fun downsampleOverride(downsampleOverride: DownsampleMode?): T = modify {
      this.downsampleOverride = downsampleOverride
    }

    fun rotate(rotationOptions: RotationOptions?): T = modify {
      this.rotationOptions = rotationOptions
    }

    fun postprocess(postprocessor: Postprocessor?): T = modify {
      this.postprocessor = postprocessor
    }

    fun imageDecodeOptions(imageDecodeOptions: ImageDecodeOptions?): T = modify {
      this.imageDecodeOptions = imageDecodeOptions
    }

    /**
     * Set the rounding options to be used or null if the image should not be rounded.
     *
     * @param roundingOptions the rounding options to use
     * @return the builder
     */
    fun round(roundingOptions: RoundingOptions?): T = modify {
      this.roundingOptions = roundingOptions
    }

    /**
     * Set the animated options to be used or null if default animation behavior should be used.
     *
     * @param animatedOptions the animated options to use
     * @return the builder
     */
    fun animated(animatedOptions: AnimatedOptions?): T = modify {
      this.animatedOptions = animatedOptions
    }

    fun borders(borderOptions: BorderOptions?): T = modify { this.borderOptions = borderOptions }

    fun scale(actualImageScaleType: ScalingUtils.ScaleType?): T = modify {
      this.actualImageScaleType =
          actualImageScaleType ?: ImageOptions.defaults().actualImageScaleType
    }

    fun focusPoint(focusPoint: PointF?): T = modify { actualFocusPoint = focusPoint }

    /**
     * Display local thumbnail previews, for example EXIF thumbnails.
     *
     * @param localThumbnailPreviewsEnabled true if thumbnails should be displayed
     * @return the builder
     */
    fun localThumbnailPreviewsEnabled(localThumbnailPreviewsEnabled: Boolean): T = modify {
      this.localThumbnailPreviewsEnabled = localThumbnailPreviewsEnabled
    }

    fun loadThumbnailOnly(loadThumbnailOnly: Boolean): T = modify {
      this.loadThumbnailOnly = loadThumbnailOnly
    }

    fun bitmapConfig(bitmapConfig: BitmapConfig?): T = modify { this.bitmapConfig = bitmapConfig }

    fun progressiveRendering(progressiveDecodingEnabled: Boolean?): T = modify {
      this.progressiveDecodingEnabled = progressiveDecodingEnabled
    }

    /**
     * Whether fetch first frame thumbnail from video.
     *
     * @param isFirstFrameThumbnailEnabled true if enforce extract first frame thumbnail
     * @return the builder
     */
    fun isFirstFrameThumbnailEnabled(isFirstFrameThumbnailEnabled: Boolean): T = modify {
      this.isFirstFrameThumbnailEnabled = isFirstFrameThumbnailEnabled
    }

    override fun build(): DecodedImageOptions = DecodedImageOptions(this)

    private inline fun modify(block: Builder<T>.() -> Unit): T {
      block()
      return getThis()
    }
  }
}
