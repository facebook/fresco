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
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.facebook.common.logging.FLog;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.producers.PriorityNetworkFetcher.Entry;
import com.facebook.imagepipeline.request.ImageRequest;
import com.google.common.base.Objects;
import java.util.ArrayList;
import java.util.List;
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
    FetchState one = fetch(fetcher, "1", callback, true);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();
    assertThat(fetcher.getCurrentlyFetching()).containsExactly(one);
    verify(delegate).fetch(eq(one), any(NetworkFetcher.Callback.class));

    // Enqueue another hi-pri image
    FetchState two = fetch(fetcher, "2", callback, true);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();
    assertThat(fetcher.getCurrentlyFetching()).containsExactly(one, two);
    verify(delegate).fetch(eq(two), any(NetworkFetcher.Callback.class));

    // Enqueue an low-pri image. Since there are already 2 outstanding requests, this one
    // will not be dequeued.
    FetchState three = fetch(fetcher, "3", callback, false);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(toTestEntry(fetcher.getLowPriQueue()))
        .containsExactly(new TestEntry(three, callback));
    assertThat(fetcher.getCurrentlyFetching()).containsExactly(one, two);
    verify(delegate, never()).fetch(eq(three), any(NetworkFetcher.Callback.class));

    // Now, 'one' completes downloading. We expect it to be removed entirely, and 'three' to be sent
    // to the fetcher.
    fetcher.onFetchCompletion(one, 4317);
    assertThat(fetcher.getHiPriQueue()).isEmpty();
    assertThat(fetcher.getLowPriQueue()).isEmpty();
    assertThat(fetcher.getCurrentlyFetching()).containsExactly(two, three);
    verify(delegate).fetch(eq(three), any(NetworkFetcher.Callback.class));

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
    FetchState dontcare1 = fetch(fetcher, "dontcare1", callback, true);
    fetch(fetcher, "dontcare2", callback, true);

    FetchState one = fetch(fetcher, "1", callback, true);
    FetchState two = fetch(fetcher, "2", callback, true);
    FetchState three = fetch(fetcher, "3", callback, true);

    // Assert that the insertion order is LIFO for hi-pri, FIFO for low-pri.
    assertThat(toTestEntry(fetcher.getHiPriQueue()))
        .containsExactly(
            new TestEntry(one, callback),
            new TestEntry(two, callback),
            new TestEntry(three, callback))
        .inOrder();
    assertThat(fetcher.getLowPriQueue()).isEmpty();

    // Now, 'dontcare1' completes downloading. We expect 'one' to be sent to the fetcher.
    fetcher.onFetchCompletion(dontcare1, 4317);
    verify(delegate).fetch(eq(one), any(NetworkFetcher.Callback.class));
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
    FetchState dontcare1 = fetch(fetcher, "dontcare1", callback, true);
    fetch(fetcher, "dontcare2", callback, true);

    FetchState one = fetch(fetcher, "1", callback, true);
    FetchState two = fetch(fetcher, "2", callback, true);
    FetchState three = fetch(fetcher, "3", callback, true);

    // Assert that the insertion order is LIFO for hi-pri, FIFO for low-pri.
    assertThat(toTestEntry(fetcher.getHiPriQueue()))
        .containsExactly(
            new TestEntry(three, callback),
            new TestEntry(two, callback),
            new TestEntry(one, callback))
        .inOrder();
    assertThat(fetcher.getLowPriQueue()).isEmpty();

    // Now, 'dontcare1' completes downloading. We expect 'three' to be sent to the fetcher.
    fetcher.onFetchCompletion(dontcare1, 4317);
    verify(delegate).fetch(eq(three), any(NetworkFetcher.Callback.class));
  }

  /** Assert that low-pri requests are FIFO. */
  @Test
  public void lowpriIsFifo() {
    // Hi-pri is FIFO, Max hi-pri: 2, max low-pri: 1
    PriorityNetworkFetcher<FetchState> fetcher = new PriorityNetworkFetcher<>(delegate, true, 2, 1);

    // Fill the currently-fetching set, so additional requests are not sent to network.
    FetchState dontcare1 = fetch(fetcher, "dontcare1", callback, true);
    FetchState dontcare2 = fetch(fetcher, "dontcare2", callback, true);

    FetchState one = fetch(fetcher, "1", callback, false);
    FetchState two = fetch(fetcher, "2", callback, false);
    FetchState three = fetch(fetcher, "3", callback, false);

    // Assert that the insertion order is LIFO for hi-pri, FIFO for low-pri.

    assertThat(toTestEntry(fetcher.getLowPriQueue()))
        .containsExactly(
            new TestEntry(one, callback),
            new TestEntry(two, callback),
            new TestEntry(three, callback))
        .inOrder();
    assertThat(fetcher.getHiPriQueue()).isEmpty();

    // Now, 'dontcare1' and 'dontcare2' complete downloading, freeing up spots for low-pri requests.
    // We expect 'one' to be sent to the fetcher.
    fetcher.onFetchCompletion(dontcare1, 4317);
    fetcher.onFetchCompletion(dontcare2, 4317);
    verify(delegate).fetch(eq(one), any(NetworkFetcher.Callback.class));
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
    FetchState one = fetch(fetcher, "1", callback, true);
    FetchState two = fetch(fetcher, "2", callback, true);
    FetchState three = fetch(fetcher, "3", callback, true);

    // Change priority of 'two' to low-pri; expect to find it at the end of the low-pri queue.
    ((SettableProducerContext) two.getContext()).setPriority(LOW);
    assertThat(toTestEntry(fetcher.getHiPriQueue()))
        .containsExactly(new TestEntry(three, callback), new TestEntry(one, callback))
        .inOrder();
    assertThat(toTestEntry(fetcher.getLowPriQueue()))
        .containsExactly(new TestEntry(two, callback))
        .inOrder();

    // Change priority of 'two' to hi-pri; expect to find it at the beginning of the hi-pri queue.
    ((SettableProducerContext) two.getContext()).setPriority(HIGH);
    assertThat(toTestEntry(fetcher.getHiPriQueue()))
        .containsExactly(
            new TestEntry(two, callback),
            new TestEntry(three, callback),
            new TestEntry(one, callback))
        .inOrder();
    assertThat(toTestEntry(fetcher.getLowPriQueue())).isEmpty();

    // Change the priority of 'three' to hi-pri; expect it to remain in the middle of the hi-pri
    // queue.
    ((SettableProducerContext) three.getContext()).setPriority(HIGH);
    assertThat(toTestEntry(fetcher.getHiPriQueue()))
        .containsExactly(
            new TestEntry(two, callback),
            new TestEntry(three, callback),
            new TestEntry(one, callback))
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
    FetchState one = fetch(fetcher, "1", callback, true);
    ((SettableProducerContext) one.getContext()).cancel();
    verify(callback).onCancellation();
  }

  private FetchState fetch(
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
    FetchState fetchState = new FetchState(consumer, context);
    when(delegate.createFetchState(eq(consumer), eq(context))).thenReturn(fetchState);
    fetcher.fetch(fetcher.createFetchState(consumer, context), callback);
    return fetchState;
  }

  private static List<TestEntry> toTestEntry(List<Entry<FetchState>> entries) {
    ArrayList<TestEntry> result = new ArrayList<>();
    for (Entry<FetchState> entry : entries) {
      result.add(new TestEntry(entry.fetchState, entry.callback));
    }
    return result;
  }

  /**
   * TestEntry is wrapped around Entry<> to provide it with equals() and toString(), so it's easier
   * to write tests and make assertions.
   */
  private static class TestEntry extends PriorityNetworkFetcher.Entry<FetchState> {
    TestEntry(FetchState fetchState, NetworkFetcher.Callback callback) {
      super(fetchState, callback);
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (o == null || getClass() != o.getClass()) return false;
      Entry<?> entry = (Entry<?>) o;
      return fetchState.equals(entry.fetchState) && callback.equals(entry.callback);
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(fetchState, callback);
    }

    @Override
    public String toString() {
      return String.format("TestEntry{fetchState=%s, callback=%s}", fetchState, callback);
    }
  }
}
