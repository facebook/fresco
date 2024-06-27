/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import android.graphics.Bitmap;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.references.ResourceReleaser;
import com.facebook.imagepipeline.bitmaps.PlatformBitmapFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import com.facebook.imagepipeline.producers.PostprocessorProducer.SingleUsePostprocessorConsumer;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.Postprocessor;
import com.facebook.imagepipeline.testing.FakeClock;
import com.facebook.imagepipeline.testing.TestExecutorService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class SingleUsePostprocessorProducerTest {

  private static final String PRODUCER_NAME = PostprocessorProducer.NAME;
  private static final String POSTPROCESSOR_NAME = "postprocessor_name";
  private static final Map<String, String> mExtraMap =
      ImmutableMap.of(PostprocessorProducer.POSTPROCESSOR, POSTPROCESSOR_NAME);

  @Mock public PlatformBitmapFactory mPlatformBitmapFactory;
  @Mock public ProducerContext mProducerContext;
  @Mock public ProducerListener2 mProducerListener;
  @Mock public Producer<CloseableReference<CloseableImage>> mInputProducer;
  @Mock public Consumer<CloseableReference<CloseableImage>> mConsumer;
  @Mock public Postprocessor mPostprocessor;
  @Mock public ResourceReleaser<Bitmap> mBitmapResourceReleaser;

  @Mock public ImageRequest mImageRequest;

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
        new PostprocessorProducer(mInputProducer, mPlatformBitmapFactory, mTestExecutorService);

    when(mImageRequest.getPostprocessor()).thenReturn(mPostprocessor);
    when(mProducerContext.getId()).thenReturn(mRequestId);
    when(mProducerContext.getProducerListener()).thenReturn(mProducerListener);
    when(mProducerContext.getImageRequest()).thenReturn(mImageRequest);

    mResults = new ArrayList<>();
    when(mPostprocessor.getName()).thenReturn(POSTPROCESSOR_NAME);
    when(mProducerListener.requiresExtraMap(mProducerContext, PRODUCER_NAME)).thenReturn(true);
    doAnswer(
            new Answer<Object>() {
              @Nullable
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                mResults.add(
                    ((CloseableReference<CloseableImage>) invocation.getArguments()[0]).clone());
                return null;
              }
            })
        .when(mConsumer)
        .onNewResult(any(CloseableReference.class), anyInt());
    mInOrder = inOrder(mPostprocessor, mProducerListener, mConsumer);

    mSourceBitmap = mock(Bitmap.class);
    mSourceCloseableStaticBitmap = mock(CloseableStaticBitmap.class);
    when(mSourceCloseableStaticBitmap.getUnderlyingBitmap()).thenReturn(mSourceBitmap);
    mSourceCloseableImageRef = CloseableReference.<CloseableImage>of(mSourceCloseableStaticBitmap);
    mDestinationBitmap = mock(Bitmap.class);
    mDestinationCloseableBitmapRef =
        CloseableReference.of(mDestinationBitmap, mBitmapResourceReleaser);
  }

  @Test
  public void testIntermediateImageIsNotProcessed() {
    SingleUsePostprocessorConsumer postprocessorConsumer = produceResults();
    postprocessorConsumer.onNewResult(mSourceCloseableImageRef, Consumer.NO_FLAGS);
    mSourceCloseableImageRef.close();
    mTestExecutorService.runUntilIdle();

    mInOrder.verifyNoMoreInteractions();
    assertEquals(0, mResults.size());

    verify(mSourceCloseableStaticBitmap).close();
  }

  @Test
  public void testSuccess() {
    SingleUsePostprocessorConsumer postprocessorConsumer = produceResults();
    doReturn(mDestinationCloseableBitmapRef)
        .when(mPostprocessor)
        .process(mSourceBitmap, mPlatformBitmapFactory);
    postprocessorConsumer.onNewResult(mSourceCloseableImageRef, Consumer.IS_LAST);
    mSourceCloseableImageRef.close();
    mTestExecutorService.runUntilIdle();

    mInOrder
        .verify(mProducerListener)
        .onProducerStart(mProducerContext, PostprocessorProducer.NAME);
    mInOrder.verify(mPostprocessor).process(mSourceBitmap, mPlatformBitmapFactory);
    mInOrder
        .verify(mProducerListener)
        .requiresExtraMap(mProducerContext, PostprocessorProducer.NAME);
    mInOrder
        .verify(mProducerListener)
        .onProducerFinishWithSuccess(mProducerContext, PostprocessorProducer.NAME, mExtraMap);
    mInOrder.verify(mConsumer).onNewResult(any(CloseableReference.class), eq(Consumer.IS_LAST));
    mInOrder.verifyNoMoreInteractions();

    assertEquals(1, mResults.size());
    CloseableReference<CloseableImage> res0 = mResults.get(0);
    assertTrue(CloseableReference.isValid(res0));
    assertSame(mDestinationBitmap, ((CloseableStaticBitmap) res0.get()).getUnderlyingBitmap());
    res0.close();

    verify(mBitmapResourceReleaser).release(mDestinationBitmap);
    verify(mSourceCloseableStaticBitmap).close();
  }

  @Test
  public void testFailure() {
    SingleUsePostprocessorConsumer postprocessorConsumer = produceResults();
    doThrow(new RuntimeException())
        .when(mPostprocessor)
        .process(eq(mSourceBitmap), eq(mPlatformBitmapFactory));
    postprocessorConsumer.onNewResult(mSourceCloseableImageRef, Consumer.IS_LAST);
    mSourceCloseableImageRef.close();
    mTestExecutorService.runUntilIdle();

    mInOrder
        .verify(mProducerListener)
        .onProducerStart(mProducerContext, PostprocessorProducer.NAME);
    mInOrder.verify(mPostprocessor).process(mSourceBitmap, mPlatformBitmapFactory);
    mInOrder
        .verify(mProducerListener)
        .requiresExtraMap(mProducerContext, PostprocessorProducer.NAME);
    mInOrder
        .verify(mProducerListener)
        .onProducerFinishWithFailure(
            eq(mProducerContext),
            eq(PostprocessorProducer.NAME),
            any(RuntimeException.class),
            eq(mExtraMap));
    mInOrder.verify(mConsumer).onFailure(nullable(Throwable.class));
    mInOrder.verifyNoMoreInteractions();
    assertEquals(0, mResults.size());

    verify(mSourceCloseableStaticBitmap).close();
  }

  private SingleUsePostprocessorConsumer produceResults() {
    mPostprocessorProducer.produceResults(mConsumer, mProducerContext);
    ArgumentCaptor<Consumer> consumerCaptor = ArgumentCaptor.forClass(Consumer.class);
    verify(mInputProducer).produceResults(consumerCaptor.capture(), eq(mProducerContext));
    return (SingleUsePostprocessorConsumer) consumerCaptor.getValue();
  }
}
