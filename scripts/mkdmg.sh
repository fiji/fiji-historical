#!/bin/sh
#
# Creates a disk image (dmg) on Mac OS X from the command line.
# usage:
#    mkdmg <volname> <srcdir>
#
# Where <volname> is the name to use for the mounted image, number of
# the volume and <srcdir> is where the contents to put on the dmg are.
#
# The result will be a file called <volname>.dmg

if [ $# != 2 ]; then
 echo "usage: mkdmg.sh volname srcdir"
 exit 0
fi

VOL="$1"
FILES="$2"

case "$VOL" in
*.dmg)
	DMG="$VOL"
	VOL="$(basename "$VOL" .dmg)"
;;
*)
	DMG="$VOL.dmg"
;;
esac

# create temporary disk image and format, ejecting when done
hdiutil create "$DMG" -srcfolder "$FILES" -fs HFS+ -volname "$VOL" -ov
DISK=`hdid "$DMG" | sed -ne '/Apple_HFS/s|^/dev/\([^ ]*\).*$|\1|p'`
hdiutil eject $DISK

