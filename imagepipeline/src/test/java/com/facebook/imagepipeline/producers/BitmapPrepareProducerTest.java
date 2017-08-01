/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
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

  @Mock public Producer<CloseableReference<CloseableImage>> mInputProducer;
  @Mock public Consumer<CloseableReference<CloseableImage>> mConsumer;
  @Mock public ProducerContext mProducerContext;

  private CloseableReference<CloseableImage> mImageReference;
  @Mock private CloseableStaticBitmap mCloseableStaticBitmap;
  @Mock private Bitmap mBitmap;

  private BitmapPrepareProducer mBitmapPrepareProducer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mBitmapPrepareProducer = new BitmapPrepareProducer(mInputProducer);

    mImageReference = CloseableReference.of((CloseableImage) mCloseableStaticBitmap);
    when(mCloseableStaticBitmap.getUnderlyingBitmap()).thenReturn(mBitmap);

    doAnswer(new Answer() {
      @Override
      public Object answer(InvocationOnMock invocation) throws Throwable {
        Object[] args = invocation.getArguments();
        Consumer<CloseableReference<CloseableImage>> consumer =
            (Consumer<CloseableReference<CloseableImage>>) args[0];
        consumer.onNewResult(mImageReference, 0);
        return null;
      }
    }).when(mInputProducer).produceResults(any(Consumer.class), any(ProducerContext.class));
  }

  @Test
  public void testProduceResults_whenCalled_thenInputProducerCalled() {
    mBitmapPrepareProducer.produceResults(mConsumer, mProducerContext);

    verify(mInputProducer, times(1)).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  @Test
  public void testProduceResults_whenPrefetch_thenPassThrough() {
    when(mProducerContext.isPrefetch()).thenReturn(true);

    mBitmapPrepareProducer.produceResults(mConsumer, mProducerContext);

    // note: the given consumer is used and not the BitmapPrepareConsumer
    verify(mInputProducer, times(1)).produceResults(eq(mConsumer), eq(mProducerContext));
  }

  @Test
  public void testProduceResults_whenNotPrefetch_thenBitmapPrepareToDrawCalled() {
    when(mProducerContext.isPrefetch()).thenReturn(false);

    mBitmapPrepareProducer.produceResults(mConsumer, mProducerContext);

    verify(mBitmap, times(1)).prepareToDraw();
  }

  @Test
  public void testProduceResults_whenPrefetch_thenBitmapPrepareToDrawCalled() {
    when(mProducerContext.isPrefetch()).thenReturn(true);

    mBitmapPrepareProducer.produceResults(mConsumer, mProducerContext);

    verify(mBitmap, never()).prepareToDraw();
  }
}
