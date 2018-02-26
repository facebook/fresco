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

package com.facebook.samples.zoomable;

import android.graphics.Matrix;
import java.util.ArrayList;
import java.util.List;

/**
 * An implementation of {@link ZoomableController.Listener} that allows multiple child listeners to
 * be added and notified about {@link ZoomableController} events.
 */
public class MultiZoomableControllerListener implements ZoomableController.Listener {

  private final List<ZoomableController.Listener> mListeners = new ArrayList<>();

  @Override
  public synchronized void onTransformBegin(Matrix transform) {
    for (ZoomableController.Listener listener : mListeners) {
      listener.onTransformBegin(transform);
    }
  }

  @Override
  public synchronized void onTransformChanged(Matrix transform) {
    for (ZoomableController.Listener listener : mListeners) {
      listener.onTransformChanged(transform);
    }
  }

  @Override
  public synchronized void onTransformEnd(Matrix transform) {
    for (ZoomableController.Listener listener : mListeners) {
      listener.onTransformEnd(transform);
    }
  }

  public synchronized void addListener(ZoomableController.Listener listener) {
    mListeners.add(listener);
  }

  public synchronized void removeListener(ZoomableController.Listener listener) {
    mListeners.remove(listener);
  }
}
