/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import bolts.Task;
import com.facebook.binaryresource.BinaryResource;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.MultiCacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.cache.common.WriterCallback;
import com.facebook.cache.disk.FileCache;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.memory.PooledByteStreams;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.testing.FakeClock;
import com.facebook.imagepipeline.testing.TestExecutorService;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
@PrepareOnlyThisForTest(StagingArea.class)
@Config(manifest = Config.NONE)
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

  private MultiCacheKey mCacheKey;
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
    List<CacheKey> keys = new ArrayList<>();
    keys.add(new SimpleCacheKey("http://test.uri"));
    keys.add(new SimpleCacheKey("http://tyrone.uri"));
    keys.add(new SimpleCacheKey("http://ian.uri"));
    mCacheKey = new MultiCacheKey(keys);

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
  public void testHasKeySyncFromFileCache() {
    when(mFileCache.hasKeySync(mCacheKey)).thenReturn(true);
    assertTrue(mBufferedDiskCache.containsSync(mCacheKey));
  }

  @Test
  public void testHasKeySyncFromStagingArea() {
    when(mStagingArea.containsKey(mCacheKey)).thenReturn(true);
    assertTrue(mBufferedDiskCache.containsSync(mCacheKey));
  }

  @Test
  public void testDoesntAlwaysHaveKeySync() {
    when(mFileCache.hasKey(mCacheKey)).thenReturn(true);
    assertFalse(mBufferedDiskCache.containsSync(mCacheKey));
  }

  @Test
  public void testSyncDiskCacheCheck() {
    when(mStagingArea.containsKey(mCacheKey) || mFileCache.hasKey(mCacheKey)).thenReturn(true);
    assertTrue(mBufferedDiskCache.diskCheckSync(mCacheKey));
  }

  @Test
  public void testQueriesDiskCache() throws Exception {
    when(mFileCache.getResource(eq(mCacheKey))).thenReturn(mBinaryResource);
    Task<EncodedImage> readTask = mBufferedDiskCache.get(mCacheKey, mIsCancelled);
    mReadPriorityExecutor.runUntilIdle();
    verify(mFileCache).getResource(eq(mCacheKey));
    EncodedImage result = readTask.getResult();
    assertEquals(
        2,
        result.getByteBufferRef().getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertSame(mPooledByteBuffer, result.getByteBufferRef().get());
  }

  @Test
  public void testCacheGetCancellation() throws Exception {
    when(mFileCache.getResource(mCacheKey)).thenReturn(mBinaryResource);
    Task<EncodedImage> readTask = mBufferedDiskCache.get(mCacheKey, mIsCancelled);
    mIsCancelled.set(true);
    mReadPriorityExecutor.runUntilIdle();
    verify(mFileCache, never()).getResource(mCacheKey);
    assertTrue(isTaskCancelled(readTask));
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
  public void testFromStagingArea() throws Exception {
    when(mStagingArea.get(mCacheKey)).thenReturn(mEncodedImage);
    assertEquals(2, mCloseableReference.getUnderlyingReferenceTestOnly().getRefCountTestOnly());
    assertSame(
        mCloseableReference.getUnderlyingReferenceTestOnly(),
        mBufferedDiskCache.get(mCacheKey, mIsCancelled).getResult()
            .getByteBufferRef().getUnderlyingReferenceTestOnly());
  }

  @Test
  public void testFromStagingAreaLater() throws Exception {
    Task<EncodedImage> readTask = mBufferedDiskCache.get(mCacheKey, mIsCancelled);
    assertFalse(readTask.isCompleted());

    when(mStagingArea.get(mCacheKey)).thenReturn(mEncodedImage);
    mReadPriorityExecutor.runUntilIdle();

    EncodedImage result = readTask.getResult();
    assertSame(result, mEncodedImage);
    verify(mFileCache, never()).getResource(eq(mCacheKey));
    // Ref count should be equal to 3 (One for mCloseableReference, one that is cloned when
    // mEncodedImage is created and a third one that is cloned when the method getByteBufferRef is
    // called in EncodedImage).
    assertEquals(
        3,
        result.getByteBufferRef().getUnderlyingReferenceTestOnly().getRefCountTestOnly());
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
  public void testContainsFromStagingAreaLater() {
    Task<Boolean> readTask = mBufferedDiskCache.contains(mCacheKey);
    assertFalse(readTask.isCompleted());
    when(mStagingArea.get(mCacheKey)).thenReturn(mEncodedImage);
    mReadPriorityExecutor.runUntilIdle();
    verify(mFileCache, never()).getResource(eq(mCacheKey));
  }

  @Test
  public void testRemoveFromStagingArea() {
    mBufferedDiskCache.remove(mCacheKey);
    verify(mStagingArea).remove(mCacheKey);
  }

  @Test
  public void testClearFromStagingArea() {
    mBufferedDiskCache.clearAll();
    verify(mStagingArea).clearAll();
  }

  private static boolean isTaskCancelled(Task<?> task) {
    return task.isCancelled() ||
        (task.isFaulted() && task.getError() instanceof CancellationException);
  }
}
