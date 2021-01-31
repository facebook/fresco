/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static com.facebook.imagepipeline.common.Priority.HIGH;
import static com.facebook.imagepipeline.common.Priority.LOW;
import static com.facebook.imagepipeline.producers.PriorityNetworkFetcher.INFINITE_REQUEUE;
import static com.facebook.imagepipeline.producers.PriorityNetworkFetcher.NO_DELAYED_REQUESTS;
import static com.google.common.collect.Iterables.getOnlyElement;
import static com.google.common.truth.Truth.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.facebook.common.logging.FLog;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.producers.PriorityNetworkFetcher.PriorityFetchState;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.imagepipeline.testing.FakeClock;
import com.google.common.collect.ArrayListMultimap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class PriorityNetworkFetcherTest {

  private static int defaultMinimumLoggingLevel;

  @BeforeClass
  public static void beforeClass() {
    defaultMinimumLoggingLevel = FLog.getMinimumLoggingLevel();
    FLog.setMinimumLoggingLevel(FLog.VERBOSE);
  }

  @AfterClass
  public static void afterClass() {
    FLog.setMinimumLoggingLevel(defaultMinimumLoggingLevel);
  }

  private final NetworkFetcher<FetchState> delegate = mock(NetworkFetcher.class);
  private final NetworkFetcher.Callback callback = mock(NetworkFetcher.Callback.class);

  /**
   * Scenario: two hi-pri images and one low-pro image are enqueued in turn. We make some assertions
   * about the state of the queue (see inline comments).
   */
  @Test
  public void sanityScenario() {
    // Hi-pri requests are LIFO, Max hi-pri: 4, max low-pri: 2
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            delegate, false, 4, 2, true, 0, false, NO_DELAYED_REQUESTS, 0, false);

    // Enqueue hi-pri image; since there are less than 4 concurrent downloads, it's dequeued
    // immediately.
    PriorityFetchState<FetchState> one = fetch(fetcher, "1", callback, true);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();
    assertThat(fetcher.getCurrentlyFetching()).containsExactly(one);
    verify(delegate).fetch(eq(one.delegatedState), any(NetworkFetcher.Callback.class));

    // Enqueue another hi-pri image
    PriorityFetchState<FetchState> two = fetch(fetcher, "2", callback, true);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();
    assertThat(fetcher.getCurrentlyFetching()).containsExactly(one, two);
    verify(delegate).fetch(eq(two.delegatedState), any(NetworkFetcher.Callback.class));

    // Enqueue an low-pri image. Since there are already 2 outstanding requests, this one
    // will not be dequeued.
    PriorityFetchState<FetchState> three = fetch(fetcher, "3", callback, false);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(toTestEntry(fetcher.getLowPriQueue())).containsExactlyElementsIn(toTestEntry(three));
    assertThat(fetcher.getCurrentlyFetching()).containsExactly(one, two);
    verify(delegate, never()).fetch(eq(three.delegatedState), any(NetworkFetcher.Callback.class));

    // Now, 'one' completes downloading. We expect it to be removed entirely, and 'three' to be sent
    // to the fetcher.
    fetcher.onFetchCompletion(one, 4317);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();
    assertThat(fetcher.getCurrentlyFetching()).containsExactly(two, three);
    verify(delegate).fetch(eq(three.delegatedState), any(NetworkFetcher.Callback.class));

    // Now, 'two' and 'three' complete; we expect the queue to become empty.
    fetcher.onFetchCompletion(two, 4317);
    fetcher.onFetchCompletion(three, 4317);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();
    assertThat(fetcher.getCurrentlyFetching()).isEmpty();
  }

  /**
   * Assert that when hi-pri requests are FIFO when PriorityNetworkFetcher is configured this way.
   */
  @Test
  public void hipriIsFifo() {
    // Hi-pri is FIFO, Max hi-pri: 2, max low-pri: 1
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            delegate, true, 2, 1, true, 0, false, NO_DELAYED_REQUESTS, 0, false);

    // Fill the currently-fetching set, so additional requests are not sent to network.
    PriorityFetchState<FetchState> dontcare1 = fetch(fetcher, "dontcare1", callback, true);
    fetch(fetcher, "dontcare2", callback, true);

    PriorityFetchState<FetchState> one = fetch(fetcher, "1", callback, true);
    PriorityFetchState<FetchState> two = fetch(fetcher, "2", callback, true);
    PriorityFetchState<FetchState> three = fetch(fetcher, "3", callback, true);

    // Assert that the insertion order is LIFO for hi-pri, FIFO for low-pri.
    assertThat(toTestEntry(fetcher.getHiPriQueue()))
        .containsExactlyElementsIn(toTestEntry(one, two, three))
        .inOrder();
    assertThat(fetcher.getLowPriQueue()).isEmpty();

    // Now, 'dontcare1' completes downloading. We expect 'one' to be sent to the fetcher.
    fetcher.onFetchCompletion(dontcare1, 4317);
    verify(delegate).fetch(eq(one.delegatedState), any(NetworkFetcher.Callback.class));
  }

  /**
   * Assert that when hi-pri requests are LIFO when PriorityNetworkFetcher is configured this way.
   */
  @Test
  public void hipriIsLifo() {
    // Hi-pri is LIFO, Max hi-pri: 2, max low-pri: 1
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            delegate, false, 2, 1, true, 0, false, NO_DELAYED_REQUESTS, 0, false);

    // Fill the currently-fetching set, so additional requests are not sent to network.
    PriorityFetchState<FetchState> dontcare1 = fetch(fetcher, "dontcare1", callback, true);
    fetch(fetcher, "dontcare2", callback, true);

    PriorityFetchState<FetchState> one = fetch(fetcher, "1", callback, true);
    PriorityFetchState<FetchState> two = fetch(fetcher, "2", callback, true);
    PriorityFetchState<FetchState> three = fetch(fetcher, "3", callback, true);

    // Assert that the insertion order is LIFO for hi-pri, FIFO for low-pri.
    assertThat(toTestEntry(fetcher.getHiPriQueue()))
        .containsExactlyElementsIn(toTestEntry(three, two, one))
        .inOrder();
    assertThat(fetcher.getLowPriQueue()).isEmpty();

    // Now, 'dontcare1' completes downloading. We expect 'three' to be sent to the fetcher.
    fetcher.onFetchCompletion(dontcare1, 4317);
    verify(delegate).fetch(eq(three.delegatedState), any(NetworkFetcher.Callback.class));
  }

  /** Assert that low-pri requests are FIFO. */
  @Test
  public void lowpriIsFifo() {
    // Hi-pri is FIFO, Max hi-pri: 2, max low-pri: 1
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            delegate, true, 2, 1, true, 0, false, NO_DELAYED_REQUESTS, 0, false);

    // Fill the currently-fetching set, so additional requests are not sent to network.
    PriorityFetchState<FetchState> dontcare1 = fetch(fetcher, "dontcare1", callback, true);
    PriorityFetchState<FetchState> dontcare2 = fetch(fetcher, "dontcare2", callback, true);

    PriorityFetchState<FetchState> one = fetch(fetcher, "1", callback, false);
    PriorityFetchState<FetchState> two = fetch(fetcher, "2", callback, false);
    PriorityFetchState<FetchState> three = fetch(fetcher, "3", callback, false);

    // Assert that the insertion order is LIFO for hi-pri, FIFO for low-pri.

    assertThat(toTestEntry(fetcher.getLowPriQueue()))
        .containsExactlyElementsIn(toTestEntry(one, two, three))
        .inOrder();
    assertThat(fetcher.getHiPriQueue()).isEmpty();

    // Now, 'dontcare1' and 'dontcare2' complete downloading, freeing up spots for low-pri requests.
    // We expect 'one' to be sent to the fetcher.
    fetcher.onFetchCompletion(dontcare1, 4317);
    fetcher.onFetchCompletion(dontcare2, 4317);
    verify(delegate).fetch(eq(one.delegatedState), any(NetworkFetcher.Callback.class));
  }

  /**
   * Scenarios:
   *
   * <p>1. The priority of an hi-pri image is changed to low-pri. We expect it to be removed from
   * the hi-pri queue and added to the low-pri queue.
   *
   * <p>2. The priority of an low-pri image is changed to hi-pri. We expect it to be moved back to
   * the hi-pri queue.
   *
   * <p>3. Changing a priority of an image to its existing priority doesn't change its place in the
   * fetcher
   */
  @Test
  public void changePriority() {
    // Hi-pri is LIFO, Max hi-pri: 2, max low-pri: 1
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            delegate, false, 2, 1, true, 0, false, NO_DELAYED_REQUESTS, 0, false);

    // Fill the currently-fetching set, so additional requests are not sent to network.
    fetch(fetcher, "dontcare1", callback, true);
    fetch(fetcher, "dontcare2", callback, true);

    // Add 3 requests; they all should remain in the fetcher
    PriorityFetchState<FetchState> one = fetch(fetcher, "1", callback, true);
    PriorityFetchState<FetchState> two = fetch(fetcher, "2", callback, true);
    PriorityFetchState<FetchState> three = fetch(fetcher, "3", callback, true);

    // Change priority of 'two' to low-pri; expect to find it at the end of the low-pri queue.
    ((SettableProducerContext) two.getContext()).setPriority(LOW);
    assertThat(toTestEntry(fetcher.getHiPriQueue()))
        .containsExactlyElementsIn(toTestEntry(three, one))
        .inOrder();
    assertThat(toTestEntry(fetcher.getLowPriQueue()))
        .containsExactlyElementsIn(toTestEntry(two))
        .inOrder();

    // Change priority of 'two' to hi-pri; expect to find it at the beginning of the hi-pri queue.
    ((SettableProducerContext) two.getContext()).setPriority(HIGH);
    assertThat(toTestEntry(fetcher.getHiPriQueue()))
        .containsExactlyElementsIn(toTestEntry(two, three, one))
        .inOrder();
    assertThat(toTestEntry(fetcher.getLowPriQueue())).isEmpty();

    // Change the priority of 'three' to hi-pri; expect it to remain in the middle of the hi-pri
    // queue.
    ((SettableProducerContext) three.getContext()).setPriority(HIGH);
    assertThat(toTestEntry(fetcher.getHiPriQueue()))
        .containsExactlyElementsIn(toTestEntry(two, three, one))
        .inOrder();
    assertThat(toTestEntry(fetcher.getLowPriQueue())).isEmpty();
  }

  /**
   * Assert that when the producer tells us the request is cancelled, we pass this on to the
   * callback.
   *
   * <p>I didn't see this documented in the API, but other fetchers (e.g.,
   * HttpUrlConnectionNetworkFetcher) do that, and without it, I observed some images get stuck -
   * Fresco doesn't know they're cancelled, and keeps asking the PriorityNetworkFetcher to fetch
   * them.
   */
  @Test
  public void contextCancellationIsCallCancellation() {
    // Hi-pri is LIFO, Max hi-pri: 2, max low-pri: 1
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            delegate, false, 2, 1, true, 0, false, NO_DELAYED_REQUESTS, 0, false);
    PriorityFetchState<FetchState> one = fetch(fetcher, "1", callback, true);
    cancel(one);
    verify(callback).onCancellation();
  }

  /**
   * Scenario: two low-pri requests are queued, so only one starts running. We cancel it, which gets
   * the second request running. We then cancel it as well, so nothing is running.
   */
  @Test
  public void testCancellations() {
    // Hi-pri is LIFO, Max hi-pri: 2, max low-pri: 1
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            delegate, false, 2, 1, true, 0, false, NO_DELAYED_REQUESTS, 0, false);

    PriorityFetchState<FetchState> one = fetch(fetcher, "1", callback, false);
    PriorityFetchState<FetchState> two = fetch(fetcher, "2", callback, false);

    // 'one' was requested from the delegate, 'two' is waiting for a free slot.
    assertThat(fetcher.getCurrentlyFetching()).containsExactly(one);

    // Cancel 'one'.
    cancel(one);

    // 'one' was cancelled, so 'two' is starting to fetch.
    assertThat(fetcher.getCurrentlyFetching()).containsExactly(two);

    // Cancel 'two'.
    cancel(two);

    // Everything was cancelled, nothing is being fetched.
    assertThat(fetcher.getCurrentlyFetching()).isEmpty();
  }

  /**
   * Scenario: a queue that allows at most 1 low-pri requests to execute concurrently. The queue has
   * 'inflightFetchesCanBeCancelled' set to false, meaning that it ignores cancellations to requests
   * that have already begun.
   *
   * <p>Two low-pri requests are queued, so only 'one' starts running. We try to cancel it, but
   * since it's already been started, nothing is cancelled. The second request continues to be
   * queued. However, cancelling the second request does in fact cancel it, because it didn't start
   * yet.
   */
  @Test
  public void testCancellations_nonInflight() {
    // Hi-pri is LIFO, Max hi-pri: 2, max low-pri: 1
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            delegate, false, 2, 1, false, 0, false, NO_DELAYED_REQUESTS, 0, false);

    PriorityFetchState<FetchState> one = fetch(fetcher, "1", callback, false);
    PriorityFetchState<FetchState> two = fetch(fetcher, "2", callback, false);

    // 'one' was requested from the delegate, 'two' is waiting for a free slot.
    assertThat(fetcher.getCurrentlyFetching()).containsExactly(one);
    assertThat(fetcher.getLowPriQueue()).contains(two);

    // Cancel 'one' - nothing happens, because it's already in-flight.
    cancel(one);
    assertThat(fetcher.getLowPriQueue()).contains(two);
    assertThat(fetcher.getCurrentlyFetching()).containsExactly(one);

    verify(callback, never()).onCancellation();

    // Cancel 'two'.
    cancel(two);
    assertThat(fetcher.getLowPriQueue()).isEmpty();
    assertThat(fetcher.getCurrentlyFetching()).containsExactly(one);

    verify(callback).onCancellation();
  }

  /**
   * Scenario: a queue that allows at most 1 low-pri requests to execute concurrently. The queue has
   * 'dontCancelRequests' set to true, meaning that it ignores cancellations to all requests (low
   * and high priority).
   *
   * <p>Two low-pri requests are queued, so only 'one' starts running, the second request continues
   * to be queued. We try to cancel both requests, we should not succeed canceling them.
   */
  @Test
  public void testCancellations_none() {
    // Hi-pri is LIFO, Max hi-pri: 2, max low-pri: 1
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            delegate, false, 2, 1, false, 0, true, NO_DELAYED_REQUESTS, 0, false);

    PriorityFetchState<FetchState> one = fetch(fetcher, "1", callback, false);
    PriorityFetchState<FetchState> two = fetch(fetcher, "2", callback, false);

    // 'one' was requested from the delegate, 'two' is waiting for a free slot.
    assertThat(fetcher.getCurrentlyFetching()).containsExactly(one);
    assertThat(fetcher.getLowPriQueue()).contains(two);

    // Cancel 'one' - nothing happens, because we don't allow canceling requests.
    cancel(one);
    assertThat(fetcher.getLowPriQueue()).contains(two);
    assertThat(fetcher.getCurrentlyFetching()).containsExactly(one);

    verify(callback, never()).onCancellation();

    // Cancel 'two' - nothing happens, because we don't allow canceling requests.
    cancel(two);
    assertThat(fetcher.getLowPriQueue()).contains(two);
    assertThat(fetcher.getCurrentlyFetching()).containsExactly(one);

    verify(callback, never()).onCancellation();
  }

  /** Make sure we tolerate when delegate.getExtraMap() returns 'null'. */
  @Test
  public void getExtraMapToleratesDelegateNullMap() {
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            delegate, false, 2, 1, true, 0, false, NO_DELAYED_REQUESTS, 0, false);

    PriorityFetchState<FetchState> one = fetch(fetcher, "1", callback, true);

    // Explicitly return a null.
    when(delegate.getExtraMap(eq(one.delegatedState), anyInt())).thenReturn(null);

    fetcher.getExtraMap(one, 123);
    // Implicitly assert we don't crash.
  }

  /**
   * Scenario: the delegate fetcher returns a map in getExtraMap(), assert that we return it from
   * PriorityNetworkFetcher's getExtraMap().
   */
  @Test
  public void getExtraMapIsDelegated() {
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            delegate, false, 2, 1, true, 0, false, NO_DELAYED_REQUESTS, 0, false);

    PriorityFetchState<FetchState> one = fetch(fetcher, "1", callback, true);

    when(delegate.getExtraMap(eq(one.delegatedState), anyInt()))
        .thenReturn(Collections.singletonMap("foo", "bar"));

    assertThat(fetcher.getExtraMap(one, 123)).containsEntry("foo", "bar");
  }

  @Test
  public void queueTimeIsReturnedInExtraMap() {
    FakeClock clock = new FakeClock();

    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            delegate, false, 1, 0, true, 0, false, NO_DELAYED_REQUESTS, 0, false, clock);

    // The queue is empty, so enqueuing a request immediately executes it. Therefore, the queue time
    // is 0.
    PriorityFetchState<FetchState> one = fetch(fetcher, "1", callback, true);
    assertThat(fetcher.getExtraMap(one, 123)).containsEntry("pri_queue_time", "0");

    // Enqueueing another fetch. The queue is now full (it allows at most 1 hi-pri concurrent
    // requests), so the request waits.
    PriorityFetchState<FetchState> two = fetch(fetcher, "1", callback, true);

    // 'one' completes, which causes 'two' to be dequeued 43ms after it was enqueued.
    clock.incrementBy(43);
    fetcher.onFetchCompletion(one, 123);

    // Complete 'two' and request its extras map.
    fetcher.onFetchCompletion(two, 123);
    assertThat(fetcher.getExtraMap(two, 123)).containsEntry("pri_queue_time", "43");
  }

  @Test
  public void changePriorityIsReturnedInExtraMap() {
    // all request should wait in the priority queues
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            delegate, false, 1, 0, true, 0, false, NO_DELAYED_REQUESTS, 0, false);

    // fill the currently fetching queue so all the next requests will wait in the priority queues.
    PriorityFetchState<FetchState> dontcare1 = fetch(fetcher, "dontcare1", callback, true);

    // enqueue a low-pri request in the queue
    PriorityFetchState<FetchState> one = fetch(fetcher, "1", callback, false);
    assertThat(fetcher.getExtraMap(one, 123))
        .containsEntry("request_initial_priority_is_high", "false");
    assertThat(fetcher.getExtraMap(one, 123)).containsEntry("priority_changed_count", "0");

    // enqueue a hi-pri request in the queue
    PriorityFetchState<FetchState> two = fetch(fetcher, "2", callback, true);
    assertThat(fetcher.getExtraMap(two, 123))
        .containsEntry("request_initial_priority_is_high", "true");
    assertThat(fetcher.getExtraMap(two, 123)).containsEntry("priority_changed_count", "0");

    // change priority from low to low (nothing should be changed)
    ((SettableProducerContext) one.getContext()).setPriority(LOW);
    assertThat(fetcher.getExtraMap(one, 123))
        .containsEntry("request_initial_priority_is_high", "false");
    assertThat(fetcher.getExtraMap(one, 123)).containsEntry("priority_changed_count", "0");

    // change priority from low to high
    ((SettableProducerContext) one.getContext()).setPriority(HIGH);
    assertThat(fetcher.getExtraMap(one, 123))
        .containsEntry("request_initial_priority_is_high", "false");
    assertThat(fetcher.getExtraMap(one, 123)).containsEntry("priority_changed_count", "1");

    // change priority from high to low (second time)
    ((SettableProducerContext) one.getContext()).setPriority(LOW);
    assertThat(fetcher.getExtraMap(one, 123))
        .containsEntry("request_initial_priority_is_high", "false");
    assertThat(fetcher.getExtraMap(one, 123)).containsEntry("priority_changed_count", "2");
  }

  @Test
  public void numberOfCurrentlyFetchingIsReturnedInExtraMap() {
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            delegate, false, 2, 1, true, 0, false, NO_DELAYED_REQUESTS, 0, false);

    // The queue is empty, so enqueuing a request immediately executes it. Therefore,
    // currentlyFetching size is 0.
    PriorityFetchState<FetchState> one = fetch(fetcher, "1", callback, false);
    assertThat(fetcher.getExtraMap(one, 123)).containsEntry("currently_fetching_size", "0");

    // enqueuing a request immediately executes it. Therefore, currentlyFetching size is 1.
    PriorityFetchState<FetchState> two = fetch(fetcher, "2", callback, true);
    assertThat(fetcher.getExtraMap(two, 123)).containsEntry("currently_fetching_size", "1");

    // CurrentlyFetching queue is full. Therefore, new request will wait in the hi-pri queue.
    PriorityFetchState<FetchState> three = fetch(fetcher, "3", callback, true);
    assertThat(fetcher.getExtraMap(three, 123)).containsEntry("currently_fetching_size", "2");

    // one of the fetching request is completed, a new one should be replace it (three).
    fetcher.onFetchCompletion(one, 4317);
    assertThat(fetcher.getExtraMap(three, 123)).containsEntry("currently_fetching_size", "2");
  }

  @Test
  public void queueSizesAreReturnedInExtraMap() {
    FakeClock clock = new FakeClock();

    // Max hi-pri: 1, max low-pri: 0
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            delegate, false, 1, 0, true, 0, false, NO_DELAYED_REQUESTS, 0, false, clock);

    PriorityFetchState<FetchState> hipri1 = fetch(fetcher, "hipri1", callback, true);
    PriorityFetchState<FetchState> hipri2 = fetch(fetcher, "hipri2", callback, true);
    PriorityFetchState<FetchState> hipri3 = fetch(fetcher, "hipri3", callback, true);
    PriorityFetchState<FetchState> lowpri1 = fetch(fetcher, "lowpri1", callback, false);
    PriorityFetchState<FetchState> lowpri2 = fetch(fetcher, "lowpri2", callback, false);

    // When hipri1 is created, there hasn't been other requests yet.
    Map<String, String> hipri1Extras = fetcher.getExtraMap(hipri1, 123);
    assertThat(hipri1Extras).containsEntry("hipri_queue_size", "0");
    assertThat(hipri1Extras).containsEntry("lowpri_queue_size", "0");

    // When hipri2 is created, only hipri1 has previously been created, and it was immediately
    // dequeued, so the queue size is 0.
    Map<String, String> hipri2Extras = fetcher.getExtraMap(hipri2, 123);
    assertThat(hipri2Extras).containsEntry("hipri_queue_size", "0");
    assertThat(hipri2Extras).containsEntry("lowpri_queue_size", "0");

    // When hipri3 is created, hipri2 is in the queue.
    Map<String, String> hipri3Extras = fetcher.getExtraMap(hipri3, 123);
    assertThat(hipri3Extras).containsEntry("hipri_queue_size", "1");
    assertThat(hipri3Extras).containsEntry("lowpri_queue_size", "0");

    // When lowpri1 is created, hipri2 and hipri3 are in the queue.
    Map<String, String> lowpri1Extras = fetcher.getExtraMap(lowpri1, 123);
    assertThat(lowpri1Extras).containsEntry("hipri_queue_size", "2");
    assertThat(lowpri1Extras).containsEntry("lowpri_queue_size", "0");

    // When lowpri2 is created, hipri2 and hipri3 are in the hipri queue, and lowpri1 is in the
    // low-pri queue.
    Map<String, String> lowpri2Extras = fetcher.getExtraMap(lowpri2, 123);
    assertThat(lowpri2Extras).containsEntry("hipri_queue_size", "2");
    assertThat(lowpri2Extras).containsEntry("lowpri_queue_size", "1");
  }

  /**
   * Scenario: an image fetch fails. We expect it to be re-queued, and since it is hi-pri, to be
   * retried immediately.
   */
  @Test
  public void testInfiniteRequeues_requeueOnFail() {
    RecordingNetworkFetcher recordingNetworkFetcher = new RecordingNetworkFetcher();

    // Max hi-pri: 1, max low-pri: 0
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            recordingNetworkFetcher,
            false,
            1,
            0,
            true,
            INFINITE_REQUEUE,
            false,
            NO_DELAYED_REQUESTS,
            0,
            false);

    PriorityFetchState<FetchState> hipri1 = fetch(fetcher, "hipri1", callback, true);
    PriorityFetchState<FetchState> hipri2 = fetch(fetcher, "hipri2", callback, true);

    assertThat(fetcher.getCurrentlyFetching()).containsExactly(hipri1);
    assertThat(fetcher.getHiPriQueue()).containsExactly(hipri2);
    assertThat(fetcher.getLowPriQueue()).isEmpty();

    // Simulate a failure in hipri1.
    getOnlyElement(recordingNetworkFetcher.callbacks.get(hipri1.delegatedState))
        .onFailure(new Exception());

    assertThat(fetcher.getCurrentlyFetching()).containsExactly(hipri1);
    assertThat(fetcher.getHiPriQueue()).containsExactly(hipri2);
    assertThat(fetcher.getLowPriQueue()).isEmpty();

    assertThat(hipri1.requeueCount).isEqualTo(1);
  }

  /** Scenario: an image fetch fails with a non-recoverable exception. Don't requeue it. */
  @Test
  public void testInfiniteRequeues_dontRequeueNonrecoverableException() {
    RecordingNetworkFetcher recordingNetworkFetcher = new RecordingNetworkFetcher();

    // Max hi-pri: 1, max low-pri: 0
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            recordingNetworkFetcher,
            false,
            1,
            0,
            true,
            INFINITE_REQUEUE,
            false,
            NO_DELAYED_REQUESTS,
            0,
            false);

    PriorityFetchState<FetchState> hipri1 = fetch(fetcher, "hipri1", callback, true);

    assertThat(fetcher.getCurrentlyFetching()).containsExactly(hipri1);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();

    // Simulate a failure in hipri1.
    getOnlyElement(recordingNetworkFetcher.callbacks.get(hipri1.delegatedState))
        .onFailure(new PriorityNetworkFetcher.NonrecoverableException("HTTP 403"));

    assertThat(fetcher.getCurrentlyFetching()).isEmpty();
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();

    assertThat(hipri1.requeueCount).isEqualTo(0);
  }

  /**
   * Scenario: an image changes priority and then fails. We expect it to be re-queued in the new
   * priority queue.
   */
  @Test
  public void testInfiniteRequeues_changePriThenFail() {
    RecordingNetworkFetcher recordingNetworkFetcher = new RecordingNetworkFetcher();

    // Max hi-pri: 1, max low-pri: 0
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            recordingNetworkFetcher,
            false,
            1,
            0,
            true,
            INFINITE_REQUEUE,
            false,
            NO_DELAYED_REQUESTS,
            0,
            false);

    PriorityFetchState<FetchState> hipri1 = fetch(fetcher, "hipri1", callback, true);
    PriorityFetchState<FetchState> hipri2 = fetch(fetcher, "hipri2", callback, true);

    assertThat(fetcher.getCurrentlyFetching()).containsExactly(hipri1);
    assertThat(fetcher.getHiPriQueue()).containsExactly(hipri2);
    assertThat(fetcher.getLowPriQueue()).isEmpty();

    ((SettableProducerContext) hipri1.getContext()).setPriority(LOW);

    // Simulate a failure in hipri1.
    getOnlyElement(recordingNetworkFetcher.callbacks.get(hipri1.delegatedState))
        .onFailure(new Exception());

    assertThat(fetcher.getCurrentlyFetching()).containsExactly(hipri2);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).containsExactly(hipri1);

    assertThat(hipri1.requeueCount).isEqualTo(1);
  }

  /**
   * Scenario: Priority Network Fetcher is paused, the priority queues will continue to hold many
   * requests. On resumption, make sure we dequeue in parallel, as many requests as we can (depends
   * on the number of free slots in the currentlyFetching queue).
   */
  @Test
  public void testMultipleDequeueOnResumption() {
    RecordingNetworkFetcher recordingNetworkFetcher = new RecordingNetworkFetcher();

    // Max hi-pri: 3, max low-pri: 0
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            recordingNetworkFetcher,
            false,
            3,
            0,
            true,
            INFINITE_REQUEUE,
            false,
            NO_DELAYED_REQUESTS,
            0,
            true);

    // pause the priority network fetcher -> no dequeue
    fetcher.pause();

    PriorityFetchState<FetchState> hipri1 = fetch(fetcher, "hipri1", callback, true);
    PriorityFetchState<FetchState> hipri2 = fetch(fetcher, "hipri2", callback, true);
    PriorityFetchState<FetchState> hipri3 = fetch(fetcher, "hipri3", callback, true);

    HashSet<PriorityFetchState<FetchState>> hiPriRequests = new HashSet<>();
    hiPriRequests.add(hipri1);
    hiPriRequests.add(hipri2);
    hiPriRequests.add(hipri3);
    assertThat(fetcher.getCurrentlyFetching()).isEmpty();
    assertThat(fetcher.getHiPriQueue()).containsExactlyElementsIn(hiPriRequests);
    assertThat(fetcher.getLowPriQueue()).isEmpty();
    assertThat(fetcher.getDelayedQeueue()).isEmpty();

    // resume the priority network fetcher -> dequeue all requests waiting in the hiPi queue
    fetcher.resume();
    assertThat(fetcher.getCurrentlyFetching()).containsExactlyElementsIn(hiPriRequests);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();
    assertThat(fetcher.getDelayedQeueue()).isEmpty();
  }

  /**
   * When we requeue a request, we recreate its delegate FetchState. This is required to reset any
   * state it might have (e.g., maximum number of retries).
   */
  @Test
  public void delegateFetchStateIsRecreatedOnRequeue() {
    RecordingNetworkFetcher recordingNetworkFetcher = new RecordingNetworkFetcher();

    // Max hi-pri: 1, max low-pri: 0
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            recordingNetworkFetcher,
            false,
            1,
            0,
            true,
            INFINITE_REQUEUE,
            false,
            NO_DELAYED_REQUESTS,
            0,
            false);

    PriorityFetchState<FetchState> fetchState = fetch(fetcher, "url", callback, true);

    assertThat(recordingNetworkFetcher.createdFetchStates).hasSize(1);
    assertThat(fetchState.delegatedState)
        .isSameInstanceAs(recordingNetworkFetcher.createdFetchStates.get(0));

    // Simulate a failure in fetchState, triggering a requeue.
    getOnlyElement(recordingNetworkFetcher.callbacks.get(fetchState.delegatedState))
        .onFailure(new Exception());

    assertThat(recordingNetworkFetcher.createdFetchStates).hasSize(2);
    assertThat(fetchState.delegatedState)
        .isSameInstanceAs(recordingNetworkFetcher.createdFetchStates.get(1));

    Map<String, String> extrasMap = fetcher.getExtraMap(fetchState, 123);
    assertThat(extrasMap).containsEntry("requeueCount", "1");
  }

  /**
   * Scenario: an image fetch fails. We expect it to be re-queued up to maxNumberOfRequeue times.
   */
  @Test
  public void testMaxNumberOfRequeue_requeueOnFail() {
    RecordingNetworkFetcher recordingNetworkFetcher = new RecordingNetworkFetcher();
    final int maxNumberOfRequeue = 2;

    // Max hi-pri: 1, max low-pri: 0
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            recordingNetworkFetcher,
            false,
            1,
            0,
            true,
            maxNumberOfRequeue,
            false,
            NO_DELAYED_REQUESTS,
            0,
            false);

    PriorityFetchState<FetchState> hipri1 = fetch(fetcher, "hipri1", callback, true);
    PriorityFetchState<FetchState> hipri2 = fetch(fetcher, "hipri2", callback, true);

    assertThat(fetcher.getCurrentlyFetching()).containsExactly(hipri1);
    assertThat(fetcher.getHiPriQueue()).containsExactly(hipri2);
    assertThat(fetcher.getLowPriQueue()).isEmpty();

    // Simulate 2 failures in hipri1, the request should be requeued.
    getOnlyElement(recordingNetworkFetcher.callbacks.get(hipri1.delegatedState))
        .onFailure(new Exception());
    getOnlyElement(recordingNetworkFetcher.callbacks.get(hipri1.delegatedState))
        .onFailure(new Exception());

    assertThat(fetcher.getCurrentlyFetching()).containsExactly(hipri1);
    assertThat(fetcher.getHiPriQueue()).containsExactly(hipri2);
    assertThat(fetcher.getLowPriQueue()).isEmpty();

    // Simulate a 3rd failure in hipri1, the request should NOT be requeued.
    getOnlyElement(recordingNetworkFetcher.callbacks.get(hipri1.delegatedState))
        .onFailure(new Exception());

    assertThat(hipri1.requeueCount).isEqualTo(2);

    // we will start fetching hipri2 immediately.
    assertThat(fetcher.getCurrentlyFetching()).containsExactly(hipri2);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();
  }

  /**
   * Scenario: an image fetch fails. We expect it to be re-queued up to maxNumberOfRequeue times.
   * The reuqest should be re-queued immediately up to immediateRequeueCount and then it will wait
   * in the delayedQueue to be re-queued again after delayTimeInMillis.
   */
  @Test
  public void testMovingDelayedRequeue_requeueOnFail() {
    RecordingNetworkFetcher recordingNetworkFetcher = new RecordingNetworkFetcher();
    final int maxNumberOfRequeue = 3;
    final int immediateRequeueCount = 1;
    final int delayTimeInMillis = 300;
    FakeClock clock = new FakeClock();

    // Max hi-pri: 1, max low-pri: 0
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            recordingNetworkFetcher,
            false,
            1,
            0,
            true,
            maxNumberOfRequeue,
            false,
            immediateRequeueCount,
            delayTimeInMillis,
            false,
            clock);

    PriorityFetchState<FetchState> hipri1 = fetch(fetcher, "hipri1", callback, true);

    assertThat(fetcher.getCurrentlyFetching()).containsExactly(hipri1);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();

    // Simulate 1st failure in hipri1, the request should be requeued immediately.
    getOnlyElement(recordingNetworkFetcher.callbacks.get(hipri1.delegatedState))
        .onFailure(new Exception());

    assertThat(fetcher.getCurrentlyFetching()).containsExactly(hipri1);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();
    assertThat(fetcher.getDelayedQeueue()).isEmpty();
    assertThat(hipri1.requeueCount).isEqualTo(1);
    assertThat(fetcher.getExtraMap(hipri1, 123)).containsEntry("delay_count", "0");

    // Simulate 2nd failure in hipri1, the request should wait in the delayedQueue for
    // delayTimeInMillis.
    getOnlyElement(recordingNetworkFetcher.callbacks.get(hipri1.delegatedState))
        .onFailure(new Exception());
    assertThat(fetcher.getCurrentlyFetching()).isEmpty();
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();
    assertThat(fetcher.getDelayedQeueue()).containsExactly(hipri1);
    assertThat(fetcher.getExtraMap(hipri1, 123)).containsEntry("delay_count", "1");

    clock.incrementBy(301);
    // to trigger inflight requests
    PriorityFetchState<FetchState> hipri2 = fetch(fetcher, "hipri2", callback, true);

    // 301 ms is bigger than delayTimeInMillis, so hipri1 request should be re-queued now.
    assertThat(fetcher.getCurrentlyFetching()).containsExactly(hipri1);
    assertThat(fetcher.getHiPriQueue()).containsExactly(hipri2);
    assertThat(fetcher.getLowPriQueue()).isEmpty();
    assertThat(fetcher.getDelayedQeueue()).isEmpty();
    assertThat(hipri1.requeueCount).isEqualTo(2);
  }

  /**
   * Scenario: an image fetch fails. We expect it to be re-queued up to maxNumberOfRequeue times.
   * The reuqest should be re-queued immediately up to immediateRequeueCount and then it will wait
   * in the delayedQueue to be re-queued again after delayTimeInMillis. In this test we make sure
   * the request is not moved back to the priority queue unless delayTimeInMillis has passed.
   */
  @Test
  public void testNotMovingDelayedRequeue_requeueOnFail() {
    RecordingNetworkFetcher recordingNetworkFetcher = new RecordingNetworkFetcher();
    final int maxNumberOfRequeue = 3;
    final int immediateRequeueCount = 1;
    final int delayTimeInMillis = 300;
    FakeClock clock = new FakeClock();

    // Max hi-pri: 1, max low-pri: 0
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            recordingNetworkFetcher,
            false,
            1,
            0,
            true,
            maxNumberOfRequeue,
            false,
            immediateRequeueCount,
            delayTimeInMillis,
            false,
            clock);

    PriorityFetchState<FetchState> hipri1 = fetch(fetcher, "hipri1", callback, true);

    assertThat(fetcher.getCurrentlyFetching()).containsExactly(hipri1);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();

    // Simulate 1st failure in hipri1, the request should be requeued immediately.
    getOnlyElement(recordingNetworkFetcher.callbacks.get(hipri1.delegatedState))
        .onFailure(new Exception());

    assertThat(fetcher.getCurrentlyFetching()).containsExactly(hipri1);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();
    assertThat(fetcher.getDelayedQeueue()).isEmpty();
    assertThat(hipri1.requeueCount).isEqualTo(1);
    assertThat(fetcher.getExtraMap(hipri1, 123)).containsEntry("delay_count", "0");

    // Simulate 2nd failure in hipri1, the request should wait in the delayedQueue for
    // delayTimeInMillis.
    getOnlyElement(recordingNetworkFetcher.callbacks.get(hipri1.delegatedState))
        .onFailure(new Exception());
    assertThat(fetcher.getCurrentlyFetching()).isEmpty();
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();
    assertThat(fetcher.getDelayedQeueue()).containsExactly(hipri1);
    assertThat(fetcher.getExtraMap(hipri1, 123)).containsEntry("delay_count", "1");

    clock.incrementBy(200);
    // to trigger inflight requests
    PriorityFetchState<FetchState> hipri2 = fetch(fetcher, "hipri2", callback, true);

    // 200 ms is smaller than delayTimeInMillis, so hipri1 request should still wait in the
    // delayedQueue.
    assertThat(fetcher.getCurrentlyFetching()).containsExactly(hipri2);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();
    assertThat(fetcher.getDelayedQeueue()).containsExactly(hipri1);
    assertThat(hipri1.requeueCount).isEqualTo(2);
  }

  /**
   * Scenario: a priority fetcher is paused before fetch() is called. We expect that no request is
   * dequeued until we call resume().
   */
  @Test
  public void pauseBeforeFetch() {
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            delegate, false, 2, 1, true, 0, false, NO_DELAYED_REQUESTS, 0, false);

    fetcher.pause();

    PriorityFetchState<FetchState> one = fetch(fetcher, "1", callback, true);

    assertThat(fetcher.getCurrentlyFetching()).isEmpty();
    assertThat(fetcher.getHiPriQueue()).containsExactly(one);
    assertThat(fetcher.getLowPriQueue()).isEmpty();

    fetcher.resume();

    assertThat(fetcher.getCurrentlyFetching()).containsExactly(one);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();
  }

  /**
   * Scenario: two requests are enqueued, and the first one starts executing immediately. pause() is
   * called, and then the first request completes. Normally, the second request would be dequeued
   * immediately, but since we're paused, it isn't. Then, when resume() is called, the second
   * request is dequeued.
   */
  @Test
  public void pauseDuringFetch() {
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(
            delegate, false, 1, 0, true, 0, false, NO_DELAYED_REQUESTS, 0, false);

    PriorityFetchState<FetchState> one = fetch(fetcher, "1", callback, true);
    PriorityFetchState<FetchState> two = fetch(fetcher, "2", callback, true);

    fetcher.pause();

    assertThat(fetcher.getCurrentlyFetching()).containsExactly(one);
    assertThat(fetcher.getHiPriQueue()).containsExactly(two);
    assertThat(fetcher.getLowPriQueue()).isEmpty();

    fetcher.onFetchCompletion(one, 123);

    assertThat(fetcher.getCurrentlyFetching()).isEmpty();
    assertThat(fetcher.getHiPriQueue()).containsExactly(two);
    assertThat(fetcher.getLowPriQueue()).isEmpty();

    fetcher.resume();

    assertThat(fetcher.getCurrentlyFetching()).containsExactly(two);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();
  }

  private PriorityFetchState<FetchState> fetch(
      PriorityNetworkFetcher<FetchState> fetcher,
      String uri,
      NetworkFetcher.Callback callback,
      boolean isHiPri) {
    Consumer<EncodedImage> consumer = mock(Consumer.class);
    SettableProducerContext context =
        new SettableProducerContext(
            ImageRequest.fromUri(uri),
            "dontcare",
            null,
            null,
            null,
            !isHiPri,
            false,
            isHiPri ? HIGH : LOW,
            null);
    FetchState delegateFetchState = new FetchState(consumer, context);
    when(delegate.createFetchState(eq(consumer), eq(context))).thenReturn(delegateFetchState);
    PriorityFetchState<FetchState> fetchState = fetcher.createFetchState(consumer, context);
    fetcher.fetch(fetchState, callback);
    return fetchState;
  }

  @SafeVarargs
  private static List<TestFetchState> toTestEntry(PriorityFetchState<FetchState>... fetchStates) {
    return toTestEntry(Arrays.asList(fetchStates));
  }

  private static List<TestFetchState> toTestEntry(
      List<PriorityFetchState<FetchState>> fetchStates) {
    ArrayList<TestFetchState> result = new ArrayList<>();
    for (PriorityFetchState<FetchState> fetchState : fetchStates) {
      result.add(new TestFetchState(fetchState));
    }
    return result;
  }

  private void cancel(PriorityFetchState<FetchState> fetchState) {
    ((SettableProducerContext) fetchState.getContext()).cancel();
  }

  /**
   * TestFetchState is wrapped around PriorityFetchState<> to provide it with equals() and
   * toString(), so it's easier to write tests and make assertions.
   */
  private static class TestFetchState {

    private final PriorityFetchState<FetchState> fetchState;

    public TestFetchState(PriorityFetchState<FetchState> fetchState) {
      this.fetchState = fetchState;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      return fetchState.equals(((TestFetchState) o).fetchState);
    }

    @Override
    public int hashCode() {
      return fetchState.hashCode();
    }

    @Override
    public String toString() {
      return String.format(
          "[%s %s %s %s]",
          fetchState.delegatedState,
          fetchState.callback,
          fetchState.enqueuedTimestamp,
          fetchState.dequeuedTimestamp);
    }
  }

  private static class RecordingNetworkFetcher implements NetworkFetcher<FetchState> {

    private final ArrayList<FetchState> createdFetchStates = new ArrayList<>();
    private final ArrayListMultimap<FetchState, Callback> callbacks = ArrayListMultimap.create();

    @Override
    public FetchState createFetchState(
        Consumer<EncodedImage> consumer, ProducerContext producerContext) {
      FetchState fetchState = new FetchState(consumer, producerContext);
      createdFetchStates.add(fetchState);
      return fetchState;
    }

    @Override
    public void fetch(FetchState fetchState, Callback callback) {
      callbacks.put(fetchState, callback);
    }

    @Override
    public boolean shouldPropagate(FetchState fetchState) {
      return false;
    }

    @Override
    public void onFetchCompletion(FetchState fetchState, int byteSize) {}

    @Nullable
    @Override
    public Map<String, String> getExtraMap(FetchState fetchState, int byteSize) {
      return null;
    }
  }
}
