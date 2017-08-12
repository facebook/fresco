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
package com.facebook.samples.scrollperf.data.impl;

import android.content.Context;
import android.net.Uri;
import android.support.annotation.ArrayRes;
import com.facebook.samples.scrollperf.data.SimpleAdapter;

/**
 * This is the implementation of a SimpleAdapter which reads data from an array resource
 */
public class LocalResourceSimpleAdapter implements SimpleAdapter<Uri> {

  private Uri[] mUris;

  private final String[] mSrcArray;

  private final boolean mLazy;

  public static LocalResourceSimpleAdapter getLazyAdapter(
      final Context context,
      @ArrayRes int arrayId) {
    return new LocalResourceSimpleAdapter(context, arrayId, true);
  }

  public static LocalResourceSimpleAdapter getEagerAdapter(
      final Context context,
      @ArrayRes int arrayId) {
    return new LocalResourceSimpleAdapter(context, arrayId, false);
  }

  private LocalResourceSimpleAdapter(final Context context, @ArrayRes int arrayId, boolean lazy) {
    mSrcArray = context.getResources().getStringArray(arrayId);
    mLazy = lazy;
    mUris = new Uri[mSrcArray.length];
    if (!lazy) {
      for (int i = 0; i < mSrcArray.length; i++) {
        mUris[i] = Uri.parse(mSrcArray[i]);
      }
    }
  }

  @Override
  public int getSize() {
    return mSrcArray.length;
  }

  @Override
  public Uri get(int position) {
    if (mLazy && mUris[position] == null) {
      mUris[position] = Uri.parse(mSrcArray[position]);
    }
    return mUris[position];
  }

  @Override
  public boolean isLazy() {
    return mLazy;
  }
}
