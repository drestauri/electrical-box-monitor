package home;

import gov.nasa.gsfc.gmsec.api.Message;
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
	public static final String DEVICE = "PI";
	public static final String LOCATION = "GARAGE";
	public static final String ROLE = "POWER-MONITOR";
	
	public static DataLogger log;
	public static GMSECConnection gmsec;
	private static GMSECSubscriber gSub;
	private static GMSECPublisher gPub;
	private static MsgFactory msgFact;
	//private static CustomCallback callback;
	
	private static boolean newAnalog0Data = false;
	private static boolean newAnalog1Data = false;
	private static boolean newAnalog2Data = false;
	private static boolean newAnalog3Data = false;
	private static boolean newLoopData = false;
	private static boolean newSampleData = false;
	private static int analog0Data = 0;
	private static int analog1Data = 0;
	private static int analog2Data = 0;
	private static int analog3Data = 0;
	private static int loopData = 0;
	private static int sampleData = 0;
	private static boolean isFinished = false;	
	
	public static void main(String[] args) throws Exception
	{
		log = new DataLogger();
		log.LogMessage_High("=============================");
		log.LogMessage_High("App started");
		
		//======== GET COMMAND LINE ARGUMENTS ==========
		log.LogMessage_Low("Getting command line args");
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
			System.out.println(" On raspberry pi, use 'ls /dev/*tty*' to list ports with your");
			System.out.println(" device plugged and then not to identify which name changes");
			System.out.println(" Example middleware IDs: bolt or activemq394");
			System.out.println(" Port is commonly 61616 for ActiveMQ and 9100 for Bolt");
			System.out.println("==========");
			System.exit(-1); // Comment this out during dev
		}
		
		
		//============== INITIALIZE =================
		log.LogMessage_Low("Initializing");
		// Setup GMSEC
		// Send the arguments to the connection class
		// GMSECConnection() also starts the connection to the message bus
		gmsec = new GMSECConnection(gmsec_args);
		
		// Create a new publisher object that can send messages.
		gPub = new GMSECPublisher(gmsec.getConnMgr());
		
		// Create a new message factory for generating messages
		msgFact = new MsgFactory();
		
		// Setup the serial port to Arduino
		Serial serial = new Serial(commPort);
		if(!serial.isConnected())
		{
			// Send GMSEC Message
			gPub.publish(msgFact.generateWarningMessage(DEVICE, LOCATION, ROLE, "Could not find desired COM port."));
		}
		
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
				// Handle Data
				if(tStr.contains("A:"))
				{
					// A = Analog Channel 0
					analog0Data = getInt(tStr);
					newAnalog0Data = true;
				}
				else if(tStr.contains("B:"))
				{
					// B = Analog Channel 1
					analog1Data = getInt(tStr);
					newAnalog1Data = true;
				}
				else if(tStr.contains("C:"))
				{
					// C = Analog Channel 2
					analog2Data = getInt(tStr);
					newAnalog2Data = true;
				}
				else if(tStr.contains("D:"))
				{
					// D = Analog Channel 3
					analog3Data = getInt(tStr);
					newAnalog3Data = true;
				}
				else if(tStr.contains("L:"))
				{
					// L = average loop time
					// 16.67 / L = how many samples per 60Hz cycle
					loopData = getInt(tStr);
					newLoopData = true;
				}
				else if(tStr.contains("S:"))
				{
					// S = sample period (how long we sample for when trying to find the max voltage)
					// Sample period / 16.67 = how many 60Hz cycles we sampled
					sampleData = getInt(tStr);
					newSampleData = true;
				}
			}
			
			// Checks for whether we need to send a GMSEC message:
			// If we have new samples from all the analog channels:
			if (newAnalog0Data && newAnalog1Data && newAnalog2Data && newAnalog3Data)
			{
				// Send the GMSEC Data Message
				gPub.publish(msgFact.generateDataMessage(DEVICE, LOCATION, ROLE, analog0Data, analog1Data, analog2Data, analog3Data));
				// Reset the flags
				newAnalog0Data = false;
				newAnalog1Data = false;
				newAnalog2Data = false;
				newAnalog3Data = false;
			}
			// If we have new loop and sample rate:
			if (newLoopData & newSampleData)
			{
				// Send the GMSEC Data Message
				gPub.publish(msgFact.generateStatusMessage(DEVICE, LOCATION, ROLE, loopData, sampleData));
				// Reset the flags
				newLoopData = false;
				newSampleData = false;
			}
		}
		
		
		log.LogMessage_High("Disconnecting from GMSEC bus");

		// Disconnect and cleanup the GMSEC connection
		gmsec.disconnect();

		return;
	}
	
	private static int getInt(String s)
	{
		// Strings come in the form A:9999 with any number of digits
		return Integer.parseInt(s.substring(2));
	}
}


