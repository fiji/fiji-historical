#!/bin/sh

action=cp
commit=
while test $# != 0
do
	case "$1" in
	--delete)
		action=mv
		;;
	--commit)
		commit=1
		;;
	*)
		break
		;;
	esac
	shift
done

test -z "$2" -o ! -z "$3" && {
	echo "Usage: $0 [--delete] [--commit] <source> <target>"
	exit 1
}

msg="$(if test -f "$2"; then echo Updated; else echo Added; fi) $2"
config=staged-plugins/$(basename "$1" .jar).config

test -f "$2" && test ! "$1" -nt "$2" &&
	(test ! -f "$config" || test ! "$config" -nt "$2") &&
	echo "$2 up-to-date" &&
	exit

if test -f "$config"
then
	cp "$config" plugins.config &&
	$action "$1" "$2" &&
	jar uvf "$2" plugins.config &&
	rm plugins.config
else
	$action "$1" "$2"
fi &&
case "$commit" in
1)
	git add "$config" "$2" &&
	git commit -s -m "$msg" "$config" "$2"
	;;
'')
	echo "$msg"
	;;
esac
