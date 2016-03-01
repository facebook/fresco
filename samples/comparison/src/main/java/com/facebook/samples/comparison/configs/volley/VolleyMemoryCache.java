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

package com.facebook.samples.comparison.configs.volley;

import android.graphics.Bitmap;
import android.support.v4.util.LruCache;

import com.android.volley.toolbox.ImageLoader;

/**
 * Default bitmap memory cache for Volley.
 */
public class VolleyMemoryCache implements ImageLoader.ImageCache {
  private final LruCache<String, Bitmap> mLruCache;

  public VolleyMemoryCache(int maxSize) {
    mLruCache = new LruCache<String, Bitmap>(maxSize) {
        protected int sizeOf(final String key, final Bitmap value) {
          return value.getRowBytes() * value.getHeight();
        }
      };
  }

  @Override
  public Bitmap getBitmap(String url) {
    return mLruCache.get(url);
  }

  @Override
  public void putBitmap(String url, Bitmap bitmap) {
    mLruCache.put(url, bitmap);
  }

  public void clear() {
    mLruCache.evictAll();
  }
}
