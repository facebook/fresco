/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.request.ImageRequest;

/**
 * Used to pass context information to producers.
 *
 * <p> Object implementing this interface is passed to all producers participating in pipeline
 * request {@see Producer#produceResults}. Its responsibility is to instruct producers which image
 * should be fetched/decoded/resized/cached etc. This class also handles request cancellation.
 *
 * <p>  In order to be notified when cancellation is requested, a producer should use the
 * {@code runOnCancellationRequested} method which takes an instance of Runnable and executes it
 * when the pipeline client cancels the image request.
 */
public interface ProducerContext {

  /**
   * @return image request that is being executed
   */
  public ImageRequest getImageRequest();

  /**
   * @return id of this request
   */
  public String getId();

  /**
   * @return ProducerListener for producer's events
   */
  public ProducerListener getListener();

  /**
   * @return the {@link Object} that indicates the caller's context
   */
  public Object getCallerContext();

  /**
   * @return the lowest permitted {@link ImageRequest.RequestLevel}
   */
  public ImageRequest.RequestLevel getLowestPermittedRequestLevel();

  /**
   * @return true if the request is a prefetch, false otherwise.
   */
  public boolean isPrefetch();

  /**
   * @return priority of the request.
   */
  public Priority getPriority();

  /**
   * @return true if request's owner expects intermediate results
   */
  public boolean isIntermediateResultExpected();

  /**
   * Adds callbacks to the set of callbacks that are executed at various points during the
   * processing of a request.
   * @param callbacks callbacks to be executed
   */
  public void addCallbacks(ProducerContextCallbacks callbacks);
}
