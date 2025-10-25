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
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.RETURNS_MOCKS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

/** Tests {@link ProducerSequenceFactory}. */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
public class ProducerSequenceFactoryTest {

  @Mock public ImageRequest mImageRequest;
  @Mock public Postprocessor mPostprocessor;
  private final String mDummyMime = "dummy_mime";
  private Uri mUri;
  private ProducerSequenceFactory mProducerSequenceFactory;
  @Mock public NetworkFetcher mNetworkFetcher;
  @Mock public ThreadHandoffProducerQueue mThreadHandoffProducerQueue;

  private MockedStatic<UriUtil> mockedUriUtil;
  private MockedStatic<MediaUtils> mockedMediaUtils;

  @Before
  public void setUp() {
    mockedMediaUtils = mockStatic(MediaUtils.class);
    mockedUriUtil = mockStatic(UriUtil.class);
    MockitoAnnotations.initMocks(this);

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
            mThreadHandoffProducerQueue,
            DownsampleMode.AUTO,
            false,
            false,
            true,
            imageTranscoderFactory,
            false,
            false,
            false,
            null,
            5,
            false,
            false);

    when(mImageRequest.getLowestPermittedRequestLevel())
        .thenReturn(ImageRequest.RequestLevel.FULL_FETCH);
    mUri = Uri.parse("http://dummy");
    when(mImageRequest.getSourceUri()).thenReturn(mUri);
    mockedMediaUtils
        .when(() -> MediaUtils.extractMime(mUri.getPath()))
        .thenAnswer((Answer<String>) invocation -> mDummyMime);
    mockedMediaUtils
        .when(() -> MediaUtils.isVideo(mDummyMime))
        .thenAnswer((Answer<Boolean>) invocation -> false);
  }

  @After
  public void tearDownStaticMocks() {
    mockedUriUtil.close();
    mockedMediaUtils.close();
  }

  @Test
  public void testNetworkFullFetch() {
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    Producer producer = mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertThat(producer).isSameAs(mProducerSequenceFactory.getNetworkFetchSequence());
  }

  @Test
  public void testCustomSequenceFetchIsCalled() {
    RecordingCustomProducerSequenceFactoryIsCalled producerSequenceFactory =
        new RecordingCustomProducerSequenceFactoryIsCalled();
    internalUseSequenceFactoryWithCustomSequence(producerSequenceFactory);
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_UNKNOWN);

    Producer producer = mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertThat(producerSequenceFactory.isCalled).isSameAs(true);
  }

  @Test
  public void testCustomSequenceFetchNotCalled() {
    RecordingCustomProducerSequenceFactoryIsCalled producerSequenceFactory =
        new RecordingCustomProducerSequenceFactoryIsCalled();
    internalUseSequenceFactoryWithCustomSequence(producerSequenceFactory);
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);

    Producer producer = mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertThat(producerSequenceFactory.isCalled).isSameAs(false);
  }

  @Test
  public void testNetworkFullPrefetch() {
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    Producer<Void> producer =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertThat(producer)
        .isSameAs(
            mProducerSequenceFactory
                .getCloseableImagePrefetchSequences()
                .get(mProducerSequenceFactory.getNetworkFetchSequence()));
  }

  @Test
  public void testLocalFileFetchToEncodedMemory() {
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_IMAGE_FILE);
    Producer<CloseableReference<PooledByteBuffer>> producer =
        mProducerSequenceFactory.getEncodedImageProducerSequence(mImageRequest);
    assertThat(producer)
        .isSameAs(mProducerSequenceFactory.getLocalFileFetchEncodedImageProducerSequence());
    // Same for Video
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_IMAGE_FILE);
    producer = mProducerSequenceFactory.getEncodedImageProducerSequence(mImageRequest);
    assertThat(producer)
        .isSameAs(mProducerSequenceFactory.getLocalFileFetchEncodedImageProducerSequence());
  }

  @Test
  public void testNetworkFetchToEncodedMemory() {
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    Producer<CloseableReference<PooledByteBuffer>> producer =
        mProducerSequenceFactory.getEncodedImageProducerSequence(mImageRequest);
    assertThat(producer)
        .isSameAs(mProducerSequenceFactory.getNetworkFetchEncodedImageProducerSequence());
  }

  @Test
  public void testLocalFileFetchToEncodedMemoryPrefetch() {
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_VIDEO_FILE);
    Producer<Void> producer =
        mProducerSequenceFactory.getEncodedImagePrefetchProducerSequence(mImageRequest);
    assertThat(producer)
        .isSameAs(mProducerSequenceFactory.getLocalFileFetchToEncodedMemoryPrefetchSequence());
    // Same for image
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_IMAGE_FILE);
    producer = mProducerSequenceFactory.getEncodedImagePrefetchProducerSequence(mImageRequest);
    assertThat(producer)
        .isSameAs(mProducerSequenceFactory.getLocalFileFetchToEncodedMemoryPrefetchSequence());
  }

  @Test
  public void testNetworkFetchToEncodedMemoryPrefetch() {
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    Producer<Void> producer =
        mProducerSequenceFactory.getEncodedImagePrefetchProducerSequence(mImageRequest);
    assertThat(producer)
        .isSameAs(mProducerSequenceFactory.getNetworkFetchToEncodedMemoryPrefetchSequence());
  }

  @Test
  public void testLocalImageFileFullFetch() {
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_IMAGE_FILE);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertThat(producer).isSameAs(mProducerSequenceFactory.getLocalImageFileFetchSequence());
  }

  @Test
  public void testLocalImageFileFullPrefetch() {
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_IMAGE_FILE);
    Producer<Void> producer =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertThat(producer)
        .isSameAs(
            mProducerSequenceFactory
                .getCloseableImagePrefetchSequences()
                .get(mProducerSequenceFactory.getLocalImageFileFetchSequence()));
  }

  @Test
  public void testLocalVideoFileFullFetch() {
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_VIDEO_FILE);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertThat(producer).isSameAs(mProducerSequenceFactory.getLocalVideoFileFetchSequence());
  }

  @Test
  public void testLocalVideoFileFullPrefetch() {
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_VIDEO_FILE);
    Producer<Void> producer =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertThat(producer)
        .isSameAs(
            mProducerSequenceFactory
                .getCloseableImagePrefetchSequences()
                .get(mProducerSequenceFactory.getLocalVideoFileFetchSequence()));
  }

  @Test
  public void testLocalContentUriFullFetch() {
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_CONTENT);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertThat(producer).isSameAs(mProducerSequenceFactory.getLocalContentUriFetchSequence());
  }

  @Test
  public void testLocalContentUriFullPrefetch() {
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_CONTENT);
    Producer<Void> producer =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertThat(producer)
        .isSameAs(
            mProducerSequenceFactory
                .getCloseableImagePrefetchSequences()
                .get(mProducerSequenceFactory.getLocalContentUriFetchSequence()));
  }

  @Test
  public void testLocalResourceFullFetch() {
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_RESOURCE);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertThat(producer).isSameAs(mProducerSequenceFactory.getLocalResourceFetchSequence());
  }

  @Test
  public void testLocalAssetFullFetch() {
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_ASSET);
    Producer<CloseableReference<CloseableImage>> producer =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertThat(producer).isSameAs(mProducerSequenceFactory.getLocalAssetFetchSequence());
  }

  @Test
  public void testLocalAssetAndResourceFullPrefetch() {
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_RESOURCE);
    Producer<Void> localResourceSequence =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertThat(localResourceSequence)
        .isSameAs(
            mProducerSequenceFactory
                .getCloseableImagePrefetchSequences()
                .get(mProducerSequenceFactory.getLocalResourceFetchSequence()));
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_ASSET);
    Producer localAssetSequence =
        mProducerSequenceFactory.getDecodedImagePrefetchProducerSequence(mImageRequest);
    assertThat(localAssetSequence)
        .isSameAs(
            mProducerSequenceFactory
                .getCloseableImagePrefetchSequences()
                .get(mProducerSequenceFactory.getLocalAssetFetchSequence()));
    assertThat(localAssetSequence).isNotSameAs(localResourceSequence);
  }

  @Test
  public void testPostprocess() {
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    when(mImageRequest.getPostprocessor()).thenReturn(mPostprocessor);
    Producer<CloseableReference<CloseableImage>> networkSequence =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertThat(networkSequence)
        .isSameAs(
            mProducerSequenceFactory
                .getPostprocessorSequences()
                .get(mProducerSequenceFactory.getNetworkFetchSequence()));

    // each source type should be different
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_LOCAL_CONTENT);

    Producer<CloseableReference<CloseableImage>> localSequence =
        mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertThat(localSequence)
        .isSameAs(
            mProducerSequenceFactory
                .getPostprocessorSequences()
                .get(mProducerSequenceFactory.getLocalContentUriFetchSequence()));
    assertThat(networkSequence).isNotSameAs(localSequence);

    // encoded return types don't get postprocessed
    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    Producer<CloseableReference<PooledByteBuffer>> encodedSequence =
        mProducerSequenceFactory.getEncodedImageProducerSequence(mImageRequest);
    assertThat(encodedSequence)
        .isSameAs(mProducerSequenceFactory.getNetworkFetchEncodedImageProducerSequence());
    assertThat(
            mProducerSequenceFactory
                .getPostprocessorSequences()
                .get(mProducerSequenceFactory.getBackgroundNetworkFetchToEncodedMemorySequence()))
        .isNull();
  }

  @Test
  public void testPrepareBitmapFactoryDefault() {
    internalUseSequenceFactoryWithBitmapPrepare();

    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);

    Producer producer = mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertThat(producer)
        .isSameAs(
            mProducerSequenceFactory
                .getBitmapPrepareSequences()
                .get(mProducerSequenceFactory.getNetworkFetchSequence()));
  }

  @Test
  public void testPrepareBitmapFactoryWithPostprocessor() {
    internalUseSequenceFactoryWithBitmapPrepare();

    when(mImageRequest.getSourceUriType()).thenReturn(SOURCE_TYPE_NETWORK);
    when(mImageRequest.getPostprocessor()).thenReturn(mPostprocessor);

    Producer producer = mProducerSequenceFactory.getDecodedImageProducerSequence(mImageRequest);
    assertThat(producer)
        .isSameAs(
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
            mThreadHandoffProducerQueue,
            DownsampleMode.AUTO,
            /* useBitmapPrepareToDraw */ true,
            false,
            true,
            imageTranscoderFactory,
            false,
            false,
            false,
            null,
            5,
            false,
            false);
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
            mThreadHandoffProducerQueue,
            DownsampleMode.AUTO,
            false,
            false,
            true,
            imageTranscoderFactory,
            false,
            false,
            false,
            Collections.singleton(customProducerSequenceFactory),
            5,
            false,
            false);
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
