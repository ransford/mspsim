FIRMWARE="$1"
test -n "$FIRMWARE" || exit 1
EXTRAFLAGS="$*"

java ${DEBUGOPTS} -classpath "mspsim.jar:lib/jcommon-1.0.14.jar:lib/jfreechart-1.0.11.jar:lib/jipv6.jar" se.sics.mspsim.platform.crfid.MooNode ${FIRMWARE} -nogui -autorun=scripts/nocap.sc.tmpl -exitwhendone ${EXTRAFLAGS}
