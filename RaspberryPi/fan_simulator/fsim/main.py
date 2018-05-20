#------------------------------------------------------------------------------
# main.py - Fan simulator - v2
#
# Use simple REST API to turn the fan animation on and off, and change speed.
# It will show a moving fan the SenseHat and a temperature guage.
# Runs on Pi with a SenseHat, uses the LED Matrix.
#
# Author: Steve Koscho - Feb 17, 2018
#------------------------------------------------------------------------------

from fan_thread import FanThread
fan_thread = FanThread()

from flask import Flask, jsonify, render_template
app = Flask(__name__)

USAGE = ( "API: "
          "curl http://IP:5000/fan/off " 
          "curl http://IP:5000/fan/on " 
          "curl http://IP:5000/fan/speed/<int> "
          "curl http://IP:5000/temp/<int> " )

# curl http://127.0.0.1:5000/fan/on
# curl http://127.0.0.1:5000/fan/off
# curl http://127.0.0.1:5000/fan/speed/<int>
# curl http://127.0.0.1:5000/temp<int>

@app.route('/')
def hello():
    return render_template( 'index.html',
                             title='Test page for Fan Animation' )

@app.route('/fan/on')
def fan_on():
    fan_thread.animation_on()
    return 'Turned FAN animation ON'

@app.route('/fan/off')
def fan_off():
    fan_thread.animation_off()
    return 'Turned FAN animation OFF'

# works as long as you pass a digit
@app.route('/fan/speed/<int:speed>')
def fan_speed(speed):
    fan_thread.set_fan_speed(speed)
    return 'Changed FAN speed'

@app.route('/temp/<int:t>')
def temp(t):
    fan_thread.set_temperature(t)
    return 'Changed temperature reading'

@app.errorhandler(404)
def not_found(error=None):
    message = {
            'status': 404,
            'message': USAGE,
    }
    resp = jsonify(message)
    resp.status_code = 404
    return resp

# Note: below code is not guaranteed to run because Flask app can be
# started under flask instead
if __name__ == '__main__':
    print(USAGE)
    app.debug = True
    app.run(host='0.0.0.0')

