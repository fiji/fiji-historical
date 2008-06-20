#!/bin/sh

CWD="$(dirname "$0")"

case "$(uname -s)" in
Darwin) platform=macosx; exe=;;
Linux)
	 case "$(uname -m)" in
		x86_64) platform=linux-amd64;;
		*) platform=linux;;
	esac; exe=;;
MINGW*|CYGWIN*) platform=win32; exe=.exe;;
esac

test "a$*" != afake.jar &&
test ! -f "$CWD"/fake.jar -o "$CWD"/fake/Fake.java -nt "$CWD"/fake.jar && {
	sh "$0" fake.jar || exit
}

test "a$*" != afake.jar -a "a$*" != afiji &&
test ! -f "$CWD"/fiji -o "$CWD"/fiji.cxx -nt "$CWD"/fiji$exe && {
	sh "$0" fiji || exit
}

test -f "$CWD"/fiji$exe -a -f "$CWD"/fake.jar &&
exec "$CWD"/fiji$exe --fake "$@"

test -f "$CWD"/precompiled/fiji-$platform$exe \
	-a -f "$CWD"/precompiled/fake.jar &&
exec "$CWD"/precompiled/fiji-$platform$exe --fake -- "$@"

test -f "$CWD"/fake.jar &&
java -classpath "$CWD"/fake.jar Fake "$@"

test -f "$CWD"/precompiled/fake.jar &&
java -classpath "$CWD"/Ãprecompiled/fake.jar Fake "$@"

javac "$CWD"/fake/Fake.java &&
java -classpath "$CWD"/fake Fake "$@"
