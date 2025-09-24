// (c) Meta Platforms, Inc. and affiliates. Confidential and proprietary.

package com.facebook.imagepipeline.producers;

import static com.facebook.imagepipeline.producers.Consumer.IS_LAST;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import android.net.Uri;
import bolts.Task;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.MultiCacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.Supplier;
import com.facebook.common.memory.ByteArrayPool;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.memory.PooledByteBufferOutputStream;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.common.BytesRange;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.core.DiskCachesStore;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class PartialDiskCacheProducerTest {
  private static final String PRODUCER_NAME = PartialDiskCacheProducer.PRODUCER_NAME;

  @Mock public ImageRequest mImageRequest;
  private final String mRequestId = "mRequestId";
  private MultiCacheKey mCacheKey;
  @Mock public ProducerListener2 mProducerListener;
  @Mock public Object mCallerContext;
  @Mock public ImagePipelineConfig mConfig;
  @Mock public Producer mInputProducer;
  @Mock public Consumer mConsumer;

  private final ByteArrayPool mByteArrayPool = mock(ByteArrayPool.class);
  private final Supplier<DiskCachesStore> mDiskCachesStoreSupplier = mock(Supplier.class);
  private final BufferedDiskCache mDefaultBufferedDiskCache = mock(BufferedDiskCache.class);
  private final BufferedDiskCache mSmallImageBufferedDiskCache = mock(BufferedDiskCache.class);
  private final String mDiskCacheId1 = "DISK_CACHE_ID_1";
  private final BufferedDiskCache mBufferedDiskCache1 = mock(BufferedDiskCache.class);
  private final Map<String, BufferedDiskCache> mDynamicBufferedDiskCaches =
      ImmutableMap.of(mDiskCacheId1, mBufferedDiskCache1);
  private SettableProducerContext mProducerContext;
  PartialDiskCacheProducer mPartialDiskCacheProducer;
  @Mock public CacheKeyFactory mCacheKeyFactory;
  PooledByteBufferFactory mPooledByteBufferFactory = mock(PooledByteBufferFactory.class);
  private final String mSourceUriString = "http://dummy.uri";

  private PooledByteBuffer mPartialPooledByteBuffer;
  private PooledByteBuffer mRestPooledByteBuffer;
  private PooledByteBuffer mFinalPooledByteBuffer;
  private CloseableReference<PooledByteBuffer> mPartialImageReference;
  private CloseableReference<PooledByteBuffer> mRestOfImageReference;
  private CloseableReference<PooledByteBuffer> mFinalImageReference;
  private EncodedImage mPartialEncodedImage;
  private EncodedImage mRestOfEncodedImage;
  private EncodedImage mFinalEncodedImage;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mPartialDiskCacheProducer =
        new PartialDiskCacheProducer(
            mDiskCachesStoreSupplier,
            mCacheKeyFactory,
            mPooledByteBufferFactory,
            mByteArrayPool,
            mInputProducer);
    List<CacheKey> keys = new ArrayList<>(1);
    keys.add(new SimpleCacheKey(mSourceUriString));
    mCacheKey = new MultiCacheKey(keys);
    mPartialPooledByteBuffer = mock(PooledByteBuffer.class);
    mRestPooledByteBuffer = mock(PooledByteBuffer.class);
    mFinalPooledByteBuffer = mock(PooledByteBuffer.class);
    mPartialImageReference = CloseableReference.of(mPartialPooledByteBuffer);
    mRestOfImageReference = CloseableReference.of(mRestPooledByteBuffer);
    mFinalImageReference = CloseableReference.of(mFinalPooledByteBuffer);
    mPartialEncodedImage = new EncodedImage(mPartialImageReference);
    mRestOfEncodedImage = new EncodedImage(mRestOfImageReference);
    mFinalEncodedImage = new EncodedImage(mFinalImageReference);

    mProducerContext =
        new SettableProducerContext(
            mImageRequest,
            mRequestId,
            mProducerListener,
            mCallerContext,
            ImageRequest.RequestLevel.FULL_FETCH,
            false,
            true,
            Priority.MEDIUM,
            mConfig);

    when(mProducerListener.requiresExtraMap(mProducerContext, PRODUCER_NAME)).thenReturn(true);
    when(mCacheKeyFactory.getEncodedCacheKey(mImageRequest, mCallerContext)).thenReturn(mCacheKey);
    when(mCacheKeyFactory.getEncodedCacheKey(eq(mImageRequest), any(Uri.class), eq(mCallerContext)))
        .thenReturn(mCacheKey);
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.DEFAULT);
    when(mImageRequest.isDiskCacheEnabled()).thenReturn(true);
    when(mImageRequest.isCacheEnabled(ImageRequest.CachesLocationsMasks.DISK_READ))
        .thenReturn(true);
    when(mImageRequest.isCacheEnabled(ImageRequest.CachesLocationsMasks.DISK_WRITE))
        .thenReturn(true);
    when(mImageRequest.getSourceUri()).thenReturn(Uri.parse(mSourceUriString));
    DiskCachesStore diskCachesStore = mock(DiskCachesStore.class);
    when(mDiskCachesStoreSupplier.get()).thenReturn(diskCachesStore);
    when(diskCachesStore.getMainBufferedDiskCache()).thenReturn(mDefaultBufferedDiskCache);
    when(diskCachesStore.getSmallImageBufferedDiskCache()).thenReturn(mSmallImageBufferedDiskCache);
    when(diskCachesStore.getDynamicBufferedDiskCaches())
        .thenReturn(ImmutableMap.copyOf(mDynamicBufferedDiskCaches));
  }

  @Test
  public void testReadPartialThenFetchRest() {
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.DEFAULT);
    when(mDefaultBufferedDiskCache.get(any(CacheKey.class), any(AtomicBoolean.class)))
        .thenReturn(Task.forResult(mPartialEncodedImage));
    when(mPartialEncodedImage.getSize()).thenReturn(1000);
    when(mImageRequest.getBytesRange()).thenReturn(BytesRange.toMax(2000));
    when(mImageRequest.newImageRequestFromImageRequestBuilder(any(ImageRequestBuilder.class)))
        .then(
            answer -> {
              ImageRequestBuilder builder = answer.getArgument(0);
              when(mImageRequest.getBytesRange()).thenReturn(builder.getBytesRange());
              return mImageRequest;
            });
    doAnswer(
            answer -> {
              PartialDiskCacheProducer.PartialDiskCacheConsumer consumer = answer.getArgument(0);
              BaseProducerContext producerContext = answer.getArgument(1);
              mRestOfEncodedImage.setBytesRange(producerContext.getImageRequest().getBytesRange());
              PartialDiskCacheProducer.PartialDiskCacheConsumer spyConsumer = spy(consumer);
              PooledByteBufferOutputStream finalOutputStream =
                  mock(PooledByteBufferOutputStream.class);
              when(finalOutputStream.toByteBuffer()).thenReturn(mFinalPooledByteBuffer);
              doReturn(finalOutputStream)
                  .when(spyConsumer)
                  .merge(eq(mPartialEncodedImage), eq(mRestOfEncodedImage));
              spyConsumer.onNewResult(mRestOfEncodedImage, IS_LAST);
              return null;
            })
        .when(mInputProducer)
        .produceResults(
            ArgumentMatchers.<Consumer<EncodedImage>>any(), any(BaseProducerContext.class));

    mPartialDiskCacheProducer.produceResults(mConsumer, mProducerContext);
    verify(mConsumer).onNewResult(any(EncodedImage.class), eq(IS_LAST));
  }
}
