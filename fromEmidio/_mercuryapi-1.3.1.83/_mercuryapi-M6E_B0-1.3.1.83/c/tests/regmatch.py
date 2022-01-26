#!/usr/bin/env python
#
# Match one file against another file consisting of a large regex.

import logging
log = logging.getLogger(__name__)
logsh = logging.StreamHandler()
logsh.setFormatter(logging.Formatter(logging.BASIC_FORMAT))
log.addHandler(logsh)

import optparse
import re
import sys

def swapstr(str, a, b):
    rega = re.escape(a)
    regb = re.escape(b)
    str = re.sub(rega, "@@@", str)
    str = re.sub(regb, a, str)
    str = re.sub("@@@", b, str)
    return str

def main(argv=None):
    if argv is None:
        argv = sys.argv

    parser = optparse.OptionParser()
    parser.add_option("--debug", default="NOTSET")
    opts,args = parser.parse_args()

    log.setLevel(getattr(logging,opts.debug))
    log.debug("debug_level=%s", opts.debug)

    textfilename = args[0]
    regexfilename = args[1]
    textfile = open(textfilename)
    regexfile = open(regexfilename)

    text = textfile.read()
    regex = regexfile.read()

    # Require that the user prefix certain special regex symbols
    # with a backslash - the opposite of the python style, since literal
    # () and [] are common in this output.
    # Don't want to use re.escape(), as that would defeat the
    # whole point of this exercise.
    regex = swapstr(regex, "\[", "[")
    regex = swapstr(regex, "\]", "]")
    regex = swapstr(regex, "\(", "(")
    regex = swapstr(regex, "\)", ")")
    # Also, allow \ at the end of a line to mean line continuation.
    regex = re.sub("\\\\[\r\n]", "", regex)

    textfile.close()
    regexfile.close()

    regprog = re.compile(regex, re.MULTILINE)

    x = regprog.match(text)
    if x:
        return 0
    else:
        log.debug("match failed")

        pre_regex = prune_regex_until_match(regex, text)
        log.debug("pre_regex = %s", repr(pre_regex))
        pre_text = prune_text_until_match(pre_regex, text)
        log.debug("pre_text = %s", repr(pre_text))

        clean_regex = ''.join([
                pre_text,
                regex[len(pre_regex):],
                ])

        open("%s.clean"%regexfilename,'w').write(clean_regex)

        return 1


## BUG: Since pruning works line-by-line, can be fooled by
# a key line that is a prefix of the output line; e.g.,
# key_line: /reader/commandTimeout: 100
# out_line: /reader/commandTimeout: 1000
# key_line matches out_line, even though it is wrong.

def prune_regex_until_match(regex, text):
    """Find the subsection of a regular expression that actually matches a block of text
    regex -- Multi-line regular expression to match against text
    text -- Text to match against regex
    Returns the beginning part of regex that actually matches against the beginning of text.
    """
    regex_lines = regex.split('\n')
    for iline, line in zip(range(len(regex_lines)), regex_lines):
        log.debug("regex line %d: %s", iline, repr(line))

    # Trim back regex from end until it matches beginning of text
    for i in range(len(regex_lines), 0, -1):
        log.debug("")
        log.debug("regex_lines[:%d]:", i)

        sub_lines = regex_lines[:i]
        for iline, line in zip(range(len(sub_lines)), sub_lines):
            log.debug("sub line %d: %s", iline, repr(line))

        sub_regex = '\n'.join(sub_lines)
        sub_regprog = re.compile(sub_regex, re.MULTILINE)
        x = sub_regprog.match(text)
        if x:
            log.debug("MATCH")
            return sub_regex
        else:
            log.debug("no match")
    return ''

def prune_text_until_match(regex, text):
    """Find the subsection of a block of text that actually matches a regular expression
    regex -- Multi-line regular expression to match against text
    text -- Text to match against regex
    Returns the beginning part of text that matches against regex
    """
    text_lines = text.split('\n')
    for iline, line in zip(range(len(text_lines)), text_lines):
        log.debug("text line %d: %s", iline, repr(line))

    regprog = re.compile(regex, re.MULTILINE)
    log.debug("%s.regex = %s" % (
            sys._getframe().f_code.co_name,
            repr(regex),
            ))

    # Build up text from front until it matches regex
    for i in range(len(text_lines)):
        log.debug("")
        log.debug("text_lines[:%d]:", i)

        sub_lines = text_lines[:i]
        for iline, line in zip(range(len(sub_lines)), sub_lines):
            log.debug("sub line %d: %s", iline, repr(line))

        sub_text = '\n'.join(sub_lines)
        x = regprog.match(sub_text)
        if x:
            log.debug("MATCH")
            return sub_text
        else:
            log.debug("no match")
    return ''

if __name__ == "__main__":
    sys.exit(main())
