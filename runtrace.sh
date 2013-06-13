#!/bin/sh

WHEREAMI=$(dirname "$0")

exec "$WHEREAMI"/runmoo.sh -s "$WHEREAMI"/scripts/moo.sc "$@"
