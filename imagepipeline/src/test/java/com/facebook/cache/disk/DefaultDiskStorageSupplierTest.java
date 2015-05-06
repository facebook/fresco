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

import android.content.Context;

import com.facebook.cache.common.CacheErrorLogger;
import com.facebook.common.file.FileTree;
import com.facebook.common.internal.Suppliers;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.mockito.Mockito.mock;

/**
 * Test out methods in DefaultDiskStorageSupplier
 */
@RunWith(RobolectricTestRunner.class)
public class DefaultDiskStorageSupplierTest {

  private int mVersion;
  private String mBaseDirectoryName;
  private CacheErrorLogger mCacheErrorLogger;
  private Context mContext;

  @Before
  public void setUp() {
    mContext = Robolectric.application.getApplicationContext();
    mVersion = 1;
    mBaseDirectoryName = "base";
    mCacheErrorLogger = mock(CacheErrorLogger.class);
  }

  private DefaultDiskStorageSupplier createSupplier(boolean useFilesDirInsteadOfCacheDir) {
    return new DefaultDiskStorageSupplier(
        mVersion,
        useFilesDirInsteadOfCacheDir ?
            Suppliers.of(mContext.getFilesDir()) :
            Suppliers.of(mContext.getCacheDir()),
        mBaseDirectoryName,
        mCacheErrorLogger);
  }

  private DefaultDiskStorageSupplier createInternalCacheDirSupplier() {
    return createSupplier(false);
  }

  private DefaultDiskStorageSupplier createInternalFilesDirSupplier() {
    return createSupplier(true);
  }

  private File getStorageSubdirectory(File rootDir, int version) {
    return new File(rootDir, DefaultDiskStorage.getVersionSubdirectoryName(version));
  }

  @Test
  public void testGet_InternalCacheDir() throws Exception {
    File cacheDir = mContext.getCacheDir();

    DefaultDiskStorageSupplier supplier = createInternalCacheDirSupplier();

    // initial state
    Assert.assertNull(supplier.mCurrentState.storage);

    // after first initialization
    DiskStorage storage = supplier.get();
    Assert.assertEquals(storage, supplier.mCurrentState.storage);
    Assert.assertTrue(storage instanceof DefaultDiskStorage);

    File baseDir = new File(cacheDir, mBaseDirectoryName);
    Assert.assertTrue(baseDir.exists());
    Assert.assertTrue(getStorageSubdirectory(baseDir, 1).exists());

    // no change => should get back the same storage instance
    DiskStorage storage2 = supplier.get();
    Assert.assertEquals(storage, storage2);

    // root directory has been moved (proxy for delete). So we should get back a different instance
    File baseDirOrig = baseDir.getAbsoluteFile();
    Assert.assertTrue(baseDirOrig.renameTo(new File(cacheDir, "dummydir")));
    DiskStorage storage3 = supplier.get();
    Assert.assertNotSame(storage, storage3);
    Assert.assertTrue(storage3 instanceof DefaultDiskStorage);
    Assert.assertTrue(baseDir.exists());
    Assert.assertTrue(getStorageSubdirectory(baseDir, 1).exists());
  }

  @Test
  public void testGet_InternalFilesDir() throws Exception {
    File dir = mContext.getFilesDir();

    DefaultDiskStorageSupplier supplier = createInternalFilesDirSupplier();

    // initial state
    Assert.assertNull(supplier.mCurrentState.storage);

    // after first initialization
    DiskStorage storage = supplier.get();
    Assert.assertEquals(storage, supplier.mCurrentState.storage);
    Assert.assertTrue(storage instanceof DefaultDiskStorage);

    File baseDir = new File(dir, mBaseDirectoryName);
    Assert.assertTrue(baseDir.exists());
    Assert.assertTrue(getStorageSubdirectory(baseDir, 1).exists());

    // no change => should get back the same storage instance
    DiskStorage storage2 = supplier.get();
    Assert.assertEquals(storage, storage2);

    // root directory has been moved (proxy for delete). So we should get back a different instance
    File baseDirOrig = baseDir.getAbsoluteFile();
    Assert.assertTrue(baseDirOrig.renameTo(new File(dir, "dummydir")));
    DiskStorage storage3 = supplier.get();
    Assert.assertNotSame(storage, storage3);
    Assert.assertTrue(storage3 instanceof DefaultDiskStorage);
    Assert.assertTrue(baseDir.exists());
    Assert.assertTrue(getStorageSubdirectory(baseDir, 1).exists());
  }

  @Test
  public void testCreateRootDirectoryIfNecessary() throws Exception {
    DefaultDiskStorageSupplier supplier = createInternalCacheDirSupplier();
    Assert.assertNull(supplier.mCurrentState.storage);
    File baseDir = new File(mContext.getCacheDir(), mBaseDirectoryName);

    // directory is clean
    supplier.createRootDirectoryIfNecessary(baseDir);
    Assert.assertTrue(baseDir.exists());

    // cleanup
    FileTree.deleteRecursively(baseDir);

    // a file with the same name exists - this should clobber the file, and create a directory
    // instead
    File dummyFile = new File(mContext.getCacheDir(), mBaseDirectoryName);
    Assert.assertTrue(dummyFile.createNewFile());
    Assert.assertTrue(dummyFile.exists());
    supplier.createRootDirectoryIfNecessary(baseDir);
    Assert.assertTrue(baseDir.exists());
    Assert.assertTrue(baseDir.isDirectory());

    // cleanup
    FileTree.deleteRecursively(baseDir);

    // a directory with the same name exists - and with a file in it.
    // Everything should stay the same
    Assert.assertTrue(baseDir.mkdirs());
    File dummyFile2 = new File(baseDir, "dummy1");
    Assert.assertTrue(dummyFile2.createNewFile());
    Assert.assertTrue(dummyFile2.exists());
    supplier.createRootDirectoryIfNecessary(baseDir);
    Assert.assertTrue(dummyFile2.exists());
  }

  @Test
  public void testDeleteStorage() throws Exception {
    DefaultDiskStorageSupplier supplier = createInternalCacheDirSupplier();
    Assert.assertNull(supplier.mCurrentState.storage);
    supplier.deleteOldStorageIfNecessary();

    DefaultDiskStorage storage = (DefaultDiskStorage)supplier.get();
    File versionDir = getStorageSubdirectory(
        new File(mContext.getCacheDir(), mBaseDirectoryName),
        mVersion);
    Assert.assertTrue(versionDir.exists());
    File dummyFile = new File(versionDir, "dummy");
    Assert.assertTrue(dummyFile.createNewFile());
    Assert.assertTrue(dummyFile.exists());
    supplier.deleteOldStorageIfNecessary();
    Assert.assertFalse(dummyFile.exists());
    Assert.assertFalse(versionDir.exists());
    Assert.assertFalse(versionDir.getParentFile().exists());
  }

  @Test
  public void testCreateStorage() throws Exception {
    DefaultDiskStorageSupplier supplier = createInternalCacheDirSupplier();

    File baseDir = new File(mContext.getCacheDir(), mBaseDirectoryName);
    File versionDir = getStorageSubdirectory(
        baseDir,
        mVersion);

    Assert.assertFalse(versionDir.exists());
    Assert.assertFalse(baseDir.exists());
    supplier.get();
    Assert.assertTrue(baseDir.exists());
    Assert.assertTrue(versionDir.exists());
  }
}
