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
