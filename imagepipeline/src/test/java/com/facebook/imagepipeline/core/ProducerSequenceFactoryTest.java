/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
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
import static com.facebook.imagepipeline.common.SourceUriType.SOURCE_TYPE_UNKNOWN;
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
import com.facebook.imagepipeline.producers.Consumer;
import com.facebook.imagepipeline.producers.CustomProducerSequenceFactory;
import com.facebook.imagepipeline.producers.NetworkFetcher;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.ProducerContext;
import com.facebook.imagepipeline.producers.ThreadHandoffProducerQueue;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.Postprocessor;
import com.facebook.imagepipeline.transcoder.ImageTranscoder;
import com.facebook.imagepipeline.transcoder.ImageTranscoderFactory;
import java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
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
  @Mock public NetworkFetcher mNetworkFetcher;
  @Mock public ThreadHandoffProducerQueue mThreadHandoffProducerQueue;

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
            mNetworkFetcher,
            true,
            false,
            mThreadHandoffProducerQueue,
            DownsampleMode.AUTO,
            false,
            false,
            true,
            imageTranscoderFactory,
            false,
            false,
            false,
            null);

    when(mImageRequest.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.FULL_FETCH);
    mUri = Uri.parse("http://dummy");
    when(mImageRequest.getSourceUri()).thenReturn(mUri);
    when(MediaUtils.extractMime(mUri.getPath()))
        .thenAnswer((Answer<String>) invocation -> mDummyMime);
    when(MediaUtils.isVideo(mDummyMime)).thenAnswer((Answer<Boolean>) invocation -> false);
  }

  @Test
  public void testNetworkFullFetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    Producer producer = mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.getNetworkFetchSequence());
  }

  @Test
  public void testCustomSequenceFetchIsCalled() {
    RecordingCustomProducerSequenceFactoryIsCalled producerSequenceFactory =
        new RecordingCustomProducerSequenceFactoryIsCalled();
    internalUseSequenceFactoryWithCustomSequence(producerSequenceFactory);
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_UNKNOWN);

    Producer producer = mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producerSequenceFactory.isCalled, true);
  }

  @Test
  public void testCustomSequenceFetchNotCalled() {
    RecordingCustomProducerSequenceFactoryIsCalled producerSequenceFactory =
        new RecordingCustomProducerSequenceFactoryIsCalled();
    internalUseSequenceFactoryWithCustomSequence(producerSequenceFactory);
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);

    Producer producer = mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producerSequenceFactory.isCalled, false);
  }

  @Test
  public void testNetworkFullPrefetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    Producer<Void> producer =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(
        producer,
        mProducerSequenceFactory
            .getCloseableImagePrefetchSequences()
            .get(mProducerSequenceFactory.getNetworkFetchSequence()));
  }

  @Test
  public void testLocalFileFetchToEncodedMemory() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_IMAGE_FILE);
    Producer<CloseableReference<PooledByteBuffer>> producer =
        mProducerSequenceFactory.getEncodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.getLocalFileFetchEncodedImageProducerSequence());
    // Same for Video
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_IMAGE_FILE);
    producer = mProducerSequenceFactory.getEncodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.getLocalFileFetchEncodedImageProducerSequence());
  }

  @Test
  public void testNetworkFetchToEncodedMemory() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    Producer<CloseableReference<PooledByteBuffer>> producer =
        mProducerSequenceFactory.getEncodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.getNetworkFetchEncodedImageProducerSequence());
  }

  @Test
  public void testLocalFileFetchToEncodedMemoryPrefetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_VIDEO_FILE);
    Producer<Void> producer =
        mProducerSequenceFactory.getEncodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(
        producer, mProducerSequenceFactory.getLocalFileFetchToEncodedMemoryPrefetchSequence());
    // Same for image
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_IMAGE_FILE);
    producer = mProducerSequenceFactory.getEncodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(
        producer, mProducerSequenceFactory.getLocalFileFetchToEncodedMemoryPrefetchSequence());
  }

  @Test
  public void testNetworkFetchToEncodedMemoryPrefetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    Producer<Void> producer =
        mProducerSequenceFactory.getEncodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.getNetworkFetchToEncodedMemoryPrefetchSequence());
  }

  @Test
  public void testLocalImageFileFullFetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_IMAGE_FILE);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.getLocalImageFileFetchSequence());
  }

  @Test
  public void testLocalImageFileFullPrefetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_IMAGE_FILE);
    Producer<Void> producer =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(
        producer,
        mProducerSequenceFactory
            .getCloseableImagePrefetchSequences()
            .get(mProducerSequenceFactory.getLocalImageFileFetchSequence()));
  }

  @Test
  public void testLocalVideoFileFullFetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_VIDEO_FILE);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.getLocalVideoFileFetchSequence());
  }

  @Test
  public void testLocalVideoFileFullPrefetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_VIDEO_FILE);
    Producer<Void> producer =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(
        producer,
        mProducerSequenceFactory
            .getCloseableImagePrefetchSequences()
            .get(mProducerSequenceFactory.getLocalVideoFileFetchSequence()));
  }

  @Test
  public void testLocalContentUriFullFetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_CONTENT);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.getLocalContentUriFetchSequence());
  }

  @Test
  public void testLocalContentUriFullPrefetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_CONTENT);
    Producer<Void> producer =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(
        producer,
        mProducerSequenceFactory
            .getCloseableImagePrefetchSequences()
            .get(mProducerSequenceFactory.getLocalContentUriFetchSequence()));
  }

  @Test
  public void testLocalResourceFullFetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_RESOURCE);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.getLocalResourceFetchSequence());
  }

  @Test
  public void testLocalAssetFullFetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_ASSET);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(producer, mProducerSequenceFactory.getLocalAssetFetchSequence());
  }

  @Test
  public void testLocalAssetAndResourceFullPrefetch() {
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_RESOURCE);
    Producer<Void> localResourceSequence =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(
        localResourceSequence,
        mProducerSequenceFactory
            .getCloseableImagePrefetchSequences()
            .get(mProducerSequenceFactory.getLocalResourceFetchSequence()));
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_ASSET);
    Producer localAssetSequence =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertSame(
        localAssetSequence,
        mProducerSequenceFactory
            .getCloseableImagePrefetchSequences()
            .get(mProducerSequenceFactory.getLocalAssetFetchSequence()));
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
        mProducerSequenceFactory
            .getPostprocessorSequences()
            .get(mProducerSequenceFactory.getNetworkFetchSequence()));

    // each source type should be different
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_CONTENT);

    Producer<CloseableReference<CloseableImage>> localSequence =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(
        localSequence,
        mProducerSequenceFactory
            .getPostprocessorSequences()
            .get(mProducerSequenceFactory.getLocalContentUriFetchSequence()));
    assertNotSame(networkSequence, localSequence);

    // encoded return types don't get postprocessed
    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    Producer<CloseableReference<PooledByteBuffer>> encodedSequence =
        mProducerSequenceFactory.getEncodedImageProducerSequence(mImageRequest);
    assertSame(
        encodedSequence, mProducerSequenceFactory.getNetworkFetchEncodedImageProducerSequence());
    assertNull(
        mProducerSequenceFactory
            .getPostprocessorSequences()
            .get(mProducerSequenceFactory.getBackgroundNetworkFetchToEncodedMemorySequence()));
  }

  @Test
  public void testPrepareBitmapFactoryDefault() {
    internalUseSequenceFactoryWithBitmapPrepare();

    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);

    Producer producer = mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(
        producer,
        mProducerSequenceFactory
            .getBitmapPrepareSequences()
            .get(mProducerSequenceFactory.getNetworkFetchSequence()));
  }

  @Test
  public void testPrepareBitmapFactoryWithPostprocessor() {
    internalUseSequenceFactoryWithBitmapPrepare();

    PowerMockito.when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    when(mImageRequest.getPostprocessor()).thenReturn(mPostprocessor);

    Producer producer = mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertSame(
        producer,
        mProducerSequenceFactory
            .getBitmapPrepareSequences()
            .get(
                mProducerSequenceFactory
                    .getPostprocessorSequences()
                    .get(mProducerSequenceFactory.getNetworkFetchSequence())));
  }

  private void internalUseSequenceFactoryWithBitmapPrepare() {
    ProducerFactory producerFactory = mock(ProducerFactory.class, RETURNS_MOCKS);
    ImageTranscoderFactory imageTranscoderFactory = mock(ImageTranscoderFactory.class);

    mProducerSequenceFactory =
        new ProducerSequenceFactory(
            RuntimeEnvironment.application.getContentResolver(),
            producerFactory,
            mNetworkFetcher,
            true,
            false,
            mThreadHandoffProducerQueue,
            DownsampleMode.AUTO,
            /* useBitmapPrepareToDraw */ true,
            false,
            true,
            imageTranscoderFactory,
            false,
            false,
            false,
            null);
  }

  private void internalUseSequenceFactoryWithCustomSequence(
      CustomProducerSequenceFactory customProducerSequenceFactory) {
    ProducerFactory producerFactory = mock(ProducerFactory.class, RETURNS_MOCKS);
    ImageTranscoderFactory imageTranscoderFactory = mock(ImageTranscoderFactory.class);

    mProducerSequenceFactory =
        new ProducerSequenceFactory(
            RuntimeEnvironment.application.getContentResolver(),
            producerFactory,
            mNetworkFetcher,
            true,
            false,
            mThreadHandoffProducerQueue,
            DownsampleMode.AUTO,
            false,
            false,
            true,
            imageTranscoderFactory,
            false,
            false,
            false,
            Collections.singleton(customProducerSequenceFactory));
  }

  private static class RecordingCustomProducerSequenceFactoryIsCalled
      extends CustomProducerSequenceFactory {

    boolean isCalled = false;

    @Override
    public Producer<CloseableReference<CloseableImage>> getCustomDecodedImageSequence(
        ImageRequest imageRequest, ProducerSequenceFactory producerSequenceFactory) {
      this.isCalled = true;
      return new DummyProducer();
    }
  }

  private static class DummyProducer implements Producer<CloseableReference<CloseableImage>> {

    @Override
    public void produceResults(
        Consumer<CloseableReference<CloseableImage>> consumer, ProducerContext context) {}
  }
}
