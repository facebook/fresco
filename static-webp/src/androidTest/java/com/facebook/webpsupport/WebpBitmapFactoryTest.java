/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.webpsupport;

import android.app.Instrumentation;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Rect;
import android.os.Build;
import androidx.test.InstrumentationRegistry;
import com.facebook.common.internal.ByteStreams;
import com.facebook.common.internal.Throwables;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import junit.framework.TestCase;
import org.junit.Before;
import org.junit.Test;

public class WebpBitmapFactoryTest extends TestCase {
  private Instrumentation mInstrumentation;

  private WebpBitmapFactoryImpl mWebpBitmapFactory;

  @Override
  @Before
  public void setUp() {
    mInstrumentation = InstrumentationRegistry.getInstrumentation();
    mWebpBitmapFactory = new WebpBitmapFactoryImpl();
    ImagePipelineConfig.Builder configBuilder =
        ImagePipelineConfig.newBuilder(mInstrumentation.getContext())
            .experiment().setWebpBitmapFactory(mWebpBitmapFactory);
    ImagePipelineFactory.initialize(configBuilder.build());
  }

  private FileDescriptor getImageFileDescriptor(String path) {
    try {
      File file = File.createTempFile("reqsquare", "tmp");
      byte[] data = ByteStreams.toByteArray(getTestImageInputStream(path));
      FileOutputStream fos = new FileOutputStream(file);
      fos.write(data);
      fos.close();
      return new FileInputStream(file).getFD();
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private InputStream getTestImageInputStream(String path) {
    try {
      return mInstrumentation.getContext().getResources().getAssets().open(path);
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  /**
   * Return input stream for jpeg
   * @return InputStream instance
   */
  private InputStream getTestJpegInputStream() {
    return getTestImageInputStream("redsquare.jpg");
  }

  /**
   * Return input stream for lossless webp
   * @return InputStream instance
   */
  private InputStream getTestWebpInputStream() {
    return getTestImageInputStream("redsquare.webp");
  }

  @Test
  public void testJpegFallback() throws Throwable {
    final Bitmap bitmap = mWebpBitmapFactory.decodeStream(
        getTestJpegInputStream(),
        null,
        null);

    assertNotNull("Bitmap should not be null", bitmap);
    assertEquals("Width should be decoded properly", 20, bitmap.getWidth());
    assertEquals("Height should be decoded properly", 20, bitmap.getHeight());

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      // TODO 8639341 - In GB colors are not decoded exactly as they are into the image
      assertEquals("Bitmap pixels should be red", 0xFFFF0100, bitmap.getPixel(5, 8));
    }
  }

  @Test
  public void testWebpDecodeStream() throws Throwable {
    final Bitmap bitmap = mWebpBitmapFactory.decodeStream(
        getTestWebpInputStream(),
        null,
        null);

    assertNotNull("Bitmap should not be null", bitmap);
    assertEquals("Width should be decoded properly", 20, bitmap.getWidth());
    assertEquals("Height should be decoded properly", 20, bitmap.getHeight());

    assertEquals("Bitmap pixels should be red", 0xFFFF0100, bitmap.getPixel(5, 8));
    // Alternatively, load image manually adb pull /mnt/sdcard/resulthooked.jpg
    //    bitmap.compress(
    //        Bitmap.CompressFormat.JPEG,
    //        90,
    //        new FileOutputStream(Environment.getExternalStorageDirectory() +
    // "/resulthooked.jpg"));
  }

  @Test
  public void testWebpJustDecodeBounds() throws Throwable {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inJustDecodeBounds = true;

    final Bitmap bitmap = mWebpBitmapFactory.decodeStream(
        getTestWebpInputStream(),
        null,
        options);

    assertNull("Bitmap should be null", bitmap);
    assertEquals("Width should be decoded properly", 20, options.outWidth);
    assertEquals("Height should be decoded properly", 20, options.outHeight);
  }

  @Test
  public void testInBitmap() throws Throwable {
    if (!WebpBitmapFactoryImpl.IN_BITMAP_SUPPORTED) {
      return;
    }
    Bitmap inBitmap = Bitmap.createBitmap(20, 20, Bitmap.Config.ARGB_8888);

    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inBitmap = inBitmap;

    final Bitmap outBitmap = mWebpBitmapFactory.decodeStream(
        getTestWebpInputStream(),
        null,
        options);

    assertNotNull("Bitmap should not be null", outBitmap);
    assertSame("Output bitmap shuold be the same as input bitmap", inBitmap, outBitmap);
    assertEquals("Bitmap pixels should be red", 0xFFFF0100, outBitmap.getPixel(5, 8));
  }

  @Test
  public void testByteArrayDecode() throws Throwable {
    byte[] data = ByteStreams.toByteArray(getTestWebpInputStream());
    final Bitmap bitmap = mWebpBitmapFactory.decodeByteArray(
        data,
        0,
        data.length,
        null);

    testBitmapDefault(bitmap, 20, 20);
  }

  @Test
  public void testOutMimeType() throws Throwable {
    BitmapFactory.Options options = new BitmapFactory.Options();

    if (options.outMimeType != null) {
      // Not all devices are able to get this info from the image
      assertEquals("Mime type should be detected properly", "image/webp", options.outMimeType);
    }

    WebpBitmapFactoryImpl.hookDecodeStream(getTestJpegInputStream(), null, options);
    assertEquals("Mime type should be detected properly", "image/jpeg", options.outMimeType);
  }

  @Test
  public void testInTempStorage() throws Throwable {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inTempStorage = new byte[128 * 1024];

    Bitmap bitmap = mWebpBitmapFactory.decodeStream(
        getTestWebpInputStream(),
        null,
        options);
    testBitmapDefault(bitmap, 20, 20);
  }

  @Test
  public void testInSampleSize() throws Throwable {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inSampleSize = 2;

    final Bitmap bitmap = mWebpBitmapFactory.decodeStream(
        getTestWebpInputStream(),
        null,
        options);

    testBitmapDefault(bitmap, 10, 10);
  }

  @Test
  public void testOutWidthHeight() throws Throwable {
    BitmapFactory.Options options = new BitmapFactory.Options();

    final Bitmap bitmap = mWebpBitmapFactory.decodeStream(
        getTestWebpInputStream(),
        null,
        options);

    assertNotNull("Bitmap should not be null", bitmap);
    assertEquals("Width should be scaled", 20, options.outWidth);
    assertEquals("Height should be scaled", 20, options.outHeight);
  }

  @Test
  public void testOutPadding() throws Throwable {
    Rect outPadding = new Rect();

    mWebpBitmapFactory.decodeStream(
        getTestWebpInputStream(),
        outPadding,
        null);

    assertNotNull("Padding rect should not be null", outPadding);
    assertEquals("Padding rect for webp should be not supported)", -1, outPadding.top);
    assertEquals("Padding rect for webp should be not supported)", -1, outPadding.left);
    assertEquals("Padding rect for webp should be not supported)", -1, outPadding.bottom);
    assertEquals("Padding rect for webp should be not supported)", -1, outPadding.right);
  }

  @Test
  public void testWebpFileDescriptorDecode() throws Throwable {
    FileDescriptor fd = getImageFileDescriptor("redsquare.webp");
    final Bitmap bitmap = mWebpBitmapFactory.decodeFileDescriptor(
        fd,
        null,
        null);

    testBitmapDefault(bitmap, 20, 20);
  }

  @Test
  public void testJpegFileDescriptorDecode() throws Throwable {
    FileDescriptor fd = getImageFileDescriptor("redsquare.jpg");
    final Bitmap bitmap = mWebpBitmapFactory.decodeFileDescriptor(
        fd,
        null,
        null);

    testBitmapDefault(bitmap, 20, 20);
  }

  @Test
  public void testInScaled() throws Throwable {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inScaled = true;
    options.inScreenDensity = 240;
    options.inDensity = 480;
    options.inTargetDensity = 240;

    final Bitmap bitmap = mWebpBitmapFactory.decodeStream(
        getTestWebpInputStream(),
        null,
        options);

    testBitmapDefault(bitmap, 10, 10);
  }

  @Test
  public void testInScaled2() throws Throwable {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inScaled = true;
    options.inScreenDensity = 480;
    options.inDensity = 480;
    options.inTargetDensity = 240;

    final Bitmap bitmap = mWebpBitmapFactory.decodeStream(
        getTestWebpInputStream(),
        null,
        options);

    testBitmapDefault(bitmap, 20, 20);
  }

  @Test
  public void testInScaled3() throws Throwable {
    BitmapFactory.Options options = new BitmapFactory.Options();
    options.inScaled = true;
    options.inScreenDensity = 240;
    options.inDensity = 0;
    options.inTargetDensity = 240;

    final Bitmap bitmap = mWebpBitmapFactory.decodeStream(
        getTestWebpInputStream(),
        null,
        options);

    testBitmapDefault(bitmap, 20, 20);
  }

  private void testBitmapDefault(Bitmap bitmap, int width, int height) {
    assertNotNull("Bitmap should not be null", bitmap);
    assertEquals("Width should be set properly", width, bitmap.getWidth());
    assertEquals("Height should be set properly", height, bitmap.getHeight());

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
      // TODO 8639341 - In GB colors are not decoded exactly as they are into the image
      assertEquals("Bitmap pixels should be red", 0xFFFF0100, bitmap.getPixel(1, 1));
    }
  }
}
