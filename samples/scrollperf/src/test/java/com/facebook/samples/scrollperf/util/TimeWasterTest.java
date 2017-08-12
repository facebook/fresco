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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * Test for TimeWaster
 */
public class TimeWasterTest {

  @Test
  public void testFib_0_0() {
    final long result = TimeWaster.Fib(0);
    assertEquals(0, result);
  }

  @Test
  public void testFib_1_1() {
    final long result = TimeWaster.Fib(1);
    assertEquals(1, result);
  }

  @Test
  public void testFib_2_1() {
    final long result = TimeWaster.Fib(2);
    assertEquals(1, result);
  }

  @Test
  public void testFib_5_5() {
    final long result = TimeWaster.Fib(5);
    assertEquals(5, result);
  }

  @Test
  public void testFib_10_55() {
    final long result = TimeWaster.Fib(10);
    assertEquals(55, result);
  }

  @Test
  public void testFib_20_6765() {
    final long result = TimeWaster.Fib(20);
    assertEquals(6765, result);
  }
}
