----
This app is part of my capstone project for University of Washington Internet of Things night class
NOTE: This app requires a Raspberry Pi with a 'SenseHat'

-- START SERVER on the Raspberry Pi
    On the RaspberryPi run:
    $ python main.py

-- START CLIENT on any computer, localhost or on the LAN
   find correct IP, port=5000

   From a browser navigate to: http://<IP>:5000
   You should see a web-page called 'Test page for Fan Animation'

---- FILES ----
main.py         - Defines the REST API to control the animation
fan_thread.py   - FanThread class has methods to control animatiion thread
fan_draw.py     - FanDraw class has methods to draw the fan and temperature guage

---- TESTS - requires a Raspberry Pi and a SenseHat ----
main.py         - see unittest folder, starts localhost and tests API with curl and script
fan_thread.py   - python fan_thread.py will run a unittest
fan_draw.py     - python fan_draw.py will run a unittest

----
Author: Steve Koscho - May 20, 2018
----

