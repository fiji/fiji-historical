#!/bin/sh

PLATFORMS="$@"
test -z "$PLATFORMS" && PLATFORMS="linux win32"

NAME=dapper
CHROOT=../chroot-$NAME-i386
CHROOT_HOME=/home/$USER

JNA=$CHROOT_HOME/jna-native
CHROOT_JNA=$CHROOT$JNA

test -d $CHROOT_JNA || mkdir -p $CHROOT_JNA || exit

NATIVE="jnalib/native"
cp -R $NATIVE/* ../includes $CHROOT_JNA || exit

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
	case $PLATFORM in
	win32)
		TARGET_PLATFORM=i386-mingw32
		CC=i386-mingw32-gcc
		MAKE_RESOURCE="i386-mingw32-windres -i jnidispatch.rc -o tmp.o"
		RESOURCE_OBJECT=tmp.o
		LIBRARY=jnidispatch.dll
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
		TARGET_DIRECTORY=linux-i386
	;;
	esac

	case $PLATFORM in
	linux|win32)
		DCHROOT=dchroot
		#JNA=$CHROOT_HOME/jna-native
		#DCHROOT_JNA=$CHROOT$JNA
	;;
	*)
		DCHROOT="sh -c"
	;;
	esac

	BUILD_LIBFFI="(cd $JNA/libffi && \
		./configure --host=$TARGET_PLATFORM && \
		make clean && \
		make)"

	CC="$CC \
		-DVERSION='\"$VERSION\"' \
		-DCHECKSUM='\"$MD5\"' \
		-I$JNA/includes \
		-I$JNA/libffi/include"

	BUILD_LIBJNIDISPATCH="(cd $JNA &&
		$MAKE_RESOURCE && \
		$CC -c -o dispatch.o dispatch.c && \
		$CC -c -o callback.o callback.c && \
		$CC -shared -o $LIBRARY dispatch.o callback.o \
			$RESOURCE_OBJECT libffi/.libs/libffi.a)"

	$DCHROOT "$BUILD_LIBFFI && $BUILD_LIBJNIDISPATCH" || exit

	mkdir -p jnalib/dist/$TARGET_DIRECTORY &&
	mv $CHROOT_JNA/$LIBRARY jnalib/dist/$TARGET_DIRECTORY/ || exit
done
