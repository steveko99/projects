/*-----------------------------------------------------------------------------
    File: client.c

    Author: Steve Koscho - March 15, 2015

    Description: UDP client for time sync project.

    MainLoop:
        send request containing: (sequence_number, time_now)
        sleep 10 seconds
        check for response and process it

    ProcessResponse:
        Compute estimate for Routing Delay (Round-Trip-Time)
        Compute estimate for Clock Difference (Theta)
        Log computations (RTT, Theta)
        Smooth jittery samples
        AdjustClock

    Computations:
        These are the formulas for computing clock difference and routing
        delays as specified in the project.

        Estimated Round-Trip-Time, RTT =
                    (srv_rcv - cli_send) + (cli_rcv - srv_send)
                OR        delta1         +        delta2

        Estimated Clock Delta, Theta =
                    1/2 * ( (srv_rcv - cli_send) - (cli_rcv - srv_send) )
                OR  1/2 *       (  delta1        -        delta2  )

        In every case, you first compute these deltas ...
            receive_time_receivers_clock - send_time_senders_clock

    Smooth Jittery Sample Data:
        Remember the prior 8 theta estimates and choose the min among them.

-----------------------------------------------------------------------------*/

#include <arpa/inet.h>
#include <sys/socket.h>
#include <stdio.h>
#include <string.h>
#include <stdlib.h>
#include "applib.h"
#include "jitter_cache.h"

#define BUFLEN          512     // configure

#define REMOTE_IP       "::1"   // default or user input
#define REMOTE_PORT     9990    // default or user input

#define MILLION         1000000     // to convert sec => usec
#define BILLION         1000000000  // to convert sec => nsec

/*
    udp_client app global blob
    must be initialized to zeroes
*/

struct UDP_CLIENT
{
    int                     socket;
    char                    buf[BUFLEN];
    struct sockaddr_in      remote_addr;
    int                     seqNum;
    int                     portNum;
    char*                   remoteIP;
};

static struct UDP_CLIENT udp_client = { 0 };

/*-----------------------------------------------------------------------------
-----------------------------------------------------------------------------*/
static void open_socket()
{
    udp_client.socket = socket(AF_INET, SOCK_DGRAM, IPPROTO_UDP);

    if ( udp_client.socket == -1 )
        exit_process("open_socket failed");
} 

/*-----------------------------------------------------------------------------
    configures
        udp_client.remote_addr

    It will then exist as a blob of type 'struct sockaddr_in' that refers
    to REMOTE_IP, REMOTE_PORT
-----------------------------------------------------------------------------*/
static int configure_ip_addr()
{
    int r;

    zero_memory(&udp_client.remote_addr, sizeof(udp_client.remote_addr));

    udp_client.remote_addr.sin_family = AF_INET;
    udp_client.remote_addr.sin_port   = htons(udp_client.portNum);  // input

    r = inet_pton ( AF_INET,
                    udp_client.remoteIP,    // input
                    &udp_client.remote_addr.sin_addr);

    if ( r == -1 )
        exit_process("inet_pton() failed");
}

/*-----------------------------------------------------------------------------
    process_server_response

    This is the most interesting part of this app as it is where it computes
    the estimates for RTT and clock difference (theta).

    Note: This uses C Run-Time string functions (sscanf), do not pass in a
    buffer that has no zero byte in it somewhere.
-----------------------------------------------------------------------------*/
static void process_server_response (
                    char *response,
                    struct timeval *client_recv_tv )
{
    double client_send = 0.0;
    double server_recv = 0.0;
    double server_send = 0.0;
    double client_recv = 0.0;

    /* Parse the text out of response and convert */

    {
        int client_send_sec, client_send_usec,
            server_recv_sec, server_recv_usec,
            server_send_sec, server_send_usec;

        int n, seq_num;

        n = sscanf(
                response,
                "%u %u.%u %u.%u %u.%u",
                &seq_num,
                &client_send_sec, &client_send_usec,
                &server_recv_sec, &server_recv_usec,
                &server_send_sec, &server_send_usec );

        if ( n != 7 ) {
            fprintf(stderr, "bad packet on server response\n");
            return;
        }

        client_send =   (double) client_send_sec +
                        (double) client_send_usec / (double) MILLION;

        server_recv =   (double) server_recv_sec +
                        (double) server_recv_usec / (double) MILLION;

        server_send =   (double) server_send_sec +
                        (double) server_send_usec / (double) MILLION;

        client_recv =   (double) client_recv_tv->tv_sec +
                        (double) client_recv_tv->tv_usec / (double) MILLION;
    }

    /* compute the routing delay and the clock diff (theta) estimates */

    double delta1 = server_recv - client_send;
    double delta2 = client_recv - server_send;

    double rtt   = ( delta1 + delta2 );
    double theta = ( delta1 - delta2 );

    /* add new sample to the jitter cache for smoothing */

    double smoothed = 0.0;

    jitter_add(rtt, theta);
    if ( jitter_is_init())
        smoothed = jitter_get_min_theta();

    /*
        Log this line so it can be easily extracted
        cut -f defaults to tab delimeters so use them here
        just grep client_data and then cut -f 2 or cut -f 3
    */

    printf("client_data\t%.06f\t%.06f\t%.06f\n", rtt, theta, smoothed);
}

/*-----------------------------------------------------------------------------
-----------------------------------------------------------------------------*/
static void send_next_packet()
{
    int r;

    /* Build the REQUEST payload
       packet format is as follows: sequence_number time_seconds.time_usec */

    struct timeval tv;

    if ( gettimeofday(&tv, NULL) == -1 ) {
        print_warning("gettimeofday error");
        return;
    }

    zero_memory( udp_client.buf, BUFLEN );   // always zero out payload on reset

    sprintf (
            udp_client.buf,
            "%u %u.%06u",
            ++udp_client.seqNum,
            (unsigned int) tv.tv_sec,
            (unsigned int) tv.tv_usec );


    r = sendto ( udp_client.socket,
                 udp_client.buf,
                 strlen(udp_client.buf) + 1,
                 0,
                 (void *) &udp_client.remote_addr,
                 sizeof(udp_client.remote_addr) );

    if ( r == -1 ) {
        print_warning("sendto failed");
        return;
    }

    printf("client_send[%d][%s]\n", r, udp_client.buf);

    /*-------------------------------------------------------------------------
         Now wait for the reply from the server with 10 second timeout
    -------------------------------------------------------------------------*/
    fd_set rfds;

    FD_ZERO(&rfds);
    FD_SET(udp_client.socket, &rfds);

    struct timeval sleep_time;

    sleep_time.tv_sec = 10;
    sleep_time.tv_usec = 0;

    int s;
    s = select(udp_client.socket+1, &rfds, NULL, NULL, &sleep_time);

    if ( s <= 0 ) {
        printf("client_lost[-1][%d LOST_PACKET_NO_REPLY]\n", udp_client.seqNum);
        return;
    }

    zero_memory( udp_client.buf, BUFLEN );   // always zero out payload before recv()

    struct sockaddr_in received_from;
    socklen_t received_from_length = sizeof(received_from); 
    zero_memory( &received_from, sizeof(received_from) );

    struct timeval arrival_time;

    arrival_time.tv_sec = 0;                // zero out possible stale data
    arrival_time.tv_usec = 0;

    r = recvfrom (  udp_client.socket,
                    udp_client.buf,
                    BUFLEN-1,               // -1 garuntees a zero byte in the buffer
                    0,
                    (void *) &received_from,
                    &received_from_length );

                                            // query arrival time asap
    if ( gettimeofday(&arrival_time, NULL) == -1 ) {
        print_warning("gettimeofday error - something really wrong");
    }

    printf("client_recv[%d][%s]\n", r, udp_client.buf);
    process_server_response(udp_client.buf, &arrival_time);

    FD_CLR(udp_client.socket, &rfds);
}

/*-----------------------------------------------------------------------------
-----------------------------------------------------------------------------*/
static void send_packets()
{
    for ( ;; ) {

        struct timeval tnow;
        struct timeval tnext;

        if ( gettimeofday(&tnext, NULL) == -1 ) {
            print_warning("gettimeofday error - tnext not set");
        }

        tnext.tv_sec += 10;     // tnow + 10 seconds

        send_next_packet();

        fflush(stdout);
        fflush(stderr);

        if ( gettimeofday(&tnow, NULL) == -1 ) {
            print_warning("gettimeofday error - tnow not set");
        }

        /*
            let t2 be the time in the future (usually)
            let t1 be tnow
        */

        double t2 = (double) tnext.tv_sec +
                    (double) tnext.tv_usec / (double) MILLION;

        double t1   = (double) tnow.tv_sec +
                      (double) tnow.tv_usec / (double) MILLION;

        /*
            Now figure out the remaining time to get us to 10 second
            pauses and do the sleep
        */

        if ( t2 > t1 )
        {
            /* Note: using a struct timespec for the sleep */
            struct timespec sleep_ts;
            double delta = t2 - t1;
            int n = (int) delta;
            double f = delta - n;

            sleep_ts.tv_sec = n;
            sleep_ts.tv_nsec = f * BILLION;

            printf("sleeptime %u.%09u\n",
                    (unsigned int) sleep_ts.tv_sec,
                    (unsigned int) sleep_ts.tv_nsec);

            nanosleep(&sleep_ts, NULL);
        }
    }
}

/*-----------------------------------------------------------------------------
-----------------------------------------------------------------------------*/
static void run_udp_client()
{
    open_socket();
    configure_ip_addr();
    send_packets();
    close(udp_client.socket);
}

/*-----------------------------------------------------------------------------
-----------------------------------------------------------------------------*/
int main(int argc, char *argv[])
{
    udp_client.remoteIP = REMOTE_IP;
    udp_client.portNum = REMOTE_PORT;

    if ( argc > 1 ) {
        udp_client.remoteIP = argv[1];
    }

    printf( "Remote Server Address = %s:%d\n", 
            udp_client.remoteIP,
            udp_client.portNum );

    ForkOutputToLog("client_");
    run_udp_client();
    CloseLog();
    return 0;
}
