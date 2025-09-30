/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk

import com.facebook.binaryresource.BinaryResource
import com.facebook.cache.common.CacheErrorLogger
import com.facebook.cache.common.CacheEvent
import com.facebook.cache.common.CacheEventAssert
import com.facebook.cache.common.CacheEventListener
import com.facebook.cache.common.CacheEventListener.EvictionReason
import com.facebook.cache.common.CacheKey
import com.facebook.cache.common.CacheKeyUtil.getFirstResourceId
import com.facebook.cache.common.MultiCacheKey
import com.facebook.cache.common.SimpleCacheKey
import com.facebook.cache.common.WriterCallback
import com.facebook.cache.common.WriterCallbacks
import com.facebook.common.disk.DiskTrimmableRegistry
import com.facebook.common.internal.ByteStreams
import com.facebook.common.internal.Supplier
import com.facebook.common.internal.Suppliers
import com.facebook.common.time.SystemClock
import com.facebook.imagepipeline.testing.FakeClock
import com.facebook.imagepipeline.testing.TestExecutorService
import java.io.File
import java.io.IOException
import java.io.OutputStream
import java.io.RandomAccessFile
import java.util.Locale
import java.util.concurrent.CyclicBarrier
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.InOrder
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Test for [DiskStorageCache] */
@RunWith(RobolectricTestRunner::class)
class DiskStorageCacheTest {
  private var mCacheDirectory: File? = null
  private var mStorage: DiskStorage? = null
  private var mCache: DiskStorageCache? = null
  private var mDiskTrimmableRegistry: DiskTrimmableRegistry? = null
  private var mCacheEventListener: CacheEventListener? = null
  private var mCacheEventListenerInOrder: InOrder? = null
  private var mClock: SystemClock? = null
  private var mBackgroundExecutor: TestExecutorService? = null

  @Before
  fun setUp() {
    mockedSystemClock = Mockito.mockStatic<SystemClock?>(SystemClock::class.java)
    mClock = Mockito.mock<SystemClock>(SystemClock::class.java)
    mockedSystemClock!!
        .`when`<Any?>(MockedStatic.Verification { SystemClock.get() })
        .thenReturn(mClock)
    mBackgroundExecutor = TestExecutorService(FakeClock())
    mDiskTrimmableRegistry = Mockito.mock<DiskTrimmableRegistry?>(DiskTrimmableRegistry::class.java)
    mCacheEventListener = Mockito.mock<CacheEventListener>(CacheEventListener::class.java)
    mCacheEventListenerInOrder = Mockito.inOrder(mCacheEventListener)

    // we know the directory will be this
    mCacheDirectory = File(RuntimeEnvironment.application.getCacheDir(), CACHE_TYPE)
    mCacheDirectory!!.mkdirs()
    if (!mCacheDirectory!!.exists()) {
      throw RuntimeException(
          String.format(
              null as Locale?,
              "Cannot create cache dir: %s: directory %s",
              mCacheDirectory!!.getAbsolutePath(),
              if (mCacheDirectory!!.exists()) "already exists" else "does not exist",
          )
      )
    }
    mStorage = createDiskStorage(TESTCACHE_VERSION_START_OF_VERSIONING)
    mCache = createDiskCache(mStorage!!, false)
    mCache!!.clearAll()
    Mockito.reset<CacheEventListener?>(mCacheEventListener)
    Mockito.verify<DiskTrimmableRegistry?>(mDiskTrimmableRegistry).registerDiskTrimmable(mCache!!)
  }

  private var mockedSystemClock: MockedStatic<SystemClock?>? = null

  private fun createDiskCache(
      diskStorage: DiskStorage,
      indexPopulateAtStartupEnabled: Boolean,
  ): DiskStorageCache {
    val diskStorageCacheParams =
        DiskStorageCache.Params(0, FILE_CACHE_MAX_SIZE_LOW_LIMIT, FILE_CACHE_MAX_SIZE_HIGH_LIMIT)

    return DiskStorageCache(
        diskStorage,
        DefaultEntryEvictionComparatorSupplier(),
        diskStorageCacheParams,
        DuplicatingCacheEventListener(mCacheEventListener!!),
        Mockito.mock<CacheErrorLogger?>(CacheErrorLogger::class.java),
        mDiskTrimmableRegistry,
        mBackgroundExecutor!!,
        indexPopulateAtStartupEnabled,
    )
  }

  @After
  fun tearDownStaticMocks() {
    mockedSystemClock!!.close()
  }

  @Test
  @Throws(Exception::class)
  fun testCacheEventListener() {
    // 1. Add first cache file
    val key1: CacheKey = SimpleCacheKey("foo")
    val value1Size = 101
    val value1 = ByteArray(value1Size)
    value1[80] = 'c'.code.toByte() // just so it's not all zeros for the equality test below.
    val resource1 = mCache!!.insert(key1, WriterCallbacks.from(value1))

    verifyListenerOnWriteAttempt(key1)
    val resourceId1 = verifyListenerOnWriteSuccessAndGetResourceId(key1, value1Size.toLong())

    val resource1Again = mCache!!.getResource(key1)
    Assert.assertEquals(resource1, resource1Again)
    verifyListenerOnHit(key1, resourceId1)

    val resource1Again2 = mCache!!.getResource(key1)
    Assert.assertEquals(resource1, resource1Again2)
    verifyListenerOnHit(key1, resourceId1)

    val missingKey = SimpleCacheKey("nonexistent_key")
    val res2 = mCache!!.getResource(missingKey)
    Assert.assertNull(res2)
    verifyListenerOnMiss(missingKey)

    mCache!!.clearAll()
    Mockito.verify<CacheEventListener?>(mCacheEventListener).onCleared()

    Mockito.verifyNoMoreInteractions(mCacheEventListener)
  }

  @Throws(IOException::class)
  private fun getResource(storage: DiskStorage, key: CacheKey): BinaryResource? {
    return storage.getResource(getFirstResourceId(key), key)
  }

  @Throws(IOException::class)
  private fun getResource(key: CacheKey): BinaryResource? {
    return mStorage!!.getResource(getFirstResourceId(key), key)
  }

  @Throws(IOException::class)
  private fun getContents(resource: BinaryResource): ByteArray? {
    return ByteStreams.toByteArray(resource.openStream())
  }

  /**
   * Tests size based file eviction of cache files. Also tests that unexpected files (which are not
   * in the format expected by the cache) do not count towards the cache size, and are also evicted
   * during both evictions (LRU and Old).
   *
   * @throws Exception
   */
  @Test
  @Throws(Exception::class)
  fun testCacheFile() {
    if (!mCacheDirectory!!.exists() && !mCacheDirectory!!.mkdirs()) {
      throw RuntimeException("Cannot create cache dir")
    }
    // Write non-cache, non-lru file in the cache directory
    val unexpected1 = File(mCacheDirectory, "unexpected1")
    val rf1 = RandomAccessFile(unexpected1, "rw")
    rf1.setLength(110)
    // Touch the non-cache, non-lru file, and assert that it succeeds.
    Mockito.`when`<Long?>(mClock!!.now()).thenReturn(TimeUnit.HOURS.toMillis(1))
    Assert.assertTrue(unexpected1.setLastModified(mClock!!.now()))

    // 1. Add first cache file
    val key1: CacheKey = SimpleCacheKey("foo")
    val value1 = ByteArray(101)
    value1[80] = 'c'.code.toByte() // just so it's not all zeros for the equality test below.
    mCache!!.insert(key1, WriterCallbacks.from(value1))

    // verify resource
    Assert.assertArrayEquals(value1, getContents(getResource(key1)!!))

    // 1. Touch the LRU file, and assert that it succeeds.
    Mockito.`when`<Long?>(mClock!!.now()).thenReturn(TimeUnit.HOURS.toMillis(2))
    Assert.assertTrue(mCache!!.probe(key1))

    // The cache size should be the size of the first file only
    // The unexpected files should not count towards size
    Assert.assertTrue(mCache!!.getSize() == 101L)

    // Write another non-cache, non-lru file in the cache directory
    val unexpected2 = File(mCacheDirectory, "unexpected2")
    val rf2 = RandomAccessFile(unexpected2, "rw")
    rf2.setLength(120)
    // Touch the non-cache, non-lru file, and assert that it succeeds.
    Mockito.`when`<Long?>(mClock!!.now()).thenReturn(TimeUnit.HOURS.toMillis(3))
    Assert.assertTrue(unexpected2.setLastModified(mClock!!.now()))

    // 2. Add second cache file
    val key2: CacheKey = SimpleCacheKey("bar")
    val value2 = ByteArray(102)
    value2[80] = 'd'.code.toByte() // just so it's not all zeros for the equality test below.
    mCache!!.insert(key2, WriterCallbacks.from(value2))
    // 2. Touch the LRU file, and assert that it succeeds.
    Mockito.`when`<Long?>(mClock!!.now()).thenReturn(TimeUnit.HOURS.toMillis(4))
    Assert.assertTrue(mCache!!.probe(key2))

    // The cache size should be the size of the first + second cache files
    // The unexpected files should not count towards size
    Assert.assertTrue(mCache!!.getSize() == 203L)

    // At this point, the filecache size has exceeded
    // FILE_CACHE_MAX_SIZE_HIGH_LIMIT. However, eviction will be triggered
    // only when the next value will be inserted (to be more particular,
    // before the next value is inserted).

    // 3. Add third cache file
    val key3: CacheKey = SimpleCacheKey("foobar")
    val value3 = ByteArray(103)
    value3[80] = 'e'.code.toByte() // just so it's not all zeros for the equality test below.
    mCache!!.insert(key3, WriterCallbacks.from(value3))

    // At this point, the first file should have been evicted. Only the
    // files associated with the second and third entries should be in cache.

    // 1. Verify that the first cache, lru files are deleted
    Assert.assertNull(getResource(key1))

    // Verify the first unexpected file is deleted, but that eviction stops
    // before the second unexpected file
    Assert.assertFalse(unexpected1.exists())
    Assert.assertFalse(unexpected2.exists())

    // 2. Verify the second cache, lru files exist
    Assert.assertArrayEquals(value2, getContents(getResource(key2)!!))

    // 3. Verify that cache, lru files for third entry still exists
    Assert.assertArrayEquals(value3, getContents(getResource(key3)!!))

    // The cache size should be the size of the second + third files
    Assert.assertTrue(
        String.format(Locale.US, "Expected cache size of %d but is %d", 205, mCache!!.getSize()),
        mCache!!.getSize() == 205L,
    )

    // Write another non-cache, non-lru file in the cache directory
    val unexpected3 = File(mCacheDirectory, "unexpected3")
    val rf3 = RandomAccessFile(unexpected3, "rw")
    rf3.setLength(120)
    Assert.assertTrue(unexpected3.exists())
    // After a clear, cache file size should be uninitialized (-1)
    mCache!!.clearAll()
    Assert.assertEquals(-1, mCache!!.getSize())
    Assert.assertFalse(unexpected3.exists())
    Assert.assertNull(getResource(key2))
    Assert.assertNull(getResource(key3))
  }

  @Test
  @Throws(Exception::class)
  fun testWithMultiCacheKeys() {
    val insertKey1: CacheKey = SimpleCacheKey("foo")
    val value1 = ByteArray(101)
    value1[50] = 'a'.code.toByte() // just so it's not all zeros for the equality test below.
    mCache!!.insert(insertKey1, WriterCallbacks.from(value1))

    val keys1: MutableList<CacheKey?> = ArrayList<CacheKey?>(2)
    keys1.add(SimpleCacheKey("bar"))
    keys1.add(SimpleCacheKey("foo"))
    val matchingMultiKey = MultiCacheKey(keys1)
    Assert.assertArrayEquals(value1, getContents(mCache!!.getResource(matchingMultiKey)!!))

    val keys2: MutableList<CacheKey?> = ArrayList<CacheKey?>(2)
    keys2.add(SimpleCacheKey("one"))
    keys2.add(SimpleCacheKey("two"))
    val insertKey2 = MultiCacheKey(keys2)
    val value2 = ByteArray(101)
    value1[50] = 'b'.code.toByte() // just so it's not all zeros for the equality test below.
    mCache!!.insert(insertKey2, WriterCallbacks.from(value2))

    val matchingSimpleKey: CacheKey = SimpleCacheKey("one")
    Assert.assertArrayEquals(value2, getContents(mCache!!.getResource(matchingSimpleKey)!!))
  }

  @Test
  @Throws(IOException::class)
  fun testCacheFileWithIOException() {
    val key1: CacheKey = SimpleCacheKey("aaa")

    // Before inserting, make sure files not exist.
    val resource1 = getResource(key1)
    Assert.assertNull(resource1)

    // 1. Should not create cache files if IOException happens in the middle.
    val writeException = IOException()
    try {
      mCache!!.insert(
          key1,
          object : WriterCallback {
            @Throws(IOException::class)
            override fun write(os: OutputStream) {
              throw writeException
            }
          },
      )
      Assert.fail()
    } catch (e: IOException) {
      Assert.assertNull(getResource(key1))
    }

    verifyListenerOnWriteAttempt(key1)
    verifyListenerOnWriteException(key1, writeException)

    // 2. Test a read failure from DiskStorage
    val key2: CacheKey = SimpleCacheKey("bbb")
    val value2Size = 42
    val value2 = ByteArray(value2Size)
    value2[25] = 'b'.code.toByte()
    mCache!!.insert(key2, WriterCallbacks.from(value2))

    verifyListenerOnWriteAttempt(key2)
    val resourceId2 = verifyListenerOnWriteSuccessAndGetResourceId(key2, value2Size.toLong())

    (mStorage as DiskStorageWithReadFailures).setPoisonResourceId(resourceId2)

    Assert.assertNull(mCache!!.getResource(key2))
    verifyListenerOnReadException(key2, DiskStorageWithReadFailures.Companion.POISON_EXCEPTION)

    Assert.assertFalse(mCache!!.probe(key2))
    verifyListenerOnReadException(key2, DiskStorageWithReadFailures.Companion.POISON_EXCEPTION)

    Mockito.verifyNoMoreInteractions(mCacheEventListener)
  }

  @Test
  @Throws(IOException::class, NoSuchFieldException::class, IllegalAccessException::class)
  fun testCleanOldCache() {
    val cacheExpirationMs = TimeUnit.DAYS.toMillis(5)
    val key1: CacheKey = SimpleCacheKey("aaa")
    val value1Size = 41
    val value1 = ByteArray(value1Size)
    value1[25] = 'a'.code.toByte()
    mCache!!.insert(key1, WriterCallbacks.from(value1))

    val resourceId1 = verifyListenerOnWriteSuccessAndGetResourceId(key1, value1Size.toLong())

    val key2: CacheKey = SimpleCacheKey("bbb")
    val value2Size = 42
    val value2 = ByteArray(value2Size)
    value2[25] = 'b'.code.toByte()
    mCache!!.insert(key2, WriterCallbacks.from(value2))

    val resourceId2 = verifyListenerOnWriteSuccessAndGetResourceId(key2, value2Size.toLong())

    // Increment clock by default expiration time + 1 day
    Mockito.`when`<Long?>(mClock!!.now()).thenReturn(cacheExpirationMs + TimeUnit.DAYS.toMillis(1))

    val key3: CacheKey = SimpleCacheKey("ccc")
    val value3Size = 43
    val value3 = ByteArray(value3Size)
    value3[25] = 'c'.code.toByte()
    mCache!!.insert(key3, WriterCallbacks.from(value3))
    val valueAge3 = TimeUnit.HOURS.toMillis(1)
    Mockito.`when`<Long?>(mClock!!.now())
        .thenReturn(cacheExpirationMs + TimeUnit.DAYS.toMillis(1) + valueAge3)

    val oldestEntry = mCache!!.clearOldEntries(cacheExpirationMs)
    Assert.assertEquals(valueAge3, oldestEntry)

    Assert.assertArrayEquals(value3, getContents(getResource(key3)!!))
    Assert.assertNull(getResource(key1))
    Assert.assertNull(getResource(key2))

    val resourceIds = arrayOf<String?>(resourceId1, resourceId2)
    val itemSizes = longArrayOf(value1Size.toLong(), value2Size.toLong())
    val cacheSizeBeforeEviction = (value1Size + value2Size + value3Size).toLong()
    verifyListenerOnEviction(
        resourceIds,
        itemSizes,
        EvictionReason.CONTENT_STALE,
        cacheSizeBeforeEviction,
    )
  }

  @Test
  @Throws(IOException::class)
  fun testCleanOldCacheNoEntriesRemaining() {
    val cacheExpirationMs = TimeUnit.DAYS.toMillis(5)
    val key1: CacheKey = SimpleCacheKey("aaa")
    val value1 = ByteArray(41)
    mCache!!.insert(key1, WriterCallbacks.from(value1))

    val key2: CacheKey = SimpleCacheKey("bbb")
    val value2 = ByteArray(42)
    mCache!!.insert(key2, WriterCallbacks.from(value2))

    // Increment clock by default expiration time + 1 day
    Mockito.`when`<Long?>(mClock!!.now()).thenReturn(cacheExpirationMs + TimeUnit.DAYS.toMillis(1))

    val oldestEntry = mCache!!.clearOldEntries(cacheExpirationMs)
    Assert.assertEquals(0L, oldestEntry)
  }

  /**
   * Test to make sure that the same item stored with two different versions of the cache will be
   * stored with two different file names.
   *
   * @throws UnsupportedEncodingException
   */
  @Test
  @Throws(IOException::class)
  fun testVersioning() {
    // Define data that will be written to cache

    val key: CacheKey = SimpleCacheKey("version_test")
    val value = ByteArray(32)
    value[0] = 'v'.code.toByte()

    // Set up cache with version == 1
    val storage1: DiskStorage = createDiskStorage(TESTCACHE_CURRENT_VERSION)
    val cache1 = createDiskCache(storage1, false)

    // Write test data to cache 1
    cache1.insert(key, WriterCallbacks.from(value))

    // Get cached file
    val resource1 = getResource(storage1, key)
    Assert.assertNotNull(resource1)

    // Set up cache with version == 2
    val storageSupplier2: DiskStorage = createDiskStorage(TESTCACHE_NEXT_VERSION)
    val cache2 = createDiskCache(storageSupplier2, false)

    // Write test data to cache 2
    cache2.insert(key, WriterCallbacks.from(value))

    // Get cached file
    val resource2 = getResource(storageSupplier2, key)
    Assert.assertNotNull(resource2)

    // Make sure filenames of the two file are different
    Assert.assertFalse(resource2 == resource1)
  }

  /** Verify that multiple threads can write to the cache at the same time. */
  @Test
  @Throws(Exception::class)
  fun testConcurrency() {
    val barrier = CyclicBarrier(3)
    val writerCallback: WriterCallback =
        object : WriterCallback {
          @Throws(IOException::class)
          override fun write(os: OutputStream) {
            try {
              // Both threads will need to hit this barrier. If writing is serialized,
              // the second thread will never reach here as the first will hold
              // the write lock forever.
              barrier.await(10, TimeUnit.SECONDS)
            } catch (e: Exception) {
              throw RuntimeException(e)
            }
          }
        }
    val key1: CacheKey = SimpleCacheKey("concurrent1")
    val key2: CacheKey = SimpleCacheKey("concurrent2")
    val t1 = runInsertionInSeparateThread(key1, writerCallback)
    val t2 = runInsertionInSeparateThread(key2, writerCallback)
    barrier.await(10, TimeUnit.SECONDS)
    t1.join(1000)
    t2.join(1000)
  }

  @Test
  @Throws(Exception::class)
  fun testIsEnabled() {
    val storageMock = Mockito.mock<DiskStorage>(DiskStorage::class.java)
    Mockito.`when`<Boolean?>(storageMock.isEnabled()).thenReturn(true).thenReturn(false)

    val cache = createDiskCache(storageMock, false)
    Assert.assertTrue(cache.isEnabled())
    Assert.assertFalse(cache.isEnabled())
  }

  private fun runInsertionInSeparateThread(key: CacheKey, callback: WriterCallback): Thread {
    val runnable: Runnable =
        object : Runnable {
          override fun run() {
            try {
              mCache!!.insert(key, callback)
            } catch (e: IOException) {
              Assert.fail()
            }
          }
        }
    val thread = Thread(runnable)
    thread.setDaemon(true)
    thread.start()
    return thread
  }

  @Test
  @Throws(Exception::class)
  fun testInsertionInIndex() {
    val key = putOneThingInCache()
    Assert.assertTrue(mCache!!.hasKeySync(key))
    Assert.assertTrue(mCache!!.hasKey(key))
  }

  @Test
  fun testDoesntHaveKey() {
    val key: CacheKey = SimpleCacheKey("foo")
    Assert.assertFalse(mCache!!.hasKeySync(key))
    Assert.assertFalse(mCache!!.hasKey(key))
  }

  @Test
  @Throws(Exception::class)
  fun testHasKeyWithoutPopulateAtStartupWithoutAwaitingIndex() {
    val key = putOneThingInCache()
    // A new cache object in the same directory. Equivalent to a process restart.
    // Index may not yet updated.
    val cache2 = createDiskCache(mStorage!!, false)
    Assert.assertTrue(cache2.isIndexReady)
    Assert.assertFalse(cache2.hasKeySync(key))
    Assert.assertTrue(cache2.hasKey(key))
    // hasKey() adds item to the index
    Assert.assertTrue(cache2.hasKeySync(key))
  }

  @Test
  @Throws(Exception::class)
  fun testHasKeyWithoutPopulateAtStartupWithAwaitingIndex() {
    // A new cache object in the same directory. Equivalent to a process restart.
    // Index may not yet updated.
    val cache2 = createDiskCache(mStorage!!, false)
    val key = putOneThingInCache()
    // Wait for index populated in cache before use of cache
    cache2.awaitIndex()
    Assert.assertTrue(cache2.isIndexReady)
    Assert.assertTrue(cache2.hasKey(key))
    Assert.assertTrue(cache2.hasKeySync(key))
  }

  @Test
  @Throws(Exception::class)
  fun testHasKeyWithPopulateAtStartupWithAwaitingIndex() {
    val cache2 = createDiskCache(mStorage!!, false)
    // A new cache object in the same directory. Equivalent to a process restart.
    // Index should be updated.
    val key = putOneThingInCache(cache2)
    // Wait for index populated in cache before use of cache
    cache2.awaitIndex()
    Assert.assertTrue(cache2.isIndexReady)
    Assert.assertTrue(cache2.hasKeySync(key))
    Assert.assertTrue(cache2.hasKey(key))
  }

  @Test
  @Throws(Exception::class)
  fun testHasKeyWithPopulateAtStartupWithoutAwaitingIndex() {
    val cache2 = createDiskCache(mStorage!!, true)
    val key = putOneThingInCache(cache2)
    // A new cache object in the same directory. Equivalent to a process restart.
    // Index may not yet updated.
    Assert.assertFalse(cache2.isIndexReady)
    Assert.assertTrue(cache2.hasKey(key))
    Assert.assertTrue(cache2.hasKeySync(key))
  }

  @Test
  @Throws(Exception::class)
  fun testGetResourceWithoutAwaitingIndex() {
    val key = putOneThingInCache()
    // A new cache object in the same directory. Equivalent to a process restart.
    // Index may not yet updated.
    val cache2 = createDiskCache(mStorage!!, false)
    Assert.assertNotNull(cache2.getResource(key))
  }

  @Test
  fun testIndexIsImmediatelyReadyIfIndexAtStartupIsOff() {
    val cache = createDiskCache(mStorage!!, false)

    Assertions.assertThat(cache.isIndexReady).isTrue()
  }

  @Test
  fun testIndexIsNotImmediatelyReadyIfIndexAtStartupIsOff() {
    val cache = createDiskCache(mStorage!!, true)

    Assertions.assertThat(cache.isIndexReady).isFalse()
  }

  @Test
  fun testIndexIsReadyIfIndexAtStartupIsOnAndTheBackgroundExecutorHasRun() {
    val cache = createDiskCache(mStorage!!, true)

    mBackgroundExecutor!!.runUntilIdle()

    Assertions.assertThat(cache.isIndexReady).isTrue()
  }

  @Test
  @Throws(Exception::class)
  fun testClearIndex() {
    val key = putOneThingInCache()
    mCache!!.clearAll()
    Assert.assertFalse(mCache!!.hasKeySync(key))
    Assert.assertFalse(mCache!!.hasKey(key))
  }

  @Test
  @Throws(Exception::class)
  fun testRemoveFileClearsIndex() {
    val key = putOneThingInCache()
    mStorage!!.clearAll()
    Assert.assertNull(mCache!!.getResource(key))
    Assert.assertFalse(mCache!!.hasKeySync(key))
  }

  @Test
  @Throws(Exception::class)
  fun testSizeEvictionClearsIndex() {
    Mockito.`when`<Long?>(mClock!!.now())
        .thenReturn(TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS))
    val key1 = putOneThingInCache()
    val key2: CacheKey = SimpleCacheKey("bar")
    val key3: CacheKey = SimpleCacheKey("duck")
    val value2 = ByteArray(FILE_CACHE_MAX_SIZE_HIGH_LIMIT.toInt())
    value2[80] = 'c'.code.toByte()
    val callback = WriterCallbacks.from(value2)
    Mockito.`when`<Long?>(mClock!!.now())
        .thenReturn(TimeUnit.MILLISECONDS.convert(2, TimeUnit.DAYS))
    mCache!!.insert(key2, callback)
    // now over limit. Next write will evict key1
    Mockito.`when`<Long?>(mClock!!.now())
        .thenReturn(TimeUnit.MILLISECONDS.convert(3, TimeUnit.DAYS))
    mCache!!.insert(key3, callback)
    Assert.assertFalse(mCache!!.hasKeySync(key1))
    Assert.assertFalse(mCache!!.hasKey(key1))
    Assert.assertTrue(mCache!!.hasKeySync(key3))
    Assert.assertTrue(mCache!!.hasKey(key3))
  }

  @Test
  @Throws(Exception::class)
  fun testTimeEvictionClearsIndex() {
    Mockito.`when`<Long?>(mClock!!.now()).thenReturn(5L)
    val key = putOneThingInCache()
    Mockito.`when`<Long?>(mClock!!.now()).thenReturn(10L)
    mCache!!.clearOldEntries(4)
    Assert.assertFalse(mCache!!.hasKeySync(key))
    Assert.assertFalse(mCache!!.hasKey(key))
  }

  @Throws(IOException::class)
  private fun putOneThingInCache(cache: DiskStorageCache = mCache!!): CacheKey {
    val key: CacheKey = SimpleCacheKey("foo")
    val value1 = ByteArray(101)
    value1[80] = 'c'.code.toByte()
    cache.insert(key, WriterCallbacks.from(value1))
    return key
  }

  private fun verifyListenerOnHit(key: CacheKey?, resourceId: String?) {
    val cacheEventCaptor = ArgumentCaptor.forClass<CacheEvent?, CacheEvent?>(CacheEvent::class.java)
    mCacheEventListenerInOrder!!
        .verify<CacheEventListener?>(mCacheEventListener)
        .onHit(cacheEventCaptor.capture())

    for (event in cacheEventCaptor.getAllValues()) {
      CacheEventAssert.assertThat(event).isNotNull().hasCacheKey(key).hasResourceId(resourceId)
    }
  }

  private fun verifyListenerOnMiss(key: CacheKey?) {
    val cacheEventCaptor = ArgumentCaptor.forClass<CacheEvent?, CacheEvent?>(CacheEvent::class.java)
    mCacheEventListenerInOrder!!
        .verify<CacheEventListener?>(mCacheEventListener)
        .onMiss(cacheEventCaptor.capture())

    for (event in cacheEventCaptor.getAllValues()) {
      CacheEventAssert.assertThat(event).isNotNull().hasCacheKey(key)
    }
  }

  private fun verifyListenerOnWriteAttempt(key: CacheKey?) {
    val cacheEventCaptor = ArgumentCaptor.forClass<CacheEvent?, CacheEvent?>(CacheEvent::class.java)
    mCacheEventListenerInOrder!!
        .verify<CacheEventListener?>(mCacheEventListener)
        .onWriteAttempt(cacheEventCaptor.capture())

    CacheEventAssert.assertThat(cacheEventCaptor.getValue()).isNotNull().hasCacheKey(key)
  }

  private fun verifyListenerOnWriteSuccessAndGetResourceId(
      key: CacheKey?,
      itemSize: Long,
  ): String? {
    val cacheEventCaptor = ArgumentCaptor.forClass<CacheEvent?, CacheEvent?>(CacheEvent::class.java)
    mCacheEventListenerInOrder!!
        .verify<CacheEventListener?>(mCacheEventListener)
        .onWriteSuccess(cacheEventCaptor.capture())

    val cacheEvent = cacheEventCaptor.getValue()
    CacheEventAssert.assertThat(cacheEvent)
        .isNotNull()
        .hasCacheKey(key)
        .hasItemSize(itemSize)
        .hasResourceIdSet()

    return cacheEvent.getResourceId()
  }

  private fun verifyListenerOnWriteException(key: CacheKey?, exception: IOException?) {
    val cacheEventCaptor = ArgumentCaptor.forClass<CacheEvent?, CacheEvent?>(CacheEvent::class.java)
    mCacheEventListenerInOrder!!
        .verify<CacheEventListener?>(mCacheEventListener)
        .onWriteException(cacheEventCaptor.capture())

    CacheEventAssert.assertThat(cacheEventCaptor.getValue())
        .isNotNull()
        .hasCacheKey(key)
        .hasException(exception)
  }

  private fun verifyListenerOnReadException(key: CacheKey?, exception: IOException?) {
    val cacheEventCaptor = ArgumentCaptor.forClass<CacheEvent?, CacheEvent?>(CacheEvent::class.java)
    mCacheEventListenerInOrder!!
        .verify<CacheEventListener?>(mCacheEventListener)
        .onReadException(cacheEventCaptor.capture())

    CacheEventAssert.assertThat(cacheEventCaptor.getValue())
        .isNotNull()
        .hasCacheKey(key)
        .hasException(exception)
  }

  private fun verifyListenerOnEviction(
      resourceIds: Array<String?>,
      itemSizes: LongArray,
      reason: EvictionReason?,
      cacheSizeBeforeEviction: Long,
  ) {
    val numberItems = resourceIds.size
    val cacheEventCaptor = ArgumentCaptor.forClass<CacheEvent?, CacheEvent?>(CacheEvent::class.java)
    mCacheEventListenerInOrder!!
        .verify<CacheEventListener?>(mCacheEventListener, Mockito.times(numberItems))
        .onEviction(cacheEventCaptor.capture())

    val found = BooleanArray(numberItems)
    var runningCacheSize = cacheSizeBeforeEviction

    // The eviction order is unknown so make allowances for them coming in different orders
    for (event in cacheEventCaptor.getAllValues()) {
      CacheEventAssert.assertThat(event).isNotNull()

      for (i in 0..<numberItems) {
        if (!found[i] && resourceIds[i] == event.getResourceId()) {
          found[i] = true
          CacheEventAssert.assertThat(event).hasItemSize(itemSizes[i]).hasEvictionReason(reason)
        }
      }

      runningCacheSize -= event.getItemSize()
      CacheEventAssert.assertThat(event).hasCacheSize(runningCacheSize)
    }

    // Ensure all resources were found
    for (i in 0..<numberItems) {
      Assert.assertTrue(
          String.format("Expected eviction of resource %s but wasn't evicted", resourceIds[i]),
          found[i],
      )
    }
  }

  private class DiskStorageWithReadFailures(
      version: Int,
      baseDirectoryPathSupplier: Supplier<File?>,
      baseDirectoryName: String,
      cacheErrorLogger: CacheErrorLogger,
  ) :
      DynamicDefaultDiskStorage(
          version,
          baseDirectoryPathSupplier,
          baseDirectoryName,
          cacheErrorLogger,
      ) {
    private var poisonResourceId: String? = null

    fun setPoisonResourceId(poisonResourceId: String?) {
      this.poisonResourceId = poisonResourceId
    }

    @Throws(IOException::class)
    override fun getResource(resourceId: String, debugInfo: Any): BinaryResource? {
      if (resourceId == this.poisonResourceId) {
        throw POISON_EXCEPTION
      }
      return get().getResource(resourceId, debugInfo!!)
    }

    @Throws(IOException::class)
    override fun touch(resourceId: String, debugInfo: Any): Boolean {
      if (resourceId == this.poisonResourceId) {
        throw POISON_EXCEPTION
      }
      return super.touch(resourceId, debugInfo!!)
    }

    companion object {
      val POISON_EXCEPTION: IOException = IOException("Poisoned resource requested")
    }
  }

  /**
   * CacheEventListener implementation which copies the data from each event into a new instance to
   * work-around the recycling of the original event and forwards the copy so that assertions can be
   * made afterwards.
   */
  private class DuplicatingCacheEventListener(private val recipientListener: CacheEventListener) :
      CacheEventListener {
    override fun onHit(cacheEvent: CacheEvent) {
      recipientListener.onHit(Companion.duplicateEvent(cacheEvent!!))
    }

    override fun onMiss(cacheEvent: CacheEvent) {
      recipientListener.onMiss(Companion.duplicateEvent(cacheEvent!!))
    }

    override fun onWriteAttempt(cacheEvent: CacheEvent) {
      recipientListener.onWriteAttempt(Companion.duplicateEvent(cacheEvent!!))
    }

    override fun onWriteSuccess(cacheEvent: CacheEvent) {
      recipientListener.onWriteSuccess(Companion.duplicateEvent(cacheEvent!!))
    }

    override fun onReadException(cacheEvent: CacheEvent) {
      recipientListener.onReadException(Companion.duplicateEvent(cacheEvent!!))
    }

    override fun onWriteException(cacheEvent: CacheEvent) {
      recipientListener.onWriteException(Companion.duplicateEvent(cacheEvent!!))
    }

    override fun onEviction(cacheEvent: CacheEvent) {
      recipientListener.onEviction(Companion.duplicateEvent(cacheEvent!!))
    }

    override fun onCleared() {
      recipientListener.onCleared()
    }

    companion object {
      private fun duplicateEvent(cacheEvent: CacheEvent): CacheEvent {
        val copyEvent = SettableCacheEvent.obtain()
        copyEvent.setCacheKey(cacheEvent.getCacheKey())
        copyEvent.setCacheLimit(cacheEvent.getCacheLimit())
        copyEvent.setCacheSize(cacheEvent.getCacheSize())
        copyEvent.setEvictionReason(cacheEvent.getEvictionReason())
        copyEvent.setException(cacheEvent.getException())
        copyEvent.setItemSize(cacheEvent.getItemSize())
        copyEvent.setResourceId(cacheEvent.getResourceId())
        return copyEvent
      }
    }
  }

  companion object {
    private const val CACHE_TYPE = "media_test"

    private const val TESTCACHE_VERSION_START_OF_VERSIONING = 1
    private val TESTCACHE_CURRENT_VERSION: Int = TESTCACHE_VERSION_START_OF_VERSIONING
    private val TESTCACHE_NEXT_VERSION: Int = TESTCACHE_CURRENT_VERSION + 1

    // The threshold (in bytes) for the size of file cache
    private const val FILE_CACHE_MAX_SIZE_HIGH_LIMIT: Long = 200
    private const val FILE_CACHE_MAX_SIZE_LOW_LIMIT: Long = 200

    private fun createDiskStorage(version: Int): DiskStorage {
      return DiskStorageWithReadFailures(
          version,
          Suppliers.of<File?>(RuntimeEnvironment.application.getApplicationContext().getCacheDir()),
          CACHE_TYPE,
          Mockito.mock<CacheErrorLogger?>(CacheErrorLogger::class.java),
      )
    }
  }
}
