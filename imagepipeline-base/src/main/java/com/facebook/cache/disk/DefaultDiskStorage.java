/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.cache.disk;

import android.os.Environment;
import androidx.annotation.StringDef;
import com.facebook.binaryresource.BinaryResource;
import com.facebook.binaryresource.FileBinaryResource;
import com.facebook.cache.common.CacheErrorLogger;
import com.facebook.cache.common.WriterCallback;
import com.facebook.common.file.FileTree;
import com.facebook.common.file.FileTreeVisitor;
import com.facebook.common.file.FileUtils;
import com.facebook.common.internal.CountingOutputStream;
import com.facebook.common.internal.Preconditions;
import com.facebook.common.internal.VisibleForTesting;
import com.facebook.common.time.Clock;
import com.facebook.common.time.SystemClock;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.annotation.Nullable;

/**
 * The default disk storage implementation. Subsumes both 'simple' and 'sharded' implementations via
 * a new SubdirectorySupplier.
 */
public class DefaultDiskStorage implements DiskStorage {

  private static final Class<?> TAG = DefaultDiskStorage.class;

  private static final String CONTENT_FILE_EXTENSION = ".cnt";
  private static final String TEMP_FILE_EXTENSION = ".tmp";

  private static final String DEFAULT_DISK_STORAGE_VERSION_PREFIX = "v2";

  /*
   * We use sharding to avoid Samsung's RFS problem, and to avoid having one big directory
   * containing thousands of files.
   * This number of directories is large enough based on the following reasoning:
   * - high usage: 150 photos per day
   * - such usage will hit Samsung's 6,500 photos cap in 43 days
   * - 100 buckets will extend that period to 4,300 days which is 11.78 years
   */
  private static final int SHARDING_BUCKET_COUNT = 100;

  /** We will allow purging of any temp files older than this. */
  static final long TEMP_FILE_LIFETIME_MS = TimeUnit.MINUTES.toMillis(30);

  /** The base directory used for the cache */
  private final File mRootDirectory;

  /** True if cache is external */
  private final boolean mIsExternal;

  /**
   * All the sharding occurs inside a version-directory. That allows for easy version upgrade. When
   * we find a base directory with no version-directory in it, it means that it's a different
   * version and we should delete the whole directory (including itself) for both reasons: 1) clear
   * all unusable files 2) avoid Samsung RFS problem that was hit with old implementations of
   * DiskStorage which used a single directory for all the files.
   */
  private final File mVersionDirectory;

  private final CacheErrorLogger mCacheErrorLogger;
  private final Clock mClock;

  /**
   * Instantiates a ShardedDiskStorage that will use the directory to save a map between keys and
   * files. The version is very important if clients change the format saved in those files.
   * ShardedDiskStorage will assure that files saved with different version will be never used and
   * eventually removed.
   *
   * @param rootDirectory root directory to create all content under
   * @param version version of the format used in the files. If passed a different version files
   *     saved with the previous value will not be read and will be purged eventually.
   * @param cacheErrorLogger logger for various events
   */
  public DefaultDiskStorage(File rootDirectory, int version, CacheErrorLogger cacheErrorLogger) {
    Preconditions.checkNotNull(rootDirectory);

    mRootDirectory = rootDirectory;
    mIsExternal = isExternal(rootDirectory, cacheErrorLogger);
    // mVersionDirectory's name identifies:
    // - the cache structure's version (sharded)
    // - the content's version (version value)
    // if structure changes, prefix will change... if content changes version will be different
    // the ideal would be asking mSharding its name, but it's created receiving the directory
    mVersionDirectory = new File(mRootDirectory, getVersionSubdirectoryName(version));
    mCacheErrorLogger = cacheErrorLogger;
    recreateDirectoryIfVersionChanges();
    mClock = SystemClock.get();
  }

  @SuppressWarnings("ExternalStorageUse")
  private static boolean isExternal(File directory, CacheErrorLogger cacheErrorLogger) {
    boolean state = false;
    String appCacheDirPath = null;

    try {
      // Whitelisted use of external storage Android changes in Target SDK 29 and above as it
      // only used for getting the canonical path
      File extStoragePath = Environment.getExternalStorageDirectory();
      if (extStoragePath != null) {
        String cacheDirPath = extStoragePath.toString();
        try {
          appCacheDirPath = directory.getCanonicalPath();
          if (appCacheDirPath.contains(cacheDirPath)) {
            state = true;
          }
        } catch (IOException e) {
          cacheErrorLogger.logError(
              CacheErrorLogger.CacheErrorCategory.OTHER,
              TAG,
              "failed to read folder to check if external: " + appCacheDirPath,
              e);
        }
      }
    } catch (Exception e) {
      cacheErrorLogger.logError(
          CacheErrorLogger.CacheErrorCategory.OTHER,
          TAG,
          "failed to get the external storage directory!",
          e);
    }
    return state;
  }

  @VisibleForTesting
  static String getVersionSubdirectoryName(int version) {
    return String.format(
        (Locale) null,
        "%s.ols%d.%d",
        DEFAULT_DISK_STORAGE_VERSION_PREFIX,
        SHARDING_BUCKET_COUNT,
        version);
  }

  @Override
  public boolean isEnabled() {
    return true;
  }

  @Override
  public boolean isExternal() {
    return mIsExternal;
  }

  @Override
  public String getStorageName() {
    String directoryName = mRootDirectory.getAbsolutePath();
    return "_"
        + directoryName.substring(directoryName.lastIndexOf('/') + 1, directoryName.length())
        + "_"
        + directoryName.hashCode();
  }

  /**
   * Checks if we have to recreate rootDirectory. This is needed because old versions of this
   * storage created too much different files in the same dir, and Samsung's RFS has a bug that
   * after the 13.000th creation fails. So if cache is not already in expected version let's destroy
   * everything (if not in expected version... there's nothing to reuse here anyway).
   */
  private void recreateDirectoryIfVersionChanges() {
    boolean recreateBase = false;
    if (!mRootDirectory.exists()) {
      recreateBase = true;
    } else if (!mVersionDirectory.exists()) {
      recreateBase = true;
      FileTree.deleteRecursively(mRootDirectory);
    }

    if (recreateBase) {
      try {
        FileUtils.mkdirs(mVersionDirectory);
      } catch (FileUtils.CreateDirectoryException e) {
        // not the end of the world, when saving files we will try to create missing parent dirs
        mCacheErrorLogger.logError(
            CacheErrorLogger.CacheErrorCategory.WRITE_CREATE_DIR,
            TAG,
            "version directory could not be created: " + mVersionDirectory,
            null);
      }
    }
  }

  private static class IncompleteFileException extends IOException {
    public final long expected;
    public final long actual;

    public IncompleteFileException(long expected, long actual) {
      super("File was not written completely. Expected: " + expected + ", found: " + actual);
      this.expected = expected;
      this.actual = actual;
    }
  }

  /** Calculates which should be the CONTENT file for the given key */
  @VisibleForTesting
  File getContentFileFor(String resourceId) {
    return new File(getFilename(resourceId));
  }

  /**
   * Gets the directory to use to store the given key
   *
   * @param resourceId the id of the file we're going to store
   * @return the directory to store the file in
   */
  private String getSubdirectoryPath(String resourceId) {
    String subdirectory = String.valueOf(Math.abs(resourceId.hashCode() % SHARDING_BUCKET_COUNT));
    return mVersionDirectory + File.separator + subdirectory;
  }

  /**
   * Gets the directory to use to store the given key
   *
   * @param resourceId the id of the file we're going to store
   * @return the directory to store the file in
   */
  private File getSubdirectory(String resourceId) {
    return new File(getSubdirectoryPath(resourceId));
  }

  /**
   * Implementation of {@link FileTreeVisitor} to iterate over all the sharded files and collect
   * those valid content files. It's used in entriesIterator method.
   */
  private class EntriesCollector implements FileTreeVisitor {

    private final List<Entry> result = new ArrayList<>();

    @Override
    public void preVisitDirectory(File directory) {}

    @Override
    public void visitFile(File file) {
      FileInfo info = getShardFileInfo(file);
      if (info != null && info.type == FileType.CONTENT) {
        result.add(new EntryImpl(info.resourceId, file));
      }
    }

    @Override
    public void postVisitDirectory(File directory) {}

    /** Returns an immutable list of the entries. */
    public List<Entry> getEntries() {
      return Collections.unmodifiableList(result);
    }
  }

  /**
   * This implements a {@link FileTreeVisitor} to iterate over all the files in mDirectory and
   * delete any unexpected file or directory. It also gets rid of any empty directory in the shard.
   * As a shortcut it checks that things are inside (current) mVersionDirectory. If it's not then
   * it's directly deleted. If it's inside then it checks if it's a recognized file and if it's in
   * the correct shard according to its name (checkShard method). If it's unexpected file is
   * deleted.
   */
  private class PurgingVisitor implements FileTreeVisitor {
    private boolean insideBaseDirectory;

    @Override
    public void preVisitDirectory(File directory) {
      if (!insideBaseDirectory && directory.equals(mVersionDirectory)) {
        // if we enter version-directory turn flag on
        insideBaseDirectory = true;
      }
    }

    @Override
    public void visitFile(File file) {
      if (!insideBaseDirectory || !isExpectedFile(file)) {
        file.delete();
      }
    }

    @Override
    public void postVisitDirectory(File directory) {
      if (!mRootDirectory.equals(directory)) { // if it's root directory we must not touch it
        if (!insideBaseDirectory) {
          // if not in version-directory then it's unexpected!
          directory.delete();
        }
      }
      if (insideBaseDirectory && directory.equals(mVersionDirectory)) {
        // if we just finished visiting version-directory turn flag off
        insideBaseDirectory = false;
      }
    }

    private boolean isExpectedFile(File file) {
      FileInfo info = getShardFileInfo(file);
      if (info == null) {
        return false;
      }
      if (info.type == FileType.TEMP) {
        return isRecentFile(file);
      }
      Preconditions.checkState(info.type == FileType.CONTENT);
      return true;
    }

    /** @return true if and only if the file is not old enough to be considered an old temp file */
    private boolean isRecentFile(File file) {
      return file.lastModified() > (mClock.now() - TEMP_FILE_LIFETIME_MS);
    }
  };

  @Override
  public void purgeUnexpectedResources() {
    FileTree.walkFileTree(mRootDirectory, new PurgingVisitor());
  }

  /**
   * Creates the directory (and its parents, if necessary). In case of an exception, log an error
   * message with the relevant parameters
   *
   * @param directory the directory to create
   * @param message message to use
   * @throws IOException
   */
  private void mkdirs(File directory, String message) throws IOException {
    try {
      FileUtils.mkdirs(directory);
    } catch (FileUtils.CreateDirectoryException cde) {
      mCacheErrorLogger.logError(
          CacheErrorLogger.CacheErrorCategory.WRITE_CREATE_DIR, TAG, message, cde);
      throw cde;
    }
  }

  @Override
  public Inserter insert(String resourceId, Object debugInfo) throws IOException {
    // ensure that the parent directory exists
    FileInfo info = new FileInfo(FileType.TEMP, resourceId);
    File parent = getSubdirectory(info.resourceId);
    if (!parent.exists()) {
      mkdirs(parent, "insert");
    }

    try {
      File file = info.createTempFile(parent);
      return new InserterImpl(resourceId, file);
    } catch (IOException ioe) {
      mCacheErrorLogger.logError(
          CacheErrorLogger.CacheErrorCategory.WRITE_CREATE_TEMPFILE, TAG, "insert", ioe);
      throw ioe;
    }
  }

  @Override
  public @Nullable BinaryResource getResource(String resourceId, Object debugInfo) {
    final File file = getContentFileFor(resourceId);
    if (file.exists()) {
      file.setLastModified(mClock.now());
      return FileBinaryResource.createOrNull(file);
    }
    return null;
  }

  private String getFilename(String resourceId) {
    FileInfo fileInfo = new FileInfo(FileType.CONTENT, resourceId);
    String path = getSubdirectoryPath(fileInfo.resourceId);
    return fileInfo.toPath(path);
  }

  @Override
  public boolean contains(String resourceId, Object debugInfo) {
    return query(resourceId, false);
  }

  @Override
  public boolean touch(String resourceId, Object debugInfo) {
    return query(resourceId, true);
  }

  private boolean query(String resourceId, boolean touch) {
    File contentFile = getContentFileFor(resourceId);
    boolean exists = contentFile.exists();
    if (touch && exists) {
      contentFile.setLastModified(mClock.now());
    }
    return exists;
  }

  @Override
  public long remove(Entry entry) {
    // it should be one entry return by us :)
    EntryImpl entryImpl = (EntryImpl) entry;
    FileBinaryResource resource = entryImpl.getResource();
    return doRemove(resource.getFile());
  }

  @Override
  public long remove(final String resourceId) {
    return doRemove(getContentFileFor(resourceId));
  }

  private long doRemove(final File contentFile) {
    if (!contentFile.exists()) {
      return 0;
    }

    final long fileSize = contentFile.length();
    if (contentFile.delete()) {
      return fileSize;
    }

    return -1;
  }

  public void clearAll() {
    FileTree.deleteContents(mRootDirectory);
  }

  @Override
  public DiskDumpInfo getDumpInfo() throws IOException {
    List<Entry> entries = getEntries();

    DiskDumpInfo dumpInfo = new DiskDumpInfo();
    for (Entry entry : entries) {
      DiskDumpInfoEntry infoEntry = dumpCacheEntry(entry);
      String type = infoEntry.type;
      if (!dumpInfo.typeCounts.containsKey(type)) {
        dumpInfo.typeCounts.put(type, 0);
      }
      dumpInfo.typeCounts.put(type, dumpInfo.typeCounts.get(type) + 1);
      dumpInfo.entries.add(infoEntry);
    }
    return dumpInfo;
  }

  private DiskDumpInfoEntry dumpCacheEntry(Entry entry) throws IOException {
    EntryImpl entryImpl = (EntryImpl) entry;
    String firstBits = "";
    byte[] bytes = entryImpl.getResource().read();
    String type = typeOfBytes(bytes);
    if (type.equals("undefined") && bytes.length >= 4) {
      firstBits =
          String.format(
              (Locale) null, "0x%02X 0x%02X 0x%02X 0x%02X", bytes[0], bytes[1], bytes[2], bytes[3]);
    }
    String path = entryImpl.getResource().getFile().getPath();
    return new DiskDumpInfoEntry(entryImpl.getId(), path, type, entryImpl.getSize(), firstBits);
  }

  private String typeOfBytes(byte[] bytes) {
    if (bytes.length >= 2) {
      if (bytes[0] == (byte) 0xFF && bytes[1] == (byte) 0xD8) {
        return "jpg";
      } else if (bytes[0] == (byte) 0x89 && bytes[1] == (byte) 0x50) {
        return "png";
      } else if (bytes[0] == (byte) 0x52 && bytes[1] == (byte) 0x49) {
        return "webp";
      } else if (bytes[0] == (byte) 0x47 && bytes[1] == (byte) 0x49) {
        return "gif";
      }
    }
    return "undefined";
  }

  @Override
  /**
   * Returns a list of entries.
   *
   * <p>This list is immutable.
   */
  public List<Entry> getEntries() throws IOException {
    EntriesCollector collector = new EntriesCollector();
    FileTree.walkFileTree(mVersionDirectory, collector);
    return collector.getEntries();
  }

  /** Implementation of Entry listed by entriesIterator. */
  @VisibleForTesting
  static class EntryImpl implements Entry {
    private final String id;
    private final FileBinaryResource resource;
    private long size;
    private long timestamp;

    private EntryImpl(String id, File cachedFile) {
      Preconditions.checkNotNull(cachedFile);
      this.id = Preconditions.checkNotNull(id);
      this.resource = FileBinaryResource.createOrNull(cachedFile);
      this.size = -1;
      this.timestamp = -1;
    }

    @Override
    public String getId() {
      return id;
    }

    @Override
    public long getTimestamp() {
      if (timestamp < 0) {
        final File cachedFile = resource.getFile();
        timestamp = cachedFile.lastModified();
      }
      return timestamp;
    }

    @Override
    public FileBinaryResource getResource() {
      return resource;
    }

    @Override
    public long getSize() {
      if (size < 0) {
        size = resource.size();
      }
      return size;
    }
  }

  /**
   * Checks that the file is placed in the correct shard according to its filename (and hence the
   * represented key). If it's correct its FileInfo is returned.
   *
   * @param file the file to check
   * @return the corresponding FileInfo object if shard is correct, null otherwise
   */
  private @Nullable FileInfo getShardFileInfo(File file) {
    FileInfo info = FileInfo.fromFile(file);
    if (info == null) {
      return null; // file with incorrect name/extension
    }
    File expectedDirectory = getSubdirectory(info.resourceId);
    boolean isCorrect = expectedDirectory.equals(file.getParentFile());
    return isCorrect ? info : null;
  }

  /**
   * Categories for the different internal files a ShardedDiskStorage maintains. CONTENT: the file
   * that has the content TEMP: temporal files, used to write the content until they are switched to
   * CONTENT files
   */
  @StringDef({
    FileType.CONTENT,
    FileType.TEMP,
  })
  public @interface FileType {
    String CONTENT = CONTENT_FILE_EXTENSION;
    String TEMP = TEMP_FILE_EXTENSION;
  }

  private static @Nullable @FileType String getFileTypefromExtension(String extension) {
    if (CONTENT_FILE_EXTENSION.equals(extension)) {
      return FileType.CONTENT;
    } else if (TEMP_FILE_EXTENSION.equals(extension)) {
      return FileType.TEMP;
    }
    return null;
  }

  /**
   * Holds information about the different files this storage uses (content, tmp). All file name
   * parsing should be done through here. Temp files creation is also handled here, to encapsulate
   * naming.
   */
  private static class FileInfo {

    public final @FileType String type;
    public final String resourceId;

    private FileInfo(@FileType String type, String resourceId) {
      this.type = type;
      this.resourceId = resourceId;
    }

    @Override
    public String toString() {
      return type + "(" + resourceId + ")";
    }

    public String toPath(String parentPath) {
      return parentPath + File.separator + resourceId + type;
    }

    public File createTempFile(File parent) throws IOException {
      File f = File.createTempFile(resourceId + ".", TEMP_FILE_EXTENSION, parent);
      return f;
    }

    @Nullable
    public static FileInfo fromFile(File file) {
      String name = file.getName();
      int pos = name.lastIndexOf('.');
      if (pos <= 0) {
        return null; // no name part
      }
      String ext = name.substring(pos);
      @FileType String type = getFileTypefromExtension(ext);
      if (type == null) {
        return null; // unknown!
      }
      String resourceId = name.substring(0, pos);
      if (type.equals(FileType.TEMP)) {
        int numPos = resourceId.lastIndexOf('.');
        if (numPos <= 0) {
          return null; // no resourceId.number
        }
        resourceId = resourceId.substring(0, numPos);
      }

      return new FileInfo(type, resourceId);
    }
  }

  @VisibleForTesting
  /* package protected */ class InserterImpl implements Inserter {

    private final String mResourceId;

    @VisibleForTesting /* package protected*/ final File mTemporaryFile;

    public InserterImpl(String resourceId, File temporaryFile) {
      mResourceId = resourceId;
      mTemporaryFile = temporaryFile;
    }

    @Override
    public void writeData(WriterCallback callback, Object debugInfo) throws IOException {
      FileOutputStream fileStream;
      try {
        fileStream = new FileOutputStream(mTemporaryFile);
      } catch (FileNotFoundException fne) {
        mCacheErrorLogger.logError(
            CacheErrorLogger.CacheErrorCategory.WRITE_UPDATE_FILE_NOT_FOUND,
            TAG,
            "updateResource",
            fne);
        throw fne;
      }

      long length;
      try {
        CountingOutputStream countingStream = new CountingOutputStream(fileStream);
        callback.write(countingStream);
        // just in case underlying stream's close method doesn't flush:
        // we flush it manually and inside the try/catch
        countingStream.flush();
        length = countingStream.getCount();
      } finally {
        // if it fails to close (or write the last piece) we really want to know
        // Normally we would want this to be quiet because a closing exception would hide one
        // inside the try, but now we really want to know if something fails at flush or close
        fileStream.close();
      }
      // this code should never throw, but if filesystem doesn't fail on a failing/uncomplete close
      // we want to know and manually fail
      if (mTemporaryFile.length() != length) {
        throw new IncompleteFileException(length, mTemporaryFile.length());
      }
    }

    @Override
    public BinaryResource commit(Object debugInfo) throws IOException {
      return commit(debugInfo, mClock.now());
    }

    @Override
    public BinaryResource commit(Object debugInfo, long time) throws IOException {
      // the temp resource must be ours!
      File targetFile = getContentFileFor(mResourceId);

      try {
        FileUtils.rename(mTemporaryFile, targetFile);
      } catch (FileUtils.RenameException re) {
        CacheErrorLogger.CacheErrorCategory category;
        Throwable cause = re.getCause();
        if (cause == null) {
          category = CacheErrorLogger.CacheErrorCategory.WRITE_RENAME_FILE_OTHER;
        } else if (cause instanceof FileUtils.ParentDirNotFoundException) {
          category =
              CacheErrorLogger.CacheErrorCategory.WRITE_RENAME_FILE_TEMPFILE_PARENT_NOT_FOUND;
        } else if (cause instanceof FileNotFoundException) {
          category = CacheErrorLogger.CacheErrorCategory.WRITE_RENAME_FILE_TEMPFILE_NOT_FOUND;
        } else {
          category = CacheErrorLogger.CacheErrorCategory.WRITE_RENAME_FILE_OTHER;
        }
        mCacheErrorLogger.logError(category, TAG, "commit", re);
        throw re;
      }
      if (targetFile.exists()) {
        targetFile.setLastModified(time);
      }
      return FileBinaryResource.createOrNull(targetFile);
    }

    @Override
    public boolean cleanUp() {
      return !mTemporaryFile.exists() || mTemporaryFile.delete();
    }
  }
}
