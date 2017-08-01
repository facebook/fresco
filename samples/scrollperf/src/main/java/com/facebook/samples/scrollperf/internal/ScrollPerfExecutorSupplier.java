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
package com.facebook.samples.scrollperf.internal;

import android.os.Process;
import com.facebook.imagepipeline.core.ExecutorSupplier;
import com.facebook.imagepipeline.core.PriorityThreadFactory;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

/**
 * An ExecutorSupplier we use just for ScrollPerf
 */
public class ScrollPerfExecutorSupplier  implements ExecutorSupplier {

  // Allows for simultaneous reads and writes.
  private static final int NUM_IO_BOUND_THREADS = 2;
  private static final int NUM_LIGHTWEIGHT_BACKGROUND_THREADS = 1;

  private final Executor mIoBoundExecutor;
  private final Executor mDecodeExecutor;
  private final Executor mBackgroundExecutor;
  private final Executor mLightWeightBackgroundExecutor;

  public ScrollPerfExecutorSupplier(int numCpuBoundThreads, int numDecodingThread) {
    ThreadFactory backgroundPriorityThreadFactory =
        new PriorityThreadFactory(Process.THREAD_PRIORITY_BACKGROUND);

    mIoBoundExecutor = Executors.newFixedThreadPool(NUM_IO_BOUND_THREADS);
    mDecodeExecutor = Executors.newFixedThreadPool(
        numDecodingThread,
        backgroundPriorityThreadFactory);
    mBackgroundExecutor = Executors.newFixedThreadPool(
        numCpuBoundThreads,
        backgroundPriorityThreadFactory);
    mLightWeightBackgroundExecutor = Executors.newFixedThreadPool(
        NUM_LIGHTWEIGHT_BACKGROUND_THREADS,
        backgroundPriorityThreadFactory);
  }

  @Override
  public Executor forLocalStorageRead() {
    return mIoBoundExecutor;
  }

  @Override
  public Executor forLocalStorageWrite() {
    return mIoBoundExecutor;
  }

  @Override
  public Executor forDecode() {
    return mDecodeExecutor;
  }

  @Override
  public Executor forBackgroundTasks() {
    return mBackgroundExecutor;
  }

  @Override
  public Executor forLightweightBackgroundTasks() {
    return mLightWeightBackgroundExecutor;
  }
}
