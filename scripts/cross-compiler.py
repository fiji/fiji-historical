#!/usr/bin/python

import os
import sys
from compat import execute

if len(sys.argv) < 2:
	print "Usage:", sys.argv[0], "<platform> <cxxflags>"
	sys.exit(1)

if sys.argv[1] != 'win64':
	print "Unsupported platform:", sys.argv[1]
	sys.exit(1)

cxx = 'root-x86_64-pc-linux/bin/x86_64-pc-mingw32-g++'
strip = 'root-x86_64-pc-linux/bin/x86_64-pc-mingw32-strip'
target = 'precompiled/fiji-win64.exe'

if not os.path.exists(cxx):
	print "You need to install the mingw64 cross compiler into", cxx
	sys.exit(1)

quoted_args = ' '.join(sys.argv[2:]).replace("'", '"').replace('"', '\"')
print(cxx + ' -o ' + target + ' ' + quoted_args + ' fiji.cxx')
print execute(cxx + ' -o ' + target + ' ' + quoted_args + ' fiji.cxx')
print execute(strip + ' ' + target)
