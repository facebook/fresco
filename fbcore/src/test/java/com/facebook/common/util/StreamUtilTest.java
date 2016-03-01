/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.common.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import com.facebook.common.internal.Closeables;
import org.robolectric.RobolectricTestRunner;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for {@link StreamUtil}
 */
@RunWith(RobolectricTestRunner.class)
public class StreamUtilTest {

  /**
   * Verify that using a ByteArrayInputStream does not allocate a new byte array.
   */
  @Test
  public void testByteArrayInputStream() throws Exception {
    byte[] bytes = new byte[8];
    InputStream input = new ByteArrayInputStream(bytes);
    try {
      byte[] bytesRead = StreamUtil.getBytesFromStream(input);
      assertTrue(Arrays.equals(bytes, bytesRead));
    } finally {
      Closeables.close(input, true);
    }
  }

  /**
   * Verify that using an offset with ByteArrayInputStream still produces correct output.
   */
  @Test
  public void testByteArrayInputStreamWithOffset() throws Exception {
    byte[] bytes = new byte[] {0, 1, 2, 3, 4};
    InputStream input = new ByteArrayInputStream(bytes, 1, 4);
    try {
      byte[] bytesRead = StreamUtil.getBytesFromStream(input);
      byte[] expectedBytes = new byte[] {1, 2, 3, 4};
      assertTrue(Arrays.equals(expectedBytes, bytesRead));
    } finally {
      Closeables.close(input, true);
    }
  }

  /**
   * Verify getting a byte array from a FileInputStream.
   */
  @Test
  public void testFileInputStream() throws Exception {
    checkFileInputStream(4);
    checkFileInputStream(64 * 1024 + 5); // Don't end on an even byte boundary
  }

  @Test
  public void testSuccessfulSkip() throws Exception {
    InputStream inputStream = mock(InputStream.class);
    when(inputStream.skip(anyLong())).thenReturn(2L);
    assertEquals(10, StreamUtil.skip(inputStream, 10));
    InOrder order = inOrder(inputStream);
    order.verify(inputStream).skip(10);
    order.verify(inputStream).skip(8);
    order.verify(inputStream).skip(6);
    order.verify(inputStream).skip(4);
    order.verify(inputStream).skip(2);
    verifyNoMoreInteractions(inputStream);
  }

  @Test
  public void testUnsuccessfulSkip() throws Exception {
    InputStream inputStream = mock(InputStream.class);
    when(inputStream.skip(anyLong())).thenReturn(3L, 5L, 0L, 6L, 0L);
    when(inputStream.read()).thenReturn(3, -1);
    assertEquals(15, StreamUtil.skip(inputStream, 20));
    InOrder order = inOrder(inputStream);
    order.verify(inputStream).skip(20);
    order.verify(inputStream).skip(17);
    order.verify(inputStream).skip(12);
    order.verify(inputStream).read();
    order.verify(inputStream).skip(11);
    order.verify(inputStream).skip(5);
    order.verify(inputStream).read();
    verifyNoMoreInteractions(inputStream);
  }

  private void checkFileInputStream(int size) throws IOException {
    byte[] bytesToWrite = new byte[size];
    for (int i=0; i<size; i++) {
      bytesToWrite[i] = (byte)i; // It's okay to truncate
    }

    File tmpFile = File.createTempFile("streamUtil", "test");
    InputStream input = null;
    OutputStream output = null;
    try {
      output = new FileOutputStream(tmpFile);
      output.write(bytesToWrite);
      output.close();

      input = new FileInputStream(tmpFile);
      byte[] bytesRead = StreamUtil.getBytesFromStream(input);
      assertTrue(Arrays.equals(bytesToWrite, bytesRead));
    } finally {
      Closeables.close(input, true);
      Closeables.close(output, false);
      assertTrue(tmpFile.delete());
    }
  }

}
