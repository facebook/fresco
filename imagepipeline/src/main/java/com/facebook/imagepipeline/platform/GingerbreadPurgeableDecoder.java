/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.platform;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.MemoryFile;
import com.facebook.common.internal.ByteStreams;
import com.facebook.common.internal.Closeables;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Throwables;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferInputStream;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.streams.LimitedInputStream;
import com.facebook.common.webp.WebpBitmapFactory;
import com.facebook.common.webp.WebpSupportStatus;
import com.facebook.imagepipeline.nativecode.DalvikPurgeableDecoder;
import java.io.FileDescriptor;
import java.io.IOException;
import java.io.OutputStream;
import java.lang.reflect.Method;
import javax.annotation.Nullable;

/**
 * Bitmap decoder (Gingerbread to Jelly Bean).
 * <p/>
 * <p>This copies incoming encoded bytes into a MemoryFile, and then decodes them using a file
 * descriptor, thus avoiding using any Java memory at all. This technique only works in JellyBean
 * and below.
 */
public class GingerbreadPurgeableDecoder extends DalvikPurgeableDecoder {

  private static Method sGetFileDescriptorMethod;

  private final @Nullable WebpBitmapFactory mWebpBitmapFactory;

  public GingerbreadPurgeableDecoder() {
    super();
    mWebpBitmapFactory = WebpSupportStatus.loadWebpBitmapFactoryIfExists();
  }

  /**
   * Decodes a byteArray into a purgeable bitmap
   *
   * @param bytesRef the byte buffer that contains the encoded bytes
   * @param options the options passed to the BitmapFactory
   * @return the decoded bitmap
   */
  @Override
  protected Bitmap decodeByteArrayAsPurgeable(
      CloseableReference<PooledByteBuffer> bytesRef, BitmapFactory.Options options) {
    return decodeFileDescriptorAsPurgeable(bytesRef, bytesRef.get().size(), null, options);
  }

  /**
   * Decodes a byteArray containing jpeg encoded bytes into a purgeable bitmap
   *
   * <p>Adds a JFIF End-Of-Image marker if needed before decoding.
   *
   * @param bytesRef the byte buffer that contains the encoded bytes
   * @param length the length of bytes for decox
   * @param options the options passed to the BitmapFactory
   * @return the decoded bitmap
   */
  @Override
  protected Bitmap decodeJPEGByteArrayAsPurgeable(
      CloseableReference<PooledByteBuffer> bytesRef, int length, BitmapFactory.Options options) {
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

  private Bitmap decodeFileDescriptorAsPurgeable(
      CloseableReference<PooledByteBuffer> bytesRef,
      int inputLength,
      byte[] suffix,
      BitmapFactory.Options options) {
    MemoryFile memoryFile = null;
    try {
      memoryFile = copyToMemoryFile(bytesRef, inputLength, suffix);
      FileDescriptor fd = getMemoryFileDescriptor(memoryFile);
      if (mWebpBitmapFactory != null) {
        Bitmap bitmap = mWebpBitmapFactory.decodeFileDescriptor(fd, null, options);
        return Preconditions.checkNotNull(bitmap, "BitmapFactory returned null");
      } else {
        throw new IllegalStateException("WebpBitmapFactory is null");
      }
    } catch (IOException e) {
      throw Throwables.propagate(e);
    } finally {
      if (memoryFile != null) {
        memoryFile.close();
      }
    }
  }
}
