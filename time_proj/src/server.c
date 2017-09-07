/*-----------------------------------------------------------------------------
    File: server.c

    Author: Steve Koscho - March 12, 2015

    Description:
        This implements the server for the Time Sync Project.
-----------------------------------------------------------------------------*/

#include <arpa/inet.h>
#include <sys/socket.h>
#include <netinet/in.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "applib.h"

#define BUFLEN          512     // configure
#define SERVER_PORT     9990    // user input

struct UDP_SERVER
{
    int socket;
    char buf[BUFLEN];
};

static struct UDP_SERVER udp_server = { 0 };

/*-----------------------------------------------------------------------------
    This is called after the recv().
    Purpose of this function is to write the server RESPONSE to the payload.
-----------------------------------------------------------------------------*/
void construct_server_reply (
        char *buf,                      // read/write pointer to payload
        int recv_len,                   // nbytes recv'd
        int buf_len,                    // allocated size of buf in bytes
        int *data_len,                  // _out, valid data len on return
        struct timeval *arrival_time )  // _in, already queried
{
    /* parse the info out of the packet we just received */

    int echo_seq_num=0;
    int echo_client_time_sec=0;
    int echo_client_time_usec=0;

    {
        int n;

        /* Warning: Caller must guarantee the buffer has a zero byte in it
           somewhere because we are using a sscanf() call below */

        n = sscanf(
                buf,
                "%d %d.%d",
                &echo_seq_num,
                &echo_client_time_sec,
                &echo_client_time_usec );

        if ( n != 3 ) {
            fprintf(stderr, "bad packet, no sequence number in it\n");
            return;
        }
    }

    /*
        send the RESPONSE
        The format of the payload is:
            echo_seq_num  echo_client_send_time  srvr_recv_time  srvr_send_time
    */

    zero_memory ( &udp_server.buf, BUFLEN );    // always zero bufffer on rewrite

    struct timeval reply_time = { 0, 0 };

    if ( gettimeofday(&reply_time, NULL) == -1 )
        print_warning("gettimeofday error");

    sprintf (
            buf,
            "%u %u.%06u %u.%06u %u.%06u",
            echo_seq_num,
            echo_client_time_sec,
            echo_client_time_usec,
            (unsigned int) arrival_time->tv_sec,
            (unsigned int) arrival_time->tv_usec,
            (unsigned int) reply_time.tv_sec,
            (unsigned int) reply_time.tv_usec);

    *data_len = strlen(buf) + 1;
}

/*---------------------------------------------------------------------------*/
static int open_socket()
{
    udp_server.socket = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);

    if ( udp_server.socket == -1 )
        exit_process("open_socket failed");
} 

/*---------------------------------------------------------------------------*/
void bind_socket()
{
    struct sockaddr_in local_addr;
    int r;

    zero_memory(&local_addr, sizeof(local_addr));

    local_addr.sin_family       = AF_INET;
    local_addr.sin_port         = htons(SERVER_PORT);
    local_addr.sin_addr.s_addr  = htonl(INADDR_ANY);

    r = bind ( udp_server.socket, (void *) &local_addr, sizeof(local_addr) );

    if ( r == -1 )
        exit_process("bind failed");
}

/*---------------------------------------------------------------------------*/
void server_listen_reply()
{
    struct sockaddr_in received_from;
    socklen_t received_from_length = sizeof(received_from); 
    struct timeval arrival_time;
    int r;
    int data_len = 0;

    zero_memory ( &received_from, sizeof(received_from) );
    zero_memory ( &arrival_time,  sizeof(arrival_time) );

    zero_memory ( &udp_server.buf, BUFLEN ); // erase stale data before recv()

    r = recvfrom (  udp_server.socket,
                    udp_server.buf,
                    BUFLEN - 1,             // -1 gaurantees a zero byte
                    0,
                    (void *) &received_from,
                    &received_from_length );

    /* get the arrival_time asap */

    if ( gettimeofday(&arrival_time, NULL) == -1 )
        print_warning("gettimeofday error");

    /* figure out a human readable name for the sender */

    char sender_name[INET_ADDRSTRLEN] = "";

    get_ip_str(
            (struct sockaddr*) &received_from,
            sender_name,
            sizeof(sender_name) );

    printf("LOG_server_recv[%s][%d][%s]\n", sender_name, r, udp_server.buf);

    if ( r == -1 ) {
        print_warning("recvfrom failed listening for a request from a client");
        return;
    }

    construct_server_reply(
            udp_server.buf,     // read/write pointer to payload
            r,                  // nbytes recv'd
            BUFLEN,             // allocated size of buf in bytes
            &data_len,          // _out, valid data len in buf on return
            &arrival_time );    // _in, already queried

    if ( data_len <= 0 ) {
        print_warning("data_len <= 0, programming error\n");
        return;
    }

    r = sendto ( udp_server.socket,
                 udp_server.buf,
                 data_len,
                 0,
                 (void *) &received_from,
                 sizeof(received_from) );

    if ( r == -1 )
        print_warning("sendto failed");

    printf("LOG_server_send[%s][%d][%s]\n", sender_name, r, udp_server.buf);
}

/*---------------------------------------------------------------------------*/
void run_udp_server()
{
    open_socket();
    bind_socket();

    for ( ;; ) {
        server_listen_reply();
        fflush(stdout);
        fflush(stderr);
    }

    close(udp_server.socket);
}

/*---------------------------------------------------------------------------*/
int main(int argc, char *argv[])
{
    ForkOutputToLog("server_");
    run_udp_server();
    CloseLog();
    return 0;
}
