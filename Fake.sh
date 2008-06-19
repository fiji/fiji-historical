#!/bin/sh

CWD="$(dirname "$0")"

case "$(uname -s)" in
Darwin) platform=macosx; exe=;;
Linux) platform=linux; exe=;;
MINGW*|CYGWIN*) platform=win32; exe=.exe;;
esac

test "a$@" != afake.jar &&
test ! -f "$CWD"/fake.jar -o "$CWD"/fake/Fake.java -nt "$CWD"/fake.jar &&
sh "$0" fake.jar

test "a$@" != afake.jar -a "a$@" != afiji &&
test ! -f "$CWD"/fiji -o "$CWD"/fiji.cxx -nt "$CWD"/fiji$exe &&
sh "$0" fiji

test -f "$CWD"/fiji$exe &&
exec "$CWD"/fiji$exe --fake "$@"

test -f "$CWD"/precompiled/fiji-$platform$exe &&
exec "$CWD"/precompiled/fiji-$platform$exe --fake -- "$@"

test -f "$CWD"/fake.jar &&
java -classpath "$CWD"/fake.jar Fake "$@"

test -f "$CWD"/precompiled/fake.jar &&
java -classpath "$CWD"/Ãprecompiled/fake.jar Fake "$@"

javac "$CWD"/fake/Fake.java &&
java -classpath "$CWD"/fake Fake "$@"
