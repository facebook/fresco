/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.net.Uri;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.MultiCacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.common.internal.Predicate;
import com.facebook.common.internal.Sets;
import com.facebook.common.internal.Supplier;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.DataSource;
import com.facebook.imagepipeline.cache.BitmapMemoryCacheKey;
import com.facebook.imagepipeline.cache.BufferedDiskCache;
import com.facebook.imagepipeline.cache.CacheKeyFactory;
import com.facebook.imagepipeline.cache.MemoryCache;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.producers.Consumer;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.ProducerContext;
import com.facebook.imagepipeline.producers.ThreadHandoffProducerQueue;
import com.facebook.imagepipeline.request.ImageRequest;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;

/**
 * Tests for ImagePipeline
 */
@RunWith(RobolectricTestRunner.class)
public class ImagePipelineTest {
  @Mock public ImageRequest mImageRequest;
  @Mock public ProducerSequenceFactory mProducerSequenceFactory;
  @Mock public CacheKeyFactory mCacheKeyFactory;
  @Mock public Object mCallerContext;

  private Supplier<Boolean> mPrefetchEnabledSupplier;
  private Supplier<Boolean> mSuppressBitmapPrefetchingSupplier;
  private Supplier<Boolean> mLazyDataSourceSupplier;
  private ImagePipeline mImagePipeline;
  private MemoryCache<CacheKey, CloseableImage> mBitmapMemoryCache;
  private MemoryCache<CacheKey, PooledByteBuffer> mEncodedMemoryCache;
  private BufferedDiskCache mMainDiskStorageCache;
  private BufferedDiskCache mSmallImageDiskStorageCache;
  private RequestListener mRequestListener1;
  private RequestListener mRequestListener2;
  private ThreadHandoffProducerQueue mThreadHandoffProducerQueue;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    mPrefetchEnabledSupplier = mock(Supplier.class);
    mSuppressBitmapPrefetchingSupplier = mock(Supplier.class);
    mLazyDataSourceSupplier = mock(Supplier.class);
    when(mPrefetchEnabledSupplier.get()).thenReturn(true);
    when(mSuppressBitmapPrefetchingSupplier.get()).thenReturn(false);
    when(mLazyDataSourceSupplier.get()).thenReturn(false);
    mRequestListener1 = mock(RequestListener.class);
    mRequestListener2 = mock(RequestListener.class);
    mBitmapMemoryCache = mock(MemoryCache.class);
    mEncodedMemoryCache = mock(MemoryCache.class);
    mMainDiskStorageCache = mock(BufferedDiskCache.class);
    mSmallImageDiskStorageCache = mock(BufferedDiskCache.class);
    mThreadHandoffProducerQueue= mock(ThreadHandoffProducerQueue.class);
    mImagePipeline =
        new ImagePipeline(
            mProducerSequenceFactory,
            Sets.newHashSet(mRequestListener1, mRequestListener2),
            mPrefetchEnabledSupplier,
            mBitmapMemoryCache,
            mEncodedMemoryCache,
            mMainDiskStorageCache,
            mSmallImageDiskStorageCache,
            mCacheKeyFactory,
            mThreadHandoffProducerQueue,
            mSuppressBitmapPrefetchingSupplier,
            mLazyDataSourceSupplier,
            null);

    when(mImageRequest.getProgressiveRenderingEnabled()).thenReturn(true);
    when(mImageRequest.getPriority()).thenReturn(Priority.HIGH);
    when(mImageRequest.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.FULL_FETCH);
    when(mImageRequest.shouldDecodePrefetches())
            .thenReturn(null);
  }

  @Test
  public void testPrefetchToDiskCacheWithPrefetchDisabled() {
    when(mPrefetchEnabledSupplier.get()).thenReturn(false);
    DataSource<Void> dataSource = mImagePipeline.prefetchToDiskCache(mImageRequest, mCallerContext);
    assertTrue(dataSource.hasFailed());
    verifyNoMoreInteractions(mProducerSequenceFactory, mRequestListener1, mRequestListener2);
  }

  @Test
  public void testPrefetchToBitmapCacheWithPrefetchDisabled() {
    when(mPrefetchEnabledSupplier.get()).thenReturn(false);
    DataSource<Void> dataSource =
        mImagePipeline.prefetchToBitmapCache(mImageRequest, mCallerContext);
    assertTrue(dataSource.hasFailed());
    verifyNoMoreInteractions(mProducerSequenceFactory, mRequestListener1, mRequestListener2);
  }

  @Test
  public void testPrefetchToBitmapCacheWithBitmapPrefetcherSuppressed() {
    Producer<Void> prefetchProducerSequence = mock(Producer.class);
    when(mProducerSequenceFactory.getEncodedImagePrefetchProducerSequence(mImageRequest))
        .thenReturn(prefetchProducerSequence);
    when(mSuppressBitmapPrefetchingSupplier.get()).thenReturn(true);
    DataSource<Void> dataSource =
        mImagePipeline.prefetchToBitmapCache(mImageRequest, mCallerContext);
    verifyPrefetchToDiskCache(dataSource, prefetchProducerSequence, Priority.MEDIUM);
  }

  @Test
  public void testPrefetchToDiskCacheDefaultPriority() {
    Producer<Void> prefetchProducerSequence = mock(Producer.class);
    when(mProducerSequenceFactory.getEncodedImagePrefetchProducerSequence(mImageRequest))
        .thenReturn(prefetchProducerSequence);
    DataSource<Void> dataSource = mImagePipeline.prefetchToDiskCache(
        mImageRequest,
        mCallerContext);
    verifyPrefetchToDiskCache(dataSource, prefetchProducerSequence, Priority.MEDIUM);
  }

  @Test
  public void testPrefetchToDiskCacheCustomPriority() {
    Producer<Void> prefetchProducerSequence = mock(Producer.class);
    when(mProducerSequenceFactory.getEncodedImagePrefetchProducerSequence(mImageRequest))
        .thenReturn(prefetchProducerSequence);
    DataSource<Void> dataSource = mImagePipeline.prefetchToDiskCache(
        mImageRequest,
        mCallerContext,
        Priority.MEDIUM);
    verifyPrefetchToDiskCache(dataSource, prefetchProducerSequence, Priority.MEDIUM);
  }

  private void verifyPrefetchToDiskCache(
      DataSource<Void> dataSource,
      Producer<Void> prefetchProducerSequence,
      Priority priority) {
    assertFalse(dataSource.isFinished());
    verify(mRequestListener1).onRequestStart(mImageRequest, mCallerContext, "0", true);
    verify(mRequestListener2).onRequestStart(mImageRequest, mCallerContext, "0", true);
    ArgumentCaptor<ProducerContext> producerContextArgumentCaptor =
        ArgumentCaptor.forClass(ProducerContext.class);
    verify(prefetchProducerSequence)
        .produceResults(any(Consumer.class), producerContextArgumentCaptor.capture());
    assertFalse(producerContextArgumentCaptor.getValue().isIntermediateResultExpected());
    assertEquals(priority, producerContextArgumentCaptor.getValue().getPriority());
  }

  @Test
  public void testPrefetchToBitmapCache() {
    Producer<Void> prefetchProducerSequence = mock(Producer.class);
    when(mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest))
        .thenReturn(prefetchProducerSequence);
    DataSource<Void> dataSource =
        mImagePipeline.prefetchToBitmapCache(mImageRequest, mCallerContext);
    assertTrue(!dataSource.isFinished());
    verify(mRequestListener1).onRequestStart(mImageRequest, mCallerContext, "0",  true);
    verify(mRequestListener2).onRequestStart(mImageRequest, mCallerContext, "0", true);
    ArgumentCaptor<ProducerContext> producerContextArgumentCaptor =
        ArgumentCaptor.forClass(ProducerContext.class);
    verify(prefetchProducerSequence)
        .produceResults(any(Consumer.class), producerContextArgumentCaptor.capture());
    assertFalse(producerContextArgumentCaptor.getValue().isIntermediateResultExpected());
    assertEquals(producerContextArgumentCaptor.getValue().getPriority(), Priority.MEDIUM);
  }

  @Test
  public void testFetchLocalEncodedImage() {
    Producer<CloseableReference<PooledByteBuffer>> encodedSequence = mock(Producer.class);
    when(mProducerSequenceFactory.getEncodedImageProducerSequence(mImageRequest))
        .thenReturn(encodedSequence);
    when(mImageRequest.getSourceUri()).thenReturn(Uri.parse("file:///local/file"));
    DataSource<CloseableReference<PooledByteBuffer>> dataSource =
        mImagePipeline.fetchEncodedImage(mImageRequest, mCallerContext);
    assertFalse(dataSource.isFinished());
    ArgumentCaptor<ImageRequest> argumentCaptor = ArgumentCaptor.forClass(ImageRequest.class);
    verify(mRequestListener1).onRequestStart(
        argumentCaptor.capture(),
        eq(mCallerContext),
        eq("0"),
        eq(false));
    ImageRequest capturedImageRequest = argumentCaptor.getValue();
    assertSame(mImageRequest.getSourceUri(), capturedImageRequest.getSourceUri());
    verify(mRequestListener2).onRequestStart(
        argumentCaptor.capture(),
        eq(mCallerContext),
        eq("0"),
        eq(false));
    capturedImageRequest = argumentCaptor.getValue();
    assertSame(mImageRequest.getSourceUri(), capturedImageRequest.getSourceUri());
    ArgumentCaptor<ProducerContext> producerContextArgumentCaptor =
        ArgumentCaptor.forClass(ProducerContext.class);
    verify(encodedSequence)
        .produceResults(any(Consumer.class), producerContextArgumentCaptor.capture());
    assertTrue(producerContextArgumentCaptor.getValue().isIntermediateResultExpected());
    assertEquals(producerContextArgumentCaptor.getValue().getPriority(), Priority.HIGH);
  }

  @Test
  public void testFetchNetworkEncodedImage() {
    Producer<CloseableReference<PooledByteBuffer>> encodedSequence = mock(Producer.class);
    when(mProducerSequenceFactory.getEncodedImageProducerSequence(mImageRequest))
        .thenReturn(encodedSequence);
    when(mImageRequest.getSourceUri()).thenReturn(Uri.parse("http://test"));
    DataSource<CloseableReference<PooledByteBuffer>> dataSource =
        mImagePipeline.fetchEncodedImage(mImageRequest, mCallerContext);
    assertFalse(dataSource.isFinished());
    ArgumentCaptor<ImageRequest> argumentCaptor = ArgumentCaptor.forClass(ImageRequest.class);
    verify(mRequestListener1).onRequestStart(
        argumentCaptor.capture(),
        eq(mCallerContext),
        eq("0"),
        eq(false));
    ImageRequest capturedImageRequest = argumentCaptor.getValue();
    assertSame(mImageRequest.getSourceUri(), capturedImageRequest.getSourceUri());
    verify(mRequestListener2).onRequestStart(
        argumentCaptor.capture(),
        eq(mCallerContext),
        eq("0"),
        eq(false));
    capturedImageRequest = argumentCaptor.getValue();
    assertSame(mImageRequest.getSourceUri(), capturedImageRequest.getSourceUri());
    ArgumentCaptor<ProducerContext> producerContextArgumentCaptor =
        ArgumentCaptor.forClass(ProducerContext.class);
    verify(encodedSequence)
        .produceResults(any(Consumer.class), producerContextArgumentCaptor.capture());
    assertTrue(producerContextArgumentCaptor.getValue().isIntermediateResultExpected());
    assertEquals(producerContextArgumentCaptor.getValue().getPriority(), Priority.HIGH);
  }

  @Test
  public void testFetchDecodedImage() {
    Producer<CloseableReference<CloseableImage>> decodedSequence = mock(Producer.class);
    when(mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest))
        .thenReturn(decodedSequence);
    DataSource<CloseableReference<CloseableImage>> dataSource =
        mImagePipeline.fetchDecodedImage(mImageRequest, mCallerContext);
    assertFalse(dataSource.isFinished());
    verify(mRequestListener1).onRequestStart(mImageRequest, mCallerContext, "0", false);
    verify(mRequestListener2).onRequestStart(mImageRequest, mCallerContext, "0", false);
    ArgumentCaptor<ProducerContext> producerContextArgumentCaptor =
        ArgumentCaptor.forClass(ProducerContext.class);
    verify(decodedSequence)
        .produceResults(any(Consumer.class), producerContextArgumentCaptor.capture());
    assertTrue(producerContextArgumentCaptor.getValue().isIntermediateResultExpected());
    assertEquals(producerContextArgumentCaptor.getValue().getPriority(), Priority.HIGH);
  }

  @Test
  public void testFetchDecodedImageWithRequestLevel() {
    Producer<CloseableReference<CloseableImage>> decodedSequence = mock(Producer.class);
    when(mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest))
        .thenReturn(decodedSequence);
    DataSource<CloseableReference<CloseableImage>> dataSource =
        mImagePipeline.fetchDecodedImage(
            mImageRequest,
            mCallerContext,
            ImageRequest.RequestLevel.DISK_CACHE);
    assertFalse(dataSource.isFinished());
    verify(mRequestListener1).onRequestStart(mImageRequest, mCallerContext, "0", false);
    verify(mRequestListener2).onRequestStart(mImageRequest, mCallerContext, "0", false);
    ArgumentCaptor<ProducerContext> producerContextArgumentCaptor =
        ArgumentCaptor.forClass(ProducerContext.class);
    verify(decodedSequence)
        .produceResults(any(Consumer.class), producerContextArgumentCaptor.capture());
    assertTrue(producerContextArgumentCaptor.getValue().isIntermediateResultExpected());
    assertEquals(producerContextArgumentCaptor.getValue().getPriority(), Priority.HIGH);
    assertEquals(
        producerContextArgumentCaptor.getValue().getLowestPermittedRequestLevel(),
        ImageRequest.RequestLevel.DISK_CACHE);
  }

  @Test
  public void testFetchFromBitmapCacheDueToMethodCall() {
    Producer<CloseableReference<CloseableImage>> bitmapCacheSequence = mock(Producer.class);
    when(mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest))
        .thenReturn(bitmapCacheSequence);
    mImagePipeline.fetchImageFromBitmapCache(mImageRequest, mCallerContext);
    verify(mRequestListener1).onRequestStart(mImageRequest, mCallerContext, "0", false);
    verify(mRequestListener2).onRequestStart(mImageRequest, mCallerContext, "0", false);
    ArgumentCaptor<ProducerContext> producerContextArgumentCaptor =
        ArgumentCaptor.forClass(ProducerContext.class);
    verify(bitmapCacheSequence)
        .produceResults(any(Consumer.class), producerContextArgumentCaptor.capture());
    assertTrue(producerContextArgumentCaptor.getValue().isIntermediateResultExpected());
    assertEquals(producerContextArgumentCaptor.getValue().getPriority(), Priority.HIGH);
    assertEquals(
        producerContextArgumentCaptor.getValue().getLowestPermittedRequestLevel(),
        ImageRequest.RequestLevel.BITMAP_MEMORY_CACHE);
  }
  @Test
  public void testFetchFromBitmapCacheDueToImageRequest() {
    Producer<CloseableReference<CloseableImage>> bitmapCacheSequence = mock(Producer.class);
    when(mImageRequest.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.BITMAP_MEMORY_CACHE);
    when(mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest))
        .thenReturn(bitmapCacheSequence);
    mImagePipeline.fetchDecodedImage(mImageRequest, mCallerContext);
    verify(mRequestListener1).onRequestStart(mImageRequest, mCallerContext, "0",  false);
    verify(mRequestListener2).onRequestStart(mImageRequest, mCallerContext, "0", false);
    ArgumentCaptor<ProducerContext> producerContextArgumentCaptor =
        ArgumentCaptor.forClass(ProducerContext.class);
    verify(bitmapCacheSequence)
        .produceResults(any(Consumer.class), producerContextArgumentCaptor.capture());
    assertTrue(producerContextArgumentCaptor.getValue().isIntermediateResultExpected());
    assertEquals(producerContextArgumentCaptor.getValue().getPriority(), Priority.HIGH);
    assertEquals(
        producerContextArgumentCaptor.getValue().getLowestPermittedRequestLevel(),
        ImageRequest.RequestLevel.BITMAP_MEMORY_CACHE);
  }

  @Test
  public void testGetBitmapCacheGetSupplier() {
    Supplier<DataSource<CloseableReference<CloseableImage>>> dataSourceSupplier =
        mImagePipeline.getDataSourceSupplier(
            mImageRequest,
            mCallerContext,
            ImageRequest.RequestLevel.BITMAP_MEMORY_CACHE);
    Producer<CloseableReference<CloseableImage>> bitmapCacheSequence = mock(Producer.class);
    when(mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest))
        .thenReturn(bitmapCacheSequence);
    dataSourceSupplier.get();
    verify(mRequestListener1).onRequestStart(mImageRequest, mCallerContext, "0",  false);
    verify(mRequestListener2).onRequestStart(mImageRequest, mCallerContext, "0", false);
    ArgumentCaptor<ProducerContext> producerContextArgumentCaptor =
        ArgumentCaptor.forClass(ProducerContext.class);
    verify(bitmapCacheSequence)
        .produceResults(any(Consumer.class), producerContextArgumentCaptor.capture());
    assertTrue(producerContextArgumentCaptor.getValue().isIntermediateResultExpected());
    assertEquals(producerContextArgumentCaptor.getValue().getPriority(), Priority.HIGH);
  }

  @Test
  public void testGetFullFetchSupplier() {
    Supplier<DataSource<CloseableReference<CloseableImage>>> dataSourceSupplier = mImagePipeline
        .getDataSourceSupplier(mImageRequest, mCallerContext, ImageRequest.RequestLevel.FULL_FETCH);
    Producer<CloseableReference<CloseableImage>> decodedSequence = mock(Producer.class);
    when(mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest))
        .thenReturn(decodedSequence);
    DataSource<CloseableReference<CloseableImage>> dataSource = dataSourceSupplier.get();
    assertFalse(dataSource.isFinished());
    verify(mRequestListener1).onRequestStart(mImageRequest, mCallerContext, "0", false);
    verify(mRequestListener2).onRequestStart(mImageRequest, mCallerContext, "0", false);
    ArgumentCaptor<ProducerContext> producerContextArgumentCaptor =
        ArgumentCaptor.forClass(ProducerContext.class);
    verify(decodedSequence)
        .produceResults(any(Consumer.class), producerContextArgumentCaptor.capture());
    assertTrue(producerContextArgumentCaptor.getValue().isIntermediateResultExpected());
    assertEquals(producerContextArgumentCaptor.getValue().getPriority(), Priority.HIGH);
  }

  @Test
  public void testIdIncrementsOnEachRequest() {
    Producer<CloseableReference<CloseableImage>> bitmapCacheSequence = mock(Producer.class);
    when(mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest))
        .thenReturn(bitmapCacheSequence);
    mImagePipeline.fetchImageFromBitmapCache(mImageRequest, mCallerContext);
    verify(mRequestListener1).onRequestStart(mImageRequest, mCallerContext, "0", false);
    mImagePipeline.fetchImageFromBitmapCache(mImageRequest, mCallerContext);
    verify(mRequestListener1).onRequestStart(mImageRequest, mCallerContext, "1", false);
  }

  @Test
  public void testEvictFromMemoryCache() {
    String uriString = "http://dummy/string";
    Uri uri = Uri.parse(uriString);
    mImagePipeline.evictFromMemoryCache(uri);

    CacheKey dummyCacheKey = mock(CacheKey.class);

    ArgumentCaptor<Predicate> bitmapCachePredicateCaptor =
        ArgumentCaptor.forClass(Predicate.class);
    verify(mBitmapMemoryCache).removeAll(bitmapCachePredicateCaptor.capture());
    Predicate<CacheKey> bitmapMemoryCacheKeyPredicate =
        bitmapCachePredicateCaptor.getValue();
    BitmapMemoryCacheKey bitmapMemoryCacheKey1 = mock(BitmapMemoryCacheKey.class);
    BitmapMemoryCacheKey bitmapMemoryCacheKey2 = mock(BitmapMemoryCacheKey.class);
    when(bitmapMemoryCacheKey1.containsUri(uri)).thenReturn(true);
    when(bitmapMemoryCacheKey2.containsUri(uri)).thenReturn(false);
    assertTrue(bitmapMemoryCacheKeyPredicate.apply(bitmapMemoryCacheKey1));
    assertFalse(bitmapMemoryCacheKeyPredicate.apply(bitmapMemoryCacheKey2));
    assertFalse(bitmapMemoryCacheKeyPredicate.apply(dummyCacheKey));

    ArgumentCaptor<Predicate> encodedMemoryCachePredicateCaptor =
        ArgumentCaptor.forClass(Predicate.class);
    verify(mEncodedMemoryCache).removeAll(encodedMemoryCachePredicateCaptor.capture());
    Predicate<CacheKey> encodedMemoryCacheKeyPredicate =
        encodedMemoryCachePredicateCaptor.getValue();
    SimpleCacheKey simpleCacheKey1 = new SimpleCacheKey(uriString);
    SimpleCacheKey simpleCacheKey2 = new SimpleCacheKey("rubbish");
    assertTrue(encodedMemoryCacheKeyPredicate.apply(simpleCacheKey1));
    assertFalse(encodedMemoryCacheKeyPredicate.apply(simpleCacheKey2));
    assertFalse(encodedMemoryCacheKeyPredicate.apply(dummyCacheKey));
  }

  @Test
  public void testEvictFromDiskCache() {
    String uriString = "http://dummy/string";
    Uri uri = Uri.parse(uriString);
    CacheKey dummyCacheKey = mock(CacheKey.class);
    List<CacheKey> list = new ArrayList<>();
    list.add(dummyCacheKey);
    MultiCacheKey multiKey = new MultiCacheKey(list);
    when(mCacheKeyFactory.getEncodedCacheKey(any(ImageRequest.class), anyObject()))
        .thenReturn(multiKey);
    mImagePipeline.evictFromDiskCache(uri);
    verify(mMainDiskStorageCache).remove(multiKey);
    verify(mSmallImageDiskStorageCache).remove(multiKey);
  }

  @Test
  public void testClearMemoryCaches() {
    String uriString = "http://dummy/string";
    Uri uri = Uri.parse(uriString);
    CacheKey dummyCacheKey = mock(CacheKey.class);

    mImagePipeline.clearMemoryCaches();

    ArgumentCaptor<Predicate> bitmapCachePredicateCaptor =
        ArgumentCaptor.forClass(Predicate.class);
    verify(mBitmapMemoryCache).removeAll(bitmapCachePredicateCaptor.capture());
    Predicate<CacheKey> bitmapMemoryCacheKeyPredicate =
        bitmapCachePredicateCaptor.getValue();
    BitmapMemoryCacheKey bitmapMemoryCacheKey1 = mock(BitmapMemoryCacheKey.class);
    BitmapMemoryCacheKey bitmapMemoryCacheKey2 = mock(BitmapMemoryCacheKey.class);
    when(bitmapMemoryCacheKey1.containsUri(uri)).thenReturn(true);
    when(bitmapMemoryCacheKey2.containsUri(uri)).thenReturn(false);
    assertTrue(bitmapMemoryCacheKeyPredicate.apply(bitmapMemoryCacheKey1));
    assertTrue(bitmapMemoryCacheKeyPredicate.apply(bitmapMemoryCacheKey2));
    assertTrue(bitmapMemoryCacheKeyPredicate.apply(dummyCacheKey));

    ArgumentCaptor<Predicate> encodedMemoryCachePredicateCaptor =
        ArgumentCaptor.forClass(Predicate.class);
    verify(mEncodedMemoryCache).removeAll(encodedMemoryCachePredicateCaptor.capture());
    Predicate<CacheKey> encodedMemoryCacheKeyPredicate =
        encodedMemoryCachePredicateCaptor.getValue();
    SimpleCacheKey simpleCacheKey1 = new SimpleCacheKey(uriString);
    SimpleCacheKey simpleCacheKey2 = new SimpleCacheKey("rubbish");
    assertTrue(encodedMemoryCacheKeyPredicate.apply(simpleCacheKey1));
    assertTrue(encodedMemoryCacheKeyPredicate.apply(simpleCacheKey2));
    assertTrue(encodedMemoryCacheKeyPredicate.apply(dummyCacheKey));
  }

  @Test
  public void testIsInDiskCacheFromMainDiskCache() {
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.DEFAULT);
    when(mMainDiskStorageCache.diskCheckSync(any(CacheKey.class))).thenReturn(true);
    assertTrue(mImagePipeline.isInDiskCacheSync(mImageRequest));
  }

  @Test
  public void testIsInDiskCacheFromSmallDiskCache() {
    when(mImageRequest.getCacheChoice()).thenReturn(ImageRequest.CacheChoice.SMALL);
    when(mSmallImageDiskStorageCache.diskCheckSync(any(CacheKey.class))).thenReturn(true);
    assertTrue(mImagePipeline.isInDiskCacheSync(mImageRequest));
  }

  @Test
  public void testClearDiskCaches() {
    mImagePipeline.clearDiskCaches();
    verify(mMainDiskStorageCache).clearAll();
    verify(mSmallImageDiskStorageCache).clearAll();
  }

  @Test
  public void testDiskCachesSize() {
    mImagePipeline.getUsedDiskCacheSize();
    verify(mMainDiskStorageCache).getSize();
    verify(mSmallImageDiskStorageCache).getSize();
  }

  @Test
  public void testLocalRequestListenerIsCalled() {
    RequestListener localRequestListner = mock(RequestListener.class);
    when(mImageRequest.getRequestListener()).thenReturn(localRequestListner);

    Producer<CloseableReference<CloseableImage>> bitmapCacheSequence = mock(Producer.class);
    when(mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest))
        .thenReturn(bitmapCacheSequence);
    mImagePipeline.fetchImageFromBitmapCache(mImageRequest, mCallerContext);

    verify(localRequestListner).onRequestStart(mImageRequest, mCallerContext, "0", false);
    verify(mRequestListener1).onRequestStart(mImageRequest, mCallerContext, "0", false);
    verify(mRequestListener2).onRequestStart(mImageRequest, mCallerContext, "0", false);
  }
}
