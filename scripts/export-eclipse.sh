#!/bin/sh

# This is a shell script because piping is easier with bash than with Python

cd "$(dirname "$0")"/..

die () {
	echo "$@"
	exit 1
}

./fiji --fake eclipse || exit

export GIT_INDEX_FILE=.git/tmpindex
cp .git/index $GIT_INDEX_FILE

git add -f .classpath .project Fiji.launch Fake.launch ||
	die "Missing files"

tree=$(git write-tree) || die "Could not write temporary tree"
git archive --format=zip --prefix=Fiji-eclipse/ $tree > Fiji-eclipse.zip
