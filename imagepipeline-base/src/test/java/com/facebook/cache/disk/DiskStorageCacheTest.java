/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk;

import static org.fest.assertions.api.Assertions.assertThat;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;
import com.facebook.binaryresource.BinaryResource;
import com.facebook.cache.common.CacheErrorLogger;
import com.facebook.cache.common.CacheEvent;
import com.facebook.cache.common.CacheEventAssert;
import com.facebook.cache.common.CacheEventListener;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.CacheKeyUtil;
import com.facebook.cache.common.MultiCacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.cache.common.WriterCallback;
import com.facebook.cache.common.WriterCallbacks;
import com.facebook.common.disk.DiskTrimmableRegistry;
import com.facebook.common.internal.ByteStreams;
import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.Suppliers;
import com.facebook.common.time.SystemClock;
import com.facebook.imagepipeline.testing.FakeClock;
import com.facebook.imagepipeline.testing.TestExecutorService;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** Test for {@link DiskStorageCache} */
@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({"org.mockito.*", "org.robolectric.*", "androidx.*", "android.*"})
@PrepareOnlyThisForTest({SystemClock.class})
public class DiskStorageCacheTest {

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  private static final String CACHE_TYPE = "media_test";

  private static final int TESTCACHE_VERSION_START_OF_VERSIONING = 1;
  private static final int TESTCACHE_CURRENT_VERSION = TESTCACHE_VERSION_START_OF_VERSIONING;
  private static final int TESTCACHE_NEXT_VERSION = TESTCACHE_CURRENT_VERSION + 1;

  private File mCacheDirectory;
  private DiskStorage mStorage;
  private DiskStorageCache mCache;
  private DiskTrimmableRegistry mDiskTrimmableRegistry;
  private CacheEventListener mCacheEventListener;
  private InOrder mCacheEventListenerInOrder;
  private SystemClock mClock;
  private TestExecutorService mBackgroundExecutor;

  @Before
  public void setUp() {
    mClock = mock(SystemClock.class);
    PowerMockito.mockStatic(SystemClock.class);
    PowerMockito.when(SystemClock.get()).thenReturn(mClock);
    mBackgroundExecutor = new TestExecutorService(new FakeClock());
    mDiskTrimmableRegistry = mock(DiskTrimmableRegistry.class);
    mCacheEventListener = mock(CacheEventListener.class);
    mCacheEventListenerInOrder = inOrder(mCacheEventListener);

    // we know the directory will be this
    mCacheDirectory = new File(RuntimeEnvironment.application.getCacheDir(), CACHE_TYPE);
    mCacheDirectory.mkdirs();
    if (!mCacheDirectory.exists()) {
      throw new RuntimeException(
          String.format(
              (Locale) null,
              "Cannot create cache dir: %s: directory %s",
              mCacheDirectory.getAbsolutePath(),
              mCacheDirectory.exists() ? "already exists" : "does not exist"));
    }
    mStorage = createDiskStorage(TESTCACHE_VERSION_START_OF_VERSIONING);
    mCache = createDiskCache(mStorage, false);
    mCache.clearAll();
    reset(mCacheEventListener);
    verify(mDiskTrimmableRegistry).registerDiskTrimmable(mCache);
  }

  // The threshold (in bytes) for the size of file cache
  private static final long FILE_CACHE_MAX_SIZE_HIGH_LIMIT = 200;
  private static final long FILE_CACHE_MAX_SIZE_LOW_LIMIT = 200;

  private static DiskStorage createDiskStorage(int version) {
    return new DiskStorageWithReadFailures(
        version,
        Suppliers.of(RuntimeEnvironment.application.getApplicationContext().getCacheDir()),
        CACHE_TYPE,
        mock(CacheErrorLogger.class));
  }

  private DiskStorageCache createDiskCache(
      DiskStorage diskStorage,
      boolean indexPopulateAtStartupEnabled) {
    DiskStorageCache.Params diskStorageCacheParams =
        new DiskStorageCache.Params(
            0,
            FILE_CACHE_MAX_SIZE_LOW_LIMIT,
            FILE_CACHE_MAX_SIZE_HIGH_LIMIT);

    Context context = RuntimeEnvironment.application.getApplicationContext();

    return new DiskStorageCache(
        diskStorage,
        new DefaultEntryEvictionComparatorSupplier(),
        diskStorageCacheParams,
        new DuplicatingCacheEventListener(mCacheEventListener),
        mock(CacheErrorLogger.class),
        mDiskTrimmableRegistry,
        context,
        mBackgroundExecutor,
        indexPopulateAtStartupEnabled);
  }

  @Test
  public void testCacheEventListener() throws Exception {
    // 1. Add first cache file
    CacheKey key1 = new SimpleCacheKey("foo");
    int value1Size = 101;
    byte[] value1 = new byte[value1Size];
    value1[80] = 'c'; // just so it's not all zeros for the equality test below.
    BinaryResource resource1 = mCache.insert(key1, WriterCallbacks.from(value1));

    verifyListenerOnWriteAttempt(key1);
    String resourceId1 = verifyListenerOnWriteSuccessAndGetResourceId(key1, value1Size);

    BinaryResource resource1Again = mCache.getResource(key1);
    assertEquals(resource1, resource1Again);
    verifyListenerOnHit(key1, resourceId1);

    BinaryResource resource1Again2 = mCache.getResource(key1);
    assertEquals(resource1, resource1Again2);
    verifyListenerOnHit(key1, resourceId1);

    SimpleCacheKey missingKey = new SimpleCacheKey("nonexistent_key");
    BinaryResource res2 = mCache.getResource(missingKey);
    assertNull(res2);
    verifyListenerOnMiss(missingKey);

    mCache.clearAll();
    verify(mCacheEventListener).onCleared();

    verifyNoMoreInteractions(mCacheEventListener);
  }

  private BinaryResource getResource(
      DiskStorage storage,
      final CacheKey key) throws IOException {
     return storage.getResource(CacheKeyUtil.getFirstResourceId(key), key);
  }

  private BinaryResource getResource(final CacheKey key) throws IOException {
    return mStorage.getResource(CacheKeyUtil.getFirstResourceId(key), key);
  }

  private byte[] getContents(BinaryResource resource) throws IOException {
    return ByteStreams.toByteArray(resource.openStream());
  }

  /**
   * Tests size based file eviction of cache files. Also tests that unexpected
   * files (which are not in the format expected by the cache) do not count
   * towards the cache size, and are also evicted during both evictions (LRU and Old).
   *
   * @throws Exception
   */
  @Test
  public void testCacheFile() throws Exception {
    if (!mCacheDirectory.exists() && !mCacheDirectory.mkdirs()) {
      throw new RuntimeException("Cannot create cache dir");
    }
    // Write non-cache, non-lru file in the cache directory
    File unexpected1 = new File(mCacheDirectory, "unexpected1");
    RandomAccessFile rf1 = new RandomAccessFile(unexpected1, "rw");
    rf1.setLength(110);
    // Touch the non-cache, non-lru file, and assert that it succeeds.
    when(mClock.now()).thenReturn(TimeUnit.HOURS.toMillis(1));
    assertTrue(unexpected1.setLastModified(mClock.now()));

    // 1. Add first cache file
    CacheKey key1 = new SimpleCacheKey("foo");
    byte[] value1 = new byte[101];
    value1[80] = 'c'; // just so it's not all zeros for the equality test below.
    mCache.insert(key1, WriterCallbacks.from(value1));

    // verify resource
    assertArrayEquals(value1, getContents(getResource(key1)));

    // 1. Touch the LRU file, and assert that it succeeds.
    when(mClock.now()).thenReturn(TimeUnit.HOURS.toMillis(2));
    assertTrue(mCache.probe(key1));

    // The cache size should be the size of the first file only
    // The unexpected files should not count towards size
    assertTrue(mCache.getSize() == 101);

    // Write another non-cache, non-lru file in the cache directory
    File unexpected2 = new File(mCacheDirectory, "unexpected2");
    RandomAccessFile rf2 = new RandomAccessFile(unexpected2, "rw");
    rf2.setLength(120);
    // Touch the non-cache, non-lru file, and assert that it succeeds.
    when(mClock.now()).thenReturn(TimeUnit.HOURS.toMillis(3));
    assertTrue(unexpected2.setLastModified(mClock.now()));

    // 2. Add second cache file
    CacheKey key2 = new SimpleCacheKey("bar");
    byte[] value2 = new byte[102];
    value2[80] = 'd'; // just so it's not all zeros for the equality test below.
    mCache.insert(key2, WriterCallbacks.from(value2));
    // 2. Touch the LRU file, and assert that it succeeds.
    when(mClock.now()).thenReturn(TimeUnit.HOURS.toMillis(4));
    assertTrue(mCache.probe(key2));

    // The cache size should be the size of the first + second cache files
    // The unexpected files should not count towards size
    assertTrue(mCache.getSize() == 203);

    // At this point, the filecache size has exceeded
    // FILE_CACHE_MAX_SIZE_HIGH_LIMIT. However, eviction will be triggered
    // only when the next value will be inserted (to be more particular,
    // before the next value is inserted).

    // 3. Add third cache file
    CacheKey key3 = new SimpleCacheKey("foobar");
    byte[] value3 = new byte[103];
    value3[80] = 'e'; // just so it's not all zeros for the equality test below.
    mCache.insert(key3, WriterCallbacks.from(value3));

    // At this point, the first file should have been evicted. Only the
    // files associated with the second and third entries should be in cache.

    // 1. Verify that the first cache, lru files are deleted
    assertNull(getResource(key1));

    // Verify the first unexpected file is deleted, but that eviction stops
    // before the second unexpected file
    assertFalse(unexpected1.exists());
    assertFalse(unexpected2.exists());

    // 2. Verify the second cache, lru files exist
    assertArrayEquals(value2, getContents(getResource(key2)));

    // 3. Verify that cache, lru files for third entry still exists
    assertArrayEquals(value3, getContents(getResource(key3)));

    // The cache size should be the size of the second + third files
    assertTrue(
        String.format(Locale.US, "Expected cache size of %d but is %d", 205, mCache.getSize()),
        mCache.getSize() == 205);

    // Write another non-cache, non-lru file in the cache directory
    File unexpected3 = new File(mCacheDirectory, "unexpected3");
    RandomAccessFile rf3 = new RandomAccessFile(unexpected3, "rw");
    rf3.setLength(120);
    assertTrue(unexpected3.exists());
    // After a clear, cache file size should be uninitialized (-1)
    mCache.clearAll();
    assertEquals(-1, mCache.getSize());
    assertFalse(unexpected3.exists());
    assertNull(getResource(key2));
    assertNull(getResource(key3));
  }

  @Test
  public void testWithMultiCacheKeys() throws Exception {
    CacheKey insertKey1 = new SimpleCacheKey("foo");
    byte[] value1 = new byte[101];
    value1[50] = 'a'; // just so it's not all zeros for the equality test below.
    mCache.insert(insertKey1, WriterCallbacks.from(value1));

    List<CacheKey> keys1 = new ArrayList<>(2);
    keys1.add(new SimpleCacheKey("bar"));
    keys1.add(new SimpleCacheKey("foo"));
    MultiCacheKey matchingMultiKey = new MultiCacheKey(keys1);
    assertArrayEquals(value1, getContents(mCache.getResource(matchingMultiKey)));

    List<CacheKey> keys2 = new ArrayList<>(2);
    keys2.add(new SimpleCacheKey("one"));
    keys2.add(new SimpleCacheKey("two"));
    MultiCacheKey insertKey2 = new MultiCacheKey(keys2);
    byte[] value2 = new byte[101];
    value1[50] = 'b'; // just so it's not all zeros for the equality test below.
    mCache.insert(insertKey2, WriterCallbacks.from(value2));

    CacheKey matchingSimpleKey = new SimpleCacheKey("one");
    assertArrayEquals(value2, getContents(mCache.getResource(matchingSimpleKey)));
  }

  @Test
  public void testCacheFileWithIOException() throws IOException {
    CacheKey key1 = new SimpleCacheKey("aaa");

    // Before inserting, make sure files not exist.
    final BinaryResource resource1 = getResource(key1);
    assertNull(resource1);

    // 1. Should not create cache files if IOException happens in the middle.
    final IOException writeException = new IOException();
    try {
      mCache.insert(
          key1, new WriterCallback() {
            @Override
            public void write(OutputStream os) throws IOException {
              throw writeException;
            }
          });
      fail();
    } catch (IOException e) {
      assertNull(getResource(key1));
    }

    verifyListenerOnWriteAttempt(key1);
    verifyListenerOnWriteException(key1, writeException);

    // 2. Test a read failure from DiskStorage
    CacheKey key2 = new SimpleCacheKey("bbb");
    int value2Size = 42;
    byte[] value2 = new byte[value2Size];
    value2[25] = 'b';
    mCache.insert(key2, WriterCallbacks.from(value2));

    verifyListenerOnWriteAttempt(key2);
    String resourceId2 = verifyListenerOnWriteSuccessAndGetResourceId(key2, value2Size);

    ((DiskStorageWithReadFailures) mStorage).setPoisonResourceId(resourceId2);

    assertNull(mCache.getResource(key2));
    verifyListenerOnReadException(key2, DiskStorageWithReadFailures.POISON_EXCEPTION);

    assertFalse(mCache.probe(key2));
    verifyListenerOnReadException(key2, DiskStorageWithReadFailures.POISON_EXCEPTION);

    verifyNoMoreInteractions(mCacheEventListener);
  }

  @Test
  public void testCleanOldCache() throws IOException, NoSuchFieldException, IllegalAccessException {
    long cacheExpirationMs = TimeUnit.DAYS.toMillis(5);
    CacheKey key1 = new SimpleCacheKey("aaa");
    int value1Size = 41;
    byte[] value1 = new byte[value1Size];
    value1[25] = 'a';
    mCache.insert(key1, WriterCallbacks.from(value1));

    String resourceId1 = verifyListenerOnWriteSuccessAndGetResourceId(key1, value1Size);

    CacheKey key2 = new SimpleCacheKey("bbb");
    int value2Size = 42;
    byte[] value2 = new byte[value2Size];
    value2[25] = 'b';
    mCache.insert(key2, WriterCallbacks.from(value2));

    String resourceId2 = verifyListenerOnWriteSuccessAndGetResourceId(key2, value2Size);

    // Increment clock by default expiration time + 1 day
    when(mClock.now())
        .thenReturn(cacheExpirationMs + TimeUnit.DAYS.toMillis(1));

    CacheKey key3 = new SimpleCacheKey("ccc");
    int value3Size = 43;
    byte[] value3 = new byte[value3Size];
    value3[25] = 'c';
    mCache.insert(key3, WriterCallbacks.from(value3));
    long valueAge3 = TimeUnit.HOURS.toMillis(1);
    when(mClock.now()).thenReturn(
        cacheExpirationMs+ TimeUnit.DAYS.toMillis(1) + valueAge3);

    long oldestEntry = mCache.clearOldEntries(cacheExpirationMs);
    assertEquals(valueAge3, oldestEntry);

    assertArrayEquals(value3, getContents(getResource(key3)));
    assertNull(getResource(key1));
    assertNull(getResource(key2));

    String[] resourceIds = new String[] { resourceId1, resourceId2 };
    long[] itemSizes = new long[] { value1Size, value2Size };
    long cacheSizeBeforeEviction = value1Size + value2Size + value3Size;
    verifyListenerOnEviction(
        resourceIds,
        itemSizes,
        CacheEventListener.EvictionReason.CONTENT_STALE,
        cacheSizeBeforeEviction);
  }

  @Test
  public void testCleanOldCacheNoEntriesRemaining() throws IOException {
    long cacheExpirationMs = TimeUnit.DAYS.toMillis(5);
    CacheKey key1 = new SimpleCacheKey("aaa");
    byte[] value1 = new byte[41];
    mCache.insert(key1, WriterCallbacks.from(value1));

    CacheKey key2 = new SimpleCacheKey("bbb");
    byte[] value2 = new byte[42];
    mCache.insert(key2, WriterCallbacks.from(value2));

    // Increment clock by default expiration time + 1 day
    when(mClock.now())
        .thenReturn(cacheExpirationMs+ TimeUnit.DAYS.toMillis(1));

    long oldestEntry = mCache.clearOldEntries(cacheExpirationMs);
    assertEquals(0L, oldestEntry);
  }

  /**
   * Test to make sure that the same item stored with two different versions
   * of the cache will be stored with two different file names.
   *
   * @throws UnsupportedEncodingException
   */
  @Test
  public void testVersioning() throws IOException {

    // Define data that will be written to cache
    CacheKey key = new SimpleCacheKey("version_test");
    byte[] value = new byte[32];
    value[0] = 'v';

    // Set up cache with version == 1
    DiskStorage storage1 = createDiskStorage(TESTCACHE_CURRENT_VERSION);
    DiskStorageCache cache1 = createDiskCache(storage1, false);

    // Write test data to cache 1
    cache1.insert(key, WriterCallbacks.from(value));

    // Get cached file
    BinaryResource resource1 = getResource(storage1, key);
    assertNotNull(resource1);

    // Set up cache with version == 2
    DiskStorage storageSupplier2 =
        createDiskStorage(TESTCACHE_NEXT_VERSION);
    DiskStorageCache cache2 = createDiskCache(storageSupplier2, false);

    // Write test data to cache 2
    cache2.insert(key, WriterCallbacks.from(value));

    // Get cached file
    BinaryResource resource2 = getResource(storageSupplier2, key);
    assertNotNull(resource2);

    // Make sure filenames of the two file are different
    assertFalse(resource2.equals(resource1));
  }

  /**
   * Verify that multiple threads can write to the cache at the same time.
   */
  @Test
  public void testConcurrency() throws Exception {
    final CyclicBarrier barrier = new CyclicBarrier(3);
    WriterCallback writerCallback = new WriterCallback() {
      @Override
      public void write(OutputStream os) throws IOException {
        try {
          // Both threads will need to hit this barrier. If writing is serialized,
          // the second thread will never reach here as the first will hold
          // the write lock forever.
          barrier.await(10, TimeUnit.SECONDS);
        } catch (Exception e) {
          throw new RuntimeException(e);
        }
      }
    };
    CacheKey key1 = new SimpleCacheKey("concurrent1");
    CacheKey key2 = new SimpleCacheKey("concurrent2");
    Thread t1 = runInsertionInSeparateThread(key1, writerCallback);
    Thread t2 = runInsertionInSeparateThread(key2, writerCallback);
    barrier.await(10, TimeUnit.SECONDS);
    t1.join(1000);
    t2.join(1000);
  }

  @Test
  public void testIsEnabled() throws Exception {
    DiskStorage storageMock = mock(DiskStorage.class);
    when(storageMock.isEnabled()).thenReturn(true).thenReturn(false);

    DiskStorageCache cache = createDiskCache(storageMock, false);
    assertTrue(cache.isEnabled());
    assertFalse(cache.isEnabled());
  }

  private Thread runInsertionInSeparateThread(final CacheKey key,
      final WriterCallback callback) {
    Runnable runnable = new Runnable() {

      @Override
      public void run() {
        try {
          mCache.insert(key, callback);
        } catch (IOException e) {
          fail();
        }
      }
    };
    Thread thread = new Thread(runnable);
    thread.setDaemon(true);
    thread.start();
    return thread;
  }

  @Test
  public void testInsertionInIndex() throws Exception {
    CacheKey key = putOneThingInCache();
    assertTrue(mCache.hasKeySync(key));
    assertTrue(mCache.hasKey(key));
  }

  @Test
  public void testDoesntHaveKey() {
    CacheKey key = new SimpleCacheKey("foo");
    assertFalse(mCache.hasKeySync(key));
    assertFalse(mCache.hasKey(key));
  }

  @Test
  public void testHasKeyWithoutPopulateAtStartupWithoutAwaitingIndex() throws Exception {
    CacheKey key = putOneThingInCache();
    // A new cache object in the same directory. Equivalent to a process restart.
    // Index may not yet updated.
    DiskStorageCache cache2 = createDiskCache(mStorage, false);
    assertTrue(cache2.isIndexReady());
    assertFalse(cache2.hasKeySync(key));
    assertTrue(cache2.hasKey(key));
    // hasKey() adds item to the index
    assertTrue(cache2.hasKeySync(key));
  }

  @Test
  public void testHasKeyWithoutPopulateAtStartupWithAwaitingIndex() throws Exception {
    // A new cache object in the same directory. Equivalent to a process restart.
    // Index may not yet updated.
    DiskStorageCache cache2 = createDiskCache(mStorage, false);
    CacheKey key = putOneThingInCache();
    // Wait for index populated in cache before use of cache
    cache2.awaitIndex();
    assertTrue(cache2.isIndexReady());
    assertTrue(cache2.hasKey(key));
    assertTrue(cache2.hasKeySync(key));
  }

  @Test
  public void testHasKeyWithPopulateAtStartupWithAwaitingIndex() throws Exception {
    DiskStorageCache cache2 = createDiskCache(mStorage, false);
    // A new cache object in the same directory. Equivalent to a process restart.
    // Index should be updated.
    CacheKey key = putOneThingInCache(cache2);
    // Wait for index populated in cache before use of cache
    cache2.awaitIndex();
    assertTrue(cache2.isIndexReady());
    assertTrue(cache2.hasKeySync(key));
    assertTrue(cache2.hasKey(key));
  }

  @Test
  public void testHasKeyWithPopulateAtStartupWithoutAwaitingIndex() throws Exception {
    DiskStorageCache cache2 = createDiskCache(mStorage, true);
    CacheKey key = putOneThingInCache(cache2);
    // A new cache object in the same directory. Equivalent to a process restart.
    // Index may not yet updated.
    assertFalse(cache2.isIndexReady());
    assertTrue(cache2.hasKey(key));
    assertTrue(cache2.hasKeySync(key));
  }

  @Test
  public void testGetResourceWithoutAwaitingIndex() throws Exception {
    CacheKey key = putOneThingInCache();
    // A new cache object in the same directory. Equivalent to a process restart.
    // Index may not yet updated.
    DiskStorageCache cache2 = createDiskCache(mStorage, false);
    assertNotNull(cache2.getResource(key));
  }

  @Test
  public void testIndexIsImmediatelyReadyIfIndexAtStartupIsOff() {
    DiskStorageCache cache = createDiskCache(mStorage, false);

    assertThat(cache.isIndexReady()).isTrue();
  }

  @Test
  public void testIndexIsNotImmediatelyReadyIfIndexAtStartupIsOff() {
    DiskStorageCache cache = createDiskCache(mStorage, true);

    assertThat(cache.isIndexReady()).isFalse();
  }

  @Test
  public void testIndexIsReadyIfIndexAtStartupIsOnAndTheBackgroundExecutorHasRun() {
    DiskStorageCache cache = createDiskCache(mStorage, true);

    mBackgroundExecutor.runUntilIdle();

    assertThat(cache.isIndexReady()).isTrue();
  }

  @Test
  public void testClearIndex() throws Exception {
    CacheKey key = putOneThingInCache();
    mCache.clearAll();
    assertFalse(mCache.hasKeySync(key));
    assertFalse(mCache.hasKey(key));
  }

  @Test
  public void testRemoveFileClearsIndex() throws Exception {
    CacheKey key = putOneThingInCache();
    mStorage.clearAll();
    assertNull(mCache.getResource(key));
    assertFalse(mCache.hasKeySync(key));
  }

  @Test
  public void testSizeEvictionClearsIndex() throws Exception {
    when(mClock.now()).thenReturn(TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS));
    CacheKey key1 = putOneThingInCache();
    CacheKey key2 = new SimpleCacheKey("bar");
    CacheKey key3 = new SimpleCacheKey("duck");
    byte[] value2 = new byte[(int) FILE_CACHE_MAX_SIZE_HIGH_LIMIT];
    value2[80] = 'c';
    WriterCallback callback = WriterCallbacks.from(value2);
    when(mClock.now()).thenReturn(TimeUnit.MILLISECONDS.convert(2, TimeUnit.DAYS));
    mCache.insert(key2, callback);
    // now over limit. Next write will evict key1
    when(mClock.now()).thenReturn(TimeUnit.MILLISECONDS.convert(3, TimeUnit.DAYS));
    mCache.insert(key3, callback);
    assertFalse(mCache.hasKeySync(key1));
    assertFalse(mCache.hasKey(key1));
    assertTrue(mCache.hasKeySync(key3));
    assertTrue(mCache.hasKey(key3));
  }

  @Test
  public void testTimeEvictionClearsIndex() throws Exception {
    when(mClock.now()).thenReturn(5l);
    CacheKey key = putOneThingInCache();
    mCache.clearOldEntries(4);
    assertFalse(mCache.hasKeySync(key));
    assertFalse(mCache.hasKey(key));
  }

  private CacheKey putOneThingInCache() throws IOException {
    return putOneThingInCache(mCache);
  }

  private CacheKey putOneThingInCache(DiskStorageCache cache) throws IOException {
    CacheKey key = new SimpleCacheKey("foo");
    byte[] value1 = new byte[101];
    value1[80] = 'c';
    cache.insert(key, WriterCallbacks.from(value1));
    return key;
  }

  private void verifyListenerOnHit(CacheKey key, String resourceId) {
    ArgumentCaptor<CacheEvent> cacheEventCaptor = ArgumentCaptor.forClass(CacheEvent.class);
    mCacheEventListenerInOrder.verify(mCacheEventListener).onHit(cacheEventCaptor.capture());

    for (CacheEvent event : cacheEventCaptor.getAllValues()) {
      CacheEventAssert.assertThat(event)
          .isNotNull()
          .hasCacheKey(key)
          .hasResourceId(resourceId);
    }
  }

  private void verifyListenerOnMiss(CacheKey key) {
    ArgumentCaptor<CacheEvent> cacheEventCaptor = ArgumentCaptor.forClass(CacheEvent.class);
    mCacheEventListenerInOrder.verify(mCacheEventListener).onMiss(cacheEventCaptor.capture());

    for (CacheEvent event : cacheEventCaptor.getAllValues()) {
      CacheEventAssert.assertThat(event)
          .isNotNull()
          .hasCacheKey(key);
    }
  }

  private void verifyListenerOnWriteAttempt(CacheKey key) {
    ArgumentCaptor<CacheEvent> cacheEventCaptor = ArgumentCaptor.forClass(CacheEvent.class);
    mCacheEventListenerInOrder.verify(mCacheEventListener)
        .onWriteAttempt(cacheEventCaptor.capture());

    CacheEventAssert.assertThat(cacheEventCaptor.getValue())
        .isNotNull()
        .hasCacheKey(key);
  }

  private String verifyListenerOnWriteSuccessAndGetResourceId(
      CacheKey key,
      long itemSize) {
    ArgumentCaptor<CacheEvent> cacheEventCaptor = ArgumentCaptor.forClass(CacheEvent.class);
    mCacheEventListenerInOrder.verify(mCacheEventListener)
        .onWriteSuccess(cacheEventCaptor.capture());

    CacheEvent cacheEvent = cacheEventCaptor.getValue();
    CacheEventAssert.assertThat(cacheEvent)
        .isNotNull()
        .hasCacheKey(key)
        .hasItemSize(itemSize)
        .hasResourceIdSet();

    return cacheEvent.getResourceId();
  }

  private void verifyListenerOnWriteException(CacheKey key, IOException exception) {
    ArgumentCaptor<CacheEvent> cacheEventCaptor = ArgumentCaptor.forClass(CacheEvent.class);
    mCacheEventListenerInOrder.verify(mCacheEventListener)
        .onWriteException(cacheEventCaptor.capture());

    CacheEventAssert.assertThat(cacheEventCaptor.getValue())
        .isNotNull()
        .hasCacheKey(key)
        .hasException(exception);
  }

  private void verifyListenerOnReadException(CacheKey key, IOException exception) {
    ArgumentCaptor<CacheEvent> cacheEventCaptor = ArgumentCaptor.forClass(CacheEvent.class);
    mCacheEventListenerInOrder.verify(mCacheEventListener)
        .onReadException(cacheEventCaptor.capture());

    CacheEventAssert.assertThat(cacheEventCaptor.getValue())
        .isNotNull()
        .hasCacheKey(key)
        .hasException(exception);
  }

  private void verifyListenerOnEviction(
      String[] resourceIds,
      long[] itemSizes,
      CacheEventListener.EvictionReason reason,
      long cacheSizeBeforeEviction) {
    int numberItems = resourceIds.length;
    ArgumentCaptor<CacheEvent> cacheEventCaptor = ArgumentCaptor.forClass(CacheEvent.class);
    mCacheEventListenerInOrder.verify(mCacheEventListener, times(numberItems))
        .onEviction(cacheEventCaptor.capture());

    boolean[] found = new boolean[numberItems];
    long runningCacheSize = cacheSizeBeforeEviction;

    // The eviction order is unknown so make allowances for them coming in different orders
    for (CacheEvent event : cacheEventCaptor.getAllValues()) {
      CacheEventAssert.assertThat(event).isNotNull();

      for (int i = 0; i < numberItems; i++) {
        if (!found[i] && resourceIds[i].equals(event.getResourceId())) {
          found[i] = true;
          CacheEventAssert.assertThat(event)
              .hasItemSize(itemSizes[i])
              .hasEvictionReason(reason);
        }
      }

      runningCacheSize -= event.getItemSize();
      CacheEventAssert.assertThat(event)
          .hasCacheSize(runningCacheSize);
    }

    // Ensure all resources were found
    for (int i = 0; i < numberItems; i++) {
      assertTrue(
          String.format("Expected eviction of resource %s but wasn't evicted", resourceIds[i]),
          found[i]);
    }
  }

  private static class DiskStorageWithReadFailures extends DynamicDefaultDiskStorage {

    public static final IOException POISON_EXCEPTION =
        new IOException("Poisoned resource requested");

    private String mPoisonResourceId;

    public DiskStorageWithReadFailures(
        int version,
        Supplier<File> baseDirectoryPathSupplier,
        String baseDirectoryName, CacheErrorLogger cacheErrorLogger) {
      super(version, baseDirectoryPathSupplier, baseDirectoryName, cacheErrorLogger);
    }

    public void setPoisonResourceId(String poisonResourceId) {
      mPoisonResourceId = poisonResourceId;
    }

    @Override
    public BinaryResource getResource(String resourceId, Object debugInfo) throws IOException {
      if (resourceId.equals(mPoisonResourceId)) {
        throw POISON_EXCEPTION;
      }
      return get().getResource(resourceId, debugInfo);
    }

    @Override
    public boolean touch(String resourceId, Object debugInfo) throws IOException {
      if (resourceId.equals(mPoisonResourceId)) {
        throw POISON_EXCEPTION;
      }
      return super.touch(resourceId, debugInfo);
    }
  }

  /**
   * CacheEventListener implementation which copies the data from each event into a new instance to
   * work-around the recycling of the original event and forwards the copy so that assertions can be
   * made afterwards.
   */
  private static class DuplicatingCacheEventListener implements CacheEventListener {

    private final CacheEventListener mRecipientListener;

    public DuplicatingCacheEventListener(CacheEventListener recipientListener) {
      mRecipientListener = recipientListener;
    }

    @Override
    public void onHit(CacheEvent cacheEvent) {
      mRecipientListener.onHit(duplicateEvent(cacheEvent));
    }

    @Override
    public void onMiss(CacheEvent cacheEvent) {
      mRecipientListener.onMiss(duplicateEvent(cacheEvent));
    }

    @Override
    public void onWriteAttempt(CacheEvent cacheEvent) {
      mRecipientListener.onWriteAttempt(duplicateEvent(cacheEvent));
    }

    @Override
    public void onWriteSuccess(CacheEvent cacheEvent) {
      mRecipientListener.onWriteSuccess(duplicateEvent(cacheEvent));
    }

    @Override
    public void onReadException(CacheEvent cacheEvent) {
      mRecipientListener.onReadException(duplicateEvent(cacheEvent));
    }

    @Override
    public void onWriteException(CacheEvent cacheEvent) {
      mRecipientListener.onWriteException(duplicateEvent(cacheEvent));
    }

    @Override
    public void onEviction(CacheEvent cacheEvent) {
      mRecipientListener.onEviction(duplicateEvent(cacheEvent));
    }

    @Override
    public void onCleared() {
      mRecipientListener.onCleared();
    }

    private static CacheEvent duplicateEvent(CacheEvent cacheEvent) {
      SettableCacheEvent copyEvent = SettableCacheEvent.obtain();
      copyEvent.setCacheKey(cacheEvent.getCacheKey());
      copyEvent.setCacheLimit(cacheEvent.getCacheLimit());
      copyEvent.setCacheSize(cacheEvent.getCacheSize());
      copyEvent.setEvictionReason(cacheEvent.getEvictionReason());
      copyEvent.setException(cacheEvent.getException());
      copyEvent.setItemSize(cacheEvent.getItemSize());
      copyEvent.setResourceId(cacheEvent.getResourceId());
      return copyEvent;
    }
  }
}
