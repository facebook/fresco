/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;

import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class RemoveImageTransformMetaDataProducerTest {
  @Mock public Producer mInputProducer;
  @Mock public Consumer<CloseableReference<PooledByteBuffer>> mConsumer;
  @Mock public EncodedImage mEncodedImage;
  @Mock public ProducerContext mProducerContext;
  @Mock public Exception mException;

  private RemoveImageTransformMetaDataProducer mRemoveMetaDataProducer;
  private Consumer<EncodedImage> mRemoveMetaDataConsumer;
  private PooledByteBuffer mIntermediateByteBuffer;
  private PooledByteBuffer mFinalByteBuffer;
  private CloseableReference<PooledByteBuffer> mIntermediateResult;
  private CloseableReference<PooledByteBuffer> mFinalResult;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mRemoveMetaDataProducer = new RemoveImageTransformMetaDataProducer(mInputProducer);

    mIntermediateByteBuffer = mock(PooledByteBuffer.class);
    mFinalByteBuffer = mock(PooledByteBuffer.class);

    mIntermediateResult = CloseableReference.of(mIntermediateByteBuffer);
    mFinalResult = CloseableReference.of(mFinalByteBuffer);

    mRemoveMetaDataConsumer = null;
    doAnswer(
        new Answer() {
          @Override
          public Object answer(InvocationOnMock invocation) throws Throwable {
            mRemoveMetaDataConsumer = (Consumer<EncodedImage>) invocation.getArguments()[0];
            return null;
          }
        }).when(mInputProducer).produceResults(any(Consumer.class), any(ProducerContext.class));
    mRemoveMetaDataProducer.produceResults(mConsumer, mProducerContext);
  }

  @Test
  public void testOnNewResult() {
    when(mEncodedImage.getByteBufferRef()).thenReturn(mIntermediateResult);
    when(mEncodedImage.isValid()).thenReturn(true);
    mRemoveMetaDataConsumer.onNewResult(mEncodedImage, false);
    ArgumentCaptor<CloseableReference> argumentCaptor =
        ArgumentCaptor.forClass(CloseableReference.class);
    verify(mConsumer).onNewResult(argumentCaptor.capture(), eq(false));
    CloseableReference intermediateResult = argumentCaptor.getValue();
    assertEquals(
        mIntermediateResult.getUnderlyingReferenceTestOnly().getRefCountTestOnly(),
        intermediateResult.getUnderlyingReferenceTestOnly().getRefCountTestOnly());

    when(mEncodedImage.getByteBufferRef()).thenReturn(mFinalResult);
    mRemoveMetaDataConsumer.onNewResult(mEncodedImage, true);
    verify(mConsumer).onNewResult(argumentCaptor.capture(), eq(false));
    CloseableReference finalResult = argumentCaptor.getValue();
    assertEquals(
        mFinalResult.getUnderlyingReferenceTestOnly().getRefCountTestOnly(),
        finalResult.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
  }

  @Test
  public void testOnNullResult() {
    mRemoveMetaDataConsumer.onNewResult(null, false);
    verify(mConsumer).onNewResult(null, false);
  }

  @Test
  public void testOnFailure() {
    mRemoveMetaDataConsumer.onFailure(mException);
    verify(mConsumer).onFailure(mException);
  }

  @Test
  public void testOnCancellation() {
    mRemoveMetaDataConsumer.onCancellation();
    verify(mConsumer).onCancellation();
  }
}
