FIRMWARE="$1"
TRACE="$2"
test -n "$FIRMWARE" || exit 1
EXTRAFLAGS="$*"

java ${DEBUGOPTS} -classpath ".:lib/jcommon-1.0.14.jar:lib/jfreechart-1.0.11.jar:lib/jipv6.jar" se.sics.mspsim.platform.wisp.WispNode ${FIRMWARE} -nogui -voltagetrace=${TRACE} -autorun=scripts/wisp.sc -exitwhendone ${EXTRAFLAGS}
