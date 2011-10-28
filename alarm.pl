#!/usr/bin/env perl
# adapted from http://www.mail-archive.com/beginners@perl.org/msg81677.html

use strict;
use warnings;
use Term::ANSIColor;

if (scalar(@ARGV) < 2) {
    print STDERR "Usage: $0 <seconds> <command> [args ...]\n";
    exit 1;
}

my $SECONDS = shift @ARGV;
my @COMMAND = @ARGV;

sub timeout ($) {
    my $pid = shift;
    # devour my own children [pace cronus]
    local $SIG{'INT'} = 'IGNORE';
    my @kids = split /\s+/,
       qx(ps -axo ppid,pid | grep "^ \*${pid}" | awk '{print \$2}');
    kill 'INT', @kids;
    die 'alarm';
}

my $kidpid;
my $plskillus = 0;
my $childnormal = 0;

eval {
    local $SIG{'ALRM'} = sub { $plskillus = 1; };
    local $SIG{'CHLD'} = sub { $childnormal = 1; };

    alarm $SECONDS;
    if ($kidpid = fork()) {
        # parent process
        while (1) {
            $childnormal and last;
            $plskillus and timeout($kidpid) and last;
            sleep 1;
        }
        waitpid($kidpid, 0);
    } else {
        # child process
        exec(@COMMAND);
        exit;
    }
    alarm 0;
};
if ($@ =~ 'alarm') {
    # eval block produced an error or took too long
    print STDERR color 'bold red';
    print STDERR "Killed command after ${SECONDS}s: @COMMAND\n";
    print STDERR color 'reset';
    exit 1;
}

# if we get here, command executed normally
exit 0;
