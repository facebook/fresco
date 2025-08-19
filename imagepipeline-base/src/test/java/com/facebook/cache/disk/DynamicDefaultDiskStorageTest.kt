/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk

import android.content.Context
import com.facebook.cache.common.CacheErrorLogger
import com.facebook.common.file.FileTree
import com.facebook.common.internal.Suppliers
import java.io.File
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Test out methods in DynamicDefaultDiskStorage */
@RunWith(RobolectricTestRunner::class)
class DynamicDefaultDiskStorageTest {

  private var version: Int = 0
  private lateinit var baseDirectoryName: String
  private lateinit var cacheErrorLogger: CacheErrorLogger
  private lateinit var context: Context

  @Before
  fun setUp() {
    context = RuntimeEnvironment.application.applicationContext
    version = 1
    baseDirectoryName = "base"
    cacheErrorLogger = Mockito.mock(CacheErrorLogger::class.java)
  }

  private fun createStorage(useFilesDirInsteadOfCacheDir: Boolean): DynamicDefaultDiskStorage {
    return DynamicDefaultDiskStorage(
        version,
        if (useFilesDirInsteadOfCacheDir) {
          Suppliers.of(context.filesDir)
        } else {
          Suppliers.of(context.cacheDir)
        },
        baseDirectoryName,
        cacheErrorLogger,
    )
  }

  private fun createInternalCacheDirStorage(): DynamicDefaultDiskStorage {
    return createStorage(false)
  }

  private fun createInternalFilesDirStorage(): DynamicDefaultDiskStorage {
    return createStorage(true)
  }

  @Test
  fun testGet_InternalCacheDir() {
    val cacheDir = context.cacheDir

    val storage = createInternalCacheDirStorage()

    // initial state
    assertThat(storage.mCurrentState.delegate).isNull()

    // after first initialization
    val delegate = storage.get()
    assertThat(storage.mCurrentState.delegate).isEqualTo(delegate)
    assertThat(delegate).isInstanceOf(DefaultDiskStorage::class.java)

    val baseDir = File(cacheDir, baseDirectoryName)
    assertThat(baseDir.exists()).isTrue()
    assertThat(getStorageSubdirectory(baseDir, 1).exists()).isTrue()

    // no change => should get back the same storage instance
    val storage2 = storage.get()
    assertThat(delegate).isEqualTo(storage2)

    // root directory has been moved (proxy for delete). So we should get back a different instance
    val baseDirOrig = baseDir.absoluteFile
    assertThat(baseDirOrig.renameTo(File(cacheDir, "dummydir"))).isTrue()
    val storage3 = storage.get()
    assertThat(storage3).isNotSameAs(delegate)
    assertThat(storage3).isInstanceOf(DefaultDiskStorage::class.java)
    assertThat(baseDir.exists()).isTrue()
    assertThat(getStorageSubdirectory(baseDir, 1).exists()).isTrue()
  }

  @Test
  fun testGet_InternalFilesDir() {
    val dir = context.filesDir

    val supplier = createInternalFilesDirStorage()

    // initial state
    assertThat(supplier.mCurrentState.delegate).isNull()

    // after first initialization
    val storage = supplier.get()
    assertThat(supplier.mCurrentState.delegate).isEqualTo(storage)
    assertThat(storage).isInstanceOf(DefaultDiskStorage::class.java)

    val baseDir = File(dir, baseDirectoryName)
    assertThat(baseDir.exists()).isTrue()
    assertThat(getStorageSubdirectory(baseDir, 1).exists()).isTrue()

    // no change => should get back the same storage instance
    val storage2 = supplier.get()
    assertThat(storage).isEqualTo(storage2)

    // root directory has been moved (proxy for delete). So we should get back a different instance
    val baseDirOrig = baseDir.absoluteFile
    assertThat(baseDirOrig.renameTo(File(dir, "dummydir"))).isTrue()
    val storage3 = supplier.get()
    assertThat(storage3).isNotSameAs(storage)
    assertThat(storage3).isInstanceOf(DefaultDiskStorage::class.java)
    assertThat(baseDir.exists()).isTrue()
    assertThat(getStorageSubdirectory(baseDir, 1).exists()).isTrue()
  }

  @Test
  fun testCreateRootDirectoryIfNecessary() {
    val supplier = createInternalCacheDirStorage()
    assertThat(supplier.mCurrentState.delegate).isNull()
    val baseDir = File(context.cacheDir, baseDirectoryName)

    // directory is clean
    supplier.createRootDirectoryIfNecessary(baseDir)
    assertThat(baseDir.exists()).isTrue()

    // cleanup
    FileTree.deleteRecursively(baseDir)

    // a file with the same name exists - this should clobber the file, and create a directory
    // instead
    val dummyFile = File(context.cacheDir, baseDirectoryName)
    assertThat(dummyFile.createNewFile()).isTrue()
    assertThat(dummyFile.exists()).isTrue()
    supplier.createRootDirectoryIfNecessary(baseDir)
    assertThat(baseDir.exists()).isTrue()
    assertThat(baseDir.isDirectory).isTrue()

    // cleanup
    FileTree.deleteRecursively(baseDir)

    // a directory with the same name exists - and with a file in it.
    // Everything should stay the same
    assertThat(baseDir.mkdirs()).isTrue()
    val dummyFile2 = File(baseDir, "dummy1")
    assertThat(dummyFile2.createNewFile()).isTrue()
    assertThat(dummyFile2.exists()).isTrue()
    supplier.createRootDirectoryIfNecessary(baseDir)
    assertThat(dummyFile2.exists()).isTrue()
  }

  @Test
  fun testDeleteStorage() {
    val storage = createInternalCacheDirStorage()
    assertThat(storage.mCurrentState.delegate).isNull()
    storage.deleteOldStorageIfNecessary()

    storage.get()
    val versionDir = getStorageSubdirectory(File(context.cacheDir, baseDirectoryName), version)
    assertThat(versionDir.exists()).isTrue()
    val dummyFile = File(versionDir, "dummy")
    assertThat(dummyFile.createNewFile()).isTrue()
    assertThat(dummyFile.exists()).isTrue()
    storage.deleteOldStorageIfNecessary()
    assertThat(dummyFile.exists()).isFalse()
    assertThat(versionDir.exists()).isFalse()
    assertThat(versionDir.parentFile?.exists() == true).isFalse()
  }

  @Test
  fun testCreateStorage() {
    val storage = createInternalCacheDirStorage()

    val baseDir = File(context.cacheDir, baseDirectoryName)
    val versionDir = getStorageSubdirectory(baseDir, version)

    assertThat(versionDir.exists()).isFalse()
    assertThat(baseDir.exists()).isFalse()
    storage.get()
    assertThat(baseDir.exists()).isTrue()
    assertThat(versionDir.exists()).isTrue()
  }

  companion object {
    private fun getStorageSubdirectory(rootDir: File, version: Int): File {
      return File(rootDir, DefaultDiskStorage.getVersionSubdirectoryName(version))
    }
  }
}
