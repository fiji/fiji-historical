#!/bin/sh

# This script checks if the Fiji launcher for a given platform is
# up-to-date.
#
# The idea is to check every line of history (in reverse chronological
# order, from HEAD) stopping when the Fiji launcher was modified.  If
# somewhere on that line, the source was modified, the launcher needs
# rebuilding.

test -z "$2" && {
	echo "Usage: $0 <source> <launcher>..."
	exit 1
}

source=$1
shift
count=0
while test $# != 0
do

	launcher=$1
	shift

	# Find out the edge commits (edges being the commits changing the
	# launcher, but having no offspring with the same property)

	edges=
	while true
	do
		edge=$(git rev-list -1 HEAD $edges -- $launcher)
		test -z "$edge" && break
		edges="$edges ^$edge"
	done

	# Now verify that that the source has not changed since any of those
	# edges

	test -z "$(git rev-list HEAD $edges -- $source)" || {
		echo "$launcher is not up-to-date."
		count=$(($count+1))
	}
done

test $count = 0

