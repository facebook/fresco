/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.cache;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

import com.facebook.binaryresource.BinaryResource;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.cache.common.WriterCallback;
import com.facebook.cache.disk.FileCache;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.memory.PooledByteBufferFactory;
import com.facebook.imagepipeline.memory.PooledByteStreams;
import com.facebook.imagepipeline.testing.FakeClock;
import com.facebook.imagepipeline.testing.TestExecutorService;

import bolts.Task;
import org.junit.*;
import org.junit.runner.*;
import org.mockito.*;
import org.mockito.Mock;
import org.powermock.core.classloader.annotations.*;
import org.powermock.modules.junit4.rule.*;
import org.robolectric.*;
import org.robolectric.annotation.*;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.*;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareOnlyThisForTest(StagingArea.class)
@Config(manifest=Config.NONE)
public class BufferedDiskCacheTest {
  @Mock public FileCache mFileCache;
  @Mock public PooledByteBufferFactory mByteBufferFactory;
  @Mock public PooledByteStreams mPooledByteStreams;
  @Mock public StagingArea mStagingArea;
  @Mock public ImageCacheStatsTracker mImageCacheStatsTracker;
  @Mock public PooledByteBuffer mPooledByteBuffer;
  @Mock public InputStream mInputStream;
  @Mock public BinaryResource mBinaryResource;

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  private CacheKey mCacheKey;
  private AtomicBoolean mIsCancelled;
  private BufferedDiskCache mBufferedDiskCache;
  private CloseableReference<PooledByteBuffer> mCloseableReference;
  private EncodedImage mEncodedImage;
  private TestExecutorService mReadPriorityExecutor;
  private TestExecutorService mWritePriorityExecutor;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);
    mCloseableReference = CloseableReference.of(mPooledByteBuffer);
    mEncodedImage = new EncodedImage(mCloseableReference);
    mCacheKey = new SimpleCacheKey("http://test.uri");
    mIsCancelled = new AtomicBoolean(false);
    FakeClock fakeClock = new FakeClock();
    mReadPriorityExecutor = new TestExecutorService(fakeClock);
    mWritePriorityExecutor = new TestExecutorService(fakeClock);

    when(mBinaryResource.openStream()).thenReturn(mInputStream);
    when(mBinaryResource.size()).thenReturn(123L);
    when(mByteBufferFactory.newByteBuffer(same(mInputStream), eq(123)))
        .thenReturn(mPooledByteBuffer);

    mockStatic(StagingArea.class);
    when(StagingArea.getInstance()).thenReturn(mStagingArea);

    mBufferedDiskCache = new BufferedDiskCache(
        mFileCache,
        mByteBufferFactory,
        mPooledByteStreams,
        mReadPriorityExecutor,
        mWritePriorityExecutor,
        mImageCacheStatsTracker);
  }

  @Test
  public void testQueriesDiskCache() throws Exception {
    when(mFileCache.getResource(eq(mCacheKey))).thenReturn(mBinaryResource);
    Task<EncodedImage> readTask = mBufferedDiskCache.get(mCacheKey, mIsCancelled);
    mReadPriorityExecutor.runUntilIdle();
    verify(mFileCache).getResource(eq(mCacheKey));
    assertEquals(
        2,
        readTask.getResult().getByteBufferRef()
            .getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertSame(mPooledByteBuffer, readTask.getResult().getByteBufferRef().get());
  }

  @Test
  public void testCacheGetCancellation() throws Exception {
    when(mFileCache.getResource(eq(mCacheKey))).thenReturn(mBinaryResource);
    Task<EncodedImage> readTask = mBufferedDiskCache.get(mCacheKey, mIsCancelled);
    mIsCancelled.set(true);
    mReadPriorityExecutor.runUntilIdle();
    verify(mFileCache, never()).getResource(mCacheKey);
    assertTrue(readTask.isFaulted());
    assertTrue(readTask.getError() instanceof CancellationException);
  }

  @Test
  public void testGetDoesNotThrow() throws Exception {
    Task<EncodedImage> readTask = mBufferedDiskCache.get(mCacheKey, mIsCancelled);
    when(mFileCache.getResource(mCacheKey))
        .thenThrow(new RuntimeException("Should not be propagated"));
    assertFalse(readTask.isFaulted());
    assertNull(readTask.getResult());
  }

  @Test
  public void testWritesToDiskCache() throws Exception {
    mBufferedDiskCache.put(mCacheKey, mEncodedImage);

    reset(mPooledByteBuffer);
    when(mPooledByteBuffer.size()).thenReturn(0);

    final ArgumentCaptor<WriterCallback> wcCapture = ArgumentCaptor.forClass(WriterCallback.class);
    when(mFileCache.insert(
            eq(mCacheKey),
            wcCapture.capture())).thenReturn(null);

    mWritePriorityExecutor.runUntilIdle();
    OutputStream os = mock(OutputStream.class);
    wcCapture.getValue().write(os);

    // Ref count should be equal to 2 ('owned' by the mCloseableReference and other 'owned' by
    // mEncodedImage)
    assertEquals(2, mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
  }

  @Test
  public void testCacheMiss() throws Exception {
    Task<EncodedImage> readTask = mBufferedDiskCache.get(mCacheKey, mIsCancelled);
    mReadPriorityExecutor.runUntilIdle();
    verify(mFileCache).getResource(eq(mCacheKey));
    assertNull(readTask.getResult());
  }

  @Test
  public void testPutBumpsRefCountBeforeSubmit() {
    mBufferedDiskCache.put(mCacheKey, mEncodedImage);
    assertEquals(3, mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
  }

  @Test
  public void testManagesReference() throws Exception {
    mBufferedDiskCache.put(mCacheKey, mEncodedImage);
    mWritePriorityExecutor.runUntilIdle();
    assertEquals(2, mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
  }

  @Test
  public void testPins() {
    mBufferedDiskCache.put(mCacheKey, mEncodedImage);
    verify(mStagingArea).put(mCacheKey, mEncodedImage);
  }

  @Test
  public void testServesPinned() throws Exception {
    when(mStagingArea.get(mCacheKey)).thenReturn(mEncodedImage);
    assertEquals(2, mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertSame(
        mCloseableReference.getUnderlyingReferenceTestOnly(),
        mBufferedDiskCache.get(mCacheKey, mIsCancelled).getResult()
            .getByteBufferRef().getUnderlyingReferenceTestOnly());
  }

  @Test
  public void testServesPinned2() throws Exception {
    Task<EncodedImage> readTask = mBufferedDiskCache.get(mCacheKey, mIsCancelled);
    assertFalse(readTask.isCompleted());

    when(mStagingArea.get(mCacheKey)).thenReturn(mEncodedImage);
    mReadPriorityExecutor.runUntilIdle();

    assertSame(readTask.getResult(), mEncodedImage);
    verify(mFileCache, never()).getResource(eq(mCacheKey));
    // Ref count should be equal to 3 (One for mCloseableReference, one that is cloned when
    // mEncodedImage is created and a third one that is cloned when the method getByteBufferRef is
    // called in EncodedImage).
    assertEquals(
        3,
        mEncodedImage.getByteBufferRef()
            .getUnderlyingReferenceTestOnly().getRefCountTestOnly());
  }

  @Test
  public void testUnpins() {
    mBufferedDiskCache.put(mCacheKey, mEncodedImage);
    mWritePriorityExecutor.runUntilIdle();
    ArgumentCaptor<EncodedImage> argumentCaptor = ArgumentCaptor.forClass(EncodedImage.class);
    verify(mStagingArea).remove(eq(mCacheKey), argumentCaptor.capture());
    EncodedImage encodedImage = argumentCaptor.getValue();
    assertSame(
        mEncodedImage.getUnderlyingReferenceTestOnly(),
        encodedImage.getUnderlyingReferenceTestOnly());
  }

  @Test
  public void testPins2() {
    Task<Boolean> readTask = mBufferedDiskCache.contains(mCacheKey);
    assertFalse(readTask.isCompleted());
    when(mStagingArea.get(mCacheKey)).thenReturn(mEncodedImage);
    mReadPriorityExecutor.runUntilIdle();
    verify(mFileCache, never()).getResource(eq(mCacheKey));
  }

  @Test
  public void testUnpins2() {
    mBufferedDiskCache.remove(mCacheKey);
    verify(mStagingArea).remove(mCacheKey);
  }

  @Test
  public void testUpins3() {
    mBufferedDiskCache.clearAll();
    verify(mStagingArea).clearAll();
  }
}
