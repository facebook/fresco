/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.cache;

import bolts.Task;
import com.facebook.binaryresource.BinaryResource;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.WriterCallback;
import com.facebook.cache.disk.FileCache;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.logging.FLog;
import com.facebook.common.memory.PooledByteBuffer;
import com.facebook.common.memory.PooledByteBufferFactory;
import com.facebook.common.memory.PooledByteStreams;
import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.instrumentation.FrescoInstrumenter;
import com.facebook.imagepipeline.systrace.FrescoSystrace;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.Nullable;

/**
 * BufferedDiskCache provides get and put operations to take care of scheduling disk-cache
 * read/writes.
 */
public class BufferedDiskCache {
  private static final Class<?> TAG = BufferedDiskCache.class;

  private final FileCache mFileCache;
  private final PooledByteBufferFactory mPooledByteBufferFactory;
  private final PooledByteStreams mPooledByteStreams;
  private final Executor mReadExecutor;
  private final Executor mWriteExecutor;
  private final StagingArea mStagingArea;
  private final ImageCacheStatsTracker mImageCacheStatsTracker;

  public BufferedDiskCache(
      FileCache fileCache,
      PooledByteBufferFactory pooledByteBufferFactory,
      PooledByteStreams pooledByteStreams,
      Executor readExecutor,
      Executor writeExecutor,
      ImageCacheStatsTracker imageCacheStatsTracker) {
    mFileCache = fileCache;
    mPooledByteBufferFactory = pooledByteBufferFactory;
    mPooledByteStreams = pooledByteStreams;
    mReadExecutor = readExecutor;
    mWriteExecutor = writeExecutor;
    mImageCacheStatsTracker = imageCacheStatsTracker;
    mStagingArea = StagingArea.getInstance();
  }

  /**
   * Returns true if the key is in the in-memory key index.
   *
   * <p>Not guaranteed to be correct. The cache may yet have this key even if this returns false.
   * But if it returns true, it definitely has it.
   *
   * <p>Avoids a disk read.
   */
  public boolean containsSync(CacheKey key) {
    return mStagingArea.containsKey(key) || mFileCache.hasKeySync(key);
  }

  /**
   * Performs a key-value look up in the disk cache. If no value is found in the staging area, then
   * disk cache checks are scheduled on a background thread. Any error manifests itself as a cache
   * miss, i.e. the returned Task resolves to false.
   *
   * @param key
   * @return Task that resolves to true if an element is found, or false otherwise
   */
  public Task<Boolean> contains(final CacheKey key) {
    if (containsSync(key)) {
      return Task.forResult(true);
    }
    return containsAsync(key);
  }

  private Task<Boolean> containsAsync(final CacheKey key) {
    try {
      final Object token = FrescoInstrumenter.onBeforeSubmitWork("BufferedDiskCache_containsAsync");
      return Task.call(
          new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
              final Object currentToken = FrescoInstrumenter.onBeginWork(token, null);
              try {
                return checkInStagingAreaAndFileCache(key);
              } catch (Throwable th) {
                FrescoInstrumenter.markFailure(token, th);
                throw th;
              } finally {
                FrescoInstrumenter.onEndWork(currentToken);
              }
            }
          },
          mReadExecutor);
    } catch (Exception exception) {
      // Log failure
      // TODO: 3697790
      FLog.w(TAG, exception, "Failed to schedule disk-cache read for %s", key.getUriString());
      return Task.forError(exception);
    }
  }

  /**
   * Performs disk cache check synchronously.
   *
   * @param key
   * @return true if the key is found in disk cache else false
   */
  public boolean diskCheckSync(final CacheKey key) {
    if (containsSync(key)) {
      return true;
    }
    return checkInStagingAreaAndFileCache(key);
  }

  /**
   * Performs key-value look up in disk cache. If value is not found in disk cache staging area then
   * disk cache read is scheduled on background thread. Any error manifests itself as cache miss,
   * i.e. the returned task resolves to null.
   *
   * @param key
   * @return Task that resolves to cached element or null if one cannot be retrieved; returned task
   *     never rethrows any exception
   */
  public Task<EncodedImage> get(CacheKey key, AtomicBoolean isCancelled) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("BufferedDiskCache#get");
      }
      final EncodedImage pinnedImage = mStagingArea.get(key);
      if (pinnedImage != null) {
        return foundPinnedImage(key, pinnedImage);
      }
      return getAsync(key, isCancelled);
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  /**
   * Performs key-value look up in disk cache. If value is not found in disk cache staging area then
   * disk cache probing is scheduled on background thread.
   *
   * @param key
   */
  public Task<Void> probe(final CacheKey key) {
    Preconditions.checkNotNull(key);
    try {
      final Object token = FrescoInstrumenter.onBeforeSubmitWork("BufferedDiskCache_probe");
      return Task.call(
          new Callable<Void>() {
            @Override
            public Void call() throws Exception {
              final Object currentToken = FrescoInstrumenter.onBeginWork(token, null);
              try {
                mFileCache.probe(key);
              } finally {
                FrescoInstrumenter.onEndWork(currentToken);
              }
              return null;
            }
          },
          mWriteExecutor);
    } catch (Exception exception) {
      FLog.w(TAG, exception, "Failed to schedule disk-cache probe for %s", key.getUriString());
      return Task.forError(exception);
    }
  }

  public void addKeyForAsyncProbing(final CacheKey key) {
    Preconditions.checkNotNull(key);
    mFileCache.probe(key);
  }

  /**
   * Performs key-value loop up in staging area and file cache. Any error manifests itself as a
   * miss, i.e. returns false.
   *
   * @param key
   * @return true if the image is found in staging area or File cache, false if not found
   */
  private boolean checkInStagingAreaAndFileCache(final CacheKey key) {
    EncodedImage result = mStagingArea.get(key);
    if (result != null) {
      result.close();
      FLog.v(TAG, "Found image for %s in staging area", key.getUriString());
      mImageCacheStatsTracker.onStagingAreaHit(key);
      return true;
    } else {
      FLog.v(TAG, "Did not find image for %s in staging area", key.getUriString());
      mImageCacheStatsTracker.onStagingAreaMiss(key);
      try {
        return mFileCache.hasKey(key);
      } catch (Exception exception) {
        return false;
      }
    }
  }

  private Task<EncodedImage> getAsync(final CacheKey key, final AtomicBoolean isCancelled) {
    try {
      final Object token = FrescoInstrumenter.onBeforeSubmitWork("BufferedDiskCache_getAsync");
      return Task.call(
          new Callable<EncodedImage>() {
            @Override
            public @Nullable EncodedImage call() throws Exception {
              final Object currentToken = FrescoInstrumenter.onBeginWork(token, null);
              try {
                if (isCancelled.get()) {
                  throw new CancellationException();
                }
                EncodedImage result = mStagingArea.get(key);
                if (result != null) {
                  FLog.v(TAG, "Found image for %s in staging area", key.getUriString());
                  mImageCacheStatsTracker.onStagingAreaHit(key);
                } else {
                  FLog.v(TAG, "Did not find image for %s in staging area", key.getUriString());
                  mImageCacheStatsTracker.onStagingAreaMiss(key);

                  try {
                    final PooledByteBuffer buffer = readFromDiskCache(key);
                    if (buffer == null) {
                      return null;
                    }
                    CloseableReference<PooledByteBuffer> ref = CloseableReference.of(buffer);
                    try {
                      result = new EncodedImage(ref);
                    } finally {
                      CloseableReference.closeSafely(ref);
                    }
                  } catch (Exception exception) {
                    return null;
                  }
                }

                if (Thread.interrupted()) {
                  FLog.v(TAG, "Host thread was interrupted, decreasing reference count");
                  if (result != null) {
                    result.close();
                  }
                  throw new InterruptedException();
                } else {
                  return result;
                }
              } catch (Throwable th) {
                FrescoInstrumenter.markFailure(token, th);
                throw th;
              } finally {
                FrescoInstrumenter.onEndWork(currentToken);
              }
            }
          },
          mReadExecutor);
    } catch (Exception exception) {
      // Log failure
      // TODO: 3697790
      FLog.w(TAG, exception, "Failed to schedule disk-cache read for %s", key.getUriString());
      return Task.forError(exception);
    }
  }

  /**
   * Associates encodedImage with given key in disk cache. Disk write is performed on background
   * thread, so the caller of this method is not blocked
   */
  public void put(final CacheKey key, EncodedImage encodedImage) {
    try {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.beginSection("BufferedDiskCache#put");
      }
      Preconditions.checkNotNull(key);
      Preconditions.checkArgument(EncodedImage.isValid(encodedImage));

      // Store encodedImage in staging area
      mStagingArea.put(key, encodedImage);

      // Write to disk cache. This will be executed on background thread, so increment the ref
      // count. When this write completes (with success/failure), then we will bump down the
      // ref count again.
      final EncodedImage finalEncodedImage = EncodedImage.cloneOrNull(encodedImage);
      try {
        final Object token = FrescoInstrumenter.onBeforeSubmitWork("BufferedDiskCache_putAsync");
        mWriteExecutor.execute(
            new Runnable() {
              @Override
              public void run() {
                final Object currentToken = FrescoInstrumenter.onBeginWork(token, null);
                try {
                  writeToDiskCache(key, finalEncodedImage);
                } catch (Throwable th) {
                  FrescoInstrumenter.markFailure(token, th);
                  throw th;
                } finally {
                  mStagingArea.remove(key, finalEncodedImage);
                  EncodedImage.closeSafely(finalEncodedImage);
                  FrescoInstrumenter.onEndWork(currentToken);
                }
              }
            });
      } catch (Exception exception) {
        // We failed to enqueue cache write. Log failure and decrement ref count
        // TODO: 3697790
        FLog.w(TAG, exception, "Failed to schedule disk-cache write for %s", key.getUriString());
        mStagingArea.remove(key, encodedImage);
        EncodedImage.closeSafely(finalEncodedImage);
      }
    } finally {
      if (FrescoSystrace.isTracing()) {
        FrescoSystrace.endSection();
      }
    }
  }

  /** Removes the item from the disk cache and the staging area. */
  public Task<Void> remove(final CacheKey key) {
    Preconditions.checkNotNull(key);
    mStagingArea.remove(key);
    try {
      final Object token = FrescoInstrumenter.onBeforeSubmitWork("BufferedDiskCache_remove");
      return Task.call(
          new Callable<Void>() {
            @Override
            public Void call() throws Exception {
              final Object currentToken = FrescoInstrumenter.onBeginWork(token, null);
              try {
                mStagingArea.remove(key);
                mFileCache.remove(key);
              } catch (Throwable th) {
                FrescoInstrumenter.markFailure(token, th);
                throw th;
              } finally {
                FrescoInstrumenter.onEndWork(currentToken);
              }
              return null;
            }
          },
          mWriteExecutor);
    } catch (Exception exception) {
      // Log failure
      // TODO: 3697790
      FLog.w(TAG, exception, "Failed to schedule disk-cache remove for %s", key.getUriString());
      return Task.forError(exception);
    }
  }

  /** Clears the disk cache and the staging area. */
  public Task<Void> clearAll() {
    mStagingArea.clearAll();
    final Object token = FrescoInstrumenter.onBeforeSubmitWork("BufferedDiskCache_clearAll");
    try {
      return Task.call(
          new Callable<Void>() {
            @Override
            public Void call() throws Exception {
              final Object currentToken = FrescoInstrumenter.onBeginWork(token, null);
              try {
                mStagingArea.clearAll();
                mFileCache.clearAll();
                return null;
              } catch (Throwable th) {
                FrescoInstrumenter.markFailure(token, th);
                throw th;
              } finally {
                FrescoInstrumenter.onEndWork(currentToken);
              }
            }
          },
          mWriteExecutor);
    } catch (Exception exception) {
      // Log failure
      // TODO: 3697790
      FLog.w(TAG, exception, "Failed to schedule disk-cache clear");
      return Task.forError(exception);
    }
  }

  public long getSize() {
    return mFileCache.getSize();
  }

  private Task<EncodedImage> foundPinnedImage(CacheKey key, EncodedImage pinnedImage) {
    FLog.v(TAG, "Found image for %s in staging area", key.getUriString());
    mImageCacheStatsTracker.onStagingAreaHit(key);
    return Task.forResult(pinnedImage);
  }

  /** Performs disk cache read. In case of any exception null is returned. */
  private @Nullable PooledByteBuffer readFromDiskCache(final CacheKey key) throws IOException {
    try {
      FLog.v(TAG, "Disk cache read for %s", key.getUriString());

      final BinaryResource diskCacheResource = mFileCache.getResource(key);
      if (diskCacheResource == null) {
        FLog.v(TAG, "Disk cache miss for %s", key.getUriString());
        mImageCacheStatsTracker.onDiskCacheMiss(key);
        return null;
      } else {
        FLog.v(TAG, "Found entry in disk cache for %s", key.getUriString());
        mImageCacheStatsTracker.onDiskCacheHit(key);
      }

      PooledByteBuffer byteBuffer;
      final InputStream is = diskCacheResource.openStream();
      try {
        byteBuffer = mPooledByteBufferFactory.newByteBuffer(is, (int) diskCacheResource.size());
      } finally {
        is.close();
      }

      FLog.v(TAG, "Successful read from disk cache for %s", key.getUriString());
      return byteBuffer;
    } catch (IOException ioe) {
      // TODO: 3697790 log failures
      // TODO: 5258772 - uncomment line below
      // mFileCache.remove(key);
      FLog.w(TAG, ioe, "Exception reading from cache for %s", key.getUriString());
      mImageCacheStatsTracker.onDiskCacheGetFail(key);
      throw ioe;
    }
  }

  /**
   * Writes to disk cache
   *
   * @throws IOException
   */
  private void writeToDiskCache(final CacheKey key, final EncodedImage encodedImage) {
    FLog.v(TAG, "About to write to disk-cache for key %s", key.getUriString());
    try {
      mFileCache.insert(
          key,
          new WriterCallback() {
            @Override
            public void write(OutputStream os) throws IOException {
              mPooledByteStreams.copy(encodedImage.getInputStream(), os);
            }
          });
      mImageCacheStatsTracker.onDiskCachePut(key);
      FLog.v(TAG, "Successful disk-cache write for key %s", key.getUriString());
    } catch (IOException ioe) {
      // Log failure
      // TODO: 3697790
      FLog.w(TAG, ioe, "Failed to write to disk-cache for key %s", key.getUriString());
    }
  }
}
