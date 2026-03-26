/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

@file:SuppressLint("ColorConstantUsageIssue")

package com.facebook.fresco.vito.core.impl.debug

import android.annotation.SuppressLint
import android.graphics.Color
import com.facebook.common.logging.FLog

/**
 * Logs per-image I:D ratio and wasted memory data to logcat for programmatic capture via `adb
 * logcat -s ImageFitRatio`.
 *
 * Active whenever the debug overlay is enabled. Each unique image ID is logged at most once
 * (deduped via LRU eviction when the capacity limit is reached).
 *
 * Output format (tagged key-value):
 * ```
 * D/ImageFitRatio: id=123 uri=https://... imgW=1024 imgH=768 drawW=512 drawH=384 ratio=2.00
 *     ratioStatus=RED waste=1.5MiB wasteStatus=RED origin=network
 * ```
 */
object DebugLogcatReporter {

  private const val TAG = "ImageFitRatio"
  private const val MAX_TRACKED_IDS = 2000

  private val reportedIds: LinkedHashMap<Long, Boolean> =
      object : LinkedHashMap<Long, Boolean>(MAX_TRACKED_IDS, 0.75f, true) {
        override fun removeEldestEntry(eldest: Map.Entry<Long, Boolean>?): Boolean =
            size > MAX_TRACKED_IDS
      }
  private val lock = Any()

  /**
   * Logs image sizing data for the given image if it has not already been reported.
   *
   * Skips reporting (without marking the ID as seen) if any dimension is <= 0, so incomplete data
   * will be retried on the next update.
   */
  fun maybeReport(
      imageId: Long,
      uri: String?,
      imgW: Int,
      imgH: Int,
      drawW: Int,
      drawH: Int,
      origin: String?,
      config: ImageFitRatioConfig = ImageFitRatioConfig(),
  ) {
    if (imgW <= 0 || imgH <= 0 || drawW <= 0 || drawH <= 0) {
      return
    }

    val alreadyReported =
        synchronized(lock) {
          if (reportedIds.containsKey(imageId)) {
            true
          } else {
            reportedIds[imageId] = true
            false
          }
        }
    if (alreadyReported) {
      return
    }

    val (ratioText, ratioColor) = computeImageFitRatioAndColor(imgW, imgH, drawW, drawH, config)
    val (wasteText, wasteColor) =
        computeWastedMemoryAndColor(imgW, imgH, drawW, drawH, config = config)

    FLog.d(
        TAG,
        "id=%d uri=%s imgW=%d imgH=%d drawW=%d drawH=%d ratio=%s ratioStatus=%s waste=%s wasteStatus=%s origin=%s",
        imageId,
        uri ?: "unknown",
        imgW,
        imgH,
        drawW,
        drawH,
        ratioText,
        colorToStatus(ratioColor),
        wasteText,
        colorToStatus(wasteColor),
        origin ?: "unknown",
    )
  }

  private fun colorToStatus(color: Int): String =
      when (color) {
        Color.GREEN -> "GREEN"
        Color.YELLOW -> "YELLOW"
        Color.RED -> "RED"
        else -> "GRAY"
      }
}
