/*
 * Copyright (c) 2015-present, Facebook, Inc.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.common.webp;

import android.graphics.BitmapFactory;
import android.os.Build;
import android.util.Base64;
import java.io.UnsupportedEncodingException;
import javax.annotation.Nullable;

public class WebpSupportStatus {
  public static final boolean sIsWebpSupportRequired =
      Build.VERSION.SDK_INT <= Build.VERSION_CODES.JELLY_BEAN_MR1;

  public static final boolean sIsSimpleWebpSupported =
      Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH;

  public static final boolean sIsExtendedWebpSupported = isExtendedWebpSupported();

  public static @Nullable WebpBitmapFactory sWebpBitmapFactory = null;

  private static boolean sWebpLibraryChecked = false;

  public static @Nullable WebpBitmapFactory loadWebpBitmapFactoryIfExists() {
    if (sWebpLibraryChecked) {
      return sWebpBitmapFactory;
    }
    WebpBitmapFactory loadedWebpBitmapFactory = null;
    try {
      loadedWebpBitmapFactory = (WebpBitmapFactory) Class
          .forName("com.facebook.webpsupport.WebpBitmapFactoryImpl")
          .newInstance();
    } catch (Throwable e) {
      // Head in the sand
    }
    sWebpLibraryChecked = true;
    return loadedWebpBitmapFactory;
  }

  /**
   * BASE64 encoded extended WebP image.
   */
  private static final String VP8X_WEBP_BASE64 = "UklGRkoAAABXRUJQVlA4WAoAAAAQAAAAAAAAAAAAQUxQSAw" +
      "AAAARBxAR/Q9ERP8DAABWUDggGAAAABQBAJ0BKgEAAQAAAP4AAA3AAP7mtQAAAA==";

  /**
   * Helper method that transforms provided string into its byte representation
   * using ASCII encoding
   * @param value bytes value
   * @return byte array representing ascii encoded value
   */
  private static byte[] asciiBytes(String value) {
    try {
      return value.getBytes("ASCII");
    } catch (UnsupportedEncodingException uee) {
      // won't happen
      throw new RuntimeException("ASCII not found!", uee);
    }
  }


  /**
   * Each WebP header should consist of at least 20 bytes and start
   * with "RIFF" bytes followed by some 4 bytes and "WEBP" bytes.
   * A more detailed description if WebP can be found here:
   * <a href="https://developers.google.com/speed/webp/docs/riff_container">
   *   https://developers.google.com/speed/webp/docs/riff_container</a>
   */
  private static final int SIMPLE_WEBP_HEADER_LENGTH = 20;

  /**
   * Each VP8X WebP image has a "features" byte following its ChunkHeader('VP8X')
   */
  private static final int EXTENDED_WEBP_HEADER_LENGTH = 21;

  private static final byte[] WEBP_RIFF_BYTES = asciiBytes("RIFF");
  private static final byte[] WEBP_NAME_BYTES = asciiBytes("WEBP");

  /**
   * This is a constant used to detect different WebP's formats: vp8, vp8l and vp8x.
   */
  private static final byte[] WEBP_VP8_BYTES = asciiBytes("VP8 ");
  private static final byte[] WEBP_VP8L_BYTES = asciiBytes("VP8L");
  private static final byte[] WEBP_VP8X_BYTES = asciiBytes("VP8X");

  /**
   * Checks whether underlying platform supports extended WebPs
   */
  private static boolean isExtendedWebpSupported() {
    // Lossless and extended formats are supported on Android 4.2.1+
    // Unfortunately SDK_INT is not enough to distinguish 4.2 and 4.2.1
    // (both are API level 17 (JELLY_BEAN_MR1))
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR1) {
      return false;
    }

    if (Build.VERSION.SDK_INT == Build.VERSION_CODES.JELLY_BEAN_MR1) {
      // Let's test if extended webp is supported
      // To this end we will try to decode bounds of vp8x webp with alpha channel
      byte[] decodedBytes = Base64.decode(VP8X_WEBP_BASE64, Base64.DEFAULT);
      BitmapFactory.Options opts = new BitmapFactory.Options();
      opts.inJustDecodeBounds = true;
      BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length, opts);

      // If Android managed to find appropriate decoder then opts.outHeight and opts.outWidth
      // should be set. We can not assume that outMimeType is set.
      // Android guys forgot to update logic for mime types when they introduced support for webp.
      // For example, on 4.2.2 this field is not set for webp images.
      if (opts.outHeight != 1 || opts.outWidth != 1) {
        return false;
      }
    }

    return true;
  }

  public static boolean isWebpSupportedByPlatform(
      final byte[] imageHeaderBytes,
      final int offset,
      final int headerSize) {
    if (isSimpleWebpHeader(imageHeaderBytes, offset)) {
      return sIsSimpleWebpSupported;
    }

    if (isLosslessWebpHeader(imageHeaderBytes, offset)) {
      return sIsExtendedWebpSupported;
    }

    if (isExtendedWebpHeader(imageHeaderBytes, offset, headerSize)) {
      if (isAnimatedWebpHeader(imageHeaderBytes, offset)) {
        return false;
      }
      return sIsExtendedWebpSupported;
    }

    return false;
  }

  public static boolean isAnimatedWebpHeader(final byte[] imageHeaderBytes, final int offset) {
    boolean isVp8x = matchBytePattern(imageHeaderBytes, offset + 12, WEBP_VP8X_BYTES);
    // ANIM is 2nd bit (00000010 == 2) on 21st byte (imageHeaderBytes[20])
    boolean hasAnimationBit = (imageHeaderBytes[offset + 20] & 2) == 2;
    return isVp8x && hasAnimationBit;
  }

  public static boolean isSimpleWebpHeader(final byte[] imageHeaderBytes, final int offset) {
    return matchBytePattern(imageHeaderBytes, offset + 12, WEBP_VP8_BYTES);
  }

  public static boolean isLosslessWebpHeader(final byte[] imageHeaderBytes, final int offset) {
    return matchBytePattern(imageHeaderBytes, offset + 12, WEBP_VP8L_BYTES);
  }

  public static boolean isExtendedWebpHeader(
      final byte[] imageHeaderBytes,
      final int offset,
      final int headerSize) {
    return headerSize >= EXTENDED_WEBP_HEADER_LENGTH &&
        matchBytePattern(imageHeaderBytes, offset + 12, WEBP_VP8X_BYTES);
  }

  public static boolean isExtendedWebpHeaderWithAlpha(
      final byte[] imageHeaderBytes,
      final int offset) {
    boolean isVp8x = matchBytePattern(imageHeaderBytes, offset + 12, WEBP_VP8X_BYTES);
    // Has ALPHA is 5th bit (00010000 == 16) on 21st byte (imageHeaderBytes[20])
    boolean hasAlphaBit = (imageHeaderBytes[offset + 20] & 16) == 16;
    return isVp8x && hasAlphaBit;
  }

  /**
   * Checks if imageHeaderBytes contains WEBP_RIFF_BYTES and WEBP_NAME_BYTES and if the
   * header is long enough to be WebP's header.
   * WebP file format can be found here:
   * <a href="https://developers.google.com/speed/webp/docs/riff_container">
   *   https://developers.google.com/speed/webp/docs/riff_container</a>
   * @param imageHeaderBytes image header bytes
   * @return true if imageHeaderBytes contains a valid webp header
   */
  public static boolean isWebpHeader(
      final byte[] imageHeaderBytes,
      final int offset,
      final int headerSize) {
    return headerSize >= SIMPLE_WEBP_HEADER_LENGTH &&
        matchBytePattern(imageHeaderBytes, offset, WEBP_RIFF_BYTES) &&
        matchBytePattern(imageHeaderBytes, offset + 8, WEBP_NAME_BYTES);
  }

  private static boolean matchBytePattern(
      final byte[] byteArray,
      final int offset,
      final byte[] pattern) {
    if (pattern == null || byteArray == null) {
      return false;
    }
    if (pattern.length + offset > byteArray.length) {
      return false;
    }

    for (int i = 0; i < pattern.length; ++i) {
      if (byteArray[i + offset] != pattern[i]) {
        return false;
      }
    }

    return true;
  }
}
