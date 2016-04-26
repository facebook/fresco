/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.core;

import android.net.Uri;

import com.facebook.common.media.MediaUtils;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.UriUtil;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.Postprocessor;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests {@link ProducerSequenceFactory}.
 */
@RunWith(RobolectricTestRunner.class)
@PrepareForTest({UriUtil.class, MediaUtils.class})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "android.*"})
@Config(manifest = Config.NONE)
public class ProducerSequenceFactoryTest {

  @Mock public ImageRequest mImageRequest;
  @Mock public Postprocessor mPostprocessor;
  private final String mDummyMime = "dummy_mime";
  private Uri mUri;
  private ProducerSequenceFactory mProducerSequenceFactory;

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    PowerMockito.mockStatic(UriUtil.class, MediaUtils.class);

    ProducerFactory producerFactory = mock(ProducerFactory.class, RETURNS_MOCKS);

    mProducerSequenceFactory =
        new ProducerSequenceFactory(producerFactory, null, true, true, false, null);

    when(mImageRequest.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.FULL_FETCH);
    mUri = Uri.parse("http://dummy");
    when(mImageRequest.getSourceUri()).thenReturn(mUri);
    when(MediaUtils.extractMime(mUri.getPath())).thenReturn(mDummyMime);
    when(MediaUtils.isVideo(mDummyMime)).thenReturn(false);
  }

  @Test
  public void testNetworkFullFetch() {
    PowerMockito.when(UriUtil.isNetworkUri(mUri)).thenReturn(true);
    Producer producer = mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mNetworkFetchSequence);
  }

  @Test
  public void testNetworkFullPrefetch() {
    PowerMockito.when(UriUtil.isNetworkUri(mUri)).thenReturn(true);
    Producer<Void> producer =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(
        producer,
        mProducerSequenceFactory.mCloseableImagePrefetchSequences.get(
            mProducerSequenceFactory.mNetworkFetchSequence));
  }

  @Test
  public void testNetworkFetchToEncodedMemory() {
    PowerMockito.when(UriUtil.isNetworkUri(mUri)).thenReturn(true);
    Producer<CloseableReference<PooledByteBuffer>> producer =
        mProducerSequenceFactory.getEncodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mEncodedImageProducerSequence);
  }

  @Test
  public void testNetworkFetchToEncodedMemoryPrefetch() {
    PowerMockito.when(UriUtil.isNetworkUri(mUri)).thenReturn(true);
    Producer<Void> producer =
        mProducerSequenceFactory.getEncodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mNetworkFetchToEncodedMemoryPrefetchSequence);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testEncodedBytesNotAllowedForLocalFiles() {
    PowerMockito.when(UriUtil.isLocalFileUri(mUri)).thenReturn(true);
    mProducerSequenceFactory.getEncodedImageProducerSequence(mImageRequest);
  }

  @Test
  public void testLocalImageFileFullFetch() {
    PowerMockito.when(UriUtil.isLocalFileUri(mUri)).thenReturn(true);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mLocalImageFileFetchSequence);
  }

  @Test
  public void testLocalImageFileFullPrefetch() {
    PowerMockito.when(UriUtil.isLocalFileUri(mUri)).thenReturn(true);
    Producer<Void> producer =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(
        producer,
        mProducerSequenceFactory.mCloseableImagePrefetchSequences.get(
            mProducerSequenceFactory.mLocalImageFileFetchSequence));
  }

  @Test
  public void testLocalVideoFileFullFetch() {
    PowerMockito.when(UriUtil.isLocalFileUri(mUri)).thenReturn(true);
    when(MediaUtils.isVideo(mDummyMime)).thenReturn(true);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mLocalVideoFileFetchSequence);
  }

  @Test
  public void testLocalVideoFileFullPrefetch() {
    PowerMockito.when(UriUtil.isLocalFileUri(mUri)).thenReturn(true);
    when(MediaUtils.isVideo(mDummyMime)).thenReturn(true);
    Producer<Void> producer =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(
        producer,
        mProducerSequenceFactory.mCloseableImagePrefetchSequences.get(
            mProducerSequenceFactory.mLocalVideoFileFetchSequence));
  }

  @Test
  public void testLocalContentUriFullFetch() {
    PowerMockito.when(UriUtil.isLocalContentUri(mUri)).thenReturn(true);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mLocalContentUriFetchSequence);
  }

  @Test
  public void testLocalContentUriFullPrefetch() {
    PowerMockito.when(UriUtil.isLocalContentUri(mUri)).thenReturn(true);
    Producer<Void> producer =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(
        producer,
        mProducerSequenceFactory.mCloseableImagePrefetchSequences.get(
            mProducerSequenceFactory.mLocalContentUriFetchSequence));
  }

  @Test
  public void testLocalResourceFullFetch() {
    PowerMockito.when(UriUtil.isLocalResourceUri(mUri)).thenReturn(true);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mLocalResourceFetchSequence);
  }

  @Test
  public void testLocalAssetFullFetch() {
    PowerMockito.when(UriUtil.isLocalAssetUri(mUri)).thenReturn(true);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mLocalAssetFetchSequence);
  }

  @Test
  public void testLocalAssetAndResourceFullPrefetch() {
    PowerMockito.when(UriUtil.isLocalResourceUri(mUri)).thenReturn(true);
    Producer<Void> localResourceSequence =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(
        localResourceSequence,
        mProducerSequenceFactory.mCloseableImagePrefetchSequences.get(
            mProducerSequenceFactory.mLocalResourceFetchSequence));
    PowerMockito.when(UriUtil.isLocalResourceUri(mUri)).thenReturn(false);
    PowerMockito.when(UriUtil.isLocalAssetUri(mUri)).thenReturn(true);
    Producer localAssetSequence =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(
        localAssetSequence,
        mProducerSequenceFactory.mCloseableImagePrefetchSequences.get(
            mProducerSequenceFactory.mLocalAssetFetchSequence));
    assertNotSame(localAssetSequence, localResourceSequence);
  }

  @Test
  public void testPostprocess() {
    PowerMockito.when(UriUtil.isNetworkUri(mUri)).thenReturn(true);
    when(mImageRequest.getPostprocessor()).thenReturn(mPostprocessor);
    Producer<CloseableReference<CloseableImage>> networkSequence =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(
        networkSequence,
        mProducerSequenceFactory.mPostprocessorSequences.get(
            mProducerSequenceFactory.mNetworkFetchSequence));

    // each source type should be different
    PowerMockito.when(UriUtil.isNetworkUri(mUri)).thenReturn(false);
    PowerMockito.when(UriUtil.isLocalContentUri(mUri)).thenReturn(true);
    Producer<CloseableReference<CloseableImage>> localSequence =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(
        localSequence,
        mProducerSequenceFactory.mPostprocessorSequences.get(
            mProducerSequenceFactory.mLocalContentUriFetchSequence));
    assertNotSame(networkSequence, localSequence);

    // encoded return types don't get postprocessed
    PowerMockito.when(UriUtil.isNetworkUri(mUri)).thenReturn(true);
    Producer<CloseableReference<PooledByteBuffer>> encodedSequence =
        mProducerSequenceFactory.getEncodedImageProducerSequence(mImageRequest);
    assertSame(
        encodedSequence,
        mProducerSequenceFactory.mEncodedImageProducerSequence);
    assertNull(
        mProducerSequenceFactory.mPostprocessorSequences.get(
            mProducerSequenceFactory.mBackgroundNetworkFetchToEncodedMemorySequence));
  }
}
