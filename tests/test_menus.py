# Test whether any menu items contain pointers to non-existent classes
# which likely indicate a missconfiguration of a plugins.config file in a .jar plugin.

import ij
from ij import ImageJ, Menus
import java
from java.lang import Class, ClassNotFoundException, System

# Launch ImageJ
ImageJ()

ok = 1

# Inspect each menu command
for it in ij.Menus.getCommands().entrySet().iterator():
  try:
    cl = it.value
    k = cl.find('(')
    if -1 != k:
      cl = cl[:k]
    cl = Class.forName(cl)
  except ClassNotFoundException:
    print 'ERROR: Class not found for menu command: ', it.key, it.value
    ok = 0

if ok:
    print "ok - Menu commands all correct."

System.exit(0)
