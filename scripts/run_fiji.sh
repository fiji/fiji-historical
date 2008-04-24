#! /bin/bash
# Script for execution of fiji from MacOSX-intel or MacOSX-ppc
#

echo "-----------" > /dev/console;
echo "Launching Fiji" > /dev/console;
echo "-----------" > /dev/console;

DIRNAME="$(dirname "$0")";
UNAME_P="$(uname -p 2>/dev/null || echo not)";


case "$UNAME_P" in
    powerpc)
	fiji-macosx;
	;;
    i386)
        fiji-macosx-intel;
	;;
    *)
	echo "Unsupported platform: " 
esac

exit