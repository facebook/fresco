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
 * TurboJPEG decompressor
 */
public class TJDecompressor {

  private static final String NO_ASSOC_ERROR =
    "No JPEG image is associated with this instance";

  /**
   * Create a TurboJPEG decompresssor instance.
   */
  public TJDecompressor() throws Exception {
    init();
  }

  /**
   * Create a TurboJPEG decompressor instance and associate the JPEG image
   * stored in <code>jpegImage</code> with the newly created instance.
   *
   * @param jpegImage JPEG image buffer (size of the JPEG image is assumed to
   * be the length of the array)
   */
  public TJDecompressor(byte[] jpegImage) throws Exception {
    init();
    setJPEGImage(jpegImage, jpegImage.length);
  }

  /**
   * Create a TurboJPEG decompressor instance and associate the JPEG image
   * of length <code>imageSize</code> bytes stored in <code>jpegImage</code>
   * with the newly created instance.
   *
   * @param jpegImage JPEG image buffer
   *
   * @param imageSize size of the JPEG image (in bytes)
   */
  public TJDecompressor(byte[] jpegImage, int imageSize) throws Exception {
    init();
    setJPEGImage(jpegImage, imageSize);
  }

  /**
   * Associate the JPEG image of length <code>imageSize</code> bytes stored in
   * <code>jpegImage</code> with this decompressor instance.  This image will
   * be used as the source image for subsequent decompress operations.
   *
   * @param jpegImage JPEG image buffer
   *
   * @param imageSize size of the JPEG image (in bytes)
   */
  public void setJPEGImage(byte[] jpegImage, int imageSize) throws Exception {
    if (jpegImage == null || imageSize < 1)
      throw new Exception("Invalid argument in setJPEGImage()");
    jpegBuf = jpegImage;
    jpegBufSize = imageSize;
    decompressHeader(jpegBuf, jpegBufSize);
  }

  /**
   * Returns the width of the JPEG image associated with this decompressor
   * instance.
   *
   * @return the width of the JPEG image associated with this decompressor
   * instance
   */
  public int getWidth() throws Exception {
    if (jpegWidth < 1)
      throw new Exception(NO_ASSOC_ERROR);
    return jpegWidth;
  }

  /**
   * Returns the height of the JPEG image associated with this decompressor
   * instance.
   *
   * @return the height of the JPEG image associated with this decompressor
   * instance
   */
  public int getHeight() throws Exception {
    if (jpegHeight < 1)
      throw new Exception(NO_ASSOC_ERROR);
    return jpegHeight;
  }

  /**
   * Returns the level of chrominance subsampling used in the JPEG image
   * associated with this decompressor instance.  See {@link TJ TJ.SAMP_*}.
   *
   * @return the level of chrominance subsampling used in the JPEG image
   * associated with this decompressor instance
   */
  public int getSubsamp() throws Exception {
    if (jpegSubsamp < 0)
      throw new Exception(NO_ASSOC_ERROR);
    if (jpegSubsamp >= TJ.NUMSAMP)
      throw new Exception("JPEG header information is invalid");
    return jpegSubsamp;
  }

  /**
   * Returns the JPEG image buffer associated with this decompressor instance.
   *
   * @return the JPEG image buffer associated with this decompressor instance
   */
  public byte[] getJPEGBuf() throws Exception {
    if (jpegBuf == null)
      throw new Exception(NO_ASSOC_ERROR);
    return jpegBuf;
  }

  /**
   * Returns the size of the JPEG image (in bytes) associated with this
   * decompressor instance.
   *
   * @return the size of the JPEG image (in bytes) associated with this
   * decompressor instance
   */
  public int getJPEGSize() throws Exception {
    if (jpegBufSize < 1)
      throw new Exception(NO_ASSOC_ERROR);
    return jpegBufSize;
  }

  /**
   * Returns the width of the largest scaled-down image that the TurboJPEG
   * decompressor can generate without exceeding the desired image width and
   * height.
   *
   * @param desiredWidth desired width (in pixels) of the decompressed image.
   * Setting this to 0 is the same as setting it to the width of the JPEG image
   * (in other words, the width will not be considered when determining the
   * scaled image size.)
   *
   * @param desiredHeight desired height (in pixels) of the decompressed image.
   * Setting this to 0 is the same as setting it to the height of the JPEG
   * image (in other words, the height will not be considered when determining
   * the scaled image size.)
   *
   * @return the width of the largest scaled-down image that the TurboJPEG
   * decompressor can generate without exceeding the desired image width and
   * height
   */
  public int getScaledWidth(int desiredWidth, int desiredHeight)
                            throws Exception {
    if (jpegWidth < 1 || jpegHeight < 1)
      throw new Exception(NO_ASSOC_ERROR);
    if (desiredWidth < 0 || desiredHeight < 0)
      throw new Exception("Invalid argument in getScaledWidth()");
    TJScalingFactor[] sf = TJ.getScalingFactors();
    if (desiredWidth == 0)
      desiredWidth = jpegWidth;
    if (desiredHeight == 0)
      desiredHeight = jpegHeight;
    int scaledWidth = jpegWidth, scaledHeight = jpegHeight;
    for (int i = 0; i < sf.length; i++) {
      scaledWidth = sf[i].getScaled(jpegWidth);
      scaledHeight = sf[i].getScaled(jpegHeight);
      if (scaledWidth <= desiredWidth && scaledHeight <= desiredHeight)
        break;
    }
    if (scaledWidth > desiredWidth || scaledHeight > desiredHeight)
      throw new Exception("Could not scale down to desired image dimensions");
    return scaledWidth;
  }

  /**
   * Returns the height of the largest scaled-down image that the TurboJPEG
   * decompressor can generate without exceeding the desired image width and
   * height.
   *
   * @param desiredWidth desired width (in pixels) of the decompressed image.
   * Setting this to 0 is the same as setting it to the width of the JPEG image
   * (in other words, the width will not be considered when determining the
   * scaled image size.)
   *
   * @param desiredHeight desired height (in pixels) of the decompressed image.
   * Setting this to 0 is the same as setting it to the height of the JPEG
   * image (in other words, the height will not be considered when determining
   * the scaled image size.)
   *
   * @return the height of the largest scaled-down image that the TurboJPEG
   * decompressor can generate without exceeding the desired image width and
   * height
   */
  public int getScaledHeight(int desiredWidth, int desiredHeight)
                             throws Exception {
    if (jpegWidth < 1 || jpegHeight < 1)
      throw new Exception(NO_ASSOC_ERROR);
    if (desiredWidth < 0 || desiredHeight < 0)
      throw new Exception("Invalid argument in getScaledHeight()");
    TJScalingFactor[] sf = TJ.getScalingFactors();
    if (desiredWidth == 0)
      desiredWidth = jpegWidth;
    if (desiredHeight == 0)
      desiredHeight = jpegHeight;
    int scaledWidth = jpegWidth, scaledHeight = jpegHeight;
    for (int i = 0; i < sf.length; i++) {
      scaledWidth = sf[i].getScaled(jpegWidth);
      scaledHeight = sf[i].getScaled(jpegHeight);
      if (scaledWidth <= desiredWidth && scaledHeight <= desiredHeight)
        break;
    }
    if (scaledWidth > desiredWidth || scaledHeight > desiredHeight)
      throw new Exception("Could not scale down to desired image dimensions");
    return scaledHeight;
  }

  /**
   * Decompress the JPEG source image associated with this decompressor
   * instance and output a decompressed image to the given destination buffer.
   *
   * @param dstBuf buffer that will receive the decompressed image.  This
   * buffer should normally be <code>pitch * scaledHeight</code> bytes in size,
   * where <code>scaledHeight</code> can be determined by calling <code>
   * scalingFactor.{@link TJScalingFactor#getScaled getScaled}(jpegHeight)
   * </code> with one of the scaling factors returned from {@link
   * TJ#getScalingFactors} or by calling {@link #getScaledHeight}.  However,
   * the buffer may also be larger than the dimensions of the JPEG image, in
   * which case the <code>x</code>, <code>y</code>, and <code>pitch</code>
   * parameters can be used to specify the region into which the JPEG image
   * should be decompressed.
   *
   * @param x x offset (in pixels) of the region into which the JPEG image
   * should be decompressed, relative to the start of <code>dstBuf</code>.
   *
   * @param y y offset (in pixels) of the region into which the JPEG image
   * should be decompressed, relative to the start of <code>dstBuf</code>.
   *
   * @param desiredWidth desired width (in pixels) of the decompressed image
   * (or image region.)  If the desired image dimensions are different than the
   * dimensions of the JPEG image being decompressed, then TurboJPEG will use
   * scaling in the JPEG decompressor to generate the largest possible image
   * that will fit within the desired dimensions.  Setting this to 0 is the
   * same as setting it to the width of the JPEG image (in other words, the
   * width will not be considered when determining the scaled image size.)
   *
   * @param pitch bytes per line of the destination image.  Normally, this
   * should be set to <code>scaledWidth * TJ.pixelSize(pixelFormat)</code> if
   * the decompressed image is unpadded, but you can use this to, for instance,
   * pad each line of the decompressed image to a 4-byte boundary or to
   * decompress the JPEG image into a region of a larger image.  NOTE:
   * <code>scaledWidth</code> can be determined by calling <code>
   * scalingFactor.{@link TJScalingFactor#getScaled getScaled}(jpegWidth)
   * </code> or by calling {@link #getScaledWidth}.  Setting this parameter to
   * 0 is the equivalent of setting it to <code>scaledWidth *
   * TJ.pixelSize(pixelFormat)</code>.
   *
   * @param desiredHeight desired height (in pixels) of the decompressed image
   * (or image region.)  If the desired image dimensions are different than the
   * dimensions of the JPEG image being decompressed, then TurboJPEG will use
   * scaling in the JPEG decompressor to generate the largest possible image
   * that will fit within the desired dimensions.  Setting this to 0 is the
   * same as setting it to the height of the JPEG image (in other words, the
   * height will not be considered when determining the scaled image size.)
   *
   * @param pixelFormat pixel format of the decompressed/decoded image (one of
   * {@link TJ#PF_RGB TJ.PF_*})
   *
   * @param flags the bitwise OR of one or more of
   * {@link TJ#FLAG_BOTTOMUP TJ.FLAG_*}
   */
  public void decompress(byte[] dstBuf, int x, int y, int desiredWidth,
                         int pitch, int desiredHeight, int pixelFormat,
                         int flags) throws Exception {
    if (jpegBuf == null)
      throw new Exception(NO_ASSOC_ERROR);
    if (dstBuf == null || x < 0 || y < 0 || desiredWidth < 0 || pitch < 0 ||
        desiredHeight < 0 || pixelFormat < 0 || pixelFormat >= TJ.NUMPF ||
        flags < 0)
      throw new Exception("Invalid argument in decompress()");
    if (x > 0 || y > 0)
      decompress(jpegBuf, jpegBufSize, dstBuf, x, y, desiredWidth, pitch,
                 desiredHeight, pixelFormat, flags);
    else
      decompress(jpegBuf, jpegBufSize, dstBuf, desiredWidth, pitch,
                 desiredHeight, pixelFormat, flags);
  }

  /**
   * @deprecated Use
   * {@link #decompress(byte[], int, int, int, int, int, int, int)} instead.
   */
  @Deprecated
  public void decompress(byte[] dstBuf, int desiredWidth, int pitch,
                         int desiredHeight, int pixelFormat, int flags)
                         throws Exception {
    decompress(dstBuf, 0, 0, desiredWidth, pitch, desiredHeight, pixelFormat,
               flags);
  }

  /**
   * Decompress the JPEG source image associated with this decompressor
   * instance and return a buffer containing the decompressed image.
   *
   * @param desiredWidth see
   * {@link #decompress(byte[], int, int, int, int, int, int, int)}
   * for description
   *
   * @param pitch see
   * {@link #decompress(byte[], int, int, int, int, int, int, int)}
   * for description
   *
   * @param desiredHeight see
   * {@link #decompress(byte[], int, int, int, int, int, int, int)}
   * for description
   *
   * @param pixelFormat pixel format of the decompressed image (one of
   * {@link TJ#PF_RGB TJ.PF_*})
   *
   * @param flags the bitwise OR of one or more of
   * {@link TJ#FLAG_BOTTOMUP TJ.FLAG_*}
   *
   * @return a buffer containing the decompressed image
   */
  public byte[] decompress(int desiredWidth, int pitch, int desiredHeight,
                           int pixelFormat, int flags) throws Exception {
    if (desiredWidth < 0 || pitch < 0 || desiredHeight < 0 ||
        pixelFormat < 0 || pixelFormat >= TJ.NUMPF || flags < 0)
      throw new Exception("Invalid argument in decompress()");
    int pixelSize = TJ.getPixelSize(pixelFormat);
    int scaledWidth = getScaledWidth(desiredWidth, desiredHeight);
    int scaledHeight = getScaledHeight(desiredWidth, desiredHeight);
    if (pitch == 0)
      pitch = scaledWidth * pixelSize;
    byte[] buf = new byte[pitch * scaledHeight];
    decompress(buf, desiredWidth, pitch, desiredHeight, pixelFormat, flags);
    return buf;
  }

  /**
   * Decompress the JPEG source image associated with this decompressor
   * instance and output a YUV planar image to the given destination buffer.
   * This method performs JPEG decompression but leaves out the color
   * conversion step, so a planar YUV image is generated instead of an RGB
   * image.  The padding of the planes in this image is the same as in the
   * images generated by {@link TJCompressor#encodeYUV(byte[], int)}.
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
  public void decompressToYUV(byte[] dstBuf, int flags) throws Exception {
    if (jpegBuf == null)
      throw new Exception(NO_ASSOC_ERROR);
    if (dstBuf == null || flags < 0)
      throw new Exception("Invalid argument in decompressToYUV()");
    decompressToYUV(jpegBuf, jpegBufSize, dstBuf, flags);
  }


  /**
   * Decompress the JPEG source image associated with this decompressor
   * instance and return a buffer containing a YUV planar image.  See {@link
   * #decompressToYUV(byte[], int)} for more detail.
   *
   * @param flags the bitwise OR of one or more of
   * {@link TJ#FLAG_BOTTOMUP TJ.FLAG_*}
   *
   * @return a buffer containing a YUV planar image
   */
  public byte[] decompressToYUV(int flags) throws Exception {
    if (flags < 0)
      throw new Exception("Invalid argument in decompressToYUV()");
    if (jpegWidth < 1 || jpegHeight < 1 || jpegSubsamp < 0)
      throw new Exception(NO_ASSOC_ERROR);
    if (jpegSubsamp >= TJ.NUMSAMP)
      throw new Exception("JPEG header information is invalid");
    byte[] buf = new byte[TJ.bufSizeYUV(jpegWidth, jpegHeight, jpegSubsamp)];
    decompressToYUV(buf, flags);
    return buf;
  }

  /**
   * Decompress the JPEG source image associated with this decompressor
   * instance and output a decompressed image to the given destination buffer.
   *
   * @param dstBuf buffer that will receive the decompressed image.  This
   * buffer should normally be <code>stride * scaledHeight</code> pixels in
   * size, where <code>scaledHeight</code> can be determined by calling <code>
   * scalingFactor.{@link TJScalingFactor#getScaled getScaled}(jpegHeight)
   * </code> with one of the scaling factors returned from {@link
   * TJ#getScalingFactors} or by calling {@link #getScaledHeight}.  However,
   * the buffer may also be larger than the dimensions of the JPEG image, in
   * which case the <code>x</code>, <code>y</code>, and <code>stride</code>
   * parameters can be used to specify the region into which the JPEG image
   * should be decompressed.
   *
   * @param x x offset (in pixels) of the region into which the JPEG image
   * should be decompressed, relative to the start of <code>dstBuf</code>.
   *
   * @param y y offset (in pixels) of the region into which the JPEG image
   * should be decompressed, relative to the start of <code>dstBuf</code>.
   *
   * @param desiredWidth desired width (in pixels) of the decompressed image
   * (or image region.)  If the desired image dimensions are different than the
   * dimensions of the JPEG image being decompressed, then TurboJPEG will use
   * scaling in the JPEG decompressor to generate the largest possible image
   * that will fit within the desired dimensions.  Setting this to 0 is the
   * same as setting it to the width of the JPEG image (in other words, the
   * width will not be considered when determining the scaled image size.)
   *
   * @param stride pixels per line of the destination image.  Normally, this
   * should be set to <code>scaledWidth</code>, but you can use this to, for
   * instance, decompress the JPEG image into a region of a larger image.
   * NOTE: <code>scaledWidth</code> can be determined by calling <code>
   * scalingFactor.{@link TJScalingFactor#getScaled getScaled}(jpegWidth)
   * </code> or by calling {@link #getScaledWidth}.  Setting this parameter to
   * 0 is the equivalent of setting it to <code>scaledWidth</code>.
   *
   * @param desiredHeight desired height (in pixels) of the decompressed image
   * (or image region.)  If the desired image dimensions are different than the
   * dimensions of the JPEG image being decompressed, then TurboJPEG will use
   * scaling in the JPEG decompressor to generate the largest possible image
   * that will fit within the desired dimensions.  Setting this to 0 is the
   * same as setting it to the height of the JPEG image (in other words, the
   * height will not be considered when determining the scaled image size.)
   *
   * @param pixelFormat pixel format of the decompressed image (one of
   * {@link TJ#PF_RGB TJ.PF_*})
   *
   * @param flags the bitwise OR of one or more of
   * {@link TJ#FLAG_BOTTOMUP TJ.FLAG_*}
   */
  public void decompress(int[] dstBuf, int x, int y, int desiredWidth,
                         int stride, int desiredHeight, int pixelFormat,
                         int flags) throws Exception {
    if (jpegBuf == null)
      throw new Exception(NO_ASSOC_ERROR);
    if (dstBuf == null || x < 0 || y < 0 || desiredWidth < 0 || stride < 0 ||
        desiredHeight < 0 || pixelFormat < 0 || pixelFormat >= TJ.NUMPF ||
        flags < 0)
      throw new Exception("Invalid argument in decompress()");
    decompress(jpegBuf, jpegBufSize, dstBuf, x, y, desiredWidth, stride,
               desiredHeight, pixelFormat, flags);
  }

  /**
   * Decompress the JPEG source image associated with this decompressor
   * instance and output a decompressed image to the given
   * <code>BufferedImage</code> instance.
   *
   * @param dstImage a <code>BufferedImage</code> instance that will receive
   * the decompressed image.  The width and height of the
   * <code>BufferedImage</code> instance must match one of the scaled image
   * sizes that TurboJPEG is capable of generating from the JPEG image.
   *
   *
   * @param flags the bitwise OR of one or more of
   * {@link TJ#FLAG_BOTTOMUP TJ.FLAG_*}
   */
  public void decompress(BufferedImage dstImage, int flags) throws Exception {
    if (dstImage == null || flags < 0)
      throw new Exception("Invalid argument in decompress()");
    int desiredWidth = dstImage.getWidth();
    int desiredHeight = dstImage.getHeight();
    int scaledWidth = getScaledWidth(desiredWidth, desiredHeight);
    int scaledHeight = getScaledHeight(desiredWidth, desiredHeight);
    if (scaledWidth != desiredWidth || scaledHeight != desiredHeight)
      throw new Exception("BufferedImage dimensions do not match one of the scaled image sizes that TurboJPEG is capable of generating.");
    int pixelFormat;  boolean intPixels = false;
    if (byteOrder == null)
      byteOrder = ByteOrder.nativeOrder();
    switch(dstImage.getType()) {
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
        if (byteOrder == ByteOrder.BIG_ENDIAN)
          pixelFormat = TJ.PF_XRGB;
        else
          pixelFormat = TJ.PF_BGRX;
        intPixels = true;  break;
      case BufferedImage.TYPE_INT_ARGB:
      case BufferedImage.TYPE_INT_ARGB_PRE:
        if (byteOrder == ByteOrder.BIG_ENDIAN)
          pixelFormat = TJ.PF_ARGB;
        else
          pixelFormat = TJ.PF_BGRA;
        intPixels = true;  break;
      default:
        throw new Exception("Unsupported BufferedImage format");
    }
    WritableRaster wr = dstImage.getRaster();
    if (intPixels) {
      SinglePixelPackedSampleModel sm =
        (SinglePixelPackedSampleModel)dstImage.getSampleModel();
      int stride = sm.getScanlineStride();
      DataBufferInt db = (DataBufferInt)wr.getDataBuffer();
      int[] buf = db.getData();
      if (jpegBuf == null)
        throw new Exception(NO_ASSOC_ERROR);
      decompress(jpegBuf, jpegBufSize, buf, scaledWidth, stride, scaledHeight,
                 pixelFormat, flags);
    } else {
      ComponentSampleModel sm =
        (ComponentSampleModel)dstImage.getSampleModel();
      int pixelSize = sm.getPixelStride();
      if (pixelSize != TJ.getPixelSize(pixelFormat))
        throw new Exception("Inconsistency between pixel format and pixel size in BufferedImage");
      int pitch = sm.getScanlineStride();
      DataBufferByte db = (DataBufferByte)wr.getDataBuffer();
      byte[] buf = db.getData();
      decompress(buf, scaledWidth, pitch, scaledHeight, pixelFormat, flags);
    }
  }

  /**
   * Decompress the JPEG source image associated with this decompressor
   * instance and return a <code>BufferedImage</code> instance containing the
   * decompressed image.
   *
   * @param desiredWidth see
   * {@link #decompress(byte[], int, int, int, int, int, int, int)} for
   * description
   *
   * @param desiredHeight see
   * {@link #decompress(byte[], int, int, int, int, int, int, int)} for
   * description
   *
   * @param bufferedImageType the image type of the <code>BufferedImage</code>
   * instance that will be created (for instance,
   * <code>BufferedImage.TYPE_INT_RGB</code>)
   *
   * @param flags the bitwise OR of one or more of
   * {@link TJ#FLAG_BOTTOMUP TJ.FLAG_*}
   *
   * @return a <code>BufferedImage</code> instance containing the
   * decompressed image
   */
  public BufferedImage decompress(int desiredWidth, int desiredHeight,
                                  int bufferedImageType, int flags)
                                  throws Exception {
    if (desiredWidth < 0 || desiredHeight < 0 || flags < 0)
      throw new Exception("Invalid argument in decompress()");
    int scaledWidth = getScaledWidth(desiredWidth, desiredHeight);
    int scaledHeight = getScaledHeight(desiredWidth, desiredHeight);
    BufferedImage img = new BufferedImage(scaledWidth, scaledHeight,
                                          bufferedImageType);
    decompress(img, flags);
    return img;
  }

  /**
   * Free the native structures associated with this decompressor instance.
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

  private native void decompressHeader(byte[] srcBuf, int size)
    throws Exception;

  private native void decompress(byte[] srcBuf, int size, byte[] dstBuf,
    int desiredWidth, int pitch, int desiredHeight, int pixelFormat, int flags)
    throws Exception; // deprecated

  private native void decompress(byte[] srcBuf, int size, byte[] dstBuf, int x,
    int y, int desiredWidth, int pitch, int desiredHeight, int pixelFormat,
    int flags) throws Exception;

  private native void decompress(byte[] srcBuf, int size, int[] dstBuf,
    int desiredWidth, int stride, int desiredHeight, int pixelFormat,
    int flags) throws Exception; // deprecated

  private native void decompress(byte[] srcBuf, int size, int[] dstBuf, int x,
    int y, int desiredWidth, int stride, int desiredHeight, int pixelFormat,
    int flags) throws Exception;

  private native void decompressToYUV(byte[] srcBuf, int size, byte[] dstBuf,
    int flags) throws Exception;

  static {
    TJLoader.load();
  }

  protected long handle = 0;
  protected byte[] jpegBuf = null;
  protected int jpegBufSize = 0;
  protected int jpegWidth = 0;
  protected int jpegHeight = 0;
  protected int jpegSubsamp = -1;
  private ByteOrder byteOrder = null;
};
