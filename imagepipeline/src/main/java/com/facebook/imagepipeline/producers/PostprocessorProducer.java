/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import javax.annotation.concurrent.GuardedBy;

import java.util.Map;
import java.util.concurrent.Executor;

import android.graphics.Bitmap;

import com.facebook.common.internal.ImmutableMap;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.decoder.CloseableImageCopier;
import com.facebook.imagepipeline.image.CloseableBitmap;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.imagepipeline.request.Postprocessor;
import com.facebook.imagepipeline.request.RepeatedPostprocessor;
import com.facebook.imagepipeline.request.RepeatedPostprocessorRunner;

/**
 * Runs a caller-supplied post-processor object.
 *
 * <p>Post-processors are only supported for static bitmaps. If the request is for an animated
 * image, the post-processor step will be skipped without warning.
 */
public class PostprocessorProducer implements Producer<CloseableReference<CloseableImage>> {

  @VisibleForTesting static final String NAME = "PostprocessorProducer";
  @VisibleForTesting static final String BITMAP_COPIED_EVENT = "bitmap_copied";
  @VisibleForTesting static final String POSTPROCESSOR = "Postprocessor";

  private final Producer<CloseableReference<CloseableImage>> mNextProducer;
  private final CloseableImageCopier mCloseableImageCopier;
  private final Executor mExecutor;

  public PostprocessorProducer(
      Producer<CloseableReference<CloseableImage>> nextProducer,
      CloseableImageCopier closeableImageCopier,
      Executor executor) {
    mNextProducer = Preconditions.checkNotNull(nextProducer);
    mCloseableImageCopier = Preconditions.checkNotNull(closeableImageCopier);
    mExecutor = Preconditions.checkNotNull(executor);
  }

  @Override
  public void produceResults(
      final Consumer<CloseableReference<CloseableImage>> consumer,
      ProducerContext context) {
    final ProducerListener listener = context.getListener();
    final Postprocessor postprocessor = context.getImageRequest().getPostprocessor();
    Consumer<CloseableReference<CloseableImage>> postprocessorConsumer;
    if (postprocessor instanceof RepeatedPostprocessor) {
      postprocessorConsumer =
          new RepeatedPostprocessorConsumer(
              consumer,
              listener,
              context.getId(),
              (RepeatedPostprocessor) postprocessor,
              context);
    } else {
      postprocessorConsumer =
          new SingleUsePostprocessorConsumer(consumer, listener, context.getId(), postprocessor);
    }
    mNextProducer.produceResults(postprocessorConsumer, context);
  }

  private abstract class AbstractPostprocessorConsumer
      extends BaseConsumer<CloseableReference<CloseableImage>> {
    protected final Consumer<CloseableReference<CloseableImage>> mConsumer;
    private final ProducerListener mListener;
    private final String mRequestId;
    private final Postprocessor mPostprocessor;

    private AbstractPostprocessorConsumer(
        Consumer<CloseableReference<CloseableImage>> consumer,
        ProducerListener listener,
        String requestId,
        Postprocessor postprocessor) {
      mConsumer = consumer;
      mListener = listener;
      mRequestId = requestId;
      mPostprocessor = postprocessor;
    }

    @Override
    protected void onFailureImpl(Throwable t) {
      mConsumer.onFailure(t);
    }

    @Override
    protected void onCancellationImpl() {
      mConsumer.onCancellation();
    }

    protected void copyAndPostprocessBitmap(
        CloseableReference<CloseableImage> sourceImageRef,
        boolean isLast) {
      mListener.onProducerStart(mRequestId, NAME);
      CloseableReference<CloseableImage> destRef = null;
      try {
        try {
          destRef = mCloseableImageCopier.copyCloseableImage(sourceImageRef);
          mListener.onProducerEvent(mRequestId, NAME, BITMAP_COPIED_EVENT);
          postprocessBitmap(destRef, mPostprocessor);
        } catch (Throwable t) {
          mListener.onProducerFinishWithFailure(
              mRequestId, NAME, t, getExtraMap(mListener, mRequestId, mPostprocessor));
          mConsumer.onFailure(t);
          return;
        }
        mListener.onProducerFinishWithSuccess(
            mRequestId, NAME, getExtraMap(mListener, mRequestId, mPostprocessor));
        mConsumer.onNewResult(destRef, isLast);
      } finally {
        CloseableReference.closeSafely(destRef);
      }
    }

    private void postprocessBitmap(
        CloseableReference<CloseableImage> destinationCloseableImageRef,
        Postprocessor postprocessor) {
      Bitmap destinationBitmap =
          ((CloseableBitmap) destinationCloseableImageRef.get()).getUnderlyingBitmap();
      postprocessor.process(destinationBitmap);
    }

    private Map<String, String> getExtraMap(
        ProducerListener listener,
        String requestId,
        Postprocessor postprocessor) {
      if (!listener.requiresExtraMap(requestId)) {
        return null;
      }
      return ImmutableMap.of(POSTPROCESSOR, postprocessor.getName());
    }
  }

  private class SingleUsePostprocessorConsumer extends AbstractPostprocessorConsumer {
    private SingleUsePostprocessorConsumer(
        Consumer<CloseableReference<CloseableImage>> consumer,
        ProducerListener listener,
        String requestId,
        Postprocessor postprocessor) {
      super(consumer, listener, requestId, postprocessor);
    }

    @Override
    protected void onNewResultImpl(
        final CloseableReference<CloseableImage> newResult, final boolean isLast) {
      if (!isLast) {
        return;
      }
      if (!mCloseableImageCopier.isCloseableImageCopyable(newResult)) {
        mConsumer.onNewResult(newResult, true);
        return;
      }

      final CloseableReference<CloseableImage> clonedResult = newResult.clone();
      mExecutor.execute(
          new Runnable() {
            @Override
            public void run() {
              try {
                copyAndPostprocessBitmap(clonedResult, isLast);
              } finally {
                CloseableReference.closeSafely(clonedResult);
              }
            }
          });
    }
  }

  private class RepeatedPostprocessorConsumer
      extends AbstractPostprocessorConsumer implements RepeatedPostprocessorRunner {
    @GuardedBy("this")
    private CloseableReference<CloseableImage> mOriginalImageRef;
    @GuardedBy("this")
    private boolean mIsDirty;
    @GuardedBy("this")
    private boolean mIsPostProcessingRunning;

    private RepeatedPostprocessorConsumer(
        Consumer<CloseableReference<CloseableImage>> consumer,
        ProducerListener listener,
        String requestId,
        RepeatedPostprocessor repeatedPostprocessor,
        ProducerContext context) {
      super(consumer, listener, requestId, repeatedPostprocessor);
      repeatedPostprocessor.setCallback(RepeatedPostprocessorConsumer.this);
      context.addCallbacks(
          new BaseProducerContextCallbacks() {
            @Override
            public void onCancellationRequested() {
              closeOriginalImage();
              mConsumer.onCancellation();
            }
          });
      mIsDirty = false;
      mIsPostProcessingRunning = false;
    }

    @Override
    protected void onNewResultImpl(
        CloseableReference<CloseableImage> newResult, boolean isLast) {
      if (!isLast) {
        return;
      }
      if (!mCloseableImageCopier.isCloseableImageCopyable(newResult)) {
        mConsumer.onNewResult(newResult, true);
        return;
      }

      synchronized (RepeatedPostprocessorConsumer.this) {
        mOriginalImageRef = newResult.clone();
      }
      maybeExecuteCopyAndPostprocessBitmap();
    }

    @Override
    public synchronized void update() {
      maybeExecuteCopyAndPostprocessBitmap();
    }

    private void maybeExecuteCopyAndPostprocessBitmap() {
      boolean shouldExecutePostProcessing = false;
      synchronized (RepeatedPostprocessorConsumer.this) {
        mIsDirty = true;
        if (!mIsPostProcessingRunning && CloseableReference.isValid(mOriginalImageRef)) {
          mIsPostProcessingRunning = true;
          shouldExecutePostProcessing = true;
        }
      }
      if (shouldExecutePostProcessing) {
        executeCopyAndPostprocessBitmap();
      }
    }

    private void executeCopyAndPostprocessBitmap() {
      mExecutor.execute(
          new Runnable() {
            @Override
            public void run() {
              CloseableReference<CloseableImage> closeableImageRef = null;
              synchronized (RepeatedPostprocessorConsumer.this) {
                mIsDirty = false;
                if (CloseableReference.isValid(mOriginalImageRef)) {
                  closeableImageRef = mOriginalImageRef.clone();
                }
              }
              if (closeableImageRef != null) {
                try {
                  copyAndPostprocessBitmap(closeableImageRef, /* isLast */false);
                } finally {
                  CloseableReference.closeSafely(closeableImageRef);
                }
              }
              boolean shouldExecutePostprocessing;
              synchronized (RepeatedPostprocessorConsumer.this) {
                shouldExecutePostprocessing = mIsDirty;
                mIsPostProcessingRunning = mIsDirty;
              }
              if (shouldExecutePostprocessing) {
                executeCopyAndPostprocessBitmap();
              }
            }
          });
    }

    private synchronized void closeOriginalImage() {
      CloseableReference.closeSafely(mOriginalImageRef);
      mOriginalImageRef = null;
    }
  }
}
