/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.options

import android.graphics.Bitmap
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
  val borderOptions: BorderOptions? = builder.borderOptions
  val actualImageScaleType: ScalingUtils.ScaleType = builder.actualImageScaleType
  val actualImageFocusPoint: PointF? = builder.actualFocusPoint
  val mLocalThumbnailPreviewsEnabled: Boolean = builder.localThumbnailPreviewsEnabled
  val loadThumbnailOnly: Boolean = builder.loadThumbnailOnly
  val bitmapConfig: Bitmap.Config? = builder.bitmapConfig
  val isProgressiveDecodingEnabled: Boolean? = builder.progressiveDecodingEnabled

  fun areLocalThumbnailPreviewsEnabled(): Boolean = mLocalThumbnailPreviewsEnabled

  override fun equals(other: Any?): Boolean {
    if (this === other) {
      return true
    }
    if (other == null || javaClass != other.javaClass) return false
    return equalDecodedOptions(other as DecodedImageOptions)
  }

  protected fun equalDecodedOptions(other: DecodedImageOptions): Boolean {
    return if (!Objects.equal(resizeOptions, other.resizeOptions) ||
        !Objects.equal(downsampleOverride, other.downsampleOverride) ||
        !Objects.equal(rotationOptions, other.rotationOptions) ||
        !Objects.equal(postprocessor, other.postprocessor) ||
        !Objects.equal(imageDecodeOptions, other.imageDecodeOptions) ||
        !Objects.equal(roundingOptions, other.roundingOptions) ||
        !Objects.equal(borderOptions, other.borderOptions) ||
        !Objects.equal(actualImageScaleType, other.actualImageScaleType) ||
        !Objects.equal(actualImageFocusPoint, other.actualImageFocusPoint) ||
        mLocalThumbnailPreviewsEnabled != other.mLocalThumbnailPreviewsEnabled ||
        loadThumbnailOnly != other.loadThumbnailOnly ||
        isProgressiveDecodingEnabled !== other.isProgressiveDecodingEnabled ||
        !Objects.equal(bitmapConfig, other.bitmapConfig)) {
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
    result = 31 * result + (borderOptions?.hashCode() ?: 0)
    result = 31 * result + actualImageScaleType.hashCode()
    result = 31 * result + (actualImageFocusPoint?.hashCode() ?: 0)
    result = 31 * result + if (mLocalThumbnailPreviewsEnabled) 1 else 0
    result = 31 * result + if (loadThumbnailOnly) 1 else 0
    result = 31 * result + (bitmapConfig?.hashCode() ?: 0)
    result = (31 * result + (isProgressiveDecodingEnabled?.hashCode() ?: 0))
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
          .add("borderOptions", borderOptions)
          .add("actualImageScaleType", actualImageScaleType)
          .add("actualImageFocusPoint", actualImageFocusPoint)
          .add("localThumbnailPreviewsEnabled", mLocalThumbnailPreviewsEnabled)
          .add("loadThumbnailOnly", loadThumbnailOnly)
          .add("bitmapConfig", bitmapConfig)
          .add("progressiveRenderingEnabled", isProgressiveDecodingEnabled)

  open class Builder<T : Builder<T>> : EncodedImageOptions.Builder<T> {
    internal var resizeOptions: ResizeOptions? = null
    internal var downsampleOverride: DownsampleMode? = null
    internal var rotationOptions: RotationOptions? = null
    internal var postprocessor: Postprocessor? = null
    internal var imageDecodeOptions: ImageDecodeOptions? = null
    internal var roundingOptions: RoundingOptions? = null
    internal var borderOptions: BorderOptions? = null
    internal var actualImageScaleType: ScalingUtils.ScaleType = ScalingUtils.ScaleType.CENTER_CROP
    internal var actualFocusPoint: PointF? = null
    internal var localThumbnailPreviewsEnabled = false
    internal var loadThumbnailOnly = false
    internal var bitmapConfig: Bitmap.Config? = null
    internal var progressiveDecodingEnabled: Boolean? = null

    constructor() : super()

    constructor(decodedImageOptions: DecodedImageOptions) : super(decodedImageOptions) {
      resizeOptions = decodedImageOptions.resizeOptions
      downsampleOverride = decodedImageOptions.downsampleOverride
      rotationOptions = decodedImageOptions.rotationOptions
      postprocessor = decodedImageOptions.postprocessor
      imageDecodeOptions = decodedImageOptions.imageDecodeOptions
      roundingOptions = decodedImageOptions.roundingOptions
      borderOptions = decodedImageOptions.borderOptions
      actualImageScaleType = decodedImageOptions.actualImageScaleType
      actualFocusPoint = decodedImageOptions.actualImageFocusPoint
      localThumbnailPreviewsEnabled = decodedImageOptions.areLocalThumbnailPreviewsEnabled()
      loadThumbnailOnly = decodedImageOptions.loadThumbnailOnly
      bitmapConfig = decodedImageOptions.bitmapConfig
      progressiveDecodingEnabled = decodedImageOptions.isProgressiveDecodingEnabled
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

    fun bitmapConfig(bitmapConfig: Bitmap.Config?): T = modify { this.bitmapConfig = bitmapConfig }

    fun progressiveRendering(progressiveDecodingEnabled: Boolean?): T = modify {
      this.progressiveDecodingEnabled = progressiveDecodingEnabled
    }

    override fun build(): DecodedImageOptions = DecodedImageOptions(this)

    private inline fun modify(block: Builder<T>.() -> Unit): T {
      block()
      return getThis()
    }
  }
}
