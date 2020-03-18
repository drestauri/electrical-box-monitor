# electrical-box-monitor
Raspberry Pi receives data over serial from Arduino, processes, then publishes status messages via GMSEC

This software will be placed in a remote AC power consumption device placed at 2 locations around my house.
It will consist of an Arduino monitoring 4 or so 60Hz AC circuits for power consumption and a Raspberry Pi
that will receive and process the data from the Arduino. This is the code running on the Raspberry Pi.

Since the data that can come across the serial interface is limited, we cannot sample four 60Hz channels very
efficiently. Instead, the Arduino will likely sample fast and do some local pre-processing of the data prior to 
sending relevant data over the serial interface. As we are primarily concerned with the power consumption, this
code will likely receive the peak of the AC voltage of the sensor from the Arduino. If we know that, we 
can calculate the measured current draw and in turn the power consumption of the circuit in question.

To limit the number of characters sent across the serial interface, data from the Arduino will come encoded
in the following format
```
A1024
```
The 1st character is a letter identifying the data being sent and the number starting from the second character
is the actual data point. The Arduino's analog data comes a value from 0-1024, and other data may also be sent
if necessary such as the sample rate, device state of health, etc.

This software will take the messages received from the Arduino and use them to populate a GMSEC message that is
sent over wifi to a message queue running another device on the network. A master device will in turn receive
and handle those messages such as display them on a screen or send email alerts, etc. This code may also
receive commands over GMSEC and change its configuration in real time.

# Installation
TBD

# Usage
TBD
```java
tbd
```

# Anticipated Updates
1. Receive GMSEC commands to change it's configuration (TBD)

# Contributing
TBD