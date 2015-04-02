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
import javax.annotation.concurrent.ThreadSafe;

import java.util.List;

import com.facebook.common.internal.Lists;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * ProducerContext that allows the client to cancel an image request in-flight.
 */
@ThreadSafe
public class SettableProducerContext implements ProducerContext {

  private final ImageRequest mImageRequest;
  private final String mId;
  private final ProducerListener mProducerListener;
  private final Object mCallerContext;
  private final ImageRequest.RequestLevel mLowestPermittedRequestLevel;
  @GuardedBy("this")
  private final List<ProducerContextCallbacks> mCallbacks;
  @GuardedBy("this")
  private boolean mIsCancelled;
  @GuardedBy("this")
  private boolean mIsPrefetch;
  @GuardedBy("this")
  private Priority mPriority;
  @GuardedBy("this")
  private boolean mIsIntermediateResultExpected;

  public SettableProducerContext(
      ImageRequest imageRequest,
      String id,
      ProducerListener producerListener,
      Object callerContext,
      ImageRequest.RequestLevel lowestPermittedRequestLevel,
      boolean isPrefetch,
      boolean isIntermediateResultExpected,
      Priority priority) {
    mImageRequest = Preconditions.checkNotNull(imageRequest);
    mId = Preconditions.checkNotNull(id);
    mProducerListener = Preconditions.checkNotNull(producerListener);
    mCallerContext = callerContext;
    mLowestPermittedRequestLevel = Preconditions.checkNotNull(lowestPermittedRequestLevel);
    mIsPrefetch = isPrefetch;
    mIsIntermediateResultExpected = isIntermediateResultExpected;
    mPriority = priority;
    mIsCancelled = false;
    mCallbacks = Lists.newArrayList();
  }

  @Override
  public ImageRequest getImageRequest() {
    return mImageRequest;
  }

  @Override
  public String getId() {
    return mId;
  }

  @Override
  public ProducerListener getListener() {
    return mProducerListener;
  }

  @Override
  public Object getCallerContext() {
    return mCallerContext;
  }

  @Override
  public ImageRequest.RequestLevel getLowestPermittedRequestLevel() {
    return mLowestPermittedRequestLevel;
  }

  @Override
  public synchronized boolean isPrefetch() {
    return mIsPrefetch;
  }

  @Override
  public synchronized Priority getPriority() {
    return mPriority;
  }

  @Override
  public synchronized boolean isIntermediateResultExpected() {
    return mIsIntermediateResultExpected;
  }

  @Override
  public void addCallbacks(ProducerContextCallbacks callbacks) {
    boolean cancelImmediately = false;
    synchronized (this) {
      mCallbacks.add(callbacks);
      if (mIsCancelled) {
        cancelImmediately = true;
      }
    }

    if (cancelImmediately) {
      callbacks.onCancellationRequested();
    }
  }

  /**
   * Cancels the request processing.
   */
  public void cancel() {
    List<ProducerContextCallbacks> callbacks = null;
    synchronized (this) {
      if (!mIsCancelled) {
        mIsCancelled = true;
        callbacks = Lists.newArrayList(mCallbacks);
      }
    }

    if (callbacks != null) {
      for (ProducerContextCallbacks callback : callbacks) {
        callback.onCancellationRequested();
      }
    }
  }

  /**
   * Set whether the request is a prefetch request or not.
   * @param isPrefetch
   */
  public void setIsPrefetch(boolean isPrefetch) {
    List<ProducerContextCallbacks> callbacks = null;
    synchronized (this) {
      if (mIsPrefetch != isPrefetch) {
        mIsPrefetch = isPrefetch;
        callbacks = Lists.newArrayList(mCallbacks);
      }
    }

    if (callbacks != null) {
      for (ProducerContextCallbacks callback : callbacks) {
        callback.onIsPrefetchChanged();
      }
    }
  }

  /**
   * Set whether intermediate result is expected or not
   * @param isIntermediateResultExpected
   */
  public void setIsIntermediateResultExpected(boolean isIntermediateResultExpected) {
    List<ProducerContextCallbacks> callbacks = null;
    synchronized (this) {
      if (mIsIntermediateResultExpected != isIntermediateResultExpected) {
        mIsIntermediateResultExpected = isIntermediateResultExpected;
        callbacks = Lists.newArrayList(mCallbacks);
      }
    }

    if (callbacks != null) {
      for (ProducerContextCallbacks callback : callbacks) {
        callback.onIsIntermediateResultExpectedChanged();
      }
    }
  }

  /**
   * Set the priority of the request
   * @param priority
   */
  public void setPriority(Priority priority) {
    List<ProducerContextCallbacks> callbacks = null;
    synchronized (this) {
      if (mPriority != priority) {
        mPriority = priority;
        callbacks = Lists.newArrayList(mCallbacks);
      }
    }

    if (callbacks != null) {
      for (ProducerContextCallbacks callback : callbacks) {
        callback.onPriorityChanged();
      }
    }
  }

  @VisibleForTesting
  synchronized boolean isCancelled() {
    return mIsCancelled;
  }
}
