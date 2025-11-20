/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk

import com.facebook.binaryresource.FileBinaryResource
import com.facebook.cache.common.CacheErrorLogger
import com.facebook.cache.common.WriterCallback
import com.facebook.common.file.FileTree
import com.facebook.common.internal.Files
import com.facebook.common.internal.Supplier
import com.facebook.common.time.SystemClock
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.OutputStream
import java.util.concurrent.TimeUnit
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.MockedStatic
import org.mockito.Mockito.mockStatic
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Tests for 'default' disk storage */
@RunWith(RobolectricTestRunner::class)
class DefaultDiskStorageTest {

  private lateinit var directory: File
  private lateinit var clock: SystemClock
  private lateinit var mockedSystemClock: MockedStatic<SystemClock>

  @Before
  @Throws(Exception::class)
  fun before() {
    mockedSystemClock = mockStatic(SystemClock::class.java)
    clock = mock()
    mockedSystemClock.`when`<SystemClock> { SystemClock.get() }.thenReturn(clock)
    directory = File(RuntimeEnvironment.application.cacheDir, "sharded-disk-storage-test")
    assertThat(directory.mkdirs()).isTrue()
    FileTree.deleteContents(directory)
  }

  private fun getStorageSupplier(version: Int): Supplier<DefaultDiskStorage> {
    return Supplier { DefaultDiskStorage(directory, version, mock<CacheErrorLogger>()) }
  }

  @After
  fun tearDownStaticMocks() {
    mockedSystemClock.close()
  }

  @Test
  @Throws(Exception::class)
  fun testStartup() {
    // create a bogus file
    val bogusFile = File(directory, "bogus")
    assertThat(bogusFile.createNewFile()).isTrue()

    // create the storage now. Bogus files should be gone now
    var storage = getStorageSupplier(1).get()
    assertThat(bogusFile.exists()).isFalse()
    val version1Dir = DefaultDiskStorage.getVersionSubdirectoryName(1)
    assertThat(File(directory, version1Dir).exists()).isTrue()

    // create a new version
    storage = getStorageSupplier(2).get()
    assertThat(storage).isNotNull()
    assertThat(File(directory, version1Dir).exists()).isFalse()
    val version2Dir = DefaultDiskStorage.getVersionSubdirectoryName(2)
    assertThat(File(directory, version2Dir).exists()).isTrue()
  }

  @Test
  fun testIsEnabled() {
    val storage = getStorageSupplier(1).get()
    assertThat(storage.isEnabled).isTrue()
  }

  @Test
  @Throws(Exception::class)
  fun testBasicOperations() {
    val storage = getStorageSupplier(1).get()
    val resourceId1 = "R1"
    val resourceId2 = "R2"

    // no file - get should fail
    var resource1 = storage.getResource(resourceId1, Any())
    assertThat(resource1).isNull()

    // write out the file now
    val key1Contents = byteArrayOf(0, 1, 2)
    writeToStorage(storage, resourceId1, key1Contents)
    // get should succeed now
    resource1 = storage.getResource(resourceId1, Any())
    assertThat(resource1).isNotNull()
    val underlyingFile = (resource1 as FileBinaryResource).file
    assertThat(Files.toByteArray(underlyingFile)).containsExactly(*key1Contents)
    // remove the file now - get should fail again
    assertThat(underlyingFile.delete()).isTrue()
    resource1 = storage.getResource(resourceId1, Any())
    assertThat(resource1).isNull()
    // no file
    val resource2 = storage.getResource(resourceId2, Any())
    assertThat(resource2).isNull()
  }

  /**
   * Test that a file is stored in a new file, and the bytes are stored plainly in the file.
   *
   * @throws Exception
   */
  @Test
  @Throws(Exception::class)
  fun testStoreFile() {
    val storage = getStorageSupplier(1).get()
    val resourceId1 = "resource1"
    val value1 = ByteArray(100)
    value1[80] = 101
    val file1 = writeFileToStorage(storage, resourceId1, value1)

    val files = mutableSetOf<File>()
    assertThat(directory.exists()).isTrue()
    val founds1 = findNewFiles(directory, files, recurse = true)
    assertThat(file1).isNotNull()
    assertThat(founds1.contains(file1)).isTrue()
    assertThat(file1.exists()).isTrue()
    assertThat(file1.length()).isEqualTo(100)
    assertThat(Files.toByteArray(file1)).containsExactly(*value1)
  }

  /**
   * Inserts 3 files with different dates. Check what files are there. Uses an iterator to remove
   * the one in the middle. Check that later.
   *
   * @throws Exception
   */
  @Test
  @Throws(Exception::class)
  fun testRemoveWithIterator() {
    val storage = getStorageSupplier(1).get()

    val resourceId1 = "resource1"
    val value1 = ByteArray(100)
    value1[80] = 101
    val resourceId2 = "resource2"
    val value2 = ByteArray(104)
    value2[80] = 102
    val resourceId3 = "resource3"
    val value3 = ByteArray(106)
    value3[80] = 103

    writeFileToStorage(storage, resourceId1, value1)

    val time2 = 1000L
    whenever(clock.now()).thenReturn(time2)
    writeFileToStorage(storage, resourceId2, value2)

    whenever(clock.now()).thenReturn(2000L)
    writeFileToStorage(storage, resourceId3, value3)

    val files = findNewFiles(directory, emptySet(), recurse = true)

    // there should be 1 file per entry
    assertThat(files.size).isEqualTo(3)

    // now delete entry2
    val entries = storage.entries
    for (entry in entries) {
      if (Math.abs(entry.timestamp - time2) < 500) {
        storage.remove(entry)
      }
    }

    assertThat(storage.contains(resourceId2, Any())).isFalse()
    val remaining = findNewFiles(directory, emptySet(), recurse = true)

    // 2 entries remain
    assertThat(remaining.size).isEqualTo(2)

    // none of them with timestamp close to time2
    val entries1 = ArrayList(storage.entries)
    assertThat(entries1.size).isEqualTo(2)
    // first
    var entry = entries1[0]
    assertThat(Math.abs(entry.timestamp - time2) < 500).isFalse()
    // second
    entry = entries1[1]
    assertThat(Math.abs(entry.timestamp - time2) < 500).isFalse()
  }

  @Test
  @Throws(Exception::class)
  fun testTouch() {
    val storage = getStorageSupplier(1).get()
    val startTime = 0L

    val resourceId1 = "resource1"
    val value1 = ByteArray(100)
    val file1 = writeFileToStorage(storage, resourceId1, value1)
    assertThat(Math.abs(file1.lastModified() - startTime) <= 500).isTrue()

    val time2 = startTime + 10000
    whenever(clock.now()).thenReturn(time2)
    val resourceId2 = "resource2"
    val value2 = ByteArray(100)
    val file2 = writeFileToStorage(storage, resourceId2, value2)
    assertThat(Math.abs(file1.lastModified() - startTime) <= 500).isTrue()
    assertThat(Math.abs(file2.lastModified() - time2) <= 500).isTrue()

    val time3 = time2 + 10000
    whenever(clock.now()).thenReturn(time3)
    storage.touch(resourceId1, Any())
    assertThat(Math.abs(file1.lastModified() - time3) <= 500).isTrue()
    assertThat(Math.abs(file2.lastModified() - time2) <= 500).isTrue()
  }

  @Test
  @Throws(Exception::class)
  fun testRemoveById() {
    val storage = getStorageSupplier(1).get()

    val resourceId1 = "resource1"
    val value1 = ByteArray(100)
    writeFileToStorage(storage, resourceId1, value1)
    val resourceId2 = "resource2"
    val value2 = ByteArray(100)
    writeFileToStorage(storage, resourceId2, value2)
    val resourceId3 = "resource3"
    val value3 = ByteArray(100)
    writeFileToStorage(storage, resourceId3, value3)

    assertThat(storage.contains(resourceId1, Any())).isTrue()
    assertThat(storage.contains(resourceId2, Any())).isTrue()
    assertThat(storage.contains(resourceId3, Any())).isTrue()

    storage.remove(resourceId2)
    assertThat(storage.contains(resourceId1, Any())).isTrue()
    assertThat(storage.contains(resourceId2, Any())).isFalse()
    assertThat(storage.contains(resourceId3, Any())).isTrue()

    storage.remove(resourceId1)
    assertThat(storage.contains(resourceId1, Any())).isFalse()
    assertThat(storage.contains(resourceId2, Any())).isFalse()
    assertThat(storage.contains(resourceId3, Any())).isTrue()

    storage.remove(resourceId3)
    assertThat(storage.contains(resourceId1, Any())).isFalse()
    assertThat(storage.contains(resourceId2, Any())).isFalse()
    assertThat(storage.contains(resourceId3, Any())).isFalse()
  }

  @Test
  @Throws(Exception::class)
  fun testEntryIds() {
    val storage = getStorageSupplier(1).get()

    val value1 = ByteArray(101)
    val value2 = ByteArray(102)
    val value3 = ByteArray(103)
    value1[80] = 123
    value2[80] = 45
    value3[80] = 67
    writeFileToStorage(storage, "resourceId1", value1)
    writeFileToStorage(storage, "resourceId2", value2)
    writeFileToStorage(storage, "resourceId3", value3)

    // check that resources are retrieved by the right name, before testing getEntries
    val res1 = storage.getResource("resourceId1", Any())
    val res2 = storage.getResource("resourceId2", Any())
    val res3 = storage.getResource("resourceId3", Any())
    assertThat(res1?.read()).containsExactly(*value1)
    assertThat(res2?.read()).containsExactly(*value2)
    assertThat(res3?.read()).containsExactly(*value3)

    // obtain entries and sort by name
    val entries = ArrayList(storage.entries).sortedBy { it.id }

    assertThat(entries.size).isEqualTo(3)
    assertThat(entries[0].id).isEqualTo("resourceId1")
    assertThat(entries[1].id).isEqualTo("resourceId2")
    assertThat(entries[2].id).isEqualTo("resourceId3")
    assertThat(entries[0].resource.read()).containsExactly(*value1)
    assertThat(entries[1].resource.read()).containsExactly(*value2)
    assertThat(entries[2].resource.read()).containsExactly(*value3)
  }

  @Test
  @Throws(Exception::class)
  fun testEntryImmutable() {
    val storage = getStorageSupplier(1).get()

    val resourceId1 = "resource1"
    val value1 = ByteArray(100)
    value1[80] = 123
    val file1 = writeFileToStorage(storage, resourceId1, value1)

    assertThat(file1.length()).isEqualTo(100)
    val entries = storage.entries
    val entry = entries.first()
    val timestamp = entry.timestamp
    whenever(clock.now()).thenReturn(TimeUnit.HOURS.toMillis(1))
    storage.getResource(resourceId1, Any())

    // now the new timestamp show be higher, but the entry should have the same value
    val newEntries = storage.entries
    val newEntry = newEntries.first()
    assertThat(timestamp < newEntry.timestamp).isTrue()
    assertThat(entry.timestamp).isEqualTo(timestamp)
  }

  @Test
  @Throws(IOException::class)
  fun testTempFileEviction() {
    whenever(clock.now()).thenReturn(TimeUnit.DAYS.toMillis(1000))
    val storage = getStorageSupplier(1).get()

    val resourceId1 = "resource1"
    val inserter = storage.insert(resourceId1, Any())
    val tempFile = (inserter as DefaultDiskStorage.InserterImpl).mTemporaryFile

    // Make sure that we don't evict a recent temp file
    purgeUnexpectedFiles(storage)
    assertThat(tempFile.exists()).isTrue()

    // Mark it old, then try eviction again. It should be gone.
    if (!tempFile.setLastModified(clock.now() - DefaultDiskStorage.TEMP_FILE_LIFETIME_MS - 1000)) {
      throw IOException("Unable to update timestamp of file: $tempFile")
    }
    purgeUnexpectedFiles(storage)
    assertThat(tempFile.exists()).isFalse()
  }

  /**
   * Test that purgeUnexpectedResources deletes all files/directories outside the version directory
   * but leaves untouched the version directory and the content files.
   *
   * @throws Exception
   */
  @Test
  @Throws(Exception::class)
  fun testPurgeUnexpectedFiles() {
    val storage = getStorageSupplier(1).get()

    val resourceId = "file1"
    val CONTENT = "content".toByteArray(Charsets.UTF_8)

    val file = writeFileToStorage(storage, resourceId, CONTENT)

    // check file exists
    assertThat(file.exists()).isTrue()
    assertThat(Files.toByteArray(file)).containsExactly(*CONTENT)

    val unexpectedFile1 = File(directory, "unexpected-file-1")
    val unexpectedFile2 = File(directory, "unexpected-file-2")

    assertThat(unexpectedFile1.createNewFile()).isTrue()
    assertThat(unexpectedFile2.createNewFile()).isTrue()

    val unexpectedDir1 = File(directory, "unexpected-dir-1")
    assertThat(unexpectedDir1.mkdirs()).isTrue()

    val unexpectedDir2 = File(directory, "unexpected-dir-2")
    assertThat(unexpectedDir2.mkdirs()).isTrue()
    val unexpectedSubfile1 = File(unexpectedDir2, "unexpected-sub-file-1")
    assertThat(unexpectedSubfile1.createNewFile()).isTrue()

    assertThat(directory.listFiles()?.size).isEqualTo(5) // 4 unexpected (files+dirs) + ver. dir
    assertThat(unexpectedDir2.listFiles()?.size).isEqualTo(1)
    assertThat(unexpectedDir1.listFiles()?.size).isEqualTo(0)

    val unexpectedFileInShard = File(file.parentFile, "unexpected-in-shard")
    assertThat(unexpectedFileInShard.createNewFile()).isTrue()

    storage.purgeUnexpectedResources()
    assertThat(unexpectedFile1.exists()).isFalse()
    assertThat(unexpectedFile2.exists()).isFalse()
    assertThat(unexpectedSubfile1.exists()).isFalse()
    assertThat(unexpectedDir1.exists()).isFalse()
    assertThat(unexpectedDir2.exists()).isFalse()

    // check file still exists
    assertThat(file.exists()).isTrue()
    // check unexpected sibling is gone
    assertThat(unexpectedFileInShard.exists()).isFalse()
    // check the only thing in root is the version directory
    assertThat(directory.listFiles()?.size).isEqualTo(1) // just the version directory
  }

  /**
   * Tests that an existing directory is nuked when it's not current version (doens't have the
   * version directory used for the structure)
   *
   * @throws Exception
   */
  @Test
  @Throws(Exception::class)
  fun testDirectoryIsNuked() {
    assertThat(directory.listFiles()?.size).isEqualTo(0)

    // create file before setting final test date
    assertThat(File(directory, "something-arbitrary").createNewFile()).isTrue()

    val lastModified = directory.lastModified() - 1000
    // some previous date to the "now" used for file creation
    assertThat(directory.setLastModified(lastModified)).isTrue()
    // check it was changed
    assertThat(directory.lastModified()).isEqualTo(lastModified)

    getStorageSupplier(1).get()

    // directory exists...
    assertThat(directory.exists()).isTrue()
    // but it was created now
    assertThat(lastModified < directory.lastModified()).isTrue()
  }

  /**
   * Tests that an existing directory is not nuked if the version directory used for the structure
   * exists (so it's current version and doesn't suffer Samsung RFS problem)
   *
   * @throws Exception
   */
  @Test
  @Throws(Exception::class)
  fun testDirectoryIsNotNuked() {
    assertThat(directory.listFiles()?.size).isEqualTo(0)

    val storage = getStorageSupplier(1).get()
    val resourceId = "file1"

    val CONTENT = "content".toByteArray(Charsets.UTF_8)

    // create a file so we know version directory really exists
    val inserter = storage.insert(resourceId, Any())
    writeToResource(inserter, CONTENT)
    inserter.commit(Any())

    // assign some previous date to the "now" used for file creation
    val lastModified = directory.lastModified() - 1000
    assertThat(directory.setLastModified(lastModified)).isTrue()
    // check it was changed
    assertThat(directory.lastModified()).isEqualTo(lastModified)

    // create again, it shouldn't delete the directory
    getStorageSupplier(1).get()

    // directory exists...
    assertThat(directory.exists()).isTrue()
    // and it's the same as before
    assertThat(directory.lastModified()).isEqualTo(lastModified)
  }

  /**
   * Test the iterator returned is ok and deletion through the iterator is ok too. This is the
   * required functionality that eviction needs.
   *
   * @throws Exception
   */
  @Test
  @Throws(Exception::class)
  fun testIterationAndRemoval() {
    val storage = getStorageSupplier(1).get()
    val resourceId0 = "file0"
    val resourceId1 = "file1"
    val resourceId2 = "file2"
    val resourceId3 = "file3"

    val CONTENT0 = "content0".toByteArray(Charsets.UTF_8)
    val CONTENT1 = "content1-bigger".toByteArray(Charsets.UTF_8)
    val CONTENT2 = "content2".toByteArray(Charsets.UTF_8)
    val CONTENT3 = "content3-biggest".toByteArray(Charsets.UTF_8)

    val files = ArrayList<File>(4)
    files.add(write(storage, resourceId0, CONTENT0))
    whenever(clock.now()).thenReturn(1000L)
    files.add(write(storage, resourceId1, CONTENT1))
    whenever(clock.now()).thenReturn(2000L)
    files.add(write(storage, resourceId2, CONTENT2))
    whenever(clock.now()).thenReturn(3000L)
    files.add(write(storage, resourceId3, CONTENT3))

    val entries = retrieveEntries(storage)
    assertThat(entries.size).isEqualTo(4)
    assertThat(entries[0].resource.file).isEqualTo(files[0])
    assertThat(entries[1].resource.file).isEqualTo(files[1])
    assertThat(entries[2].resource.file).isEqualTo(files[2])
    assertThat(entries[3].resource.file).isEqualTo(files[3])

    // try the same after removing 2 entries
    for (entry in storage.entries) {
      // delete the 2 biggest files: key1 and key3 (see the content values)
      if (entry.size >= CONTENT1.size.toLong()) {
        storage.remove(entry)
      }
    }

    val entriesAfterRemoval = retrieveEntries(storage)
    assertThat(entriesAfterRemoval.size).isEqualTo(2)
    assertThat(entriesAfterRemoval[0].resource.file).isEqualTo(files[0])
    assertThat(entriesAfterRemoval[1].resource.file).isEqualTo(files[2])
  }

  private fun purgeUnexpectedFiles(storage: DefaultDiskStorage) {
    storage.purgeUnexpectedResources()
  }

  private fun findNewFiles(directory: File, existing: Set<File>, recurse: Boolean): List<File> {
    val result = mutableListOf<File>()
    findNewFiles(directory, existing, recurse, result)
    return result
  }

  private fun findNewFiles(
      directory: File,
      existing: Set<File>,
      recurse: Boolean,
      result: MutableList<File>,
  ) {
    val files = directory.listFiles()
    if (files != null) {
      for (file in files) {
        if (file.isDirectory && recurse) {
          findNewFiles(file, existing, true, result)
        } else if (!existing.contains(file)) {
          result.add(file)
        }
      }
    }
  }

  companion object {
    @Throws(IOException::class)
    private fun writeToStorage(
        storage: DefaultDiskStorage,
        resourceId: String,
        value: ByteArray,
    ): FileBinaryResource {
      val inserter = storage.insert(resourceId, Any())
      writeToResource(inserter, value)
      return inserter.commit(Any()) as FileBinaryResource
    }

    @Throws(IOException::class)
    private fun writeFileToStorage(
        storage: DefaultDiskStorage,
        resourceId: String,
        value: ByteArray,
    ): File {
      return writeToStorage(storage, resourceId, value).file
    }

    @Throws(IOException::class)
    private fun write(storage: DefaultDiskStorage, resourceId: String, content: ByteArray): File {
      val inserter = storage.insert(resourceId, Any())
      val file = (inserter as DefaultDiskStorage.InserterImpl).mTemporaryFile
      FileOutputStream(file).use { fos -> fos.write(content) }
      return (inserter.commit(Any()) as FileBinaryResource).file
    }

    @Throws(IOException::class)
    private fun writeToResource(inserter: DiskStorage.Inserter, content: ByteArray) {
      inserter.writeData(
          object : WriterCallback {
            @Throws(IOException::class)
            override fun write(os: OutputStream) {
              os.write(content)
            }
          },
          Any(),
      )
    }

    /**
     * Retrieves a list of entries (the one returned by DiskStorage.Session.entriesIterator) ordered
     * by timestamp.
     *
     * @param storage
     */
    @Throws(IOException::class)
    private fun retrieveEntries(storage: DefaultDiskStorage): List<DefaultDiskStorage.EntryImpl> {
      val entries = ArrayList(storage.entries)

      entries.sortWith { a, b ->
        val al = a.timestamp
        val bl = b.timestamp
        when {
          al < bl -> -1
          al > bl -> 1
          else -> 0
        }
      }
      val newEntries = ArrayList<DefaultDiskStorage.EntryImpl>()
      for (entry in entries) {
        newEntries.add(entry as DefaultDiskStorage.EntryImpl)
      }
      return newEntries
    }
  }
}
