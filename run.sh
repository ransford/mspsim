#!/bin/sh

while getopts "hs:t:" opt; do
	case $opt in
		s) EXTRAFLAGS="$EXTRAFLAGS -autorun=$OPTARG" ;;
		t) EXTRAFLAGS="$EXTRAFLAGS -voltagetrace=$OPTARG" ;;
		h) echo "Usage: $(basename "$0") [options] firmware\n" \
			"  -s script	path to autorun script\n" \
			"  -t trace	voltage trace file" \
			>&2 ;;
	esac
done
shift $(($OPTIND - 1))

FIRMWARE="$1"; shift
test -n "$FIRMWARE" || exit 1
EXTRAFLAGS="$EXTRAFLAGS $*"

java ${DEBUGOPTS} \
	-classpath "dist/mspsim.jar:lib/jcommon-1.0.14.jar:lib/jfreechart-1.0.11.jar:lib/jipv6.jar" \
	se.sics.mspsim.platform.crfid.MooNode \
	-nogui \
	-exitwhendone \
	${EXTRAFLAGS} \
	${FIRMWARE}
