/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.options

import android.graphics.ColorFilter
import android.graphics.PointF
import android.graphics.drawable.Drawable
import androidx.annotation.ColorInt
import androidx.annotation.DrawableRes
import com.facebook.common.internal.Objects
import com.facebook.drawee.drawable.ScalingUtils
import com.facebook.imagepipeline.common.Priority

class ImageOptions(builder: Builder) : DecodedImageOptions(builder) {

  // Placeholder
  @get:ColorInt @ColorInt val placeholderColor: Int? = builder._placeholderColor
  @get:DrawableRes @DrawableRes val placeholderRes: Int = builder._placeholderRes
  val placeholderDrawable: Drawable? = builder._placeholderDrawable
  val placeholderScaleType: ScalingUtils.ScaleType? = builder._placeholderScaleType
  val placeholderFocusPoint: PointF? = builder._placeholderFocusPoint
  val placeholderApplyRoundingOptions: Boolean = builder._placeholderApplyRoundingOptions

  // Progress bar
  @get:DrawableRes @DrawableRes val progressRes: Int = builder._progressRes
  val progressDrawable: Drawable? = builder._progressDrawable
  val progressScaleType: ScalingUtils.ScaleType? = builder._progressScaleType

  // Error
  @get:ColorInt @ColorInt val errorColor: Int? = builder._errorColor
  @get:DrawableRes @DrawableRes val errorRes: Int = builder._errorRes
  val errorScaleType: ScalingUtils.ScaleType? = builder._errorScaleType
  val errorFocusPoint: PointF? = builder._errorFocusPoint
  val errorDrawable: Drawable? = builder._errorDrawable
  val errorApplyRoundingOptions: Boolean = builder._errorApplyRoundingOptions

  // Actual image
  val actualImageColorFilter: ColorFilter? = builder._actualImageColorFilter

  // Overlay
  @get:DrawableRes @DrawableRes val overlayRes: Int = builder._overlayRes
  val overlayDrawable: Drawable? = builder._overlayDrawable

  // Background
  val backgroundDrawable: Drawable? = builder._backgroundDrawable

  private val _resizeToViewport: Boolean = builder._resizeToViewport
  val fadeDurationMs: Int = builder._fadeDurationMs
  private val _autoPlay: Boolean = builder._autoPlay
  private val _autoStop: Boolean = builder._autoStop
  val isPerfMediaRemountInstrumentationFix: Boolean = builder._perfMediaRemountInstrumentationFix
  val customDrawableFactory: ImageOptionsDrawableFactory? = builder._customDrawableFactory

  fun extend(): Builder = extend(this)

  fun shouldAutoPlay(): Boolean = _autoPlay

  fun shouldAutoStop(): Boolean = _autoStop

  fun shouldResizeToViewport(): Boolean = _resizeToViewport

  fun equalsForActualImage(other: ImageOptions): Boolean {
    if (this === other) {
      return true
    }
    if (isPerfMediaRemountInstrumentationFix) {
      if (overlayRes != other.overlayRes ||
          !Objects.equal(overlayDrawable, other.overlayDrawable) ||
          !Objects.equal(backgroundDrawable, other.backgroundDrawable) ||
          !Objects.equal(actualImageColorFilter, other.actualImageColorFilter) ||
          _resizeToViewport != other._resizeToViewport ||
          _autoPlay != other._autoPlay ||
          _autoStop != other._autoStop ||
          !Objects.equal(customDrawableFactory, other.customDrawableFactory) ||
          isPerfMediaRemountInstrumentationFix != other.isPerfMediaRemountInstrumentationFix) {
        return false
      }
    } else {
      if (overlayRes != other.overlayRes ||
          !Objects.equal(overlayDrawable, other.overlayDrawable) ||
          !Objects.equal(backgroundDrawable, other.backgroundDrawable) ||
          !Objects.equal(actualImageColorFilter, other.actualImageColorFilter) ||
          _resizeToViewport != other._resizeToViewport ||
          !Objects.equal(customDrawableFactory, other.customDrawableFactory)) {
        return false
      }
    }
    return equalDecodedOptions(other)
  }

  override fun equals(otherObject: Any?): Boolean {
    if (this === otherObject) return true
    if (otherObject == null || javaClass != otherObject.javaClass) return false

    val other = otherObject as ImageOptions
    if (isPerfMediaRemountInstrumentationFix) {
      if (placeholderColor != other.placeholderColor ||
          placeholderRes != other.placeholderRes ||
          !Objects.equal(placeholderDrawable, other.placeholderDrawable) ||
          !Objects.equal(placeholderScaleType, other.placeholderScaleType) ||
          !Objects.equal(placeholderFocusPoint, other.placeholderFocusPoint) ||
          placeholderApplyRoundingOptions != other.placeholderApplyRoundingOptions ||
          errorColor != other.errorColor ||
          errorRes != other.errorRes ||
          !Objects.equal(errorScaleType, other.errorScaleType) ||
          !Objects.equal(errorFocusPoint, other.errorFocusPoint) ||
          errorApplyRoundingOptions != other.errorApplyRoundingOptions ||
          overlayRes != other.overlayRes ||
          !Objects.equal(overlayDrawable, other.overlayDrawable) ||
          !Objects.equal(errorDrawable, other.errorDrawable) ||
          progressRes != other.progressRes ||
          !Objects.equal(progressDrawable, other.progressDrawable) ||
          !Objects.equal(progressScaleType, other.progressScaleType) ||
          !Objects.equal(actualImageColorFilter, other.actualImageColorFilter) ||
          _resizeToViewport != other._resizeToViewport ||
          fadeDurationMs != other.fadeDurationMs ||
          _autoPlay != other._autoPlay ||
          _autoStop != other._autoStop ||
          !Objects.equal(customDrawableFactory, other.customDrawableFactory) ||
          !Objects.equal(errorDrawable, other.errorDrawable) ||
          isPerfMediaRemountInstrumentationFix != other.isPerfMediaRemountInstrumentationFix) {
        return false
      }
    } else {
      if (placeholderColor != other.placeholderColor ||
          placeholderRes != other.placeholderRes ||
          !Objects.equal(placeholderDrawable, other.placeholderDrawable) ||
          !Objects.equal(placeholderScaleType, other.placeholderScaleType) ||
          !Objects.equal(placeholderFocusPoint, other.placeholderFocusPoint) ||
          placeholderApplyRoundingOptions != other.placeholderApplyRoundingOptions ||
          errorColor != other.errorColor ||
          errorRes != other.errorRes ||
          !Objects.equal(errorScaleType, other.errorScaleType) ||
          !Objects.equal(errorFocusPoint, other.errorFocusPoint) ||
          errorApplyRoundingOptions != other.errorApplyRoundingOptions ||
          overlayRes != other.overlayRes ||
          !Objects.equal(overlayDrawable, other.overlayDrawable) ||
          !Objects.equal(errorDrawable, other.errorDrawable) ||
          progressRes != other.progressRes ||
          progressDrawable !== other.progressDrawable ||
          progressScaleType !== other.progressScaleType ||
          !Objects.equal(actualImageColorFilter, other.actualImageColorFilter) ||
          _resizeToViewport != other._resizeToViewport ||
          fadeDurationMs != other.fadeDurationMs ||
          _autoPlay != other._autoPlay ||
          _autoStop != other._autoStop ||
          !Objects.equal(customDrawableFactory, other.customDrawableFactory) ||
          errorDrawable !== other.errorDrawable) {
        return false
      }
    }
    return equalDecodedOptions(other)
  }

  override fun hashCode(): Int {
    var result = super.hashCode()
    result = 31 * result + (placeholderColor ?: 0)
    result = 31 * result + placeholderRes
    result = 31 * result + (placeholderDrawable?.hashCode() ?: 0)
    result = 31 * result + (placeholderScaleType?.hashCode() ?: 0)
    result = 31 * result + (placeholderFocusPoint?.hashCode() ?: 0)
    result = 31 * result + if (placeholderApplyRoundingOptions) 1 else 0
    result = 31 * result + (errorColor ?: 0)
    result = 31 * result + errorRes
    result = 31 * result + (errorScaleType?.hashCode() ?: 0)
    result = 31 * result + (errorFocusPoint?.hashCode() ?: 0)
    result = 31 * result + (errorDrawable?.hashCode() ?: 0)
    result = 31 * result + if (errorApplyRoundingOptions) 1 else 0
    result = 31 * result + overlayRes
    result = 31 * result + (overlayDrawable?.hashCode() ?: 0)
    result = 31 * result + (backgroundDrawable?.hashCode() ?: 0)
    result = 31 * result + (progressDrawable?.hashCode() ?: 0)
    result = 31 * result + (progressScaleType?.hashCode() ?: 0)
    result = 31 * result + (actualImageColorFilter?.hashCode() ?: 0)
    result = 31 * result + if (_resizeToViewport) 1 else 0
    result = 31 * result + fadeDurationMs
    result = 31 * result + if (_autoPlay) 1 else 0
    result = 31 * result + if (_autoStop) 1 else 0
    result = 31 * result + if (isPerfMediaRemountInstrumentationFix) 1 else 0
    result = 31 * result + progressRes
    result = 31 * result + (customDrawableFactory?.hashCode() ?: 0)
    return result
  }

  override fun toString(): String = "ImageOptions{${toStringHelper()}}"

  override fun toStringHelper(): Objects.ToStringHelper =
      super.toStringHelper()
          .add("placeholderColor", placeholderColor)
          .add("placeholderRes", placeholderRes)
          .add("placeholderDrawable", placeholderDrawable)
          .add("placeholderScaleType", placeholderScaleType)
          .add("placeholderFocusPoint", placeholderFocusPoint)
          .add("placeholderApplyRoundingOptions", placeholderApplyRoundingOptions)
          .add("progressRes", progressRes)
          .add("progressDrawable", progressDrawable)
          .add("progressScaleType", progressScaleType)
          .add("errorColor", errorColor)
          .add("errorRes", errorRes)
          .add("errorScaleType", errorScaleType)
          .add("errorFocusPoint", errorFocusPoint)
          .add("errorDrawable", errorDrawable)
          .add("errorApplyRoundingOptions", errorApplyRoundingOptions)
          .add("actualImageColorFilter", actualImageColorFilter)
          .add("overlayRes", overlayRes)
          .add("overlayDrawable", overlayDrawable)
          .add("backgroundDrawable", backgroundDrawable)
          .add("resizeToViewport", _resizeToViewport)
          .add("autoPlay", _autoPlay)
          .add("autoStop", _autoStop)
          .add("mPerfMediaRemountInstrumentationFix", isPerfMediaRemountInstrumentationFix)
          .add("fadeDurationMs", fadeDurationMs)
          .add("customDrawableFactory", customDrawableFactory)

  class Builder : DecodedImageOptions.Builder<Builder> {
    @ColorInt internal var _placeholderColor: Int? = null
    @DrawableRes internal var _placeholderRes = 0
    internal var _placeholderDrawable: Drawable? = null
    internal var _placeholderScaleType: ScalingUtils.ScaleType? = null
    internal var _placeholderFocusPoint: PointF? = null
    internal var _placeholderApplyRoundingOptions = false

    @DrawableRes internal var _progressRes = 0
    internal var _progressDrawable: Drawable? = null
    internal var _progressScaleType: ScalingUtils.ScaleType? = null

    @ColorInt internal var _errorColor: Int? = null
    @DrawableRes internal var _errorRes = 0
    internal var _errorScaleType: ScalingUtils.ScaleType? = null
    internal var _errorFocusPoint: PointF? = null
    internal var _errorDrawable: Drawable? = null
    internal var _errorApplyRoundingOptions = false
    internal var _actualImageColorFilter: ColorFilter? = null

    @DrawableRes internal var _overlayRes = 0
    internal var _overlayDrawable: Drawable? = null

    internal var _backgroundDrawable: Drawable? = null

    internal var _resizeToViewport = false
    internal var _autoPlay = false
    internal var _autoStop = true
    internal var _perfMediaRemountInstrumentationFix = false
    internal var _fadeDurationMs = 0
    internal var _customDrawableFactory: ImageOptionsDrawableFactory? = null

    internal constructor() : super()

    internal constructor(defaultOptions: ImageOptions) : super(defaultOptions) {
      _placeholderColor = defaultOptions.placeholderColor
      _placeholderRes = defaultOptions.placeholderRes
      _placeholderDrawable = defaultOptions.placeholderDrawable
      _placeholderScaleType = defaultOptions.placeholderScaleType
      _placeholderFocusPoint = defaultOptions.placeholderFocusPoint
      _placeholderApplyRoundingOptions = defaultOptions.placeholderApplyRoundingOptions
      _progressRes = defaultOptions.progressRes
      _progressDrawable = defaultOptions.progressDrawable
      _progressScaleType = defaultOptions.progressScaleType
      _errorColor = defaultOptions.errorColor
      _errorRes = defaultOptions.errorRes
      _errorScaleType = defaultOptions.errorScaleType
      _errorFocusPoint = defaultOptions.errorFocusPoint
      _errorDrawable = defaultOptions.errorDrawable
      _errorApplyRoundingOptions = defaultOptions.errorApplyRoundingOptions
      _actualImageColorFilter = defaultOptions.actualImageColorFilter
      _overlayRes = defaultOptions.overlayRes
      _overlayDrawable = defaultOptions.overlayDrawable
      _resizeToViewport = defaultOptions.shouldResizeToViewport()
      _autoPlay = defaultOptions.shouldAutoPlay()
      _autoStop = defaultOptions.shouldAutoStop()
      _fadeDurationMs = defaultOptions.fadeDurationMs
      _customDrawableFactory = defaultOptions.customDrawableFactory
    }

    fun placeholder(placeholder: Drawable?): Builder = modify {
      _placeholderDrawable = placeholder
      _placeholderColor = null
      _placeholderRes = 0
    }

    fun placeholder(
        placeholder: Drawable?,
        placeholderScaleType: ScalingUtils.ScaleType?
    ): Builder = modify {
      _placeholderDrawable = placeholder
      _placeholderScaleType = placeholderScaleType
      _placeholderColor = null
      _placeholderRes = 0
    }

    fun placeholderColor(@ColorInt placeholderColor: Int): Builder = modify {
      _placeholderColor = placeholderColor
      _placeholderRes = 0
      _placeholderDrawable = null
    }

    fun placeholderRes(@DrawableRes placeholderRes: Int): Builder = modify {
      _placeholderRes = placeholderRes
      _placeholderColor = null
      _placeholderDrawable = null
    }

    fun placeholderRes(
        @DrawableRes placeholderRes: Int,
        placeholderScaleType: ScalingUtils.ScaleType?
    ): Builder = modify {
      _placeholderRes = placeholderRes
      _placeholderScaleType = placeholderScaleType
      _placeholderColor = null
      _placeholderDrawable = null
    }

    fun placeholderScaleType(placeholderScaleType: ScalingUtils.ScaleType?): Builder = modify {
      _placeholderScaleType = placeholderScaleType
    }

    fun placeholderFocusPoint(placeholderFocusPoint: PointF?): Builder = modify {
      _placeholderFocusPoint = placeholderFocusPoint
    }

    fun placeholderApplyRoundingOptions(placeholderApplyRoundingOptions: Boolean): Builder =
        modify {
          _placeholderApplyRoundingOptions = placeholderApplyRoundingOptions
        }

    fun errorColor(@ColorInt errorColor: Int): Builder = modify {
      _errorColor = errorColor
      _errorRes = 0
      _errorDrawable = null
    }

    fun errorRes(@DrawableRes errorRes: Int): Builder = modify {
      _errorColor = null
      _errorRes = errorRes
      _errorDrawable = null
    }

    fun errorScaleType(errorScaleType: ScalingUtils.ScaleType?): Builder = modify {
      _errorScaleType = errorScaleType
    }

    fun errorFocusPoint(errorFocusPoint: PointF?): Builder = modify {
      _errorFocusPoint = errorFocusPoint
    }

    fun errorDrawable(errorDrawable: Drawable?): Builder = modify {
      _errorColor = null
      _errorRes = 0
      _errorDrawable = errorDrawable
    }

    fun errorApplyRoundingOptions(errorApplyRoundingOptions: Boolean): Builder = modify {
      _errorApplyRoundingOptions = errorApplyRoundingOptions
    }

    fun progress(progress: Drawable?): Builder = modify { _progressDrawable = progress }

    fun progress(progress: Drawable?, progressScaleType: ScalingUtils.ScaleType?): Builder =
        modify {
          _progressDrawable = progress
          _progressScaleType = progressScaleType
        }

    fun progressRes(@DrawableRes progressRes: Int): Builder = modify { _progressRes = progressRes }

    fun progressRes(
        @DrawableRes progressRes: Int,
        progressScaleType: ScalingUtils.ScaleType?
    ): Builder = modify {
      _progressRes = progressRes
      _progressScaleType = progressScaleType
    }

    fun progressScaleType(progressScaleType: ScalingUtils.ScaleType?): Builder = modify {
      _progressScaleType = progressScaleType
    }

    fun overlayRes(@DrawableRes overlayRes: Int): Builder = modify {
      _overlayRes = overlayRes
      _overlayDrawable = null
    }

    fun overlay(overlayDrawable: Drawable?): Builder = modify {
      _overlayDrawable = overlayDrawable
      _overlayRes = 0
    }

    fun background(drawable: Drawable?): Builder = modify { _backgroundDrawable = drawable }

    fun colorFilter(colorFilter: ColorFilter?): Builder = modify {
      _actualImageColorFilter = colorFilter
    }

    /**
     * Turns on autoplay for animated images
     *
     * @param autoPlay whether to enable autoplay for animated images
     */
    fun autoPlay(autoPlay: Boolean): Builder = modify { _autoPlay = autoPlay }

    /**
     * Turns on autoStop for animated images
     *
     * @param autoStop whether to enable autoStop for animated images when it scrolls off screen
     */
    fun autoStop(autoStop: Boolean): Builder = modify { _autoStop = autoStop }

    fun perfMediaRemountInstrumentationFix(fix: Boolean): Builder = modify {
      _perfMediaRemountInstrumentationFix = fix
    }

    /**
     * Will resize bitmap to viewport dimensions. Works only if
     * [com.facebook.imagepipeline.common.ResizeOptions] are not set. Works only with Vito for now.
     * Please do not use unless you messaged me for details: @defhlt
     *
     * @param resizeToViewport whether to enable this optimization
     */
    fun resizeToViewport(resizeToViewport: Boolean): Builder = modify {
      _resizeToViewport = resizeToViewport
    }

    /**
     * Sets the fade duration.
     *
     * @param fadeInDurationMs
     */
    fun fadeDurationMs(fadeInDurationMs: Int): Builder = modify {
      _fadeDurationMs = fadeInDurationMs
    }

    /**
     * Set a custom drawable factory to be used to create the actual image drawable.
     *
     * @param drawableFactory the factory to use
     */
    fun customDrawableFactory(drawableFactory: ImageOptionsDrawableFactory?): Builder = modify {
      _customDrawableFactory = drawableFactory
    }

    override fun build(): ImageOptions = ImageOptions(this)

    private inline fun modify(block: Builder.() -> Unit): Builder {
      block()
      return this
    }
  }

  companion object {
    private var defaultImageOptions =
        Builder()
            .placeholderScaleType(ScalingUtils.ScaleType.CENTER_INSIDE)
            .progressScaleType(ScalingUtils.ScaleType.CENTER_INSIDE)
            .errorScaleType(ScalingUtils.ScaleType.CENTER_INSIDE)
            .priority(Priority.HIGH)
            .build()

    @JvmStatic fun defaults(): ImageOptions = defaultImageOptions

    @JvmStatic
    fun setDefaults(imageOptions: ImageOptions) {
      defaultImageOptions = imageOptions
    }

    @JvmStatic fun extend(imageOptions: ImageOptions): Builder = Builder(imageOptions)

    @JvmStatic fun create(): Builder = extend(defaults())
  }
}
