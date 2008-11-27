#!/bin/sh

# TODO: nvidia stuff

LIVECD=livecd
SVG=images/fiji-logo-1.0.svg
XPM=images/fiji.xpm.gz
LSS16=images/fiji16.lss
PNG16=images/fiji16.png


# some functions

# die <message>

die () {
	echo "$@" >&2
	exit 1
}

# upToDate <target> <source>

upToDate () {
	test -f "$1" && test ! "$2" -nt "$1"
}



# go to the Fiji root

cd "$(dirname "$0")"/..
FIJIROOT="$(pwd)"/

sudo apt-get install live-helper ||
die "live-helper package is not available"

sudo apt-get install libbogl-dev ||
die "libbogl-dev package is not available"

# TODO: this depends on i386
test -x Fiji.app/fiji-linux ||
sh Fake.sh app-linux ||
die "Could not make Fiji for Linux/i386"

# make the logos

WIDTH=640; HEIGHT=400
upToDate $LSS16 $SVG ||
./fiji -eval 'run("SVG...", "choose='$SVG' width='$HEIGHT' height='$HEIGHT'");
	run("Canvas Size...", "width='$WIDTH' height='$HEIGHT' position=Center zero");
	run("8-bit Color", "number=16");
	run("LSS16 ...", "save='$LSS16'");' -batch ||
die "Could not make $LSS16"


WIDTH=640; HEIGHT=480
upToDate $PNG16 $SVG ||
./fiji -eval 'run("SVG...", "choose='$SVG' width='$HEIGHT' height='$HEIGHT'");
	run("Canvas Size...", "width='$WIDTH' height='$HEIGHT' position=Center zero");
	run("8-bit Color", "number=16");
	saveAs("PNG ...", "'$PNG16'");' -batch ||
die "Could not make $PNG16"

WIDTH=640; HEIGHT=320
upToDate $XPM $SVG || {
XPM2=$(dirname $XPM)/$(basename $XPM .gz)
./fiji -eval 'run("SVG...", "choose='$SVG' width='$HEIGHT' height='$HEIGHT'");
	run("Canvas Size...", "width='$WIDTH' height='$HEIGHT' position=Center zero");
	run("8-bit Color", "number=92");
	run("XPM ...", "save='$XPM2'");' -batch &&
gzip -9 $XPM2
} ||
die "Could not make $XPM"

# cp  /usr/share/live-helper/examples/hooks/nvidia-legacy.sh \
#	config/chroot_local-hooks/ &&

mkdir -p $LIVECD &&
(cd $LIVECD &&
 for i in dev/pts proc sys
 do
	sudo umount chroot/$i || true
 done &&
 sudo rm -rf binary* cache/stages_bootstrap/ chroot/ .stage/ .lock \
	config/chroot_local* &&
 lh_config -p minimal \
	-a i386 \
	-d intrepid \
	--mirror-bootstrap "http://us.archive.ubuntu.com/ubuntu" \
	--mirror-binary "http://us.archive.ubuntu.com/ubuntu" \
	--sections="main restricted universe multiverse" \
	--mirror-binary-security "http://security.ubuntu.com/ubuntu" \
	--initramfs casper \
	-k generic \
	--linux-packages="linux-image" \
	--apt-secure disabled \
	--bootstrap cdebootstrap \
	--mirror-chroot-security "http://security.ubuntu.com/ubuntu" \
	--grub-splash "$FIJIROOT"$XPM \
	--bootappend-live "quiet splash vga=785" \
	--iso-application "Fiji Live" \
	--iso-publisher "Fiji project; http://pacific.mpi-cbg.de" \
	--iso-volume "Fiji Live $(date +%Y%m%d-%H:%M)" \
	--syslinux-splash "$FIJIROOT"$LSS16 \
	--syslinux-timeout 5 \
	--username fiji \
	--packages "kdebase-workspace-bin xinit xserver-xorg usplash" &&
 perl -pi.bak -e 's/LIVE_ENTRY=.*/LIVE_ENTRY="Start Fiji Live"/' \
	config/binary &&
 INCLUDES=config/chroot_local-includes &&
 USPLASH=/usr/local/lib/usplash &&
 mkdir -p $INCLUDES$USPLASH &&
 pngtobogl "$FIJIROOT"$PNG16 > usplash-fiji.c &&
 gcc -I/usr/include/bogl -Os -g -fPIC -c usplash-fiji.c -o usplash-fiji.o &&
 gcc -shared -Wl,-soname,usplash-fiji.so usplash-fiji.o -o usplash-fiji.so &&
 sudo mv usplash-fiji.so $INCLUDES$USPLASH/ &&
 cat > config/chroot_local-hooks/splash << EOF &&
#!/bin/sh

sudo ln -sf $USPLASH/usplash-fiji.so /usr/lib/usplash/usplash-artwork.so
EOF
 cat > config/chroot_local-hooks/names << EOF &&
#!/bin/sh

sudo perl -pi.bak -e 's/ubuntu/fiji/g' /etc/casper.conf &&
sudo rm /etc/casper.conf.bak
EOF
 cat > config/chroot_local-hooks/profile << EOF &&
#!/bin/sh

sudo sh -c 'cat >> /etc/skel/.profile << EOF
test 6 = \\\$(stat -c %T -L /proc/self/fd/0) && {
	mkdir -p \\\$HOME/.kde/Autostart &&
	ln -s /usr/share/applications/Fiji.desktop \\\$HOME/.kde/Autostart &&
	startx
}
'
EOF
 FIJITARGET=/usr/local/fiji &&
 mkdir -p $INCLUDES$FIJITARGET &&
 (cd "$FIJIROOT"Fiji.app && tar cvf - .) |
	(cd $INCLUDES$FIJITARGET && sudo tar xvf -) &&
 mkdir -p $INCLUDES/usr/bin &&
 sudo ln -s ../local/fiji/fiji-linux $INCLUDES/usr/bin/fiji &&
 mkdir -p $INCLUDES/usr/share/applications &&
 cat > $INCLUDES/usr/share/applications/Fiji.desktop << EOF &&
[Desktop Entry]
Type=Application
Encoding=UTF-8
Name=Fiji
GenericName=
Comment=
Icon=$FIJITARGET/images/fiji-icon-32x32.xpm
Exec=/usr/bin/fiji
Terminal=false
Categories=Graphics
EOF
 sudo lh_build &&
 mv binary.iso "$FIJIROOT"fiji-live.iso) ||
die "Building LiveCD failed"

