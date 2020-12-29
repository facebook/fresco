/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imageformat;

import com.facebook.common.internal.Ints;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.webp.WebpSupportStatus;
import com.facebook.infer.annotation.Nullsafe;
import java.io.InputStream;
import javax.annotation.Nullable;

/** Default image format checker that is able to determine all {@link DefaultImageFormats}. */
@Nullsafe(Nullsafe.Mode.STRICT)
public class DefaultImageFormatChecker implements ImageFormat.FormatChecker {

  /**
   * Maximum header size for any image type.
   *
   * <p>This determines how much data {@link ImageFormatChecker#getImageFormat(InputStream) reads
   * from a stream. After changing any of the type detection algorithms, or adding a new one, this
   * value should be edited.
   */
  final int MAX_HEADER_LENGTH =
      Ints.max(
          EXTENDED_WEBP_HEADER_LENGTH,
          SIMPLE_WEBP_HEADER_LENGTH,
          JPEG_HEADER_LENGTH,
          PNG_HEADER_LENGTH,
          GIF_HEADER_LENGTH,
          BMP_HEADER_LENGTH,
          ICO_HEADER_LENGTH,
          HEIF_HEADER_LENGTH);

  @Override
  public int getHeaderSize() {
    return MAX_HEADER_LENGTH;
  }

  /**
   * Tries to match imageHeaderByte and headerSize against every known image format. If any match
   * succeeds, corresponding ImageFormat is returned.
   *
   * @param headerBytes the header bytes to check
   * @param headerSize the available header size
   * @return ImageFormat for given imageHeaderBytes or UNKNOWN if no such type could be recognized
   */
  @Nullable
  @Override
  public final ImageFormat determineFormat(byte[] headerBytes, int headerSize) {
    Preconditions.checkNotNull(headerBytes);

    if (WebpSupportStatus.isWebpHeader(headerBytes, 0, headerSize)) {
      return getWebpFormat(headerBytes, headerSize);
    }

    if (isJpegHeader(headerBytes, headerSize)) {
      return DefaultImageFormats.JPEG;
    }

    if (isPngHeader(headerBytes, headerSize)) {
      return DefaultImageFormats.PNG;
    }

    if (isGifHeader(headerBytes, headerSize)) {
      return DefaultImageFormats.GIF;
    }

    if (isBmpHeader(headerBytes, headerSize)) {
      return DefaultImageFormats.BMP;
    }

    if (isIcoHeader(headerBytes, headerSize)) {
      return DefaultImageFormats.ICO;
    }

    if (isHeifHeader(headerBytes, headerSize)) {
      return DefaultImageFormats.HEIF;
    }

    if (isDngHeader(headerBytes, headerSize)) {
      return DefaultImageFormats.DNG;
    }

    return ImageFormat.UNKNOWN;
  }

  /**
   * Each WebP header should consist of at least 20 bytes and start with "RIFF" bytes followed by
   * some 4 bytes and "WEBP" bytes. More detailed description if WebP can be found here: <a
   * href="https://developers.google.com/speed/webp/docs/riff_container">
   * https://developers.google.com/speed/webp/docs/riff_container</a>
   */
  private static final int SIMPLE_WEBP_HEADER_LENGTH = 20;

  /** Each VP8X WebP image has "features" byte following its ChunkHeader('VP8X') */
  private static final int EXTENDED_WEBP_HEADER_LENGTH = 21;

  /** Determines type of WebP image. imageHeaderBytes has to be header of a WebP image */
  private static ImageFormat getWebpFormat(final byte[] imageHeaderBytes, final int headerSize) {
    Preconditions.checkArgument(WebpSupportStatus.isWebpHeader(imageHeaderBytes, 0, headerSize));
    if (WebpSupportStatus.isSimpleWebpHeader(imageHeaderBytes, 0)) {
      return DefaultImageFormats.WEBP_SIMPLE;
    }

    if (WebpSupportStatus.isLosslessWebpHeader(imageHeaderBytes, 0)) {
      return DefaultImageFormats.WEBP_LOSSLESS;
    }

    if (WebpSupportStatus.isExtendedWebpHeader(imageHeaderBytes, 0, headerSize)) {
      if (WebpSupportStatus.isAnimatedWebpHeader(imageHeaderBytes, 0)) {
        return DefaultImageFormats.WEBP_ANIMATED;
      }
      if (WebpSupportStatus.isExtendedWebpHeaderWithAlpha(imageHeaderBytes, 0)) {
        return DefaultImageFormats.WEBP_EXTENDED_WITH_ALPHA;
      }
      return DefaultImageFormats.WEBP_EXTENDED;
    }

    return ImageFormat.UNKNOWN;
  }

  /**
   * Every JPEG image should start with SOI mark (0xFF, 0xD8) followed by beginning of another
   * segment (0xFF)
   */
  private static final byte[] JPEG_HEADER = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};

  private static final int JPEG_HEADER_LENGTH = JPEG_HEADER.length;

  /**
   * Checks if imageHeaderBytes starts with SOI (start of image) marker, followed by 0xFF. If
   * headerSize is lower than 3 false is returned. Description of jpeg format can be found here: <a
   * href="http://www.w3.org/Graphics/JPEG/itu-t81.pdf">
   * http://www.w3.org/Graphics/JPEG/itu-t81.pdf</a> Annex B deals with compressed data format
   *
   * @param imageHeaderBytes
   * @param headerSize
   * @return true if imageHeaderBytes starts with SOI_BYTES and headerSize >= 3
   */
  private static boolean isJpegHeader(final byte[] imageHeaderBytes, final int headerSize) {
    return headerSize >= JPEG_HEADER.length
        && ImageFormatCheckerUtils.startsWithPattern(imageHeaderBytes, JPEG_HEADER);
  }

  /** Every PNG image starts with 8 byte signature consisting of following bytes */
  private static final byte[] PNG_HEADER =
      new byte[] {(byte) 0x89, 'P', 'N', 'G', (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A};

  private static final int PNG_HEADER_LENGTH = PNG_HEADER.length;

  /**
   * Checks if array consisting of first headerSize bytes of imageHeaderBytes starts with png
   * signature. More information on PNG can be found there: <a
   * href="http://en.wikipedia.org/wiki/Portable_Network_Graphics">
   * http://en.wikipedia.org/wiki/Portable_Network_Graphics</a>
   *
   * @param imageHeaderBytes
   * @param headerSize
   * @return true if imageHeaderBytes starts with PNG_HEADER
   */
  private static boolean isPngHeader(final byte[] imageHeaderBytes, final int headerSize) {
    return headerSize >= PNG_HEADER.length
        && ImageFormatCheckerUtils.startsWithPattern(imageHeaderBytes, PNG_HEADER);
  }

  /**
   * Every gif image starts with "GIF" bytes followed by bytes indicating version of gif standard
   */
  private static final byte[] GIF_HEADER_87A = ImageFormatCheckerUtils.asciiBytes("GIF87a");

  private static final byte[] GIF_HEADER_89A = ImageFormatCheckerUtils.asciiBytes("GIF89a");
  private static final int GIF_HEADER_LENGTH = 6;

  /**
   * Checks if first headerSize bytes of imageHeaderBytes constitute a valid header for a gif image.
   * Details on GIF header can be found <a href="http://www.w3.org/Graphics/GIF/spec-gif89a.txt">on
   * page 7</a>
   *
   * @param imageHeaderBytes
   * @param headerSize
   * @return true if imageHeaderBytes is a valid header for a gif image
   */
  private static boolean isGifHeader(final byte[] imageHeaderBytes, final int headerSize) {
    if (headerSize < GIF_HEADER_LENGTH) {
      return false;
    }
    return ImageFormatCheckerUtils.startsWithPattern(imageHeaderBytes, GIF_HEADER_87A)
        || ImageFormatCheckerUtils.startsWithPattern(imageHeaderBytes, GIF_HEADER_89A);
  }

  /** Every bmp image starts with "BM" bytes */
  private static final byte[] BMP_HEADER = ImageFormatCheckerUtils.asciiBytes("BM");

  private static final int BMP_HEADER_LENGTH = BMP_HEADER.length;

  /**
   * Checks if first headerSize bytes of imageHeaderBytes constitute a valid header for a bmp image.
   * Details on BMP header can be found <a href="http://www.onicos.com/staff/iz/formats/bmp.html">
   * </a>
   *
   * @param imageHeaderBytes
   * @param headerSize
   * @return true if imageHeaderBytes is a valid header for a bmp image
   */
  private static boolean isBmpHeader(final byte[] imageHeaderBytes, final int headerSize) {
    if (headerSize < BMP_HEADER.length) {
      return false;
    }
    return ImageFormatCheckerUtils.startsWithPattern(imageHeaderBytes, BMP_HEADER);
  }

  /** Every ico image starts with 0x00000100 bytes */
  private static final byte[] ICO_HEADER =
      new byte[] {(byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00};

  private static final int ICO_HEADER_LENGTH = ICO_HEADER.length;

  /**
   * Checks if first headerSize bytes of imageHeaderBytes constitute a valid header for a ico image.
   * Details on ICO header can be found <a href="https://en.wikipedia.org/wiki/ICO_(file_format)">
   * </a>
   *
   * @param imageHeaderBytes
   * @param headerSize
   * @return true if imageHeaderBytes is a valid header for a ico image
   */
  private static boolean isIcoHeader(final byte[] imageHeaderBytes, final int headerSize) {
    if (headerSize < ICO_HEADER.length) {
      return false;
    }
    return ImageFormatCheckerUtils.startsWithPattern(imageHeaderBytes, ICO_HEADER);
  }

  /**
   * Every HEIF image starts with a specific signature. It's guaranteed to include "ftyp". The 4th
   * byte of the header gives us the size of the box, then we have "ftyp" followed by the exact
   * image format which can be one of: heic, heix, hevc, hevx.
   */
  private static final byte[] HEIF_HEADER_PREFIX = ImageFormatCheckerUtils.asciiBytes("ftyp");

  private static final byte[][] HEIF_HEADER_SUFFIXES = {
    ImageFormatCheckerUtils.asciiBytes("heic"),
    ImageFormatCheckerUtils.asciiBytes("heix"),
    ImageFormatCheckerUtils.asciiBytes("hevc"),
    ImageFormatCheckerUtils.asciiBytes("hevx"),
    ImageFormatCheckerUtils.asciiBytes("mif1"),
    ImageFormatCheckerUtils.asciiBytes("msf1")
  };
  private static final int HEIF_HEADER_LENGTH = 12;

  /**
   * Checks if first headerSize bytes of imageHeaderBytes constitute a valid header for a HEIF
   * image. Details on HEIF header can be found at: <a
   * href="http://nokiatech.github.io/heif/technical.html"></a>
   *
   * @param imageHeaderBytes
   * @param headerSize
   * @return true if imageHeaderBytes is a valid header for a HEIF image
   */
  private static boolean isHeifHeader(final byte[] imageHeaderBytes, final int headerSize) {
    if (headerSize < HEIF_HEADER_LENGTH) {
      return false;
    }

    final byte boxLength = imageHeaderBytes[3];
    if (boxLength < 8) {
      return false;
    }

    if (!ImageFormatCheckerUtils.hasPatternAt(imageHeaderBytes, HEIF_HEADER_PREFIX, 4)) {
      return false;
    }

    for (final byte[] heifFtype : HEIF_HEADER_SUFFIXES) {
      if (ImageFormatCheckerUtils.hasPatternAt(imageHeaderBytes, heifFtype, 8)) {
        return true;
      }
    }

    return false;
  }

  /**
   * DNG has TIFF format which begins with binary order ('II' or 'MM' (stands for intel and motorola
   * binary order respectively)) then a word containing the number 42
   * http://www.fileformat.info/format/tiff/corion.htm
   */
  private static final byte[] DNG_HEADER_II =
      new byte[] {(byte) 0x49, (byte) 0x49, (byte) 0x2A, (byte) 0x00};

  private static final byte[] DNG_HEADER_MM =
      new byte[] {(byte) 0x4D, (byte) 0x4D, (byte) 0x00, (byte) 0x2A};
  private static final int DNG_HEADER_LENGTH = DNG_HEADER_II.length;

  /**
   * Checks if imageHeaderBytes starts with 'II' or 'MM' then followed by a word containing 42
   * respecting the binary order. If headerSize is lower than 4 false is returned. Description of
   * DNG format can be found here: <a
   * href="https://www.adobe.com/content/dam/acom/en/products/photoshop/pdfs/dng_spec_1.4.0.0.pdf">
   * </a>
   *
   * @param imageHeaderBytes
   * @param headerSize
   * @return true if imageHeaderBytes starts with ('II' or 'MM') then 42 and headerSize >= 4
   */
  private static boolean isDngHeader(final byte[] imageHeaderBytes, final int headerSize) {
    return headerSize >= DNG_HEADER_LENGTH
        && (ImageFormatCheckerUtils.startsWithPattern(imageHeaderBytes, DNG_HEADER_II)
            || ImageFormatCheckerUtils.startsWithPattern(imageHeaderBytes, DNG_HEADER_MM));
  }
}
