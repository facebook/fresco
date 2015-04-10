/*
 * Copyright (C)2011-2013 D. R. Commander.  All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * - Redistributions of source code must retain the above copyright notice,
 *   this list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 * - Neither the name of the libjpeg-turbo Project nor the names of its
 *   contributors may be used to endorse or promote products derived from this
 *   software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS",
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED.  IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */

package org.libjpegturbo.turbojpeg;

/**
 * TurboJPEG utility class (cannot be instantiated)
 */
public final class TJ {


  /**
   * The number of chrominance subsampling options
   */
  public static final int NUMSAMP   = 5;
  /**
   * 4:4:4 chrominance subsampling (no chrominance subsampling).  The JPEG
   * or YUV image will contain one chrominance component for every pixel in the
   * source image.
   */
  public static final int SAMP_444  = 0;
  /**
   * 4:2:2 chrominance subsampling.  The JPEG or YUV image will contain one
   * chrominance component for every 2x1 block of pixels in the source image.
   */
  public static final int SAMP_422  = 1;
  /**
   * 4:2:0 chrominance subsampling.  The JPEG or YUV image will contain one
   * chrominance component for every 2x2 block of pixels in the source image.
   */
  public static final int SAMP_420  = 2;
  /**
   * Grayscale.  The JPEG or YUV image will contain no chrominance components.
   */
  public static final int SAMP_GRAY = 3;
  /**
   * 4:4:0 chrominance subsampling.  The JPEG or YUV image will contain one
   * chrominance component for every 1x2 block of pixels in the source image.
   * Note that 4:4:0 subsampling is not fully accelerated in libjpeg-turbo.
   */
  public static final int SAMP_440  = 4;


  /**
   * Returns the MCU block width for the given level of chrominance
   * subsampling.
   *
   * @param subsamp the level of chrominance subsampling (one of
   * <code>SAMP_*</code>)
   *
   * @return the MCU block width for the given level of chrominance subsampling
   */
  public static int getMCUWidth(int subsamp) throws Exception {
    if (subsamp < 0 || subsamp >= NUMSAMP)
      throw new Exception("Invalid subsampling type");
    return mcuWidth[subsamp];
  }

  private static final int[] mcuWidth = {
    8, 16, 16, 8, 8
  };


  /**
   * Returns the MCU block height for the given level of chrominance
   * subsampling.
   *
   * @param subsamp the level of chrominance subsampling (one of
   * <code>SAMP_*</code>)
   *
   * @return the MCU block height for the given level of chrominance
   * subsampling
   */
  public static int getMCUHeight(int subsamp) throws Exception {
    if (subsamp < 0 || subsamp >= NUMSAMP)
      throw new Exception("Invalid subsampling type");
    return mcuHeight[subsamp];
  }

  private static final int[] mcuHeight = {
    8, 8, 16, 8, 16
  };


  /**
   * The number of pixel formats
   */
  public static final int NUMPF   = 11;
  /**
   * RGB pixel format.  The red, green, and blue components in the image are
   * stored in 3-byte pixels in the order R, G, B from lowest to highest byte
   * address within each pixel.
   */
  public static final int PF_RGB  = 0;
  /**
   * BGR pixel format.  The red, green, and blue components in the image are
   * stored in 3-byte pixels in the order B, G, R from lowest to highest byte
   * address within each pixel.
   */
  public static final int PF_BGR  = 1;
  /**
   * RGBX pixel format.  The red, green, and blue components in the image are
   * stored in 4-byte pixels in the order R, G, B from lowest to highest byte
   * address within each pixel.  The X component is ignored when compressing
   * and undefined when decompressing.
   */
  public static final int PF_RGBX = 2;
  /**
   * BGRX pixel format.  The red, green, and blue components in the image are
   * stored in 4-byte pixels in the order B, G, R from lowest to highest byte
   * address within each pixel.  The X component is ignored when compressing
   * and undefined when decompressing.
   */
  public static final int PF_BGRX = 3;
  /**
   * XBGR pixel format.  The red, green, and blue components in the image are
   * stored in 4-byte pixels in the order R, G, B from highest to lowest byte
   * address within each pixel.  The X component is ignored when compressing
   * and undefined when decompressing.
   */
  public static final int PF_XBGR = 4;
  /**
   * XRGB pixel format.  The red, green, and blue components in the image are
   * stored in 4-byte pixels in the order B, G, R from highest to lowest byte
   * address within each pixel.  The X component is ignored when compressing
   * and undefined when decompressing.
   */
  public static final int PF_XRGB = 5;
  /**
   * Grayscale pixel format.  Each 1-byte pixel represents a luminance
   * (brightness) level from 0 to 255.
   */
  public static final int PF_GRAY = 6;
  /**
   * RGBA pixel format.  This is the same as {@link #PF_RGBX}, except that when
   * decompressing, the X byte is guaranteed to be 0xFF, which can be
   * interpreted as an opaque alpha channel.
   */
  public static final int PF_RGBA = 7;
  /**
   * BGRA pixel format.  This is the same as {@link #PF_BGRX}, except that when
   * decompressing, the X byte is guaranteed to be 0xFF, which can be
   * interpreted as an opaque alpha channel.
   */
  public static final int PF_BGRA = 8;
  /**
   * ABGR pixel format.  This is the same as {@link #PF_XBGR}, except that when
   * decompressing, the X byte is guaranteed to be 0xFF, which can be
   * interpreted as an opaque alpha channel.
   */
  public static final int PF_ABGR = 9;
  /**
   * ARGB pixel format.  This is the same as {@link #PF_XRGB}, except that when
   * decompressing, the X byte is guaranteed to be 0xFF, which can be
   * interpreted as an opaque alpha channel.
   */
  public static final int PF_ARGB = 10;


  /**
   * Returns the pixel size (in bytes) for the given pixel format.
   *
   * @param pixelFormat the pixel format (one of <code>PF_*</code>)
   *
   * @return the pixel size (in bytes) for the given pixel format
   */
  public static int getPixelSize(int pixelFormat) throws Exception {
    if (pixelFormat < 0 || pixelFormat >= NUMPF)
      throw new Exception("Invalid pixel format");
    return pixelSize[pixelFormat];
  }

  private static final int[] pixelSize = {
    3, 3, 4, 4, 4, 4, 1, 4, 4, 4, 4
  };


  /**
   * For the given pixel format, returns the number of bytes that the red
   * component is offset from the start of the pixel.  For instance, if a pixel
   * of format <code>TJ.PF_BGRX</code> is stored in <code>char pixel[]</code>,
   * then the red component will be
   * <code>pixel[TJ.getRedOffset(TJ.PF_BGRX)]</code>.
   *
   * @param pixelFormat the pixel format (one of <code>PF_*</code>)
   *
   * @return the red offset for the given pixel format
   */
  public static int getRedOffset(int pixelFormat) throws Exception {
    if (pixelFormat < 0 || pixelFormat >= NUMPF)
      throw new Exception("Invalid pixel format");
    return redOffset[pixelFormat];
  }

  private static final int[] redOffset = {
    0, 2, 0, 2, 3, 1, 0, 0, 2, 3, 1
  };


  /**
   * For the given pixel format, returns the number of bytes that the green
   * component is offset from the start of the pixel.  For instance, if a pixel
   * of format <code>TJ.PF_BGRX</code> is stored in <code>char pixel[]</code>,
   * then the green component will be
   * <code>pixel[TJ.getGreenOffset(TJ.PF_BGRX)]</code>.
   *
   * @param pixelFormat the pixel format (one of <code>PF_*</code>)
   *
   * @return the green offset for the given pixel format
   */
  public static int getGreenOffset(int pixelFormat) throws Exception {
    if (pixelFormat < 0 || pixelFormat >= NUMPF)
      throw new Exception("Invalid pixel format");
    return greenOffset[pixelFormat];
  }

  private static final int[] greenOffset = {
    1, 1, 1, 1, 2, 2, 0, 1, 1, 2, 2
  };


  /**
   * For the given pixel format, returns the number of bytes that the blue
   * component is offset from the start of the pixel.  For instance, if a pixel
   * of format <code>TJ.PF_BGRX</code> is stored in <code>char pixel[]</code>,
   * then the blue component will be
   * <code>pixel[TJ.getBlueOffset(TJ.PF_BGRX)]</code>.
   *
   * @param pixelFormat the pixel format (one of <code>PF_*</code>)
   *
   * @return the blue offset for the given pixel format
   */
  public static int getBlueOffset(int pixelFormat) throws Exception {
    if (pixelFormat < 0 || pixelFormat >= NUMPF)
      throw new Exception("Invalid pixel format");
    return blueOffset[pixelFormat];
  }

  private static final int[] blueOffset = {
    2, 0, 2, 0, 1, 3, 0, 2, 0, 1, 3
  };


  /**
   * The uncompressed source/destination image is stored in bottom-up (Windows,
   * OpenGL) order, not top-down (X11) order.
   */
  public static final int FLAG_BOTTOMUP     = 2;
  /**
   * Turn off CPU auto-detection and force TurboJPEG to use MMX code
   * (if the underlying codec supports it.)
   */
  public static final int FLAG_FORCEMMX     = 8;
  /**
   * Turn off CPU auto-detection and force TurboJPEG to use SSE code
   * (if the underlying codec supports it.)
   */
  public static final int FLAG_FORCESSE     = 16;
  /**
   * Turn off CPU auto-detection and force TurboJPEG to use SSE2 code
   * (if the underlying codec supports it.)
   */
  public static final int FLAG_FORCESSE2    = 32;
  /**
   * Turn off CPU auto-detection and force TurboJPEG to use SSE3 code
   * (if the underlying codec supports it.)
   */
  public static final int FLAG_FORCESSE3    = 128;
  /**
   * When decompressing an image that was compressed using chrominance
   * subsampling, use the fastest chrominance upsampling algorithm available in
   * the underlying codec.  The default is to use smooth upsampling, which
   * creates a smooth transition between neighboring chrominance components in
   * order to reduce upsampling artifacts in the decompressed image.
   */
  public static final int FLAG_FASTUPSAMPLE = 256;
  /**
   * Use the fastest DCT/IDCT algorithm available in the underlying codec.  The
   * default if this flag is not specified is implementation-specific.  For
   * example, the implementation of TurboJPEG for libjpeg[-turbo] uses the fast
   * algorithm by default when compressing, because this has been shown to have
   * only a very slight effect on accuracy, but it uses the accurate algorithm
   * when decompressing, because this has been shown to have a larger effect.
   */
  public static final int FLAG_FASTDCT      =  2048;
  /**
   * Use the most accurate DCT/IDCT algorithm available in the underlying
   * codec.  The default if this flag is not specified is
   * implementation-specific.  For example, the implementation of TurboJPEG for
   * libjpeg[-turbo] uses the fast algorithm by default when compressing,
   * because this has been shown to have only a very slight effect on accuracy,
   * but it uses the accurate algorithm when decompressing, because this has
   * been shown to have a larger effect.
   */
  public static final int FLAG_ACCURATEDCT  =  4096;


  /**
   * Returns the maximum size of the buffer (in bytes) required to hold a JPEG
   * image with the given width, height, and level of chrominance subsampling.
   *
   * @param width the width (in pixels) of the JPEG image
   *
   * @param height the height (in pixels) of the JPEG image
   *
   * @param jpegSubsamp the level of chrominance subsampling to be used when
   * generating the JPEG image (one of {@link TJ TJ.SAMP_*})
   *
   * @return the maximum size of the buffer (in bytes) required to hold a JPEG
   * image with the given width, height, and level of chrominance subsampling
   */
  public static native int bufSize(int width, int height, int jpegSubsamp)
    throws Exception;

  /**
   * Returns the size of the buffer (in bytes) required to hold a YUV planar
   * image with the given width, height, and level of chrominance subsampling.
   *
   * @param width the width (in pixels) of the YUV image
   *
   * @param height the height (in pixels) of the YUV image
   *
   * @param subsamp the level of chrominance subsampling used in the YUV
   * image (one of {@link TJ TJ.SAMP_*})
   *
   * @return the size of the buffer (in bytes) required to hold a YUV planar
   * image with the given width, height, and level of chrominance subsampling
   */
  public static native int bufSizeYUV(int width, int height, int subsamp)
    throws Exception;

  /**
   * Returns a list of fractional scaling factors that the JPEG decompressor in
   * this implementation of TurboJPEG supports.
   *
   * @return a list of fractional scaling factors that the JPEG decompressor in
   * this implementation of TurboJPEG supports
   */
  public static native TJScalingFactor[] getScalingFactors()
    throws Exception;

  static {
    TJLoader.load();
  }
};
