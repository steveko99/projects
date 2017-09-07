/*-----------------------------------------------------------------------------
    File: jitter_cache.c

    Description:

        Implements the logic of the sample smoothing algorithm as specified
        in the project.

        Remember the 8 most recent samples for clock adjustment estimates.

        After getting 8 samples at the start, then the smoothing algorithm
        begins.  It chooses the minimum clock difference estimate and will
        use that one until either:

            1. It is used 8 times and expires
            2. A new sample that is closer to zero arrives

    Author: Steve Koscho - March 15, 2015
-----------------------------------------------------------------------------*/

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "applib.h"

static double abs_double(double x) {
    if ( x < 0.0 )
        return -x;
    return x;
}

#define JITTER_CACHE_SIZE  8

struct JITTER_SAMPLE
{
    double rtt;
    double theta;
};

static struct JITTER_SAMPLE jitters[JITTER_CACHE_SIZE] = { 0 };
static int jit_idx = 0;
static int jit_count = 0;

/*-----------------------------------------------------------------------------
    You can only add to the cache and it remembers JITTER_CACHE_SIZE
    previous samples
-----------------------------------------------------------------------------*/
void jitter_add(double rtt, double theta)
{
    int i = jit_idx;

    jitters[i].rtt = rtt;
    jitters[i].theta = theta;

    jit_idx++;
    jit_count++;

    if ( jit_idx  >= JITTER_CACHE_SIZE )
        jit_idx = 0;

    if ( jit_count >= JITTER_CACHE_SIZE )
        jit_count = JITTER_CACHE_SIZE;
}

/*-----------------------------------------------------------------------------
-----------------------------------------------------------------------------*/
int jitter_is_init()
{
    return jit_count >= JITTER_CACHE_SIZE;
}

/*-----------------------------------------------------------------------------
-----------------------------------------------------------------------------*/
double jitter_get_min_theta()
{
    if ( jit_count < JITTER_CACHE_SIZE )
        return 0.0;

    double min_theta = 0.0;
    double new_theta;
    int i;

    min_theta = jitters[0].theta;

    for ( i=1; i<JITTER_CACHE_SIZE; i++ ) {

        new_theta = jitters[i].theta;

        if ( abs_double(new_theta) < abs_double(min_theta) ) {
            min_theta = new_theta;
        }
    }

    return min_theta;
}
