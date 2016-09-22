/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.webpsupport;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;

import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.os.MemoryFile;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.InstrumentationTestCase;

import com.facebook.common.internal.ByteStreams;
import com.facebook.common.internal.Throwables;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * This is the test in order to directly decoding all types of webp images
 */
@RunWith(AndroidJUnit4.class)
public class WebpDecodingTest extends InstrumentationTestCase {

  private static Method sGetFileDescriptorMethod;

  private Instrumentation mInstrumentation;

  @Override
  @Before
  public void setUp() {
    mInstrumentation = InstrumentationRegistry.getInstrumentation();
  }

  private MemoryFile getMemoryFile(String path) {
    try {
      byte[] data = ByteStreams.toByteArray(getTestImageInputStream(path));
      MemoryFile memoryFile = new MemoryFile(null, data.length);
      memoryFile.allowPurging(false);
      memoryFile.writeBytes(data, 0, 0, data.length);
      return memoryFile;
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private synchronized Method getFileDescriptorMethod() {
    if (sGetFileDescriptorMethod == null) {
      try {
        sGetFileDescriptorMethod = MemoryFile.class.getDeclaredMethod("getFileDescriptor");
      } catch (Exception e) {
        throw Throwables.propagate(e);
      }
    }
    return sGetFileDescriptorMethod;
  }

  private FileDescriptor getMemoryFileDescriptor(MemoryFile memoryFile) {
    try {
      Object rawFD = getFileDescriptorMethod().invoke(memoryFile);
      return (FileDescriptor) rawFD;
    } catch (Exception e) {
      throw Throwables.propagate(e);
    }
  }

  private InputStream getTestImageInputStream(String path) {
    try {
      return mInstrumentation.getContext().getResources().getAssets().open(path);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  @Test
  public void test_webp_extended_decoding_inputstream_bitmap() throws Throwable {
    final Bitmap bitmap = WebpBitmapFactoryImpl.hookDecodeStream(
        getTestImageInputStream("webp_e.webp"),
        null,
        null);
    assertBitmap(bitmap, 480, 320);
  }

  @Test
  public void test_webp_extended_decoding_filedescriptor_bitmap() throws Throwable {
    final MemoryFile memoryFile =  getMemoryFile("webp_e.webp");
    final Bitmap bitmap = WebpBitmapFactoryImpl.hookDecodeFileDescriptor(
        getMemoryFileDescriptor(memoryFile),
        null,
        null);
    memoryFile.close();
    assertBitmap(bitmap, 480, 320);
  }

  @Test
  public void test_webp_extended_with_alpha_decoding_inputstream_bitmap() throws Throwable {
    final Bitmap bitmap = WebpBitmapFactoryImpl.hookDecodeStream(
        getTestImageInputStream("webp_ea.webp"),
        null,
        null);

    assertBitmap(bitmap, 400 ,301);
  }

  @Test
  public void test_webp_extended_with_alpha_decoding_filedescriptor_bitmap() throws Throwable {
    final MemoryFile memoryFile =  getMemoryFile("webp_ea.webp");
    final Bitmap bitmap = WebpBitmapFactoryImpl.hookDecodeFileDescriptor(
        getMemoryFileDescriptor(memoryFile),
        null,
        null);
    memoryFile.close();
    assertBitmap(bitmap, 400 ,301);
  }

  @Test
  public void test_webp_lossless_decoding_inputstream_bitmap() throws Throwable {
    final Bitmap bitmap = WebpBitmapFactoryImpl.hookDecodeStream(
        getTestImageInputStream("webp_ll.webp"),
        null,
        null);

    assertBitmap(bitmap, 400, 301);
  }

  @Test
  public void test_webp_lossless_decoding_filedescriptor_bitmap() throws Throwable {
    final MemoryFile memoryFile =  getMemoryFile("webp_ll.webp");
    final Bitmap bitmap = WebpBitmapFactoryImpl.hookDecodeFileDescriptor(
        getMemoryFileDescriptor(memoryFile),
        null,
        null);
    memoryFile.close();
    assertBitmap(bitmap, 400, 301);
  }

  @Test
  public void test_webp_plain_inputstream_bitmap() throws Throwable {
    final Bitmap bitmap = WebpBitmapFactoryImpl.hookDecodeStream(
        getTestImageInputStream("webp_plain.webp"),
        null,
        null);

    assertBitmap(bitmap, 320, 214);
  }

  @Test
  public void test_webp_plain_decoding_filedescriptor_bitmap() throws Throwable {
    final MemoryFile memoryFile =  getMemoryFile("webp_plain.webp");
    final Bitmap bitmap = WebpBitmapFactoryImpl.hookDecodeFileDescriptor(
        getMemoryFileDescriptor(memoryFile),
        null,
        null);
    memoryFile.close();
    assertBitmap(bitmap, 320, 214);
  }

  private void assertBitmap(Bitmap bitmap, int width, int height) {
    assertNotNull("Bitmap should not be null", bitmap);
    assertEquals("Width should be decoded properly", width, bitmap.getWidth());
    assertEquals("Height should be decoded properly", height, bitmap.getHeight());
  }
}
