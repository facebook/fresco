/*
 * Copyright (c) 2015-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.facebook.imagepipeline.producers;

import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

import android.os.SystemClock;

import com.facebook.common.references.CloseableReference;
import com.facebook.imagepipeline.image.EncodedImage;
import com.facebook.imagepipeline.memory.PooledByteBuffer;
import com.facebook.imagepipeline.testing.FakeClock;
import com.facebook.imagepipeline.testing.TestExecutorService;
import com.facebook.imagepipeline.testing.TestScheduledExecutorService;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.rule.PowerMockRule;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;

@RunWith(RobolectricTestRunner.class)
@PowerMockIgnore({ "org.mockito.*", "org.robolectric.*", "android.*" })
@Config(manifest= Config.NONE)
@PrepareForTest({SystemClock.class, JobScheduler.JobStartExecutorSupplier.class})
public class JobSchedulerTest {

  private static final int INTERVAL = 100;

  @Rule
  public PowerMockRule rule = new PowerMockRule();

  private static class TestJobRunnable implements JobScheduler.JobRunnable {

    private static class Job {
      public final EncodedImage encodedImage;
      public final boolean isLast;

      public Job(EncodedImage encodedImage, boolean isLast) {
        this.encodedImage = EncodedImage.cloneOrNull(encodedImage);
        this.isLast = isLast;
      }
    }

    public final AtomicBoolean running = new AtomicBoolean();
    public final AtomicBoolean wait = new AtomicBoolean();
    public final AtomicBoolean fail = new AtomicBoolean();
    public final ArrayList<Job> jobs = new ArrayList<>();

    @Override
    public void run(EncodedImage encodedImage, boolean isLast) {
      running.set(true);
      try {
        waitForCondition(wait, false);
        if (fail.get()) {
          throw new RuntimeException();
        } else {
          jobs.add(new Job(encodedImage, isLast));
        }
      } finally {
        running.set(false);
      }
    }
  }

  private FakeClock mFakeClockForTime;
  private FakeClock mFakeClockForWorker;
  private TestExecutorService mTestExecutorService;
  private FakeClock mFakeClockForScheduled;
  private TestScheduledExecutorService mTestScheduledExecutorService;
  private TestJobRunnable mTestJobRunnable;
  private JobScheduler mJobScheduler;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    mFakeClockForTime = new FakeClock();
    mFakeClockForWorker = new FakeClock();
    mFakeClockForScheduled = new FakeClock();
    mFakeClockForTime.incrementBy(1000);
    mFakeClockForWorker.incrementBy(1000);
    mFakeClockForScheduled.incrementBy(1000);
    PowerMockito.mockStatic(SystemClock.class);
    when(SystemClock.uptimeMillis()).thenAnswer(
        new Answer<Long>() {
          @Override
          public Long answer(InvocationOnMock invocation) throws Throwable {
            return mFakeClockForTime.now();
          }
        });

    mTestExecutorService = new TestExecutorService(mFakeClockForWorker);
    mTestScheduledExecutorService = new TestScheduledExecutorService(mFakeClockForScheduled);
    PowerMockito.mockStatic(JobScheduler.JobStartExecutorSupplier.class);
    when(JobScheduler.JobStartExecutorSupplier.get()).thenReturn(mTestScheduledExecutorService);

    mTestJobRunnable = new TestJobRunnable();
    mJobScheduler = new JobScheduler(mTestExecutorService, mTestJobRunnable, INTERVAL);
  }

  private EncodedImage fakeEncodedImage() {
    PooledByteBuffer buf = mock(PooledByteBuffer.class);
    CloseableReference<PooledByteBuffer> ref = CloseableReference.of(buf);
    return new EncodedImage(ref);
  }

  @Test
  public void testUpdate_Intermediate() {
    EncodedImage encodedImage = fakeEncodedImage();
    assertTrue(mJobScheduler.updateJob(encodedImage, false));
    assertNotSame(encodedImage, mJobScheduler.mEncodedImage);
    assertReferencesEqual(encodedImage, mJobScheduler.mEncodedImage);
    assertEquals(false, mJobScheduler.mIsLast);
    assertEquals(0, mTestScheduledExecutorService.getPendingCount());
    assertEquals(0, mTestExecutorService.getPendingCount());
    assertEquals(0, mTestJobRunnable.jobs.size());
  }

  @Test
  public void testUpdate_Intermediate_Invalid() {
    assertTrue(mJobScheduler.updateJob(fakeEncodedImage(), false));
    assertFalse(mJobScheduler.updateJob(null, false));
    assertNotNull(mJobScheduler.mEncodedImage);
    assertEquals(false, mJobScheduler.mIsLast);
    assertEquals(0, mTestScheduledExecutorService.getPendingCount());
    assertEquals(0, mTestExecutorService.getPendingCount());
    assertEquals(0, mTestJobRunnable.jobs.size());
  }

  @Test
  public void testUpdate_Last() {
    EncodedImage encodedImage = fakeEncodedImage();
    assertTrue(mJobScheduler.updateJob(encodedImage, true));
    assertNotSame(encodedImage, mJobScheduler.mEncodedImage);
    assertReferencesEqual(encodedImage, mJobScheduler.mEncodedImage);
    assertEquals(true, mJobScheduler.mIsLast);
    assertEquals(0, mTestScheduledExecutorService.getPendingCount());
    assertEquals(0, mTestExecutorService.getPendingCount());
    assertEquals(0, mTestJobRunnable.jobs.size());
  }

  @Test
  public void testUpdate_Last_Null() {
    assertTrue(mJobScheduler.updateJob(fakeEncodedImage(), false));
    assertTrue(mJobScheduler.updateJob(null, true));
    assertEquals(null, mJobScheduler.mEncodedImage);
    assertEquals(true, mJobScheduler.mIsLast);
    assertEquals(0, mTestScheduledExecutorService.getPendingCount());
    assertEquals(0, mTestExecutorService.getPendingCount());
    assertEquals(0, mTestJobRunnable.jobs.size());
  }

  @Test
  public void testClear() throws Exception {
    EncodedImage encodedImage = fakeEncodedImage();
    mJobScheduler.updateJob(encodedImage, true);
    mJobScheduler.clearJob();
    assertEquals(null, mJobScheduler.mEncodedImage);
    encodedImage.close();
    assertNull(encodedImage.getByteBufferRef());
  }

  @Test
  public void testSchedule_Intermediate() {
    EncodedImage encodedImage = fakeEncodedImage();
    mJobScheduler.updateJob(encodedImage, false);

    assertTrue(mJobScheduler.scheduleJob());
    assertEquals(0, mTestScheduledExecutorService.getPendingCount());
    assertEquals(1, mTestExecutorService.getPendingCount());
    assertEquals(0, mTestJobRunnable.jobs.size());

    mFakeClockForTime.incrementBy(1234);
    mFakeClockForWorker.incrementBy(1234);
    mFakeClockForScheduled.incrementBy(1234);
    assertEquals(1, mTestJobRunnable.jobs.size());
    assertJobsEqual(mTestJobRunnable.jobs.get(0), encodedImage, false);
  }

  @Test
  public void testSchedule_Intermediate_Invalid() {
    mJobScheduler.updateJob(null, false);
    assertFalse(mJobScheduler.scheduleJob());
    assertEquals(0, mTestScheduledExecutorService.getPendingCount());
    assertEquals(0, mTestExecutorService.getPendingCount());
    assertEquals(0, mTestJobRunnable.jobs.size());
  }

  @Test
  public void testSchedule_Last_Null() {
    mJobScheduler.updateJob(null, true);
    assertTrue(mJobScheduler.scheduleJob());
    assertEquals(0, mTestScheduledExecutorService.getPendingCount());
    assertEquals(1, mTestExecutorService.getPendingCount());
    assertEquals(0, mTestJobRunnable.jobs.size());

    mFakeClockForTime.incrementBy(1234);
    mFakeClockForWorker.incrementBy(1234);
    mFakeClockForScheduled.incrementBy(1234);
    assertEquals(1, mTestJobRunnable.jobs.size());
    assertJobsEqual(mTestJobRunnable.jobs.get(0), null, true);
  }

  @Test
  public void testSchedule_Last_Idle() throws Exception {
    EncodedImage encodedImage = fakeEncodedImage();
    mJobScheduler.updateJob(encodedImage, true);

    assertEquals(JobScheduler.JobState.IDLE, mJobScheduler.mJobState);
    assertTrue(mJobScheduler.scheduleJob());
    assertEquals(0, mTestScheduledExecutorService.getPendingCount());
    assertEquals(1, mTestExecutorService.getPendingCount());
    assertEquals(0, mTestJobRunnable.jobs.size());

    mFakeClockForTime.incrementBy(1234);
    mFakeClockForWorker.incrementBy(1234);
    mFakeClockForScheduled.incrementBy(1234);
    assertEquals(1, mTestJobRunnable.jobs.size());
    assertJobsEqual(mTestJobRunnable.jobs.get(0), encodedImage, true);

    mTestJobRunnable.jobs.get(0).encodedImage.close();
    encodedImage.close();
    assertNull(encodedImage.getByteBufferRef());
  }

  @Test
  public void testSchedule_Last_Queued() {
    mJobScheduler.updateJob(fakeEncodedImage(), true);
    assertTrue(mJobScheduler.scheduleJob());

    EncodedImage encodedImage2 = fakeEncodedImage();
    mJobScheduler.updateJob(encodedImage2, true);
    assertEquals(JobScheduler.JobState.QUEUED, mJobScheduler.mJobState);
    assertTrue(mJobScheduler.scheduleJob());

    assertEquals(0, mTestScheduledExecutorService.getPendingCount());
    assertEquals(1, mTestExecutorService.getPendingCount());
    assertEquals(0, mTestJobRunnable.jobs.size());

    mFakeClockForTime.incrementBy(1234);
    mFakeClockForWorker.incrementBy(1234);
    mFakeClockForScheduled.incrementBy(1234);
    assertEquals(1, mTestJobRunnable.jobs.size());
    assertJobsEqual(mTestJobRunnable.jobs.get(0), encodedImage2, true);
  }

  @Test
  public void testSchedule_Last_Running_And_Pending() {
    EncodedImage encodedImage1 = fakeEncodedImage();
    mJobScheduler.updateJob(encodedImage1, true);
    assertTrue(mJobScheduler.scheduleJob());

    final EncodedImage encodedImage2 = fakeEncodedImage();
    final EncodedImage encodedImage3 = fakeEncodedImage();

    Executors.newFixedThreadPool(1).execute(
        new Runnable() {
          @Override
          public void run() {
            // wait until the job starts running
            waitForCondition(mTestJobRunnable.running, true);
            assertEquals(0, mTestScheduledExecutorService.getPendingCount());
            assertEquals(0, mTestExecutorService.getPendingCount());
            assertEquals(0, mTestJobRunnable.jobs.size());

            mJobScheduler.updateJob(encodedImage2, true);
            assertEquals(JobScheduler.JobState.RUNNING, mJobScheduler.mJobState);
            assertTrue(mJobScheduler.scheduleJob());
            assertEquals(JobScheduler.JobState.RUNNING_AND_PENDING, mJobScheduler.mJobState);

            mJobScheduler.updateJob(encodedImage3, true);
            assertEquals(JobScheduler.JobState.RUNNING_AND_PENDING, mJobScheduler.mJobState);
            assertTrue(mJobScheduler.scheduleJob());

            assertEquals(JobScheduler.JobState.RUNNING_AND_PENDING, mJobScheduler.mJobState);
            mTestJobRunnable.wait.set(false);
          }
        });

    // block running until the above code executed on another thread finishes
    mTestJobRunnable.wait.set(true);

    mFakeClockForTime.incrementBy(0);
    mFakeClockForScheduled.incrementBy(0);
    mFakeClockForWorker.incrementBy(0); // this line blocks
    assertEquals(JobScheduler.JobState.QUEUED, mJobScheduler.mJobState);
    assertEquals(1, mTestScheduledExecutorService.getPendingCount());
    assertEquals(0, mTestExecutorService.getPendingCount());
    assertEquals(1, mTestJobRunnable.jobs.size());
    assertJobsEqual(mTestJobRunnable.jobs.get(0), encodedImage1, true);

    mFakeClockForTime.incrementBy(INTERVAL);
    mFakeClockForScheduled.incrementBy(INTERVAL);
    mFakeClockForWorker.incrementBy(INTERVAL);
    assertEquals(JobScheduler.JobState.IDLE, mJobScheduler.mJobState);
    assertEquals(0, mTestScheduledExecutorService.getPendingCount());
    assertEquals(0, mTestExecutorService.getPendingCount());
    assertEquals(2, mTestJobRunnable.jobs.size());
    assertJobsEqual(mTestJobRunnable.jobs.get(1), encodedImage3, true);
  }

  @Test
  public void testSchedule_TooSoon() {
    EncodedImage encodedImage1 = fakeEncodedImage();
    mJobScheduler.updateJob(encodedImage1, false);
    mJobScheduler.scheduleJob();
    mFakeClockForTime.incrementBy(1234);
    mFakeClockForWorker.incrementBy(1234);
    mFakeClockForScheduled.incrementBy(1234);

    EncodedImage encodedImage2 = fakeEncodedImage();
    mJobScheduler.updateJob(encodedImage2, true);
    mFakeClockForTime.incrementBy(INTERVAL - 5);
    mFakeClockForWorker.incrementBy(INTERVAL - 5);
    mFakeClockForScheduled.incrementBy(INTERVAL - 5);
    mJobScheduler.scheduleJob();
    mFakeClockForTime.incrementBy(0);
    mFakeClockForWorker.incrementBy(0);
    mFakeClockForScheduled.incrementBy(0);
    assertEquals(1, mTestScheduledExecutorService.getPendingCount());
    assertEquals(5, mTestScheduledExecutorService.getScheduledQueue().getNextPendingCommandDelay());
    assertEquals(0, mTestExecutorService.getPendingCount());
    assertEquals(1, mTestJobRunnable.jobs.size());

    mFakeClockForTime.incrementBy(5);
    mFakeClockForWorker.incrementBy(5);
    mFakeClockForScheduled.incrementBy(5);
    assertEquals(0, mTestScheduledExecutorService.getPendingCount());
    assertEquals(1, mTestExecutorService.getPendingCount());
    assertEquals(1, mTestJobRunnable.jobs.size());

    mFakeClockForTime.incrementBy(0);
    mFakeClockForWorker.incrementBy(0);
    mFakeClockForScheduled.incrementBy(0);
    assertEquals(0, mTestScheduledExecutorService.getPendingCount());
    assertEquals(0, mTestExecutorService.getPendingCount());
    assertEquals(2, mTestJobRunnable.jobs.size());
    assertJobsEqual(mTestJobRunnable.jobs.get(1), encodedImage2, true);
  }

  @Test
  public void testFailure() {
    mJobScheduler.updateJob(fakeEncodedImage(), false);
    mJobScheduler.scheduleJob();
    mTestJobRunnable.fail.set(true);
    try {
      mFakeClockForTime.incrementBy(1234);
      mFakeClockForWorker.incrementBy(0);
      fail("job should have failed, but it didn't.");
    } catch (Exception e) {
      // expected
    }
    assertEquals(JobScheduler.JobState.IDLE, mJobScheduler.mJobState);
    assertEquals(0, mTestScheduledExecutorService.getPendingCount());
    assertEquals(0, mTestExecutorService.getPendingCount());
    assertEquals(0, mTestJobRunnable.jobs.size());
  }

  private static void assertJobsEqual(
      TestJobRunnable.Job job,
      EncodedImage encodedImage,
      boolean isLast) {
    assertReferencesEqual(encodedImage, job.encodedImage);
    assertEquals(isLast, job.isLast);
  }

  private static void assertReferencesEqual(EncodedImage expected, EncodedImage actual) {
    if (expected == null) {
      assertNull(actual);
    } else {
      if (expected.getByteBufferRef() == null) {
        assertNull(actual.getByteBufferRef());
      } else {
        assertNotNull(actual.getByteBufferRef());
        assertEquals(expected.isValid(), actual.isValid());
        assertEquals(
            expected.getByteBufferRef().getUnderlyingReferenceTestOnly(),
            actual.getByteBufferRef().getUnderlyingReferenceTestOnly());
      }
    }
  }

  private static void waitForCondition(AtomicBoolean wait, boolean value) {
    int cnt = 100;
    while (wait.get() != value) {
      try {
        Thread.sleep(1);
      } catch (InterruptedException e) {
        // ignore
      }
      if (--cnt < 0) {
        throw new RuntimeException("Waited for too long!");
      }
    }
  }
}
