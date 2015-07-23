#!/usr/bin/env python

# This file provided by Facebook is for non-commercial testing and evaluation
# purposes only.  Facebook reserves all rights not expressly granted.
#
# THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
# IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
# FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL
# FACEBOOK BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN
# ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
# CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

"""
This script runs a comparative test with the sample app.

It builds and runs the sample app, switching from one library to the next,
taking measurements as it goes.

To select a subset of the libraries, use the -s option with a
space-separated list.
"""

from __future__ import absolute_import
from __future__ import division
from __future__ import print_function
from __future__ import unicode_literals

import argparse
import glob
import os
import re
import tempfile

from collections import namedtuple
from subprocess import check_call, PIPE, Popen

""" List of tested libraries """
TESTS = (
    'fresco',
    'fresco-okhttp',
    'glide',
    'picasso',
    'uil',
    'volley',
    'drawee-volley'
)

TEST_SOURCES = (
    'network',
    'local'
)

ABIS = (
    'arm64-v8a',
    'armeabi',
    'armeabi-v7a',
    'x86',
    'x86_64'
)


""" Appends test class name to method name """
TEST_PATTERN = 'test{}{}'

""" Named tuple containing relevant numbers reported by a test """
Stats = namedtuple('Stats', [
    'success_wait_times',
    'failure_wait_times',
    'cancellation_wait_times',
    'java_heap_sizes',
    'native_heap_sizes',
    'skipped_frames'])


def parse_args():
    parser = argparse.ArgumentParser(
        description='Runs comparison test and processes results')
    parser.add_argument('-s', '--scenarios', choices=TESTS, nargs='+')
    parser.add_argument('-d', '--sources', choices=TEST_SOURCES, nargs='+')
    parser.add_argument('-c', '--cpu', choices=ABIS, required=True)
    return parser.parse_args()


def start_subprocess(command, **kwargs):
    """ Starts subprocess after printing command to stdout. """
    return Popen(command.split(), **kwargs)


def run_command(command):
    """ Runs given command and waits for it to terminate.
        Prints the command to stdout and redirects its output to /dev/null. """
    with open('/dev/null', 'w') as devnull:
        check_call(command.split(), stdout=devnull, stderr=devnull)


def gradle(*tasks):
    """ Runs given gradle tasks """
    if tasks:
        run_command('./gradlew {}'.format(" ".join(tasks)))


def adb(command):
    """ Runs adb command - arguments are given as single string"""
    run_command('adb {}'.format(command))


def install_apks(abi):
    """ Installs comparison app and test apks """
    print("Installing comparison app...")
    gradle(':samples:comparison:assembleDebug',
           ':samples:comparison:assembleDebugAndroidTest')
    cmd = ('install -r samples/comparison/build/outputs/apk/comparison-'
           '{}-debug.apk'.format(abi))
    adb(cmd)
    adb('install -r samples/comparison/build/outputs/apk/'
        'comparison-debug-androidTest-unaligned.apk')


class ComparisonTest:
    """ Comparison test case """
    def __init__(
            self,
            method_name,
            class_name='com.facebook.samples.comparison.test.ScrollTest',
            test_package='com.facebook.samples.comparison.test',
            test_runner='android.test.InstrumentationTestRunner'):
        self.method_name = method_name
        self.class_name = class_name
        self.test_package = test_package
        self.test_runner = test_runner

    def __call__(self):
        """ Executes test case and captures logcat output """
        adb('logcat -c')
        with tempfile.TemporaryFile() as logcat_file:
            logcat_reader = start_subprocess(
                'adb logcat',
                stdout=logcat_file)
            adb('shell am instrument -w -e class {}#{} {}/{}'.format(
                self.class_name,
                self.method_name,
                self.test_package,
                self.test_runner))
            logcat_reader.terminate()
            logcat_reader.wait()

            logcat_file.seek(0)
            self.logcat = logcat_file.readlines()


def get_float_from_logs(regex, logs):
    pattern = re.compile(regex)
    return [float(match.group(1)) for match in map(pattern.search, logs) if match]


def get_int_from_logs(regex, logs):
    pattern = re.compile(regex)
    return [int(match.group(1)) for match in map(pattern.search, logs) if match]


def get_stats(logs):
    pattern = re.compile("""]: loaded after (\d+) ms""")
    success_wait_times = [
        int(match.group(1)) for match in map(pattern.search, logs) if match]

    pattern = re.compile("""]: failed after (\d+) ms""")
    failure_wait_times = [
        int(match.group(1)) for match in map(pattern.search, logs) if match]

    pattern = re.compile("""]: cancelled after (\d+) ms""")
    cancellation_wait_times = [
        int(match.group(1)) for match in map(pattern.search, logs) if match]

    pattern = re.compile("""\s+(\d+.\d+) MB Java""")
    java_heap_sizes = [
        float(match.group(1)) for match in map(pattern.search, logs) if match]

    pattern = re.compile("""\s+(\d+.\d+) MB native""")
    native_heap_sizes = [
        float(match.group(1)) for match in map(pattern.search, logs) if match]

    pattern = re.compile("""Skipped (\d+) frames!  The application may be""")
    skipped_frames = [
        int(match.group(1)) for match in map(pattern.search, logs) if match]

    return Stats(
        success_wait_times,
        failure_wait_times,
        cancellation_wait_times,
        java_heap_sizes,
        native_heap_sizes,
        skipped_frames)


def print_stats(stats):
    successes = len(stats.success_wait_times)
    cancellations = len(stats.cancellation_wait_times)
    failures = len(stats.failure_wait_times)
    total_count = successes + cancellations + failures

    total_wait_time = (
        sum(stats.success_wait_times) +
        sum(stats.cancellation_wait_times) +
        sum(stats.failure_wait_times))

    avg_wait_time = float(total_wait_time) / total_count

    max_java_heap = max(stats.java_heap_sizes)
    max_native_heap = max(stats.native_heap_sizes)

    total_skipped_frames = sum(stats.skipped_frames)

    print("Average wait time    = {0:.1f}".format(avg_wait_time))
    print("Successful requests  = {}".format(successes))
    print("Failures             = {}".format(failures))
    print("Cancellations        = {}".format(cancellations))
    print("Max java heap        = {0:.1f}".format(max_java_heap))
    print("Max native heap      = {0:.1f}".format(max_native_heap))
    print("Total skipped frames = {}".format(total_skipped_frames))


def get_test_name(option_name, source_name):
    return TEST_PATTERN.format(
        ''.join(word.capitalize() for word in option_name.split('-')), source_name.capitalize())

def valid_scenario(scenario_name, source_name):
    return source_name != 'local' or (scenario_name != 'volley' and scenario_name != 'drawee-volley')


def list_producers():
    sdir = os.path.dirname(os.path.abspath(__file__))
    producer_path = '%s/imagepipeline/src/main/java/com/facebook/imagepipeline/producers/*Producer.java' % sdir
    files = glob.glob(producer_path)
    return [f.split('.')[0].split('/')[-1] for f in files]


def print_fresco_perf_line(margin, name, times):
    length = len(times)
    if length == 0:
        return
    print("%s: %d requests, avg %d" % (name.rjust(margin), length, float(sum(times)) / length))


def print_fresco_perf(logs):
    producers = list_producers()
    margin = max([len(p) for p in producers])
    requests = get_int_from_logs(""".*RequestLoggingListener.*onRequestSuccess.*elapsedTime:\s(\d+).*""", logs)
    print_fresco_perf_line(margin, 'Total', requests)
    for producer in producers:
        queue = get_int_from_logs(".*onProducerFinishWithSuccess.*producer:\s%s.*queueTime=(\d+).*" % producer, logs)
        print_fresco_perf_line(margin, '%s queue' % producer, queue)
        times = get_int_from_logs(".*onProducerFinishWithSuccess.*producer:\s%s.*elapsedTime:\s(\d+).*" % producer, logs)
        print_fresco_perf_line(margin, producer, times)


def main():
    args = parse_args()
    scenarios = []
    sources = []
    if args.scenarios:
        scenarios = args.scenarios
    else:
        scenarios = TESTS

    if args.sources:
        sources = args.sources
    else:
        sources = TEST_SOURCES

    install_apks(args.cpu)

    for scenario_name in scenarios:
        for source_name in sources:
            if valid_scenario(scenario_name, source_name):
                print()
                print('Testing {} {}'.format(scenario_name, source_name))
                print(get_test_name(scenario_name, source_name))
                test = ComparisonTest(get_test_name(scenario_name, source_name))
                test()
                stats = get_stats(test.logcat)
                print_stats(stats)
                if scenario_name[:6] == 'fresco':
                    print()
                    print_fresco_perf(test.logcat)

if __name__ == "__main__":
    main()
