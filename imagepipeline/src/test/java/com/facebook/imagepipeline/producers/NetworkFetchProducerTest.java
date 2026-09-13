/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.*;

import android.net.Uri;
import com.facebook.common.internal.Throwables;
import com.facebook.common.memory.ByteArrayPool;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.memory.PooledByteBufferOutputStream;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.core.ImagePipelineConfig;
import com.facebook.imagepipeline.decoder.ProgressiveJpegConfig;
import com.facebook.imagepipeline.request.ImageRequest;
import com.facebook.infer.annotation.Nullsafe;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicInteger;
import javax.annotation.Nullable;
import javax.annotation.concurrent.GuardedBy;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.Mock;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE)
@Nullsafe(Nullsafe.Mode.LOCAL)
public class NetworkFetchProducerTest {

  @Mock public ByteArrayPool mByteArrayPool;
  @Mock public PooledByteBuffer mPooledByteBuffer;
  @Mock public PooledByteBufferOutputStream mPooledByteBufferOutputStream;
  @Mock public PooledByteBufferFactory mPooledByteBufferFactory;
  @Mock public ImageRequest mImageRequest;
  @Mock public ProducerListener2 mProducerListener;
  @Mock public Consumer mConsumer;
  @Mock public NetworkFetcher mNetworkFetcher;
  @Mock public Map<String, String> mExtrasMap;
  @Mock public ImagePipelineConfig mConfig;
  @Mock public ProgressiveJpegConfig mProgressiveJpegConfig;

  private byte[] mCommonByteArray;
  private final String mRequestId = "mRequestId";
  private long mCurrentTimeMs = 86400L;
  private TestNetworkFetchProducer mNetworkFetchProducer;
  private SettableProducerContext mProducerContext;
  private FetchState mFetchState;
  private ExecutorService mTestExecutor;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mNetworkFetchProducer =
        new TestNetworkFetchProducer(mPooledByteBufferFactory, mByteArrayPool, mNetworkFetcher);
    mProducerContext =
        new SettableProducerContext(
            mImageRequest,
            mRequestId,
            mProducerListener,
            mock(Object.class),
            ImageRequest.RequestLevel.FULL_FETCH,
            false /* isPrefetch */,
            true /* isIntermediateResultExpected */,
            Priority.MEDIUM,
            mConfig);
    when(mConfig.getProgressiveJpegConfig()).thenReturn(mProgressiveJpegConfig);
    when(mProgressiveJpegConfig.decodeProgressively(mImageRequest)).thenReturn(true);
    when(mProgressiveJpegConfig.getTimeBetweenPartialResultsMs(mImageRequest))
        .thenReturn(NetworkFetchProducer.TIME_BETWEEN_PARTIAL_RESULTS_MS);
    mFetchState = new FetchState(mConsumer, mProducerContext);
    when(mImageRequest.getSourceUri()).thenReturn(Uri.parse("http://www.facebook.com"));
    mCommonByteArray = new byte[10];
    when(mByteArrayPool.get(anyInt())).thenReturn(mCommonByteArray);
    when(mPooledByteBufferFactory.newOutputStream(anyInt()))
        .thenReturn(mPooledByteBufferOutputStream);
    when(mPooledByteBufferFactory.newOutputStream()).thenReturn(mPooledByteBufferOutputStream);
    when(mPooledByteBufferOutputStream.toByteBuffer()).thenReturn(mPooledByteBuffer);
    when(mProducerListener.requiresExtraMap(mProducerContext, NetworkFetchProducer.PRODUCER_NAME))
        .thenReturn(true);
    when(mNetworkFetcher.getExtraMap(any(FetchState.class), anyInt())).thenReturn(mExtrasMap);
    when(mNetworkFetcher.createFetchState(eq(mConsumer), eq(mProducerContext)))
        .thenReturn(mFetchState);
    mTestExecutor = Executors.newCachedThreadPool();
  }

  @After
  public void tearDown() {
    mTestExecutor.shutdownNow();
  }

  @Test
  public void testExceptionInFetchImage() {
    NetworkFetcher.Callback callback = performFetch();

    callback.onFailure(new RuntimeException());
    verify(mProducerListener)
        .onProducerFinishWithFailure(
            eq(mProducerContext),
            eq(NetworkFetchProducer.PRODUCER_NAME),
            any(RuntimeException.class),
            isNull(Map.class));
    verify(mProducerListener)
        .onUltimateProducerReached(mProducerContext, NetworkFetchProducer.PRODUCER_NAME, false);
  }

  @Test(timeout = 5000)
  public void testNoIntermediateResults() throws Exception {
    long currentTime = 86400l;
    mNetworkFetchProducer.setSystemUptime(currentTime);
    NetworkFetcher.Callback callback = performFetch();

    when(mNetworkFetcher.shouldPropagate(any(FetchState.class))).thenReturn(false);
    final BlockingInputStream inputStream = new BlockingInputStream();
    final Future requestHandlerFuture = performResponse(inputStream, -1, callback);

    // Consumer should not be notified before any data is read
    inputStream.waitUntilReadingThreadBlocked();
    verify(mPooledByteBufferFactory).newOutputStream();
    verify(mConsumer, never()).onNewResult(any(CloseableReference.class), anyInt());
    verifyPooledByteBufferUsed(0);

    // Allow NetworkFetchProducer to read 1024 bytes and check that consumer is not notified
    inputStream.increaseBytesToRead(1024);
    inputStream.waitUntilReadingThreadBlocked();
    inputStream.increaseBytesToRead(1024);
    inputStream.waitUntilReadingThreadBlocked();
    verify(mConsumer, never()).onNewResult(any(CloseableReference.class), anyInt());
    verifyPooledByteBufferUsed(0);

    inputStream.signalEof();
    requestHandlerFuture.get();
    // Check no intermediate results were propagated
    verify(mProducerListener, times(0))
        .onProducerEvent(
            mProducerContext,
            NetworkFetchProducer.PRODUCER_NAME,
            NetworkFetchProducer.INTERMEDIATE_RESULT_PRODUCER_EVENT);
    // Test final result
    verify(mConsumer, times(1)).onNewResult(any(), eq(Consumer.IS_LAST));
    verifyPooledByteBufferUsed(1);
    // When everything is over, pooled byte buffer output stream should be closed
    verify(mPooledByteBufferOutputStream).close();
  }

  @Test(timeout = 5000)
  public void testDownloadHandler() throws Exception {
    long currentTime = 86400l;
    mNetworkFetchProducer.setSystemUptime(currentTime);
    NetworkFetcher.Callback callback = performFetch();

    when(mNetworkFetcher.shouldPropagate(any(FetchState.class))).thenReturn(true);
    final BlockingInputStream inputStream = new BlockingInputStream();
    final Future requestHandlerFuture = performResponse(inputStream, -1, callback);

    // Consumer should not be notified before any data is read
    inputStream.waitUntilReadingThreadBlocked();
    verify(mPooledByteBufferFactory).newOutputStream();
    verify(mConsumer, never()).onNewResult(any(CloseableReference.class), anyInt());
    verifyPooledByteBufferUsed(0);

    // Allow NetworkFetchProducer to read 1024 bytes and check that consumer is notified once
    inputStream.increaseBytesToRead(1024);
    inputStream.waitUntilReadingThreadBlocked();
    verify(mProducerListener, times(1))
        .onProducerEvent(
            mProducerContext,
            NetworkFetchProducer.PRODUCER_NAME,
            NetworkFetchProducer.INTERMEDIATE_RESULT_PRODUCER_EVENT);
    verify(mConsumer, times(1)).onNewResult(any(), eq(Consumer.NO_FLAGS));
    verifyPooledByteBufferUsed(1);

    // Read another 1024 bytes, but do not bump timer - consumer should not be notified
    inputStream.increaseBytesToRead(1024);
    inputStream.waitUntilReadingThreadBlocked();
    verify(mProducerListener, times(1))
        .onProducerEvent(
            mProducerContext,
            NetworkFetchProducer.PRODUCER_NAME,
            NetworkFetchProducer.INTERMEDIATE_RESULT_PRODUCER_EVENT);
    verify(mConsumer, times(1)).onNewResult(any(), eq(Consumer.NO_FLAGS));
    verifyPooledByteBufferUsed(1);

    // Read another 1024 bytes - this time bump timer. Consumer should be notified
    currentTime += NetworkFetchProducer.TIME_BETWEEN_PARTIAL_RESULTS_MS;
    mNetworkFetchProducer.setSystemUptime(currentTime);
    inputStream.increaseBytesToRead(1024);
    inputStream.waitUntilReadingThreadBlocked();
    verify(mProducerListener, times(2))
        .onProducerEvent(
            mProducerContext,
            NetworkFetchProducer.PRODUCER_NAME,
            NetworkFetchProducer.INTERMEDIATE_RESULT_PRODUCER_EVENT);
    verify(mConsumer, times(2)).onNewResult(any(), eq(Consumer.NO_FLAGS));
    verifyPooledByteBufferUsed(2);

    // Test final result
    verify(mConsumer, times(0)).onNewResult(any(), eq(Consumer.IS_LAST));
    inputStream.signalEof();
    requestHandlerFuture.get();
    verify(mProducerListener, times(2))
        .onProducerEvent(
            mProducerContext,
            NetworkFetchProducer.PRODUCER_NAME,
            NetworkFetchProducer.INTERMEDIATE_RESULT_PRODUCER_EVENT);
    verify(mProducerListener)
        .onProducerFinishWithSuccess(
            eq(mProducerContext), eq(NetworkFetchProducer.PRODUCER_NAME), eq(mExtrasMap));
    verify(mProducerListener)
        .onUltimateProducerReached(mProducerContext, NetworkFetchProducer.PRODUCER_NAME, true);
    verify(mConsumer, times(1)).onNewResult(any(), eq(Consumer.IS_LAST));
    verifyPooledByteBufferUsed(3);

    // When everything is over, pooled byte buffer output stream should be closed
    verify(mPooledByteBufferOutputStream).close();
  }

  @Test
  public void testExceptionInResponseHandler() throws IOException {
    NetworkFetcher.Callback callback = performFetch();
    InputStream inputStream = mock(InputStream.class);
    when(inputStream.read(any(byte[].class))).thenThrow(new IOException());
    when(mNetworkFetcher.shouldPropagate(any(FetchState.class))).thenReturn(false);
    try {
      callback.onResponse(inputStream, 100);
      fail("Expected exception to be thrown");
    } catch (Exception e) {
      verify(mPooledByteBufferFactory).newOutputStream(100);
      verify(mPooledByteBufferOutputStream).close();
      verify(mProducerListener, never())
          .onProducerEvent(eq(mProducerContext), any(String.class), any(String.class));
    }
  }

  @Test
  public void testPartialResultsAreSpacedFromTheSizeTheLastOneWentOutAt() {
    when(mNetworkFetcher.shouldPropagate(any(FetchState.class))).thenReturn(true);
    when(mNetworkFetcher.getPartialResultThrottle(any(FetchState.class)))
        .thenReturn(new PartialResultThrottle(100_000, 20_000, 0));

    deliverBytes(90_000);
    verify(mConsumer, never()).onNewResult(any(), eq(Consumer.NO_FLAGS));

    // Overshoots the first threshold, so the next one is 20k past 150_000, not past 100_000.
    deliverBytes(150_000);
    verify(mConsumer, times(1)).onNewResult(any(), eq(Consumer.NO_FLAGS));

    deliverBytes(160_000);
    verify(mConsumer, times(1)).onNewResult(any(), eq(Consumer.NO_FLAGS));

    deliverBytes(170_000);
    verify(mConsumer, times(2)).onNewResult(any(), eq(Consumer.NO_FLAGS));
    assertThat(mFetchState.getLastIntermediateResultSizeBytes()).isEqualTo(170_000);

    verify(mNetworkFetcher, times(1)).getPartialResultThrottle(any(FetchState.class));
  }

  @Test
  public void testATimeThrottledPartialResultDoesNotConsumeAByteThreshold() {
    mNetworkFetchProducer.setSystemUptime(mCurrentTimeMs);
    mFetchState.setLastIntermediateResultTimeMs(mCurrentTimeMs);
    when(mNetworkFetcher.shouldPropagate(any(FetchState.class))).thenReturn(true);
    when(mNetworkFetcher.getPartialResultThrottle(any(FetchState.class)))
        .thenReturn(new PartialResultThrottle(100_000, 20_000, 0));
    when(mPooledByteBufferOutputStream.size()).thenReturn(150_000);

    mNetworkFetchProducer.maybeHandleIntermediateResult(mPooledByteBufferOutputStream, mFetchState);

    verify(mConsumer, never()).onNewResult(any(), eq(Consumer.NO_FLAGS));
    assertThat(mFetchState.getLastIntermediateResultSizeBytes()).isEqualTo(0);

    // The threshold was not consumed, so the same size goes out as soon as the timer allows.
    deliverBytes(150_000);
    verify(mConsumer, times(1)).onNewResult(any(), eq(Consumer.NO_FLAGS));
  }

  @Test
  public void testAMinIntervalReplacesTheProgressiveJpegInterval() {
    when(mNetworkFetcher.shouldPropagate(any(FetchState.class))).thenReturn(true);
    when(mNetworkFetcher.getPartialResultThrottle(any(FetchState.class)))
        .thenReturn(new PartialResultThrottle(0, 0, 500L));
    when(mPooledByteBufferOutputStream.size()).thenReturn(1024);
    mFetchState.setLastIntermediateResultTimeMs(mCurrentTimeMs);

    // Past the 100 ms the pjpeg config asks for, but short of the 500 ms this fetch asks for.
    mNetworkFetchProducer.setSystemUptime(mCurrentTimeMs + 499L);
    mNetworkFetchProducer.maybeHandleIntermediateResult(mPooledByteBufferOutputStream, mFetchState);
    verify(mConsumer, never()).onNewResult(any(), eq(Consumer.NO_FLAGS));

    mNetworkFetchProducer.setSystemUptime(mCurrentTimeMs + 500L);
    mNetworkFetchProducer.maybeHandleIntermediateResult(mPooledByteBufferOutputStream, mFetchState);
    verify(mConsumer, times(1)).onNewResult(any(), eq(Consumer.NO_FLAGS));
  }

  @Test
  public void testAnImageSmallerThanTheFirstChunkOnlyDeliversTheFinalResult() {
    when(mNetworkFetcher.shouldPropagate(any(FetchState.class))).thenReturn(true);
    when(mNetworkFetcher.getPartialResultThrottle(any(FetchState.class)))
        .thenReturn(new PartialResultThrottle(100_000, 20_000, 0));

    deliverBytes(10_000);
    mNetworkFetchProducer.handleFinalResult(mPooledByteBufferOutputStream, mFetchState);

    verify(mConsumer, never()).onNewResult(any(), eq(Consumer.NO_FLAGS));
    verify(mConsumer, times(1)).onNewResult(any(), eq(Consumer.IS_LAST));
  }

  @Test(timeout = 5000)
  public void testANewResponseBodyStartsTheByteThresholdsOver() throws Exception {
    when(mNetworkFetcher.shouldPropagate(any(FetchState.class))).thenReturn(true);
    when(mNetworkFetcher.getPartialResultThrottle(any(FetchState.class)))
        .thenReturn(new PartialResultThrottle(100_000, 20_000, 0));
    deliverBytes(150_000);

    mNetworkFetchProducer.onResponse(mFetchState, new ByteArrayInputStream(new byte[0]), -1);

    assertThat(mFetchState.getLastIntermediateResultSizeBytes()).isEqualTo(0);
  }

  @Test
  public void testASecondResponseBodyIsNotSuppressedByTheSizeTheFirstOneReached() {
    // A NetworkFetchProducer subclass can read a second body after a retry without going back
    // through onResponse, so the reset there is not enough on its own.
    when(mNetworkFetcher.shouldPropagate(any(FetchState.class))).thenReturn(true);
    deliverBytes(300_000);
    verify(mConsumer, times(1)).onNewResult(any(), eq(Consumer.NO_FLAGS));

    deliverBytes(16_384);

    verify(mConsumer, times(2)).onNewResult(any(), eq(Consumer.NO_FLAGS));
  }

  @Test(timeout = 5000)
  public void testTheReadLoopPropagatesAtTheConfiguredChunkBoundaries() throws Exception {
    final List<Integer> propagatedAt = new ArrayList<>();
    doAnswer(
            invocation -> {
              propagatedAt.add(mFetchState.getLastIntermediateResultSizeBytes());
              return null;
            })
        .when(mConsumer)
        .onNewResult(any(), eq(Consumer.NO_FLAGS));
    trackOutputStreamSize();
    when(mByteArrayPool.get(anyInt())).thenReturn(new byte[16 * 1024]);
    when(mNetworkFetcher.shouldPropagate(any(FetchState.class))).thenReturn(true);
    when(mNetworkFetcher.getPartialResultThrottle(any(FetchState.class)))
        .thenReturn(new PartialResultThrottle(100_000, 20_000, 0));
    // The loop reads far faster than any interval, so the byte thresholds alone decide here.
    when(mProgressiveJpegConfig.getTimeBetweenPartialResultsMs(mImageRequest)).thenReturn(0L);

    mNetworkFetchProducer.onResponse(
        mFetchState, new ByteArrayInputStream(new byte[250_000]), 250_000);

    // 16 KB reads: the first multiple past 100_000, then the first past each 20_000 step from
    // wherever the previous one landed. The 250_000 tail is 4_240 short of the next step.
    assertThat(propagatedAt).containsExactly(114_688, 147_456, 180_224, 212_992, 245_760);
    verify(mConsumer, times(1)).onNewResult(any(), eq(Consumer.IS_LAST));
  }

  /** Makes the mocked output stream report the size the read loop has actually written to it. */
  private void trackOutputStreamSize() throws IOException {
    final AtomicInteger sizeBytes = new AtomicInteger();
    doAnswer(
            invocation -> {
              sizeBytes.addAndGet((Integer) invocation.getArguments()[2]);
              return null;
            })
        .when(mPooledByteBufferOutputStream)
        .write(any(byte[].class), anyInt(), anyInt());
    when(mPooledByteBufferOutputStream.size()).thenAnswer(invocation -> sizeBytes.get());
  }

  /** Runs one read of the response body that brings the accumulated size to {@code sizeBytes}. */
  private void deliverBytes(int sizeBytes) {
    mCurrentTimeMs += NetworkFetchProducer.TIME_BETWEEN_PARTIAL_RESULTS_MS;
    mNetworkFetchProducer.setSystemUptime(mCurrentTimeMs);
    when(mPooledByteBufferOutputStream.size()).thenReturn(sizeBytes);
    mNetworkFetchProducer.maybeHandleIntermediateResult(mPooledByteBufferOutputStream, mFetchState);
  }

  private void verifyPooledByteBufferUsed(int times) {
    verify(mPooledByteBufferOutputStream, times(times)).toByteBuffer();
    verify(mPooledByteBuffer, times(times)).close();
  }

  private NetworkFetcher.Callback performFetch() {
    mNetworkFetchProducer.produceResults(mConsumer, mProducerContext);
    ArgumentCaptor<NetworkFetcher.Callback> callbackCaptor =
        ArgumentCaptor.forClass(NetworkFetcher.Callback.class);
    verify(mNetworkFetcher).fetch(any(FetchState.class), callbackCaptor.capture());
    return callbackCaptor.getValue();
  }

  private Future performResponse(
      final InputStream inputStream, final int length, final NetworkFetcher.Callback callback) {
    return mTestExecutor.submit(
        new Callable() {
          @Nullable
          @Override
          public Object call() throws Exception {
            callback.onResponse(inputStream, length);
            return null;
          }
        });
  }

  private class BlockingInputStream extends InputStream {

    @GuardedBy("BlockingInputStream.this")
    private int mBytesLeft = 0;

    @GuardedBy("BlockingInputStream.this")
    private boolean mFinished = false;

    @GuardedBy("BlockingInputStream.this")
    private boolean mReaderBlocked = false;

    @Override
    public int read() throws IOException {
      fail("This method should not be called");
      return 0;
    }

    @Override
    public synchronized int read(byte[] buffer, int offset, int length) throws IOException {
      while (true) {
        if (mBytesLeft > 0) {
          final int bytesToRead = Math.min(mBytesLeft, length);
          mBytesLeft -= bytesToRead;
          return bytesToRead;
        } else if (mFinished) {
          return -1;
        } else {
          mReaderBlocked = true;
          try {
            notify();
            wait();
          } catch (InterruptedException ie) {
            throw Throwables.propagate(ie);
          } finally {
            mReaderBlocked = false;
          }
        }
      }
    }

    public synchronized void increaseBytesToRead(int n) {
      mBytesLeft += n;
      notify();
    }

    public synchronized void waitUntilReadingThreadBlocked() {
      while (mBytesLeft > 0 || !mReaderBlocked) {
        try {
          wait();
        } catch (InterruptedException ie) {
          throw Throwables.propagate(ie);
        }
      }
    }

    public synchronized void signalEof() {
      mFinished = true;
      notify();
    }
  }

  private static class TestNetworkFetchProducer extends NetworkFetchProducer {

    private long mSystemUptime;

    public TestNetworkFetchProducer(
        PooledByteBufferFactory pooledByteBufferFactory,
        ByteArrayPool byteArrayPool,
        NetworkFetcher networkFetcher) {
      super(pooledByteBufferFactory, byteArrayPool, networkFetcher);
    }

    public void setSystemUptime(long systemUptime) {
      mSystemUptime = systemUptime;
    }

    @Override
    protected long getSystemUptime() {
      return mSystemUptime;
    }
  }
}
