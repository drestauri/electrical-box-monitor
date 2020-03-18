package home;

import home.gmsec.GMSECConnection;
import home.gmsec.GMSECPublisher;
import home.gmsec.GMSECSubscriber;
import home.gmsec.MsgFactory;
import home.utils.DataLogger;
import home.utils.Serial;

/************* NOTES *****************
 * Expected data from Arduino:
 * 	A1024 = Analog Channel 1, Values: 0-1024
 *  B1024 = Analog Channel 2, Values: 0-1024
 *  C1024 = Analog Channel 3, Values: 0-1024
 *  D1024 = Analog Channel 4, Values: 0-1024
 *  Z99 = Heartbeat, Values: 0-99, 
 *  	- echos this app's Heartbeat value to ensure sync
 *  	- 0 from RPi means stop sending data (let serial buffer clear and resync)
 *
 *
 */

public class App {
	
	public static final String EXAMPLE_SUBSCRIPTION_SUBJECT =  "GMSEC.TEST.PUBLISH";
	
	public static DataLogger log;
	public static GMSECConnection gmsec;
	private static GMSECSubscriber gSub;
	private static GMSECPublisher gPub;
	private static MsgFactory msgFact;
	//private static CustomCallback callback;
	
	private static boolean isFinished = false;	
	
	public static void main(String[] args) throws Exception
	{
		log = new DataLogger();
		log.LogMessage_High("=============================");
		log.LogMessage_High("App started");
		log.LogMessage_Low("Getting command line args");
		
		//======== GET COMMAND LINE ARGUMENTS ==========
		String commPort = "COM5";
		String gmsec_args[] = {"subscribe", "mw-id=bolt", "server=localhost:9100"};
		
		if (args.length == 1)
		{
			// Only the serial comm port was provided (rest are default)
			commPort = args[0];
		}
		else if (args.length == 4)
		{
			// Serial comm port, subscribe, middleware ID, and middleware location were provided 
			commPort = args[0];
			gmsec_args[0] = args[1];
			gmsec_args[1] = args[2];
			gmsec_args[2] = args[3];
		}else
		{
			System.out.println("usage: java -jar serial-over-usb.jar <SERIAL_COMM_PORT> subscribe mw-id=<middleware> server=<ip_address>:<port>\n");
			System.out.println("\n== NOTE ==");
			System.out.println(" On raspberry pi, use 'ls /dev/*tty*' to list ports with");
			System.out.println(" your device plugged and then not to identify which name changes");
			System.out.println(" Example middleware IDs:");
			System.out.println(" bolt:");
			System.out.println(" activemq394");
			System.out.println(" Port 61616 for ActiveMQ and 9100 for Bolt");
			//System.exit(-1); // Comment this out during dev
		}
		
		log.LogMessage_Low("Initializing");
		//============== INITIALIZE =================		
		// Setup the serial port to Arduino
		Serial serial = new Serial(commPort);
		
		// Setup GMSEC
		// Send the arguments to the connection class
		// GMSECConnection() also starts the connection to the message bus
		gmsec = new GMSECConnection(gmsec_args);
		
		// Create a new publisher object that can send messages.
		gPub = new GMSECPublisher(gmsec.getConnMgr());
		
		
		log.LogMessage_Low("Starting main loop. Note: Ignore any immediate warnings of losing data as the serial buffer is cleared");
		//================ LOOP =====================
		String tStr = "";
		while(!isFinished)
		{
			try {
				Thread.sleep(0,0);
			} catch (InterruptedException e) {
				log.LogMessage_High("isFinished error!");
				e.printStackTrace();
			}
			
			if(serial.isDataAvail())
			{
				tStr = serial.getLastData();
				if(tStr.contains("A:"))
					System.out.println(tStr);
			}
		}
		
		
		log.LogMessage_High("Disconnecting from GMSEC bus");

		// Disconnect and cleanup the GMSEC connection
		gmsec.disconnect();

		return;
	}
}


