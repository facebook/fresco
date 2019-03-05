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
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.testing.FakeClock;
import com.facebook.imagepipeline.testing.TestExecutorService;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.*;

/** Basic tests for LocalContentUriThumbnailFetchProducer */
@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
@PrepareForTest({LocalContentUriThumbnailFetchProducer.class, MediaStore.Images.class})
@Config(manifest = Config.NONE)
public class LocalContentUriThumbnailFetchProducerTest {
  private static final String PRODUCER_NAME = LocalContentUriThumbnailFetchProducer.PRODUCER_NAME;
  private static final String THUMBNAIL_FILE_NAME = "////sdcard/thumb.jpg";
  private static final long THUMBNAIL_FILE_SIZE = 1374;

  @Rule public PowerMockRule mPowerMockRule = new PowerMockRule();

  @Mock public PooledByteBufferFactory mPooledByteBufferFactory;
  @Mock public ContentResolver mContentResolver;
  @Mock public Consumer<EncodedImage> mConsumer;
  @Mock public ImageRequest mImageRequest;
  @Mock public ProducerListener mProducerListener;
  @Mock public Exception mException;
  @Mock public Cursor mCursor;
  @Mock public File mThumbnailFile;

  private TestExecutorService mExecutor;
  private SettableProducerContext mProducerContext;
  private final String mRequestId = "mRequestId";
  private Uri mContentUri;
  private LocalContentUriThumbnailFetchProducer mLocalContentUriThumbnailFetchProducer;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    mExecutor = new TestExecutorService(new FakeClock());
    mLocalContentUriThumbnailFetchProducer = new LocalContentUriThumbnailFetchProducer(
        mExecutor,
        mPooledByteBufferFactory,
        mContentResolver
    );
    mContentUri = Uri.parse("content://media/external/images/media/1");

    mProducerContext = new SettableProducerContext(
        mImageRequest,
        mRequestId,
        mProducerListener,
        mock(Object.class),
        ImageRequest.RequestLevel.FULL_FETCH,
        false,
        true,
        Priority.MEDIUM);
    when(mImageRequest.getSourceUri()).thenReturn(mContentUri);

    mockMediaStoreCursor();
    mockThumbnailFile();
    mockContentResolver();
  }

  private void mockMediaStoreCursor() {
    PowerMockito.mockStatic(MediaStore.Images.Thumbnails.class, File.class);
    PowerMockito.when(MediaStore.Images.Thumbnails
        .queryMiniThumbnail(any(ContentResolver.class), anyLong(), anyInt(), any(String[].class)))
        .thenReturn(mCursor);

    final int dataColumnIndex = 5;
    when(mCursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA)).thenReturn(dataColumnIndex);
    when(mCursor.getString(dataColumnIndex)).thenReturn(THUMBNAIL_FILE_NAME);
    when(mCursor.getCount()).thenReturn(1);
  }

  private void mockThumbnailFile() throws Exception {
    PowerMockito.whenNew(File.class)
        .withArguments(THUMBNAIL_FILE_NAME)
        .thenReturn(mThumbnailFile);
    when(mThumbnailFile.exists()).thenReturn(true);
    when(mThumbnailFile.length()).thenReturn(THUMBNAIL_FILE_SIZE);

    PowerMockito.whenNew(FileInputStream.class)
        .withArguments(THUMBNAIL_FILE_NAME)
        .thenReturn(mock(FileInputStream.class));

    EncodedImage encodedImage = mock(EncodedImage.class);
    when(encodedImage.getSize()).thenReturn((int) THUMBNAIL_FILE_SIZE);

    PowerMockito.whenNew(EncodedImage.class)
        .withAnyArguments()
        .thenReturn(encodedImage);
  }

  private void mockContentResolver() throws Exception {
    when(mContentResolver.query(
        eq(mContentUri),
        any(String[].class),
        any(String.class),
        any(String[].class),
        any(String.class))).thenReturn(mCursor);
    when(mContentResolver.openInputStream(mContentUri)).thenReturn(mock(InputStream.class));
  }

  @Test
  public void testLocalContentUriFetchCancelled() {
    mockResizeOptions(512, 384);

    produceResults();

    mProducerContext.cancel();
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithCancellation(mRequestId, PRODUCER_NAME, null);
    verify(mConsumer).onCancellation();
    mExecutor.runUntilIdle();
    verifyZeroInteractions(mPooledByteBufferFactory);
  }

  @Test
  public void testFetchLocalContentUri() throws Exception {
    mockResizeOptions(512, 384);

    PooledByteBuffer pooledByteBuffer = mock(PooledByteBuffer.class);
    when(mPooledByteBufferFactory.newByteBuffer(any(InputStream.class)))
        .thenReturn(pooledByteBuffer);

    produceResultsAndRunUntilIdle();

    assertConsumerReceivesImage();
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, null);
    verify(mProducerListener).onUltimateProducerReached(mRequestId, PRODUCER_NAME, true);
  }

  @Test(expected = RuntimeException.class)
  public void testFetchLocalContentUriFailsByThrowing() throws Exception {
    mockResizeOptions(512, 384);

    when(mPooledByteBufferFactory.newByteBuffer(any(InputStream.class))).thenThrow(mException);
    verify(mConsumer).onFailure(mException);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithFailure(
        mRequestId, PRODUCER_NAME, mException, null);
    verify(mProducerListener).onUltimateProducerReached(mRequestId, PRODUCER_NAME, false);
  }

  @Test
  public void testIsLargerThanThumbnailMaxSize() {
    mockResizeOptions(1000, 384);

    produceResultsAndRunUntilIdle();

    assertConsumerReceivesNull();
  }

  @Test
  public void testWithoutResizeOptions() {
    produceResultsAndRunUntilIdle();

    assertConsumerReceivesNull();
  }

  private void mockResizeOptions(int width, int height) {
    ResizeOptions resizeOptions = new ResizeOptions(width, height);
    when(mImageRequest.getResizeOptions()).thenReturn(resizeOptions);
  }

  private void produceResults() {
    mLocalContentUriThumbnailFetchProducer.produceResults(mConsumer, mProducerContext);
  }

  private void produceResultsAndRunUntilIdle() {
    mLocalContentUriThumbnailFetchProducer.produceResults(mConsumer, mProducerContext);
    mExecutor.runUntilIdle();
  }

  private void assertConsumerReceivesNull() {
    verify(mConsumer).onNewResult(null, Consumer.IS_LAST);
    verifyNoMoreInteractions(mConsumer);

    verifyZeroInteractions(mPooledByteBufferFactory);
  }

  private void assertConsumerReceivesImage() {
    ArgumentCaptor<EncodedImage> resultCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mConsumer).onNewResult(resultCaptor.capture(), eq(Consumer.IS_LAST));

    assertNotNull(resultCaptor.getValue());
    assertEquals(THUMBNAIL_FILE_SIZE, resultCaptor.getValue().getSize());

    verifyNoMoreInteractions(mConsumer);
  }
}
