/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import com.facebook.binaryresource.BinaryResource;
import com.facebook.binaryresource.FileBinaryResource;
import com.facebook.cache.common.CacheErrorLogger;
import com.facebook.cache.common.WriterCallback;
import com.facebook.common.file.FileTree;
import com.facebook.common.internal.Files;
import com.facebook.common.internal.Supplier;
import com.facebook.common.time.SystemClock;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockedStatic;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

/** Tests for 'default' disk storage */
@RunWith(RobolectricTestRunner.class)
public class DefaultDiskStorageTest {

  private File mDirectory;
  private SystemClock mClock;
  private MockedStatic<SystemClock> mockedSystemClock;

  @Before
  public void before() throws Exception {
    mockedSystemClock = mockStatic(SystemClock.class);
    mClock = mock(SystemClock.class);
    mockedSystemClock.when(() -> SystemClock.get()).thenReturn(mClock);
    mDirectory =
        new File(RuntimeEnvironment.application.getCacheDir(), "sharded-disk-storage-test");
    assertThat(mDirectory.mkdirs()).isTrue();
    FileTree.deleteContents(mDirectory);
  }

  private Supplier<DefaultDiskStorage> getStorageSupplier(final int version) {
    return new Supplier<DefaultDiskStorage>() {
      @Override
      public DefaultDiskStorage get() {
        return new DefaultDiskStorage(mDirectory, version, mock(CacheErrorLogger.class));
      }
    };
  }

  @After
  public void tearDownStaticMocks() {
    mockedSystemClock.close();
  }

  @Test
  public void testStartup() throws Exception {
    // create a bogus file
    File bogusFile = new File(mDirectory, "bogus");
    assertThat(bogusFile.createNewFile()).isTrue();

    // create the storage now. Bogus files should be gone now
    DefaultDiskStorage storage = getStorageSupplier(1).get();
    assertThat(bogusFile.exists()).isFalse();
    String version1Dir = DefaultDiskStorage.getVersionSubdirectoryName(1);
    assertThat(new File(mDirectory, version1Dir).exists()).isTrue();

    // create a new version
    storage = getStorageSupplier(2).get();
    assertThat(storage).isNotNull();
    assertThat(new File(mDirectory, version1Dir).exists()).isFalse();
    String version2Dir = DefaultDiskStorage.getVersionSubdirectoryName(2);
    assertThat(new File(mDirectory, version2Dir).exists()).isTrue();
  }

  @Test
  public void testIsEnabled() {
    DefaultDiskStorage storage = getStorageSupplier(1).get();
    assertThat(storage.isEnabled()).isTrue();
  }

  @Test
  public void testBasicOperations() throws Exception {
    DefaultDiskStorage storage = getStorageSupplier(1).get();
    final String resourceId1 = "R1";
    final String resourceId2 = "R2";

    // no file - get should fail
    BinaryResource resource1 = storage.getResource(resourceId1, null);
    assertThat(resource1).isNull();

    // write out the file now
    byte[] key1Contents = new byte[] {0, 1, 2};
    writeToStorage(storage, resourceId1, key1Contents);
    // get should succeed now
    resource1 = storage.getResource(resourceId1, null);
    assertThat(resource1).isNotNull();
    File underlyingFile = ((FileBinaryResource) resource1).getFile();
    assertThat(Files.toByteArray(underlyingFile)).containsExactly(key1Contents);
    // remove the file now - get should fail again
    assertThat(underlyingFile.delete()).isTrue();
    resource1 = storage.getResource(resourceId1, null);
    assertThat(resource1).isNull();
    // no file
    BinaryResource resource2 = storage.getResource(resourceId2, null);
    assertThat(resource2).isNull();
  }

  /**
   * Test that a file is stored in a new file, and the bytes are stored plainly in the file.
   *
   * @throws Exception
   */
  @Test
  public void testStoreFile() throws Exception {
    DefaultDiskStorage storage = getStorageSupplier(1).get();
    final String resourceId1 = "resource1";
    final byte[] value1 = new byte[100];
    value1[80] = 101;
    File file1 = writeFileToStorage(storage, resourceId1, value1);

    Set<File> files = new HashSet<>();
    assertThat(mDirectory.exists()).isTrue();
    List<File> founds1 = findNewFiles(mDirectory, files, /*recurse*/ true);
    assertThat(file1).isNotNull();
    assertThat(founds1.contains(file1)).isTrue();
    assertThat(file1.exists()).isTrue();
    assertThat(file1.length()).isEqualTo(100);
    assertThat(Files.toByteArray(file1)).containsExactly(value1);
  }

  /**
   * Inserts 3 files with different dates. Check what files are there. Uses an iterator to remove
   * the one in the middle. Check that later.
   *
   * @throws Exception
   */
  @Test
  public void testRemoveWithIterator() throws Exception {
    DefaultDiskStorage storage = getStorageSupplier(1).get();

    final String resourceId1 = "resource1";
    final byte[] value1 = new byte[100];
    value1[80] = 101;
    final String resourceId2 = "resource2";
    final byte[] value2 = new byte[104];
    value2[80] = 102;
    final String resourceId3 = "resource3";
    final byte[] value3 = new byte[106];
    value3[80] = 103;

    writeFileToStorage(storage, resourceId1, value1);

    final long time2 = 1000L;
    when(mClock.now()).thenReturn(time2);
    writeFileToStorage(storage, resourceId2, value2);

    when(mClock.now()).thenReturn(2000L);
    writeFileToStorage(storage, resourceId3, value3);

    List<File> files = findNewFiles(mDirectory, Collections.<File>emptySet(), /*recurse*/ true);

    // there should be 1 file per entry
    assertThat(files.size()).isEqualTo(3);

    // now delete entry2
    Collection<DiskStorage.Entry> entries = storage.getEntries();
    for (DiskStorage.Entry entry : entries) {
      if (Math.abs(entry.getTimestamp() - time2) < 500) {
        storage.remove(entry);
      }
    }

    assertThat(storage.contains(resourceId2, null)).isFalse();
    List<File> remaining = findNewFiles(mDirectory, Collections.<File>emptySet(), /*recurse*/ true);

    // 2 entries remain
    assertThat(remaining.size()).isEqualTo(2);

    // none of them with timestamp close to time2
    List<DiskStorage.Entry> entries1 = new ArrayList<>(storage.getEntries());
    assertThat(entries1.size()).isEqualTo(2);
    // first
    DiskStorage.Entry entry = entries1.get(0);
    assertThat(Math.abs(entry.getTimestamp() - time2) < 500).isFalse();
    // second
    entry = entries1.get(1);
    assertThat(Math.abs(entry.getTimestamp() - time2) < 500).isFalse();
  }

  @Test
  public void testTouch() throws Exception {
    DefaultDiskStorage storage = getStorageSupplier(1).get();
    final long startTime = 0;

    final String resourceId1 = "resource1";
    final byte[] value1 = new byte[100];
    final File file1 = writeFileToStorage(storage, resourceId1, value1);
    assertThat(Math.abs(file1.lastModified() - startTime) <= 500).isTrue();

    final long time2 = startTime + 10000;
    when(mClock.now()).thenReturn(time2);
    final String resourceId2 = "resource2";
    final byte[] value2 = new byte[100];
    final File file2 = writeFileToStorage(storage, resourceId2, value2);
    assertThat(Math.abs(file1.lastModified() - startTime) <= 500).isTrue();
    assertThat(Math.abs(file2.lastModified() - time2) <= 500).isTrue();

    final long time3 = time2 + 10000;
    when(mClock.now()).thenReturn(time3);
    storage.touch(resourceId1, null);
    assertThat(Math.abs(file1.lastModified() - time3) <= 500).isTrue();
    assertThat(Math.abs(file2.lastModified() - time2) <= 500).isTrue();
  }

  @Test
  public void testRemoveById() throws Exception {
    final DefaultDiskStorage storage = getStorageSupplier(1).get();

    final String resourceId1 = "resource1";
    final byte[] value1 = new byte[100];
    writeFileToStorage(storage, resourceId1, value1);
    final String resourceId2 = "resource2";
    final byte[] value2 = new byte[100];
    writeFileToStorage(storage, resourceId2, value2);
    final String resourceId3 = "resource3";
    final byte[] value3 = new byte[100];
    writeFileToStorage(storage, resourceId3, value3);

    assertThat(storage.contains(resourceId1, null)).isTrue();
    assertThat(storage.contains(resourceId2, null)).isTrue();
    assertThat(storage.contains(resourceId3, null)).isTrue();

    storage.remove(resourceId2);
    assertThat(storage.contains(resourceId1, null)).isTrue();
    assertThat(storage.contains(resourceId2, null)).isFalse();
    assertThat(storage.contains(resourceId3, null)).isTrue();

    storage.remove(resourceId1);
    assertThat(storage.contains(resourceId1, null)).isFalse();
    assertThat(storage.contains(resourceId2, null)).isFalse();
    assertThat(storage.contains(resourceId3, null)).isTrue();

    storage.remove(resourceId3);
    assertThat(storage.contains(resourceId1, null)).isFalse();
    assertThat(storage.contains(resourceId2, null)).isFalse();
    assertThat(storage.contains(resourceId3, null)).isFalse();
  }

  @Test
  public void testEntryIds() throws Exception {
    DefaultDiskStorage storage = getStorageSupplier(1).get();

    final byte[] value1 = new byte[101];
    final byte[] value2 = new byte[102];
    final byte[] value3 = new byte[103];
    value1[80] = 123;
    value2[80] = 45;
    value3[80] = 67;
    writeFileToStorage(storage, "resourceId1", value1);
    writeFileToStorage(storage, "resourceId2", value2);
    writeFileToStorage(storage, "resourceId3", value3);

    // check that resources are retrieved by the right name, before testing getEntries
    BinaryResource res1 = storage.getResource("resourceId1", null);
    BinaryResource res2 = storage.getResource("resourceId2", null);
    BinaryResource res3 = storage.getResource("resourceId3", null);
    assertThat(res1.read()).containsExactly(value1);
    assertThat(res2.read()).containsExactly(value2);
    assertThat(res3.read()).containsExactly(value3);

    // obtain entries and sort by name
    List<DiskStorage.Entry> entries = new ArrayList<>(storage.getEntries());
    Collections.sort(
        entries,
        new Comparator<DiskStorage.Entry>() {
          @Override
          public int compare(DiskStorage.Entry lhs, DiskStorage.Entry rhs) {
            return lhs.getId().compareTo(rhs.getId());
          }
        });

    assertThat(entries.size()).isEqualTo(3);
    assertThat(entries.get(0).getId()).isEqualTo("resourceId1");
    assertThat(entries.get(1).getId()).isEqualTo("resourceId2");
    assertThat(entries.get(2).getId()).isEqualTo("resourceId3");
    assertThat(entries.get(0).getResource().read()).containsExactly(value1);
    assertThat(entries.get(1).getResource().read()).containsExactly(value2);
    assertThat(entries.get(2).getResource().read()).containsExactly(value3);
  }

  @Test
  public void testEntryImmutable() throws Exception {
    DefaultDiskStorage storage = getStorageSupplier(1).get();

    final String resourceId1 = "resource1";
    final byte[] value1 = new byte[100];
    value1[80] = 123;
    final File file1 = writeFileToStorage(storage, resourceId1, value1);

    assertThat(file1.length()).isEqualTo(100);
    List<DiskStorage.Entry> entries = storage.getEntries();
    DiskStorage.Entry entry = entries.get(0);
    long timestamp = entry.getTimestamp();
    when(mClock.now()).thenReturn(TimeUnit.HOURS.toMillis(1));
    storage.getResource(resourceId1, null);

    // now the new timestamp show be higher, but the entry should have the same value
    List<DiskStorage.Entry> newEntries = storage.getEntries();
    DiskStorage.Entry newEntry = newEntries.get(0);
    assertThat(timestamp < newEntry.getTimestamp()).isTrue();
    assertThat(entry.getTimestamp()).isEqualTo(timestamp);
  }

  @Test
  public void testTempFileEviction() throws IOException {
    when(mClock.now()).thenReturn(TimeUnit.DAYS.toMillis(1000));
    DefaultDiskStorage storage = getStorageSupplier(1).get();

    final String resourceId1 = "resource1";
    DiskStorage.Inserter inserter = storage.insert(resourceId1, null);
    final File tempFile = ((DefaultDiskStorage.InserterImpl) inserter).mTemporaryFile;

    // Make sure that we don't evict a recent temp file
    purgeUnexpectedFiles(storage);
    assertThat(tempFile.exists()).isTrue();

    // Mark it old, then try eviction again. It should be gone.
    if (!tempFile.setLastModified(mClock.now() - DefaultDiskStorage.TEMP_FILE_LIFETIME_MS - 1000)) {
      throw new IOException("Unable to update timestamp of file: " + tempFile);
    }
    purgeUnexpectedFiles(storage);
    assertThat(tempFile.exists()).isFalse();
  }

  /**
   * Test that purgeUnexpectedResources deletes all files/directories outside the version directory
   * but leaves untouched the version directory and the content files.
   *
   * @throws Exception
   */
  @Test
  public void testPurgeUnexpectedFiles() throws Exception {
    final DefaultDiskStorage storage = getStorageSupplier(1).get();

    final String resourceId = "file1";
    final byte[] CONTENT = "content".getBytes("UTF-8");

    File file = writeFileToStorage(storage, resourceId, CONTENT);

    // check file exists
    assertThat(file.exists()).isTrue();
    assertThat(Files.toByteArray(file)).containsExactly(CONTENT);

    final File unexpectedFile1 = new File(mDirectory, "unexpected-file-1");
    final File unexpectedFile2 = new File(mDirectory, "unexpected-file-2");

    assertThat(unexpectedFile1.createNewFile()).isTrue();
    assertThat(unexpectedFile2.createNewFile()).isTrue();

    final File unexpectedDir1 = new File(mDirectory, "unexpected-dir-1");
    assertThat(unexpectedDir1.mkdirs()).isTrue();

    final File unexpectedDir2 = new File(mDirectory, "unexpected-dir-2");
    assertThat(unexpectedDir2.mkdirs()).isTrue();
    final File unexpectedSubfile1 = new File(unexpectedDir2, "unexpected-sub-file-1");
    assertThat(unexpectedSubfile1.createNewFile()).isTrue();

    assertThat(mDirectory.listFiles().length).isEqualTo(5); // 4 unexpected (files+dirs) + ver. dir
    assertThat(unexpectedDir2.listFiles().length).isEqualTo(1);
    assertThat(unexpectedDir1.listFiles().length).isEqualTo(0);

    File unexpectedFileInShard = new File(file.getParentFile(), "unexpected-in-shard");
    assertThat(unexpectedFileInShard.createNewFile()).isTrue();

    storage.purgeUnexpectedResources();
    assertThat(unexpectedFile1.exists()).isFalse();
    assertThat(unexpectedFile2.exists()).isFalse();
    assertThat(unexpectedSubfile1.exists()).isFalse();
    assertThat(unexpectedDir1.exists()).isFalse();
    assertThat(unexpectedDir2.exists()).isFalse();

    // check file still exists
    assertThat(file.exists()).isTrue();
    // check unexpected sibling is gone
    assertThat(unexpectedFileInShard.exists()).isFalse();
    // check the only thing in root is the version directory
    assertThat(mDirectory.listFiles().length).isEqualTo(1); // just the version directory
  }

  /**
   * Tests that an existing directory is nuked when it's not current version (doens't have the
   * version directory used for the structure)
   *
   * @throws Exception
   */
  @Test
  public void testDirectoryIsNuked() throws Exception {
    assertThat(mDirectory.listFiles().length).isEqualTo(0);

    // create file before setting final test date
    assertThat(new File(mDirectory, "something-arbitrary").createNewFile()).isTrue();

    long lastModified = mDirectory.lastModified() - 1000;
    // some previous date to the "now" used for file creation
    assertThat(mDirectory.setLastModified(lastModified)).isTrue();
    // check it was changed
    assertThat(mDirectory.lastModified()).isEqualTo(lastModified);

    getStorageSupplier(1).get();

    // mDirectory exists...
    assertThat(mDirectory.exists()).isTrue();
    // but it was created now
    assertThat(lastModified < mDirectory.lastModified()).isTrue();
  }

  /**
   * Tests that an existing directory is not nuked if the version directory used for the structure
   * exists (so it's current version and doesn't suffer Samsung RFS problem)
   *
   * @throws Exception
   */
  @Test
  public void testDirectoryIsNotNuked() throws Exception {
    assertThat(mDirectory.listFiles().length).isEqualTo(0);

    final DefaultDiskStorage storage = getStorageSupplier(1).get();
    final String resourceId = "file1";

    final byte[] CONTENT = "content".getBytes("UTF-8");

    // create a file so we know version directory really exists
    DiskStorage.Inserter inserter = storage.insert(resourceId, null);
    writeToResource(inserter, CONTENT);
    inserter.commit(null);

    // assign some previous date to the "now" used for file creation
    long lastModified = mDirectory.lastModified() - 1000;
    assertThat(mDirectory.setLastModified(lastModified)).isTrue();
    // check it was changed
    assertThat(mDirectory.lastModified()).isEqualTo(lastModified);

    // create again, it shouldn't delete the directory
    getStorageSupplier(1).get();

    // mDirectory exists...
    assertThat(mDirectory.exists()).isTrue();
    // and it's the same as before
    assertThat(mDirectory.lastModified()).isEqualTo(lastModified);
  }

  /**
   * Test the iterator returned is ok and deletion through the iterator is ok too. This is the
   * required functionality that eviction needs.
   *
   * @throws Exception
   */
  @Test
  public void testIterationAndRemoval() throws Exception {
    DefaultDiskStorage storage = getStorageSupplier(1).get();
    final String resourceId0 = "file0";
    final String resourceId1 = "file1";
    final String resourceId2 = "file2";
    final String resourceId3 = "file3";

    final byte[] CONTENT0 = "content0".getBytes("UTF-8");
    final byte[] CONTENT1 = "content1-bigger".getBytes("UTF-8");
    final byte[] CONTENT2 = "content2".getBytes("UTF-8");
    final byte[] CONTENT3 = "content3-biggest".getBytes("UTF-8");

    List<File> files = new ArrayList<>(4);
    files.add(write(storage, resourceId0, CONTENT0));
    when(mClock.now()).thenReturn(1000L);
    files.add(write(storage, resourceId1, CONTENT1));
    when(mClock.now()).thenReturn(2000L);
    files.add(write(storage, resourceId2, CONTENT2));
    when(mClock.now()).thenReturn(3000L);
    files.add(write(storage, resourceId3, CONTENT3));

    List<DefaultDiskStorage.EntryImpl> entries = retrieveEntries(storage);
    assertThat(entries.size()).isEqualTo(4);
    assertThat(entries.get(0).getResource().getFile()).isEqualTo(files.get(0));
    assertThat(entries.get(1).getResource().getFile()).isEqualTo(files.get(1));
    assertThat(entries.get(2).getResource().getFile()).isEqualTo(files.get(2));
    assertThat(entries.get(3).getResource().getFile()).isEqualTo(files.get(3));

    // try the same after removing 2 entries
    for (DiskStorage.Entry entry : storage.getEntries()) {
      // delete the 2 biggest files: key1 and key3 (see the content values)
      if (entry.getSize() >= CONTENT1.length) {
        storage.remove(entry);
      }
    }

    List<DefaultDiskStorage.EntryImpl> entriesAfterRemoval = retrieveEntries(storage);
    assertThat(entriesAfterRemoval.size()).isEqualTo(2);
    assertThat(entriesAfterRemoval.get(0).getResource().getFile()).isEqualTo(files.get(0));
    assertThat(entriesAfterRemoval.get(1).getResource().getFile()).isEqualTo(files.get(2));
  }

  private static FileBinaryResource writeToStorage(
      final DefaultDiskStorage storage, final String resourceId, final byte[] value)
      throws IOException {
    DiskStorage.Inserter inserter = storage.insert(resourceId, null);
    writeToResource(inserter, value);
    return (FileBinaryResource) inserter.commit(null);
  }

  private static File writeFileToStorage(
      DefaultDiskStorage storage, String resourceId, byte[] value) throws IOException {
    return writeToStorage(storage, resourceId, value).getFile();
  }

  private static File write(DefaultDiskStorage storage, String resourceId, byte[] content)
      throws IOException {
    DiskStorage.Inserter inserter = storage.insert(resourceId, null);
    File file = ((DefaultDiskStorage.InserterImpl) inserter).mTemporaryFile;
    FileOutputStream fos = new FileOutputStream(file);
    try {
      fos.write(content);
    } finally {
      fos.close();
    }
    return ((FileBinaryResource) inserter.commit(null)).getFile();
  }

  private static void writeToResource(DiskStorage.Inserter inserter, final byte[] content)
      throws IOException {
    inserter.writeData(
        new WriterCallback() {
          @Override
          public void write(OutputStream os) throws IOException {
            os.write(content);
          }
        },
        null);
  }

  private void purgeUnexpectedFiles(DefaultDiskStorage storage) throws IOException {
    storage.purgeUnexpectedResources();
  }

  private List<File> findNewFiles(File directory, Set<File> existing, boolean recurse) {
    List<File> result = new ArrayList<>();
    findNewFiles(directory, existing, recurse, result);
    return result;
  }

  private void findNewFiles(
      File directory, Set<File> existing, boolean recurse, List<File> result) {
    File[] files = directory.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.isDirectory() && recurse) {
          findNewFiles(file, existing, true, result);
        } else if (!existing.contains(file)) {
          result.add(file);
        }
      }
    }
  }

  /**
   * Retrieves a list of entries (the one returned by DiskStorage.Session.entriesIterator) ordered
   * by timestamp.
   *
   * @param storage
   */
  private static List<DefaultDiskStorage.EntryImpl> retrieveEntries(DefaultDiskStorage storage)
      throws IOException {
    List<DiskStorage.Entry> entries = new ArrayList<>(storage.getEntries());

    Collections.sort(
        entries,
        new Comparator<DiskStorage.Entry>() {
          @Override
          public int compare(DefaultDiskStorage.Entry a, DefaultDiskStorage.Entry b) {
            long al = a.getTimestamp();
            long bl = b.getTimestamp();
            return (al < bl) ? -1 : ((al > bl) ? 1 : 0);
          }
        });
    List<DefaultDiskStorage.EntryImpl> newEntries = new ArrayList<>();
    for (DiskStorage.Entry entry : entries) {
      newEntries.add((DefaultDiskStorage.EntryImpl) entry);
    }
    return newEntries;
  }
}
