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

import com.facebook.binaryresource.BinaryResource;
import com.facebook.binaryresource.FileBinaryResource;
import com.facebook.cache.common.CacheErrorLogger;
import com.facebook.cache.common.WriterCallback;
import com.facebook.common.file.FileTree;
import com.facebook.common.internal.Files;
import com.facebook.common.internal.Supplier;
import com.facebook.common.time.SystemClock;

import org.junit.Assert;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for 'default' disk storage
 */
@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@PrepareOnlyThisForTest({SystemClock.class})
public class DefaultDiskStorageTest {

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  private File mDirectory;
  private SystemClock mClock;

  @Before
  public void before() throws Exception {
    mClock = mock(SystemClock.class);
    PowerMockito.mockStatic(SystemClock.class);
    PowerMockito.when(SystemClock.get()).thenReturn(mClock);
    mDirectory = new File(Robolectric.application.getCacheDir(), "sharded-disk-storage-test");
    Assert.assertTrue(mDirectory.mkdirs());
    FileTree.deleteContents(mDirectory);
  }

  private Supplier<DefaultDiskStorage> getStorageSupplier(final int version) {
    return new Supplier<DefaultDiskStorage>() {
      @Override
      public DefaultDiskStorage get() {
        return new DefaultDiskStorage(
            mDirectory,
            version,
            mock(CacheErrorLogger.class));
      }
    };
  }

  @Test
  public void testStartup() throws Exception {
    // create a bogus file
    File bogusFile = new File(mDirectory, "bogus");
    Assert.assertTrue(bogusFile.createNewFile());

    // create the storage now. Bogus files should be gone now
    DefaultDiskStorage storage = getStorageSupplier(1).get();
    Assert.assertFalse(bogusFile.exists());
    String version1Dir = DefaultDiskStorage.getVersionSubdirectoryName(1);
    Assert.assertTrue(new File(mDirectory, version1Dir).exists());

    // create a new version
    storage = getStorageSupplier(2).get();
    Assert.assertNotNull(storage);
    Assert.assertFalse(new File(mDirectory, version1Dir).exists());
    String version2Dir = DefaultDiskStorage.getVersionSubdirectoryName(2);
    Assert.assertTrue(new File(mDirectory, version2Dir).exists());
  }

  @Test
  public void testIsEnabled() {
    DefaultDiskStorage storage = getStorageSupplier(1).get();
    Assert.assertTrue(storage.isEnabled());
  }

  @Test
  public void testBasicOperations() throws Exception {
    DefaultDiskStorage storage = getStorageSupplier(1).get();
    final String resourceId1 = "R1";
    final String resourceId2 = "R2";

    // no file - get should fail
    FileBinaryResource resource1 = storage.getResource(resourceId1, null);
    Assert.assertNull(resource1);

    // write out the file now
    byte[] key1Contents = new byte[] {0, 1, 2};
    writeToStorage(storage, resourceId1, key1Contents);
    // get should succeed now
    resource1 = storage.getResource(resourceId1, null);
    Assert.assertNotNull(resource1);
    Assert.assertArrayEquals(key1Contents, Files.toByteArray(resource1.getFile()));
    // remove the file now - get should fail again
    Assert.assertTrue(resource1.getFile().delete());
    resource1 = storage.getResource(resourceId1, null);
    Assert.assertNull(resource1);
    // no file
    FileBinaryResource resource2 = storage.getResource(resourceId2, null);
    Assert.assertNull(resource2);
  }

  /**
   * Test that a file is stored in a new file,
   * and the bytes are stored plainly in the file.
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
    Assert.assertTrue(mDirectory.exists());
    List<File> founds1 = findNewFiles(mDirectory, files, /*recurse*/true);
    Assert.assertNotNull(file1);
    Assert.assertTrue(founds1.contains(file1));
    Assert.assertTrue(file1.exists());
    assertEquals(100, file1.length());
    Assert.assertArrayEquals(value1, Files.toByteArray(file1));
  }

  /**
   * Inserts 3 files with different dates.
   * Check what files are there.
   * Uses an iterator to remove the one in the middle.
   * Check that later.
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

    List<File> files = findNewFiles(mDirectory, Collections.<File>emptySet(), /*recurse*/true);

    // there should be 1 file per entry
    assertEquals(3, files.size());

    // now delete entry2
    Collection<DiskStorage.Entry> entries = storage.getEntries();
    for (DiskStorage.Entry entry : entries) {
      if (Math.abs(entry.getTimestamp() - time2) < 500) {
        storage.remove(entry);
      }
    }

    assertFalse(storage.contains(resourceId2, null));
    List<File> remaining = findNewFiles(mDirectory, Collections.<File>emptySet(), /*recurse*/true);

    // 2 entries remain
    assertEquals(2, remaining.size());

    // none of them with timestamp close to time2
    List<DiskStorage.Entry> entries1 = new ArrayList<>(storage.getEntries());
    assertEquals(2, entries1.size());
    // first
    DiskStorage.Entry entry = entries1.get(0);
    assertFalse(Math.abs(entry.getTimestamp() - time2) < 500);
    // second
    entry = entries1.get(1);
    assertFalse(Math.abs(entry.getTimestamp() - time2) < 500);
  }

  @Test
  public void testTouch() throws Exception {
    DefaultDiskStorage storage = getStorageSupplier(1).get();
    final long startTime = 0;

    final String resourceId1 = "resource1";
    final byte[] value1 = new byte[100];
    final File file1 = writeFileToStorage(storage, resourceId1, value1);
    assertTrue(Math.abs(file1.lastModified() - startTime) <= 500);

    final long time2 = startTime + 10000;
    when(mClock.now()).thenReturn(time2);
    final String resourceId2 = "resource2";
    final byte[] value2 = new byte[100];
    final File file2 = writeFileToStorage(storage, resourceId2, value2);
    assertTrue(Math.abs(file1.lastModified() - startTime) <= 500);
    assertTrue(Math.abs(file2.lastModified() - time2) <= 500);

    final long time3 = time2 + 10000;
    when(mClock.now()).thenReturn(time3);
    storage.touch(resourceId1, null);
    assertTrue(Math.abs(file1.lastModified() - time3) <= 500);
    assertTrue(Math.abs(file2.lastModified() - time2) <= 500);
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

    assertTrue(storage.contains(resourceId1, null));
    assertTrue(storage.contains(resourceId2, null));
    assertTrue(storage.contains(resourceId3, null));

    storage.remove(resourceId2);
    assertTrue(storage.contains(resourceId1, null));
    assertFalse(storage.contains(resourceId2, null));
    assertTrue(storage.contains(resourceId3, null));

    storage.remove(resourceId1);
    assertFalse(storage.contains(resourceId1, null));
    assertFalse(storage.contains(resourceId2, null));
    assertTrue(storage.contains(resourceId3, null));

    storage.remove(resourceId3);
    assertFalse(storage.contains(resourceId1, null));
    assertFalse(storage.contains(resourceId2, null));
    assertFalse(storage.contains(resourceId3, null));
  }

  @Test
  public void testEntryImmutable() throws Exception {
    DefaultDiskStorage storage = getStorageSupplier(1).get();

    final String resourceId1 = "resource1";
    final byte[] value1 = new byte[100];
    value1[80] = 123;
    final File file1 = writeFileToStorage(storage, resourceId1, value1);

    assertEquals(100, file1.length());
    List<DiskStorage.Entry> entries = storage.getEntries();
    DiskStorage.Entry entry = entries.get(0);
    long timestamp = entry.getTimestamp();
    when(mClock.now()).thenReturn(TimeUnit.HOURS.toMillis(1));
    storage.getResource(resourceId1, null);

    // now the new timestamp show be higher, but the entry should have the same value
    List<DiskStorage.Entry> newEntries = storage.getEntries();
    DiskStorage.Entry newEntry = newEntries.get(0);
    assertTrue(timestamp < newEntry.getTimestamp());
    assertEquals(timestamp, entry.getTimestamp());
  }

  @Test
  public void testTempFileEviction() throws IOException {
    when(mClock.now()).thenReturn(TimeUnit.DAYS.toMillis(1000));
    DefaultDiskStorage storage = getStorageSupplier(1).get();

    final String resourceId1 = "resource1";
    final File tempFile = storage.createTemporary(resourceId1, null).getFile();

    // Make sure that we don't evict a recent temp file
    purgeUnexpectedFiles(storage);
    assertTrue(tempFile.exists());

    // Mark it old, then try eviction again. It should be gone.
    if (!tempFile.setLastModified(mClock.now() - DefaultDiskStorage.TEMP_FILE_LIFETIME_MS - 1000)) {
      throw new IOException("Unable to update timestamp of file: " + tempFile);
    }
    purgeUnexpectedFiles(storage);
    assertFalse(tempFile.exists());
  }

  /**
   * Test that purgeUnexpectedResources deletes all files/directories outside the version directory
   * but leaves untouched the version directory and the content files.
   * @throws Exception
   */
  @Test
  public void testPurgeUnexpectedFiles() throws Exception {
    final DiskStorage storage = getStorageSupplier(1).get();

    final String resourceId = "file1";
    final byte[] CONTENT = "content".getBytes("UTF-8");

    File file = writeFileToStorage(storage, resourceId, CONTENT);

    // check file exists
    Assert.assertTrue(file.exists());
    Assert.assertArrayEquals(CONTENT, Files.toByteArray(file));

    final File unexpectedFile1 = new File(mDirectory, "unexpected-file-1");
    final File unexpectedFile2 = new File(mDirectory, "unexpected-file-2");

    Assert.assertTrue(unexpectedFile1.createNewFile());
    Assert.assertTrue(unexpectedFile2.createNewFile());

    final File unexpectedDir1 = new File(mDirectory, "unexpected-dir-1");
    Assert.assertTrue(unexpectedDir1.mkdirs());

    final File unexpectedDir2 = new File(mDirectory, "unexpected-dir-2");
    Assert.assertTrue(unexpectedDir2.mkdirs());
    final File unexpectedSubfile1 = new File(unexpectedDir2, "unexpected-sub-file-1");
    Assert.assertTrue(unexpectedSubfile1.createNewFile());

    Assert.assertEquals(5, mDirectory.listFiles().length); // 4 unexpected (files+dirs) + ver. dir
    Assert.assertEquals(1, unexpectedDir2.listFiles().length);
    Assert.assertEquals(0, unexpectedDir1.listFiles().length);

    File unexpectedFileInShard = new File(file.getParentFile(), "unexpected-in-shard");
    Assert.assertTrue(unexpectedFileInShard.createNewFile());

    storage.purgeUnexpectedResources();
    Assert.assertFalse(unexpectedFile1.exists());
    Assert.assertFalse(unexpectedFile2.exists());
    Assert.assertFalse(unexpectedSubfile1.exists());
    Assert.assertFalse(unexpectedDir1.exists());
    Assert.assertFalse(unexpectedDir2.exists());

    // check file still exists
    Assert.assertTrue(file.exists());
    // check unexpected sibling is gone
    Assert.assertFalse(unexpectedFileInShard.exists());
    // check the only thing in root is the version directory
    Assert.assertEquals(1, mDirectory.listFiles().length); // just the version directory
  }

  /**
   * Tests that an existing directory is nuked when it's not current version (doens't have
   * the version directory used for the structure)
   * @throws Exception
   */
  @Test
  public void testDirectoryIsNuked() throws Exception {
    Assert.assertEquals(0, mDirectory.listFiles().length);

    // create file before setting final test date
    Assert.assertTrue(new File(mDirectory, "something-arbitrary").createNewFile());

    long lastModified = mDirectory.lastModified() - 1000;
    // some previous date to the "now" used for file creation
    Assert.assertTrue(mDirectory.setLastModified(lastModified));
    // check it was changed
    Assert.assertEquals(lastModified, mDirectory.lastModified());

    getStorageSupplier(1).get();

    // mDirectory exists...
    Assert.assertTrue(mDirectory.exists());
    // but it was created now
    Assert.assertTrue(lastModified < mDirectory.lastModified());
  }

  /**
   * Tests that an existing directory is not nuked if the version directory used for the structure
   * exists (so it's current version and doesn't suffer Samsung RFS problem)
   * @throws Exception
   */
  @Test
  public void testDirectoryIsNotNuked() throws Exception {
    Assert.assertEquals(0, mDirectory.listFiles().length);

    final DiskStorage storage = getStorageSupplier(1).get();
    final String resourceId = "file1";

    final byte[] CONTENT = "content".getBytes("UTF-8");

    // create a file so we know version directory really exists
    BinaryResource temporary = storage.createTemporary(resourceId, null);
    writeToResource(storage, resourceId, temporary, CONTENT);
    storage.commit(resourceId, temporary, null);

    // assign some previous date to the "now" used for file creation
    long lastModified = mDirectory.lastModified() - 1000;
    Assert.assertTrue(mDirectory.setLastModified(lastModified));
    // check it was changed
    Assert.assertEquals(lastModified, mDirectory.lastModified());

    // create again, it shouldn't delete the directory
    getStorageSupplier(1).get();

    // mDirectory exists...
    Assert.assertTrue(mDirectory.exists());
    // and it's the same as before
    Assert.assertEquals(lastModified, mDirectory.lastModified());
  }

  /**
   * Test the iterator returned is ok and deletion through the iterator is ok too.
   * This is the required functionality that eviction needs.
   * @throws Exception
   */
  @Test
  public void testIterationAndRemoval() throws Exception {
    DiskStorage storage = getStorageSupplier(1).get();
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
    Assert.assertEquals(4, entries.size());
    Assert.assertEquals(files.get(0), entries.get(0).getResource().getFile());
    Assert.assertEquals(files.get(1), entries.get(1).getResource().getFile());
    Assert.assertEquals(files.get(2), entries.get(2).getResource().getFile());
    Assert.assertEquals(files.get(3), entries.get(3).getResource().getFile());

    // try the same after removing 2 entries
    for (DiskStorage.Entry entry : storage.getEntries()) {
      // delete the 2 biggest files: key1 and key3 (see the content values)
      if (entry.getSize() >= CONTENT1.length) {
        storage.remove(entry);
      }
    }

    List<DefaultDiskStorage.EntryImpl> entriesAfterRemoval = retrieveEntries(storage);
    Assert.assertEquals(2, entriesAfterRemoval.size());
    Assert.assertEquals(files.get(0), entriesAfterRemoval.get(0).getResource().getFile());
    Assert.assertEquals(files.get(2), entriesAfterRemoval.get(1).getResource().getFile());
  }

  private static FileBinaryResource writeToStorage(
      final DiskStorage storage,
      final String resourceId,
      final byte[] value) throws IOException {
    BinaryResource temporary = storage.createTemporary(resourceId, null);
    writeToResource(storage, resourceId, temporary, value);
    BinaryResource resource = storage.commit(resourceId, temporary, null);
    return (FileBinaryResource)resource;
  }

  private static File writeFileToStorage(
      DiskStorage storage,
      String resourceId,
      byte[] value) throws IOException {
    return writeToStorage(storage, resourceId, value).getFile();
  }

  private static File write(
      DiskStorage storage,
      String resourceId,
      byte[] content) throws IOException {
    BinaryResource temporary = storage.createTemporary(resourceId, null);
    File file = ((FileBinaryResource)temporary).getFile();
    FileOutputStream fos = new FileOutputStream(file);
    try {
      fos.write(content);
    } finally {
      fos.close();
    }
    return ((FileBinaryResource)storage.commit(resourceId, temporary, null)).getFile();
  }

  private static void writeToResource(
      DiskStorage storage,
      String resourceId,
      BinaryResource resource,
      final byte[] content) throws IOException {
    storage.updateResource(resourceId, resource, new WriterCallback() {
      @Override
      public void write(OutputStream os) throws IOException {
        os.write(content);
      }
    }, null);
  }

  private void purgeUnexpectedFiles(DefaultDiskStorage storage)
      throws IOException {
    storage.purgeUnexpectedResources();
  }

  private List<File> findNewFiles(File directory, Set<File> existing, boolean recurse) {
    List<File> result = new ArrayList<>();
    findNewFiles(directory, existing, recurse, result);
    return result;
  }

  private void findNewFiles(
      File directory,
      Set<File> existing,
      boolean recurse,
      List<File> result) {
    File[] files = directory.listFiles();
    if (files != null) {
      for (File file: files) {
        if (file.isDirectory() && recurse) {
          findNewFiles(file, existing, true, result);
        } else if (!existing.contains(file)) {
          result.add(file);
        }
      }
    }
  }

  /**
   * Retrieves a list of entries (the one returned by DiskStorage.Session.entriesIterator)
   * ordered by timestamp.
   * @param storage
   */
  private static List<DefaultDiskStorage.EntryImpl> retrieveEntries(
      DiskStorage storage)
      throws IOException {
    List<DiskStorage.Entry> entries = new ArrayList<>(storage.getEntries());

    Collections.sort(entries, new Comparator<DiskStorage.Entry>() {
      @Override
      public int compare(DefaultDiskStorage.Entry a, DefaultDiskStorage.Entry b) {
        long al = a.getTimestamp();
        long bl = b.getTimestamp();
        return (al < bl) ? -1 : ((al > bl) ? 1 : 0);
      }
    });
    List<DefaultDiskStorage.EntryImpl> newEntries = new ArrayList<>();
    for (DiskStorage.Entry entry: entries) {
      newEntries.add((DefaultDiskStorage.EntryImpl)entry);
    }
    return newEntries;
  }
}
