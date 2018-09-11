/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.graphics.Bitmap;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.image.CloseableStaticBitmap;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class BitmapPrepareProducerTest {

  public static final int MIN_BITMAP_SIZE_BYTES = 1000;
  public static final int MAX_BITMAP_SIZE_BYTES = 2000;

  @Mock public Producer<CloseableReference<CloseableImage>> mInputProducer;
  @Mock public Consumer<CloseableReference<CloseableImage>> mConsumer;
  @Mock public ProducerContext mProducerContext;

  private CloseableReference<CloseableImage> mImageReference;
  @Mock private CloseableStaticBitmap mCloseableStaticBitmap;
  @Mock private Bitmap mBitmap;

  private BitmapPrepareProducer mBitmapPrepareProducer;

  @Before
  public void setup() {
    MockitoAnnotations.initMocks(this);

    mImageReference = CloseableReference.of((CloseableImage) mCloseableStaticBitmap);
    when(mCloseableStaticBitmap.getUnderlyingBitmap()).thenReturn(mBitmap);

    // 100 * 15 = 1500 (between MIN_BITMAP_SIZE_BYTES and MAX_BITMAP_SIZE_BYTES)
    when(mBitmap.getRowBytes()).thenReturn(100);
    when(mBitmap.getHeight()).thenReturn(15);

    doAnswer(
            new Answer() {
              @Override
              public Object answer(InvocationOnMock invocation) throws Throwable {
                Object[] args = invocation.getArguments();
                Consumer<CloseableReference<CloseableImage>> consumer =
                    (Consumer<CloseableReference<CloseableImage>>) args[0];
                consumer.onNewResult(mImageReference, 0);
                return null;
              }
            })
        .when(mInputProducer)
        .produceResults(any(Consumer.class), any(ProducerContext.class));
  }

  @Test
  public void testProduceResults_whenCalled_thenInputProducerCalled() {
    createBitmapPrepareProducer(false);

    mBitmapPrepareProducer.produceResults(mConsumer, mProducerContext);

    verify(mInputProducer, times(1)).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  @Test
  public void testProduceResults_whenPrefetch_andPreparePrefetchNotEnabled_thenPassThrough() {
    createBitmapPrepareProducer(false);

    when(mProducerContext.isPrefetch()).thenReturn(true);

    mBitmapPrepareProducer.produceResults(mConsumer, mProducerContext);

    // note: the given consumer is used and not the BitmapPrepareConsumer
    verify(mInputProducer, times(1)).produceResults(eq(mConsumer), eq(mProducerContext));
  }

  @Test
  public void testProduceResults_whenPrefetch_andPreparePrefetchEnabled_thenNotPassThrough() {
    createBitmapPrepareProducer(true);

    when(mProducerContext.isPrefetch()).thenReturn(true);

    mBitmapPrepareProducer.produceResults(mConsumer, mProducerContext);

    // note: the given consumer is used and not the BitmapPrepareConsumer
    verify(mInputProducer, never()).produceResults(eq(mConsumer), eq(mProducerContext));
  }

  @Test
  public void
      testProduceResults_whenPrefetch_andPreparePrefetchNotEnabled_thenBitmapPrepareToDrawNotCalled() {
    createBitmapPrepareProducer(false);

    when(mProducerContext.isPrefetch()).thenReturn(true);

    mBitmapPrepareProducer.produceResults(mConsumer, mProducerContext);

    verify(mBitmap, never()).prepareToDraw();
  }

  @Test
  public void
      testProduceResults_whenPrefetch_andPreparePrefetchEnabled_thenBitmapPrepareToDrawCalled() {
    createBitmapPrepareProducer(true);

    when(mProducerContext.isPrefetch()).thenReturn(true);

    mBitmapPrepareProducer.produceResults(mConsumer, mProducerContext);

    verify(mBitmap, times(1)).prepareToDraw();
  }

  @Test
  public void testProduceResults_whenNotPrefetch_thenBitmapPrepareToDrawCalled() {
    createBitmapPrepareProducer(false);

    when(mProducerContext.isPrefetch()).thenReturn(false);

    mBitmapPrepareProducer.produceResults(mConsumer, mProducerContext);

    verify(mBitmap, times(1)).prepareToDraw();
  }

  @Test
  public void
      testProduceResults_whenNotPrefetchButBitmapTooSmall_thenBitmapPrepareToDrawNotCalled() {
    createBitmapPrepareProducer(false);

    when(mProducerContext.isPrefetch()).thenReturn(false);

    // 100 * 9 = 900 (< MIN_BITMAP_SIZE_BYTES)
    when(mBitmap.getRowBytes()).thenReturn(100);
    when(mBitmap.getHeight()).thenReturn(9);

    mBitmapPrepareProducer.produceResults(mConsumer, mProducerContext);

    verify(mBitmap, never()).prepareToDraw();
  }

  @Test
  public void
      testProduceResults_whenNotPrefetchButBitmapTooLarge_thenBitmapPrepareToDrawNotCalled() {
    createBitmapPrepareProducer(false);

    when(mProducerContext.isPrefetch()).thenReturn(false);

    // 100 * 21 = 2100 (> MAX_BITMAP_SIZE_BYTES)
    when(mBitmap.getRowBytes()).thenReturn(100);
    when(mBitmap.getHeight()).thenReturn(21);
    when(mBitmap.getByteCount()).thenReturn(MAX_BITMAP_SIZE_BYTES + 1);

    mBitmapPrepareProducer.produceResults(mConsumer, mProducerContext);

    verify(mBitmap, never()).prepareToDraw();
  }

  private void createBitmapPrepareProducer(boolean preparePrefetch) {
    mBitmapPrepareProducer =
        new BitmapPrepareProducer(
            mInputProducer, MIN_BITMAP_SIZE_BYTES, MAX_BITMAP_SIZE_BYTES, preparePrefetch);
  }
}
