/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.systrace

import android.os.Build
import android.os.Trace
import com.facebook.imagepipeline.systrace.FrescoSystrace.ArgsBuilder
import com.facebook.imagepipeline.systrace.FrescoSystrace.Systrace
import com.facebook.imagepipelinebase.BuildConfig

class DefaultFrescoSystrace : Systrace {

  override fun beginSection(name: String) {
    if (isTracing()) {
      Trace.beginSection(name)
    }
  }

  override fun beginSectionWithArgs(name: String): ArgsBuilder =
      if (isTracing()) {
        DefaultArgsBuilder(name)
      } else {
        FrescoSystrace.NO_OP_ARGS_BUILDER
      }

  override fun endSection() {
    if (isTracing()) {
      Trace.endSection()
    }
  }

  override fun isTracing(): Boolean =
      BuildConfig.DEBUG && Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2

  /**
   * Handles adding args to a systrace section by naively appending them to the section name. This
   * functionality has a more intelligent implementation when using a tracer that writes directly to
   * ftrace instead of using Android's Trace class.
   */
  private class DefaultArgsBuilder(name: String) : ArgsBuilder {

    private val stringBuilder: StringBuilder = StringBuilder(name)

    override fun flush() {
      // 127 is the max name length according to
      // https://developer.android.com/reference/android/os/Trace.html
      if (stringBuilder.length > 127) {
        stringBuilder.setLength(127)
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        Trace.beginSection(stringBuilder.toString())
      }
    }

    override fun arg(key: String, value: Any) = apply { appendArgument(key, value) }

    override fun arg(key: String, value: Int) = apply { appendArgument(key, value) }

    override fun arg(key: String, value: Long) = apply { appendArgument(key, value) }

    override fun arg(key: String, value: Double) = apply { appendArgument(key, value) }

    private fun appendArgument(key: String, value: Any) =
        stringBuilder.append(';').append("$key=$value")
  }
}
