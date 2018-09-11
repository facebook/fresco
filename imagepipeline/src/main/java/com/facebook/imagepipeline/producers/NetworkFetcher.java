/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import com.facebook.imagepipeline.image.EncodedImage;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import javax.annotation.Nullable;

/**
 * Interface that specifies network fetcher used by the image pipeline.
 *
 * <p>It is strongly recommended that implementations use an {@link Executor} in their
 * {@link #fetch} method to execute the network request on a different thread.
 *
 * <p> When the fetch from the network fails or is cancelled, the subclass is responsible for
 * calling {@link Callback} methods. If these are not called, the pipeline will not know that the
 * image fetch has failed and the application may not behave properly.
 *
 * @param <FETCH_STATE> The type to store fetch state. {@link FetchState} can be used or extended.
 */
public interface NetworkFetcher<FETCH_STATE extends FetchState> {

  /**
   * Callback used to inform the network fetch producer.
   */
  interface Callback {

    /**
     * Called upon a response from the network stack.
     *
     * @param response the InputStream for the data
     * @param responseLength the length of the data if known, -1 otherwise
     */
    void onResponse(InputStream response, int responseLength) throws IOException;

    /**
     * Called upon a failure in the network stack.
     *
     * @param throwable the cause of failure
     */
    void onFailure(Throwable throwable);

    /**
     * Called upon a cancellation of the request.
     */
    void onCancellation();
  }

  /**
   * Creates a new instance of the {@link FetchState}-derived object used to store state.
   *
   * @param consumer the consumer
   * @param producerContext the producer's context
   * @return a new fetch state instance
   */
  FETCH_STATE createFetchState(
      Consumer<EncodedImage> consumer,
      ProducerContext producerContext);

  /**
   * Initiates the network fetch and informs the producer when a response is received via the
   * provided callback.
   *
   * @param fetchState the fetch-specific state
   * @param callback the callback used to inform the network fetch producer
   */
  void fetch(FETCH_STATE fetchState, Callback callback);

  /**
   * Gets whether the intermediate results should be propagated.
   *
   * <p>In <i>addition</i> to the requirements of this method, intermediate results are throttled so
   * that a maximum of one every 100 ms is propagated. This is to conserve CPU and other resources.
   *
   * <p>Not applicable if progressive rendering is disabled or not supported for this image.
   *
   * @param fetchState the fetch-specific state
   * @return whether the intermediate results should be propagated
   */
  boolean shouldPropagate(FETCH_STATE fetchState);

  /**
   * Called after the fetch completes.
   *
   * <p> Implementing this method is optional and is useful for instrumentation purposes.
   *
   * @param fetchState the fetch-specific state
   * @param byteSize size of the data in bytes
   */
  void onFetchCompletion(FETCH_STATE fetchState, int byteSize);

  /**
   * Gets a map containing extra parameters to pass to the listeners.
   *
   * <p> Returning map is optional and is useful for instrumentation purposes.
   *
   * <p> This map won't be modified by the caller.
   *
   * @param fetchState the fetch-specific state
   * @param byteSize size of the data in bytes
   * @return a map with extra parameters
   */
  @Nullable
  Map<String, String> getExtraMap(FETCH_STATE fetchState, int byteSize);
}
