/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.draweesupport

import android.content.Context
import android.content.res.TypedArray
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.AttributeSet
import android.view.View
import com.facebook.drawee.R
import com.facebook.drawee.drawable.AutoRotateDrawable
import com.facebook.drawee.drawable.ScalingUtils.ScaleType
import com.facebook.drawee.generic.GenericDraweeHierarchyInflater
import com.facebook.fresco.vito.options.BorderOptions
import com.facebook.fresco.vito.options.ImageOptions
import com.facebook.fresco.vito.options.RoundingOptions

object VitoViewInflater {

  @JvmStatic
  fun inflateImageOptionsBuilder(
      context: Context,
      attributeSet: AttributeSet?
  ): ImageOptions.Builder {
    if (attributeSet == null) {
      return ImageOptions.create()
    }
    val attrs = context.obtainStyledAttributes(attributeSet, R.styleable.GenericDraweeHierarchy)
    try {
      return ImageOptions.create().apply {
        val indexCount = attrs.indexCount
        for (i in 0 until indexCount) {
          when (val attr = attrs.getIndex(i)) {
            R.styleable.GenericDraweeHierarchy_actualImageScaleType ->
                scale(getScaleType(attrs, attr))
            R.styleable.GenericDraweeHierarchy_placeholderImage ->
                placeholder(GenericDraweeHierarchyInflater.getDrawable(context, attrs, attr))
            R.styleable.GenericDraweeHierarchy_progressBarImage -> {
              val progressBarDrawable = getDrawable(context, attrs, attr)
              if (progressBarDrawable != null) {
                // wrap progress bar if auto-rotating requested
                val progressBarAutoRotateInterval =
                    attrs.getInteger(
                        R.styleable.GenericDraweeHierarchy_progressBarAutoRotateInterval, 0)
                progress(
                    if (progressBarAutoRotateInterval > 0) {
                      AutoRotateDrawable(progressBarDrawable, progressBarAutoRotateInterval)
                    } else {
                      progressBarDrawable
                    })
              }
            }
            R.styleable.GenericDraweeHierarchy_fadeDuration -> fadeDurationMs(attrs.getInt(attr, 0))
            R.styleable.GenericDraweeHierarchy_placeholderImageScaleType ->
                placeholderScaleType(getScaleType(attrs, attr))
            R.styleable.GenericDraweeHierarchy_failureImage ->
                errorDrawable(getDrawable(context, attrs, attr))
            R.styleable.GenericDraweeHierarchy_failureImageScaleType ->
                errorScaleType(getScaleType(attrs, attr))
            R.styleable.GenericDraweeHierarchy_progressBarImageScaleType ->
                progressScaleType(getScaleType(attrs, attr))
            R.styleable.GenericDraweeHierarchy_overlayImage ->
                overlay(getDrawable(context, attrs, attr))
            R.styleable.GenericDraweeHierarchy_roundAsCircle ->
                round(RoundingOptions.asCircle(antiAliasing = true, forceRoundAtDecode = true))
            R.styleable.GenericDraweeHierarchy_roundedCornerRadius -> {
              // Set rounded corner radii if requested
              val roundedCornerRadius = attrs.getDimensionPixelSize(attr, 0)
              if (roundedCornerRadius > 0) {
                val radius = roundedCornerRadius.toFloat()
                var roundTopLeft =
                    attrs.getBoolean(R.styleable.GenericDraweeHierarchy_roundTopLeft, true)
                var roundTopRight =
                    attrs.getBoolean(R.styleable.GenericDraweeHierarchy_roundTopRight, true)
                var roundBottomLeft =
                    attrs.getBoolean(R.styleable.GenericDraweeHierarchy_roundBottomLeft, true)
                var roundBottomRight =
                    attrs.getBoolean(R.styleable.GenericDraweeHierarchy_roundBottomRight, true)
                val roundTopStart =
                    attrs.getBoolean(R.styleable.GenericDraweeHierarchy_roundTopStart, true)
                val roundTopEnd =
                    attrs.getBoolean(R.styleable.GenericDraweeHierarchy_roundTopEnd, true)
                val roundBottomStart =
                    attrs.getBoolean(R.styleable.GenericDraweeHierarchy_roundBottomStart, true)
                val roundBottomEnd =
                    attrs.getBoolean(R.styleable.GenericDraweeHierarchy_roundBottomEnd, true)

                if (Build.VERSION.SDK_INT >= 17 &&
                    context.resources.configuration.layoutDirection == View.LAYOUT_DIRECTION_RTL) {
                  roundTopLeft = roundTopLeft && roundTopEnd
                  roundTopRight = roundTopRight && roundTopStart
                  roundBottomRight = roundBottomRight && roundBottomStart
                  roundBottomLeft = roundBottomLeft && roundBottomEnd
                } else {
                  roundTopLeft = roundTopLeft && roundTopStart
                  roundTopRight = roundTopRight && roundTopEnd
                  roundBottomRight = roundBottomRight && roundBottomEnd
                  roundBottomLeft = roundBottomLeft && roundBottomStart
                }
                round(
                    RoundingOptions.forCornerRadii(
                        if (roundTopLeft) radius else 0f,
                        if (roundTopRight) radius else 0f,
                        if (roundBottomRight) radius else 0f,
                        if (roundBottomLeft) radius else 0f))
              }
            }
            R.styleable.GenericDraweeHierarchy_roundingBorderWidth -> {
              val borderWidth = attrs.getDimensionPixelSize(attr, 0).toFloat()
              if (borderWidth != 0f) {
                borders(
                    BorderOptions.create(
                        attrs.getColor(R.styleable.GenericDraweeHierarchy_roundingBorderColor, 0),
                        borderWidth,
                        attrs
                            .getDimensionPixelSize(
                                R.styleable.GenericDraweeHierarchy_roundingBorderPadding, 0)
                            .toFloat()))
              }
            }
            R.styleable.GenericDraweeHierarchy_retryImage,
            R.styleable.GenericDraweeHierarchy_retryImageScaleType,
            R.styleable.GenericDraweeHierarchy_roundWithOverlayColor,
            R.styleable.GenericDraweeHierarchy_pressedStateOverlayImage,
            R.styleable.GenericDraweeHierarchy_backgroundImage ->
                throw UnsupportedOperationException("Not supported for Vito")
          }
        }
      }
    } finally {
      attrs.recycle()
    }
  }

  private fun getScaleType(attrs: TypedArray, attrId: Int): ScaleType? =
      GenericDraweeHierarchyInflater.getScaleTypeFromXml(attrs, attrId)

  private fun getDrawable(context: Context, attrs: TypedArray, attrId: Int): Drawable? =
      GenericDraweeHierarchyInflater.getDrawable(context, attrs, attrId)
}
