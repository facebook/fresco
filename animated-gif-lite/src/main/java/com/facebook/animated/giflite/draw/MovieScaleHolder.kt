/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.animated.giflite.draw

internal class MovieScaleHolder(private val movieWidth: Int, private val movieHeight: Int) {

  private var viewPortWidth = 0
  private var viewPortHeight = 0

  @get:Synchronized
  var scale = 1f
    private set

  @get:Synchronized
  var left = 0f
    private set

  @get:Synchronized
  var top = 0f
    private set

  @Synchronized
  fun updateViewPort(viewPortWidth: Int, viewPortHeight: Int) {
    if (this.viewPortWidth == viewPortWidth && this.viewPortHeight == viewPortHeight) {
      return
    }
    this.viewPortWidth = viewPortWidth
    this.viewPortHeight = viewPortHeight
    determineScaleAndPosition()
  }

  @Synchronized
  private fun determineScaleAndPosition() {
    val inputRatio = (movieWidth / movieHeight).toFloat()
    val outputRatio = (viewPortWidth / viewPortHeight).toFloat()
    var width = viewPortWidth
    var height = viewPortHeight
    if (outputRatio > inputRatio) {
      // Not enough width to fill the output. (Black bars on left and right.)
      width = (viewPortHeight * inputRatio).toInt()
    } else if (outputRatio < inputRatio) {
      // Not enough height to fill the output. (Black bars on top and bottom.)
      height = (viewPortWidth / inputRatio).toInt()
    }
    if (viewPortWidth > movieWidth) {
      scale = movieWidth / viewPortWidth.toFloat()
    } else if (movieWidth > viewPortWidth) {
      scale = viewPortWidth / movieWidth.toFloat()
    } else {
      scale = 1f
    }
    left = (viewPortWidth - width) / 2f / scale
    top = (viewPortHeight - height) / 2f / scale
  }
}
