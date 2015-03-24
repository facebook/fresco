/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import javax.annotation.Nullable;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Executor;

import android.os.SystemClock;

import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.memory.ByteArrayPool;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.memory.PooledByteBufferOutputStream;

/**
 * A producer to actually fetch images from the network.
 *
 * <p> Downloaded bytes are passed to the consumer as they are downloaded, but not more often than
 * {@link #TIME_BETWEEN_PARTIAL_RESULTS_MS}.

 * <p>Implementations should subclass this to make use of the network stack they are using.
 * Use {@link HttpURLConnectionNetworkFetchProducer} as a model.
 *
 * <p>Most implementations will only need to override
 * {@link #newRequestState(Consumer, ProducerContext)} and {@link #fetchImage(NfpRequestState)}.
 *
 * <p>It is strongly recommended that implementations use an {@link Executor} in their fetchImage
 * method to execute the network request on a different thread.
 *
 * <p>When the fetch from the network fails or is cancelled, the subclass is responsible for
 * calling {@link #onCancellation(NfpRequestState, Map)} or
 * {@link #onFailure(NfpRequestState, Throwable, Map)}. If these are not called, the
 * rest of the pipeline will not know that the image has failed to load and the application
 * may not behave properly.
 *
 * @param <RS> The type to store all request-scoped data. NfpRequestState can be used or extended.
 */
public abstract class NetworkFetchProducer<RS extends NfpRequestState>
    implements Producer<CloseableReference<PooledByteBuffer>> {

  @VisibleForTesting static final String PRODUCER_NAME = "NetworkFetchProducer";
  public static final String INTERMEDIATE_RESULT_PRODUCER_EVENT = "intermediate_result";
  private static final int READ_SIZE = 16 * 1024;

  /**
   * Time between two consecutive partial results are propagated upstream
   *
   * TODO 5399646: make this configurable
   */
  @VisibleForTesting static final long TIME_BETWEEN_PARTIAL_RESULTS_MS = 100;

  private final PooledByteBufferFactory mPooledByteBufferFactory;
  private final ByteArrayPool mByteArrayPool;

  public NetworkFetchProducer(
      PooledByteBufferFactory pooledByteBufferFactory,
      ByteArrayPool byteArrayPool) {
    mPooledByteBufferFactory = pooledByteBufferFactory;
    mByteArrayPool = byteArrayPool;
  }

  /**
   *  Returns the name of the producer.
   *
   *  <p>This name is passed to the {@link ProducerListener}s.
   */
  protected String getProducerName() {
    return PRODUCER_NAME;
  }

  @Override
  public void produceResults(
      Consumer<CloseableReference<PooledByteBuffer>> consumer,
      ProducerContext context) {
    context.getListener().onProducerStart(
        context.getId(),
        getProducerName());
    RS requestState = newRequestState(consumer, context);
    fetchImage(requestState);
  }

  /**
   * Returns an instance of the {@link NfpRequestState}-derived object used to store state.
   */
  protected abstract RS newRequestState(
      Consumer<CloseableReference<PooledByteBuffer>> consumer,
      ProducerContext context);

  /**
   * Subclasses should override this method to actually call their network stack.
   *
   * <p>It is strongly recommended that this method be asynchronous.
   */
  protected abstract void fetchImage(final RS requestState);

  protected void processResult(
      RS requestState,
      InputStream responseData,
      int responseContentLength,
      boolean propagateIntermediateResults)
      throws IOException {
    final PooledByteBufferOutputStream pooledOutputStream;
    if (responseContentLength > 0) {
      pooledOutputStream = mPooledByteBufferFactory.newOutputStream(responseContentLength);
    } else {
      pooledOutputStream = mPooledByteBufferFactory.newOutputStream();
    }
    final byte[] ioArray = mByteArrayPool.get(READ_SIZE);
    try {
      int length;
      while ((length = responseData.read(ioArray)) >= 0) {
        if (length > 0) {
          pooledOutputStream.write(ioArray, 0, length);
          if (propagateIntermediateResults) {
            maybeHandleIntermediateResult(pooledOutputStream, requestState);
          }
        }
      }
      handleFinalResult(pooledOutputStream, requestState);
    } finally {
      mByteArrayPool.release(ioArray);
      pooledOutputStream.close();
    }
  }

  private void maybeHandleIntermediateResult(
      PooledByteBufferOutputStream pooledOutputStream,
      NfpRequestState requestState) {
    final long nowMs = SystemClock.elapsedRealtime();
    if (nowMs - requestState.getLastIntermediateResultTimeMs() >= TIME_BETWEEN_PARTIAL_RESULTS_MS) {
      requestState.setLastIntermediateResultTimeMs(nowMs);
      requestState.getListener().onProducerEvent(
          requestState.getId(), getProducerName(), INTERMEDIATE_RESULT_PRODUCER_EVENT);
      passResultToConsumer(pooledOutputStream, false, requestState.getConsumer());
    }
  }

  protected void handleFinalResult(
      PooledByteBufferOutputStream pooledOutputStream,
      RS requestState) {
    requestState.getListener().onProducerFinishWithSuccess(
        requestState.getId(),
        getProducerName(),
        getExtraMap(pooledOutputStream.size(), requestState));
    passResultToConsumer(pooledOutputStream, true, requestState.getConsumer());
  }

  @VisibleForTesting
  void passResultToConsumer(
      PooledByteBufferOutputStream pooledOutputStream,
      boolean isFinal,
      Consumer<CloseableReference<PooledByteBuffer>> consumer) {
    CloseableReference<PooledByteBuffer> result =
        CloseableReference.of(pooledOutputStream.toByteBuffer());
    consumer.onNewResult(result, isFinal);
    result.close();
  }

  private @Nullable Map<String, String> getExtraMap(
      int byteSize,
      RS requestState) {
    if (!requestState.getListener().requiresExtraMap(requestState.getId())) {
      return null;
    }
    return buildExtraMapForFinalResult(byteSize, requestState);
  }

  /**
   * Override this to provide a map containing extra parameters to pass to the listeners.
   *
   * @return An immutable map of the parameters. Attempts to modify this map afterwards
   * will result in an exception being thrown.
   */
  protected @Nullable Map<String, String> buildExtraMapForFinalResult(
      int byteSize,
      RS requestState) {
    return null;
  }

  /**
   * Called upon a failure in the network stack.
   *
   * @param requestState Request-specific data.
   * @param e The exception thrown.
   * @param extraMap An immutable map of the parameters. Attempts to modify this map afterwards
   * will result in an exception being thrown.
   */
  protected void onFailure(RS requestState, Throwable e, Map<String, String> extraMap) {
    requestState.getListener().onProducerFinishWithFailure(
        requestState.getId(),
        getProducerName(),
        e,
        extraMap);
    requestState.getConsumer().onFailure(e);
  }

  /**
   * Called upon a cancellation of the request.
   *
   * @param requestState Request-specific data.
   * @param extraMap An immutable map of the parameters. Attempts to modify this map afterwards
   * will result in an exception being thrown.
   */
  protected void onCancellation(RS requestState, Map<String, String> extraMap) {
    requestState.getListener().onProducerFinishWithCancellation(
        requestState.getId(),
        getProducerName(),
        extraMap);
    requestState.getConsumer().onCancellation();
  }
}
