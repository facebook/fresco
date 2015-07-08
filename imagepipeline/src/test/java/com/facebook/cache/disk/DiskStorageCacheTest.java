/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.cache.disk;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.UnsupportedEncodingException;
import java.util.Locale;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;

import com.facebook.binaryresource.BinaryResource;
import com.facebook.cache.common.CacheErrorLogger;
import com.facebook.cache.common.CacheEventListener;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import com.facebook.cache.common.WriterCallback;
import com.facebook.cache.common.WriterCallbacks;
import com.facebook.common.disk.DiskTrimmableRegistry;
import com.facebook.common.internal.ByteStreams;
import com.facebook.common.internal.Suppliers;
import com.facebook.common.time.SystemClock;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareOnlyThisForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Test for {@link DiskStorageCache}
 */
@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareOnlyThisForTest({SystemClock.class})
public class DiskStorageCacheTest {

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  private static final String CACHE_TYPE = "media_test";

  private static final int TESTCACHE_VERSION_START_OF_VERSIONING = 1;
  private static final int TESTCACHE_CURRENT_VERSION = TESTCACHE_VERSION_START_OF_VERSIONING;
  private static final int TESTCACHE_NEXT_VERSION = TESTCACHE_CURRENT_VERSION + 1;

  private File mCacheDirectory;
  private DiskStorageSupplier mStorageSupplier;
  private DiskStorageCache mCache;
  private DiskTrimmableRegistry mDiskTrimmableRegistry;
  private CacheEventListener mCacheEventListener;
  private SystemClock mClock;

  @Before
  public void setUp() {
    mClock = mock(SystemClock.class);
    PowerMockito.mockStatic(SystemClock.class);
    PowerMockito.when(SystemClock.get()).thenReturn(mClock);
    mDiskTrimmableRegistry = mock(DiskTrimmableRegistry.class);
    mCacheEventListener = mock(CacheEventListener.class);

    // we know the directory will be this
    mCacheDirectory = new File(Robolectric.application.getCacheDir(), CACHE_TYPE);
    mCacheDirectory.mkdirs();
    if (!mCacheDirectory.exists()) {
      throw new RuntimeException(
          String.format(
              (Locale) null,
              "Cannot create cache dir: %s: directory %s",
              mCacheDirectory.getAbsolutePath(),
              mCacheDirectory.exists() ? "already exists" : "does not exist"));
    }
    mStorageSupplier = createDiskStorageSupplier(TESTCACHE_VERSION_START_OF_VERSIONING);
    mCache = createDiskCache(mStorageSupplier);
    verify(mDiskTrimmableRegistry).registerDiskTrimmable(mCache);
  }

  // The threshold (in bytes) for the size of file cache
  private static final long FILE_CACHE_MAX_SIZE_HIGH_LIMIT = 200;
  private static final long FILE_CACHE_MAX_SIZE_LOW_LIMIT = 200;

  private DiskStorageSupplier createDiskStorageSupplier(int version) {
    return new DefaultDiskStorageSupplier(
        version,
        Suppliers.of(Robolectric.application.getApplicationContext().getCacheDir()),
        CACHE_TYPE,
        mock(CacheErrorLogger.class));
  }

  private DiskStorageCache createDiskCache(DiskStorageSupplier diskStorageSupplier) {
    DiskStorageCache.Params diskStorageCacheParams =
        new DiskStorageCache.Params(
            0,
            FILE_CACHE_MAX_SIZE_LOW_LIMIT,
            FILE_CACHE_MAX_SIZE_HIGH_LIMIT);

    return new DiskStorageCache(
        diskStorageSupplier,
        diskStorageCacheParams,
        mCacheEventListener,
        mock(CacheErrorLogger.class),
        mDiskTrimmableRegistry);
  }

  @Test
  public void testCacheEventListener() throws Exception {
    // 1. Add first cache file
    CacheKey key1 = new SimpleCacheKey("foo");
    byte[] value1 = new byte[101];
    value1[80] = 'c'; // just so it's not all zeros for the equality test below.
    BinaryResource resource1 = mCache.insert(key1, WriterCallbacks.from(value1));
    verify(mCacheEventListener).onWriteAttempt();

    BinaryResource resource1Again = mCache.getResource(key1);
    assertEquals(resource1, resource1Again);
    verify(mCacheEventListener).onHit();
    BinaryResource resource1Again2 = mCache.getResource(key1);
    assertEquals(resource1, resource1Again2);
    verify(mCacheEventListener, times(2)).onHit();

    BinaryResource res2 = mCache.getResource(new SimpleCacheKey("nonexistent_key"));
    assertNull(res2);
    verify(mCacheEventListener).onMiss();

    verifyNoMoreInteractions(mCacheEventListener);
  }


  private BinaryResource getResource(
      DiskStorageSupplier supplier,
      final CacheKey key) throws IOException {
     return supplier.get().getResource(mCache.getResourceId(key), key);
  }

  private BinaryResource getResource(final CacheKey key) throws IOException {
    return mStorageSupplier.get().getResource(mCache.getResourceId(key), key);
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
    // Note: It might seem more natural to increment the clock before calling
    // MediaCache.insertCachedMedia() to apply a desired timestamp to the
    // files. But the time is being explicitly modified here so that any
    // failures in setting/re-setting file timestamps are caught by the assert,
    // instead of being hidden inside MediaCache code that can lead to
    // intermittent test failures which are very tricky to debug.
    // Note: For MediaCache.markForLru() to update the lru time, the clock
    // needs to be incremented by at least MediaCache.CACHE_UPDATE_PERIOD_MS
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
    assertTrue(mCache.getSize() == 205);

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
  public void testCacheFileWithIOException() throws IOException {
    CacheKey key1 = new SimpleCacheKey("aaa");

    // Before inserting, make sure files not exist.
    final BinaryResource resource1 = getResource(key1);
    assertNull(resource1);


    // Should not create cache files if IOException happens in the middle.
    try {
      mCache.insert(
          key1, new WriterCallback() {
            @Override
            public void write(OutputStream os) throws IOException {
              throw new IOException();
            }
          });
      fail();
    } catch (IOException e) {
      assertNull(getResource(key1));
    }

    try {
      // Should create cache files if everything is ok.
      mCache.insert(key1, WriterCallbacks.from(new byte[100]));
      assertNotNull(getResource(key1));
    } catch (IOException e) {
      fail();
    }

    // Should not create a new file if reading hits an IOException.
    CacheKey key2 = new SimpleCacheKey("bbb");
    try {
      mCache.insert(
          key2, new WriterCallback() {
            @Override
            public void write(OutputStream os) throws IOException {
              throw new IOException();
            }
          });
      fail();
    } catch (IOException e) {
      assertNull(getResource(key2));
    }
  }

  @Test
  public void testCleanOldCache() throws IOException, NoSuchFieldException, IllegalAccessException {
    long cacheExpirationMs = TimeUnit.DAYS.toMillis(5);
    int value1size = 41;
    int value2size = 42;
    CacheKey key1 = new SimpleCacheKey("aaa");
    byte[] value1 = new byte[value1size];
    value1[25] = 'a';
    mCache.insert(key1, WriterCallbacks.from(value1));

    CacheKey key2 = new SimpleCacheKey("bbb");
    byte[] value2 = new byte[value2size];
    value2[25] = 'b';
    mCache.insert(key2, WriterCallbacks.from(value2));

    // Increment clock by default expiration time + 1 day
    when(mClock.now())
        .thenReturn(cacheExpirationMs + TimeUnit.DAYS.toMillis(1));

    CacheKey key3 = new SimpleCacheKey("ccc");
    byte[] value3 = new byte[43];
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

    verify(mCacheEventListener)
        .onEviction(CacheEventListener.EvictionReason.CONTENT_STALE, 2, value1size + value2size);
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
    DiskStorageSupplier storageSupplier1 = createDiskStorageSupplier(TESTCACHE_CURRENT_VERSION);
    DiskStorageCache cache1 = createDiskCache(storageSupplier1);

    // Write test data to cache 1
    cache1.insert(key, WriterCallbacks.from(value));

    // Get cached file
    BinaryResource resource1 = getResource(storageSupplier1, key);
    assertNotNull(resource1);

    // Set up cache with version == 2
    DiskStorageSupplier storageSupplier2 =
        createDiskStorageSupplier(TESTCACHE_NEXT_VERSION);
    DiskStorageCache cache2 = createDiskCache(storageSupplier2);

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
    DiskStorageSupplier supplierMock = mock(DiskStorageSupplier.class);
    when(supplierMock.get()).thenReturn(storageMock);

    DiskStorageCache cache = createDiskCache(supplierMock);
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
}
