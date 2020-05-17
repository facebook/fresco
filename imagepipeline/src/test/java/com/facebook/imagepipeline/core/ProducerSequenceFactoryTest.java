/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.core;

import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_ASSET;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_CONTENT;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_IMAGE_FILE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_RESOURCE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_LOCAL_VIDEO_FILE;
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_NETWORK;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import android.net.Uri;
import com.facebook.common.media.MediaUtils;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.UriUtil;
import com.facebook.imageformat.ImageFormat;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.Postprocessor;
import com.facebook.imagepipeline.transcoder.ImageTranscoder;
import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory;
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
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Tests {@link ProducerSequenceFactory}. */
@RunWith(RobolectricTestRunner.class)
@PrepareForTest({UriUtil.class, MediaUtils.class})
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
@Config(manifest = Config.NONE)
public class ProducerSequenceFactoryTest {

  @Mock public ImageRequest mImageRequest;
  @Mock public Postprocessor mPostprocessor;
  private final String mDummyMime = "dummy_mime";
  private Uri mUri;
  private ProducerSequenceFactory mProducerSequenceFactory;

  @Rule public PowerMockRule rule = new PowerMockRule();

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    PowerMockito.mockStatic(UriUtil.class, MediaUtils.class);

    ProducerFactory producerFactory = mock(ProducerFactory.class, RETURNS_MOCKS);
    ImageTranscoder imageTranscoder = mock(ImageTranscoder.class);
    ImageTranscoderFactory imageTranscoderFactory = mock(ImageTranscoderFactory.class);
    when(imageTranscoderFactory.createImageTranscoder(any(ImageFormat.class), anyBoolean()))
        .thenReturn(imageTranscoder);

    mProducerSequenceFactory =
        new ProducerSequenceFactory(
            RuntimeEnvironment.application.getContentResolver(),
            producerFactory,
            null,
            true,
            false,
            null,
            false,
            false,
            false,
            true,
            imageTranscoderFactory,
            false,
            false);

    when(mImageRequest.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.FULL_FETCH);
    mUri = Uri.parse("http://dummy");
    when(mImageRequest.getSourceUri()).thenReturn(mUri);
    when(MediaUtils.extractMime(mUri.getPath())).thenReturn(mDummyMime);
    when(MediaUtils.isVideo(mDummyMime)).thenReturn(false);
  }

  @Test
  public void testNetworkFullFetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    Producer producer = mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mNetworkFetchSequence);
  }

  @Test
  public void testNetworkFullPrefetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    Producer<Void> producer =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(
        producer,
        mProducerSequenceFactory.mCloseableImagePrefetchSequences.get(
            mProducerSequenceFactory.mNetworkFetchSequence));
  }

  @Test
  public void testLocalFileFetchToEncodedMemory() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_IMAGE_FILE);
    Producer<CloseableReference<PooledByteBuffer>> producer =
        mProducerSequenceFactory.getEncodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mLocalFileEncodedImageProducerSequence);
    // Same for Video
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_IMAGE_FILE);
    producer = mProducerSequenceFactory.getEncodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mLocalFileEncodedImageProducerSequence);
  }

  @Test
  public void testNetworkFetchToEncodedMemory() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    Producer<CloseableReference<PooledByteBuffer>> producer =
        mProducerSequenceFactory.getEncodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mNetworkEncodedImageProducerSequence);
  }

  @Test
  public void testLocalFileFetchToEncodedMemoryPrefetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_VIDEO_FILE);
    Producer<Void> producer =
        mProducerSequenceFactory.getEncodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mLocalFileFetchToEncodedMemoryPrefetchSequence);
    // Same for image
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_IMAGE_FILE);
    producer = mProducerSequenceFactory.getEncodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mLocalFileFetchToEncodedMemoryPrefetchSequence);
  }

  @Test
  public void testNetworkFetchToEncodedMemoryPrefetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    Producer<Void> producer =
        mProducerSequenceFactory.getEncodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mNetworkFetchToEncodedMemoryPrefetchSequence);
  }

  @Test
  public void testLocalImageFileFullFetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_IMAGE_FILE);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mLocalImageFileFetchSequence);
  }

  @Test
  public void testLocalImageFileFullPrefetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_IMAGE_FILE);
    Producer<Void> producer =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(
        producer,
        mProducerSequenceFactory.mCloseableImagePrefetchSequences.get(
            mProducerSequenceFactory.mLocalImageFileFetchSequence));
  }

  @Test
  public void testLocalVideoFileFullFetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_VIDEO_FILE);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mLocalVideoFileFetchSequence);
  }

  @Test
  public void testLocalVideoFileFullPrefetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_VIDEO_FILE);
    Producer<Void> producer =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(
        producer,
        mProducerSequenceFactory.mCloseableImagePrefetchSequences.get(
            mProducerSequenceFactory.mLocalVideoFileFetchSequence));
  }

  @Test
  public void testLocalContentUriFullFetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_CONTENT);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mLocalContentUriFetchSequence);
  }

  @Test
  public void testLocalContentUriFullPrefetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_CONTENT);
    Producer<Void> producer =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(
        producer,
        mProducerSequenceFactory.mCloseableImagePrefetchSequences.get(
            mProducerSequenceFactory.mLocalContentUriFetchSequence));
  }

  @Test
  public void testLocalResourceFullFetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_RESOURCE);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mLocalResourceFetchSequence);
  }

  @Test
  public void testLocalAssetFullFetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_ASSET);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.mLocalAssetFetchSequence);
  }

  @Test
  public void testLocalAssetAndResourceFullPrefetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_RESOURCE);
    Producer<Void> localResourceSequence =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(
        localResourceSequence,
        mProducerSequenceFactory.mCloseableImagePrefetchSequences.get(
            mProducerSequenceFactory.mLocalResourceFetchSequence));
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_ASSET);
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
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    when(mImageRequest.getPostprocessor()).thenReturn(mPostprocessor);
    Producer<CloseableReference<CloseableImage>> networkSequence =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(
        networkSequence,
        mProducerSequenceFactory.mPostprocessorSequences.get(
            mProducerSequenceFactory.mNetworkFetchSequence));

    // each source type should be different
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_CONTENT);

    Producer<CloseableReference<CloseableImage>> localSequence =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(
        localSequence,
        mProducerSequenceFactory.mPostprocessorSequences.get(
            mProducerSequenceFactory.mLocalContentUriFetchSequence));
    assertNotSame(networkSequence, localSequence);

    // encoded return types don't get postprocessed
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    Producer<CloseableReference<PooledByteBuffer>> encodedSequence =
        mProducerSequenceFactory.getEncodedImageProducerSequence(mImageRequest);
    assertSame(encodedSequence, mProducerSequenceFactory.mNetworkEncodedImageProducerSequence);
    assertNull(
        mProducerSequenceFactory.mPostprocessorSequences.get(
            mProducerSequenceFactory.mBackgroundNetworkFetchToEncodedMemorySequence));
  }

  @Test
  public void testPrepareBitmapFactoryDefault() {
    internalUseSequenceFactoryWithBitmapPrepare();

    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);

    Producer producer = mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(
        producer,
        mProducerSequenceFactory.mBitmapPrepareSequences.get(
            mProducerSequenceFactory.mNetworkFetchSequence));
  }

  @Test
  public void testPrepareBitmapFactoryWithPostprocessor() {
    internalUseSequenceFactoryWithBitmapPrepare();

    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    when(mImageRequest.getPostprocessor()).thenReturn(mPostprocessor);

    Producer producer = mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(
        producer,
        mProducerSequenceFactory.mBitmapPrepareSequences.get(
            mProducerSequenceFactory.mPostprocessorSequences.get(
                mProducerSequenceFactory.mNetworkFetchSequence)));
  }

  private void internalUseSequenceFactoryWithBitmapPrepare() {
    ProducerFactory producerFactory = mock(ProducerFactory.class, RETURNS_MOCKS);
    ImageTranscoderFactory imageTranscoderFactory = mock(ImageTranscoderFactory.class);

    mProducerSequenceFactory =
        new ProducerSequenceFactory(
            RuntimeEnvironment.application.getContentResolver(),
            producerFactory,
            null,
            true,
            false,
            null,
            false,
            /* useBitmapPrepareToDraw */ true,
            false,
            true,
            imageTranscoderFactory,
            false,
            false);
  }
}
