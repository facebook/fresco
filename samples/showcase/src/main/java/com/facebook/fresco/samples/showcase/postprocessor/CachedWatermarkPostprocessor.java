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
package com.facebook.fresco.samples.showcase.postprocessor;

import com.facebook.cache.common.CacheKey;
import com.facebook.cache.common.SimpleCacheKey;
import java.util.Locale;

/**
 * Adds a watermark at random positions to the bitmap like {@link WatermarkPostprocessor}. However,
 * this implementation specifies a cache key such that the bitmap stays the same every time the
 * postprocessor is applied.
 */
public class CachedWatermarkPostprocessor extends WatermarkPostprocessor {

  public CachedWatermarkPostprocessor(
      int count,
      String watermarkText) {
    super(count, watermarkText);
  }

  @Override
  public CacheKey getPostprocessorCacheKey() {
    return new SimpleCacheKey(String.format(
        (Locale) null,
        "text=%s,count=%d",
        mWatermarkText,
        mCount));
  }
}
