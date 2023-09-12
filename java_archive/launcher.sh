#Launch the app
sleep 20
lxterminal -e sudo java -jar -Djava.library.path=/home/pi/Desktop/opt/GMSEC_API-4.4.2/bin/ /home/pi/Desktop/electrical-monitor/electrical-box-monitor.jar ttyACM0 subscribe mw-id=bolt server=localhost:9100