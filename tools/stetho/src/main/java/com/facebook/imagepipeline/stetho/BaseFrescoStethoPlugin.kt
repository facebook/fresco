/*
 * Copyright (c) Meta Platforms, Inc. and affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

package com.facebook.imagepipeline.stetho

import android.util.SparseIntArray
import com.facebook.cache.common.CacheKey
import com.facebook.cache.disk.DiskStorage.DiskDumpInfo
import com.facebook.cache.disk.DiskStorage.DiskDumpInfoEntry
import com.facebook.cache.disk.FileCache
import com.facebook.common.internal.Supplier
import com.facebook.common.time.RealtimeSinceBootClock
import com.facebook.imagepipeline.cache.BitmapMemoryCacheKey
import com.facebook.imagepipeline.cache.CountingMemoryCacheInspector
import com.facebook.imagepipeline.cache.CountingMemoryCacheInspector.DumpInfo
import com.facebook.imagepipeline.cache.CountingMemoryCacheInspector.DumpInfoEntry
import com.facebook.imagepipeline.core.DiskCachesStore
import com.facebook.imagepipeline.core.ImagePipeline
import com.facebook.imagepipeline.core.ImagePipelineFactory
import com.facebook.imagepipeline.image.CloseableImage
import com.facebook.stetho.dumpapp.DumpException
import com.facebook.stetho.dumpapp.DumpUsageException
import com.facebook.stetho.dumpapp.DumperContext
import com.facebook.stetho.dumpapp.DumperPlugin
import java.io.IOException
import java.io.PrintStream
import java.util.ArrayList

/**
 * Base class for the Fresco Stetho plugin.
 *
 * Applications should instantiate
 */
abstract class BaseFrescoStethoPlugin() : DumperPlugin {

  protected var initialized = false
  private var bitmapMemoryCacheInspector: CountingMemoryCacheInspector<CacheKey, CloseableImage>? =
      null
  private lateinit var diskCachesStoreSupplier: Supplier<DiskCachesStore>
  private var imagePipeline: ImagePipeline? = null

  protected constructor(factory: ImagePipelineFactory) : this() {
    initialize(factory)
  }

  @Throws(DumpException::class) protected abstract fun ensureInitialized()

  protected fun initialize(factory: ImagePipelineFactory) {
    bitmapMemoryCacheInspector = CountingMemoryCacheInspector(factory.bitmapCountingMemoryCache)
    diskCachesStoreSupplier = factory.diskCachesStoreSupplier
    imagePipeline = factory.imagePipeline
    initialized = true
  }

  override fun getName(): String = NAME

  /**
   * Entry point for the Stetho dumpapp script.
   *
   * [initialize] must have been called in the app before running dumpapp.
   */
  @Throws(DumpException::class)
  override fun dump(dumpContext: DumperContext) {
    ensureInitialized()
    val args = dumpContext.argsAsList
    val writer = dumpContext.stdout
    val cmd = if (args.isEmpty()) null else args[0]
    val rest: List<String> = if (args.isEmpty()) ArrayList() else args.subList(1, args.size)
    when {
      cmd != null && cmd == "memcache" -> memcache(writer, rest)
      cmd != null && cmd == "diskcache" -> {
        val mainFileCache = checkNotNull(this.diskCachesStoreSupplier.get().mainFileCache)
        val smallFileCache = checkNotNull(this.diskCachesStoreSupplier.get().smallImageFileCache)

        diskcache(mainFileCache, "Main", writer, rest)
        diskcache(smallFileCache, "Small", writer, rest)
      }
      cmd != null && cmd == "clear" -> {
        val imagePipeline = checkNotNull(this.imagePipeline)
        imagePipeline.clearCaches()
      }
      else -> {
        usage(writer)
        if (cmd.isNullOrEmpty()) {
          throw DumpUsageException("Missing command")
        } else {
          throw DumpUsageException("Unknown command: $cmd")
        }
      }
    }
  }

  @Throws(DumpException::class)
  private fun diskcache(cache: FileCache, title: String, writer: PrintStream, args: List<String>) {
    val intDiskDumpInfo =
        try {
          cache.dumpInfo
        } catch (e: IOException) {
          throw DumpException(e.message)
        }
    if (args.isNotEmpty() && args[0] == "-s") {
      writeDiskDumpInfoScriptReadable(writer, intDiskDumpInfo)
    } else {
      writer.println()
      writer.println("$title disk cache contents:")
      writeDiskDumpInfo(writer, intDiskDumpInfo)
    }
  }

  private fun writeDiskDumpInfo(writer: PrintStream, dumpInfo: DiskDumpInfo) {
    if (dumpInfo.entries.isEmpty()) {
      writer.println("Empty")
      return
    }
    val histogram = emptyHistogram()
    var total = 0f
    for (entry in dumpInfo.entries) {
      writeDiskDumpEntry(writer, entry)
      addToHistogram(histogram, entry)
      total += entry.size
    }
    writer.println()
    writer.println(formatStrLocaleSafe("Total size: %.1f MB", total / 1_024 / KB))
    printFileTypes(writer, dumpInfo)
    printHistogram(writer, histogram)
  }

  private fun printFileTypes(writer: PrintStream, dumpInfo: DiskDumpInfo) {
    writer.println()
    writer.println("File Type Counts:")
    for (type in dumpInfo.typeCounts.keys) {
      val typeCounts = checkNotNull(dumpInfo.typeCounts[type])
      writer.println(formatStrLocaleSafe("%4s: %5d", type, typeCounts))
    }
  }

  private fun addToHistogram(histogram: SparseIntArray, entry: DiskDumpInfoEntry) {
    for (i in 0 until histogram.size()) {
      val key = histogram.keyAt(i)
      if (entry.size / KB < key) {
        histogram.put(key, histogram[key] + 1)
        return
      }
    }
    // big
    histogram.put((entry.size / KB).toInt(), 1)
  }

  private fun printHistogram(writer: PrintStream, histogram: SparseIntArray) {
    writer.println()
    writer.println("File Size Counts:")
    for (i in 1 until histogram.size()) {
      val lb = histogram.keyAt(i - 1)
      val ub = histogram.keyAt(i)
      writer.println(formatStrLocaleSafe("%4d-%4dK: %3d", lb, ub, histogram[ub]))
    }
  }

  private fun writeDiskDumpEntry(writer: PrintStream, entry: DiskDumpInfoEntry) {
    if (entry.firstBits != null && entry.firstBits.isNotEmpty()) {
      writer.println("Undefined: ${entry.firstBits}")
    }
    writer.println(
        formatStrLocaleSafe(
            "type: %5s size: %7.2fkB path: %9s", entry.type, entry.size / KB, entry.path))
  }

  private fun writeDiskDumpInfoScriptReadable(writer: PrintStream, dumpInfo: DiskDumpInfo) {
    for (entry in dumpInfo.entries) {
      writeDiskDumpEntryScriptReadable(writer, entry)
    }
  }

  private fun writeDiskDumpEntryScriptReadable(writer: PrintStream, entry: DiskDumpInfoEntry) {
    writer.println(formatStrLocaleSafe("%s\t%s", entry.type, entry.path))
  }

  private fun writeCacheEntry(writer: PrintStream, entry: DumpInfoEntry<CacheKey, CloseableImage>) {
    if (entry.key !is BitmapMemoryCacheKey) {
      writer.println("Undefined: ${entry.key.javaClass}")
    }
    val cacheKey = entry.key as BitmapMemoryCacheKey
    val entryValue = checkNotNull(entry.value)
    writer.println(
        formatStrLocaleSafe(
            "size: %7.2fkB (%4d x %4d) key: %s, duration: %dms",
            entryValue.get().sizeInBytes / KB,
            entryValue.get().width,
            entryValue.get().height,
            entry.key,
            RealtimeSinceBootClock.get().now() - cacheKey.inBitmapCacheSince))
  }

  @Throws(DumpException::class)
  private fun memcache(writer: PrintStream, args: List<String>) {
    val bitmapMemoryCacheInspector = checkNotNull(this.bitmapMemoryCacheInspector)
    val dumpInfo: DumpInfo<CacheKey, CloseableImage> = bitmapMemoryCacheInspector.dumpCacheContent()
    try {
      writer.println(bitmapMemoryCacheInspector.javaClass.simpleName)
      writer.println()
      writer.println("Params:")
      writer.println(
          formatStrLocaleSafe("Max size:          %7.2fMB", dumpInfo.maxSize / (1_024.0 * KB)))
      writer.println(formatStrLocaleSafe("Max entries count: %9d", dumpInfo.maxEntriesCount))
      writer.println(
          formatStrLocaleSafe("Max entry size:    %7.2fMB", dumpInfo.maxEntrySize / (1_024.0 * KB)))
      writer.println()
      writer.println("Summary of current content:")
      writer.println(
          formatStrLocaleSafe(
              "Total size:        %7.2fMB (includes in-use content)",
              dumpInfo.size / (1_024.0 * KB)))
      writer.println(
          formatStrLocaleSafe(
              "Entries count:     %9d", dumpInfo.lruEntries.size + dumpInfo.sharedEntries.size))
      writer.println(
          formatStrLocaleSafe("LRU size:          %7.2fMB", dumpInfo.lruSize / (1_024.0 * KB)))
      writer.println(formatStrLocaleSafe("LRU count:         %9d", dumpInfo.lruEntries.size))
      writer.println(
          formatStrLocaleSafe(
              "Shared size:       %7.2fMB", (dumpInfo.size - dumpInfo.lruSize) / (1_024.0 * KB)))
      writer.println(formatStrLocaleSafe("Shared count:      %9d", dumpInfo.sharedEntries.size))
      writer.println()
      writer.println("The cache consists of two parts: Things currently being used and things not.")
      writer.println("Those things that are *not* currently being used are in the LRU.")
      writer.println("Things currently being used are considered to be shared. They will be added")
      writer.println("to the LRU if/when they stop being used.")
      writer.println()
      writer.println("LRU contents: (things near the top will be evicted first)")
      for (entry in dumpInfo.lruEntries) {
        writeCacheEntry(writer, entry)
      }
      writer.println()
      writer.println("Shared contents:")
      for (entry in dumpInfo.sharedEntries) {
        writeCacheEntry(writer, entry)
      }
    } catch (e: IOException) {
      throw DumpException(e.message)
    } finally {
      dumpInfo.release()
    }
  }

  companion object {
    private const val NAME = "image"
    private const val KB = 1_024f

    private fun emptyHistogram(): SparseIntArray {
      val histogram = SparseIntArray()
      histogram.put(0, 0)
      histogram.put(5, 0)
      histogram.put(10, 0)
      histogram.put(20, 0)
      histogram.put(50, 0)
      histogram.put(100, 0)
      histogram.put(200, 0)
      histogram.put(512, 0)
      histogram.put(1_024, 0)
      return histogram
    }

    private fun usage(writer: PrintStream) {
      val cmdName = "dumpapp ${NAME}"
      val usagePrefix = "Usage: $cmdName "
      writer.println("$usagePrefix<command> [command-options]")
      writer.println("${usagePrefix}memcache|diskcache")
      writer.println()
      writer.println("$cmdName memcache: Show contents of bitmap memory cache.")
      writer.println(
          "$cmdName memcache -g: Get contents of bitmap memory cache and store themon the sdcard.")
      writer.println("$cmdName diskcache: Show contents of disk storage cache.")
      writer.println(
          "$cmdName diskcache -s: Show contents of disk storage cache formatted for script consumption.")
      writer.println("$cmdName clear: Clear all caches.")
      writer.println()
    }

    private fun formatStrLocaleSafe(format: String, vararg args: Any): String =
        String.format(/* locale */ null, "  $format", *args)
  }
}
