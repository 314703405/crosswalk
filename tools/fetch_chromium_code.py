#!/usr/bin/env python

# Copyright (c) 2014 Intel Corporation. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

"""
This script is responsible for fetching chromium code to src.
"""

import os
import sys

from utils import TryAddDepotToolsToPythonPath

try:
  from checkout import GitCheckout
except ImportError:
  TryAddDepotToolsToPythonPath()

try:
  from checkout import GitCheckout
except ImportError:
  sys.stderr.write("Can't find gclient_utils, please add your depot_tools "\
                   "to PATH or PYTHONPATH\n")
  sys.exit(1)

ROOT_DIR = os.path.abspath(
    os.path.join(os.path.dirname(__file__), '..', '..', '..'))


def GetChromiumPoint():
  deps_file = os.path.join(ROOT_DIR, 'src', 'xwalk', 'DEPS.xwalk')
  if not os.path.exists(deps_file):
    raise IOError('Deps file does not exist (%s).' % deps_file)

  exec_globals = {}
  execfile(deps_file, exec_globals)
  return exec_globals['chromium_crosswalk_point']


def main():
  git_url = 'https://github.com/crosswalk-project/chromium-crosswalk.git'
  co = GitCheckout(ROOT_DIR, 'src', 'master', git_url, None)
  co.prepare(GetChromiumPoint())


if __name__ == '__main__':
  if os.environ.get("XWALK_USE_GIT") is None:
    sys.exit(0)

  main()
