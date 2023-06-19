/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.systrace

/**
 * This is intended as a hook into `android.os.Trace`, but allows you to provide your own
 * functionality. Use it as
 *
 * `FrescoSystrace.beginSection("tag"); ... FrescoSystrace.endSection(); ` As a default, it simply
 * calls `android.os.Trace` (see [DefaultFrescoSystrace]). You may supply your own with
 * [FrescoSystrace.provide].
 */
object FrescoSystrace {

  @JvmField
  /** Convenience implementation of ArgsBuilder to use when we aren't tracing. */
  val NO_OP_ARGS_BUILDER: ArgsBuilder = NoOpArgsBuilder()

  @JvmStatic
  fun provide(instance: Systrace?) {
    _instance = instance
  }

  @JvmStatic fun beginSection(name: String) = instance.beginSection(name)

  @JvmStatic
  fun beginSectionWithArgs(name: String): ArgsBuilder = instance.beginSectionWithArgs(name)

  @JvmStatic
  fun endSection() {
    instance.endSection()
  }

  inline fun <T> traceSection(name: String, block: () -> T): T {
    if (!isTracing()) {
      return block()
    }

    beginSection(name)
    try {
      return block()
    } finally {
      endSection()
    }
  }

  @JvmStatic fun isTracing(): Boolean = instance.isTracing()

  private var _instance: Systrace? = null
  private val instance: Systrace
    get() {
      return _instance
          ?: synchronized(FrescoSystrace::class.java) {
            val systrace = DefaultFrescoSystrace()
            _instance = systrace
            systrace
          }
    }

  interface Systrace {
    fun beginSection(name: String)

    fun beginSectionWithArgs(name: String): ArgsBuilder

    fun endSection()

    fun isTracing(): Boolean
  }

  /** Object that accumulates arguments. */
  interface ArgsBuilder {
    /**
     * Write the full message to the Systrace buffer.
     *
     * You must call this to log the trace message.
     */
    fun flush()

    /**
     * Logs an argument whose value is any object. It will be stringified with [ ][String.valueOf].
     */
    fun arg(key: String, value: Any): ArgsBuilder

    /** Logs an argument whose value is an int. It will be stringified with [ ][String.valueOf]. */
    fun arg(key: String, value: Int): ArgsBuilder

    /** Logs an argument whose value is a long. It will be stringified with [ ][String.valueOf]. */
    fun arg(key: String, value: Long): ArgsBuilder

    /**
     * Logs an argument whose value is a double. It will be stringified with [ ][String.valueOf].
     */
    fun arg(key: String, value: Double): ArgsBuilder
  }

  private class NoOpArgsBuilder : ArgsBuilder {
    override fun flush() = Unit

    override fun arg(key: String, value: Any): ArgsBuilder = this

    override fun arg(key: String, value: Int): ArgsBuilder = this

    override fun arg(key: String, value: Long): ArgsBuilder = this

    override fun arg(key: String, value: Double): ArgsBuilder = this
  }
}
