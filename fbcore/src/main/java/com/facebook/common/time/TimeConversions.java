/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.time;

/**
 * Time unit conversions.
 */
public class TimeConversions {

  /**
   * Convert time in millisecond to hours rounding down.
   */
  public static long millisecondsToHours(long timeMs) {
    return timeMs / TimeConstants.MS_PER_HOUR;
  }

  /**
   * Convert time in milliseconds to minutes rounding down.
   */
  public static long millisecondsToMinutes(long timeMs) {
    return timeMs / TimeConstants.MS_PER_MINUTE;
  }

  public static long millisecondsToDays(long timeMs) {
    return timeMs / TimeConstants.MS_PER_DAY;
  }

  public static long millisecondsToYears(long timeMs) {
    return timeMs / TimeConstants.MS_PER_YEAR;
  }

  /**
   * Convert time in milliseconds to seconds rounding down.
   */
  public static long millisecondsToSeconds(long timeMs) {
    return timeMs / TimeConstants.MS_PER_SECOND;
  }
}
