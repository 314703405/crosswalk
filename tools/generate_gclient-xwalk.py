#!/usr/bin/env python

# Copyright (c) 2013 Intel Corporation. All rights reserved.
# Use of this source code is governed by a BSD-style license that can be
# found in the LICENSE file.

"""
This script is responsible for generating .gclient-xwalk in the top-level
source directory by parsing .DEPS.xwalk (or any other file passed to the --deps
option).
"""

import optparse
import os
import pprint
import re
import shutil
import sys

from utils import TryAddDepotToolsToPythonPath

try:
  import patch
  from checkout import SvnCheckout
except ImportError:
  TryAddDepotToolsToPythonPath()

try:
  import patch
  from checkout import SvnCheckout

except ImportError:
  sys.stderr.write("Can't find gclient_utils, please add your depot_tools "\
                   "to PATH or PYTHONPATH\n")
  sys.exit(1)


class GClientFileGenerator(object):
  def __init__(self, options):
    self._options = options
    self._xwalk_dir = os.path.dirname(
        os.path.dirname(os.path.abspath(__file__)))
    if options.deps:
      self._deps_file = options.deps
    else:
      self._deps_file = os.path.join(self._xwalk_dir, 'DEPS.xwalk')
    self._deps = None
    self._vars = None
    self._chromium_version = None
    self._use_git = False
    if options.use_git or os.environ.get("XWALK_USE_GIT") is not None:
      self._use_git = True
    self._ParseDepsFile()

    if not 'src' in self._deps:
      raise RuntimeError("'src' not specified in deps file(%s)" % options.deps)
    self._src_dep = self._deps['src']
    # self should be at src/xwalk/tools/fetch_deps.py
    # so src is at self/../../../
    self._src_dir = os.path.dirname(self._xwalk_dir)
    self._root_dir = os.path.dirname(self._src_dir)
    self._new_gclient_file = os.path.join(self._root_dir,
                                          '.gclient-xwalk')

  def _ParseDepsFile(self):
    if not os.path.exists(self._deps_file):
      raise IOError('Deps file does not exist (%s).' % self._deps_file)
    exec_globals = {}

    execfile(self._deps_file, exec_globals)
    self._deps = exec_globals['deps_xwalk']
    self._vars = exec_globals['vars_xwalk']
    self._chromium_version = exec_globals['chromium_version']

  def _AddIgnorePathFromEnv(self):
    """Read paths from environ XWALK_SYNC_IGNORE.
       Set the path with None value to ignore it when syncing chromium.

       If environ not set, will ignore the ones upstream wiki recommended
       by default.
    """
    ignores_str = os.environ.get("XWALK_SYNC_IGNORE")
    if not ignores_str:
      ignores = ['build',
                 'build/scripts/command_wrapper/bin',
                 'build/scripts/gsd_generate_index',
                 'build/scripts/private/data/reliability',
                 'build/scripts/tools/deps2git',
                 'build/third_party/cbuildbot_chromite',
                 'build/third_party/gsutil',
                 'build/third_party/lighttpd',
                 'build/third_party/swarm_client',
                 'build/third_party/xvfb',
                 'build/xvfb',
                 'commit-queue',
                 'depot_tools',
                 'src/webkit/data/layout_tests/LayoutTests',
                 'src/third_party/WebKit/LayoutTests',
                 'src/content/test/data/layout_tests/LayoutTests',
                 'src/chrome/tools/test/reference_build/chrome_win',
                 'src/chrome_frame/tools/test/reference_build/chrome_win',
                 'src/chrome/tools/test/reference_build/chrome_linux',
                 'src/chrome/tools/test/reference_build/chrome_mac',
                 'src/third_party/chromite',
                 'src/third_party/hunspell_dictionaries',
                 'src/third_party/pyelftools']
    else:
      ignores_str = ignores_str.replace(':', ';')
      ignores = ignores_str.split(';')
    for ignore in ignores:
      self._deps[ignore] = None

  def _GenerateGitDeps(self):
    """Generate .DEPS.git instead of DEPS
    """
    # Update subversion anyway
    svn_url = 'http://src.chromium.org/svn/releases/' + self._chromium_version
    co = SvnCheckout(self._root_dir, self._chromium_version,
                     None, None, svn_url)
    co.prepare(None)

    # Get branch name
    rel_dir = os.path.join(self._root_dir, self._chromium_version)
    f = open(os.path.join(rel_dir, 'DEPS'), 'r')
    for line in f:
      m = re.search('^.*/branches/chromium/(\d+)@.*$', line)
      if m and m.group(1):
        branch_name = m.group(1)
        break

    f.close()
    if branch_name is None:
      return

    # Get .DEPS.git
    deps_git_dir = os.path.join(rel_dir, 'deps_git')
    svn_url = 'http://src.chromium.org/svn/branches/' + branch_name + '/src/'
    co = SvnCheckout(rel_dir, 'deps_git', None, None, svn_url)
    co.prepare(None, ['.DEPS.git'])
    # Apply deps changes after branching point.
    # .DEPS.git stopped updated after branching, while DEPS does, for eg:
    # http://src.chromium.org/viewvc/chrome/branches/1985/src/DEPS?view=log
    # http://src.chromium.org/viewvc/chrome/branches/1985/src/.DEPS.git?view=log
    #
    # Rebase owner is responsible to update the DEPS_git.diff
    f = open(os.path.join(self._root_dir,
                          'src', 'xwalk', 'tools', 'DEPS_git.diff'))
    diff = f.read()
    f.close()

    p = patch.FilePatchDiff('.DEPS.git', diff, [])
    co.apply_patch([p])

    shutil.copyfile(os.path.join(deps_git_dir, '.DEPS.git'),
                    os.path.join(rel_dir, '.DEPS.git'))

  def Generate(self):
    self._AddIgnorePathFromEnv()
    # FIXME(halton): with crbug.com/380991, src can not be specified in
    # .gclient file, instead src will be fetched via
    # tools/fetch_chromium_code.py in hooks phase.
    if self._use_git:
      self._deps['src'] = None

    solution = {
        'name': self._chromium_version,
        'url': 'http://src.chromium.org/svn/releases/%s' %
               self._chromium_version,
        'custom_deps': self._deps,
    }

    # Most of dependencies in <release_dir>/DEPS are pointing to subversion.
    # Unfortunately, svn command behind firewall randomly fails with error
    # "501 Not Implemented". Instead, git repos are more stable to fetch.
    if self._use_git:
      solution['deps_file'] = '.DEPS.git'
      self._GenerateGitDeps()

    if self._vars:
      solution['custom_vars'] = self._vars

    solutions = [solution]
    gclient_file = open(self._new_gclient_file, 'w')
    print "Place %s with solutions:\n%s" % (self._new_gclient_file, solutions)
    gclient_file.write('solutions = %s\n' % pprint.pformat(solutions))
    # Check whether the target OS is Android.
    if os.environ.get('XWALK_OS_ANDROID'):
      target_os = ['android']
      gclient_file.write('target_os = %s\n' % target_os)

    if self._options.cache_dir:
      gclient_file.write('cache_dir = %s\n' %
                         pprint.pformat(self._options.cache_dir))


def main():
  option_parser = optparse.OptionParser()

  option_parser.add_option('--deps', default=None,
                           help='The deps file contains the dependencies path '
                                'and url')
  option_parser.add_option('--cache-dir',
                           help='Set "cache_dir" in the .gclient file to this '
                                'directory, so that all git repositories are '
                                'cached there and shared across multiple '
                                'clones.')
  option_parser.add_option('--use-git', default=False,
                           help='Use .DEPS.git instead of subversion DEPS. '
                                'It is useful for those have subversion '
                                'problem behind proxy.')

  # pylint: disable=W0612
  options, args = option_parser.parse_args()

  sys.exit(GClientFileGenerator(options).Generate())


if __name__ == '__main__':
  main()
