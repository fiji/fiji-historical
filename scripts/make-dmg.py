#!/usr/bin/python

import os
import re

dmg='fiji-macosx.dmg'
app='Fiji.app'

if 'symlink' in dir(os):
	def symlink(src, dest):
		os.symlink(src, dest)
else:
	def symlink(src, dest):
		os.system('ln -s ' + src + ' ' + dest)
try:
	from java.lang import Runtime
	from java.io import BufferedReader, InputStreamReader

	def execute(cmd):
		runtime = Runtime.getRuntime()
		#p = runtime.exec(cmd)
		p.outputStream.close()
		result=""
		reader=BufferedReader(InputStreamReader(p.inputStream))
		while True:
			line=reader.readLine()
			if line == None:
				break
			result+=line + "\n"
		return result
except:
	def execute(cmd):
		proc = os.popen(cmd)
		return "\n".join(proc.readlines())
def hdiutil(cmd):
	print cmd
	os.system('hdiutil ' + cmd)
def get_disk_id(dmg):
	match=re.match('.*/dev/([^ ]*)[^/]*Apple_HFS.*', execute('hdid ' + dmg),
		re.MULTILINE | re.DOTALL)
	if match != None:
		return match.group(1)
	return None
def get_folder(dmg):
	match=re.match('.*Apple_HFS\s*([^\n]*).*', execute('hdid ' + dmg),
		re.MULTILINE | re.DOTALL)
	if match != None:
		return match.group(1)
	return None
def eject(dmg):
	disk_id=get_disk_id(dmg)
	print "disk_id: ", disk_id
	hdiutil('eject ' + disk_id)

# create temporary disk image and format, ejecting when done
hdiutil('create ' + dmg + ' -srcfolder ' + app \
	+ ' -fs HFS+ -format UDRW -volname Fiji -ov')
folder=get_folder(dmg)
print "folder: ", folder
for d in ['plugins', 'macros']:
	symlink('Fiji.app/' + d, folder + '/' + d)
eject(dmg)

os.rename(dmg, dmg + '.tmp')
hdiutil('convert ' + dmg + '.tmp -format UDZO -o ' + dmg)
eject(dmg)
