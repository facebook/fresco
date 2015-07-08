/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */
package com.facebook.imagepipeline.producers;

import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;

import java.util.ArrayList;
import java.util.List;

import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * ProducerContext that can be cancelled. Exposes low level API to manipulate state of the
 * ProducerContext.
 */
public class BaseProducerContext implements ProducerContext {
  private final ImageRequest mImageRequest;
  private final String mId;
  private final ProducerListener mProducerListener;
  private final Object mCallerContext;
  private final ImageRequest.RequestLevel mLowestPermittedRequestLevel;

  @GuardedBy("this")
  private boolean mIsPrefetch;
  @GuardedBy("this")
  private Priority mPriority;
  @GuardedBy("this")
  private boolean mIsIntermediateResultExpected;
  @GuardedBy("this")
  private boolean mIsCancelled;
  @GuardedBy("this")
  private final List<ProducerContextCallbacks> mCallbacks;

  public BaseProducerContext(
      ImageRequest imageRequest,
      String id,
      ProducerListener producerListener,
      Object callerContext,
      ImageRequest.RequestLevel lowestPermittedRequestLevel,
      boolean isPrefetch,
      boolean isIntermediateResultExpected,
      Priority priority) {
    mImageRequest = imageRequest;
    mId = id;
    mProducerListener = producerListener;
    mCallerContext = callerContext;
    mLowestPermittedRequestLevel = lowestPermittedRequestLevel;

    mIsPrefetch = isPrefetch;
    mPriority = priority;
    mIsIntermediateResultExpected = isIntermediateResultExpected;

    mIsCancelled = false;
    mCallbacks = new ArrayList<>();
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

  public synchronized boolean isCancelled() {
    return mIsCancelled;
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
   * Cancels the request processing and calls appropriate callbacks.
   */
  public void cancel() {
    BaseProducerContext.callOnCancellationRequested(cancelNoCallbacks());
  }

  /**
   * Changes isPrefetch property.
   *
   * <p> This method does not call any callbacks. Instead, caller of this method is responsible for
   * iterating over returned list and calling appropriate method on each callback object.
   * {@see #callOnIsPrefetchChanged}
   *
   * @return list of callbacks if the value actually changes, null otherwise
   */
  @Nullable
  public synchronized List<ProducerContextCallbacks> setIsPrefetchNoCallbacks(boolean isPrefetch) {
    if (isPrefetch == mIsPrefetch) {
      return null;
    }
    mIsPrefetch = isPrefetch;
    return new ArrayList<>(mCallbacks);
  }

  /**
   * Changes priority.
   *
   * <p> This method does not call any callbacks. Instead, caller of this method is responsible for
   * iterating over returned list and calling appropriate method on each callback object.
   * {@see #callOnPriorityChanged}
   *
   * @return list of callbacks if the value actually changes, null otherwise
   */
  @Nullable
  public synchronized List<ProducerContextCallbacks> setPriorityNoCallbacks(Priority priority) {
    if (priority == mPriority) {
      return null;
    }
    mPriority = priority;
    return new ArrayList<>(mCallbacks);
  }

  /**
   * Changes isIntermediateResultExpected property.
   *
   * <p> This method does not call any callbacks. Instead, caller of this method is responsible for
   * iterating over returned list and calling appropriate method on each callback object.
   * {@see #callOnIntermediateResultChanged}
   *
   * @return list of callbacks if the value actually changes, null otherwise
   */
  @Nullable
  public synchronized List<ProducerContextCallbacks> setIsIntermediateResultExpectedNoCallbacks(
      boolean isIntermediateResultExpected) {
    if (isIntermediateResultExpected == mIsIntermediateResultExpected) {
      return null;
    }
    mIsIntermediateResultExpected = isIntermediateResultExpected;
    return new ArrayList<>(mCallbacks);
  }

  /**
   * Marks this ProducerContext as cancelled.
   *
   * <p> This method does not call any callbacks. Instead, caller of this method is responsible for
   * iterating over returned list and calling appropriate method on each callback object.
   * {@see #callOnCancellationRequested}
   *
   * @return list of callbacks if the value actually changes, null otherwise
   */
  @Nullable
  public synchronized List<ProducerContextCallbacks> cancelNoCallbacks() {
    if (mIsCancelled) {
      return null;
    }
    mIsCancelled = true;
    return new ArrayList<>(mCallbacks);
  }

  /**
   * Calls {@code onCancellationRequested} on each element of the list. Does nothing if list == null
   */
  public static void callOnCancellationRequested(
      @Nullable List<ProducerContextCallbacks> callbacks) {
    if (callbacks == null) {
      return;
    }
    for (ProducerContextCallbacks callback : callbacks) {
      callback.onCancellationRequested();
    }
  }

  /**
   * Calls {@code onIsPrefetchChanged} on each element of the list. Does nothing if list == null
   */
  public static void callOnIsPrefetchChanged(
      @Nullable List<ProducerContextCallbacks> callbacks) {
    if (callbacks == null) {
      return;
    }
    for (ProducerContextCallbacks callback : callbacks) {
      callback.onIsPrefetchChanged();
    }
  }

  /**
   * Calls {@code onIsIntermediateResultExpected} on each element of the list. Does nothing if
   * list == null
   */
  public static void callOnIsIntermediateResultExpectedChanged(
      @Nullable List<ProducerContextCallbacks> callbacks) {
    if (callbacks == null) {
      return;
    }
    for (ProducerContextCallbacks callback : callbacks) {
      callback.onIsIntermediateResultExpectedChanged();
    }
  }

  /**
   * Calls {@code onPriorityChanged} on each element of the list. Does nothing if list == null
   */
  public static void callOnPriorityChanged(@Nullable List<ProducerContextCallbacks> callbacks) {
    if (callbacks == null) {
      return;
    }
    for (ProducerContextCallbacks callback : callbacks) {
      callback.onPriorityChanged();
    }
  }
}
