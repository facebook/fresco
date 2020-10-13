/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import android.os.SystemClock;
import androidx.annotation.VisibleForTesting;
import com.facebook.common.memory.ByteArrayPool;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.memory.PooledByteBufferOutputStream;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.common.BytesRange;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.image.EncodedImageOrigin;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import com.facebook.infer.annotation.Nullsafe;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * A producer to actually fetch images from the network.
 *
 * <p>Downloaded bytes may be passed to the consumer as they are downloaded, but not more often than
 * {@link #TIME_BETWEEN_PARTIAL_RESULTS_MS}.
 *
 * <p>Clients should provide an instance of {@link NetworkFetcher} to make use of their networking
 * stack. Use {@link HttpUrlConnectionNetworkFetcher} as a model.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class NetworkFetchProducer implements Producer<EncodedImage> {

  public static final String PRODUCER_NAME = "NetworkFetchProducer";
  public static final String INTERMEDIATE_RESULT_PRODUCER_EVENT = "intermediate_result";
  private static final int READ_SIZE = 16 * 1024;

  /**
   * Time between two consecutive partial results are propagated upstream
   *
   * <p>TODO 5399646: make this configurable
   */
  @VisibleForTesting static final long TIME_BETWEEN_PARTIAL_RESULTS_MS = 100;

  protected final PooledByteBufferFactory mPooledByteBufferFactory;
  private final ByteArrayPool mByteArrayPool;
  private final NetworkFetcher mNetworkFetcher;

  public NetworkFetchProducer(
      PooledByteBufferFactory pooledByteBufferFactory,
      ByteArrayPool byteArrayPool,
      NetworkFetcher networkFetcher) {
    mPooledByteBufferFactory = pooledByteBufferFactory;
    mByteArrayPool = byteArrayPool;
    mNetworkFetcher = networkFetcher;
  }

  @Override
  public void produceResults(Consumer<EncodedImage> consumer, ProducerContext context) {
    context.getProducerListener().onProducerStart(context, PRODUCER_NAME);
    final FetchState fetchState = mNetworkFetcher.createFetchState(consumer, context);
    mNetworkFetcher.fetch(
        fetchState,
        new NetworkFetcher.Callback() {
          @Override
          public void onResponse(InputStream response, int responseLength) throws IOException {
            if (FrescoSystrace.isTracing()) {
              FrescoSystrace.beginSection("NetworkFetcher->onResponse");
            }
            NetworkFetchProducer.this.onResponse(fetchState, response, responseLength);
            if (FrescoSystrace.isTracing()) {
              FrescoSystrace.endSection();
            }
          }

          @Override
          public void onFailure(Throwable throwable) {
            NetworkFetchProducer.this.onFailure(fetchState, throwable);
          }

          @Override
          public void onCancellation() {
            NetworkFetchProducer.this.onCancellation(fetchState);
          }
        });
  }

  protected void onResponse(
      FetchState fetchState, InputStream responseData, int responseContentLength)
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
          maybeHandleIntermediateResult(pooledOutputStream, fetchState);
          float progress = calculateProgress(pooledOutputStream.size(), responseContentLength);
          fetchState.getConsumer().onProgressUpdate(progress);
        }
      }
      mNetworkFetcher.onFetchCompletion(fetchState, pooledOutputStream.size());
      handleFinalResult(pooledOutputStream, fetchState);
    } finally {
      mByteArrayPool.release(ioArray);
      pooledOutputStream.close();
    }
  }

  protected static float calculateProgress(int downloaded, int total) {
    if (total > 0) {
      return (float) downloaded / total;
    } else {
      // If we don't know the total number of bytes, we approximate the progress by an exponential
      // that approaches 1. Here are some values of the progress, given the number of bytes:
      // 0.5 kB ~  1%
      // 2.5 kB ~  5%
      //   5 kB ~ 10%
      //  14 kB ~ 25%
      //  34 kB ~ 50%
      //  68 kB ~ 75%
      // 113 kB ~ 90%
      // 147 kB ~ 95%
      // 225 kB ~ 99%
      return 1 - (float) Math.exp(-downloaded / 5e4);
    }
  }

  protected void maybeHandleIntermediateResult(
      PooledByteBufferOutputStream pooledOutputStream, FetchState fetchState) {
    final long nowMs = getSystemUptime();
    if (shouldPropagateIntermediateResults(fetchState)
        && nowMs - fetchState.getLastIntermediateResultTimeMs()
            >= TIME_BETWEEN_PARTIAL_RESULTS_MS) {
      fetchState.setLastIntermediateResultTimeMs(nowMs);
      fetchState
          .getListener()
          .onProducerEvent(
              fetchState.getContext(), PRODUCER_NAME, INTERMEDIATE_RESULT_PRODUCER_EVENT);
      notifyConsumer(
          pooledOutputStream,
          fetchState.getOnNewResultStatusFlags(),
          fetchState.getResponseBytesRange(),
          fetchState.getConsumer(),
          fetchState.getContext());
    }
  }

  protected void handleFinalResult(
      PooledByteBufferOutputStream pooledOutputStream, FetchState fetchState) {
    Map<String, String> extraMap = getExtraMap(fetchState, pooledOutputStream.size());
    ProducerListener2 listener = fetchState.getListener();
    listener.onProducerFinishWithSuccess(fetchState.getContext(), PRODUCER_NAME, extraMap);
    listener.onUltimateProducerReached(fetchState.getContext(), PRODUCER_NAME, true);
    fetchState.getContext().putOriginExtra("network");
    notifyConsumer(
        pooledOutputStream,
        Consumer.IS_LAST | fetchState.getOnNewResultStatusFlags(),
        fetchState.getResponseBytesRange(),
        fetchState.getConsumer(),
        fetchState.getContext());
  }

  protected static void notifyConsumer(
      PooledByteBufferOutputStream pooledOutputStream,
      @Consumer.Status int status,
      @Nullable BytesRange responseBytesRange,
      Consumer<EncodedImage> consumer,
      ProducerContext context) {
    CloseableReference<PooledByteBuffer> result =
        CloseableReference.of(pooledOutputStream.toByteBuffer());
    EncodedImage encodedImage = null;
    try {
      encodedImage = new EncodedImage(result);
      encodedImage.setBytesRange(responseBytesRange);
      encodedImage.parseMetaData();
      context.setEncodedImageOrigin(EncodedImageOrigin.NETWORK);
      consumer.onNewResult(encodedImage, status);
    } finally {
      EncodedImage.closeSafely(encodedImage);
      CloseableReference.closeSafely(result);
    }
  }

  private void onFailure(FetchState fetchState, Throwable e) {
    fetchState
        .getListener()
        .onProducerFinishWithFailure(fetchState.getContext(), PRODUCER_NAME, e, null);
    fetchState
        .getListener()
        .onUltimateProducerReached(fetchState.getContext(), PRODUCER_NAME, false);
    fetchState.getContext().putOriginExtra("network");
    fetchState.getConsumer().onFailure(e);
  }

  private void onCancellation(FetchState fetchState) {
    fetchState
        .getListener()
        .onProducerFinishWithCancellation(fetchState.getContext(), PRODUCER_NAME, null);
    fetchState.getConsumer().onCancellation();
  }

  private boolean shouldPropagateIntermediateResults(FetchState fetchState) {
    if (!fetchState.getContext().isIntermediateResultExpected()) {
      return false;
    }
    return mNetworkFetcher.shouldPropagate(fetchState);
  }

  @Nullable
  private Map<String, String> getExtraMap(FetchState fetchState, int byteSize) {
    if (!fetchState.getListener().requiresExtraMap(fetchState.getContext(), PRODUCER_NAME)) {
      return null;
    }
    return mNetworkFetcher.getExtraMap(fetchState, byteSize);
  }

  @VisibleForTesting
  protected long getSystemUptime() {
    return SystemClock.uptimeMillis();
  }
}
