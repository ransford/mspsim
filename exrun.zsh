#!/bin/zsh
# USAGE: $0 <testcase1> <testcase2>
# e.g.   $0 crc-vanilla
# set a $VTRACE=filename env var to run against a voltage trace

MEMENTOS="/opt/mementos/src/mementos"
JAVACMD="java -classpath '.:lib/jcommon-1.0.14.jar:lib/jfreechart-1.0.11.jar:lib/jipv6.jar' se.sics.mspsim.platform.wisp.WispNode -nogui -exitwhendone"
LOGDIRPART=""
if [[ -n "$VTRACE" ]]; then
	JAVACMD+=" -voltagetrace=${VTRACE}"
	LOGDIRPART="trace${VTRACE:t:r}/"
fi
CHECKPOINTSDIR=checkpoints
RANDNAME="$RANDOM"
CLONEDIR="$MEMENTOS/../$RANDNAME"

# kill a java run after this many seconds
TIMEOUT=120 # i'm impatient

# TESTCASES+=(program_a expectedretval_a)
typeset -A TESTCASES
TESTCASES+=(crc-vanilla2 0x1d sense 0x5b rsa64 0x0b)
typeset -A EXTRASIMOPTS
EXTRASIMOPTS+=(rsa64 "-msp430f1611")
typeset -A EXTRAMAKEOPTS
EXTRAMAKEOPTS+=(rsa64 "MCU=msp430x1611")

typeset -a VOLTAGES
VOLTAGES=(2.4 2.6 2.8 3.0 3.2 3.4)
#VOLTAGES=(2.8 2.9 3.0)

typeset -a TIMERINTERVALS
TIMERINTERVALS=(20000 40000 60000)
#TIMERINTERVALS=()

# go to where this script is
HGDIR=`dirname "$0"`
cd "$HGDIR" || exit 1
LOGSDIR="${HGDIR}/logs"
mkdir -p "${LOGSDIR}/${LOGDIRPART}"

if [[ $# -gt 0 ]]; then
	typeset -a SELECTED
	for x in $@; do
		print "Will run $x"
		if [[ -z "${TESTCASES[$x]}" ]]; then
			echo "Unknown testcase \"$x\""
			exit 1
		fi
		SELECTED+=($x)
	done
	SELECTED_TESTCASES="${SELECTED}"
else
	SELECTED_TESTCASES="${(k)TESTCASES}"
fi

function runcmd () {
	TC="$1"; shift
	CMDTIMEOUT="$1"; shift
	RUNLOG="${LOGSDIR}/${LOGDIRPART}${TC}$1"; shift
	CMD="$@"
	echo "Running command:\e[1;30m $CMD \e[0;m"
	./alarm.pl $CMDTIMEOUT "$CMD 2>$RUNLOG"
	RETVAL=$?
	if [[ $RETVAL -ne 0 ]]; then
		echo -n "\e[1;31m"
		echo -n "Command failed!"
		echo "\e[0;m"
		# read UHOH
	else
		echo -n "\e[1;32m"
		echo -n "Command completed successfully."
		echo "\e[0;m"
	fi
	return $RETVAL
}

# build the testcase with the desired threshold
function buildtestcase () {
	TC="$1"; shift
	RN="$1"; shift
	VT="$1"; shift
	TI="$1"; shift
	pushd "$CLONEDIR"
	echo "Building $TC (VTHRESH=$VT, TIMERINT=$TI) ..."
	make ${EXTRAMAKEOPTS[$TC]} TARGET="samples/$TC" VTHRESH="$VT" TIMERINT="$TI" \
		clean all || exit 2
	popd
}

function uptodatecheck () {
	pushd "$CLONEDIR"
	git diff --exit-code >/dev/null
	DIFFEXIT=$?
	git diff --exit-code --staged >/dev/null
	DIFFEXIT=$((DIFFEXIT+$?))
	if [[ $DIFFEXIT -gt 0 ]]; then
		echo -n "\e[1;31m"
		echo "WARNING: working copy in $MEMENTOS out of date?"
		echo -n "\e[0;31m"
		echo "$STATUS"
		echo -n "\e[0;m"
		svn diff
		echo "------"
		echo -n "\e[1;37mOK? [y/N]\e[0;m "
		read OK
		if [[ $OK != 'y' ]]; then
			exit 1
		fi
	fi
	popd
}

function setup () {
	git clone "$MEMENTOS" "$CLONEDIR"
	pushd "$CLONEDIR"
	./configure
	popd
}

setup

mkdir -p "$CHECKPOINTSDIR"

typeset -a RUNNAMES
echo "SELECTED_TESTCASES: ${SELECTED_TESTCASES}"
for FIRMWARE in ${(z)SELECTED_TESTCASES}; do
	RUNNAME="${FIRMWARE}-${RANDOM}"
	RUNNAMES+=($RUNNAME)

	# +plainmspgcc, +plainclang, +oracle; voltage & interval don't matter
	buildtestcase "$FIRMWARE" "$RUNNAME" '' '' || exit 2
	sed -e "s,@@NAME@@,${RUNNAME},g" scripts/uninstr.sc.tmpl > \
		scripts/uninstr-${RUNNAME}.sc
	runcmd $RUNNAME+plainmspgcc $(($TIMEOUT/2)) .log \
		"${JAVACMD} "$CLONEDIR"/samples/${FIRMWARE}+plainmspgcc" \
		"-expectedexitcode=${TESTCASES[$FIRMWARE]}" \
		"-autorun=scripts/uninstr-${RUNNAME}.sc" \
		${EXTRASIMOPTS[$FIRMWARE]}
	runcmd $RUNNAME+plainclang $(($TIMEOUT/2)) .log \
		"${JAVACMD} "$CLONEDIR"/samples/${FIRMWARE}+plainclang" \
		"-expectedexitcode=${TESTCASES[$FIRMWARE]}" \
		"-autorun=scripts/uninstr-${RUNNAME}.sc" \
		${EXTRASIMOPTS[$FIRMWARE]}
	sed -e "s,@@NAME@@,${RUNNAME},g" scripts/nocap.sc.tmpl > \
		scripts/nocap-${RUNNAME}.sc
	runcmd $RUNNAME+plainmspgcc $TIMEOUT .log \
		"${JAVACMD} "$CLONEDIR"/samples/${FIRMWARE}+plainmspgcc" \
		"-expectedexitcode=${TESTCASES[$FIRMWARE]}" \
		"-autorun=scripts/nocap-${RUNNAME}.sc" \
		${EXTRASIMOPTS[$FIRMWARE]}
	runcmd $RUNNAME+plainclang $TIMEOUT .log \
		"${JAVACMD} "$CLONEDIR"/samples/${FIRMWARE}+plainclang" \
		"-expectedexitcode=${TESTCASES[$FIRMWARE]}" \
		"-autorun=scripts/nocap-${RUNNAME}.sc" \
		${EXTRASIMOPTS[$FIRMWARE]}
	rm scripts/nocap-${RUNNAME}.sc
	sed -e "s,@@NAME@@,${RUNNAME},g" scripts/oracle.sc.tmpl > \
		scripts/oracle-${RUNNAME}.sc
	runcmd $RUNNAME+oracle $((2*$TIMEOUT)) .log \
		"${JAVACMD} "$CLONEDIR"/samples/${FIRMWARE}+oracle" \
		"-expectedexitcode=${TESTCASES[$FIRMWARE]}" \
		"-autorun=scripts/oracle-${RUNNAME}.sc" \
		${EXTRASIMOPTS[$FIRMWARE]}
	rm scripts/oracle-${RUNNAME}.sc

	# scripts for mspsim for each instrumentation strategy
	sed -e "s,@@NAME@@,${RUNNAME},g" scripts/latch.sc.tmpl > \
		scripts/latch-${RUNNAME}.sc
	sed -e "s,@@NAME@@,${RUNNAME},g" scripts/return.sc.tmpl > \
		scripts/return-${RUNNAME}.sc
	sed -e "s,@@NAME@@,${RUNNAME},g" scripts/timer.sc.tmpl > \
		scripts/timer-${RUNNAME}.sc

	# +latch, +return, +timer are voltage dependent
	for VOLTAGE in $VOLTAGES; do
		buildtestcase "$FIRMWARE" "$RUNNAME" "$VOLTAGE" '' || exit 2

		# +latch
		runcmd $RUNNAME+latch $TIMEOUT "-${VOLTAGE}.log" \
			"${JAVACMD} "$CLONEDIR"/samples/${FIRMWARE}+latch" \
			"-expectedexitcode=${TESTCASES[$FIRMWARE]}" \
			"-autorun=scripts/latch-${RUNNAME}.sc" \
			${EXTRASIMOPTS[$FIRMWARE]}

		# +return
		runcmd $RUNNAME+return $TIMEOUT "-${VOLTAGE}.log" \
			"${JAVACMD} "$CLONEDIR"/samples/${FIRMWARE}+return" \
			"-expectedexitcode=${TESTCASES[$FIRMWARE]}" \
			"-autorun=scripts/return-${RUNNAME}.sc" \
			${EXTRASIMOPTS[$FIRMWARE]}

		# +timer is voltage and interval dependent
		for TINTERVAL in $TIMERINTERVALS; do
			buildtestcase "$FIRMWARE" "$RUNNAME" "$VOLTAGE" "$TINTERVAL" || exit 2
			runcmd $RUNNAME+timer $TIMEOUT "-${VOLTAGE}-${TINTERVAL}.log" \
				"${JAVACMD} "$CLONEDIR"/samples/${FIRMWARE}+timer" \
				"-expectedexitcode=${TESTCASES[$FIRMWARE]}" \
				"-autorun=scripts/timer-${RUNNAME}.sc" \
				${EXTRASIMOPTS[$FIRMWARE]}
		done
	done

	rm -f \
		scripts/latch-${RUNNAME}.sc \
		scripts/return-${RUNNAME}.sc \
		scripts/timer-${RUNNAME}.sc
done

echo "Run logs:"
for R in $RUNNAMES; do
	ls -l "${LOGSDIR}/${R}"*
done
