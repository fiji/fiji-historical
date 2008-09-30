#!/bin/sh

# This is a shell script because piping is easier with bash than with Python

cd "$(dirname "$0")"/..

die () {
	echo "$@"
	exit 1
}

test -d ImageJA/.git || die "ImageJA was not checked out"

./fiji --fake netbeans || exit

export GIT_INDEX_FILE=.git/tmpindex
cp .git/index $GIT_INDEX_FILE

git rm --cached ImageJA
(cd ImageJA && GIT_INDEX_FILE=.git/index git ls-files --stage) |
	sed -e "/	ij\/plugin\/Memory.java$/d" \
		-e "/	.akefile$/d" \
		-e "s/	/&ImageJA\//" |
	git update-index --index-info ||
	die "Could not add ImageJA"

git add -f build.xml manifest.mf \
	nbproject/project.xml nbproject/project.properties ||
	die "Missing files"

export GIT_ALTERNATE_OBJECT_DIRECTORIES=ImageJA/.git/objects/
tree=$(git write-tree) || die "Could not write temporary tree"
git archive --format=zip --prefix=Fiji-netbeans/ $tree > Fiji-netbeans.zip
