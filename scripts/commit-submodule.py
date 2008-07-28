#!/usr/bin/python

import os
import sys

from compat import execute

# TODO: use JGit

if not len(sys.argv) in [2, 3]:
	print 'Usage:', sys.argv[0], '<submodule> [<target>]'
	sys.exit(1)

submodule = sys.argv[1]
if not submodule.endswith('/'):
	submodule += '/'
if not os.path.isdir(submodule):
	print 'Huh?', submodule, 'does not exist...'
	sys.exit(1)

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

faked_submodules = dict()
last_submodule_line = -1
has_rule = dict()
last_rule_line = -1
precompileds = dict()
last_precompiled_line = -1
for i in range(0, len(fakefile)):
	if fakefile[i].startswith('SUBMODULE_TARGETS='):
		while i < len(fakefile) and fakefile[i] != "\n":
			if fakefile[i].endswith(" \\\n"):
				last_submodule_line = i
				faked_submodules[fakefile[i]] = i
			i += 1
	elif fakefile[i].startswith('ij.jar <- '):
		while i < len(fakefile) and fakefile[i] != "\n":
			space = fakefile[i].find(' <- ')
			if space > 0:
				last_rule_line = i
				last_space = fakefile[i].rfind(' ')
				has_rule[fakefile[i][last_space + 1:-1]] = \
					fakefile[i][0:space]
			i += 1
	elif fakefile[i].startswith('precompile-submodules[] <-'):
		while i < len(fakefile) and fakefile[i] != "\n":
			if fakefile[i].startswith("\t") and \
					fakefile[i].endswith(" \\\n"):
				last_precompiled_line = i
				precompileds[fakefile[i]] = i
			i += 1

if len(sys.argv) == 2:
	if submodule in has_rule.keys():
		target = has_rule[submodule]
	else:
		print 'Need a target, as the submodule', submodule, \
			'was not added yet'
		sys.exit(1)
elif len(sys.argv) == 3:
	if not submodule in has_rule.keys() or \
			sys.argv[2] == has_rule[submodule]:
		target = sys.argv[2]
	else:
		print 'Submodule', submodule, 'already has target', \
			has_rule[submodule], '(you gave', sys.argv[2] + ')'
		sys.exit(1)

# push submodule

print 'Making sure that the submodule is pushed:', \
	execute('git --git-dir=' + submodule + '.git push origin HEAD')

# add to .gitignore if not yet there

ignore_line = '/' + target + "\n"
if not ignore_line in ignored:
	f = open('.gitignore', 'a')
	f.write(ignore_line)
	f.close()
	ignored[target] = -1
	print execute('git add .gitignore')

# add to Fakefile if not yet there

write_fakefile = False
slash = target.rfind('/')
precompiled_target = 'precompiled/' + target[slash + 1:]
precompile_line = "\t" + precompiled_target + " \\\n"
if not precompile_line in precompileds.keys():
	fakefile.insert(last_precompiled_line + 1, precompile_line)
	write_fakefile = True
rule_line = target + ' <- ' + submodule + "\n"
if not submodule in has_rule.keys():
	fakefile.insert(last_rule_line + 1, rule_line)
	write_fakefile = True
submodule_line = "\t" + target + " \\\n"
if not submodule_line in faked_submodules.keys():
	fakefile.insert(last_submodule_line + 1, submodule_line)
	write_fakefile = True
if write_fakefile:
	f = open ('Fakefile', 'w')
	f.write(''.join(fakefile))
	f.close()
	execute('git add Fakefile')

# precompile

print execute('./fiji --fake ' + precompiled_target)

# git add submodule & precompiled

execute('git add ' + precompiled_target + ' ' + submodule[:-1])

# commit it

action = 'Add'
if not target in has_rule.keys():
	action = 'Update'
f = open('.msg', 'w')
f.write(action + ' the submodule "' + submodule[:-1] + '"')
f.close()
print execute('git commit -s -F .msg')
os.remove('.msg')
