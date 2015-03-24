/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.logging;

import android.util.Log;

/**
 * Logging wrapper with format style support.
 */
public class FLog {

  public static final int VERBOSE = Log.VERBOSE;

  public static final int DEBUG = Log.DEBUG;

  public static final int INFO = Log.INFO;

  public static final int WARN = Log.WARN;

  public static final int ERROR = Log.ERROR;

  public static final int ASSERT = Log.ASSERT;

  private static LoggingDelegate sHandler = FLogDefaultLoggingDelegate.getInstance();

  /**
   * Sets the logging delegate that overrides the default delegate.
   *
   * @param delegate the delegate to use
   */
  public static void setLoggingDelegate(LoggingDelegate delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException();
    }
    sHandler = delegate;
  }

  public static boolean isLoggable(int level) {
    return sHandler.isLoggable(level);
  }

  public static void setMinimumLoggingLevel(int level) {
    sHandler.setMinimumLoggingLevel(level);
  }

  public static int getMinimumLoggingLevel() {
    return sHandler.getMinimumLoggingLevel();
  }

  public static void v(String tag, String msg) {
    if (sHandler.isLoggable(VERBOSE)) {
      sHandler.v(tag, msg);
    }
  }

  public static void v(String tag, String msg, Object arg1) {
    if (sHandler.isLoggable(VERBOSE)) {
      sHandler.v(tag, formatString(msg, arg1));
    }
  }

  public static void v(String tag, String msg, Object arg1, Object arg2) {
    if (sHandler.isLoggable(VERBOSE)) {
      sHandler.v(tag, formatString(msg, arg1, arg2));
    }
  }

  public static void v(String tag, String msg, Object arg1, Object arg2, Object arg3) {
    if (sHandler.isLoggable(VERBOSE)) {
      sHandler.v(tag, formatString(msg, arg1, arg2, arg3));
    }
  }

  public static void v(String tag, String msg, Object arg1, Object arg2, Object arg3, Object arg4) {
    if (sHandler.isLoggable(VERBOSE)) {
      sHandler.v(tag, formatString(msg, arg1, arg2, arg3, arg4));
    }
  }

  public static void v(Class<?> cls, String msg) {
    if (sHandler.isLoggable(VERBOSE)) {
      sHandler.v(getTag(cls), msg);
    }
  }

  public static void v(Class<?> cls, String msg, Object arg1) {
    if (sHandler.isLoggable(VERBOSE)) {
      sHandler.v(getTag(cls), formatString(msg, arg1));
    }
  }

  public static void v(Class<?> cls, String msg, Object arg1, Object arg2) {
    if (sHandler.isLoggable(VERBOSE)) {
      sHandler.v(getTag(cls), formatString(msg, arg1, arg2));
    }
  }

  public static void v(Class<?> cls, String msg, Object arg1, Object arg2, Object arg3) {
    if (isLoggable(VERBOSE)) {
      v(cls, formatString(msg, arg1, arg2, arg3));
    }
  }

  public static void v(Class<?> cls, String msg, Object arg1, Object arg2, Object arg3,
      Object arg4) {
    if (sHandler.isLoggable(VERBOSE)) {
      sHandler.v(getTag(cls), formatString(msg, arg1, arg2, arg3, arg4));
    }
  }

  public static void v(String tag, String msg, Object... args) {
    if (sHandler.isLoggable(VERBOSE)) {
      sHandler.v(tag, formatString(msg, args));
    }
  }

  public static void v(String tag, Throwable tr, String msg, Object... args) {
    if (sHandler.isLoggable(VERBOSE)) {
      sHandler.v(tag, formatString(msg, args), tr);
    }
  }

  public static void v(Class<?> cls, String msg, Object... args) {
    if (sHandler.isLoggable(VERBOSE)) {
      sHandler.v(getTag(cls), formatString(msg, args));
    }
  }

  public static void v(Class<?> cls, Throwable tr, String msg, Object... args) {
    if (sHandler.isLoggable(VERBOSE)) {
      sHandler.v(getTag(cls), formatString(msg, args), tr);
    }
  }

  public static void v(String tag, String msg, Throwable tr) {
    if (sHandler.isLoggable(VERBOSE)) {
      sHandler.v(tag, msg, tr);
    }
  }

  public static void v(Class<?> cls, String msg, Throwable tr) {
    if (sHandler.isLoggable(VERBOSE)) {
      sHandler.v(getTag(cls), msg, tr);
    }
  }

  public static void d(String tag, String msg) {
    if (sHandler.isLoggable(DEBUG)) {
      sHandler.d(tag, msg);
    }
  }

  public static void d(String tag, String msg, Object arg1) {
    if (sHandler.isLoggable(DEBUG)) {
      sHandler.d(tag, formatString(msg, arg1));
    }
  }

  public static void d(String tag, String msg, Object arg1, Object arg2) {
    if (sHandler.isLoggable(DEBUG)) {
      sHandler.d(tag, formatString(msg, arg1, arg2));
    }
  }

  public static void d(String tag, String msg, Object arg1, Object arg2, Object arg3) {
    if (sHandler.isLoggable(DEBUG)) {
      sHandler.d(tag, formatString(msg, arg1, arg2, arg3));
    }
  }

  public static void d(String tag, String msg, Object arg1, Object arg2, Object arg3, Object arg4) {
    if (sHandler.isLoggable(DEBUG)) {
      sHandler.d(tag, formatString(msg, arg1, arg2, arg3, arg4));
    }
  }

  public static void d(Class<?> cls, String msg) {
    if (sHandler.isLoggable(DEBUG)) {
      sHandler.d(getTag(cls), msg);
    }
  }

  public static void d(Class<?> cls, String msg, Object arg1) {
    if (sHandler.isLoggable(DEBUG)) {
      sHandler.d(getTag(cls), formatString(msg, arg1));
    }
  }

  public static void d(Class<?> cls, String msg, Object arg1, Object arg2) {
    if (sHandler.isLoggable(DEBUG)) {
      sHandler.d(getTag(cls), formatString(msg, arg1, arg2));
    }
  }

  public static void d(Class<?> cls, String msg, Object arg1, Object arg2, Object arg3) {
    if (sHandler.isLoggable(DEBUG)) {
      sHandler.d(getTag(cls), formatString(msg, arg1, arg2, arg3));
    }
  }

  public static void d(Class<?> cls, String msg, Object arg1, Object arg2, Object arg3,
      Object arg4) {
    if (sHandler.isLoggable(DEBUG)) {
      sHandler.d(getTag(cls), formatString(msg, arg1, arg2, arg3, arg4));
    }
  }

  public static void d(String tag, String msg, Object... args) {
    if (sHandler.isLoggable(DEBUG)) {
      d(tag, formatString(msg, args));
    }
  }

  public static void d(String tag, Throwable tr, String msg, Object... args) {
    if (sHandler.isLoggable(DEBUG)) {
      d(tag, formatString(msg, args), tr);
    }
  }

  public static void d(Class<?> cls, String msg, Object... args) {
    if (sHandler.isLoggable(DEBUG)) {
      sHandler.d(getTag(cls), formatString(msg, args));
    }
  }

  public static void d(Class<?> cls, Throwable tr, String msg, Object... args) {
    if (sHandler.isLoggable(DEBUG)) {
      sHandler.d(getTag(cls), formatString(msg, args), tr);
    }
  }

  public static void d(String tag, String msg, Throwable tr) {
    if (sHandler.isLoggable(DEBUG)) {
      sHandler.d(tag, msg, tr);
    }
  }

  public static void d(Class<?> cls, String msg, Throwable tr) {
    if (sHandler.isLoggable(DEBUG)) {
      sHandler.d(getTag(cls), msg, tr);
    }
  }

  public static void i(String tag, String msg) {
    if (sHandler.isLoggable(INFO)) {
      sHandler.i(tag, msg);
    }
  }

  public static void i(String tag, String msg, Object arg1) {
    if (sHandler.isLoggable(INFO)) {
      sHandler.i(tag, formatString(msg, arg1));
    }
  }

  public static void i(String tag, String msg, Object arg1, Object arg2) {
    if (sHandler.isLoggable(INFO)) {
      sHandler.i(tag, formatString(msg, arg1, arg2));
    }
  }

  public static void i(String tag, String msg, Object arg1, Object arg2, Object arg3) {
    if (sHandler.isLoggable(INFO)) {
      sHandler.i(tag, formatString(msg, arg1, arg2, arg3));
    }
  }

  public static void i(String tag, String msg, Object arg1, Object arg2, Object arg3, Object arg4) {
    if (sHandler.isLoggable(INFO)) {
      sHandler.i(tag, formatString(msg, arg1, arg2, arg3, arg4));
    }
  }

  public static void i(Class<?> cls, String msg) {
    if (sHandler.isLoggable(INFO)) {
      sHandler.i(getTag(cls), msg);
    }
  }

  public static void i(Class<?> cls, String msg, Object arg1) {
    if (sHandler.isLoggable(INFO)) {
      sHandler.i(getTag(cls), formatString(msg, arg1));
    }
  }

  public static void i(Class<?> cls, String msg, Object arg1, Object arg2) {
    if (sHandler.isLoggable(INFO)) {
      sHandler.i(getTag(cls), formatString(msg, arg1, arg2));
    }
  }

  public static void i(Class<?> cls, String msg, Object arg1, Object arg2, Object arg3) {
    if (sHandler.isLoggable(INFO)) {
      sHandler.i(getTag(cls), formatString(msg, arg1, arg2, arg3));
    }
  }

  public static void i(Class<?> cls, String msg, Object arg1, Object arg2, Object arg3,
      Object arg4) {
    if (sHandler.isLoggable(INFO)) {
      sHandler.i(getTag(cls), formatString(msg, arg1, arg2, arg3, arg4));
    }
  }

  public static void i(String tag, String msg, Object... args) {
    if (sHandler.isLoggable(INFO)) {
      sHandler.i(tag, formatString(msg, args));
    }
  }

  public static void i(String tag, Throwable tr, String msg, Object... args) {
    if (sHandler.isLoggable(INFO)) {
      sHandler.i(tag, formatString(msg, args), tr);
    }
  }

  public static void i(Class<?> cls, String msg, Object... args) {
    if (sHandler.isLoggable(INFO)) {
      sHandler.i(getTag(cls), formatString(msg, args));
    }
  }

  public static void i(Class <?> cls, Throwable tr, String msg, Object... args) {
    if (isLoggable(INFO)) {
      sHandler.i(getTag(cls), formatString(msg, args), tr);
    }
  }

  public static void i(String tag, String msg, Throwable tr) {
    if (sHandler.isLoggable(INFO)) {
      sHandler.i(tag, msg, tr);
    }
  }

  public static void i(Class<?> cls, String msg, Throwable tr) {
    if (sHandler.isLoggable(INFO)) {
      sHandler.i(getTag(cls), msg, tr);
    }
  }

  public static void w(String tag, String msg) {
    if (sHandler.isLoggable(WARN)) {
      sHandler.w(tag, msg);
    }
  }

  public static void w(Class <?> cls, String msg) {
    if (sHandler.isLoggable(WARN)) {
      sHandler.w(getTag(cls), msg);
    }
  }

  public static void w(String tag, String msg, Object... args) {
    if (sHandler.isLoggable(WARN)) {
      sHandler.w(tag, formatString(msg, args));
    }
  }

  public static void w(String tag, Throwable tr, String msg, Object... args) {
    if (sHandler.isLoggable(WARN)) {
      sHandler.w(tag, formatString(msg, args), tr);
    }
  }

  public static void w(Class<?> cls, String msg, Object... args) {
    if (sHandler.isLoggable(WARN)) {
      sHandler.w(getTag(cls), formatString(msg, args));
    }
  }

  public static void w(Class<?> cls, Throwable tr, String msg, Object... args) {
    if (isLoggable(WARN)) {
      w(cls, formatString(msg, args), tr);
    }
  }

  public static void w(String tag, String msg, Throwable tr) {
    if (sHandler.isLoggable(WARN)) {
      sHandler.w(tag, msg, tr);
    }
  }

  public static void w(Class <?> cls, String msg, Throwable tr) {
    if (sHandler.isLoggable(WARN)) {
      sHandler.w(getTag(cls), msg, tr);
    }
  }

  public static void e(String tag, String msg) {
    if (sHandler.isLoggable(ERROR)) {
      sHandler.e(tag, msg);
    }
  }

  public static void e(Class <?> cls, String msg) {
    if (sHandler.isLoggable(ERROR)) {
      sHandler.e(getTag(cls), msg);
    }
  }

  public static void e(String tag, String msg, Object... args) {
    if (sHandler.isLoggable(ERROR)) {
      sHandler.e(tag, formatString(msg, args));
    }
  }

  public static void e(String tag, Throwable tr, String msg, Object... args) {
    if (sHandler.isLoggable(ERROR)) {
      sHandler.e(tag, formatString(msg, args), tr);
    }
  }

  public static void e(Class<?> cls, String msg, Object... args) {
    if (sHandler.isLoggable(ERROR)) {
      sHandler.e(getTag(cls), formatString(msg, args));
    }
  }

  public static void e(Class<?> cls, Throwable tr, String msg, Object... args) {
    if (sHandler.isLoggable(ERROR)) {
      sHandler.e(getTag(cls), formatString(msg, args), tr);
    }
  }

  public static void e(String tag, String msg, Throwable tr) {
    if (sHandler.isLoggable(ERROR)) {
      sHandler.e(tag, msg, tr);
    }
  }

  public static void e(Class <?> cls, String msg, Throwable tr) {
    if (sHandler.isLoggable(ERROR)) {
      sHandler.e(getTag(cls), msg, tr);
    }
  }

  public static void wtf(String tag, String msg) {
    if (sHandler.isLoggable(ERROR)) {
      sHandler.e(tag, msg);
    }
  }

  public static void wtf(Class <?> cls, String msg) {
    if (sHandler.isLoggable(ERROR)) {
      sHandler.e(getTag(cls), msg);
    }
  }

  public static void wtf(String tag, String msg, Object... args) {
    if (sHandler.isLoggable(ERROR)) {
      sHandler.wtf(tag, formatString(msg, args));
    }
  }

  public static void wtf(String tag, Throwable tr, String msg, Object... args) {
    if (sHandler.isLoggable(ERROR)) {
      sHandler.wtf(tag, formatString(msg, args), tr);
    }
  }

  public static void wtf(Class <?> cls, String msg, Object... args) {
    if (sHandler.isLoggable(ERROR)) {
      sHandler.wtf(getTag(cls), formatString(msg, args));
    }
  }

  public static void wtf(Class <?> cls, Throwable tr, String msg, Object... args) {
    if (sHandler.isLoggable(ERROR)) {
      sHandler.wtf(getTag(cls), formatString(msg, args), tr);
    }
  }

  public static void wtf(String tag, String msg, Throwable tr) {
    if (sHandler.isLoggable(ERROR)) {
      sHandler.wtf(tag, msg, tr);
    }
  }

  public static void wtf(Class <?> cls, String msg, Throwable tr) {
    if (sHandler.isLoggable(ERROR)) {
      sHandler.wtf(getTag(cls), msg, tr);
    }
  }

  private static String formatString(String str, Object... args) {
    return String.format(null, str, args);
  }

  private static String getTag(Class<?> clazz) {
    return clazz.getSimpleName();
  }
}
