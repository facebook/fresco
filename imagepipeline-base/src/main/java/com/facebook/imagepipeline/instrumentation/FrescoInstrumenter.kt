/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.instrumentation

import com.facebook.imagepipeline.instrumentation.FrescoInstrumenter.Instrumenter
import com.facebook.infer.annotation.PropagatesNullable

/**
 * Utility class that provides hooks to capture execution of different units of work. Client code
 * can specify a custom [Instrumenter] that will receive ad-hoc updates when work that has to be
 * executed across threads gets moved around.
 */
object FrescoInstrumenter {

  @Volatile private var instance: Instrumenter? = null

  /**
   * Allows to provide a [Instrumenter] that will receive units of work updates.
   *
   * @param instrumenter to be notified or null to reset.
   */
  @JvmStatic
  fun provide(instrumenter: Instrumenter?) {
    instance = instrumenter
  }

  @get:JvmStatic
  val isTracing: Boolean
    get() {
      val instrumenter = instance ?: return false
      return instrumenter.isTracing
    }

  @JvmStatic
  fun onBeforeSubmitWork(tag: String?): Any? {
    val instrumenter = instance
    return if (instrumenter == null || tag == null) {
      null
    } else {
      instrumenter.onBeforeSubmitWork(tag)
    }
  }

  @JvmStatic
  fun onBeginWork(token: Any?, tag: String?): Any? {
    val instrumenter = instance
    return if (instrumenter == null || token == null) {
      null
    } else {
      instrumenter.onBeginWork(token, tag)
    }
  }

  @JvmStatic
  fun onEndWork(token: Any?) {
    val instrumenter = instance
    if (instrumenter == null || token == null) {
      return
    }
    instrumenter.onEndWork(token)
  }

  @JvmStatic
  fun markFailure(token: Any?, th: Throwable) {
    val instrumenter = instance
    if (instrumenter == null || token == null) {
      return
    }
    instrumenter.markFailure(token, th)
  }

  @JvmStatic
  fun decorateRunnable(@PropagatesNullable runnable: Runnable?, tag: String?): Runnable? {
    val instrumenter = instance
    if (instrumenter == null || runnable == null) {
      return runnable
    }
    val nonNullTag = tag ?: ""
    return instrumenter.decorateRunnable(runnable, nonNullTag)
  }

  /** Allows to capture unit of works across different threads. */
  interface Instrumenter {
    /**
     * Allows to know in advance if the custom instrumenter desires to receive continuation updates.
     * This can be used to avoid un-necessary work if the subscriber will not use the information
     * provided.
     *
     * @return true to specify interest in handling the updates, false otherwise.
     */
    val isTracing: Boolean

    /**
     * Called before scheduling a new unit work.
     *
     * @param tag name.
     * @return a token object that allows to track work units.
     */
    fun onBeforeSubmitWork(tag: String): Any?

    /**
     * Captures the beginning of the continuation for stolen work.
     *
     * @param token returned by [Instrumenter#onBeforeSubmitWork].
     * @param tag optional name.
     */
    fun onBeginWork(token: Any, tag: String?): Any?

    /**
     * Captures the end of the continuation for stolen work.
     *
     * @param token returned by [Instrumenter#onBeginWork].
     */
    fun onEndWork(token: Any)

    /**
     * Reports a failure while executing work.
     *
     * <note>[Instrumenter#onEndWork(Object)] still needs to be invoked.
     *
     * @param token returned by [Instrumenter#onBeginWork(Object, String)].
     * @param th containing the failure. </note>
     */
    fun markFailure(token: Any, th: Throwable)

    /**
     * Called when a unit of work is about to be scheduled.
     *
     * @param runnable that will be executed.
     * @param tag name.
     * @return the wrapped input runnable or just the input one.
     */
    fun decorateRunnable(runnable: Runnable, tag: String): Runnable?
  }
}
