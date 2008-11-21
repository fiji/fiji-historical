#!/bin/sh

PLATFORMS="$@"
test -z "$PLATFORMS" && PLATFORMS="linux win32 linux64 win64 macosx"

NAME=dapper
CHROOT=../chroot-$NAME-i386
CHROOT_HOME=/home/$USER

JNA=$CHROOT_HOME/jna-native
CHROOT_JNA=$CHROOT$JNA

test -d $CHROOT_JNA || mkdir -p $CHROOT_JNA || exit

NATIVE="jnalib/native"
rsync -vac $NATIVE/* ../includes $CHROOT_JNA || exit

javah -d $CHROOT_JNA -classpath jna.jar \
	com.sun.jna.CallbackReference \
	com.sun.jna.Function \
	com.sun.jna.Memory \
	com.sun.jna.Native \
	com.sun.jna.NativeLibrary \
	com.sun.jna.Pointer ||
exit

MD5=$(cd $CHROOT_JNA && grep -A 1 JNIEXPORT com*.h | md5sum)
VERSION="$(grep ^VERSION= jnalib/native/Makefile)"
test -z "$VERSION" || eval $VERSION

for PLATFORM in $PLATFORMS
do
	NEED_CHROOT=false
	NEED_SSH=false
	unset CFLAGS
	case $PLATFORM in
	win32)
		TARGET_PLATFORM=i386-mingw32
		CC=i386-mingw32-gcc
		MAKE_RESOURCE="i386-mingw32-windres -i jnidispatch.rc -o tmp.o"
		RESOURCE_OBJECT=tmp.o
		LIBRARY=jnidispatch.dll
		NEED_CHROOT=true
		TARGET_DIRECTORY=win32-x86
	;;
	linux)
		TARGET_PLATFORM=i386-linux
		CC="gcc -W -Wall -Wno-unused -Wno-parentheses -fPIC \
			-fno-omit-frame-pointer -fno-strict-aliasing \
			-D_REENTRANT -DHAVE_PROTECTION"
		MAKE_RESOURCE=echo
		RESOURCE_OBJECT=
		LIBRARY=libjnidispatch.so
		NEED_CHROOT=true
		TARGET_DIRECTORY=linux-i386
	;;
	win64)
		TARGET_PLATFORM=x86_64-pc-mingw32
		ROOTWIN64=$(pwd)/../root-x86_64-pc-linux/bin
		export PATH=$PATH:$ROOTWIN64
		CC="$ROOTWIN64/x86_64-pc-mingw32-gcc"
		MAKE_RESOURCE="$ROOTWIN64/x86_64-pc-mingw32-windres \
			-i jnidispatch.rc -o tmp.o"
		RESOURCE_OBJECT=tmp.o
		LIBRARY=jnidispatch.dll
		TARGET_DIRECTORY=win32-amd64
	;;
	linux64)
		TARGET_PLATFORM=x86_64-linux
		CC="gcc -W -Wall -Wno-unused -Wno-parentheses -fPIC \
			-fno-omit-frame-pointer -fno-strict-aliasing \
			-D_REENTRANT -DHAVE_PROTECTION"
		MAKE_RESOURCE=echo
		RESOURCE_OBJECT=
		LIBRARY=libjnidispatch.so
		export CFLAGS="-fPIC -dPIC"
		TARGET_DIRECTORY=linux-amd64
	;;
	macosx)
		TARGET_PLATFORM=i386-apple-darwin9.5.0
		CC="gcc -fPIC"
		MAKE_RESOURCE=echo
		RESOURCE_OBJECT=
		LIBRARY=libjnidispatch.dylib
		RSYNC_RSH=ssh rsync -vac $CHROOT_JNA macosx10.5:
		NEED_SSH=macosx
		TARGET_DIRECTORY=darwin
	;;
	esac

	extra=
	DISPATCH_EXTENSION=c
	EXTRA_LDFLAGS=
	EXTRA_LIBS=
	case $NEED_CHROOT,$NEED_SSH in
	true,false)
		DCHROOT=dchroot
		SOURCE_PATH=$JNA
		REAL_SOURCE_PATH=$CHROOT_JNA
	;;
	false,macosx)
		DCHROOT="ssh macosx10.5"
		SOURCE_PATH="\$HOME/jna-native"
		REAL_SOURCE_PATH=macosx10.5:jna-native
		EXTRA_COMMANDS="(cd $SOURCE_PATH &&
			find . -type f -exec touch {} \\; &&
			cp dispatch.c dispatch.m) &&"
		EXTRA_LDFLAGS="-dynamiclib -flat_namespace -lSystem"
		EXTRA_LIBS="-framework JavaVM"
		DISPATCH_EXTENSION=m
	;;
	*)
		DCHROOT="sh -x -c"
		SOURCE_PATH=$(pwd)/$CHROOT_JNA
		REAL_SOURCE_PATH=$CHROOT_JNA
	;;
	esac

	BUILD_LIBFFI="(cd $SOURCE_PATH/libffi && \
		./configure --host=$TARGET_PLATFORM && \
		make clean && \
		make)"

	CC="$CC \
		-DVERSION='\"$VERSION\"' \
		-DCHECKSUM='\"$MD5\"' \
		-I$SOURCE_PATH/includes \
		-I$SOURCE_PATH/libffi/include"

	BUILD_LIBJNIDISPATCH="(cd $SOURCE_PATH &&
		$MAKE_RESOURCE && \
		$CC -c -o dispatch.o dispatch.$DISPATCH_EXTENSION && \
		$CC -c -o callback.o callback.c && \
		$CC $EXTRA_LDFLAGS -shared -o $LIBRARY dispatch.o callback.o \
			$RESOURCE_OBJECT libffi/.libs/libffi.a $EXTRA_LIBS)"

	$DCHROOT "$EXTRA_COMMANDS $BUILD_LIBFFI && $BUILD_LIBJNIDISPATCH" ||
	exit

	mkdir -p jnalib/dist/$TARGET_DIRECTORY &&
	if test $NEED_SSH = false
	then
		mv $REAL_SOURCE_PATH/$LIBRARY jnalib/dist/$TARGET_DIRECTORY/
	else
		scp $REAL_SOURCE_PATH/$LIBRARY jnalib/dist/$TARGET_DIRECTORY/
	fi || exit
done
