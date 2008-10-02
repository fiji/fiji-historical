#!/bin/sh

CWD="$(dirname "$0")"

case "$(uname -s)" in
Darwin) platform=macosx; exe=;;
Linux)
	case "$(uname -m)" in
		x86_64) platform=linux64;;
		*) platform=linux;;
	esac; exe=;;
MINGW*|CYGWIN*) platform=win32; exe=.exe;;
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

# make sure fake.jar is up-to-date
test "a$targets" != afake.jar &&
test ! -f "$CWD"/fake.jar -o "$CWD"/fake/Fake.java -nt "$CWD"/fake.jar && {
	sh "$0" fake.jar || exit
}

# make sure the Fiji launcher is up-to-date
test "a$targets" != afake.jar -a "a$targets" != afiji &&
test ! -f "$CWD"/fiji -o "$CWD"/fiji.cxx -nt "$CWD"/fiji$exe && {
	sh "$0" fiji || exit
}

# still needed for Windows, which cannot overwrite files that are in use
test -f "$CWD"/fiji$exe -a -f "$CWD"/fake.jar &&
test "a$targets" != afake.jar -a "a$targets" != afiji &&
exec "$CWD"/fiji$exe --fake "$@"

# fall back to precompiled
test -f "$CWD"/precompiled/fiji-$platform$exe \
	-a -f "$CWD"/precompiled/fake.jar &&
exec "$CWD"/precompiled/fiji-$platform$exe --fake -- "$@"

# fall back to calling Fake with system Java
test -f "$CWD"/fake.jar &&
java -classpath "$CWD"/fake.jar Fake "$@"

# fall back to calling precompiled Fake with system Java
test -f "$CWD"/precompiled/fake.jar &&
java -classpath "$CWD"/precompiled/fake.jar Fake "$@"

# fall back to compiling and running with system Java
javac -source 1.3 -target 1.3 "$CWD"/fake/Fake.java &&
java -classpath "$CWD"/fake Fake "$@"
