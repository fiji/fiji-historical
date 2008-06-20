#!/usr/bin/python

# TODO: all-platform portable-app
# TODO: all platforms

import os
import shutil
import sys

# Jython does not support removedirs and symlink.
# Warning: this implementation is not space-safe!
if 'removedirs' in dir(os):
	def removedirs(dir):
		os.removedirs(dir)
else:
	def removedirs(dir):
		os.system('rm -rf ' + dir)
if 'symlink' in dir(os):
	def symlink(src, dest):
		os.symlink(src, dest)
else:
	def symlink(src, dest):
		os.system('ln -s ' + src + " " + dest)
def make_macosx_app():
	print 'Making app'
	if os.path.isdir('Fiji.app'):
		removedirs('Fiji.app')
	macos='Fiji.app/Contents/MacOS/'
	resources='Fiji.app/Contents/Resources/'
	os.makedirs(macos)
	os.makedirs(resources)
	shutil.copy('Info.plist', 'Fiji.app/Contents/')
	shutil.copy('fiji', macos + 'fiji-macosx')
	shutil.copy('precompiled/fiji-tiger', macos)
	for d in ['java', 'plugins', 'macros', 'ij.jar', 'jars', 'misc']:
		symlink('../../' + d, macos + d)
	symlink('Contents/Resources', 'Fiji.app/images')
	symlink('../Resources', macos + 'images')
	shutil.copy('images/icon.png', 'Fiji.app/images/')
	os.system('git archive --prefix=$@/java/macosx-java3d/ ' \
		+ 'origin/java/macosx-java3d: | ' \
		+ 'tar xvf -')
	shutil.copy('ij.jar', 'Fiji.app/')
	for d in ['plugins', 'macros', 'jars', 'misc']:
		shutil.copytree(d, 'Fiji.app/' + d)
	shutil.copy('images/Fiji.icns', resources)

make_macosx_app()
