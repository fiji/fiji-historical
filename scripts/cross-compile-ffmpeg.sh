#!/bin/sh

PRECOMPILED="$(dirname "$0")/../precompiled"

LIST="util codec format"

CONFIGURE="./configure --enable-shared --disable-static 
	--disable-ffmpeg --disable-ffplay --disable-ffserver"
ENABLE_CROSS_COMPILE=--enable-cross-compile
#ENABLE_CROSS_COMPILE=--cross-compile

(test ! -z "$NO_MAKE" ||
case "$PLATFORM" in
macosx)
	SO_OPTS="-dynamiclib -Wl,-single_module -Wl,-read_only_relocs,suppress"
	LDFLAGS="-arch i386 -m32" CFLAGS="-arch i386 -m32" $CONFIGURE \
		--arch=i386 $ENABLE_CROSS_COMPILE --disable-mmx &&
	make clean all &&
	SO_OBJS="$(for i in $LIST
		do
			echo libav$i/*.o
		done) -lz -lbz2" &&
	gcc $SO_OPTS -arch i386 -m32 -o .libffmpeg32.dylib $SO_OBJS &&
	LDFLAGS="-arch x86_64 -m64" CFLAGS="-arch x86_64 -m64" $CONFIGURE \
		--arch=x86_64 $ENABLE_CROSS_COMPILE --disable-mmx &&
	make clean all &&
	gcc $SO_OPTS -arch x86_64 -o .libffmpeg64.dylib $SO_OBJS &&
	lipo -create .libffmpeg32.dylib .libffmpeg64.dylib \
		-output libffmpeg.dylib &&
	for i in $LIST
	do
		ln -s libffmpeg.dylib libav$i.dylib || break
	done
;;
win32)
	$CONFIGURE $ENABLE_CROSS_COMPILE --cross-prefix=mingw32- \
		--target-os=mingw32 \
		--enable-memalign-hack &&
	make clean all
;;
win64)
	CFLAGS="-D__MINGW32_MAJOR_VERSION=3 -D__MINGW32_MINOR_VERSION=15 -m64" \
	$CONFIGURE $ENABLE_CROSS_COMPILE --cross-prefix=x86_64-pc-mingw32- \
		--target-os=mingw32 --arch=x86_64 \
		--disable-avisynth &&
	(make -j50 -k clean all &&
	 for i in $LIST
	 do
		x86_64-pc-mingw32-dlltool --export-all-symbols \
			-l libav$i/av$i.dll $(find libav$i -name \*.o) || break
	 done)
;;
linux64)
	$CONFIGURE &&
	make clean all
;;
esac) &&

case "$PLATFORM" in
win*)
	LIBPREFIX=av
	EXTENSION=dll
;;
linux*)
	LIBPREFIX=libav
	EXTENSION=so
;;
macosx*)
	LIBPREFIX=lib
	EXTENSION=dylib
	LIST=ffmpeg
	test -h libffmpeg || ln -s . libffmpeg
;;
*)
	echo "Unknown platform: $PLATFORM"
	exit 1
esac &&

mkdir -p $PLATFORM &&
for i in $LIST
do
	cp -L *$i/$LIBPREFIX$i.$EXTENSION $PLATFORM/ || break
done &&
zip -9r $PRECOMPILED/ffmpeg-$PLATFORM.jar $(for i in $LIST
	do
		echo $PLATFORM/$LIBPREFIX$i.$EXTENSION
	done)

exit


