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

package com.facebook.samples.gestures;

import android.view.MotionEvent;

public class MotionEventTestUtils {
  public static MotionEvent.PointerCoords createCoords(float x, float y) {
    MotionEvent.PointerCoords pointerCoords = new MotionEvent.PointerCoords();
    pointerCoords.x = x;
    pointerCoords.y = y;
    return pointerCoords;
  }

  public static MotionEvent obtainMotionEvent(
      long downTime,
      long eventTime,
      int action,
      int id1,
      float x1,
      float y1) {
    int[] ids = new int[] {id1};
    MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[] {createCoords(x1, y1)};
    return MotionEvent
        .obtain(downTime, eventTime, action, 1, ids, coords, 0, 1.0f, 1.0f, 0, 0, 0, 0);
  }

  public static MotionEvent obtainMotionEvent(
      long downTime,
      long eventTime,
      int action,
      int id1,
      float x1,
      float y1,
      int id2,
      float x2,
      float y2) {
    int[] ids = new int[] {id1, id2};
    MotionEvent.PointerCoords[] coords = new MotionEvent.PointerCoords[] {
        createCoords(x1, y1),
        createCoords(x2, y2)};
    return MotionEvent
        .obtain(downTime, eventTime, action, 2, ids, coords, 0, 1.0f, 1.0f, 0, 0, 0, 0);
  }
}
