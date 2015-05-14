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

package com.facebook.samples.comparison.configs.glide;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.load.engine.cache.DiskCache;
import com.bumptech.glide.load.engine.cache.DiskLruCacheWrapper;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.GlideModule;

import com.facebook.samples.comparison.configs.ConfigConstants;

/**
 * {@link com.bumptech.glide.module.GlideModule} implementation for the sample app.
 */
public class SampleGlideModule implements GlideModule {
  @Override
  public void applyOptions(final Context context, GlideBuilder builder) {
    builder.setDiskCache(
        new DiskCache.Factory() {
          @Override
          public DiskCache build() {
            return DiskLruCacheWrapper.get(
                Glide.getPhotoCacheDir(context),
                ConfigConstants.MAX_DISK_CACHE_SIZE);
          }
        });
    builder.setMemoryCache(new LruResourceCache(ConfigConstants.MAX_MEMORY_CACHE_SIZE));
  }

  @Override
  public void registerComponents(Context context, Glide glide) {
  }
}
