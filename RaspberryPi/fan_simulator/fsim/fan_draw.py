#------------------------------------------------------------------------------
# fan_draw.py
#
# Implements a class that can draw the fan images on the Pi SenseHat.
# It also draws the thermometer (temp_guage) on the SenseHat.
# The LED Matrix on the SenseHat is an 8X8 bitmap.
# The fan simulation is 7X7 and the temp_guage is drawn in the margin.
#
# Usage: use FanDraw class methods
#
# Example:
#
# from fan_draw import FanDraw
#
# _draw = FanDraw()
# _draw.render_fan1()
# _draw.render_fan2()
# _draw.set_temperature(t)      #0..15, 0 means no reading at all
# _draw.get_max_temperature()   #returns 15 for num pixels avail for guage
#
# Author:
# Steve Koscho - Feb 17,2018
#------------------------------------------------------------------------------

import time
from sense_hat import SenseHat

_MAX_TEMPERATURE = 15    #in pixels 1..15, caller scales, 0 is none

_MROWS, _NCOLS = (8, 8)

# Each matrix is stored as a linear list
# But it is used like a 2-dimensional array in the code

_r = [255, 0, 0]         #red
_w = [255, 255, 255]     #white
_y = [255, 255, 0]       #yellow
_o = [255, 127, 0]       #orange
_b = [0, 0, 0]           #black
_t = [-1, -1, -1]        #transparent

_RED = _r
_WHITE = _w
_YELLOW = _y
_ORANGE = _o
_BLACK = _b
_TRANSPARENT = _t

# Fan position 1
_FAN_BMP1 = [
    _b, _b, _b, _r, _b, _b, _b, _t, 
    _b, _b, _b, _r, _b, _b, _b, _t, 
    _b, _b, _b, _r, _b, _b, _b, _t, 
    _r, _r, _r, _r, _r, _r, _r, _t, 
    _b, _b, _b, _r, _b, _b, _b, _t, 
    _b, _b, _b, _r, _b, _b, _b, _t, 
    _b, _b, _b, _r, _b, _b, _b, _t, 
    _t, _t, _t, _t, _t, _t, _t, _t]

# Fan position 2
_FAN_BMP2 = [
    _r, _b, _b, _b, _b, _b, _r, _t, 
    _b, _r, _b, _b, _b, _r, _b, _t, 
    _b, _b, _r, _b, _r, _b, _b, _t, 
    _b, _b, _b, _r, _b, _b, _b, _t, 
    _b, _b, _r, _b, _r, _b, _b, _t, 
    _b, _r, _b, _b, _b, _r, _b, _t, 
    _r, _b, _b, _b, _b, _b, _r, _t, 
    _t, _t, _t, _t, _t, _t, _t, _t]

_TEMP_GUAGE_BMP = [
    _t, _t, _t, _t, _t, _t, _t, _b,
    _t, _t, _t, _t, _t, _t, _t, _b,
    _t, _t, _t, _t, _t, _t, _t, _b,
    _t, _t, _t, _t, _t, _t, _t, _b,
    _t, _t, _t, _t, _t, _t, _t, _b,
    _t, _t, _t, _t, _t, _t, _t, _b,
    _t, _t, _t, _t, _t, _t, _t, _b,
    _b, _b, _b, _b, _b, _b, _b, _b]

# temperature of 0 means off
# temperature valid range is [1..15] (there are 15 pixels available)
# e.g. if temperature == 1, then 1 light is on 
_THERMOMETER_MAP = [
    (7,0),(7,1),(7,2),(7,3),(7,4),(7,5),(7,6),(7,7),
    (6,7),(5,7),(4,7),(3,7),(2,7),(1,7),(0,7)]

_sense = SenseHat()

# mutable globals _bmp and _current_temperature shared among all threads
# must declare them 'global' in functions that write to them

_bmp = [ [0,0,0] for i in range(_MROWS*_NCOLS) ]
_current_temperature = 0

def _get_temp_guage_color(m, n):
    if _current_temperature == 0:
        return _BLACK
    for i in range(len(_THERMOMETER_MAP)):
        if (m,n) == _THERMOMETER_MAP[i]:
            if i + 1 > _current_temperature:
                return _BLACK
            else:
                if i < 5:
                    return _YELLOW
                elif i < 10:
                    return _ORANGE
                elif i < 15:
                    return _RED

def _render_fan(mt):
    global _bmp
    for mm in range(_MROWS):
        for nn in range(_NCOLS*mm, _NCOLS*(mm+1)):
            if mt[nn] != _TRANSPARENT:
                _bmp[nn] = mt[nn]

def _render_temp_guage():
    global _bmp
    i = 0
    for mm in range(_MROWS):
        for nn in range(_NCOLS*mm, _NCOLS*(mm+1)):
            if _TEMP_GUAGE_BMP[nn] != _TRANSPARENT:
                _bmp[nn] = _get_temp_guage_color(mm, nn % _NCOLS)

def _print_bmp():
    _sense.set_pixels(_bmp)

def _clear_bmp():
    for mm in range(_MROWS):
        for nn in range(_NCOLS*mm, _NCOLS*(mm+1)):
            _bmp[nn] = _BLACK

# -------------------------------------------------------------------------
# Public interface class and methods
# Clients of this module should import FanDraw only
# -------------------------------------------------------------------------
class FanDraw:
    def __init__(self):
        pass

    def clear(self):
        _sense.clear()
        _clear_bmp()

    def render_fan1(self):
        _render_temp_guage()
        _render_fan(_FAN_BMP1)
        _print_bmp()

    def render_fan2(self):
        _render_temp_guage()
        _render_fan(_FAN_BMP2)
        _print_bmp()

    def set_temperature(self, t):
        global _current_temperature
        _current_temperature = t
        _render_temp_guage()
        _print_bmp()

    def get_max_temperature(self):
        return _MAX_TEMPERATURE

# testcase if started as main
def _run_tests():
    d = FanDraw()
    def test01():
        t = 0
        tmax = d.get_max_temperature()
        for i in range(tmax+1):
            print("TEST LOOP TOP")
            d.set_temperature(t)
            if t % 2 == 0:
                d.render_fan1()
            else:
                d.render_fan2()
            time.sleep(2)
            t += 1
            if t > tmax:
                t = 0
        d.clear();
    test01()

if __name__ == '__main__':
    _run_tests()

