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
 * Useful time constants.
 */
public class TimeConstants {
  public static final long NS_PER_MS = 1000000;
  public static final long NS_PER_US = 1000;
  public static final long US_PER_MS = 1000;
  public static final long NS_PER_SECOND = 1000 * 1000 * 1000;
  public static final long US_PER_SECOND = 1000 * 1000;
  public static final long MS_PER_SECOND = 1000;
  public static final long SECONDS_PER_MINUTE = 60;
  public static final long MINUTES_PER_HOUR = 60;
  public static final long HOURS_PER_DAY = 24;
  public static final long DAYS_PER_WEEK = 7;
  public static final long DAYS_PER_MONTH = 30;
  public static final long DAYS_PER_YEAR = 365;

  public static final long MS_PER_MINUTE = MS_PER_SECOND * SECONDS_PER_MINUTE;
  public static final long MS_PER_HOUR = MS_PER_MINUTE * MINUTES_PER_HOUR;
  public static final long MS_PER_DAY = MS_PER_HOUR * HOURS_PER_DAY;
  public static final long MS_PER_WEEK = MS_PER_DAY * DAYS_PER_WEEK;
  public static final long MS_PER_YEAR = MS_PER_DAY * DAYS_PER_YEAR;

  public static final long SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR;
  public static final long SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY;
  public static final long SECONDS_PER_WEEK = SECONDS_PER_DAY * DAYS_PER_WEEK;
  public static final long SECONDS_PER_MONTH = SECONDS_PER_DAY * DAYS_PER_MONTH;
  public static final long SECONDS_PER_YEAR = SECONDS_PER_DAY * DAYS_PER_YEAR;
}
