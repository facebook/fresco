/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.fresco.vito.core;

import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import androidx.core.util.ObjectsCompat;
import com.facebook.cache.common.CacheKey;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.references.CloseableReference;
import com.facebook.common.util.UriUtil;
import com.facebook.datasource.DataSource;
import com.facebook.drawee.backends.pipeline.info.ImageOrigin;
import com.facebook.drawee.components.DeferredReleaser;
import com.facebook.fresco.vito.listener.AutoPlayImageListener;
import com.facebook.fresco.vito.listener.ForwardingImageListener;
import com.facebook.fresco.vito.listener.ImageListener;
import com.facebook.fresco.vito.options.ImageOptions;
import com.facebook.imagepipeline.common.ResizeOptions;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.listener.RequestListener;
import com.facebook.imagepipeline.producers.SettableProducerContext;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.request.ImageRequestBuilder;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import javax.annotation.Nullable;

public class FrescoControllerImpl implements FrescoController {

  private final FrescoContext mFrescoContext;

  public FrescoControllerImpl(FrescoContext frescoContext) {
    mFrescoContext = frescoContext;
  }

  @Override
  public FrescoState onPrepare(
      final FrescoState frescoState,
      final Uri uri,
      final ImageOptions imageOptions,
      final Object callerContext,
      final Resources resources,
      final ImageListener imageListener) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("FrescoControllerImpl#onPrepare");
    }
    try {
      final boolean stateHasSameProps =
          ObjectsCompat.equals(uri, frescoState.getUri())
              && ObjectsCompat.equals(imageOptions, frescoState.getImageOptions())
              && (mFrescoContext.getExperiments().shouldDiffCallerContext()
                  ? ObjectsCompat.equals(callerContext, frescoState.getCallerContext())
                  : true);
      // If state is recycled from another component we must create a new one
      return stateHasSameProps
          ? frescoState
          : createState(uri, imageOptions, callerContext, resources, imageListener);
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  @Override
  public FrescoState createState(
      Uri uri,
      ImageOptions imageOptions,
      Object callerContext,
      Resources resources,
      ImageListener imageListener) {
    mFrescoContext.verifyCallerContext(callerContext);
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("FrescoControllerImpl#createState");
    }
    try {
      final FrescoExperiments frescoExperiments = mFrescoContext.getExperiments();

      final ImageRequest imageRequest = mFrescoContext.buildImageRequest(uri, imageOptions);
      final CacheKey cacheKey =
          mFrescoContext.getImagePipeline().getCacheKey(imageRequest, callerContext);
      CloseableReference<CloseableImage> cachedImage = null;
      boolean isImageCached;
      if (frescoExperiments.cacheImageInState()) {
        cachedImage = getCachedImage(cacheKey);
        isImageCached = cachedImage != null;
      } else {
        isImageCached = mFrescoContext.getImagePipeline().hasCachedImage(cacheKey);
      }
      FrescoState frescoState =
          new FrescoState(
              FrescoContext.generateIdentifier(),
              mFrescoContext,
              uri,
              imageOptions,
              callerContext,
              imageRequest,
              cacheKey,
              cachedImage,
              resources,
              new ForwardingImageListener(
                  mFrescoContext.getGlobalImageListener(),
                  ForwardingImageListener.create(
                      imageListener,
                      imageOptions.shouldAutoPlay() ? AutoPlayImageListener.getInstance() : null)));
      if (frescoExperiments.prepareActualImageWrapperInBackground()) {
        prepareActualImageInBackground(frescoState);
      }

      if (!isImageCached) {
        if (frescoExperiments.prepareImagePipelineComponents() && imageRequest != null) {
          prepareImagePipelineComponents(frescoState, imageRequest, callerContext);
        }

        if (frescoExperiments.prefetchInOnPrepare()) {
          DataSource<CloseableReference<CloseableImage>> datasource = fireOffRequest(frescoState);
          datasource.subscribe(frescoState, mFrescoContext.getUiThreadExecutorService());
          if (frescoExperiments.keepRefToPrefetchDatasouce()) {
            frescoState.setPrefetchDatasource(datasource);
          }
        }

        if (frescoExperiments.preparePlaceholderDrawableInBackground()) {
          final Drawable placeholderDrawable =
              mFrescoContext
                  .getHierarcher()
                  .buildPlaceholderDrawable(
                      frescoState.getResources(), frescoState.getImageOptions());
          frescoState.setPlaceholderDrawable(placeholderDrawable);
        }
      }
      return frescoState;
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  private void prepareActualImageInBackground(FrescoState frescoState) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("FrescoControllerImpl#prepareActualImageInBackground");
    }
    Hierarcher hierarcher = mFrescoContext.getHierarcher();
    frescoState.setActualImageWrapper(
        hierarcher.buildActualImageWrapper(frescoState.getImageOptions()));
    frescoState.setOverlayDrawable(
        hierarcher.buildOverlayDrawable(frescoState.getResources(), frescoState.getImageOptions()));
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
  }

  /** Creates imagepipeline components and sets them to frescoState */
  @VisibleForTesting
  void prepareImagePipelineComponents(
      FrescoState frescoState, ImageRequest imageRequest, Object callerContext) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("FrescoControllerImpl#prepareImagePipelineComponents");
    }
    try {
      frescoState.setProducerSequence(
          mFrescoContext
              .getImagePipeline()
              .getProducerSequenceFactory()
              .getDecodedImageProducerSequence(imageRequest));

      ImageRequest.RequestLevel lowestPermittedRequestLevel =
          ImageRequest.RequestLevel.getMax(
              imageRequest.getLowestPermittedRequestLevel(), ImageRequest.RequestLevel.FULL_FETCH);

      // TODO(T35949558): Add support for external request listeners
      RequestListener requestListener =
          mFrescoContext
              .getImagePipeline()
              .getRequestListenerForRequest(imageRequest, frescoState.getImageOriginListener());
      frescoState.setRequestListener(requestListener);

      frescoState.setSettableProducerContext(
          new SettableProducerContext(
              imageRequest,
              mFrescoContext.getImagePipeline().generateUniqueFutureId(),
              requestListener,
              callerContext,
              lowestPermittedRequestLevel,
              /* isPrefetch */ false,
              imageRequest.getProgressiveRenderingEnabled()
                  || !UriUtil.isNetworkUri(imageRequest.getSourceUri()),
              imageRequest.getPriority()));
    } catch (Exception exception) {
      // This is how ImagePipeline handles the error case.
      // Something went wrong and we can't prepare components ahead of time.
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  @Override
  public void onAttach(FrescoState frescoState) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("FrescoControllerImpl#onAttach");
    }
    try {
      frescoState.setAttached(true);

      final FrescoExperiments experiments = mFrescoContext.getExperiments();

      final ImageRequest imageRequest = frescoState.getImageRequest();
      if (frescoState.getImageOptions().shouldResizeToViewport()
          && frescoState.getTargetWidthPx() > 0
          && frescoState.getTargetHeightPx() > 0
          && imageRequest != null
          && imageRequest.getResizeOptions() == null) {
        frescoState.setImageRequest(
            ImageRequestBuilder.fromRequest(imageRequest)
                .setResizeOptions(
                    ResizeOptions.forDimensions(
                        frescoState.getTargetWidthPx(), frescoState.getTargetHeightPx()))
                .setResizingAllowedOverride(true)
                .build());
      }

      if (experiments.closeDatasource()) {
        DeferredReleaser.getInstance().cancelDeferredRelease(frescoState);
      }

      if (!frescoState.getFrescoDrawable().isDefaultLayerIsOn()) {
        frescoState.getFrescoDrawable().reset();
      }

      mFrescoContext
          .getHierarcher()
          .setupOverlayDrawable(
              mFrescoContext,
              frescoState.getFrescoDrawable(),
              frescoState.getResources(),
              frescoState.getImageOptions(),
              frescoState.getOverlayDrawable());

      frescoState.onSubmit(frescoState.getId(), frescoState.getCallerContext());

      // Check if we have a cached image in the state
      CloseableReference<CloseableImage> cachedImage = frescoState.getCachedImage();
      if (CloseableReference.isValid(cachedImage)) {
        frescoState.setImageOrigin(ImageOrigin.MEMORY_BITMAP);
        displayResultOrError(frescoState, cachedImage, true);
        return;
      }

      // Check if the image is in cache now
      if (experiments.checkCacheInAttach()) {
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.beginSection("FrescoControllerImpl#onAttach->getCachedImage");
        }
        try {
          cachedImage = getCachedImage(frescoState.getCacheKey());
        } finally {
          if (FrescoSystrace.isTracing()) {
            FrescoSystrace.endSection();
          }
        }
        if (CloseableReference.isValid(cachedImage)) {
          if (experiments.cacheImageInState()) {
            frescoState.setCachedImage(cachedImage);
          }
          frescoState.setImageOrigin(ImageOrigin.MEMORY_BITMAP);
          displayResultOrError(frescoState, cachedImage, true);
          return;
        }
      }

      // Not in cache, set placeholder and fetch image
      final Drawable placeholderDrawable;
      if (frescoState.getPlaceholderDrawable() != null) {
        placeholderDrawable = frescoState.getPlaceholderDrawable();
      } else {
        placeholderDrawable =
            mFrescoContext
                .getHierarcher()
                .buildPlaceholderDrawable(
                    frescoState.getResources(), frescoState.getImageOptions());
      }
      frescoState.getFrescoDrawable().setImageDrawable(placeholderDrawable);

      final Drawable progressDrawable =
          mFrescoContext
              .getHierarcher()
              .buildProgressDrawable(frescoState.getResources(), frescoState.getImageOptions());
      frescoState.getFrescoDrawable().setProgressDrawable(progressDrawable);

      if (frescoState.getImageRequest() != null) {
        if (FrescoSystrace.isTracing()) {
          FrescoSystrace.beginSection("FrescoControllerImpl#onAttach->fetch");
        }
        try {
          DataSource<CloseableReference<CloseableImage>> dataSource = fireOffRequest(frescoState);
          dataSource.subscribe(frescoState, mFrescoContext.getUiThreadExecutorService());
          if (experiments.keepRefToMainFetchDatasouce()) {
            frescoState.setMainFetchDatasource(dataSource);
          }
        } finally {
          if (FrescoSystrace.isTracing()) {
            FrescoSystrace.endSection();
          }
        }
      }
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  protected DataSource<CloseableReference<CloseableImage>> fireOffRequest(FrescoState frescoState) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("FrescoControllerImpl#fireOffRequest");
    }
    DataSource<CloseableReference<CloseableImage>> dataSource;
    if (mFrescoContext.getExperiments().prepareImagePipelineComponents()
        && frescoState.getProducerSequence() != null
        && frescoState.getSettableProducerContext() != null) {
      dataSource =
          mFrescoContext
              .getImagePipeline()
              .submitFetchRequest(
                  frescoState.getProducerSequence(),
                  frescoState.getSettableProducerContext(),
                  frescoState.getRequestListener());
    } else {
      dataSource =
          mFrescoContext
              .getImagePipeline()
              .fetchDecodedImage(
                  frescoState.getImageRequest(),
                  frescoState.getCallerContext(),
                  frescoState.getRequestListener());
    }
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
    return dataSource;
  }

  @Override
  public void onDetach(FrescoState frescoState) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("FrescoControllerImpl#onDetach");
    }
    frescoState.setAttached(false);

    if (mFrescoContext.getExperiments().closeDatasource()) {
      DeferredReleaser.getInstance().scheduleDeferredRelease(frescoState);
    }

    if (frescoState.getFrescoDrawable() != null) {
      frescoState.getFrescoDrawable().close();
      frescoState.setFrescoDrawable(null);
    }
    CloseableReference.closeSafely(frescoState.getCachedImage());

    frescoState.onRelease(frescoState.getId());
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.endSection();
    }
  }

  @Override
  public void onNewResult(
      FrescoState frescoState, DataSource<CloseableReference<CloseableImage>> dataSource) {
    if (dataSource != null && !dataSource.isClosed()) {
      final CloseableReference<CloseableImage> result = dataSource.getResult();
      try {
        frescoState.setImageFetched(true);
        if (frescoState.isAttached()) {
          displayResultOrError(frescoState, result, false);
        }
      } finally {
        CloseableReference.closeSafely(result);
      }
    }
  }

  @Override
  public void onFailure(
      FrescoState frescoState, DataSource<CloseableReference<CloseableImage>> dataSource) {
    Drawable errorDrawable =
        mFrescoContext
            .getHierarcher()
            .buildErrorDrawable(frescoState.getResources(), frescoState.getImageOptions());
    displayErrorImage(frescoState, errorDrawable);
    frescoState.onFailure(frescoState.getId(), errorDrawable, dataSource.getFailureCause());
  }

  @Override
  public void onCancellation(
      FrescoState frescoState, DataSource<CloseableReference<CloseableImage>> dataSource) {}

  @Override
  public void onProgressUpdate(
      FrescoState frescoState, DataSource<CloseableReference<CloseableImage>> dataSource) {}

  @Nullable
  private CloseableReference<CloseableImage> getCachedImage(@Nullable CacheKey cacheKey) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("FrescoControllerImpl#getCachedImage");
    }
    try {
      CloseableReference<CloseableImage> cachedImageReference =
          mFrescoContext.getImagePipeline().getCachedImage(cacheKey);
      if (CloseableReference.isValid(cachedImageReference)) {
        return cachedImageReference;
      }
      return null;
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  @VisibleForTesting
  void displayErrorImage(FrescoState frescoState, @Nullable Drawable errorDrawable) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("FrescoControllerImpl#displayErrorImage");
    }
    try {
      if (!frescoState.isAttached()) {
        return;
      }
      frescoState.getFrescoDrawable().setProgressDrawable(null);

      if (errorDrawable != null) {
        frescoState.getFrescoDrawable().setImageDrawable(errorDrawable);
      }
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  @VisibleForTesting
  void displayResultOrError(
      FrescoState frescoState,
      @Nullable CloseableReference<CloseableImage> result,
      boolean wasImmediate) {
    if (FrescoSystrace.isTracing()) {
      FrescoSystrace.beginSection("FrescoControllerImpl#displayResultOrError");
    }
    try {
      if (!frescoState.isAttached()) {
        return;
      }

      CloseableImage closeableImage;
      if (result == null || (closeableImage = result.get()) == null) {
        Drawable errorDrawable =
            mFrescoContext
                .getHierarcher()
                .buildErrorDrawable(frescoState.getResources(), frescoState.getImageOptions());
        displayErrorImage(frescoState, errorDrawable);
        frescoState.onFailure(frescoState.getId(), errorDrawable, null);
        return;
      }

      frescoState.getFrescoDrawable().setProgressDrawable(null);
      Drawable actualDrawable =
          mFrescoContext
              .getHierarcher()
              .setupActualImageDrawable(
                  mFrescoContext,
                  frescoState.getFrescoDrawable(),
                  frescoState.getResources(),
                  frescoState.getImageOptions(),
                  result,
                  frescoState.getActualImageWrapper(),
                  wasImmediate);

      frescoState.onFinalImageSet(
          frescoState.getId(), frescoState.getImageOrigin(), closeableImage, actualDrawable);
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }
}
