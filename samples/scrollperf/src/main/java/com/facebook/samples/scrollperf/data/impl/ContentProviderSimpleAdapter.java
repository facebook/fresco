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
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import com.facebook.samples.scrollperf.data.SimpleAdapter;

/**
 * This is a SimpleAdapter which which uses a set of elements from a ContentProvider
 */
public class ContentProviderSimpleAdapter implements SimpleAdapter<Uri> {

  private final Uri[] mUris;

  private ContentProviderSimpleAdapter(final Uri baseProvider, Context context) {
    String[] projection = {MediaStore.Images.Media._ID};
    Cursor cursor = context.getContentResolver()
            .query(baseProvider, projection, null, null, null);
    final int columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
    mUris = new Uri[cursor.getCount()];
    int i = 0;
    while (cursor.moveToNext()) {
      final String imageId = cursor.getString(columnIndex);
      mUris[i++] = Uri.withAppendedPath(baseProvider, imageId);
    }
    cursor.close();
  }

  /**
   * Creates and returns a SimpleAdapter for Internal Photos
   *
   * @param context The Context
   * @return The SimpleAdapter for local photo
   */
  public static ContentProviderSimpleAdapter getInternalPhotoSimpleAdapter(Context context) {
    return new ContentProviderSimpleAdapter(MediaStore.Images.Media.INTERNAL_CONTENT_URI, context);
  }

  /**
   * Creates and returns a SimpleAdapter for External Photos
   *
   * @param context The Context
   * @return The SimpleAdapter for local photo
   */
  public static ContentProviderSimpleAdapter getExternalPhotoSimpleAdapter(Context context) {
    return new ContentProviderSimpleAdapter(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, context);
  }

  @Override
  public int getSize() {
    return mUris.length;
  }

  @Override
  public Uri get(int position) {
    return mUris[position];
  }

  @Override
  public boolean isLazy() {
    return false;
  }
}
