package com.facebook.imagepipeline.decoder;

import android.annotation.TargetApi;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Picture;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PictureDrawable;
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

  private final BitmapPool mBitmapPool;

  /**
   * ArtPlatformImageDecoder decodes images from InputStream - to do so we need to provide
   * temporary buffer, otherwise framework will allocate one for us for each decode request
   */
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

  /**
   * Creates a bitmap from encoded JPEG bytes. Supports a partial JPEG image.
   *
   * @param encodedImage the encoded image with reference to the encoded bytes
   * @param bitmapConfig the {@link android.graphics.Bitmap.Config}
   *                     used to create the decoded Bitmap
   * @param length       the number of encoded bytes in the buffer
   * @return the bitmap
   * @throws java.lang.OutOfMemoryError if the Bitmap cannot be allocated
   */
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
      Picture pic = svg.renderToPicture(options.outWidth, options.outHeight);
      Drawable drawable = new PictureDrawable(pic);
      decodedBitmap = drawableToBitmap(drawable);
    } catch (RuntimeException re) {
      throw re;
    } catch (SVGParseException e) {
      e.printStackTrace();
    }

    return CloseableReference.of(decodedBitmap, mBitmapPool);
  }

  public static Bitmap drawableToBitmap(Drawable drawable) {
    Bitmap bitmap;

    if (drawable instanceof BitmapDrawable) {
      BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
      if (bitmapDrawable.getBitmap() != null) {
        return bitmapDrawable.getBitmap();
      }
    }

    if (drawable.getIntrinsicWidth() <= 0 || drawable.getIntrinsicHeight() <= 0) {
      bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
    } else {
      bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
    }

    Canvas canvas = new Canvas(bitmap);
    drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
    drawable.draw(canvas);
    return bitmap;
  }

  private static BitmapFactory.Options getDecodeOptionsForStream(
          EncodedImage encodedImage,
          Bitmap.Config bitmapConfig) {
    final BitmapFactory.Options options = new BitmapFactory.Options();
    // Sample size should ONLY be different than 1 when downsampling is enabled in the pipeline
    options.inSampleSize = encodedImage.getSampleSize();
    options.inJustDecodeBounds = true;
    // fill outWidth and outHeight
    options.outWidth = 400;
    options.outHeight = 400;

    options.inJustDecodeBounds = false;
    options.inDither = true;
    options.inPreferredConfig = bitmapConfig;
    options.inMutable = true;

    return options;
  }
}