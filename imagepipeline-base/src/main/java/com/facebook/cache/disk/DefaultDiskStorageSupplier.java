/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.cache.disk;

import javax.annotation.Nullable;

import java.io.File;
import java.io.IOException;

import com.facebook.cache.common.CacheErrorLogger;
import com.facebook.common.file.FileTree;
import com.facebook.common.file.FileUtils;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.Supplier;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.logging.FLog;

/**
 * A supplier of a DiskStorage concrete implementation.
 */
public class DefaultDiskStorageSupplier implements DiskStorageSupplier {
  private static final Class<?> TAG = DefaultDiskStorageSupplier.class;

  private final int mVersion;
  private final Supplier<File> mBaseDirectoryPathSupplier;
  private final String mBaseDirectoryName;
  private final CacheErrorLogger mCacheErrorLogger;

  @VisibleForTesting
  volatile State mCurrentState;

  /**
   * Represents the current 'cached' state.
   */
  @VisibleForTesting static class State {
    public final @Nullable DiskStorage storage;
    public final @Nullable File rootDirectory;

    @VisibleForTesting State(@Nullable File rootDirectory, @Nullable DiskStorage storage) {
      this.storage = storage;
      this.rootDirectory = rootDirectory;
    }
  }

  public DefaultDiskStorageSupplier(
      int version,
      Supplier<File> baseDirectoryPathSupplier,
      String baseDirectoryName,
      CacheErrorLogger cacheErrorLogger) {
    mVersion = version;
    mCacheErrorLogger = cacheErrorLogger;
    mBaseDirectoryPathSupplier = baseDirectoryPathSupplier;
    mBaseDirectoryName = baseDirectoryName;
    mCurrentState = new State(null, null);
  }

  /**
   * Gets a concrete disk-storage instance. If nothing has changed since the last call, then
   * the last state is returned
   * @return an instance of the appropriate DiskStorage class
   * @throws IOException
   */
  @Override
  public synchronized DiskStorage get() throws IOException {
    if (shouldCreateNewStorage()) {
      // discard anything we created
      deleteOldStorageIfNecessary();
      createStorage();
    }
    return Preconditions.checkNotNull(mCurrentState.storage);
  }

  private boolean shouldCreateNewStorage() {
    State currentState = mCurrentState;
    return (currentState.storage == null ||
        currentState.rootDirectory == null ||
        !currentState.rootDirectory.exists());
  }

  @VisibleForTesting
  void deleteOldStorageIfNecessary() {
    if (mCurrentState.storage != null && mCurrentState.rootDirectory != null) {
      // LATER: Actually delegate this call to the storage. We shouldn't be
      // making an end-run around it
      FileTree.deleteRecursively(mCurrentState.rootDirectory);
    }
  }

  private void createStorage() throws IOException {
    File rootDirectory = new File(mBaseDirectoryPathSupplier.get(), mBaseDirectoryName);
    createRootDirectoryIfNecessary(rootDirectory);
    DiskStorage storage = new DefaultDiskStorage(rootDirectory, mVersion, mCacheErrorLogger);
    mCurrentState = new State(rootDirectory, storage);
  }

  @VisibleForTesting
  void createRootDirectoryIfNecessary(File rootDirectory) throws IOException {
    try {
      FileUtils.mkdirs(rootDirectory);
    } catch (FileUtils.CreateDirectoryException cde) {
      mCacheErrorLogger.logError(
          CacheErrorLogger.CacheErrorCategory.WRITE_CREATE_DIR,
          TAG,
          "createRootDirectoryIfNecessary",
          cde);
      throw cde;
    }
    FLog.d(TAG, "Created cache directory %s", rootDirectory.getAbsolutePath());
  }
}
