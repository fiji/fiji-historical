#!/bin/sh

NAME=dapper
CHROOT=chroot-$NAME-i386
CHROOT_HOME=/home/$USER

test -d $CHROOT || {
	mkdir $CHROOT &&
	sudo apt-get install dchroot debootstrap &&
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

test -f $CHROOT/usr/bin/wget ||
dchroot sudo apt-get install gcc libcurl3-openssl-dev make \
	libexpat-dev perl-modules tk8.4 g++ pax patch \
	autoconf automake libtool bison flex unzip make wget || {
	echo "Could not install packages"
	exit 1
}

(cd $CHROOT/$CHROOT_HOME &&
 if test -d IMCROSS
 then
	cd IMCROSS
	git rev-parse --verify refs/heads/fiji 2>/dev/null >/dev/null ||
	git checkout -b fiji origin/fiji
	git pull
 else
	git clone git://pacific.mpi-cbg.de/IMCROSS/.git
	cd IMCROSS
	git checkout -b fiji origin/fiji
 fi)

dchroot "cd IMCROSS && sudo make fiji"
