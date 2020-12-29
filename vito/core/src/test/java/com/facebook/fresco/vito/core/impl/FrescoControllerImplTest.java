/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.executors.UiThreadImmediateExecutorService;
import com.facebook.common.references.CloseableReference;
import com.facebook.datasource.SimpleDataSource;
import com.facebook.fresco.vito.core.FrescoContext;
import com.facebook.fresco.vito.core.FrescoDrawable;
import com.facebook.fresco.vito.core.FrescoExperiments;
import com.facebook.fresco.vito.core.FrescoState;
import com.facebook.fresco.vito.core.Hierarcher;
import com.facebook.fresco.vito.core.impl.debug.NoOpDebugOverlayFactory;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.core.ImagePipeline;
import com.facebook.imagepipeline.core.ProducerSequenceFactory;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.BaseRequestListener;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.producers.Producer;
import com.facebook.imagepipeline.producers.SettableProducerContext;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import java.util.concurrent.Executor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class FrescoControllerImplTest {

  private static final long IMAGE_ID = 123L;
  private static final String IMAGE_ID_STRING = "v" + IMAGE_ID;
  private static final String CALLER_CONTEXT = "abc";

  private static final int PLACEHOLDER_RES_ID = 234;
  private static final int ERROR_RES_ID = 345;

  private FrescoContext mFrescoContext;
  private FrescoExperiments mFrescoExperiments;
  private ImagePipeline mImagePipeline;
  private Hierarcher mHierarcher;
  private FrescoState mFrescoState;
  private FrescoDrawable mFrescoDrawable;
  private Resources mResources;
  private ImageOptions mImageOptions;
  private Drawable mOverlayDrawable;
  private Executor mLightweightBackgroundThreadExecutor;

  private FrescoControllerImpl mFrescoController;

  @Before
  public void setup() {
    mFrescoContext = mock(FrescoContext.class);
    mImagePipeline = mock(ImagePipeline.class);
    mHierarcher = mock(Hierarcher.class);
    mFrescoState = mock(FrescoState.class);
    mFrescoDrawable = mock(FrescoDrawable.class);
    mResources = mock(Resources.class);
    mImageOptions = mock(ImageOptions.class);
    mOverlayDrawable = mock(Drawable.class);
    mLightweightBackgroundThreadExecutor =
        new Executor() {
          @Override
          public void execute(Runnable command) {
            command.run();
          }
        };

    mFrescoExperiments = new FrescoExperiments();

    when(mFrescoContext.getHierarcher()).thenReturn(mHierarcher);
    when(mFrescoContext.getExperiments()).thenReturn(mFrescoExperiments);
    when(mFrescoContext.getImagePipeline()).thenReturn(mImagePipeline);
    when(mFrescoContext.getLightweightBackgroundThreadExecutor())
        .thenReturn(mLightweightBackgroundThreadExecutor);

    when(mFrescoState.getId()).thenReturn(IMAGE_ID);
    when(mFrescoState.getStringId()).thenReturn(IMAGE_ID_STRING);
    when(mFrescoState.getCallerContext()).thenReturn(CALLER_CONTEXT);
    when(mFrescoState.getFrescoDrawable()).thenReturn(mFrescoDrawable);
    when(mFrescoState.getResources()).thenReturn(mResources);
    when(mFrescoState.getImageOptions()).thenReturn(mImageOptions);
    when(mFrescoState.getOverlayDrawable()).thenReturn(mOverlayDrawable);
    when(mFrescoState.isAttached()).thenReturn(true);

    mFrescoController =
        new FrescoControllerImpl(mFrescoContext, new NoOpDebugOverlayFactory(), false, null);
  }

  @Test
  public void testOnAttach() {
    CacheKey cacheKey = mock(CacheKey.class);
    CloseableImage closeableImage = mock(CloseableImage.class);
    CloseableReference<CloseableImage> imageReference = CloseableReference.of(closeableImage);
    when(mFrescoState.getCacheKey()).thenReturn(cacheKey);
    when(mImagePipeline.getCachedImage(eq(cacheKey))).thenReturn(imageReference);
    mFrescoController.onAttach(mFrescoState, null);

    verify(mFrescoState).setAttached(true);
    verify(mHierarcher)
        .setupOverlayDrawable(
            eq(mFrescoDrawable), eq(mResources), eq(mImageOptions), eq(mOverlayDrawable));
    verify(mFrescoContext).getImagePipeline();
    verify(mImagePipeline).getCachedImage(eq(cacheKey));
    assertThat(imageReference.isValid()).isFalse();
  }

  @Test
  public void testOnAttach_whenCachedImageAvailable_thenDisplayAndCloseReference() {
    mFrescoController.onAttach(mFrescoState, null);

    verify(mFrescoState).setAttached(true);
    verify(mFrescoContext, atLeast(1)).getHierarcher();
    verify(mHierarcher)
        .setupOverlayDrawable(
            eq(mFrescoDrawable), eq(mResources), eq(mImageOptions), eq(mOverlayDrawable));
    verify(mFrescoContext, atLeast(1)).getExperiments();
    verify(mFrescoContext).getImagePipeline();
    verify(mFrescoState).onSubmit(eq(IMAGE_ID), eq(CALLER_CONTEXT));
    verifyNoMoreInteractions(mFrescoContext);
  }

  @Test
  public void testErrorHandling_whenErrorDrawableSet_thenDisplayErrorDrawable() {
    ImageOptions imageOptions = ImageOptions.create().errorRes(ERROR_RES_ID).build();
    Drawable errorDrawable = mock(Drawable.class);

    when(mFrescoState.getImageOptions()).thenReturn(imageOptions);
    when(mFrescoState.isAttached()).thenReturn(true);

    mFrescoController.displayErrorImage(mFrescoState, errorDrawable);

    verify(mFrescoDrawable).setProgressDrawable(null);
    verifyNoMoreInteractions(mFrescoContext);
    verify(mFrescoDrawable).setImageDrawable(eq(errorDrawable));
    verifyNoMoreInteractions(mFrescoDrawable);
  }

  @Test
  public void testErrorHandling_whenErrorAndPlaceholderDrawableSet_thenDisplayErrorDrawable() {
    ImageOptions imageOptions =
        ImageOptions.create().placeholderRes(PLACEHOLDER_RES_ID).errorRes(ERROR_RES_ID).build();
    Drawable placeholderDrawable = mock(Drawable.class);
    Drawable errorDrawable = mock(Drawable.class);

    when(mFrescoState.getImageOptions()).thenReturn(imageOptions);
    when(mFrescoState.isAttached()).thenReturn(true);
    when(mHierarcher.buildPlaceholderDrawable(eq(mResources), eq(imageOptions)))
        .thenReturn(placeholderDrawable);

    mFrescoController.displayErrorImage(mFrescoState, errorDrawable);

    verify(mFrescoDrawable).setProgressDrawable(null);
    verify(mFrescoDrawable).setImageDrawable(eq(errorDrawable));
    verifyNoMoreInteractions(mFrescoDrawable);
    verifyNoMoreInteractions(mFrescoContext);
  }

  @Test
  public void testErrorHandling_whenNoErrorDrawableSet_thenDoNotUpdateDrawable() {
    ImageOptions imageOptions = ImageOptions.create().placeholderRes(PLACEHOLDER_RES_ID).build();

    when(mFrescoState.getImageOptions()).thenReturn(imageOptions);
    when(mFrescoState.isAttached()).thenReturn(true);

    mFrescoController.displayErrorImage(mFrescoState, null);

    verify(mFrescoDrawable).setProgressDrawable(null);
    verifyNoMoreInteractions(mFrescoContext);
    verifyNoMoreInteractions(mFrescoDrawable);
  }

  @Test
  public void testPrepareImagePipelineComponents_whenValidRequest_thenPrepareComponents() {
    final String imageId = "ID 123";
    ImageRequest request = mock(ImageRequest.class);
    ProducerSequenceFactory producerSequenceFactory = mock(ProducerSequenceFactory.class);
    Producer<CloseableReference<CloseableImage>> producerSequence = mock(MockProducer.class);
    RequestListener requestListener = mock(RequestListener.class);
    ArgumentCaptor<SettableProducerContext> producerContextCaptor =
        ArgumentCaptor.forClass(SettableProducerContext.class);

    when(request.getLowestPermittedRequestLevel()).thenReturn(ImageRequest.RequestLevel.FULL_FETCH);
    when(mImagePipeline.generateUniqueFutureId()).thenReturn(imageId);
    when(mImagePipeline.getProducerSequenceFactory()).thenReturn(producerSequenceFactory);
    when(mImagePipeline.getRequestListenerForRequest(eq(request), nullable(RequestListener.class)))
        .thenReturn(requestListener);
    when(producerSequenceFactory.getDecodedImageProducerSequence(eq(request)))
        .thenReturn(producerSequence);

    mFrescoController.prepareImagePipelineComponents(mFrescoState, request, CALLER_CONTEXT);

    verify(mFrescoState).setProducerSequence(eq(producerSequence));
    verify(mFrescoState).setRequestListener(eq(requestListener));
    verify(mFrescoState).setSettableProducerContext(producerContextCaptor.capture());

    SettableProducerContext producerContext = producerContextCaptor.getValue();
    assertThat(producerContext).isNotNull();
    assertThat(producerContext.getImageRequest()).isEqualTo(request);
    assertThat(producerContext.getId()).isEqualTo(imageId);
  }

  @Test
  public void testPrepareImagePipelineComponents_whenInvalidRequest_thenDoNotPrepareComponents() {
    ImageRequest request = mock(ImageRequest.class);
    ProducerSequenceFactory producerSequenceFactory = mock(ProducerSequenceFactory.class);

    when(request.getLowestPermittedRequestLevel()).thenReturn(ImageRequest.RequestLevel.FULL_FETCH);
    when(mImagePipeline.getProducerSequenceFactory()).thenReturn(producerSequenceFactory);
    when(producerSequenceFactory.getDecodedImageProducerSequence(eq(request)))
        .thenThrow(new IllegalArgumentException());

    mFrescoController.prepareImagePipelineComponents(mFrescoState, request, CALLER_CONTEXT);

    verifyNoMoreInteractions(mFrescoState);
  }

  @Test
  public void testPrepareImagePipelineComponents_whenNullRequest_thenDoNotPrepareComponents() {
    ProducerSequenceFactory producerSequenceFactory = mock(ProducerSequenceFactory.class);

    when(mImagePipeline.getProducerSequenceFactory()).thenReturn(producerSequenceFactory);
    when(producerSequenceFactory.getDecodedImageProducerSequence(isNull(ImageRequest.class)))
        .thenThrow(new NullPointerException());

    mFrescoController.prepareImagePipelineComponents(mFrescoState, null, CALLER_CONTEXT);

    verifyNoMoreInteractions(mFrescoState);
  }

  @Test
  public void testListeners() {
    RequestListener requestListener = baseTestListeners();
    verify(mImagePipeline)
        .fetchDecodedImage(
            any(ImageRequest.class),
            any(Object.class),
            any(ImageRequest.RequestLevel.class),
            eq(requestListener),
            eq(IMAGE_ID_STRING));
  }

  @Test
  public void testListenersPrepPipelineComponents() {
    SettableProducerContext settableProducerContext = mock(SettableProducerContext.class);
    Producer producerSequence = mock(Producer.class);

    when(mFrescoState.getProducerSequence()).thenReturn(producerSequence);
    when(mFrescoState.getSettableProducerContext()).thenReturn(settableProducerContext);
    when(mFrescoContext.getExperiments())
        .thenReturn(
            new FrescoExperiments() {
              @Override
              public boolean prepareImagePipelineComponents() {
                return true;
              }
            });

    RequestListener requestListener = baseTestListeners();

    verify(mImagePipeline)
        .submitFetchRequest(eq(producerSequence), eq(settableProducerContext), eq(requestListener));
  }

  private RequestListener baseTestListeners() {
    ImageOptions imageOptions = ImageOptions.create().build();
    RequestListener requestListener = mock(BaseRequestListener.class);

    when(mFrescoContext.getUiThreadExecutorService())
        .thenReturn(UiThreadImmediateExecutorService.getInstance());

    final Uri uri = Uri.parse("http://fresco");
    ImageRequest imageRequest = ImageRequestBuilder.newBuilderWithSource(uri).build();
    when(mFrescoState.getImageRequest()).thenReturn(imageRequest);

    when(mFrescoState.getImageOptions()).thenReturn(imageOptions);
    when(mFrescoState.getRequestListener()).thenReturn(requestListener);

    when(mFrescoState.getUri()).thenReturn(uri);

    SimpleDataSource dataSource = SimpleDataSource.create();

    when(mImagePipeline.submitFetchRequest(
            any(Producer.class),
            any(com.facebook.imagepipeline.producers.SettableProducerContext.class),
            eq(requestListener)))
        .thenReturn(dataSource);
    when(mImagePipeline.fetchDecodedImage(
            any(ImageRequest.class),
            any(Object.class),
            any(ImageRequest.RequestLevel.class),
            eq(requestListener),
            eq(IMAGE_ID_STRING)))
        .thenReturn(dataSource);

    mFrescoController.onAttach(mFrescoState, null);

    verify(mFrescoContext, atLeast(1)).getImagePipeline();
    verify(mFrescoState, atLeast(1)).getCachedImage();
    verify(mFrescoState).onSubmit(eq(IMAGE_ID), eq(CALLER_CONTEXT));
    verify(mFrescoState, atLeast(1)).getImageRequest();
    return requestListener;
  }

  public interface MockProducer extends Producer<CloseableReference<CloseableImage>> {}
}
