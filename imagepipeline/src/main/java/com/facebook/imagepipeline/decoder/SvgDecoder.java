package com.facebook.imagepipeline.decoder;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.os.Build;
import android.support.v4.util.Pools;

import com.caverock.androidsvg.SVG;
import com.caverock.androidsvg.SVGParseException;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.streams.LimitedInputStream;
import com.facebook.common.streams.TailAppendingInputStream;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.BitmapPool;
import com.facebook.imagepipeline.platform.PlatformDecoder;
import com.facebook.imageutils.JfifUtil;
import java.io.InputStream;
import java.nio.ByteBuffer;
import javax.annotation.concurrent.ThreadSafe;

@TargetApi(Build.VERSION_CODES.HONEYCOMB)
@ThreadSafe
public class SvgDecoder implements PlatformDecoder {

  /**
   * Size of temporary array. Value recommended by Android docs for decoding Bitmaps.
   */
  private static final int DECODE_BUFFER_SIZE = 16 * 1024;

  /**
   * The normal screen width for mobile devices
   */
  private static final int MAX_WIDTH = 1080;

  private final BitmapPool mBitmapPool;

  @VisibleForTesting
  final Pools.SynchronizedPool<ByteBuffer> mDecodeBuffers;

  // TODO (5884402) - remove dependency on JfifUtil
  private static final byte[] EOI_TAIL = new byte[]{
          (byte) JfifUtil.MARKER_FIRST_BYTE,
          (byte) JfifUtil.MARKER_EOI};

  public SvgDecoder(BitmapPool bitmapPool, int maxNumThreads, Pools.SynchronizedPool decodeBuffers) {
    mBitmapPool = bitmapPool;
    mDecodeBuffers = decodeBuffers;
    for (int i = 0; i < maxNumThreads; i++) {
      mDecodeBuffers.release(ByteBuffer.allocate(DECODE_BUFFER_SIZE));
    }
  }

  /**
   * Creates a bitmap from encoded bytes.
   *
   * @param encodedImage the encoded image with a reference to the encoded bytes
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config}
   *                     used to create the decoded Bitmap
   * @return the bitmap
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
  @Override
  public CloseableReference<Bitmap> decodeFromEncodedImage(
          EncodedImage encodedImage,
          Bitmap.Config bitmapConfig) {
    final BitmapFactory.Options options = getDecodeOptionsForStream(encodedImage, bitmapConfig);
    boolean retryOnFail = options.inPreferredConfig != Bitmap.Config.ARGB_8888;
    try {
      return decodeStaticImageFromStream(encodedImage.getInputStream(), options);
    } catch (RuntimeException re) {
      if (retryOnFail) {
        return decodeFromEncodedImage(encodedImage, Bitmap.Config.ARGB_8888);
      }
      throw re;
    }
  }

  @Override
  public CloseableReference<Bitmap> decodeJPEGFromEncodedImage(
          EncodedImage encodedImage,
          Bitmap.Config bitmapConfig,
          int length) {
    boolean isJpegComplete = encodedImage.isCompleteAt(length);
    final BitmapFactory.Options options = getDecodeOptionsForStream(encodedImage, bitmapConfig);

    InputStream jpegDataStream = encodedImage.getInputStream();
    // At this point the InputStream from the encoded image should not be null since in the
    // pipeline,this comes from a call stack where this was checked before. Also this method needs
    // the InputStream to decode the image so this can't be null.
    Preconditions.checkNotNull(jpegDataStream);
    if (encodedImage.getSize() > length) {
      jpegDataStream = new LimitedInputStream(jpegDataStream, length);
    }
    if (!isJpegComplete) {
      jpegDataStream = new TailAppendingInputStream(jpegDataStream, EOI_TAIL);
    }
    boolean retryOnFail = options.inPreferredConfig != Bitmap.Config.ARGB_8888;
    try {
      return decodeStaticImageFromStream(jpegDataStream, options);
    } catch (RuntimeException re) {
      if (retryOnFail) {
        return decodeFromEncodedImage(encodedImage, Bitmap.Config.ARGB_8888);
      }
      throw re;
    }
  }

  protected CloseableReference<Bitmap> decodeStaticImageFromStream(
          InputStream inputStream,
          BitmapFactory.Options options) {
    Preconditions.checkNotNull(inputStream);

    Bitmap decodedBitmap = null;

    try {
      SVG svg = SVG.getFromInputStream(inputStream);
      float aspectRatio = svg.getDocumentAspectRatio();
      svg.setDocumentWidth(MAX_WIDTH * aspectRatio);
      svg.setDocumentHeight(MAX_WIDTH);
      decodedBitmap = Bitmap.createBitmap((int) ((float) MAX_WIDTH * aspectRatio), MAX_WIDTH, Bitmap.Config.ARGB_8888);
      Canvas canvas = new Canvas(decodedBitmap);
      svg.renderToCanvas(canvas);
    } catch (RuntimeException re) {
      throw re;
    } catch (SVGParseException e) {
      e.printStackTrace();
    }

    return CloseableReference.of(decodedBitmap, mBitmapPool);
  }

  /**
   * We will render image considering it will be shown
   * in full screen width mode preserving aspect ratio and quality for this image
   * Optimization will be done while caching
   */
  private static BitmapFactory.Options getDecodeOptionsForStream(
          EncodedImage encodedImage,
          Bitmap.Config bitmapConfig) {
    final BitmapFactory.Options options = new BitmapFactory.Options();
    // Sample size should ONLY be different than 1 when downsampling is enabled in the pipeline
    options.inSampleSize = encodedImage.getSampleSize();
    options.inJustDecodeBounds = true;
    // fill outWidth and outHeight
    options.outWidth = MAX_WIDTH;
    options.outHeight = MAX_WIDTH;

    options.inJustDecodeBounds = false;
    options.inDither = true;
    options.inPreferredConfig = bitmapConfig;
    options.inMutable = true;

    return options;
  }
}