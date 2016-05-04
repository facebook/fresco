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
package com.facebook.samples.scrollperf.util;

import android.app.Activity;
import android.view.View;

/**
 * Utility class to get View references using type inference
 */
public final class UI {

  /**
   * This method returns the reference of the View with the given Id in the layout of the
   * Activity passed as parameter
   *
   * @param act    The Activity that is using the layout with the given View
   * @param viewId The id of the View we want to get a reference
   * @return The View with the given id and type
   */
  public static <T extends View> T findViewById(Activity act, int viewId) {
    View containerView = act.getWindow().getDecorView();
    return findViewById(containerView, viewId);
  }

  /**
   * This method returns the reference of the View with the given Id in the view passed
   * as parameter
   *
   * @param containerView The container View
   * @param viewId        The id of the View we want to get a reference
   * @return The View with the given id and type
   */
  @SuppressWarnings("unchecked")
  public static <T extends View> T findViewById(View containerView, int viewId) {
    View foundView = containerView.findViewById(viewId);
    return (T) foundView;
  }
}
