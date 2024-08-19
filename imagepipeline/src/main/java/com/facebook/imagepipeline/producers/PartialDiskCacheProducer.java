/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import android.net.Uri;
import androidx.annotation.VisibleForTesting;
import bolts.Continuation;
import bolts.Task;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.logging.FLog;
import com.facebook.common.memory.ByteArrayPool;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.memory.PooledByteBufferOutputStream;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.ByteConstants;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.common.BytesRange;
import com.facebook.imagepipeline.core.DiskCachesStore;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.infer.annotation.Nullsafe;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;

/**
 * Partial disk cache producer.
 *
 * <p>This producer looks in the disk cache to see if it holds part of the requested image. If the
 * image is found, then it is passed to the consumer as a non-final result, but an adjusted request
 * is still sent further along to request the remainder of the image.
 *
 * <p>When the final result comes from the input producer, the two parts are stitched back together
 * and returned as a whole.
 */
@Nullsafe(Nullsafe.Mode.LOCAL)
public class PartialDiskCacheProducer implements Producer<EncodedImage> {
  public static final String PRODUCER_NAME = "PartialDiskCacheProducer";

  public static final String EXTRA_CACHED_VALUE_FOUND = ProducerConstants.EXTRA_CACHED_VALUE_FOUND;

  public static final String ENCODED_IMAGE_SIZE = ProducerConstants.ENCODED_IMAGE_SIZE;

  private final Supplier<DiskCachesStore> mDiskCachesStoreSupplier;
  private final CacheKeyFactory mCacheKeyFactory;
  private final PooledByteBufferFactory mPooledByteBufferFactory;
  private final ByteArrayPool mByteArrayPool;
  private final Producer<EncodedImage> mInputProducer;

  public PartialDiskCacheProducer(
      Supplier<DiskCachesStore> diskCachesStoreSupplier,
      CacheKeyFactory cacheKeyFactory,
      PooledByteBufferFactory pooledByteBufferFactory,
      ByteArrayPool byteArrayPool,
      Producer<EncodedImage> inputProducer) {
    mDiskCachesStoreSupplier = diskCachesStoreSupplier;
    mCacheKeyFactory = cacheKeyFactory;
    mPooledByteBufferFactory = pooledByteBufferFactory;
    mByteArrayPool = byteArrayPool;
    mInputProducer = inputProducer;
  }

  public void produceResults(
      final Consumer<EncodedImage> consumer, final ProducerContext producerContext) {
    final ImageRequest imageRequest = producerContext.getImageRequest();
    final boolean isDiskCacheEnabledForRead =
        producerContext
            .getImageRequest()
            .isCacheEnabled(ImageRequest.CachesLocationsMasks.DISK_READ);
    final boolean isDiskCacheEnabledForWrite =
        producerContext
            .getImageRequest()
            .isCacheEnabled(ImageRequest.CachesLocationsMasks.DISK_WRITE);

    if (!isDiskCacheEnabledForRead && !isDiskCacheEnabledForWrite) {
      mInputProducer.produceResults(consumer, producerContext);
      return;
    }

    final ProducerListener2 listener = producerContext.getProducerListener();
    listener.onProducerStart(producerContext, PRODUCER_NAME);

    final Uri uriForPartialCacheKey = createUriForPartialCacheKey(imageRequest);
    final CacheKey partialImageCacheKey =
        mCacheKeyFactory.getEncodedCacheKey(
            imageRequest, uriForPartialCacheKey, producerContext.getCallerContext());

    if (!isDiskCacheEnabledForRead) {
      listener.onProducerFinishWithSuccess(
          producerContext, PRODUCER_NAME, getExtraMap(listener, producerContext, false, 0));
      startInputProducer(consumer, producerContext, partialImageCacheKey, null);
      return;
    }

    final AtomicBoolean isCancelled = new AtomicBoolean(false);
    final Task<EncodedImage> diskLookupTask =
        mDiskCachesStoreSupplier
            .get()
            .getMainBufferedDiskCache()
            .get(partialImageCacheKey, isCancelled);
    final Continuation<EncodedImage, Void> continuation =
        onFinishDiskReads(consumer, producerContext, partialImageCacheKey);

    diskLookupTask.continueWith(continuation);
    subscribeTaskForRequestCancellation(isCancelled, producerContext);
  }

  private Continuation<EncodedImage, Void> onFinishDiskReads(
      final Consumer<EncodedImage> consumer,
      final ProducerContext producerContext,
      final CacheKey partialImageCacheKey) {
    final ProducerListener2 listener = producerContext.getProducerListener();
    return new Continuation<EncodedImage, Void>() {
      @Nullable
      @Override
      public Void then(Task<EncodedImage> task) throws Exception {
        if (isTaskCancelled(task)) {
          listener.onProducerFinishWithCancellation(producerContext, PRODUCER_NAME, null);
          consumer.onCancellation();
        } else if (task.isFaulted()) {
          listener.onProducerFinishWithFailure(
              producerContext, PRODUCER_NAME, task.getError(), null);
          startInputProducer(consumer, producerContext, partialImageCacheKey, null);
        } else {
          EncodedImage cachedReference = task.getResult();
          if (cachedReference != null) {
            listener.onProducerFinishWithSuccess(
                producerContext,
                PRODUCER_NAME,
                getExtraMap(listener, producerContext, true, cachedReference.getSize()));
            final BytesRange cachedRange = BytesRange.toMax(cachedReference.getSize() - 1);
            cachedReference.setBytesRange(cachedRange);

            // Create a new ImageRequest for the remaining data
            final int cachedLength = cachedReference.getSize();
            final ImageRequest originalRequest = producerContext.getImageRequest();

            if (cachedRange.contains(originalRequest.getBytesRange())) {
              producerContext.putOriginExtra("disk", "partial");
              listener.onUltimateProducerReached(producerContext, PRODUCER_NAME, true);
              consumer.onNewResult(cachedReference, Consumer.IS_LAST | Consumer.IS_PARTIAL_RESULT);
            } else {
              consumer.onNewResult(cachedReference, Consumer.IS_PARTIAL_RESULT);

              // Pass the request on, but only for the remaining bytes
              final ImageRequest remainingRequest =
                  ImageRequestBuilder.fromRequest(originalRequest)
                      .setBytesRange(BytesRange.from(cachedLength - 1))
                      .build();
              final SettableProducerContext contextForRemainingRequest =
                  new SettableProducerContext(remainingRequest, producerContext);

              startInputProducer(
                  consumer, contextForRemainingRequest, partialImageCacheKey, cachedReference);
            }
          } else {
            listener.onProducerFinishWithSuccess(
                producerContext, PRODUCER_NAME, getExtraMap(listener, producerContext, false, 0));
            startInputProducer(consumer, producerContext, partialImageCacheKey, cachedReference);
          }
        }
        return null;
      }
    };
  }

  private void startInputProducer(
      Consumer<EncodedImage> consumerOfPartialDiskCacheProducer,
      ProducerContext producerContext,
      CacheKey partialImageCacheKey,
      @Nullable EncodedImage partialResultFromCache) {
    Consumer<EncodedImage> consumer =
        new PartialDiskCacheConsumer(
            consumerOfPartialDiskCacheProducer,
            mDiskCachesStoreSupplier,
            partialImageCacheKey,
            mPooledByteBufferFactory,
            mByteArrayPool,
            partialResultFromCache,
            producerContext
                .getImageRequest()
                .isCacheEnabled(ImageRequest.CachesLocationsMasks.DISK_WRITE));

    mInputProducer.produceResults(consumer, producerContext);
  }

  private static boolean isTaskCancelled(Task<?> task) {
    return task.isCancelled()
        || (task.isFaulted() && task.getError() instanceof CancellationException);
  }

  @Nullable
  @VisibleForTesting
  static Map<String, String> getExtraMap(
      final ProducerListener2 listener,
      final ProducerContext producerContext,
      final boolean valueFound,
      final int sizeInBytes) {
    if (!listener.requiresExtraMap(producerContext, PRODUCER_NAME)) {
      return null;
    }
    if (valueFound) {
      return ImmutableMap.of(
          EXTRA_CACHED_VALUE_FOUND,
          String.valueOf(valueFound),
          ENCODED_IMAGE_SIZE,
          String.valueOf(sizeInBytes));
    } else {
      return ImmutableMap.of(EXTRA_CACHED_VALUE_FOUND, String.valueOf(valueFound));
    }
  }

  private void subscribeTaskForRequestCancellation(
      final AtomicBoolean isCancelled, ProducerContext producerContext) {
    producerContext.addCallbacks(
        new BaseProducerContextCallbacks() {
          @Override
          public void onCancellationRequested() {
            isCancelled.set(true);
          }
        });
  }

  private static Uri createUriForPartialCacheKey(ImageRequest imageRequest) {
    return imageRequest
        .getSourceUri()
        .buildUpon()
        .appendQueryParameter("fresco_partial", "true")
        .build();
  }

  /**
   * Consumer that consumes results from next producer in the sequence.
   *
   * <p>If the consumer receives a partial result, it caches it on disk with an appropriate key to
   * differentiate from the full image.
   *
   * <p>If a partial image is already held, it combines new results with that partial data and
   * passes the combination to the next consumer.
   */
  private static class PartialDiskCacheConsumer
      extends DelegatingConsumer<EncodedImage, EncodedImage> {

    private static final int READ_SIZE = 16 * ByteConstants.KB;

    private final Supplier<DiskCachesStore> mDiskCachesStoreSupplier;
    private final CacheKey mPartialImageCacheKey;
    private final PooledByteBufferFactory mPooledByteBufferFactory;
    private final ByteArrayPool mByteArrayPool;
    private final @Nullable EncodedImage mPartialEncodedImageFromCache;
    private final boolean mIsDiskCacheEnabledForWrite;

    private PartialDiskCacheConsumer(
        final Consumer<EncodedImage> consumer,
        final Supplier<DiskCachesStore> diskCachesStoreSupplier,
        final CacheKey partialImageCacheKey,
        final PooledByteBufferFactory pooledByteBufferFactory,
        final ByteArrayPool byteArrayPool,
        final @Nullable EncodedImage partialEncodedImageFromCache,
        final boolean isDiskCacheEnabledForWrite) {
      super(consumer);
      mDiskCachesStoreSupplier = diskCachesStoreSupplier;
      mPartialImageCacheKey = partialImageCacheKey;
      mPooledByteBufferFactory = pooledByteBufferFactory;
      mByteArrayPool = byteArrayPool;
      mPartialEncodedImageFromCache = partialEncodedImageFromCache;
      mIsDiskCacheEnabledForWrite = isDiskCacheEnabledForWrite;
    }

    @Override
    public void onNewResultImpl(@Nullable EncodedImage newResult, @Status int status) {
      if (isNotLast(status)) {
        // TODO 19247361 Consider merging of non-final results
        return;
      }

      if (mPartialEncodedImageFromCache != null
          && newResult != null
          && newResult.getBytesRange() != null) {
        try {
          final PooledByteBufferOutputStream pooledOutputStream =
              merge(mPartialEncodedImageFromCache, newResult);
          sendFinalResultToConsumer(pooledOutputStream);
        } catch (IOException e) {
          // TODO 19247425 Delete cached file and request full image
          FLog.e(PRODUCER_NAME, "Error while merging image data", e);
          getConsumer().onFailure(e);
        } finally {
          newResult.close();
          mPartialEncodedImageFromCache.close();
        }

        mDiskCachesStoreSupplier.get().getMainBufferedDiskCache().remove(mPartialImageCacheKey);
      } else if (mIsDiskCacheEnabledForWrite
          && statusHasFlag(status, IS_PARTIAL_RESULT)
          && isLast(status)
          && newResult != null
          && newResult.getImageFormat() != ImageFormat.UNKNOWN) {
        mDiskCachesStoreSupplier
            .get()
            .getMainBufferedDiskCache()
            .put(mPartialImageCacheKey, newResult);
        getConsumer().onNewResult(newResult, status);
      } else {
        getConsumer().onNewResult(newResult, status);
      }
    }

    private PooledByteBufferOutputStream merge(EncodedImage initialData, EncodedImage remainingData)
        throws IOException {
      int bytesToReadFromInitialData =
          Preconditions.checkNotNull(remainingData.getBytesRange()).from;
      final int totalLength = remainingData.getSize() + bytesToReadFromInitialData;
      final PooledByteBufferOutputStream pooledOutputStream =
          mPooledByteBufferFactory.newOutputStream(totalLength);

      // Only read from the original image data up to the start of the new data
      copy(initialData.getInputStreamOrThrow(), pooledOutputStream, bytesToReadFromInitialData);
      copy(remainingData.getInputStreamOrThrow(), pooledOutputStream, remainingData.getSize());

      return pooledOutputStream;
    }

    private void copy(InputStream from, OutputStream to, int length) throws IOException {
      int bytesStillToRead = length;
      final byte[] ioArray = mByteArrayPool.get(READ_SIZE);
      try {
        int bufferLength;
        while (bytesStillToRead > 0
            && (bufferLength = from.read(ioArray, 0, Math.min(READ_SIZE, bytesStillToRead))) >= 0) {
          if (bufferLength > 0) {
            to.write(ioArray, 0, bufferLength);
            bytesStillToRead -= bufferLength;
          }
        }
      } finally {
        mByteArrayPool.release(ioArray);
      }

      if (bytesStillToRead > 0) {
        throw new IOException(
            String.format(
                (Locale) null,
                "Failed to read %d bytes - finished %d short",
                length,
                bytesStillToRead));
      }
    }

    private void sendFinalResultToConsumer(PooledByteBufferOutputStream pooledOutputStream) {
      CloseableReference<PooledByteBuffer> result =
          CloseableReference.of(pooledOutputStream.toByteBuffer());
      EncodedImage encodedImage = null;
      try {
        encodedImage = new EncodedImage(result);
        encodedImage.parseMetaData();
        getConsumer().onNewResult(encodedImage, IS_LAST);
      } finally {
        EncodedImage.closeSafely(encodedImage);
        CloseableReference.closeSafely(result);
      }
    }
  }
}
