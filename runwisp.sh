#!/bin/sh

WHEREAMI=$(dirname "$0")

while getopts "f:hs:t:" opt; do
	case $opt in
		f) EXTRAFLAGS="$EXTRAFLAGS -sin_freq=$OPTARG" ;;
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
	-classpath "${WHEREAMI}/dist/mspsim.jar:${WHEREAMI}/lib/jcommon-1.0.14.jar:${WHEREAMI}/lib/jfreechart-1.0.11.jar:${WHEREAMI}/lib/jipv6.jar" \
	se.sics.mspsim.platform.crfid.WispNode \
	-nogui \
	-exitwhendone \
	${EXTRAFLAGS} \
	${FIRMWARE}
