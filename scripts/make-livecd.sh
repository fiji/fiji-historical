#!/bin/sh

# TODO: make use of user "fiji"
# TODO: generate /etc/{passwd,shadow,group,sudoers,hosts}
# TODO: name chroot-* differently (indicate this is for the live CD)

NAME=gutsy
CHROOT=chroot-$NAME-i386
CHROOT_HOME=/home/$USER
DCHROOT="dchroot -c $NAME"

test -d $CHROOT || {
	mkdir $CHROOT &&
	sudo apt-get install -q -y --force-yes dchroot debootstrap &&
	sudo debootstrap --arch i386 $NAME \
		./$CHROOT/ http://archive.ubuntu.com/ubuntu &&
	for i in passwd shadow group sudoers hosts
	do
		sudo cp /etc/$i ./$CHROOT/etc/
	done &&

	(grep "$(pwd)/$CHROOT" /etc/dchroot.conf 2> /dev/null ||
	 sudo sh -c "echo \"$NAME $(pwd)/$CHROOT\" >> /etc/dchroot.conf") &&

	sudo mkdir $CHROOT/$CHROOT_HOME &&
	sudo chown $USER.$USER $CHROOT/$CHROOT_HOME || {
		echo "Could not make chroot"
		exit 1
	}
}

APTSOURCE="deb http://ftp.de.debian.org/debian etch main"
APTSOURCEFILE=/etc/apt/sources.list
grep "$APTSOURCE" $CHROOT$APTSOURCEFILE > /dev/null 2> /dev/null || {
	sudo sh -c "echo $APTSOURCE >> $CHROOT$APTSOURCEFILE"
	$DCHROOT "sudo apt-get update" ||
	exit
}

APTSOURCE="deb http://de.archive.ubuntu.com/ubuntu/ gutsy main restricted"
APTSOURCEFILE=/etc/apt/sources.list
grep "$APTSOURCE" $CHROOT$APTSOURCEFILE > /dev/null 2> /dev/null || {
	sudo sh -c "echo $APTSOURCE >> $CHROOT$APTSOURCEFILE"
	$DCHROOT "sudo apt-get update" ||
	exit
}

test -f $CHROOT/usr/bin/bootcdwrite ||
$DCHROOT "sudo apt-get install -q -y --force-yes bootcd" ||
exit

test -f $CHROOT/vmlinuz ||
$DCHROOT "sudo apt-get install -q -y --force-yes linux-image" ||
exit

# TODO: install X11 and Java
# TODO: choose an appropriate window manager
# TODO: automatic login
# TODO: copy Fiji
# TODO: start Fiji upon startup

$DCHROOT "sudo bash bootcdwrite -s"

ISO=/var/spool/bootcd/cdimage.iso
$DCHROOT "sudo chmod a+w $ISO"

mv $CHROOT$ISO fiji-livecd.iso
