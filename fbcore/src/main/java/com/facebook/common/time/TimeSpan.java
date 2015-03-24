/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.time;

import java.util.concurrent.TimeUnit;

/**
 * Represents a duration of time
 */
public class TimeSpan {

  /**
   * @param nanos Duration, in nanoseconds
   * @return The time span representing the given duration
   */
  public static TimeSpan fromNanos(long nanos) {
    return new TimeSpan(nanos, TimeUnit.NANOSECONDS);
  }

  /**
   * @param micros Duration, in microseconds
   * @return The time span representing the given duration
   */
  public static TimeSpan fromMicros(long micros) {
    return new TimeSpan(micros, TimeUnit.MICROSECONDS);
  }

  /**
   * @param millis Duration, in milliseconds
   * @return The time span representing the given duration
   */
  public static TimeSpan fromMillis(long millis) {
    return new TimeSpan(millis, TimeUnit.MILLISECONDS);
  }

  /**
   * @param seconds Duration, in seconds
   * @return The time span representing the given duration
   */
  public static TimeSpan fromSeconds(long seconds) {
    return new TimeSpan(seconds, TimeUnit.SECONDS);
  }

  /**
   * @param minutes Duration, in minutes
   * @return The time span representing the given duration
   */
  public static TimeSpan fromMinutes(long minutes) {
    return new TimeSpan(minutes, TimeUnit.MINUTES);
  }

  /**
   * @param hours Duration, in hours
   * @return The time span representing the given duration
   */
  public static TimeSpan fromHours(long hours) {
    return new TimeSpan(hours, TimeUnit.HOURS);
  }

  /**
   * @param days Duration, in days
   * @return The time span representing the given duration
   */
  public static TimeSpan fromDays(long days) {
    return new TimeSpan(days, TimeUnit.DAYS);
  }

  /**
   * Actual time span, normalized to nanoseconds (shortest time supported by
   * {@link java.util.concurrent.TimeUnit})
   */
  private final long mDurationNanos;

  /**
   * Constructs an object representing the given time span
   * @param duration Duration, in the given units
   * @param timeUnit Unit duration is given
   */
  public TimeSpan(long duration, TimeUnit timeUnit) {
    mDurationNanos = timeUnit.toNanos(duration);
  }

  /**
   * @return Duration in nanoseconds
   */
  public long toNanos() {
    return mDurationNanos;
  }

  /**
   * @return Duration in microseconds
   */
  public long toMicros() {
    return TimeUnit.NANOSECONDS.toMicros(mDurationNanos);
  }

  /**
   * @return Duration in milliseconds
   */
  public long toMillis() {
    return TimeUnit.NANOSECONDS.toMillis(mDurationNanos);
  }

  /**
   * @return Duration in seconds
   */
  public long toSeconds() {
    return TimeUnit.NANOSECONDS.toSeconds(mDurationNanos);
  }

  /**
   * @return Duration in minutes
   */
  public long toMinutes() {
    return TimeUnit.NANOSECONDS.toMinutes(mDurationNanos);
  }

  /**
   * @return Duration in hours
   */
  public long toHours() {
    return TimeUnit.NANOSECONDS.toHours(mDurationNanos);
  }

  /**
   * @return Duration in days
   */
  public long toDays() {
    return TimeUnit.NANOSECONDS.toDays(mDurationNanos);
  }

  /**
   * Get duration as the given time unit
   *
   * @param timeUnit Time unit to get the duration in
   * @return Duration in requested time unit
   */
  public long as(TimeUnit timeUnit) {
    return timeUnit.convert(mDurationNanos, TimeUnit.NANOSECONDS);
  }

  @Override
  public String toString() {
    long days = toDays();
    long hours = toHours() % 24;
    long minutes = toMinutes() % 60;
    long seconds = toSeconds() % 60;
    long millis = toMillis() % 1000;
    long micros = toMicros() % 1000;
    long nanos = toNanos() % 1000;

    String sep = "";
    StringBuilder sb = new StringBuilder("TimeSpan{");
    if (days > 0) {
      sb.append(days).append(" ");
      pluralize(sb, "Day", days);
      sep = ", ";
    }
    if (hours > 0) {
      sb.append(sep).append(hours).append(" ");
      pluralize(sb, "Hour", hours);
      sep = ", ";
    }
    if (minutes > 0) {
      sb.append(sep).append(minutes).append(" ");
      pluralize(sb, "Minute", minutes);
      sep = ", ";
    }
    if (seconds > 0) {
      sb.append(sep).append(seconds).append(" ");
      pluralize(sb, "Second", seconds);
      sep = ", ";
    }
    if (millis > 0) {
      sb.append(sep).append(millis).append(" ");
      pluralize(sb, "Milli", millis);
      sep = ", ";
    }
    if (micros > 0) {
      sb.append(sep).append(micros).append(" ");
      pluralize(sb, "Micro", micros);
      sep = ", ";
    }
    if (nanos > 0) {
      sb.append(sep).append(nanos).append(" ");
      pluralize(sb, "Nano", nanos);
    }
    sb.append("}");
    return sb.toString();
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    TimeSpan timeSpan = (TimeSpan) o;

    if (mDurationNanos != timeSpan.mDurationNanos) return false;

    return true;
  }

  @Override
  public int hashCode() {
    return (int)(mDurationNanos ^ (mDurationNanos >>> 32));
  }

  private static final void pluralize(StringBuilder sb, String singular, long num) {
    sb.append(singular);
    if (num > 1) {
      sb.append("s");
    }
  }
}
