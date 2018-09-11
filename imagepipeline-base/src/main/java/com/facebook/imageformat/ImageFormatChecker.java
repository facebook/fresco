/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageformat;

import com.facebook.common.internal.ByteStreams;
import com.facebook.common.internal.Closeables;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Throwables;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import javax.annotation.Nullable;

/**
 * Detects the format of an encoded image.
 */
public class ImageFormatChecker {

  private static ImageFormatChecker sInstance;

  private int mMaxHeaderLength;

  @Nullable
  private List<ImageFormat.FormatChecker> mCustomImageFormatCheckers;

  private final ImageFormat.FormatChecker mDefaultFormatChecker = new DefaultImageFormatChecker();

  private ImageFormatChecker() {
    updateMaxHeaderLength();
  }

  public void setCustomImageFormatCheckers(
      @Nullable List<ImageFormat.FormatChecker> customImageFormatCheckers) {
    mCustomImageFormatCheckers = customImageFormatCheckers;
    updateMaxHeaderLength();
  }

  public ImageFormat determineImageFormat(final InputStream is) throws IOException {
    Preconditions.checkNotNull(is);
    final byte[] imageHeaderBytes = new byte[mMaxHeaderLength];
    final int headerSize = readHeaderFromStream(mMaxHeaderLength, is, imageHeaderBytes);

    ImageFormat format = mDefaultFormatChecker.determineFormat(imageHeaderBytes, headerSize);
    if (format != null && format != ImageFormat.UNKNOWN) {
      return format;
    }

    if (mCustomImageFormatCheckers != null) {
      for (ImageFormat.FormatChecker formatChecker : mCustomImageFormatCheckers) {
        format = formatChecker.determineFormat(imageHeaderBytes, headerSize);
        if (format != null && format != ImageFormat.UNKNOWN) {
          return format;
        }
      }
    }
    return ImageFormat.UNKNOWN;
  }

  private void updateMaxHeaderLength() {
    mMaxHeaderLength = mDefaultFormatChecker.getHeaderSize();
    if (mCustomImageFormatCheckers != null) {
      for (ImageFormat.FormatChecker checker : mCustomImageFormatCheckers) {
        mMaxHeaderLength = Math.max(mMaxHeaderLength, checker.getHeaderSize());
      }
    }
  }

  /**
   * Reads up to maxHeaderLength bytes from is InputStream. If mark is supported by is, it is
   * used to restore content of the stream after appropriate amount of data is read.
   * Read bytes are stored in imageHeaderBytes, which should be capable of storing
   * maxHeaderLength bytes.
   * @param maxHeaderLength the maximum header length
   * @param is
   * @param imageHeaderBytes
   * @return number of bytes read from is
   * @throws IOException
   */
  private static int readHeaderFromStream(
      int maxHeaderLength,
      final InputStream is,
      final byte[] imageHeaderBytes)
      throws IOException {
    Preconditions.checkNotNull(is);
    Preconditions.checkNotNull(imageHeaderBytes);
    Preconditions.checkArgument(imageHeaderBytes.length >= maxHeaderLength);

    // If mark is supported by the stream, use it to let the owner of the stream re-read the same
    // data. Otherwise, just consume some data.
    if (is.markSupported()) {
      try {
        is.mark(maxHeaderLength);
        return ByteStreams.read(is, imageHeaderBytes, 0, maxHeaderLength);
      } finally {
        is.reset();
      }
    } else {
      return ByteStreams.read(is, imageHeaderBytes, 0, maxHeaderLength);
    }
  }

  /**
   * Get the currently used instance of the image format checker
   * @return the image format checker to use
   */
  public static synchronized ImageFormatChecker getInstance() {
    if (sInstance == null) {
      sInstance = new ImageFormatChecker();
    }
    return sInstance;
  }

  /**
   * Tries to read up to MAX_HEADER_LENGTH bytes from InputStream is and use read bytes to
   * determine type of the image contained in is. If provided input stream does not support mark,
   * then this method consumes data from is and it is not safe to read further bytes from is after
   * this method returns. Otherwise, if mark is supported, it will be used to preserve original
   * content of is.
   * @param is
   * @return ImageFormat matching content of is InputStream or UNKNOWN if no type is suitable
   * @throws IOException if exception happens during read
   */
  public static ImageFormat getImageFormat(final InputStream is) throws IOException {
    return getInstance().determineImageFormat(is);
  }

  /*
   * A variant of getImageFormat that wraps IOException with RuntimeException.
   * This relieves clients of implementing dummy rethrow try-catch block.
   */
  public static ImageFormat getImageFormat_WrapIOException(final InputStream is) {
    try {
      return getImageFormat(is);
    } catch (IOException ioe) {
      throw Throwables.propagate(ioe);
    }
  }

  /**
   * Reads image header from a file indicated by provided filename and determines
   * its format. This method does not throw IOException if one occurs. In this case,
   * {@link ImageFormat#UNKNOWN} will be returned.
   * @param filename
   * @return ImageFormat for image stored in filename
   */
  public static ImageFormat getImageFormat(String filename) {
    FileInputStream fileInputStream = null;
    try {
      fileInputStream = new FileInputStream(filename);
      return getImageFormat(fileInputStream);
    } catch (IOException ioe) {
      return ImageFormat.UNKNOWN;
    } finally {
      Closeables.closeQuietly(fileInputStream);
    }
  }
}
