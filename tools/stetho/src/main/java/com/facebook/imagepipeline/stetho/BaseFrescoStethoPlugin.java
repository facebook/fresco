/*
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.stetho;

import android.graphics.Bitmap;
import android.os.Environment;
import android.text.TextUtils;
import android.util.SparseArray;
import com.facebook.cache.common.CacheKey;
import com.facebook.cache.disk.DiskStorage;
import com.facebook.cache.disk.FileCache;
import com.facebook.common.time.RealtimeSinceBootClock;
import com.facebook.imagepipeline.cache.BitmapMemoryCacheKey;
import com.facebook.imagepipeline.cache.CountingMemoryCacheInspector;
import com.facebook.imagepipeline.core.ImagePipelineFactory;
import com.facebook.imagepipeline.image.CloseableBitmap;
import com.facebook.imagepipeline.image.CloseableImage;
import com.facebook.stetho.dumpapp.DumpException;
import com.facebook.stetho.dumpapp.DumpUsageException;
import com.facebook.stetho.dumpapp.DumperContext;
import com.facebook.stetho.dumpapp.DumperPlugin;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Base class for the Fresco Stetho plugin.
 *
 * <p>Applications should instantiate
 */
public abstract class BaseFrescoStethoPlugin implements DumperPlugin {

  private static final String NAME = "image";
  private static final float KB = 1024f;

  protected boolean mInitialized;
  private CountingMemoryCacheInspector<CacheKey, CloseableImage>
      mBitmapMemoryCacheInspector;
  private FileCache mMainFileCache;
  private FileCache mSmallFileCache;

  protected BaseFrescoStethoPlugin() {
    mInitialized = false;
  }

  protected BaseFrescoStethoPlugin(ImagePipelineFactory factory) {
    initialize(factory);
  }

  protected abstract void ensureInitialized() throws DumpException;

  protected void initialize(ImagePipelineFactory factory) {
    mBitmapMemoryCacheInspector = new CountingMemoryCacheInspector<>(
        factory.getBitmapCountingMemoryCache());
    mMainFileCache = factory.getMainFileCache();
    mSmallFileCache = factory.getSmallImageFileCache();
    mInitialized = true;
  }

  @Override
  public String getName() {
    return NAME;
  }

  /**
   * Entry point for the Stetho dumpapp script.
   *
   * {@link #initialize} must have been called in the app before running dumpapp.
   */
  @Override
  public void dump(DumperContext dumpContext) throws DumpException {
    ensureInitialized();
    List<String> args = dumpContext.getArgsAsList();
    PrintStream writer = dumpContext.getStdout();

    String cmd = args.isEmpty() ? null : args.get(0);
    List<String> rest = args.isEmpty() ? new ArrayList<String>() : args.subList(1, args.size());

    if (cmd != null && cmd.equals("memcache")) {
      memcache(writer, rest);
    } else if (cmd != null && cmd.equals("diskcache")) {
      diskcache(mMainFileCache, "Main", writer, rest);
      diskcache(mSmallFileCache, "Small", writer, rest);
    } else {
      usage(writer);
      if (TextUtils.isEmpty(cmd)) {
        throw new DumpUsageException("Missing command");
      } else {
        throw new DumpUsageException("Unknown command: " + cmd);
      }
    }
  }

  private void diskcache(FileCache cache, String title, PrintStream writer, List<String> args)
      throws DumpException {
    DiskStorage.DiskDumpInfo intDiskDumpInfo;
    try {
      intDiskDumpInfo = cache.getDumpInfo();
    } catch (IOException e) {
      throw new DumpException(e.getMessage());
    }
    if (!args.isEmpty() && args.get(0).equals("-s")) {
      writeDiskDumpInfoScriptReadable(writer, intDiskDumpInfo);
    } else {
      writer.println();
      writer.println(title + " disk cache contents:");
      writeDiskDumpInfo(writer, intDiskDumpInfo);
    }
  }

  private void writeDiskDumpInfo(PrintStream writer, DiskStorage.DiskDumpInfo dumpInfo) {
    if (dumpInfo.entries.isEmpty()) {
      writer.println("Empty");
      return;
    }

    SparseArray<Integer> histogram  = emptyHistogram();
    float total = 0f;
    for (DiskStorage.DiskDumpInfoEntry entry : dumpInfo.entries) {
      writeDiskDumpEntry(writer, entry);
      addToHistogram(histogram, entry);
      total += entry.size;
    }
    writer.println();
    writer.println(formatStrLocaleSafe("Total size: %.1f MB", total / 1024 / KB));
    printFileTypes(writer, dumpInfo);
    printHistogram(writer, histogram);
  }

  private static SparseArray<Integer> emptyHistogram() {
    SparseArray<Integer> histogram = new SparseArray<>();
    histogram.put(0, 0);
    histogram.put(5, 0);
    histogram.put(10, 0);
    histogram.put(20, 0);
    histogram.put(50, 0);
    histogram.put(100, 0);
    histogram.put(200, 0);
    histogram.put(512, 0);
    histogram.put(1024, 0);
    return histogram;
  }

  private void printFileTypes(PrintStream writer, DiskStorage.DiskDumpInfo dumpInfo) {
    writer.println();
    writer.println("File Type Counts:");
    for (String type : dumpInfo.typeCounts.keySet()) {
      writer.println(formatStrLocaleSafe(
          "%4s: %5d",
          type,
          dumpInfo.typeCounts.get(type)));
    }

  }

  private void addToHistogram(
      SparseArray<Integer> histogram,
      DiskStorage.DiskDumpInfoEntry entry) {
    for (int i = 0; i < histogram.size(); i++) {
      int key = histogram.keyAt(i);
      if (entry.size / KB < key) {
        histogram.put(key, histogram.get(key) + 1);
        return;
      }
    }
    // big
    histogram.put((int) (entry.size / KB), 1);
  }

  private void printHistogram(PrintStream writer, SparseArray<Integer> histogram) {
    writer.println();
    writer.println("File Size Counts:");
    for (int i = 1; i < histogram.size(); i++) {
      int lb = histogram.keyAt(i - 1);
      int ub = histogram.keyAt(i);
      writer.println(formatStrLocaleSafe("%4d-%4dK: %3d", lb, ub, histogram.get(ub)));
    }
  }

  private void writeDiskDumpEntry(PrintStream writer, DiskStorage.DiskDumpInfoEntry entry) {
    if (entry.firstBits != null && !entry.firstBits.isEmpty()) {
      writer.println("Undefined: " + entry.firstBits);
    }
    writer.println(formatStrLocaleSafe(
        "type: %5s size: %7.2fkB path: %9s",
        entry.type,
        entry.size / KB,
        entry.path));
  }

  private void writeDiskDumpInfoScriptReadable(
      PrintStream writer, DiskStorage.DiskDumpInfo dumpInfo) {
    for (DiskStorage.DiskDumpInfoEntry entry : dumpInfo.entries) {
      writeDiskDumpEntryScriptReadable(writer, entry);
    }
  }

  private void writeDiskDumpEntryScriptReadable(
      PrintStream writer, DiskStorage.DiskDumpInfoEntry entry) {
    writer.println(formatStrLocaleSafe("%s\t%s", entry.type, entry.path));
  }

  private void writeCacheEntry(
      PrintStream writer,
      CountingMemoryCacheInspector.DumpInfoEntry<CacheKey, CloseableImage> entry) {
    if (!(entry.key instanceof BitmapMemoryCacheKey)) {
      writer.println("Undefined: " + entry.key.getClass());
    }
    BitmapMemoryCacheKey cacheKey = (BitmapMemoryCacheKey) entry.key;
    writer.println(formatStrLocaleSafe(
        "size: %7.2fkB (%4d x %4d) key: %s, %s, duration: %dms",
        entry.value.get().getSizeInBytes() / KB,
        entry.value.get().getWidth(),
        entry.value.get().getHeight(),
        entry.key,
        cacheKey.getCallerContext(),
        RealtimeSinceBootClock.get().now() - cacheKey.getInBitmapCacheSince()));
  }

  private void memcache(PrintStream writer, List<String> args) throws DumpException {
    CountingMemoryCacheInspector.DumpInfo<CacheKey, CloseableImage> dumpInfo =
        mBitmapMemoryCacheInspector.dumpCacheContent();

    try {

      writer.println(mBitmapMemoryCacheInspector.getClass().getSimpleName());
      writer.println();
      writer.println("Params:");
      writer.println(formatStrLocaleSafe(
          "Max size:          %7.2fMB", dumpInfo.maxSize / (1024.0 * KB)));
      writer.println(formatStrLocaleSafe(
          "Max entries count: %9d", dumpInfo.maxEntriesCount));
      writer.println(formatStrLocaleSafe(
          "Max entry size:    %7.2fMB", dumpInfo.maxEntrySize / (1024.0 * KB)));
      writer.println();

      writer.println("Summary of current content:");
      writer.println(formatStrLocaleSafe(
          "Total size:        %7.2fMB (includes in-use content)",
          dumpInfo.size / (1024.0 * KB)));
      writer.println(formatStrLocaleSafe(
          "Entries count:     %9d",
          dumpInfo.lruEntries.size() + dumpInfo.sharedEntries.size()));
      writer.println(formatStrLocaleSafe(
          "LRU size:          %7.2fMB", dumpInfo.lruSize / (1024.0 * KB)));
      writer.println(formatStrLocaleSafe(
          "LRU count:         %9d", dumpInfo.lruEntries.size()));
      writer.println(formatStrLocaleSafe(
          "Shared size:       %7.2fMB",
          (dumpInfo.size - dumpInfo.lruSize) / (1024.0 * KB)));
      writer.println(formatStrLocaleSafe(
          "Shared count:      %9d", dumpInfo.sharedEntries.size()));
      writer.println();

      writer.println("The cache consists of two parts: Things " +
              "currently being used and things not.");
      writer.println("Those things that are *not* currently being used are in the LRU.");
      writer.println("Things currently being used are considered to be shared. They will be added");
      writer.println("to the LRU if/when they stop being used.");
      writer.println();

      writer.println("LRU contents: (things near the top will be evicted first)");
      for (CountingMemoryCacheInspector.DumpInfoEntry entry : dumpInfo.lruEntries) {
        writeCacheEntry(writer, entry);
      }
      writer.println();

      writer.println("Shared contents:");
      for (CountingMemoryCacheInspector.DumpInfoEntry entry : dumpInfo.sharedEntries) {
        writeCacheEntry(writer, entry);
      }

      if (!args.isEmpty() && "-g".equals(args.get(0))) {
        getFiles(writer, dumpInfo);
      }
    } catch (IOException e) {
      throw new DumpException(e.getMessage());
    } finally {
      dumpInfo.release();
    }

  }

  private void getFiles(
      PrintStream writer,
      CountingMemoryCacheInspector.DumpInfo<CacheKey, CloseableImage> dumpInfo)
      throws DumpException, IOException {
    writer.println("\nStoring all images in the memory cache into /sdcard/imagedumperfiles/ ...");

    File dir = new File(Environment.getExternalStorageDirectory().getPath() + "/imagedumperfiles/");
    if (dir.exists() && dir.isDirectory()) {
      File[] files = dir.listFiles();
      if (files != null) {
        for (File file : files) {
          file.delete();
        }
      }
      if (!dir.delete()) {
        throw new DumpException("Failed to clear existing /sdcard/imagedumperfiles directory");
      }
    }
    if (!dir.mkdirs()) {
      throw new DumpException("Failed to create /sdcard/imagedumperfiles directory");
    }
    if (!dumpInfo.lruEntries.isEmpty()) {
      writer.println("LRU Entries:");
      storeEntries(dumpInfo.lruEntries, 1, writer, dir);
    }
    if (!dumpInfo.sharedEntries.isEmpty()) {
      writer.println("Shared Entries:");
      storeEntries(dumpInfo.sharedEntries, dumpInfo.lruEntries.size()+1, writer, dir);
    }

    writer.println("Done!");
  }

  private void storeEntries(
      List<CountingMemoryCacheInspector.DumpInfoEntry<CacheKey, CloseableImage>>
          entries,
      int i,
      PrintStream writer,
      File directory) throws IOException {
    String filename;
    for (CountingMemoryCacheInspector.DumpInfoEntry<CacheKey, CloseableImage>
        entry : entries) {
        CloseableImage closeableImage = entry.value.get();
        if (closeableImage instanceof CloseableBitmap) {
          CloseableBitmap closeableBitmap = (CloseableBitmap) closeableImage;
          filename = "tmp" + i + ".png";
          writer.println(formatStrLocaleSafe(
              "Storing image %d as %s. Key: %s",
              i,
              filename,
              entry.key));
          storeImage(
              closeableBitmap.getUnderlyingBitmap(),
              new File(directory, filename),
              Bitmap.CompressFormat.PNG,
              100);
        } else {
          writer.println(formatStrLocaleSafe(
              "Image %d has unrecognized type %s. Key: %s",
              i,
              closeableImage,
              entry.key));
        }
      i++;
      }
  }

  private void storeImage(
      Bitmap image,
      File pictureFile,
      Bitmap.CompressFormat compressionFormat,
      int quality) throws IOException {
    FileOutputStream fos = null;
    try {
      fos = new FileOutputStream(pictureFile);
      image.compress(compressionFormat, quality, fos);
    } catch (FileNotFoundException e) {
      throw new IOException(e.getMessage());
    } finally {
      if (fos != null) {
        fos.close();
      }
    }
  }

  private static void usage(PrintStream writer) {
    final String cmdName = "dumpapp " + NAME;
    final String usagePrefix = "Usage: " + cmdName + " ";

    writer.println(usagePrefix + "<command> [command-options]");
    writer.println(usagePrefix + "memcache|diskcache");
    writer.println();
    writer.println(cmdName + " memcache: Show contents of bitmap memory cache.");
    writer.println(cmdName + " memcache -g: Get contents of bitmap memory cache and store them" +
            "on the sdcard.");
    writer.println(cmdName + " diskcache: Show contents of disk storage cache.");
    writer.println(cmdName + " diskcache -s: Show contents of disk storage cache formatted " +
            "for script consumption.");
    writer.println();
  }

  private static String formatStrLocaleSafe(String format, Object... args) {
    String str =  String.format(/* locale */ null, "  " + format, args);
    return str;
  }
}
