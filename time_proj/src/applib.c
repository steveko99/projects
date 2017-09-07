/*-----------------------------------------------------------------------------
    File: applib.c
    Description: Low level routines for UDP time project
    Author: Steve Koscho, March 15, 2015
-----------------------------------------------------------------------------*/

#include "applib.h"

void exit_process(char *s)
{
    perror(s);
    exit(1);
}

void print_warning(char *s)
{
    perror(s);
}

void print_trace(char *s)
{
    fprintf(stdout, "%s", s);
}

void zero_memory(void *p, int len)
{
    memset ((char *) p, 0, len);
}

char *heap_alloc(int size)
{
    char *p;

    if ( (p = malloc(size)) == NULL )
        exit_process("out of heap\n");

    return p;
}

void heap_free(void *p)
{
    if ( p )
        free(p);
}

/*-----------------------------------------------------------------------------
    collects everything written to stdout and stderr to a single logfile
-----------------------------------------------------------------------------*/

static FILE *s_filep = NULL;
static char *s_filename = NULL;

#define USER_MESSAGE "stdout and stderr are being redirected to %s\n"

void ForkOutputToLog(char *name)
{
    struct timeval tv = { 0, 0 };

    /* Build a unique filename of FMT: nameTimeStamp */

    if ( gettimeofday(&tv, NULL) == -1 )
        print_warning("gettimeofday error");

    int len = strlen(name) + 16 + 4 + 1;    // room enough for name%016u.log\0
    s_filename = heap_alloc(len);
    sprintf(s_filename,
            "%s%016u.log",      // nameTimeStamp
            name, (unsigned int) tv.tv_sec);

    fprintf(stderr, USER_MESSAGE, s_filename);

    int dup_stdout = dup(1);    // 1 is stdout
    int dup_stderr = dup(2);    // 2 is stderr

    if ( (s_filep = fopen(s_filename, "w")) == NULL ) {
        print_warning("fopen for dup stdout failed");
        return;
    }

    dup2(fileno(s_filep), 1);
    dup2(fileno(s_filep), 2);
}

void CloseLog()
{
    if ( s_filep != NULL )
        fclose(s_filep);

    if ( s_filename != NULL )
        heap_free(s_filename);
}

/*-----------------------------------------------------------------------------
    Convert a struct sockaddr address to a string
-----------------------------------------------------------------------------*/

void get_ip_str(const struct sockaddr *sa, char *str, size_t maxlen)
{
    switch(sa->sa_family)
    {
        case AF_INET:
        {
            struct sockaddr_in *p = (struct sockaddr_in *) sa;
            inet_ntop(AF_INET, &(p->sin_addr), str, maxlen);
            break;
        }

        case AF_INET6:
        {
            struct sockaddr_in6 *p = (struct sockaddr_in6 *) sa;
            inet_ntop(AF_INET6, &(p->sin6_addr), str, maxlen);
            break;
        }

        default:
        {
            fprintf(stderr, "programming error get_ip_str\n");
        }
    }
}
