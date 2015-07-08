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

package com.facebook.samples.comparison.configs.picasso;

import android.content.Context;

import com.squareup.picasso.LruCache;
import com.squareup.picasso.OkHttpDownloader;
import com.squareup.picasso.Picasso;

import com.facebook.samples.comparison.configs.ConfigConstants;

/**
 * Provides instance of Picasso with common configuration for the sample app
 */
public class SamplePicassoFactory {

  private static Picasso sPicasso;

  public static Picasso getPicasso(Context context) {
    if (sPicasso == null) {
        sPicasso = new Picasso.Builder(context)
            .downloader(new OkHttpDownloader(context, ConfigConstants.MAX_DISK_CACHE_SIZE))
            .memoryCache(new LruCache(ConfigConstants.MAX_MEMORY_CACHE_SIZE))
            .build();
    }
    return sPicasso;
  }
}
