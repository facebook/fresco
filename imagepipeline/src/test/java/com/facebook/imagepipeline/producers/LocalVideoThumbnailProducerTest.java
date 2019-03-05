/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.*;

import android.graphics.Bitmap;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.provider.MediaStore;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.testing.FakeClock;
import com.facebook.imagepipeline.testing.TestExecutorService;
import java.io.File;
import java.util.Map;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.Mock;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.powermock.core.classloader.annotations.*;
import org.powermock.modules.junit4.rule.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

/** Basic tests for {@link LocalVideoThumbnailProducer} */
@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
@PrepareForTest(android.media.ThumbnailUtils.class)
@Config(manifest = Config.NONE)
public class LocalVideoThumbnailProducerTest {
  private static final String PRODUCER_NAME = LocalVideoThumbnailProducer.PRODUCER_NAME;
  private static final String TEST_FILENAME = "dummy.jpg";
  private static final android.net.Uri LOCAL_VIDEO_URI = Uri.parse("file:///dancing_hotdog.mp4");

  @Mock public PooledByteBufferFactory mPooledByteBufferFactory;
  @Mock public Consumer<CloseableReference<CloseableImage>> mConsumer;
  @Mock public ImageRequest mImageRequest;
  @Mock public ProducerListener mProducerListener;
  @Mock public Exception mException;
  @Mock public Bitmap mBitmap;

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  private TestExecutorService mExecutor;
  private SettableProducerContext mProducerContext;
  private final String mRequestId = "mRequestId";
  private File mFile;
  private LocalVideoThumbnailProducer mLocalVideoThumbnailProducer;
  private CloseableReference<CloseableStaticBitmap> mCloseableReference;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    mExecutor = new TestExecutorService(new FakeClock());
    mLocalVideoThumbnailProducer = new LocalVideoThumbnailProducer(
        mExecutor,
        RuntimeEnvironment.application.getContentResolver());
    mFile = new File(RuntimeEnvironment.application.getExternalFilesDir(null), TEST_FILENAME);

    mockStatic(ThumbnailUtils.class);
    mProducerContext = new SettableProducerContext(
        mImageRequest,
        mRequestId,
        mProducerListener,
        mock(Object.class),
        ImageRequest.RequestLevel.FULL_FETCH,
        false,
        false,
        Priority.MEDIUM);
    when(mImageRequest.getSourceFile()).thenReturn(mFile);
  }

  @Test
  public void testLocalVideoThumbnailCancelled() {
    mLocalVideoThumbnailProducer.produceResults(mConsumer, mProducerContext);
    mProducerContext.cancel();
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithCancellation(mRequestId, PRODUCER_NAME, null);
    verify(mProducerListener, never())
        .onUltimateProducerReached(anyString(), anyString(), anyBoolean());
    verify(mConsumer).onCancellation();
  }

  @Test
  public void testLocalVideoMiniThumbnailSuccess() throws Exception {
    when(mImageRequest.getPreferredWidth()).thenReturn(100);
    when(mImageRequest.getPreferredHeight()).thenReturn(100);
    when(mImageRequest.getSourceUri()).thenReturn(LOCAL_VIDEO_URI);
    when(
        android.media.ThumbnailUtils.createVideoThumbnail(
            mFile.getPath(), MediaStore.Images.Thumbnails.MINI_KIND))
        .thenReturn(mBitmap);
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            mCloseableReference = ((CloseableReference) invocation.getArguments()[0]).clone();
            return null;
          }
        }).when(mConsumer).onNewResult(any(CloseableReference.class), eq(Consumer.IS_LAST));
    mLocalVideoThumbnailProducer.produceResults(mConsumer, mProducerContext);
    mExecutor.runUntilIdle();
    assertEquals(1, mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertEquals(
        mBitmap,
        mCloseableReference.getUnderlyingReferenceTestOnly().get().getUnderlyingBitmap());
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithSuccess(mRequestId, PRODUCER_NAME, null);
    verify(mProducerListener).onUltimateProducerReached(mRequestId, PRODUCER_NAME, true);
  }

  @Test
  public void testLocalVideoMicroThumbnailSuccess() throws Exception {
    when(mImageRequest.getSourceUri()).thenReturn(LOCAL_VIDEO_URI);
    when(mProducerListener.requiresExtraMap(mRequestId)).thenReturn(true);
    when(
        android.media.ThumbnailUtils.createVideoThumbnail(
            mFile.getPath(), MediaStore.Images.Thumbnails.MICRO_KIND))
        .thenReturn(mBitmap);
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            mCloseableReference = ((CloseableReference) invocation.getArguments()[0]).clone();
            return null;
          }
        }).when(mConsumer).onNewResult(any(CloseableReference.class), eq(Consumer.IS_LAST));
    mLocalVideoThumbnailProducer.produceResults(mConsumer, mProducerContext);
    mExecutor.runUntilIdle();
    assertEquals(1, mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertEquals(
        mBitmap,
        mCloseableReference.getUnderlyingReferenceTestOnly().get().getUnderlyingBitmap());
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> thumbnailFoundMap =
        ImmutableMap.of(LocalVideoThumbnailProducer.CREATED_THUMBNAIL, "true");
    verify(mProducerListener).onProducerFinishWithSuccess(
        mRequestId, PRODUCER_NAME, thumbnailFoundMap);
    verify(mProducerListener).onUltimateProducerReached(mRequestId, PRODUCER_NAME, true);
  }

  @Test
  public void testLocalVideoMicroThumbnailReturnsNull() throws Exception {
    when(mProducerListener.requiresExtraMap(mRequestId)).thenReturn(true);
    when(
        android.media.ThumbnailUtils.createVideoThumbnail(
            mFile.getPath(), MediaStore.Images.Thumbnails.MICRO_KIND))
        .thenReturn(null);
    mLocalVideoThumbnailProducer.produceResults(mConsumer, mProducerContext);
    mExecutor.runUntilIdle();
    verify(mConsumer).onNewResult(null, Consumer.IS_LAST);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    Map<String, String> thumbnailNotFoundMap =
        ImmutableMap.of(LocalVideoThumbnailProducer.CREATED_THUMBNAIL, "false");
    verify(mProducerListener).onProducerFinishWithSuccess(
        mRequestId, PRODUCER_NAME, thumbnailNotFoundMap);
    verify(mProducerListener).onUltimateProducerReached(mRequestId, PRODUCER_NAME, false);
  }

  @Test(expected = RuntimeException.class)
  public void testFetchLocalFileFailsByThrowing() throws Exception {
    when(
        android.media.ThumbnailUtils.createVideoThumbnail(
            mFile.getPath(), MediaStore.Images.Thumbnails.MICRO_KIND))
        .thenThrow(mException);
    verify(mConsumer).onFailure(mException);
    verify(mProducerListener).onProducerStart(mRequestId, PRODUCER_NAME);
    verify(mProducerListener).onProducerFinishWithFailure(
        mRequestId, PRODUCER_NAME, mException, null);
    verify(mProducerListener).onUltimateProducerReached(mRequestId, PRODUCER_NAME, false);
  }

  @After
  public void tearDown() throws Exception {
    mFile.delete();
  }
}
