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

if [ $# -lt 2 ]; then
 echo "usage: mkdmg.sh volname srcdir [<symlinks>]"
 exit 0
fi

VOL="$1"; shift
FILES="$1"; shift

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
hdiutil create "$DMG" -srcfolder "$FILES" -fs HFS+ -format UDRW \
	-volname "$VOL" -ov
DISK=`hdid "$DMG" | sed -ne '/Apple_HFS/s|^/dev/\([^ ]*\).*$|\1|p'`
FOLDER=`hdid "$DMG" | sed -ne 's|^/dev/.*Apple_HFS[ 	]*\([^ ]*\)$|\1|p'`
(cd "$FOLDER" &&
 for p in "$@"
 do
	ln -s "$FILES"/"$p" .
 done)
hdiutil eject $DISK

mv "$DMG" "$DMG.tmp" &&
hdiutil convert "$DMG.tmp" -format UDZO -o "$DMG"
DISK=`hdid "$DMG" | sed -ne '/Apple_HFS/s|^/dev/\([^ ]*\).*$|\1|p'`
hdiutil eject $DISK

