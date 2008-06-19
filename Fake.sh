#!/bin/sh

CWD="$(dirname "$0")"

test "a$@" != afake.jar -a "$CWD"/fake/Fake.java -nt "$CWD"/fake.jar &&
sh "$0" fake.jar

test -f "$CWD"/fiji &&
exec "$CWD"/fiji --fake "$@"

case "$(uname -s)" in
Darwin) platform=macosx;;
Linux) platform=linux;;
MINGW*|CYGWIN*) platform=win32;;
esac

test -f "$CWD"/precompiled/fiji-$platform &&
exec "$CWD"/precompiled/fiji-$platform --fake -- "$@"

test -f "$CWD"/fake.jar &&
java -classpath "$CWD"/fake.jar Fake "$@"

test -f "$CWD"/precompiled/fake.jar &&
java -classpath "$CWD"/Ãprecompiled/fake.jar Fake "$@"

javac "$CWD"/fake/Fake.java &&
java -classpath "$CWD"/fake Fake "$@"
