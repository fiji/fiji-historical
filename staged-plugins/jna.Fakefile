JAVAVERSION=1.5
all <- jna.jar

PACKAGE=com/sun/jna
DIST=jar:file:jnalib/dist
JNILIB=libjnidispatch.jnilib
SO=libjnidispatch.so
DLL=jnidispatch.dll
jna.jar <- \
	$PACKAGE/darwin/$JNILIB[$DIST/darwin.jar!/$JNILIB] \
	$PACKAGE/freebsd-amd64/$SO[$DIST/freebsd-amd64.jar!/$SO] \
	$PACKAGE/freebsd-i386/$SO[$DIST/freebsd-i386.jar!/$SO] \
	$PACKAGE/linux-amd64/$SO[$DIST/linux-amd64.jar!/$SO] \
	$PACKAGE/linux-i386/$SO[$DIST/linux-i386.jar!/$SO] \
	$PACKAGE/openbsd-i386/$SO[$DIST/openbsd-i386.jar!/$SO] \
	$PACKAGE/sunos-amd64/$SO[$DIST/sunos-amd64.jar!/$SO] \
	$PACKAGE/sunos-sparc/$SO[$DIST/sunos-sparc.jar!/$SO] \
	$PACKAGE/sunos-sparcv9/$SO[$DIST/sunos-sparcv9.jar!/$SO] \
	$PACKAGE/sunos-x86/$SO[$DIST/sunos-x86.jar!/$SO] \
	$PACKAGE/win32-amd64/$DLL[$DIST/win32-amd64.jar!/$DLL] \
	$PACKAGE/win32-x86/$DLL[$DIST/win32-x86.jar!/$DLL] \
	\
	jnalib/src/**/*.java

bundle-linux[] <- cross-linux jnalib/dist/linux-i386.jar
bundle-win32[] <- cross-win32 jnalib/dist/win32-x86.jar
bundle-linux64[] <- cross-linux64 jnalib/dist/linux-amd64.jar
bundle-win64[] <- cross-win64 jnalib/dist/win32-amd64.jar
bundle-macosx[] <- cross-macosx jnalib/dist/darwin.jar

DIST=jnalib/dist
jnalib/dist/darwin.jar <- $JNILIB[$DIST/darwin/libjnidispatch.dylib]
jnalib/dist/freebsd-amd64.jar <- $SO[$DIST/freebsd-amd64/$SO]
jnalib/dist/freebsd-i386.jar <- $SO[$DIST/freebsd-i386/$SO]
jnalib/dist/linux-amd64.jar <- $SO[$DIST/linux-amd64/$SO]
jnalib/dist/linux-i386.jar <- $SO[$DIST/linux-i386/$SO]
jnalib/dist/openbsd-i386.jar <- $SO[$DIST/openbsd-i386/$SO]
jnalib/dist/sunos-amd64.jar <- $SO[$DIST/sunos-amd64/$SO]
jnalib/dist/sunos-sparc.jar <- $SO[$DIST/sunos-sparc/$SO]
jnalib/dist/sunos-sparcv9.jar <- $SO[$DIST/sunos-sparcv9/$SO]
jnalib/dist/sunos-x86.jar <- $SO[$DIST/sunos-x86/$SO]
jnalib/dist/win32-amd64.jar <- $DLL[$DIST/win32-amd64/$DLL]
jnalib/dist/win32-x86.jar <- $DLL[$DIST/win32-x86/$DLL]

cross-*[../scripts/build-jna.sh *] <-
