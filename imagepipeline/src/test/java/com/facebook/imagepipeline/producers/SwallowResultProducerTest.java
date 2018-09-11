/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.*;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.CloseableImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.invocation.*;
import org.mockito.stubbing.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

/**
 * Checks basic properties of swallow result producer, that is:
 *   - it swallows all results.
 *   - it notifies previous consumer of last result.
 *   - it notifies previous consumer of a failure.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest= Config.NONE)
public class SwallowResultProducerTest {
  @Mock public Producer<CloseableReference<CloseableImage>> mInputProducer;
  @Mock public Consumer<Void> mConsumer;
  @Mock public ProducerContext mProducerContext;
  @Mock public Exception mException;
  private CloseableReference<CloseableImage> mFinalImageReference;
  private CloseableReference<CloseableImage> mIntermediateImageReference;
  private SwallowResultProducer<CloseableReference<CloseableImage>>
      mSwallowResultProducer;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mSwallowResultProducer =
        new SwallowResultProducer<CloseableReference<CloseableImage>>(mInputProducer);
    mFinalImageReference = CloseableReference.of(mock(CloseableImage.class));
    mIntermediateImageReference = CloseableReference.of(mock(CloseableImage.class));
  }

  @Test
  public void testSwallowResults() {
    setupInputProducerStreamingSuccess();
    mSwallowResultProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(null, Consumer.IS_LAST);
    verifyNoMoreInteractions(mConsumer);
  }

  @Test
  public void testPassOnNullResult() {
    setupInputProducerNotFound();
    mSwallowResultProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(null, Consumer.IS_LAST);
    verifyNoMoreInteractions(mConsumer);
  }

  @Test
  public void testPassOnFailure() {
    setupInputProducerFailure();
    mSwallowResultProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onFailure(mException);
    verifyNoMoreInteractions(mConsumer);
  }

  @Test
  public void testPassOnCancellation() {
    setupInputProducerCancellation();
    mSwallowResultProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onCancellation();
    verifyNoMoreInteractions(mConsumer);
  }

  private void setupInputProducerStreamingSuccess() {
    doAnswer(new ProduceResultsNewResultAnswer(
            Arrays.asList(mIntermediateImageReference, mFinalImageReference)))
        .when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private void setupInputProducerNotFound() {
    final List<CloseableReference<CloseableImage>> empty =
        new ArrayList<CloseableReference<CloseableImage>>(1);
    empty.add(null);
    doAnswer(
        new ProduceResultsNewResultAnswer(empty))
                .when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private void setupInputProducerFailure() {
    doAnswer(new ProduceResultsFailureAnswer()).
        when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private void setupInputProducerCancellation() {
    doAnswer(new ProduceResultsCancellationAnswer()).
        when(mInputProducer).produceResults(any(Consumer.class), eq(mProducerContext));
  }

  private static class ProduceResultsNewResultAnswer implements Answer<Void> {
    private final List<CloseableReference<CloseableImage>> mResults;

    private ProduceResultsNewResultAnswer(List<CloseableReference<CloseableImage>> results) {
      mResults = results;
    }

    @Override
    public Void answer(InvocationOnMock invocation) throws Throwable {
      Consumer consumer = (Consumer) invocation.getArguments()[0];
      Iterator<CloseableReference<CloseableImage>> iterator = mResults.iterator();
      while (iterator.hasNext()) {
        CloseableReference<CloseableImage> result = iterator.next();
        consumer.onNewResult(result, BaseConsumer.simpleStatusForIsLast(!iterator.hasNext()));
      }
      return null;
    }
  }

  private class ProduceResultsFailureAnswer implements Answer<Void> {
    @Override
    public Void answer(InvocationOnMock invocation) throws Throwable {
      Consumer consumer = (Consumer) invocation.getArguments()[0];
      consumer.onFailure(mException);
      return null;
    }
  }

  private class ProduceResultsCancellationAnswer implements Answer<Void> {
    @Override
    public Void answer(InvocationOnMock invocation) throws Throwable {
      Consumer consumer = (Consumer) invocation.getArguments()[0];
      consumer.onCancellation();
      return null;
    }
  }
}
