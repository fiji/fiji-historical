#!/bin/sh

CONFIGURE="./configure --enable-shared --disable-static 
	--disable-ffmpeg --disable-ffplay --disable-ffserver"
ENABLE_CROSS_COMPILE=--enable-cross-compile
#ENABLE_CROSS_COMPILE=--cross-compile

LIST="util codec format"

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
esac
