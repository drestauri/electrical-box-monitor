# electrical-box-monitor
Raspberry Pi receives data over serial from Arduino, processes, then publishes status messages via GMSEC

This software will be placed in a remote AC power consumption measuring device placed at 2 locations around my house.
It will consist of an Arduino monitoring four to six 60Hz AC circuits for power consumption and a Raspberry Pi
that will receive and process the data from the Arduino. This is the code running on the Raspberry Pi.

Since the data that can come across the serial interface is limited, we cannot sample four 60Hz channels very
efficiently. Instead, the Arduino samples as fast as possible and does some local pre-processing of the data prior
to sending relevant data over the serial interface. As we are primarily concerned with the power consumption, 
the Arduino samples voltage as quickly as possible for a few cycles and saves only the peak reading. The peak
is desireable because we are measuring a sinusoidal signal and want to know the amplitude, so we can calculate the
current being produced by the sensor and in turn the current/power consumption of the circuit in question.

To limit the number of characters sent across the serial interface, data from the Arduino is encoded in the
following format
```
A1024
```
The 1st character is a letter identifying the data being sent and the number starting from the second character
is the actual data point. The Arduino's analog data comes as a value from 0-1024, and other data may also be sent
if necessary such as the sample rate, device state of health, Arduino's software version, etc.

The Electrical Box Monitor code (this code) takes the messages received from the Arduino and calculates the average 
over 1 second. This data is stored for 60 seconds and then used to calculate and save the average consumption for 
the past minute, hour, day, month, and stores the history for up to 24 months. Every second, the data is also sent 
via a GMSEC message to a message broker (GMSEC Bolt) that is also running on this or another device. Another device
(such as a PC) can connect over wi-fi to the the message broker and receive those messages and process them for 
displaying them on a screen or sending email alerts, etc. 

IMPORTANT: The Arduino must send values for A, B, C, and D (data from analog channels 0-3) as well as S, L (status
information for sample rate and average loop time) in order for the data or status GMSEC message to be sent. If no
data or only partial data is received, it is not send. When this code receives all 4 values, it will add it to an
average running value and that average value gets sent every second so that we portray the power consumption measured
over 1 second as accurately as possible rather than just the max value for that one second. Modify this to fit your
application or for testing as you see fit.

This code can also receive commands over GMSEC to send strings of data for all other durations of time other than
the 1 second samples in case the other device would like to display or process longer term data.

# Installation
This code is provided as a complete software package with the exception of the GMSEC API components. The GMSEC 
API can be downloaded from the links at the bottom of the page below for PC. Until I get permission from NASA
to post the ARM build I have for Raspberry Pi, you will need to contact them to get that build. They are generally
pretty good to respond to support requests from the GMSEC community:
```
https://opensource.gsfc.nasa.gov/projects/GMSEC_API_30/index.php
```
Once downloaded, extract the files and add a GMSEC_HOME environment variable to the folder containing the /bin
folder, and then add %GMSEC_HOME%/bin to your PATH variable (Windows). See the Usage section below for Linux.

Launch bolt by running the following command:
```
java -jar %GMSEC_HOME%\bin\bolt.jar
```

The project should already be set to connect to a locally hosted Bolt message broker, but if not update App_EBM.java
as follows to set the default values for gmsec_args. Also, connect your Arduino and make sure the default COM port
matches the port for your Arduino:
```java
String commPort = "COM3";
String gmsec_args[] = {"subscribe", "mw-id=bolt", "server=localhost:9100"}; 
```

Run the project and make sure it connects to the Arduino and message broker. Once everything works on the PC, export 
the project as a Runnable JAR file. Transfer the JAR file to your Raspberry Pi to a folder like
/home/pi/Desktop/project. 

Assuming you've obtained the ARM build of the GMSEC API, copy the files to the Raspberry Pi as well to a location
such as /home/pi/Desktop/GMSEC_API. 

# Usage
Now that all the files are on your Linux system/Raspberry Pi, launch the GMSEC Bolt message broker using the following command:
```
sudo java -jar /home/pi/Desktop/GMSEC_API/bin/bolt.jar
```
Find out which port your Arduino is on, by running the following command without your Aruino plugged in and then
again when it is, and note what changed:
```
ls /dev/*tty*
```
Launch this application using the following command, which includes linking the GMSEC libraries similar to what
was required for the environment variables used in Windows, assuming your Arduino is on USB port ttyACM0:
```
sudo java -jar -Djava.library.path=/home/pi/Desktop/GMSEC_API/bin/ /home/pi/Desktop/project/electrical-box-monitor.jar ttyACM0 subscribe mw-id=bolt server=localhost:9100
```
If all goes well, you should see the electrical-box-monitor.jar code writing data and status messages over GMSEC once every second.

# Anticipated Updates
1. Receive GMSEC commands to change it's configuration (TBD)
2. Implement better error handling for data that doesn't exist or is incorrectly spelled
3. Potentially will make the DataLogger more generic code to encourage reuse of that
4. Update code to send last Data point and Status every second regardless of whether new data is available

# Contributing
This is for a home project and while you're free to copy and modify to your liking, I will not be accepting contributions.