/*
 * Copyright (C)2011-2014 D. R. Commander.  All Rights Reserved.
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

import java.awt.image.*;
import java.nio.*;

/**
 * TurboJPEG compressor
 */
public class TJCompressor {

  private static final String NO_ASSOC_ERROR =
    "No source image is associated with this instance";

  /**
   * Create a TurboJPEG compressor instance.
   */
  public TJCompressor() throws Exception {
    init();
  }

  /**
   * Create a TurboJPEG compressor instance and associate the uncompressed
   * source image stored in <code>srcImage</code> with the newly created
   * instance.
   *
   * @param srcImage see {@link #setSourceImage} for description
   *
   * @param x see {@link #setSourceImage} for description
   *
   * @param y see {@link #setSourceImage} for description
   *
   * @param width see {@link #setSourceImage} for description
   *
   * @param pitch see {@link #setSourceImage} for description
   *
   * @param height see {@link #setSourceImage} for description
   *
   * @param pixelFormat pixel format of the source image (one of
   * {@link TJ#PF_RGB TJ.PF_*})
   */
  public TJCompressor(byte[] srcImage, int x, int y, int width, int pitch,
                      int height, int pixelFormat) throws Exception {
    setSourceImage(srcImage, x, y, width, pitch, height, pixelFormat);
  }

  /**
   * @deprecated Use
   * {@link #TJCompressor(byte[], int, int, int, int, int, int)} instead.
   */
  @Deprecated
  public TJCompressor(byte[] srcImage, int width, int pitch, int height,
                      int pixelFormat) throws Exception {
    setSourceImage(srcImage, width, pitch, height, pixelFormat);
  }

  /**
   * Associate an uncompressed source image with this compressor instance.
   *
   * @param srcImage image buffer containing RGB or grayscale pixels to be
   * compressed or encoded
   *
   * @param x x offset (in pixels) of the region in the source image from which
   * the JPEG or YUV image should be compressed/encoded
   *
   * @param y y offset (in pixels) of the region in the source image from which
   * the JPEG or YUV image should be compressed/encoded
   *
   * @param width width (in pixels) of the region in the source image from
   * which the JPEG or YUV image should be compressed/encoded
   *
   * @param pitch bytes per line of the source image.  Normally, this should be
   * <code>width * TJ.pixelSize(pixelFormat)</code> if the source image is
   * unpadded, but you can use this parameter to, for instance, specify that
   * the scanlines in the source image are padded to a 4-byte boundary or to
   * compress/encode a JPEG or YUV image from a region of a larger source
   * image.  You can also be clever and use this parameter to skip lines, etc.
   * Setting this parameter to 0 is the equivalent of setting it to
   * <code>width * TJ.pixelSize(pixelFormat)</code>.
   *
   * @param height height (in pixels) of the region in the source image from
   * which the JPEG or YUV image should be compressed/encoded
   *
   * @param pixelFormat pixel format of the source image (one of
   * {@link TJ#PF_RGB TJ.PF_*})
   */
  public void setSourceImage(byte[] srcImage, int x, int y, int width,
                             int pitch, int height, int pixelFormat)
                             throws Exception {
    if (handle == 0) init();
    if (srcImage == null || x < 0 || y < 0 || width < 1 || height < 1 ||
        pitch < 0 || pixelFormat < 0 || pixelFormat >= TJ.NUMPF)
      throw new Exception("Invalid argument in setSourceImage()");
    srcBuf = srcImage;
    srcWidth = width;
    if (pitch == 0)
      srcPitch = width * TJ.getPixelSize(pixelFormat);
    else
      srcPitch = pitch;
    srcHeight = height;
    srcPixelFormat = pixelFormat;
    srcX = x;
    srcY = y;
  }

  /**
   * @deprecated Use
   * {@link #setSourceImage(byte[], int, int, int, int, int, int)} instead.
   */
  @Deprecated
  public void setSourceImage(byte[] srcImage, int width, int pitch,
                             int height, int pixelFormat) throws Exception {
    setSourceImage(srcImage, 0, 0, width, pitch, height, pixelFormat);
    srcX = srcY = -1;
  }


  /**
   * Set the level of chrominance subsampling for subsequent compress/encode
   * operations.
   *
   * @param newSubsamp the level of chrominance subsampling to use in
   * subsequent compress/encode operations (one of
   * {@link TJ#SAMP_444 TJ.SAMP_*})
   */
  public void setSubsamp(int newSubsamp) throws Exception {
    if (newSubsamp < 0 || newSubsamp >= TJ.NUMSAMP)
      throw new Exception("Invalid argument in setSubsamp()");
    subsamp = newSubsamp;
  }

  /**
   * Set the JPEG image quality level for subsequent compress operations.
   *
   * @param quality the new JPEG image quality level (1 to 100, 1 = worst,
   * 100 = best)
   */
  public void setJPEGQuality(int quality) throws Exception {
    if (quality < 1 || quality > 100)
      throw new Exception("Invalid argument in setJPEGQuality()");
    jpegQuality = quality;
  }

  /**
   * Compress the uncompressed source image associated with this compressor
   * instance and output a JPEG image to the given destination buffer.
   *
   * @param dstBuf buffer that will receive the JPEG image.  Use
   * {@link TJ#bufSize} to determine the maximum size for this buffer based on
   * the source image's width and height and the desired level of chrominance
   * subsampling.
   *
   * @param flags the bitwise OR of one or more of
   * {@link TJ#FLAG_BOTTOMUP TJ.FLAG_*}
   */
  public void compress(byte[] dstBuf, int flags) throws Exception {
    if (dstBuf == null || flags < 0)
      throw new Exception("Invalid argument in compress()");
    if (srcBuf == null)
      throw new Exception(NO_ASSOC_ERROR);
    if (jpegQuality < 0)
      throw new Exception("JPEG Quality not set");
    if (subsamp < 0)
      throw new Exception("Subsampling level not set");
    if (srcX >= 0 && srcY >= 0)
      compressedSize = compress(srcBuf, srcX, srcY, srcWidth, srcPitch,
                                srcHeight, srcPixelFormat, dstBuf, subsamp,
                                jpegQuality, flags);
    else
      compressedSize = compress(srcBuf, srcWidth, srcPitch, srcHeight,
                                srcPixelFormat, dstBuf, subsamp, jpegQuality,
                                flags);
  }

  /**
   * Compress the uncompressed source image associated with this compressor
   * instance and return a buffer containing a JPEG image.
   *
   * @param flags the bitwise OR of one or more of
   * {@link TJ#FLAG_BOTTOMUP TJ.FLAG_*}
   *
   * @return a buffer containing a JPEG image.  The length of this buffer will
   * not be equal to the size of the JPEG image.  Use {@link
   * #getCompressedSize} to obtain the size of the JPEG image.
   */
  public byte[] compress(int flags) throws Exception {
    if (srcWidth < 1 || srcHeight < 1)
      throw new Exception(NO_ASSOC_ERROR);
    byte[] buf = new byte[TJ.bufSize(srcWidth, srcHeight, subsamp)];
    compress(buf, flags);
    return buf;
  }

  /**
   * Compress the uncompressed source image stored in <code>srcImage</code>
   * and output a JPEG image to the given destination buffer.
   *
   * @param srcImage a <code>BufferedImage</code> instance containing RGB or
   * grayscale pixels to be compressed
   *
   * @param dstBuf buffer that will receive the JPEG image.  Use
   * {@link TJ#bufSize} to determine the maximum size for this buffer based on
   * the image width, height, and level of chrominance subsampling.
   *
   * @param flags the bitwise OR of one or more of
   * {@link TJ#FLAG_BOTTOMUP TJ.FLAG_*}
   */
  public void compress(BufferedImage srcImage, byte[] dstBuf, int flags)
                       throws Exception {
    if (srcImage == null || dstBuf == null || flags < 0)
      throw new Exception("Invalid argument in compress()");
    int width = srcImage.getWidth();
    int height = srcImage.getHeight();
    int pixelFormat;
    boolean intPixels = false;
    if (byteOrder == null)
      byteOrder = ByteOrder.nativeOrder();
    switch(srcImage.getType()) {
      case BufferedImage.TYPE_3BYTE_BGR:
        pixelFormat = TJ.PF_BGR;  break;
      case BufferedImage.TYPE_4BYTE_ABGR:
      case BufferedImage.TYPE_4BYTE_ABGR_PRE:
        pixelFormat = TJ.PF_XBGR;  break;
      case BufferedImage.TYPE_BYTE_GRAY:
        pixelFormat = TJ.PF_GRAY;  break;
      case BufferedImage.TYPE_INT_BGR:
        if (byteOrder == ByteOrder.BIG_ENDIAN)
          pixelFormat = TJ.PF_XBGR;
        else
          pixelFormat = TJ.PF_RGBX;
        intPixels = true;  break;
      case BufferedImage.TYPE_INT_RGB:
      case BufferedImage.TYPE_INT_ARGB:
      case BufferedImage.TYPE_INT_ARGB_PRE:
        if (byteOrder == ByteOrder.BIG_ENDIAN)
          pixelFormat = TJ.PF_XRGB;
        else
          pixelFormat = TJ.PF_BGRX;
        intPixels = true;  break;
      default:
        throw new Exception("Unsupported BufferedImage format");
    }
    WritableRaster wr = srcImage.getRaster();
    if (jpegQuality < 0)
      throw new Exception("JPEG Quality not set");
    if (subsamp < 0)
      throw new Exception("Subsampling level not set");
    if (intPixels) {
      SinglePixelPackedSampleModel sm =
        (SinglePixelPackedSampleModel)srcImage.getSampleModel();
      int stride = sm.getScanlineStride();
      DataBufferInt db = (DataBufferInt)wr.getDataBuffer();
      int[] buf = db.getData();
      if (srcX >= 0 && srcY >= 0)
        compressedSize = compress(buf, srcX, srcY, width, stride, height,
                                  pixelFormat, dstBuf, subsamp, jpegQuality,
                                  flags);
      else
        compressedSize = compress(buf, width, stride, height, pixelFormat,
                                  dstBuf, subsamp, jpegQuality, flags);
    } else {
      ComponentSampleModel sm =
        (ComponentSampleModel)srcImage.getSampleModel();
      int pixelSize = sm.getPixelStride();
      if (pixelSize != TJ.getPixelSize(pixelFormat))
        throw new Exception("Inconsistency between pixel format and pixel size in BufferedImage");
      int pitch = sm.getScanlineStride();
      DataBufferByte db = (DataBufferByte)wr.getDataBuffer();
      byte[] buf = db.getData();
      if (srcX >= 0 && srcY >= 0)
        compressedSize = compress(buf, srcX, srcY, width, pitch, height,
                                  pixelFormat, dstBuf, subsamp, jpegQuality,
                                  flags);
      else
        compressedSize = compress(buf, width, pitch, height, pixelFormat,
                                  dstBuf, subsamp, jpegQuality, flags);
    }
  }

  /**
   * Compress the uncompressed source image stored in <code>srcImage</code>
   * and return a buffer containing a JPEG image.
   *
   * @param srcImage a <code>BufferedImage</code> instance containing RGB or
   * grayscale pixels to be compressed
   *
   * @param flags the bitwise OR of one or more of
   * {@link TJ#FLAG_BOTTOMUP TJ.FLAG_*}
   *
   * @return a buffer containing a JPEG image.  The length of this buffer will
   * not be equal to the size of the JPEG image.  Use {@link
   * #getCompressedSize} to obtain the size of the JPEG image.
   */
  public byte[] compress(BufferedImage srcImage, int flags) throws Exception {
    int width = srcImage.getWidth();
    int height = srcImage.getHeight();
    byte[] buf = new byte[TJ.bufSize(width, height, subsamp)];
    compress(srcImage, buf, flags);
    return buf;
  }

  /**
   * Encode the uncompressed source image associated with this compressor
   * instance and output a YUV planar image to the given destination buffer.
   * This method uses the accelerated color conversion routines in
   * TurboJPEG's underlying codec to produce a planar YUV image that is
   * suitable for direct video display.  Specifically, if the chrominance
   * components are subsampled along the horizontal dimension, then the width
   * of the luminance plane is padded to the nearest multiple of 2 in the
   * output image (same goes for the height of the luminance plane, if the
   * chrominance components are subsampled along the vertical dimension.)
   * Also, each line of each plane in the output image is padded to 4 bytes.
   * Although this will work with any subsampling option, it is really only
   * useful in combination with {@link TJ#SAMP_420}, which produces an image
   * compatible with the I420 (AKA "YUV420P") format.
   * <p>
   * NOTE: Technically, the JPEG format uses the YCbCr colorspace, but per the
   * convention of the digital video community, the TurboJPEG API uses "YUV" to
   * refer to an image format consisting of Y, Cb, and Cr image planes.
   *
   * @param dstBuf buffer that will receive the YUV planar image.  Use
   * {@link TJ#bufSizeYUV} to determine the appropriate size for this buffer
   * based on the image width, height, and level of chrominance subsampling.
   *
   * @param flags the bitwise OR of one or more of
   * {@link TJ#FLAG_BOTTOMUP TJ.FLAG_*}
   */
  public void encodeYUV(byte[] dstBuf, int flags) throws Exception {
    if (dstBuf == null || flags < 0)
      throw new Exception("Invalid argument in compress()");
    if (srcBuf == null)
      throw new Exception(NO_ASSOC_ERROR);
    if (subsamp < 0)
      throw new Exception("Subsampling level not set");
    encodeYUV(srcBuf, srcWidth, srcPitch, srcHeight,
              srcPixelFormat, dstBuf, subsamp, flags);
    compressedSize = TJ.bufSizeYUV(srcWidth, srcHeight, subsamp);
  }

  /**
   * Encode the uncompressed source image associated with this compressor
   * instance and return a buffer containing a YUV planar image.  See
   * {@link #encodeYUV(byte[], int)} for more detail.
   *
   * @param flags the bitwise OR of one or more of
   * {@link TJ#FLAG_BOTTOMUP TJ.FLAG_*}
   *
   * @return a buffer containing a YUV planar image
   */
  public byte[] encodeYUV(int flags) throws Exception {
    if (srcWidth < 1 || srcHeight < 1)
      throw new Exception(NO_ASSOC_ERROR);
    if (subsamp < 0)
      throw new Exception("Subsampling level not set");
    byte[] buf = new byte[TJ.bufSizeYUV(srcWidth, srcHeight, subsamp)];
    encodeYUV(buf, flags);
    return buf;
  }

  /**
   * Encode the uncompressed source image stored in <code>srcImage</code>
   * and output a YUV planar image to the given destination buffer.  See
   * {@link #encodeYUV(byte[], int)} for more detail.
   *
   * @param srcImage a <code>BufferedImage</code> instance containing RGB or
   * grayscale pixels to be encoded
   *
   * @param dstBuf buffer that will receive the YUV planar image.  Use
   * {@link TJ#bufSizeYUV} to determine the appropriate size for this buffer
   * based on the image width, height, and level of chrominance subsampling.
   *
   * @param flags the bitwise OR of one or more of
   * {@link TJ#FLAG_BOTTOMUP TJ.FLAG_*}
   */
  public void encodeYUV(BufferedImage srcImage, byte[] dstBuf, int flags)
    throws Exception {
    if (srcImage == null || dstBuf == null || flags < 0)
      throw new Exception("Invalid argument in encodeYUV()");
    int width = srcImage.getWidth();
    int height = srcImage.getHeight();
    int pixelFormat;  boolean intPixels = false;
    if (byteOrder == null)
      byteOrder = ByteOrder.nativeOrder();
    switch(srcImage.getType()) {
      case BufferedImage.TYPE_3BYTE_BGR:
        pixelFormat = TJ.PF_BGR;  break;
      case BufferedImage.TYPE_4BYTE_ABGR:
      case BufferedImage.TYPE_4BYTE_ABGR_PRE:
        pixelFormat = TJ.PF_XBGR;  break;
      case BufferedImage.TYPE_BYTE_GRAY:
        pixelFormat = TJ.PF_GRAY;  break;
      case BufferedImage.TYPE_INT_BGR:
        if (byteOrder == ByteOrder.BIG_ENDIAN)
          pixelFormat = TJ.PF_XBGR;
        else
          pixelFormat = TJ.PF_RGBX;
        intPixels = true;  break;
      case BufferedImage.TYPE_INT_RGB:
      case BufferedImage.TYPE_INT_ARGB:
      case BufferedImage.TYPE_INT_ARGB_PRE:
        if (byteOrder == ByteOrder.BIG_ENDIAN)
          pixelFormat = TJ.PF_XRGB;
        else
          pixelFormat = TJ.PF_BGRX;
        intPixels = true;  break;
      default:
        throw new Exception("Unsupported BufferedImage format");
    }
    WritableRaster wr = srcImage.getRaster();
    if (subsamp < 0) throw new Exception("Subsampling level not set");
    if (intPixels) {
      SinglePixelPackedSampleModel sm =
        (SinglePixelPackedSampleModel)srcImage.getSampleModel();
      int stride = sm.getScanlineStride();
      DataBufferInt db = (DataBufferInt)wr.getDataBuffer();
      int[] buf = db.getData();
      encodeYUV(buf, width, stride, height, pixelFormat, dstBuf, subsamp,
                flags);
    } else {
      ComponentSampleModel sm =
        (ComponentSampleModel)srcImage.getSampleModel();
      int pixelSize = sm.getPixelStride();
      if (pixelSize != TJ.getPixelSize(pixelFormat))
        throw new Exception("Inconsistency between pixel format and pixel size in BufferedImage");
      int pitch = sm.getScanlineStride();
      DataBufferByte db = (DataBufferByte)wr.getDataBuffer();
      byte[] buf = db.getData();
      encodeYUV(buf, width, pitch, height, pixelFormat, dstBuf, subsamp,
                flags);
    }
    compressedSize = TJ.bufSizeYUV(width, height, subsamp);
  }

  /**
   * Encode the uncompressed source image stored in <code>srcImage</code>
   * and return a buffer containing a YUV planar image.  See
   * {@link #encodeYUV(byte[], int)} for more detail.
   *
   * @param srcImage a <code>BufferedImage</code> instance containing RGB or
   * grayscale pixels to be encoded
   *
   * @param flags the bitwise OR of one or more of
   * {@link TJ#FLAG_BOTTOMUP TJ.FLAG_*}
   *
   * @return a buffer containing a YUV planar image
   */
  public byte[] encodeYUV(BufferedImage srcImage, int flags) throws Exception {
    if (subsamp < 0)
      throw new Exception("Subsampling level not set");
    int width = srcImage.getWidth();
    int height = srcImage.getHeight();
    byte[] buf = new byte[TJ.bufSizeYUV(width, height, subsamp)];
    encodeYUV(srcImage, buf, flags);
    return buf;
  }

  /**
   * Returns the size of the image (in bytes) generated by the most recent
   * compress/encode operation.
   *
   * @return the size of the image (in bytes) generated by the most recent
   * compress/encode operation
   */
  public int getCompressedSize() {
    return compressedSize;
  }

  /**
   * Free the native structures associated with this compressor instance.
   */
  public void close() throws Exception {
    destroy();
  }

  protected void finalize() throws Throwable {
    try {
      close();
    } catch(Exception e) {
    } finally {
      super.finalize();
    }
  };

  private native void init() throws Exception;

  private native void destroy() throws Exception;

  // JPEG size in bytes is returned
  private native int compress(byte[] srcBuf, int width, int pitch,
    int height, int pixelFormat, byte[] dstBuf, int jpegSubsamp, int jpegQual,
    int flags) throws Exception; // deprecated

  private native int compress(byte[] srcBuf, int x, int y, int width,
    int pitch, int height, int pixelFormat, byte[] dstBuf, int jpegSubsamp,
    int jpegQual, int flags) throws Exception;

  private native int compress(int[] srcBuf, int width, int stride,
    int height, int pixelFormat, byte[] dstBuf, int jpegSubsamp, int jpegQual,
    int flags) throws Exception; // deprecated

  private native int compress(int[] srcBuf, int x, int y, int width,
    int stride, int height, int pixelFormat, byte[] dstBuf, int jpegSubsamp,
    int jpegQual, int flags) throws Exception;

  private native void encodeYUV(byte[] srcBuf, int width, int pitch,
    int height, int pixelFormat, byte[] dstBuf, int subsamp, int flags)
    throws Exception;

  private native void encodeYUV(int[] srcBuf, int width, int stride,
    int height, int pixelFormat, byte[] dstBuf, int subsamp, int flags)
    throws Exception;

  static {
    TJLoader.load();
  }

  private long handle = 0;
  private byte[] srcBuf = null;
  private int srcWidth = 0;
  private int srcHeight = 0;
  private int srcX = -1;
  private int srcY = -1;
  private int srcPitch = 0;
  private int srcPixelFormat = -1;
  private int subsamp = -1;
  private int jpegQuality = -1;
  private int compressedSize = 0;
  private ByteOrder byteOrder = null;
};
