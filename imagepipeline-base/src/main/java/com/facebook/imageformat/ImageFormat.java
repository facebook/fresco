/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageformat;

import javax.annotation.Nullable;

/**
 * Class representing all used image formats.
 */
public class ImageFormat {

  public interface FormatChecker {

    /**
     * Get the number of header bytes the format checker requires
     * @return the number of header bytes needed
     */
    int getHeaderSize();

    /**
     * Returns an {@link ImageFormat} if the checker is able to determine the format
     * or null otherwise.
     * @param headerBytes the header bytes to check
     * @param headerSize the size of the header in bytes
     * @return the image format or null if unknown
     */
    @Nullable
    ImageFormat determineFormat(byte[] headerBytes, int headerSize);
  }

  // Unknown image format
  public static final ImageFormat UNKNOWN = new ImageFormat("UNKNOWN", null);

  private final @Nullable String mFileExtension;
  private final String mName;

  public ImageFormat(String name, @Nullable String fileExtension) {
    mName = name;
    mFileExtension = fileExtension;
  }

  /**
   * Get the default file extension for the given image format.
   * @return file extension for the image format
   */
  @Nullable
  public String getFileExtension() {
    return mFileExtension;
  }

  @Override
  public String toString() {
    return getName();
  }

  public String getName() {
    return mName;
  }
}
