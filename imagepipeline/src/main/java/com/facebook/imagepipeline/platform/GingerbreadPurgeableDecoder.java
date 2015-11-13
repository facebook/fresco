/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.platform;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.MemoryFile;

import javax.annotation.Nullable;

import com.facebook.common.internal.ByteStreams;
import com.facebook.common.internal.Closeables;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Throwables;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.streams.LimitedInputStream;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.FlexByteArrayPool;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferInputStream;

import static com.facebook.common.webp.WebpSupportStatus.sWebpBitmapFactory;

import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;

/**
 * Bitmap decoder (Gingerbread to Jelly Bean).
 * <p/>
 * <p>This copies incoming encoded bytes into a MemoryFile, and then decodes them using a file
 * descriptor, thus avoiding using any Java memory at all. This technique only works in JellyBean
 * and below.
 */
public class GingerbreadPurgeableDecoder extends DalvikPurgeableDecoder {

  private static Method sGetFileDescriptorMethod;
  private final boolean mWebpSupportEnabled;

  /**
   * Creates a GingerbreadPurgeableDecoder with optional support for webp
   * @param webpSupportEnabled If true the webpsupport library is enabled
   */
  public GingerbreadPurgeableDecoder(boolean webpSupportEnabled) {
    this.mWebpSupportEnabled = webpSupportEnabled;
  }

  /**
   * Decodes a byteArray into a purgeable bitmap
   *
   * @param bytesRef the byte buffer that contains the encoded bytes
   * @param options the options passed to the BitmapFactory
   * @return
   */
  @Override
  protected Bitmap decodeByteArrayAsPurgeable(
      CloseableReference<PooledByteBuffer> bytesRef,
      BitmapFactory.Options options) {
    return decodeFileDescriptorAsPurgeable(bytesRef, bytesRef.get().size(), null, options);
  }

  /**
   * Decodes a byteArray containing jpeg encoded bytes into a purgeable bitmap
   * <p/>
   * <p> Adds a JFIF End-Of-Image marker if needed before decoding.
   *
   * @param bytesRef the byte buffer that contains the encoded bytes
   * @param length the length of bytes for decox
   * @param options the options passed to the BitmapFactory
   * @return
   */
  @Override
  protected Bitmap decodeJPEGByteArrayAsPurgeable(
      CloseableReference<PooledByteBuffer> bytesRef,
      int length,
      BitmapFactory.Options options) {
    byte[] suffix = endsWithEOI(bytesRef, length) ? null : EOI;
    return decodeFileDescriptorAsPurgeable(bytesRef, length, suffix, options);
  }

  private static MemoryFile copyToMemoryFile(
      CloseableReference<PooledByteBuffer> bytesRef,
      int inputLength,
      @Nullable byte[] suffix) throws IOException {
    int outputLength = inputLength + (suffix == null ? 0 : suffix.length);
    MemoryFile memoryFile = new MemoryFile(null, outputLength);
    memoryFile.allowPurging(false);
    PooledByteBufferInputStream pbbIs = null;
    LimitedInputStream is = null;
    OutputStream os = null;
    try {
      pbbIs = new PooledByteBufferInputStream(bytesRef.get());
      is = new LimitedInputStream(pbbIs, inputLength);
      os = memoryFile.getOutputStream();
      ByteStreams.copy(is, os);
      if (suffix != null) {
        memoryFile.writeBytes(suffix, 0, inputLength, suffix.length);
      }
      return memoryFile;
    } finally {
      CloseableReference.closeSafely(bytesRef);
      Closeables.closeQuietly(pbbIs);
      Closeables.closeQuietly(is);
      Closeables.close(os, true);
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

  protected Bitmap decodeFileDescriptorAsPurgeable(
      CloseableReference<PooledByteBuffer> bytesRef,
      int inputLength,
      byte[] suffix,
      BitmapFactory.Options options) {
    MemoryFile memoryFile = null;
    try {
      memoryFile = copyToMemoryFile(bytesRef, inputLength, suffix);
      FileDescriptor fd = getMemoryFileDescriptor(memoryFile);
      Bitmap bitmap;
      if (mWebpSupportEnabled) {
        bitmap = sWebpBitmapFactory.decodeFileDescriptor(fd, null, options);
      } else {
        bitmap = BitmapFactory.decodeFileDescriptor(fd, null, options);
      }
      return Preconditions.checkNotNull(bitmap, "BitmapFactory returned null");
    } catch (IOException e) {
      throw Throwables.propagate(e);
    } finally {
      if (memoryFile != null) {
        memoryFile.close();
      }
    }
  }
}
