/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import android.graphics.Bitmap;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.producers.PostprocessorProducer.RepeatedPostprocessorConsumer;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.RepeatedPostprocessor;
import com.facebook.imagepipeline.request.RepeatedPostprocessorRunner;
import com.facebook.imagepipeline.testing.FakeClock;
import com.facebook.imagepipeline.testing.TestExecutorService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class RepeatedPostprocessorProducerTest {

  private static final String POSTPROCESSOR_NAME = "postprocessor_name";
  private static final Map<String, String> mExtraMap =
      ImmutableMap.of(PostprocessorProducer.POSTPROCESSOR, POSTPROCESSOR_NAME);

  @Mock public PlatformBitmapFactory mPlatformBitmapFactory;
  @Mock public ProducerListener mProducerListener;
  @Mock public Producer<CloseableReference<CloseableImage>> mInputProducer;
  @Mock public Consumer<CloseableReference<CloseableImage>> mConsumer;
  @Mock public RepeatedPostprocessor mPostprocessor;
  @Mock public ResourceReleaser<Bitmap> mBitmapResourceReleaser;

  @Mock public ImageRequest mImageRequest;

  private SettableProducerContext mProducerContext;
  private String mRequestId = "mRequestId";
  private Bitmap mSourceBitmap;
  private CloseableStaticBitmap mSourceCloseableStaticBitmap;
  private CloseableReference<CloseableImage> mSourceCloseableImageRef;
  private Bitmap mDestinationBitmap;
  private CloseableReference<Bitmap> mDestinationCloseableBitmapRef;
  private TestExecutorService mTestExecutorService;
  private PostprocessorProducer mPostprocessorProducer;
  private List<CloseableReference<CloseableImage>> mResults;

  private InOrder mInOrder;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mTestExecutorService = new TestExecutorService(new FakeClock());
    mPostprocessorProducer =
        new PostprocessorProducer(
            mInputProducer,
            mPlatformBitmapFactory,
            mTestExecutorService);
    mProducerContext =
        new SettableProducerContext(
            mImageRequest,
            mRequestId,
            mProducerListener,
            mock(Object.class),
            ImageRequest.RequestLevel.FULL_FETCH,
            false /* isPrefetch */,
            false /* isIntermediateResultExpected */,
            Priority.MEDIUM);
    when(mImageRequest.getPostprocessor()).thenReturn(mPostprocessor);
    mResults = new ArrayList<>();
    when(mPostprocessor.getName()).thenReturn(POSTPROCESSOR_NAME);
    when(mProducerListener.requiresExtraMap(mRequestId)).thenReturn(true);
    doAnswer(
        new Answer<Object>() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            mResults.add(
                ((CloseableReference<CloseableImage>) invocation.getArguments()[0]).clone());
            return null;
          }
        }
    ).when(mConsumer).onNewResult(any(CloseableReference.class), anyInt());
    mInOrder = inOrder(mPostprocessor, mProducerListener, mConsumer);
  }

  @Test
  public void testIntermediateImageIsNotProcessed() {
    RepeatedPostprocessorConsumer postprocessorConsumer = produceResults();
    RepeatedPostprocessorRunner repeatedPostprocessorRunner = getRunner();

    setupNewSourceImage();
    postprocessorConsumer.onNewResult(mSourceCloseableImageRef, Consumer.NO_FLAGS);
    mSourceCloseableImageRef.close();
    mTestExecutorService.runUntilIdle();

    mInOrder.verifyNoMoreInteractions();
    assertEquals(0, mResults.size());
    verify(mSourceCloseableStaticBitmap).close();

    // final result should be post-processed
    performNewResult(postprocessorConsumer, true);
    verifyNewResultProcessed(0);

    performCancelAndVerifyOnCancellation();
    verify(mSourceCloseableStaticBitmap).close();
  }

  @Test
  public void testPostprocessSuccessful() {
    RepeatedPostprocessorConsumer postprocessorConsumer = produceResults();
    RepeatedPostprocessorRunner repeatedPostprocessorRunner = getRunner();

    performNewResult(postprocessorConsumer, true);
    verifyNewResultProcessed(0);

    performCancelAndVerifyOnCancellation();
    verify(mSourceCloseableStaticBitmap).close();
  }

  @Test
  public void testMultiplePostprocessThenClose() {
    RepeatedPostprocessorConsumer postprocessorConsumer = produceResults();
    RepeatedPostprocessorRunner repeatedPostprocessorRunner = getRunner();

    performNewResult(postprocessorConsumer, true);
    verifyNewResultProcessed(0);

    performUpdate(repeatedPostprocessorRunner, true);
    verifyNewResultProcessed(1);

    performUpdate(repeatedPostprocessorRunner, true);
    verifyNewResultProcessed(2);

    performUpdate(repeatedPostprocessorRunner, true);
    verifyNewResultProcessed(3);

    performCancelAndVerifyOnCancellation();
    verify(mSourceCloseableStaticBitmap).close();

    // Can't update now that the request is cancelled.
    performUpdate(repeatedPostprocessorRunner, true);
    mInOrder.verifyNoMoreInteractions();
  }

  @Test
  public void testCannotPostprocessAfterPostprocessFailure() {
    RepeatedPostprocessorConsumer postprocessorConsumer = produceResults();
    RepeatedPostprocessorRunner repeatedPostprocessorRunner = getRunner();

    performNewResult(postprocessorConsumer, true);
    verifyNewResultProcessed(0);

    performFailure(repeatedPostprocessorRunner);

    mInOrder.verify(mProducerListener).onProducerStart(mRequestId, PostprocessorProducer.NAME);
    mInOrder.verify(mPostprocessor).process(mSourceBitmap, mPlatformBitmapFactory);
    mInOrder.verify(mProducerListener).requiresExtraMap(mRequestId);
    mInOrder.verify(mProducerListener).onProducerFinishWithFailure(
        eq(mRequestId),
        eq(PostprocessorProducer.NAME),
        any(RuntimeException.class),
        eq(mExtraMap));
    mInOrder.verify(mConsumer).onFailure(any(RuntimeException.class));
    mInOrder.verifyNoMoreInteractions();

    // Can't update now that the request has failed.
    performUpdate(repeatedPostprocessorRunner, true);
    mInOrder.verifyNoMoreInteractions();

    performCancelAfterFinished();
    verify(mSourceCloseableStaticBitmap).close();
  }

  @Test
  public void testUpdateBeforeNewResultDoesNothing() {
    RepeatedPostprocessorConsumer postprocessorConsumer = produceResults();
    RepeatedPostprocessorRunner repeatedPostprocessorRunner = getRunner();

    performUpdate(repeatedPostprocessorRunner, true);
    performUpdate(repeatedPostprocessorRunner, true);
    mInOrder.verifyNoMoreInteractions();

    performNewResult(postprocessorConsumer, true);
    verifyNewResultProcessed(0);
  }

  @Test
  public void runPostprocessAgainWhenDirty() {
    final RepeatedPostprocessorConsumer postprocessorConsumer = produceResults();
    final RepeatedPostprocessorRunner repeatedPostprocessorRunner = getRunner();

    performNewResult(postprocessorConsumer, false);

    Bitmap destBitmap0 = mDestinationBitmap;
    performUpdateDuringTheNextPostprocessing(repeatedPostprocessorRunner);
    mTestExecutorService.runNextPendingCommand();
    verifyNewResultProcessed(0, destBitmap0);

    Bitmap destBitmap1 = mDestinationBitmap;
    performUpdateDuringTheNextPostprocessing(repeatedPostprocessorRunner);
    mTestExecutorService.runNextPendingCommand();
    verifyNewResultProcessed(1, destBitmap1);

    Bitmap destBitmap2 = mDestinationBitmap;
    mTestExecutorService.runNextPendingCommand();
    verifyNewResultProcessed(2, destBitmap2);
  }

  private void setupNewSourceImage() {
    mSourceBitmap = mock(Bitmap.class);
    mSourceCloseableStaticBitmap = mock(CloseableStaticBitmap.class);
    when(mSourceCloseableStaticBitmap.getUnderlyingBitmap()).thenReturn(mSourceBitmap);
    mSourceCloseableImageRef =
        CloseableReference.<CloseableImage>of(mSourceCloseableStaticBitmap);
  }

  private void setupNewDestinationImage() {
    mDestinationBitmap = mock(Bitmap.class);
    mDestinationCloseableBitmapRef =
        CloseableReference.of(mDestinationBitmap, mBitmapResourceReleaser);
    doReturn(mDestinationCloseableBitmapRef)
        .when(mPostprocessor).process(mSourceBitmap, mPlatformBitmapFactory);
  }

  private RepeatedPostprocessorConsumer produceResults() {
    mPostprocessorProducer.produceResults(mConsumer, mProducerContext);
    ArgumentCaptor<Consumer> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
    verify(mInputProducer).produceResults(consumerCaptor.capture(), eq(mProducerContext));
    return (RepeatedPostprocessorConsumer) consumerCaptor.getValue();
  }

  private RepeatedPostprocessorRunner getRunner() {
    ArgumentCaptor<RepeatedPostprocessorRunner> captor =
        ArgumentCaptor.forClass(RepeatedPostprocessorRunner.class);
    mInOrder.verify(mPostprocessor).setCallback(captor.capture());
    return captor.getValue();
  }

  private void performNewResult(RepeatedPostprocessorConsumer postprocessorConsumer, boolean run) {
    setupNewSourceImage();
    setupNewDestinationImage();
    postprocessorConsumer.onNewResult(mSourceCloseableImageRef, Consumer.IS_LAST);
    mSourceCloseableImageRef.close();
    if (run) {
      mTestExecutorService.runUntilIdle();
    }
  }

  private void performUpdate(RepeatedPostprocessorRunner repeatedPostprocessorRunner, boolean run) {
    setupNewDestinationImage();
    repeatedPostprocessorRunner.update();
    if (run) {
      mTestExecutorService.runUntilIdle();
    }
  }

  private void performUpdateDuringTheNextPostprocessing(
      final RepeatedPostprocessorRunner repeatedPostprocessorRunner) {
    doAnswer(
        new Answer<CloseableReference<Bitmap>>() {
          @Override
          public CloseableReference<Bitmap> answer(InvocationOnMock invocation) throws Throwable {
            CloseableReference<Bitmap> destBitmapRef = mDestinationCloseableBitmapRef;
            performUpdate(repeatedPostprocessorRunner, false);
            // the following call should be ignored
            performUpdate(repeatedPostprocessorRunner, false);
            return destBitmapRef;
          }
        }).when(mPostprocessor).process(mSourceBitmap, mPlatformBitmapFactory);
  }

  private void performFailure(RepeatedPostprocessorRunner repeatedPostprocessorRunner) {
    setupNewDestinationImage();
    doThrow(new RuntimeException())
        .when(mPostprocessor).process(mSourceBitmap, mPlatformBitmapFactory);
    repeatedPostprocessorRunner.update();
    mTestExecutorService.runUntilIdle();
  }

  private void performCancelAndVerifyOnCancellation() {
    performCancel();
    mInOrder.verify(mConsumer).onCancellation();
  }

  private void performCancelAfterFinished() {
    performCancel();
    mInOrder.verify(mConsumer, never()).onCancellation();
  }

  private void performCancel() {
    mProducerContext.cancel();
    mTestExecutorService.runUntilIdle();
  }

  private void verifyNewResultProcessed(int index) {
    verifyNewResultProcessed(index, mDestinationBitmap);
  }

  private void verifyNewResultProcessed(int index, Bitmap destBitmap) {
    mInOrder.verify(mProducerListener).onProducerStart(mRequestId, PostprocessorProducer.NAME);
    mInOrder.verify(mPostprocessor).process(mSourceBitmap, mPlatformBitmapFactory);
    mInOrder.verify(mProducerListener).requiresExtraMap(mRequestId);
    mInOrder.verify(mProducerListener)
        .onProducerFinishWithSuccess(mRequestId, PostprocessorProducer.NAME, mExtraMap);
    mInOrder.verify(mConsumer).onNewResult(any(CloseableReference.class), eq(Consumer.NO_FLAGS));
    mInOrder.verifyNoMoreInteractions();

    assertEquals(index + 1, mResults.size());
    CloseableReference<CloseableImage> res0 = mResults.get(index);
    assertTrue(CloseableReference.isValid(res0));
    assertSame(destBitmap, ((CloseableStaticBitmap) res0.get()).getUnderlyingBitmap());
    res0.close();
    verify(mBitmapResourceReleaser).release(destBitmap);
  }
}
