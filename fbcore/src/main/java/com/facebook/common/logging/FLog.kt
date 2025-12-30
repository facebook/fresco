/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.common.logging

import android.util.Log

/** Logging wrapper with format style support. */
object FLog {
  @JvmField val VERBOSE: Int = Log.VERBOSE

  @JvmField val DEBUG: Int = Log.DEBUG

  @JvmField val INFO: Int = Log.INFO

  @JvmField val WARN: Int = Log.WARN

  @JvmField val ERROR: Int = Log.ERROR

  @JvmField val ASSERT: Int = Log.ASSERT

  private var handler: LoggingDelegate = FLogDefaultLoggingDelegate.getInstance()

  /**
   * Sets the logging delegate that overrides the default delegate.
   *
   * @param delegate the delegate to use
   */
  @JvmStatic
  fun setLoggingDelegate(delegate: LoggingDelegate) {
    requireNotNull(delegate)
    handler = delegate
  }

  @JvmStatic
  fun isLoggable(level: Int): Boolean {
    return handler.isLoggable(level)
  }

  @JvmStatic
  var minimumLoggingLevel: Int
    get() = handler.getMinimumLoggingLevel()
    set(level) {
      handler.setMinimumLoggingLevel(level)
    }

  @JvmStatic
  fun v(tag: String?, msg: String?) {
    if (handler.isLoggable(VERBOSE)) {
      handler.v(tag!!, msg!!)
    }
  }

  @JvmStatic
  fun v(tag: String?, msg: String?, arg1: Any?) {
    if (handler.isLoggable(VERBOSE)) {
      handler.v(tag!!, formatString(msg, arg1)!!)
    }
  }

  @JvmStatic
  fun v(tag: String?, msg: String?, arg1: Any?, arg2: Any?) {
    if (handler.isLoggable(VERBOSE)) {
      handler.v(tag!!, formatString(msg, arg1, arg2)!!)
    }
  }

  @JvmStatic
  fun v(tag: String?, msg: String?, arg1: Any?, arg2: Any?, arg3: Any?) {
    if (handler.isLoggable(VERBOSE)) {
      handler.v(tag!!, formatString(msg, arg1, arg2, arg3)!!)
    }
  }

  @JvmStatic
  fun v(tag: String?, msg: String?, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?) {
    if (handler.isLoggable(VERBOSE)) {
      handler.v(tag!!, formatString(msg, arg1, arg2, arg3, arg4)!!)
    }
  }

  @JvmStatic
  fun v(cls: Class<*>?, msg: String?) {
    if (handler.isLoggable(VERBOSE)) {
      handler.v(FLog.getTag(cls!!)!!, msg!!)
    }
  }

  @JvmStatic
  fun v(cls: Class<*>?, msg: String?, arg1: Any?) {
    if (handler.isLoggable(VERBOSE)) {
      handler.v(FLog.getTag(cls!!)!!, formatString(msg, arg1)!!)
    }
  }

  @JvmStatic
  fun v(cls: Class<*>?, msg: String?, arg1: Any?, arg2: Any?) {
    if (handler.isLoggable(VERBOSE)) {
      handler.v(FLog.getTag(cls!!)!!, formatString(msg, arg1, arg2)!!)
    }
  }

  @JvmStatic
  fun v(cls: Class<*>?, msg: String?, arg1: Any?, arg2: Any?, arg3: Any?) {
    if (isLoggable(VERBOSE)) {
      FLog.v(cls, formatString(msg, arg1, arg2, arg3))
    }
  }

  @JvmStatic
  fun v(cls: Class<*>?, msg: String?, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?) {
    if (handler.isLoggable(VERBOSE)) {
      handler.v(FLog.getTag(cls!!)!!, formatString(msg, arg1, arg2, arg3, arg4)!!)
    }
  }

  @JvmStatic
  fun v(tag: String?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(VERBOSE)) {
      handler.v(tag!!, formatString(msg, *args)!!)
    }
  }

  @JvmStatic
  fun v(tag: String?, tr: Throwable?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(VERBOSE)) {
      handler.v(tag!!, formatString(msg, *args)!!, tr!!)
    }
  }

  @JvmStatic
  fun v(cls: Class<*>?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(VERBOSE)) {
      handler.v(FLog.getTag(cls!!)!!, formatString(msg, *args)!!)
    }
  }

  @JvmStatic
  fun v(cls: Class<*>?, tr: Throwable?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(VERBOSE)) {
      handler.v(FLog.getTag(cls!!)!!, formatString(msg, *args)!!, tr!!)
    }
  }

  @JvmStatic
  fun v(tag: String?, msg: String?, tr: Throwable?) {
    if (handler.isLoggable(VERBOSE)) {
      handler.v(tag!!, msg!!, tr!!)
    }
  }

  @JvmStatic
  fun v(cls: Class<*>?, msg: String?, tr: Throwable?) {
    if (handler.isLoggable(VERBOSE)) {
      handler.v(FLog.getTag(cls!!)!!, msg!!, tr!!)
    }
  }

  @JvmStatic
  fun d(tag: String?, msg: String?) {
    if (handler.isLoggable(DEBUG)) {
      handler.d(tag!!, msg!!)
    }
  }

  @JvmStatic
  fun d(tag: String?, msg: String?, arg1: Any?) {
    if (handler.isLoggable(DEBUG)) {
      handler.d(tag!!, formatString(msg, arg1)!!)
    }
  }

  @JvmStatic
  fun d(tag: String?, msg: String?, arg1: Any?, arg2: Any?) {
    if (handler.isLoggable(DEBUG)) {
      handler.d(tag!!, formatString(msg, arg1, arg2)!!)
    }
  }

  @JvmStatic
  fun d(tag: String?, msg: String?, arg1: Any?, arg2: Any?, arg3: Any?) {
    if (handler.isLoggable(DEBUG)) {
      handler.d(tag!!, formatString(msg, arg1, arg2, arg3)!!)
    }
  }

  @JvmStatic
  fun d(tag: String?, msg: String?, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?) {
    if (handler.isLoggable(DEBUG)) {
      handler.d(tag!!, formatString(msg, arg1, arg2, arg3, arg4)!!)
    }
  }

  @JvmStatic
  fun d(cls: Class<*>?, msg: String?) {
    if (handler.isLoggable(DEBUG)) {
      handler.d(FLog.getTag(cls!!)!!, msg!!)
    }
  }

  @JvmStatic
  fun d(cls: Class<*>?, msg: String?, arg1: Any?) {
    if (handler.isLoggable(DEBUG)) {
      handler.d(FLog.getTag(cls!!)!!, formatString(msg, arg1)!!)
    }
  }

  @JvmStatic
  fun d(cls: Class<*>?, msg: String?, arg1: Any?, arg2: Any?) {
    if (handler.isLoggable(DEBUG)) {
      handler.d(FLog.getTag(cls!!)!!, formatString(msg, arg1, arg2)!!)
    }
  }

  @JvmStatic
  fun d(cls: Class<*>?, msg: String?, arg1: Any?, arg2: Any?, arg3: Any?) {
    if (handler.isLoggable(DEBUG)) {
      handler.d(FLog.getTag(cls!!)!!, formatString(msg, arg1, arg2, arg3)!!)
    }
  }

  @JvmStatic
  fun d(cls: Class<*>?, msg: String?, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?) {
    if (handler.isLoggable(DEBUG)) {
      handler.d(FLog.getTag(cls!!)!!, formatString(msg, arg1, arg2, arg3, arg4)!!)
    }
  }

  @JvmStatic
  fun d(tag: String?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(DEBUG)) {
      FLog.d(tag, formatString(msg, *args))
    }
  }

  @JvmStatic
  fun d(tag: String?, tr: Throwable?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(DEBUG)) {
      FLog.d(tag, formatString(msg, *args), tr)
    }
  }

  @JvmStatic
  fun d(cls: Class<*>?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(DEBUG)) {
      handler.d(FLog.getTag(cls!!)!!, formatString(msg, *args)!!)
    }
  }

  @JvmStatic
  fun d(cls: Class<*>?, tr: Throwable?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(DEBUG)) {
      handler.d(FLog.getTag(cls!!)!!, formatString(msg, *args)!!, tr!!)
    }
  }

  @JvmStatic
  fun d(tag: String?, msg: String?, tr: Throwable?) {
    if (handler.isLoggable(DEBUG)) {
      handler.d(tag!!, msg!!, tr!!)
    }
  }

  @JvmStatic
  fun d(cls: Class<*>?, msg: String?, tr: Throwable?) {
    if (handler.isLoggable(DEBUG)) {
      handler.d(FLog.getTag(cls!!)!!, msg!!, tr!!)
    }
  }

  @JvmStatic
  fun i(tag: String?, msg: String?) {
    if (handler.isLoggable(INFO)) {
      handler.i(tag!!, msg!!)
    }
  }

  @JvmStatic
  fun i(tag: String?, msg: String?, arg1: Any?) {
    if (handler.isLoggable(INFO)) {
      handler.i(tag!!, formatString(msg, arg1)!!)
    }
  }

  @JvmStatic
  fun i(tag: String?, msg: String?, arg1: Any?, arg2: Any?) {
    if (handler.isLoggable(INFO)) {
      handler.i(tag!!, formatString(msg, arg1, arg2)!!)
    }
  }

  @JvmStatic
  fun i(tag: String?, msg: String?, arg1: Any?, arg2: Any?, arg3: Any?) {
    if (handler.isLoggable(INFO)) {
      handler.i(tag!!, formatString(msg, arg1, arg2, arg3)!!)
    }
  }

  @JvmStatic
  fun i(tag: String?, msg: String?, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?) {
    if (handler.isLoggable(INFO)) {
      handler.i(tag!!, formatString(msg, arg1, arg2, arg3, arg4)!!)
    }
  }

  @JvmStatic
  fun i(cls: Class<*>?, msg: String?) {
    if (handler.isLoggable(INFO)) {
      handler.i(FLog.getTag(cls!!)!!, msg!!)
    }
  }

  @JvmStatic
  fun i(cls: Class<*>?, msg: String?, arg1: Any?) {
    if (handler.isLoggable(INFO)) {
      handler.i(FLog.getTag(cls!!)!!, formatString(msg, arg1)!!)
    }
  }

  @JvmStatic
  fun i(cls: Class<*>?, msg: String?, arg1: Any?, arg2: Any?) {
    if (handler.isLoggable(INFO)) {
      handler.i(FLog.getTag(cls!!)!!, formatString(msg, arg1, arg2)!!)
    }
  }

  @JvmStatic
  fun i(cls: Class<*>?, msg: String?, arg1: Any?, arg2: Any?, arg3: Any?) {
    if (handler.isLoggable(INFO)) {
      handler.i(FLog.getTag(cls!!)!!, formatString(msg, arg1, arg2, arg3)!!)
    }
  }

  @JvmStatic
  fun i(cls: Class<*>?, msg: String?, arg1: Any?, arg2: Any?, arg3: Any?, arg4: Any?) {
    if (handler.isLoggable(INFO)) {
      handler.i(FLog.getTag(cls!!)!!, formatString(msg, arg1, arg2, arg3, arg4)!!)
    }
  }

  @JvmStatic
  fun i(tag: String?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(INFO)) {
      handler.i(tag!!, formatString(msg, *args)!!)
    }
  }

  @JvmStatic
  fun i(tag: String?, tr: Throwable?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(INFO)) {
      handler.i(tag!!, formatString(msg, *args)!!, tr!!)
    }
  }

  @JvmStatic
  fun i(cls: Class<*>?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(INFO)) {
      handler.i(FLog.getTag(cls!!)!!, formatString(msg, *args)!!)
    }
  }

  @JvmStatic
  fun i(cls: Class<*>?, tr: Throwable?, msg: String?, vararg args: Any?) {
    if (isLoggable(INFO)) {
      handler.i(FLog.getTag(cls!!)!!, formatString(msg, *args)!!, tr!!)
    }
  }

  @JvmStatic
  fun i(tag: String?, msg: String?, tr: Throwable?) {
    if (handler.isLoggable(INFO)) {
      handler.i(tag!!, msg!!, tr!!)
    }
  }

  @JvmStatic
  fun i(cls: Class<*>?, msg: String?, tr: Throwable?) {
    if (handler.isLoggable(INFO)) {
      handler.i(FLog.getTag(cls!!)!!, msg!!, tr!!)
    }
  }

  @JvmStatic
  fun w(tag: String?, msg: String?) {
    if (handler.isLoggable(WARN)) {
      handler.w(tag!!, msg!!)
    }
  }

  @JvmStatic
  fun w(cls: Class<*>?, msg: String?) {
    if (handler.isLoggable(WARN)) {
      handler.w(FLog.getTag(cls!!)!!, msg!!)
    }
  }

  @JvmStatic
  fun w(tag: String?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(WARN)) {
      handler.w(tag!!, formatString(msg, *args)!!)
    }
  }

  @JvmStatic
  fun w(tag: String?, tr: Throwable?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(WARN)) {
      handler.w(tag!!, formatString(msg, *args)!!, tr!!)
    }
  }

  @JvmStatic
  fun w(cls: Class<*>?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(WARN)) {
      handler.w(FLog.getTag(cls!!)!!, formatString(msg, *args)!!)
    }
  }

  @JvmStatic
  fun w(cls: Class<*>?, tr: Throwable?, msg: String?, vararg args: Any?) {
    if (isLoggable(WARN)) {
      FLog.w(cls, formatString(msg, *args), tr)
    }
  }

  @JvmStatic
  fun w(tag: String?, msg: String?, tr: Throwable?) {
    if (handler.isLoggable(WARN)) {
      handler.w(tag!!, msg!!, tr!!)
    }
  }

  @JvmStatic
  fun w(cls: Class<*>?, msg: String?, tr: Throwable?) {
    if (handler.isLoggable(WARN)) {
      handler.w(FLog.getTag(cls!!)!!, msg!!, tr!!)
    }
  }

  @JvmStatic
  fun e(tag: String?, msg: String?) {
    if (handler.isLoggable(ERROR)) {
      handler.e(tag!!, msg!!)
    }
  }

  @JvmStatic
  fun e(cls: Class<*>?, msg: String?) {
    if (handler.isLoggable(ERROR)) {
      handler.e(FLog.getTag(cls!!)!!, msg!!)
    }
  }

  @JvmStatic
  fun e(tag: String?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(ERROR)) {
      handler.e(tag!!, formatString(msg, *args)!!)
    }
  }

  @JvmStatic
  fun e(tag: String?, tr: Throwable?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(ERROR)) {
      handler.e(tag!!, formatString(msg, *args)!!, tr!!)
    }
  }

  @JvmStatic
  fun e(cls: Class<*>?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(ERROR)) {
      handler.e(FLog.getTag(cls!!)!!, formatString(msg, *args)!!)
    }
  }

  @JvmStatic
  fun e(cls: Class<*>?, tr: Throwable?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(ERROR)) {
      handler.e(FLog.getTag(cls!!)!!, formatString(msg, *args)!!, tr!!)
    }
  }

  @JvmStatic
  fun e(tag: String?, msg: String?, tr: Throwable?) {
    if (handler.isLoggable(ERROR)) {
      handler.e(tag!!, msg!!, tr!!)
    }
  }

  @JvmStatic
  fun e(cls: Class<*>?, msg: String?, tr: Throwable?) {
    if (handler.isLoggable(ERROR)) {
      handler.e(FLog.getTag(cls!!)!!, msg!!, tr!!)
    }
  }

  @JvmStatic
  fun wtf(tag: String?, msg: String?) {
    if (handler.isLoggable(ERROR)) {
      handler.wtf(tag!!, msg!!)
    }
  }

  @JvmStatic
  fun wtf(cls: Class<*>?, msg: String?) {
    if (handler.isLoggable(ERROR)) {
      handler.wtf(FLog.getTag(cls!!)!!, msg!!)
    }
  }

  @JvmStatic
  fun wtf(tag: String?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(ERROR)) {
      handler.wtf(tag!!, formatString(msg, *args)!!)
    }
  }

  @JvmStatic
  fun wtf(tag: String?, tr: Throwable?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(ERROR)) {
      handler.wtf(tag!!, formatString(msg, *args)!!, tr!!)
    }
  }

  @JvmStatic
  fun wtf(cls: Class<*>?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(ERROR)) {
      handler.wtf(FLog.getTag(cls!!)!!, formatString(msg, *args)!!)
    }
  }

  @JvmStatic
  fun wtf(cls: Class<*>?, tr: Throwable?, msg: String?, vararg args: Any?) {
    if (handler.isLoggable(ERROR)) {
      handler.wtf(FLog.getTag(cls!!)!!, formatString(msg, *args)!!, tr!!)
    }
  }

  @JvmStatic
  fun wtf(tag: String?, msg: String?, tr: Throwable?) {
    if (handler.isLoggable(ERROR)) {
      handler.wtf(tag!!, msg!!, tr!!)
    }
  }

  @JvmStatic
  fun wtf(cls: Class<*>?, msg: String?, tr: Throwable?) {
    if (handler.isLoggable(ERROR)) {
      handler.wtf(FLog.getTag(cls!!)!!, msg!!, tr!!)
    }
  }

  private fun formatString(str: String?, vararg args: Any?): String? {
    return String.format(null, str!!, *args)
  }

  private fun getTag(clazz: Class<*>): String? {
    return clazz.getSimpleName()
  }
}
