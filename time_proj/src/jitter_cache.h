#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "applib.h"

void jitter_add(double rtt, double theta);
int jitter_is_init();
double jitter_get_min_theta();
