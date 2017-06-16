/*
 * This file provided by Facebook is for non-commercial testing and evaluation
 * purposes only.  Facebook reserves all rights not expressly granted.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
 * FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
 * ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.facebook.samples.kotlin

import android.app.Application
import com.facebook.cache.disk.DiskCacheConfig
import com.facebook.common.util.ByteConstants
import com.facebook.drawee.backends.pipeline.Fresco
import com.facebook.imagepipeline.cache.MemoryCacheParams
import com.facebook.imagepipeline.core.ImagePipelineConfig

class KotlinApp : Application() {
  companion object {
    private val MAX_HEAP_SIZE = Runtime.getRuntime().maxMemory().toInt()
    private val MAX_MEMORY_CACHE_SIZE = MAX_HEAP_SIZE / 4
    private const val MAX_DISK_CACHE_SIZE = 40L * ByteConstants.MB
  }

  override fun onCreate() {
    super.onCreate()
    val pipelineConfig = ImagePipelineConfig.newBuilder(this)
        .setBitmapMemoryCacheParamsSupplier {
          MemoryCacheParams(
              MAX_MEMORY_CACHE_SIZE,
              Int.MAX_VALUE,
              MAX_MEMORY_CACHE_SIZE,
              Int.MAX_VALUE,
              Int.MAX_VALUE)
        }
        .setMainDiskCacheConfig(DiskCacheConfig.newBuilder(this)
            .setBaseDirectoryPath(cacheDir)
            .setBaseDirectoryName("stuff")
            .setMaxCacheSize(MAX_DISK_CACHE_SIZE)
            .build())
        .setDownsampleEnabled(true)
        .build()
    Fresco.initialize(this, pipelineConfig)
  }
}
