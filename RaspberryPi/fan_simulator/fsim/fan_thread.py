#------------------------------------------------------------------------------
# fan_thread.py
#
# Implements spawning and coordinating with a thread that runs the fan
# animation.
#
# Usage: import and use the FanThread class methods
#
# Example:
#
# from fan_thread import FanThread
#
# _fan_thread = FanThread()
# _fan_thread.animation_on()
# _fan_thread.animation_off()
# _fan_thread.set_fan_speed(s)
# _fan_thread.set_temperature(t)
# _fan_thread.stop_thread()
#
# Author: Steve Koscho - Feb 17, 2018
# Update: Steve Koscho - May 19, 2018
#         - refactor and cleanup everything
#         - non-confusing methods
#         - exits thread clean and clears screen
#------------------------------------------------------------------------------

import time
import threading
from fan_draw import FanDraw

_fan_draw = FanDraw()
_animation_on = False
_animation_thread = None
_thread_stop_request = False

# The fan animation is done with 2 bitmaps (see FanDraw.py)
# One is like a plus sign, the other like a X
# There are 8 moves +X+X+X+X to make 1 full revolution 

DEFAULT_FAN_SPEED_RPM = 60
MINUTES_PER_HOUR = 60

_fan_speed = DEFAULT_FAN_SPEED_RPM

def _set_fan_speed(speed):
    global _fan_speed
    if speed > 0:
        _fan_speed = speed

def _get_current_fan_delay():
    global _fan_speed
    return float(MINUTES_PER_HOUR) / float(_fan_speed) / float(8)

#------------------------------------------------------------------------------
# _do_fan_animation(): Entry-point of the spawned child thread
# Paint +X+X+X+X... and stop if caller asked for the fan to stop
# This function and whatever it calls runs on the child thread
#------------------------------------------------------------------------------
def _do_fan_animation():
    while not _thread_stop_request:
        if _animation_on:
            _fan_draw.render_fan1()
        time.sleep(_get_current_fan_delay())
        if not _thread_stop_request and _animation_on:
            if _animation_on:
                _fan_draw.render_fan2()
            time.sleep(_get_current_fan_delay())
    _fan_draw.clear()

#------------------------------------------------------------------------------
# Primitives for controlling the fan animation thread
# These functions are internal to the implementation of this module
# These functions run on the main thread
#------------------------------------------------------------------------------
def _start_animation_thread():
    global _animation_thread
    global _thread_stop_request
    _thread_stop_request = False
    # Singleton create of the child thread
    if not _animation_thread:
        _animation_thread = threading.Thread(target=_do_fan_animation, args=())
        _animation_thread.start()

# This merely sets a flag that asks the child thread to exit gracefully
# when it naturally checks the next time. It does not interrupt or wake-up
# the child thread.
def _stop_animation_thread():
    global _thread_stop_request
    _thread_stop_request = True

# After asking the child thread to exit, wait for it to do so
def _wait_for_animation_thread_to_exit():
    global _animation_thread
    if _animation_thread:
        _animation_thread.join()
        _animation_thread = None

def _turn_animation_on():
    global _animation_on
    _animation_on = True

def _turn_animation_off():
    global _animation_on
    _animation_on = False
    _fan_draw.clear()

def _set_temperature(t):
    _fan_draw.set_temperature(t)

# -----------------------------------------------------------------------------
# Class for the public interface to this module
# Client of this module should import and use this class only
# -----------------------------------------------------------------------------
class FanThread:
    def __init__(self):
        pass

    def animation_on(self):
        _start_animation_thread()
        _turn_animation_on()

    def animation_off(self):
        _turn_animation_off()

    def set_fan_speed(self, s):
        _set_fan_speed(s)

    def set_temperature(self, t):
        _set_temperature(t)

    def stop_thread(self):
        _stop_animation_thread()
        _wait_for_animation_thread_to_exit()

#------------------------------------------------------------------------------
# Unit tests and example for usage of class FanThread
# Runs only if started as a main python program
#------------------------------------------------------------------------------
def _run_tests():
    #import pdb; pdb.set_trace()    # uncomment to break into PDB
    import traceback
    print("_run_tests(): Start Unit test")

    # test the methods of FanThread() class 
    fan_thread = FanThread()

    # test01:
    # cycle through all valid temperature settings 0..15 inclusive
    # increase the fan speed as the temperature increases
    # alternate between fan spinning and not
    def test01():
        for i in range(16):
            time.sleep(2)
            fan_thread.set_temperature(i)
            fan_thread.set_fan_speed(60 + 5*i)
            if i % 2 == 0:
                fan_thread.animation_on()
            else:
                fan_thread.animation_off()
        time.sleep(2)   #give fan thread a little time to repaint before exit

    # If user presses CTRL-C in the middle of the test then the main thread
    # exits but then there is no good way to kill the child thread.  So catch
    # all exceptions at the top so we can ask the child thread to exit and wait
    # for it to do so.
    try:
        test01()
    except Exception as e:
        print(traceback.format_exc())
    finally:
        print("_run_tests(): Waiting for the animation thread to exit...")
        fan_thread.stop_thread()

if __name__ == "__main__":
    _run_tests()
