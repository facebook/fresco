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

import org.robolectric.RobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.fest.assertions.api.Assertions.assertThat;

@RunWith(RobolectricTestRunner.class)
public class TimeSpanTest {
  @Test
  public void testFromNanos() throws Exception {
    final long duration = 36;
    TimeSpan underTest = TimeSpan.fromNanos(duration);
    assertThat(underTest.as(TimeUnit.NANOSECONDS)).isEqualTo(duration);
    assertThat(underTest.toNanos()).isEqualTo(duration);
  }

  @Test
  public void testFromMicros() throws Exception {
    final long duration = 37;
    TimeSpan underTest = TimeSpan.fromMicros(duration);
    assertThat(underTest.as(TimeUnit.MICROSECONDS)).isEqualTo(duration);
    assertThat(underTest.toMicros()).isEqualTo(duration);
  }
  @Test
  public void testFromMillis() throws Exception {
    final long duration = 38;
    TimeSpan underTest = TimeSpan.fromMillis(duration);
    assertThat(underTest.as(TimeUnit.MILLISECONDS)).isEqualTo(duration);
    assertThat(underTest.toMillis()).isEqualTo(duration);
  }

  @Test
  public void testFromSeconds() throws Exception {
    final long duration = 39;
    TimeSpan underTest = TimeSpan.fromSeconds(duration);
    assertThat(underTest.as(TimeUnit.SECONDS)).isEqualTo(duration);
    assertThat(underTest.toSeconds()).isEqualTo(duration);
  }

  @Test
  public void testFromMinutes() throws Exception {
    final long duration = 40;
    TimeSpan underTest = TimeSpan.fromMinutes(duration);
    assertThat(underTest.as(TimeUnit.MINUTES)).isEqualTo(duration);
    assertThat(underTest.toMinutes()).isEqualTo(duration);
  }

  @Test
  public void testFromHours() throws Exception {
    final long duration = 41;
    TimeSpan underTest = TimeSpan.fromHours(duration);
    assertThat(underTest.as(TimeUnit.HOURS)).isEqualTo(duration);
    assertThat(underTest.toHours()).isEqualTo(duration);
  }

  @Test
  public void testFromDays() throws Exception {
    final long duration = 42;
    TimeSpan underTest = TimeSpan.fromDays(duration);
    assertThat(underTest.as(TimeUnit.DAYS)).isEqualTo(duration);
    assertThat(underTest.toDays()).isEqualTo(duration);
  }

  @Test
  public void testCtor() throws Exception {
    final long duration = 555;
    TimeSpan underTest = new TimeSpan(duration, TimeUnit.HOURS);
    assertThat(underTest.as(TimeUnit.HOURS)).isEqualTo(duration);
  }

  @Test
  public void testConversion() throws Exception {
    assertThat(TimeSpan.fromMillis(1000).as(TimeUnit.SECONDS)).isEqualTo(1);
  }

  @Test
  public void testEquality() throws Exception {
    assertThat(TimeSpan.fromMillis(1000)).isEqualTo(TimeSpan.fromSeconds(1));
  }

  @Test
  public void testToString() throws Exception {
    assertThat(TimeSpan.fromNanos(1).toString()).isEqualTo("TimeSpan{1 Nano}");
    assertThat(TimeSpan.fromNanos(2).toString()).isEqualTo("TimeSpan{2 Nanos}");

    assertThat(TimeSpan.fromMicros(1).toString()).isEqualTo("TimeSpan{1 Micro}");
    assertThat(TimeSpan.fromMicros(2).toString()).isEqualTo("TimeSpan{2 Micros}");

    assertThat(TimeSpan.fromMillis(1).toString()).isEqualTo("TimeSpan{1 Milli}");
    assertThat(TimeSpan.fromMillis(2).toString()).isEqualTo("TimeSpan{2 Millis}");

    assertThat(TimeSpan.fromSeconds(1).toString()).isEqualTo("TimeSpan{1 Second}");
    assertThat(TimeSpan.fromSeconds(2).toString()).isEqualTo("TimeSpan{2 Seconds}");

    assertThat(TimeSpan.fromMinutes(1).toString()).isEqualTo("TimeSpan{1 Minute}");
    assertThat(TimeSpan.fromMinutes(2).toString()).isEqualTo("TimeSpan{2 Minutes}");

    assertThat(TimeSpan.fromHours(1).toString()).isEqualTo("TimeSpan{1 Hour}");
    assertThat(TimeSpan.fromHours(2).toString()).isEqualTo("TimeSpan{2 Hours}");

    assertThat(TimeSpan.fromDays(1).toString()).isEqualTo("TimeSpan{1 Day}");
    assertThat(TimeSpan.fromDays(2).toString()).isEqualTo("TimeSpan{2 Days}");

    assertThat(TimeSpan.fromMinutes(90).toString()).isEqualTo("TimeSpan{1 Hour, 30 Minutes}");
  }
}
