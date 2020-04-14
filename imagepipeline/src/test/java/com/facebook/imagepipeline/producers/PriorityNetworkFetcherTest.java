/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static com.facebook.imagepipeline.common.Priority.HIGH;
import static com.facebook.imagepipeline.common.Priority.LOW;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
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
        new PriorityNetworkFetcher<>(delegate, false, 4, 2);

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
    PriorityNetworkFetcher<FetchState> fetcher = new PriorityNetworkFetcher<>(delegate, true, 2, 1);

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
        new PriorityNetworkFetcher<>(delegate, false, 2, 1);

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
    PriorityNetworkFetcher<FetchState> fetcher = new PriorityNetworkFetcher<>(delegate, true, 2, 1);

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
        new PriorityNetworkFetcher<>(delegate, false, 2, 1);

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
        new PriorityNetworkFetcher<>(delegate, false, 2, 1);
    PriorityFetchState<FetchState> one = fetch(fetcher, "1", callback, true);
    ((SettableProducerContext) one.getContext()).cancel();
    verify(callback).onCancellation();
  }

  /** Make sure we tolerate when delegate.getExtraMap() returns 'null'. */
  @Test
  public void getExtraMapToleratesDelegateNullMap() {
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(delegate, false, 2, 1);

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
        new PriorityNetworkFetcher<>(delegate, false, 2, 1);

    PriorityFetchState<FetchState> one = fetch(fetcher, "1", callback, true);

    when(delegate.getExtraMap(eq(one.delegatedState), anyInt()))
        .thenReturn(Collections.singletonMap("foo", "bar"));

    assertThat(fetcher.getExtraMap(one, 123)).containsEntry("foo", "bar");
  }

  @Test
  public void queueTimeIsReturnedInExtraMap() {
    FakeClock clock = new FakeClock();

    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(delegate, false, 1, 0, clock);

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
  public void queueSizesAreReturnedInExtraMap() {
    FakeClock clock = new FakeClock();

    // Max hi-pri: 1, max low-pri: 0
    PriorityNetworkFetcher<FetchState> fetcher =
        new PriorityNetworkFetcher<>(delegate, false, 1, 0, clock);

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
}
