#!/usr/bin/python

import os
import sys

from compat import execute

# TODO: add .jar plugins
# TODO: allow other cwds than fiji/
# TODO: use JGit

if len(sys.argv) < 2:
	print 'Usage:', sys.argv[0], 'src-plugins/<path>...'
	sys.exit(1)

list = list()
for file in sys.argv[1:]:
	if not file.startswith('src-plugins/'):
		print 'Will not add plugin outside src-plugins:', file
		continue
	if file.find('_') < 0:
		print 'This is not a plugin:', file
		continue
	if not file.endswith('.java'):
		print 'Will not add non-Java file:', file
		continue
	list.append(file[12:])

# read .gitignore

ignored = dict()
f = open('.gitignore', 'r')
line_number = 0
for line in f.readlines():
	ignored[line] = line_number
	line_number += 1
f.close()

# read Fakefile

f = open('Fakefile', 'r')
fakefile = f.readlines()
f.close()
faked_plugins = dict()
last_plugin_line = -1
for i in range(0, len(fakefile)):
	if fakefile[i].startswith('PLUGIN_TARGETS='):
		while i < len(fakefile) and fakefile[i] != "\n":
			if fakefile[i].endswith(".class \\\n"):
				last_plugin_line = i
				faked_plugins[fakefile[i]] = i
			i += 1
		break

# add the plugin to .gitignore, Fakefile, and the file itself

def add_plugin(plugin):
	class_file = 'plugins/' + plugin[0:len(plugin) - 5] + '.class'

	ignore_line = '/' + class_file + "\n"
	if not ignore_line in ignored:
		f = open('.gitignore', 'a')
		f.write(ignore_line)
		f.close()
		ignored[class_file] = -1
		execute('git add .gitignore')

	plugin_line = "\t" + class_file + " \\\n"
	global last_plugin_line, faked_plugins
	if not plugin_line in faked_plugins:
		last_plugin_line += 1
		fakefile.insert(last_plugin_line, plugin_line)
		f = open ('Fakefile', 'w')
		f.write(''.join(fakefile))
		f.close()
		execute('git add Fakefile')

	file = 'src-plugins/' + plugin
	if execute('git ls-files ' + file) == '':
		action = 'Added'
	else:
		action = 'Modified'
	execute('git add ' + file)
	f = open('.msg', 'w')
	name = plugin[0:len(plugin) - 5].replace('/', '>').replace('_', ' ')
	f.write(action + ' the plugin "' + name + '"')
	f.close() 
	execute('git commit -s -F .msg')
	os.remove('.msg')

for plugin in list:
	print 'Adding', plugin
	add_plugin(plugin)
