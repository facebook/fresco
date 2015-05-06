/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.memory;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import org.robolectric.RobolectricTestRunner;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(RobolectricTestRunner.class)
public class PooledByteStreamsTest  {
  private static final int POOLED_ARRAY_SIZE = 4;

  private ByteArrayPool mByteArrayPool;
  private byte[] mPooledArray;

  private byte[] mData;
  private InputStream mIs;
  private ByteArrayOutputStream mOs;

  private PooledByteStreams mPooledByteStreams;

  @Before
  public void setUp() {
    mByteArrayPool = mock(ByteArrayPool.class);
    mData = new byte[]{1, 2, 3, 4, 5, 6, 7, 8, 9, 115};
    mIs = new ByteArrayInputStream(mData);
    mOs = new ByteArrayOutputStream();

    mPooledArray = new byte[4];
    mPooledByteStreams = new PooledByteStreams(mByteArrayPool, POOLED_ARRAY_SIZE);
    when(mByteArrayPool.get(POOLED_ARRAY_SIZE)).thenReturn(mPooledArray);
  }

  @Test
  public void testUsesPool() throws IOException {
    mPooledByteStreams.copy(mIs, mOs);
    verify(mByteArrayPool).get(POOLED_ARRAY_SIZE);
    verify(mByteArrayPool).release(mPooledArray);
  }

  @Test
  public void testReleasesOnException() throws IOException {
    try {
      mPooledByteStreams.copy(
          mIs,
          new OutputStream() {
            @Override
            public void write(int oneByte) throws IOException {
              throw new IOException();
            }
          });
      fail();
    } catch (IOException ioe) {
      // expected
    }

    verify(mByteArrayPool).release(mPooledArray);
  }

  @Test
  public void testCopiesData() throws IOException {
    mPooledByteStreams.copy(mIs, mOs);
    assertArrayEquals(mData, mOs.toByteArray());
  }

  @Test
  public void testReleasesOnExceptionWithSize() throws IOException {
    try {
      mPooledByteStreams.copy(
          mIs,
          new OutputStream() {
            @Override
            public void write(int oneByte) throws IOException {
              throw new IOException();
            }
          }, 3);
      fail();
    } catch (IOException ioe) {
      // expected
    }

    verify(mByteArrayPool).release(mPooledArray);
  }

  @Test
  public void testCopiesDataWithSize() throws IOException {
    mPooledByteStreams.copy(mIs, mOs, 3);
    assertArrayEquals(Arrays.copyOf(mData, 3), mOs.toByteArray());
  }
}
