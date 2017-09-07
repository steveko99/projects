/* applib.h */

#ifndef _APPLIB_H_INCLUDED_
#define _APPLIB_H_INCLUDED_ 1

#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <sys/socket.h>

void exit_process(char *s);
void print_warning(char *s);
void print_trace(char *s);
void zero_memory(void *p, int len);
void ForkOutputToLog(char *name);
void CloseLog();
void get_ip_str(const struct sockaddr *sa, char *s, size_t maxlen);

#endif // _APPLIB_H_INCLUDED_
