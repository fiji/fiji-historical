#!/bin/sh

CWD="$(dirname "$0")"

can_overwrite_in_use_files=t
case "$(uname -s)" in
Darwin) platform=macosx; exe=;;
Linux)
	case "$(uname -m)" in
		x86_64) platform=linux64;;
		*) platform=linux;;
	esac; exe=;;
MINGW*|CYGWIN*) platform=win32; exe=.exe; can_overwrite_in_use_files=;;
esac

strip_variables () {
	while test $# -ge 1
	do
		case "$1" in
		*=*) ;;
		*) echo "$1";
		esac
		shift
	done
}

targets=$(strip_variables "$@")

test "a$targets" != afake.jar &&
test ! -f "$CWD"/fake.jar -o "$CWD"/fake/Fake.java -nt "$CWD"/fake.jar && {
	sh "$0" fake.jar || exit
}

test "a$targets" != afake.jar -a "a$targets" != afiji &&
test ! -f "$CWD"/fiji -o "$CWD"/fiji.cxx -nt "$CWD"/fiji$exe && {
	sh "$0" fiji || exit
}

test -f "$CWD"/fiji$exe -a -f "$CWD"/fake.jar &&
(test ! -z "$can_overwrite_in_use_files" ||
 test "a$targets" != afake.jar -a "a$targets" != afiji ) &&
exec "$CWD"/fiji$exe --fake "$@"

test -f "$CWD"/precompiled/fiji-$platform$exe \
	-a -f "$CWD"/precompiled/fake.jar &&
exec "$CWD"/precompiled/fiji-$platform$exe --fake -- "$@"

test -f "$CWD"/fake.jar &&
java -classpath "$CWD"/fake.jar Fake "$@"

test -f "$CWD"/precompiled/fake.jar &&
java -classpath "$CWD"/precompiled/fake.jar Fake "$@"

javac -source 1.3 -target 1.3 "$CWD"/fake/Fake.java &&
java -classpath "$CWD"/fake Fake "$@"
