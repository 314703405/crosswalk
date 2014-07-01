#!/usr/bin/env python

# Copyright (c) 2014 Intel Corporation. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

"""
This script is responsible for analyze the tombstone or ANR files
from Android deivces.
"""

import datetime
import optparse
import os
import re
import subprocess
import sys


def _info(msg):
    print "[INFO] " + msg + "."


def _error(msg, abort=True, error_code=1):
    print "[ERROR] " + msg + "!"
    if abort:
        quit(error_code)


# show_command: Print command if Ture. Default to True.
# show_duration: Report duration to execute command if True. Default to False.
# show_progress: print stdout and stderr to console if True. Default to False.
# return_output: Put stdout and stderr in result if True. Default to False.
# dryrun: Do not actually run command if True. Default to False.
# abort: Quit after execution failed if True. Default to False.
# log_file: Print stderr to log file if existed. Default to ''.
# interactive: Need user's input if true. Default to False.
def execute(command, show_command=True, show_duration=False,
            show_progress=False, return_output=False, dryrun=False,
            abort=False, log_file='', interactive=False):
    if show_command:
      print '[COMMAND] ' + command

    if dryrun:
        return [0, '']

    start_time = datetime.datetime.now().replace(microsecond=0)

    if interactive:
        ret = os.system(command)
        result = [ret / 256, '']
    else:
        process = subprocess.Popen(command, shell=True,
                                   stdout=subprocess.PIPE,
                                   stderr=subprocess.PIPE)
        while show_progress:
            nextline = process.stdout.readline()
            if nextline == '' and process.poll() is not None:
                break
            sys.stdout.write(nextline)
            sys.stdout.flush()

        (out, err) = process.communicate()
        ret = process.returncode

        if return_output:
            result = [ret, out + err]
        else:
            result = [ret, '']

    if log_file:
        os.system('echo ' + err + ' >>' + log_file)

    end_time = datetime.datetime.now().replace(microsecond=0)
    time_diff = end_time - start_time

    if show_duration:
        _info(str(time_diff) +
              ' was spent to execute following command: ' +
              command)

    if abort and result[0]:
        _error('Failed to execute', error_code=result[0])

    return result


def analyze_issue(build_type='Debug', type='tombstone'):
  self_dir = os.path.dirname(os.path.abspath(__file__))
  root_dir = os.path.join(self_dir, '..', '..', '..')
  lib_dir = os.path.join(root_dir, 'out', build_type, 'lib')
  addr2line_bin = os.path.join(root_dir,
                               'third_party', 'android_tools', 'ndk',
                               'toolchains', 'x86-4.6', 'prebuilt',
                               'linux-x86_64', 'bin',
                               'i686-linux-android-addr2line')

  if not os.path.exists(lib_dir):
    _error('Please build xwalk_builder target first', 1)

  if not os.path.exists(addr2line_bin):
    _error('%s is not exisiting, please sync your code.' % addr2line_bin, 1)

  if type == 'tombstone':
    result = execute('adb shell \ls /data/tombstones', return_output=True)
    files = result[1].split('\n')
    file_name = files[-2].strip()
    _info('Start to analyze ' + file_name)
    execute('adb pull /data/tombstones/' + file_name + ' /tmp/')
    result = execute('cat /tmp/' + file_name, return_output=True)
    lines = result[1].split('\n')
  elif type == 'anr':
    execute('adb pull /data/anr/traces.txt /tmp/')
    result = execute('cat /tmp/traces.txt', return_output=True)
    lines = result[1].split('\n')

  for line in lines:
    pattern = re.compile('pc (.*)  .*lib(.*)\.so')
    match = pattern.search(line)
    if match is None:
      continue

    #print line
    name = match.group(2)
    path = os.path.join(lib_dir, 'lib%s.so' % name)
    if not os.path.exists(path):
      continue
    cmd = addr2line_bin + ' -C -e %s -f %s' % (path, match.group(1))
    result = execute(cmd, return_output=True, show_command=False)
    print result[1]


def main():
  parser = optparse.OptionParser()
  parser.add_option('--release', dest='release',
                    help='Use libraries under Release instead of Debug.',
                    default=False,
                    action='store_true')
  parser.add_option('--type', dest='type',
                    help='Analyze ANR instead of tombstone.',
                    default='tombstone',
                    choices=['tombstone', 'anr'])

  options, _ = parser.parse_args()
  if options.release:
    analyze_issue('Release', options.type)
  else:
    analyze_issue('Debug', options.type)


if __name__ == '__main__':
  main()
