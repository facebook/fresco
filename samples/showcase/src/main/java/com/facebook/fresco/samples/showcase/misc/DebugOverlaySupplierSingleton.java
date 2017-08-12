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
package com.facebook.fresco.samples.showcase.misc;

import android.content.Context;
import android.content.SharedPreferences;
import android.support.v7.preference.PreferenceManager;
import com.facebook.common.internal.Supplier;
import com.facebook.fresco.samples.showcase.settings.SettingsFragment;

public class DebugOverlaySupplierSingleton implements Supplier<Boolean> {

  private static final String KEY_DEBUG_OVERLAY = SettingsFragment.KEY_DEBUG_OVERLAY;

  private static DebugOverlaySupplierSingleton sInstance;

  private final SharedPreferences mSharedPreferences;

  private DebugOverlaySupplierSingleton(Context context) {
    mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
  }

  public static DebugOverlaySupplierSingleton getInstance(Context context) {
    synchronized (DebugOverlaySupplierSingleton.class) {
      if (sInstance == null) {
        sInstance = new DebugOverlaySupplierSingleton(context.getApplicationContext());
      }
      return sInstance;
    }
  }

  @Override
  public Boolean get() {
    return mSharedPreferences.getBoolean(KEY_DEBUG_OVERLAY, false);
  }
}
