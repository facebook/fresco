/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */
package com.facebook.imageutils;

import android.util.Pair;
import java.io.IOException;
import java.io.InputStream;
import javax.annotation.Nullable;

/**
 * This class contains utility method in order to manage the WebP format metadata
 */
public class WebpUtil {

  /**
   * Header for VP8 (lossy WebP). Take care of the space into the String
   */
  private static final String VP8_HEADER = "VP8 ";

  /**
   * Header for Lossless WebP images
   */
  private static final String VP8L_HEADER = "VP8L";

  /**
   * Header for WebP enhanced
   */
  private static final String VP8X_HEADER = "VP8X";

  private WebpUtil() {
  }

  /**
   * This method checks for the dimension of the WebP image from the given InputStream. We don't
   * support mark/reset and the Stream is always closed.
   *
   * @param is The InputStream used for read WebP data
   * @return The Size of the WebP image if any or null if the size is not available
   */
  @Nullable public static Pair<Integer, Integer> getSize(InputStream is) {
    // Here we have to parse the WebP data skipping all the information which are not
    // the size
    Pair<Integer, Integer> result = null;
    byte[] headerBuffer = new byte[4];
    try {
      is.read(headerBuffer);
      // These must be RIFF
      if (!compare(headerBuffer, "RIFF")) {
        return null;
      }
      // Next there's the file size
      getInt(is);
      // Next the WEBP header
      is.read(headerBuffer);
      if (!compare(headerBuffer, "WEBP")) {
        return null;
      }
      // Now we can have different headers
      is.read(headerBuffer);
      final String headerAsString = getHeader(headerBuffer);
      if (VP8_HEADER.equals(headerAsString)) {
        return getVP8Dimension(is);
      } else if (VP8L_HEADER.equals(headerAsString)) {
        return getVP8LDimension(is);
      } else if (VP8X_HEADER.equals(headerAsString)) {
        return getVP8XDimension(is);
      }
    } catch (IOException e) {
      e.printStackTrace();
    } finally {
      if (is != null) {
        try {
          is.close();
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    // In this case we don't have the dimension
    return null;
  }

  /**
   * We manage the Simple WebP case
   *
   * @param is The InputStream we're reading
   * @return The dimensions if any
   * @throws IOException In case or error reading from the InputStream
   */
  private static @Nullable Pair<Integer, Integer> getVP8Dimension(final InputStream is)
      throws IOException {
    // We need to skip 7 bytes
    is.skip(7);
    // And then check the signature
    final short sign1 = getShort(is);
    final short sign2 = getShort(is);
    final short sign3 = getShort(is);
    if (sign1 != 0x9D || sign2 != 0x01 || sign3 != 0x2A) {
      // Signature error
      return null;
    }
    // We read the dimensions
    return new Pair<>(get2BytesAsInt(is), get2BytesAsInt(is));
  }

  /**
   * We manage the Lossless WebP case
   *
   * @param is The InputStream we're reading
   * @return The dimensions if any
   * @throws IOException In case or error reading from the InputStream
   */
  private static @Nullable Pair<Integer, Integer> getVP8LDimension(final InputStream is)
      throws IOException {
    // Skip 4 bytes
    getInt(is);
    //We have a check here
    final byte check = getByte(is);
    if (check != 0x2F) {
      return null;
    }
    int data1 = ((byte) is.read()) & 0xFF;
    int data2 = ((byte) is.read()) & 0xFF;
    int data3 = ((byte) is.read()) & 0xFF;
    int data4 = ((byte) is.read()) & 0xFF;
    // In this case the bits for size are 14!!! The sizes are -1!!!
    final int width = ((data2 & 0x3F) << 8 | data1) + 1;
    final int height = ((data4 & 0x0F) << 10 | data3 << 2 | (data2 & 0xC0) >> 6) + 1;
    return new Pair<>(width, height);
  }

  /**
   * We manage the Extended WebP case
   *
   * @param is The InputStream we're reading
   * @return The dimensions if any
   * @throws IOException In case or error reading from the InputStream
   */
  private static Pair<Integer, Integer> getVP8XDimension(final InputStream is) throws IOException {
    // We have to skip 8 bytes
    is.skip(8);
    // Read 3 bytes for width and height
    return new Pair<>(read3Bytes(is) + 1, read3Bytes(is) + 1);
  }

  /**
   * Compares some bytes with the text we're expecting
   *
   * @param what The bytes to compare
   * @param with The string those bytes should contains
   * @return True if they match and false otherwise
   */
  private static boolean compare(byte[] what, String with) {
    if (what.length != with.length()) {
      return false;
    }
    for (int i = 0; i < what.length; i++) {
      if (with.charAt(i) != what[i]) {
        return false;
      }
    }
    return true;
  }

  private static String getHeader(byte[] header) {
    StringBuilder str = new StringBuilder();
    for (int i = 0; i < header.length; i++) {
      str.append((char) header[i]);
    }
    return str.toString();
  }

  private static int getInt(InputStream is) throws IOException {
    byte byte1 = (byte) is.read();
    byte byte2 = (byte) is.read();
    byte byte3 = (byte) is.read();
    byte byte4 = (byte) is.read();
    return (byte4 << 24) & 0xFF000000 |
        (byte3 << 16) & 0xFF0000 |
        (byte2 << 8) & 0xFF00 |
        (byte1) & 0xFF;
  }

  public static int get2BytesAsInt(InputStream is) throws IOException {
    byte byte1 = (byte) is.read();
    byte byte2 = (byte) is.read();
    return (byte2 << 8 & 0xFF00) | (byte1 & 0xFF);
  }

  private static int read3Bytes(InputStream is) throws IOException {
    byte byte1 = getByte(is);
    byte byte2 = getByte(is);
    byte byte3 = getByte(is);
    return (((int) byte3) << 16 & 0xFF0000) |
        (((int) byte2) << 8 & 0xFF00) |
        (((int) byte1) & 0xFF);
  }

  private static short getShort(InputStream is) throws IOException {
    return (short) (is.read() & 0xFF);
  }

  private static byte getByte(InputStream is) throws IOException {
    return (byte) (is.read() & 0xFF);
  }

  private static boolean isBitOne(byte input, int bitIndex) {
    return ((input >> (bitIndex % 8)) & 1) == 1;
  }
}
