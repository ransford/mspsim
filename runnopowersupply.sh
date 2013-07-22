#!/bin/sh

WHEREAMI=$(dirname "$0")

exec "$WHEREAMI"/runwisp.sh -s "$WHEREAMI"/scripts/nopowersupply.sc.tmpl "$@"
