/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.producers;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import android.os.SystemClock;
import com.facebook.common.internal.Throwables;
import com.facebook.common.memory.ByteArrayPool;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.memory.PooledByteBufferOutputStream;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.common.Priority;
import com.facebook.imagepipeline.request.ImageRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import javax.annotation.concurrent.GuardedBy;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.Mock;
import org.powermock.api.mockito.*;
import org.powermock.core.classloader.annotations.*;
import org.powermock.modules.junit4.rule.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
@Config(manifest = Config.NONE)
@PrepareForTest({SystemClock.class})
public class NetworkFetchProducerTest {

  @Mock public ByteArrayPool mByteArrayPool;
  @Mock public PooledByteBuffer mPooledByteBuffer;
  @Mock public PooledByteBufferOutputStream mPooledByteBufferOutputStream;
  @Mock public PooledByteBufferFactory mPooledByteBufferFactory;
  @Mock public ImageRequest mImageRequest;
  @Mock public ProducerListener mProducerListener;
  @Mock public Consumer mConsumer;
  @Mock public NetworkFetcher mNetworkFetcher;
  @Mock public Map<String, String> mExtrasMap;

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  private byte[] mCommonByteArray;
  private final String mRequestId = "mRequestId";
  private NetworkFetchProducer mNetworkFetchProducer;
  private SettableProducerContext mProducerContext;
  private FetchState mFetchState;
  private ExecutorService mTestExecutor;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    PowerMockito.mockStatic(SystemClock.class);
    mNetworkFetchProducer = new NetworkFetchProducer(
        mPooledByteBufferFactory,
        mByteArrayPool,
        mNetworkFetcher);
    mProducerContext = new SettableProducerContext(
        mImageRequest,
        mRequestId,
        mProducerListener,
        mock(Object.class),
        ImageRequest.RequestLevel.FULL_FETCH,
        false /* isPrefetch */,
        true /* isIntermediateResultExpected */,
        Priority.MEDIUM);
    mFetchState = new FetchState(mConsumer, mProducerContext);
    mCommonByteArray = new byte[10];
    when(mByteArrayPool.get(anyInt())).thenReturn(mCommonByteArray);
    when(mPooledByteBufferFactory.newOutputStream(anyInt()))
        .thenReturn(mPooledByteBufferOutputStream);
    when(mPooledByteBufferFactory.newOutputStream())
        .thenReturn(mPooledByteBufferOutputStream);
    when(mPooledByteBufferOutputStream.toByteBuffer()).thenReturn(mPooledByteBuffer);
    when(mProducerListener.requiresExtraMap(anyString())).thenReturn(true);
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
    verify(mProducerListener).onProducerFinishWithFailure(
        eq(mRequestId),
        eq(NetworkFetchProducer.PRODUCER_NAME),
        any(RuntimeException.class),
        isNull(Map.class));
    verify(mProducerListener)
        .onUltimateProducerReached(mRequestId, NetworkFetchProducer.PRODUCER_NAME, false);
  }

  @Test(timeout = 5000)
  public void testNoIntermediateResults() throws Exception {
    long currentTime = 86400l;
    when(SystemClock.uptimeMillis()).thenReturn(currentTime);
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
    verify(mProducerListener, times(0)).onProducerEvent(
        mRequestId,
        NetworkFetchProducer.PRODUCER_NAME,
        NetworkFetchProducer.INTERMEDIATE_RESULT_PRODUCER_EVENT);
    // Test final result
    verify(mConsumer, times(1)).onNewResult(any(CloseableReference.class), eq(Consumer.IS_LAST));
    verifyPooledByteBufferUsed(1);
    // When everything is over, pooled byte buffer output stream should be closed
    verify(mPooledByteBufferOutputStream).close();
  }

  @Test(timeout = 5000)
  public void testDownloadHandler() throws Exception {
    long currentTime = 86400l;
    when(SystemClock.uptimeMillis()).thenReturn(currentTime);
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
    verify(mProducerListener, times(1)).onProducerEvent(
        mRequestId,
        NetworkFetchProducer.PRODUCER_NAME,
        NetworkFetchProducer.INTERMEDIATE_RESULT_PRODUCER_EVENT);
    verify(mConsumer, times(1)).onNewResult(any(CloseableReference.class), eq(Consumer.NO_FLAGS));
    verifyPooledByteBufferUsed(1);

    // Read another 1024 bytes, but do not bump timer - consumer should not be notified
    inputStream.increaseBytesToRead(1024);
    inputStream.waitUntilReadingThreadBlocked();
    verify(mProducerListener, times(1)).onProducerEvent(
        mRequestId,
        NetworkFetchProducer.PRODUCER_NAME,
        NetworkFetchProducer.INTERMEDIATE_RESULT_PRODUCER_EVENT);
    verify(mConsumer, times(1)).onNewResult(any(CloseableReference.class), eq(Consumer.NO_FLAGS));
    verifyPooledByteBufferUsed(1);

    // Read another 1024 bytes - this time bump timer. Consumer should be notified
    currentTime += NetworkFetchProducer.TIME_BETWEEN_PARTIAL_RESULTS_MS;
    when(SystemClock.uptimeMillis()).thenReturn(currentTime);
    inputStream.increaseBytesToRead(1024);
    inputStream.waitUntilReadingThreadBlocked();
    verify(mProducerListener, times(2)).onProducerEvent(
        mRequestId,
        NetworkFetchProducer.PRODUCER_NAME,
        NetworkFetchProducer.INTERMEDIATE_RESULT_PRODUCER_EVENT);
    verify(mConsumer, times(2)).onNewResult(any(CloseableReference.class), eq(Consumer.NO_FLAGS));
    verifyPooledByteBufferUsed(2);

    // Test final result
    verify(mConsumer, times(0)).onNewResult(any(CloseableReference.class), eq(Consumer.IS_LAST));
    inputStream.signalEof();
    requestHandlerFuture.get();
    verify(mProducerListener, times(2)).onProducerEvent(
        mRequestId,
        NetworkFetchProducer.PRODUCER_NAME,
        NetworkFetchProducer.INTERMEDIATE_RESULT_PRODUCER_EVENT);
    verify(mProducerListener).onProducerFinishWithSuccess(
        eq(mRequestId), eq(NetworkFetchProducer.PRODUCER_NAME), eq(mExtrasMap));
    verify(mProducerListener)
        .onUltimateProducerReached(mRequestId, NetworkFetchProducer.PRODUCER_NAME, true);
    verify(mConsumer, times(1)).onNewResult(any(CloseableReference.class), eq(Consumer.IS_LAST));
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
      fail();
    } catch (Exception e) {
      verify(mPooledByteBufferFactory).newOutputStream(100);
      verify(mPooledByteBufferOutputStream).close();
      verify(mProducerListener, never())
          .onProducerEvent(eq(mRequestId), any(String.class), any(String.class));
    }
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
      final InputStream inputStream,
      final int length,
      final NetworkFetcher.Callback callback) {
    return mTestExecutor.submit(
        new Callable() {
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
      fail();
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
}
