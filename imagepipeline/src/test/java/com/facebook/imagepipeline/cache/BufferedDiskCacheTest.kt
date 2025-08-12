/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache

import bolts.Task
import com.facebook.binaryresource.BinaryResource
import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.MultiCacheKey
import com.facebook.cache.common.SimpleCacheKey
import com.facebook.cache.common.WriterCallback
import com.facebook.cache.disk.FileCache
import com.facebook.common.memory.PooledByteBuffer
import com.facebook.common.memory.PooledByteBufferFactory
import com.facebook.common.memory.PooledByteStreams
import com.facebook.common.references.CloseableReference
import com.facebook.imagepipeline.image.EncodedImage
import com.facebook.imagepipeline.testing.FakeClock
import com.facebook.imagepipeline.testing.TestExecutorService
import java.io.InputStream
import java.io.OutputStream
import java.util.ArrayList
import java.util.concurrent.CancellationException
import java.util.concurrent.atomic.AtomicBoolean
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockedStatic
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.same
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BufferedDiskCacheTest {
  @Mock lateinit var fileCache: FileCache
  @Mock lateinit var byteBufferFactory: PooledByteBufferFactory
  @Mock lateinit var pooledByteStreams: PooledByteStreams
  @Mock lateinit var stagingArea: StagingArea
  @Mock lateinit var imageCacheStatsTracker: ImageCacheStatsTracker
  @Mock lateinit var pooledByteBuffer: PooledByteBuffer
  @Mock lateinit var inputStream: InputStream
  @Mock lateinit var binaryResource: BinaryResource

  private lateinit var cacheKey: MultiCacheKey
  private lateinit var isCancelled: AtomicBoolean
  private lateinit var bufferedDiskCache: BufferedDiskCache
  private lateinit var closeableReference: CloseableReference<PooledByteBuffer>
  private lateinit var encodedImage: EncodedImage
  private lateinit var readPriorityExecutor: TestExecutorService
  private lateinit var writePriorityExecutor: TestExecutorService
  private lateinit var mockedStagingArea: MockedStatic<StagingArea>

  @Before
  fun setUp() {
    mockedStagingArea = org.mockito.Mockito.mockStatic(StagingArea::class.java)
    MockitoAnnotations.initMocks(this)
    closeableReference = CloseableReference.of(pooledByteBuffer)
    encodedImage = EncodedImage(closeableReference)
    val keys = ArrayList<CacheKey>()
    keys.add(SimpleCacheKey("http://test.uri"))
    keys.add(SimpleCacheKey("http://tyrone.uri"))
    keys.add(SimpleCacheKey("http://ian.uri"))
    cacheKey = MultiCacheKey(keys)

    isCancelled = AtomicBoolean(false)
    val fakeClock = FakeClock()
    readPriorityExecutor = TestExecutorService(fakeClock)
    writePriorityExecutor = TestExecutorService(fakeClock)

    whenever(binaryResource.openStream()).thenReturn(inputStream)
    whenever(binaryResource.size()).thenReturn(123L)
    whenever(byteBufferFactory.newByteBuffer(same(inputStream), eq(123)))
        .thenReturn(pooledByteBuffer)

    whenever(StagingArea.getInstance()).thenAnswer { stagingArea }

    bufferedDiskCache =
        BufferedDiskCache(
            fileCache,
            byteBufferFactory,
            pooledByteStreams,
            readPriorityExecutor,
            writePriorityExecutor,
            imageCacheStatsTracker,
            false)
  }

  @After
  fun tearDownStaticMocks() {
    mockedStagingArea.close()
  }

  @Test
  fun testHasKeySyncFromFileCache() {
    whenever(fileCache.hasKeySync(cacheKey)).thenReturn(true)
    assertThat(bufferedDiskCache.containsSync(cacheKey)).isTrue()
  }

  @Test
  fun testHasKeySyncFromStagingArea() {
    whenever(stagingArea.containsKey(cacheKey)).thenReturn(true)
    assertThat(bufferedDiskCache.containsSync(cacheKey)).isTrue()
  }

  @Test
  fun testDoesntAlwaysHaveKeySync() {
    whenever(fileCache.hasKey(cacheKey)).thenReturn(true)
    assertThat(bufferedDiskCache.containsSync(cacheKey)).isFalse()
  }

  @Test
  fun testSyncDiskCacheCheck() {
    whenever(stagingArea.containsKey(cacheKey) || fileCache.hasKey(cacheKey)).thenReturn(true)
    assertThat(bufferedDiskCache.diskCheckSync(cacheKey)).isTrue()
  }

  @Test
  fun testQueriesDiskCache() {
    whenever(fileCache.getResource(eq(cacheKey))).thenReturn(binaryResource)
    val readTask = bufferedDiskCache.get(cacheKey, isCancelled)
    readPriorityExecutor.runUntilIdle()
    verify(fileCache).getResource(eq(cacheKey))
    val result = readTask.result
    assertThat(result.byteBufferRef.underlyingReferenceTestOnly.refCountTestOnly).isEqualTo(2)
    assertThat(result.byteBufferRef.get()).isSameAs(pooledByteBuffer)
  }

  @Test
  fun testCacheGetCancellation() {
    whenever(fileCache.getResource(cacheKey)).thenReturn(binaryResource)
    val readTask = bufferedDiskCache.get(cacheKey, isCancelled)
    isCancelled.set(true)
    readPriorityExecutor.runUntilIdle()
    verify(fileCache, never()).getResource(cacheKey)
    assertThat(isTaskCancelled(readTask)).isTrue()
  }

  @Test
  fun testGetDoesNotThrow() {
    val readTask = bufferedDiskCache.get(cacheKey, isCancelled)
    whenever(fileCache.getResource(cacheKey))
        .thenThrow(RuntimeException("Should not be propagated"))
    assertThat(readTask.isFaulted).isFalse()
    assertThat(readTask.result).isNull()
  }

  @Test
  fun testWritesToDiskCache() {
    bufferedDiskCache.put(cacheKey, encodedImage)

    reset(pooledByteBuffer)
    whenever(pooledByteBuffer.size()).thenReturn(0)

    val wcCapture = argumentCaptor<WriterCallback>()
    val os: OutputStream = mock()
    whenever(fileCache.insert(eq(cacheKey), wcCapture.capture())).thenAnswer { invocation ->
      val wc = invocation.arguments[1] as WriterCallback
      wc.write(os)
      null
    }

    writePriorityExecutor.runUntilIdle()

    // Ref count should be equal to 2 ('owned' by the closeableReference and other 'owned' by
    // encodedImage)
    assertThat(closeableReference.underlyingReferenceTestOnly.refCountTestOnly).isEqualTo(2)
  }

  @Test
  fun testCacheMiss() {
    val readTask = bufferedDiskCache.get(cacheKey, isCancelled)
    readPriorityExecutor.runUntilIdle()
    verify(fileCache).getResource(eq(cacheKey))
    assertThat(readTask.result).isNull()
  }

  @Test
  fun testPutBumpsRefCountBeforeSubmit() {
    bufferedDiskCache.put(cacheKey, encodedImage)
    assertThat(closeableReference.underlyingReferenceTestOnly.refCountTestOnly).isEqualTo(3)
  }

  @Test
  fun testManagesReference() {
    bufferedDiskCache.put(cacheKey, encodedImage)
    writePriorityExecutor.runUntilIdle()
    assertThat(closeableReference.underlyingReferenceTestOnly.refCountTestOnly).isEqualTo(2)
  }

  @Test
  fun testPins() {
    bufferedDiskCache.put(cacheKey, encodedImage)
    verify(stagingArea).put(cacheKey, encodedImage)
  }

  @Test
  fun testFromStagingArea() {
    whenever(stagingArea.get(cacheKey)).thenReturn(encodedImage)
    assertThat(closeableReference.underlyingReferenceTestOnly.refCountTestOnly).isEqualTo(2)
    assertThat(
            bufferedDiskCache
                .get(cacheKey, isCancelled)
                .result
                .byteBufferRef
                .underlyingReferenceTestOnly)
        .isSameAs(closeableReference.underlyingReferenceTestOnly)
  }

  @Test
  fun testFromStagingAreaLater() {
    val readTask = bufferedDiskCache.get(cacheKey, isCancelled)
    assertThat(readTask.isCompleted).isFalse()

    whenever(stagingArea.get(cacheKey)).thenReturn(encodedImage)
    readPriorityExecutor.runUntilIdle()

    val result = readTask.result
    assertThat(result).isSameAs(encodedImage)
    verify(fileCache, never()).getResource(eq(cacheKey))
    // Ref count should be equal to 3 (One for closeableReference, one that is cloned when
    // encodedImage is created and a third one that is cloned when the method getByteBufferRef is
    // called in EncodedImage).
    assertThat(result.byteBufferRef.underlyingReferenceTestOnly.refCountTestOnly).isEqualTo(3)
  }

  @Test
  fun testUnpins() {
    bufferedDiskCache.put(cacheKey, encodedImage)
    writePriorityExecutor.runUntilIdle()
    val argumentCaptor = argumentCaptor<EncodedImage>()
    verify(stagingArea).remove(eq(cacheKey), argumentCaptor.capture())
    val capturedEncodedImage = argumentCaptor.firstValue
    assertThat(capturedEncodedImage.underlyingReferenceTestOnly)
        .isSameAs(encodedImage.underlyingReferenceTestOnly)
  }

  @Test
  fun testContainsFromStagingAreaLater() {
    val readTask = bufferedDiskCache.contains(cacheKey)
    assertThat(readTask.isCompleted).isFalse()
    whenever(stagingArea.get(cacheKey)).thenReturn(encodedImage)
    readPriorityExecutor.runUntilIdle()
    verify(fileCache, never()).getResource(eq(cacheKey))
  }

  @Test
  fun testRemoveFromStagingArea() {
    bufferedDiskCache.remove(cacheKey)
    verify(stagingArea).remove(cacheKey)
  }

  @Test
  fun testClearFromStagingArea() {
    bufferedDiskCache.clearAll()
    verify(stagingArea).clearAll()
  }

  private fun isTaskCancelled(task: Task<*>): Boolean {
    return task.isCancelled || (task.isFaulted && task.error is CancellationException)
  }
}
