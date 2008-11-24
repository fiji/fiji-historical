#!/usr/bin/python

import jarray
import os
import re
import sys
from compat import execute

if len(sys.argv) < 3:
	print 'Usage: ', sys.argv[0], ' <zipfile> <files>'
	exit(1)

zipfile = sys.argv[1]
pattern = sys.argv[2]

verbose = False

print 'Removing', pattern, 'from', zipfile

if os.name == 'java':
	regex = re.compile(pattern.replace('?', '.').replace('*', '.*'))

	from java.io import FileInputStream, FileOutputStream
	from java.util.zip import ZipInputStream, ZipOutputStream, ZipEntry

	input = ZipInputStream(FileInputStream(zipfile))
	output = ZipOutputStream(FileOutputStream(zipfile + '.tmp'))

	while True:
		entry = input.getNextEntry()
		if entry == None:
			break
		if regex.match(entry.getName()):
			continue
		buffer = jarray.zeros(entry.getSize(), 'b')
		input.read(buffer)
		input.closeEntry()
		entry.setCompressedSize(-1)
		output.putNextEntry(entry)
		output.write(buffer)
		output.closeEntry()

	input.close()
	output.close()

	os.rename(zipfile + '.tmp', zipfile)
else:
	execute('zip -d ' + zipfile + ' ' + pattern)
