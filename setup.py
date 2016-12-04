#!/usr/bin/env python
# ---------------------------------------------------------------------------

from __future__ import print_function

import sys
import os
sys.path += [os.getcwd()]

from setuptools import setup, find_packages
import re

# Pull version from __init__.py

version_re = re.compile(r'''^__version__\s*=\s*['"](.*)['"].*$''')
with open('peoplegen/__init__.py') as f:
    for line in f.readlines():
        m = version_re.match(line)
        if m:
            version = m.group(1)
            break
    else:
        sys.stderr.write("Can't find version in peoplegen/__init__.py.\n")
        sys.exit(1)


DESCRIPTION = "Generate random people data"
NAME = 'peoplegen'

DOWNLOAD_URL = 'https://github.com/bmc/peoplegen/archive/release-{0}'.format(version)


# Now the setup stuff.

print("{0}, version {1}".format(NAME, version))

gen_README = False
if not os.path.exists('README.rst'):
  gen_README = True
else:
  md_time = os.path.getmtime('README.md')
  rst_time = os.path.getmtime('README.rst')
  if rst_time < md_time:
    gen_README = True

print("Making README.rst. This will fail if you don't have pandoc installed.")
rc = os.system('pandoc -o README.rst README.md')
if rc != 0:
    print("*** WARNING: Unable to generate README.rst.")

setup(name                 = NAME,
      download_url         = DOWNLOAD_URL,
      version              = version,
      description          = DESCRIPTION,
      long_description     = DESCRIPTION,
      packages             = find_packages(),
      package_data         = {'peoplegen': ['data/*.txt']},
      include_package_data = True,
      url                  = 'http://github.com/bmc/peoplegen',
      license              = 'BSD New',
      author               = 'Brian M. Clapper',
      author_email         = 'bmc@clapper.org',
      entry_points         = {'console_scripts' : 'peoplegen=peoplegen:main'},
      install_requires     = ['docopt>=0.6.2',
                              'numpy>=1.10.1',
                              'future >= 0.15.2'],
      classifiers = [
          'Intended Audience :: Developers',
          'License :: OSI Approved :: BSD License',
          'Programming Language :: Python :: 2.7',
          'Programming Language :: Python :: 3'
      ]
)
