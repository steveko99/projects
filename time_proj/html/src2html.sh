#!/bin/bash
# File: src2html
# - print each source file in this project (time_proj) to HTML
# - Generate index.HTML to browse the project code in _out\_html
# - Gather a copy of each source file in _out\_src.zip
# - Zip the entire tree into _out.zip
#
# Note: If you add a source file to this project and you want to include it
# on the webpage, you need to explicitly list the file below.

#------------------------------------------------------------------------------
function print_index_header {
    echo "<!DOCTYPE html PUBLIC \"-//IETF//DTD HTML 2.0//EN\">"
    echo "<HTML>"
    echo "<HEAD>"
    echo "<TITLE>Time Sync Project</TITLE>"
    echo "</HEAD>"
    echo "<BODY>"
    echo "<H1>time_proj</H1>"
    echo "<PRE>"
}

function print_index_footer {
    echo "<a href=\"../_src.zip\">All source files zip</a>" >> _out/_html/index.html
    echo "</PRE>"
    echo "</BODY>"
    echo "</HTML>"
}

#------------------------------------------------------------------------------
# NOTE: Add new files you want published in HTML to the list here
#       If you add a new folder, add a mkdir for that too
#------------------------------------------------------------------------------
function convert_all {

    # the _out folder should be empty (unless the user didn't allow deletion)
    # make all the empty output folders first
    mkdir _out
    mkdir _out/_html
    mkdir _out/_html/src
    mkdir _out/_html/dogreps
    mkdir _out/_html/html

    # Start generating index.html
    print_index_header > _out/_html/index.html

    # convert and copy all the files to publish
    print_to_html   src/applib.c
    print_to_html   src/applib.h
    print_to_html   src/build.sh
    print_to_html   src/client.c
    print_to_html   src/jitter_cache.c
    print_to_html   src/jitter_cache.h
    print_to_html   src/server.c

    print_to_html   dogreps/dogreps.sh
    print_to_html   dogreps/doloop.sh

    print_to_html   html/src2html.sh
    print_to_html   html/clean.sh

    print_to_html   readme.txt
    copy_html       a1.html

    # finish the index.html
    print_index_footer >> _out/_html/index.html
}

#------------------------------------------------------------------------------
function print_to_html {
    # pretty print $1 to .html
    # put a link to $1.html in index.html
    # put a copy of the original source file in _src.zip

    INPUT_FILE=$ROOT/$1
    enscript  $EN_OPTIONS -p _out/_html/$1.html  $INPUT_FILE
    echo "<a href=\"$1.html\">$1</a>" >> _out/_html/index.html
    zip  _out/_src.zip  $INPUT_FILE
}

function copy_html {
    # if it is already a .html copy it to _out/_html tree
    # put a link to it in index.html
    # put a copy of the original source file in _src.zip

    INPUT_FILE=$ROOT/$1
    cp  $INPUT_FILE  _out/_html/$1
    echo "<a href=\"$1\">$1</a>" >> _out/_html/index.html
    zip  _out/_src.zip  $INPUT_FILE
}

#----------------------------------------------------------------------------
ROOT=..
EN_OPTIONS="-E -C --color -w html"

echo WARNING: will delete old _out folder before starting
rm -r -I _out

convert_all
    
echo WARNING: will overwrite old zip file
rm  -i _out.zip
zip -r _out.zip  _out

echo FINISHED: Created _out and _out.zip
