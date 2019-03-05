/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.content.ContentResolver;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Pair;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.imageformat.DefaultImageFormats;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.testing.FakeClock;
import com.facebook.imagepipeline.testing.TestExecutorService;
import com.facebook.imageutils.BitmapUtil;
import com.facebook.imageutils.JfifUtil;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.Executor;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.Mock;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.powermock.api.mockito.*;
import org.powermock.core.classloader.annotations.*;
import org.powermock.modules.junit4.rule.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
@Config(manifest = Config.NONE)
@PrepareForTest({JfifUtil.class, BitmapUtil.class})
public class LocalExifThumbnailProducerTest {

  private static final int WIDTH = 10;
  private static final int HEIGHT = 20;
  private static final int ORIENTATION = 8;
  private static final int ANGLE = 270;
  @Mock public ExifInterface mExifInterface;
  @Mock public ImageRequest mImageRequest;
  @Mock public ProducerListener mProducerListener;
  @Mock public Consumer<EncodedImage> mConsumer;
  @Mock public ProducerContext mProducerContext;
  @Mock public PooledByteBufferFactory mPooledByteBufferFactory;
  @Mock public PooledByteBuffer mThumbnailByteBuffer;
  @Mock public File mFile;
  @Mock public ContentResolver mContentResolver;

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  private final Uri mUri = Uri.parse("/dummy/path");
  private byte[] mThumbnailBytes;
  private TestExecutorService mTestExecutorService;
  private TestLocalExifThumbnailProducer mTestLocalExifThumbnailProducer;
  private EncodedImage mCapturedEncodedImage;

  @Before
  public void setUp() throws IOException {
    MockitoAnnotations.initMocks(this);
    PowerMockito.mockStatic(JfifUtil.class, BitmapUtil.class);
    mTestExecutorService = new TestExecutorService(new FakeClock());

    mTestLocalExifThumbnailProducer = new TestLocalExifThumbnailProducer(
        mTestExecutorService,
        mPooledByteBufferFactory,
        mContentResolver);

    when(mProducerContext.getImageRequest()).thenReturn(mImageRequest);
    when(mImageRequest.getSourceUri()).thenReturn(mUri);
    when(mProducerContext.getListener()).thenReturn(mProducerListener);

    mThumbnailBytes = new byte[100];
    when(mExifInterface.hasThumbnail()).thenReturn(true);
    when(mExifInterface.getThumbnail()).thenReturn(mThumbnailBytes);
    when(mPooledByteBufferFactory.newByteBuffer(mThumbnailBytes))
        .thenReturn(mThumbnailByteBuffer);

    when(mExifInterface.getAttribute(ExifInterface.TAG_ORIENTATION))
        .thenReturn(Integer.toString(ORIENTATION));
    when(JfifUtil.getAutoRotateAngleFromOrientation(ORIENTATION)).thenReturn(ANGLE);
    when(BitmapUtil.decodeDimensions(any(InputStream.class))).thenReturn(new Pair(WIDTH, HEIGHT));

    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            mCapturedEncodedImage = EncodedImage.cloneOrNull(
                (EncodedImage) invocation.getArguments()[0]);
            return null;
          }
        })
        .when(mConsumer)
        .onNewResult(notNull(EncodedImage.class), anyInt());
  }

  @Test
  public void testFindExifThumbnail() {
    mTestLocalExifThumbnailProducer.produceResults(mConsumer, mProducerContext);
    mTestExecutorService.runUntilIdle();
    // Should have 2 references open: The cloned reference when the argument is
    // captured by EncodedImage and the one that is created when
    // getByteBufferRef is called on EncodedImage
    assertEquals(
        2,
        mCapturedEncodedImage.
            getByteBufferRef().getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertSame(mThumbnailByteBuffer, mCapturedEncodedImage.getByteBufferRef().get());
    assertEquals(DefaultImageFormats.JPEG, mCapturedEncodedImage.getImageFormat());
    assertEquals(WIDTH, mCapturedEncodedImage.getWidth());
    assertEquals(HEIGHT, mCapturedEncodedImage.getHeight());
    assertEquals(ANGLE, mCapturedEncodedImage.getRotationAngle());
  }

  @Test
  public void testNoExifThumbnail() {
    when(mExifInterface.hasThumbnail()).thenReturn(false);
    mTestLocalExifThumbnailProducer.produceResults(mConsumer, mProducerContext);
    mTestExecutorService.runUntilIdle();
    verify(mConsumer).onNewResult(null, Consumer.IS_LAST);
  }

  private class TestLocalExifThumbnailProducer extends LocalExifThumbnailProducer {

    private TestLocalExifThumbnailProducer(
        Executor executor,
        PooledByteBufferFactory pooledByteBufferFactory,
        ContentResolver contentResolver) {
      super(executor, pooledByteBufferFactory, contentResolver);
    }

    @Override
    ExifInterface getExifInterface(Uri uri) {
      if (uri.equals(mUri)) {
        return mExifInterface;
      }
      return null;
    }
  }
}
