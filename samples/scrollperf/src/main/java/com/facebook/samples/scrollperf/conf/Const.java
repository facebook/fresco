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
package com.facebook.samples.scrollperf.conf;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;

/**
 * Constants for the Application
 */
public final class Const {

  public static final Drawable PLACEHOLDER = new ColorDrawable(Color.GRAY);

  public static final Drawable FAILURE = new ColorDrawable(Color.RED);

  public static final double RATIO = 4.0 / 3.0;

  public static final String DATA_SOURCE_KEY = "uris_data_source";
  public static final String INFINITE_DATA_SOURCE_KEY = "infinite_data_source";
  public static final String DISTINCT_DATA_SOURCE_KEY ="distinct_uri_data_source";
  public static final String RECYCLER_LAYOUT_KEY = "recycler_layout";
  public static final String REUSE_OLD_CONTROLLER_KEY= "reuse_old_controller";
  public static final String ROUNDED_CORNERS_KEY= "rounded_corners";
  public static final String ROUNDED_AS_CIRCLE_KEY= "rounded_as_circle";
}
