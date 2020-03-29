package home;

import java.util.Calendar;

/******* TO DO ************
 * Subject Template
 * GMSEC.<SOURCE-DEVICE>.<SOURCE-ROLE>.<SOURCE-LOCATION>.<TYPE>.<SUBTYPE>.{DEST-DEVICE}.{DEST-ROLE}.{DEST-LOCATION}
 * Implement auto launch of this code upon boot
 * Implement repeated attempts to connect to message queue
 * 	> Try to do in code, but otherwise may need to auto reboot device
 * Consider hosting message queue on this device
 */

import home.gmsec.GMSECConnection;
import home.gmsec.GMSECPublisher;
import home.gmsec.GMSECSubscriber;
//import home.gmsec.GMSECSubscriber;
import home.gmsec.MsgFactory;
import home.gmsec.callbacks.DataRequestCB;
import home.utils.DataLogger;
import home.utils.Logger;
import home.utils.Serial;

/************* NOTES *****************
 * Expected data from Arduino:
 *  A1024 = Analog Channel 1, Values: 0-1024
 *  B1024 = Analog Channel 2, Values: 0-1024
 *  C1024 = Analog Channel 3, Values: 0-1024
 *  D1024 = Analog Channel 4, Values: 0-1024
 *  L999 = Average loop time
 *  S999 = Sample period before analog channel max is sent
 *  TBD:
 *  	Z99 = Heartbeat, Values: 0-99, 
 *  	- echos this app's Heartbeat value to ensure sync
 *  	- 0 from RPi means stop sending data (let serial buffer clear and resync)
 *
 *
 */

public class App {
	
	public static final String DEVICE = "PI";
	public static final String LOCATION = "GARAGE";
	public static final String ROLE = "EBM";
	//private static final int DATA_AVERAGING = 11; // 1000 / 85 = 11.7 so sample 11 times to make sure we fill the array within 1s
	private static final int LOW_POWER_OHM = 100;
	private static final int HIGH_POWER_OHM = 64;
	private static final int CURRENT_RATIO = 550;

	//GMSEC.<SOURCE-DEVICE>.<SOURCE-ROLE>.<SOURCE-LOCATION>.<TYPE>.<SUBTYPE>.{DEST-DEVICE}.{DEST-ROLE}.{DEST-LOCATION}
	public static final String TOPIC_DATA_REQ = "GMSEC.*.*.*.DATA.REQ.PI.EBM.GARAGE";
	
	public static Logger log;
	private static DataLogger dataLog;
	public static GMSECConnection gmsec;
	private static GMSECSubscriber gSub;
	private static GMSECPublisher gPub;
	private static MsgFactory msgFact;
	private static DataRequestCB dataReqCB;
	
	private static boolean newAnalog0Data = false;
	private static boolean newAnalog1Data = false;
	private static boolean newAnalog2Data = false;
	private static boolean newAnalog3Data = false;
	private static boolean newLoopData = false;
	private static boolean newSampleData = false;
	private static short analog0Data = 0;
	private static short analog1Data = 0;
	private static short analog2Data = 0;
	private static short analog3Data = 0;
	private static short loopData = 0;
	private static short sampleData = 0;
	private static boolean isFinished = false;	
	
	private static short index = 0;
	//private static short[] analog0 = new short[DATA_AVERAGING];
	//private static short[] analog1 = new short[DATA_AVERAGING];
	//private static short[] analog2 = new short[DATA_AVERAGING];
	//private static short[] analog3 = new short[DATA_AVERAGING];
	private static int lastSecond;
	private static short[] dataGarageMainSeconds = new short[60];
	private static short[] dataGaragePlugsSeconds = new short[60];
	private static short[] dataLaundrySeconds = new short[60];
	private static short dataGarageMainAvg = 0; // for averaging the data every second
	private static short dataGaragePlugsAvg = 0; // for averaging the data every second
	private static short dataLaundryAvg = 0; // for averaging the data every second
	private static short dataAvgCount = 1; // for averaging calculation
	
	public static void main(String[] args) throws Exception
	{
		log = new Logger();
		log.LogMessage_High("=============================");
		log.LogMessage_High("App started");
		
		//======== GET COMMAND LINE ARGUMENTS ==========
		log.LogMessage_Low("Getting command line args");
		String commPort = "COM3";
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
			//System.exit(-1); // Comment this out during dev
		}
		

		
		//============== INITIALIZE =================
		log.LogMessage_Low("Initializing");
		/********************************
		 * DATA FILE
		 ********************************/
		dataLog = new DataLogger();
		dataLog.setFile("data.properties");
		dataLog.loadData();
		lastSecond = Calendar.getInstance().get(Calendar.SECOND);
		
		//============== Setup GMSEC ================
		// Send the arguments to the connection class
		// GMSECConnection() also starts the connection to the message bus
		gmsec = new GMSECConnection(gmsec_args);
		
		// Create a new publisher object that can send messages.
		gPub = new GMSECPublisher(gmsec.getConnMgr());
		
		dataReqCB = new DataRequestCB();
		gSub = new GMSECSubscriber(gmsec.getConnMgr());
		gSub.subscribe(TOPIC_DATA_REQ, dataReqCB);
		
		// Create a new message factory for generating messages
		msgFact = new MsgFactory();
		
		// Setup the serial port to Arduino
		Serial serial = new Serial(commPort);
		if(!serial.isConnected())
		{
			// Send GMSEC Message
			gPub.publish(msgFact.generateWarningMessage(DEVICE, ROLE, LOCATION, "Could not find desired COM port."));
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
			
			// Check if there's data available and if so, store it in the
			// appropriate local variable and set the flag that data is available.
			// Data will come in unpredictable chunks and is stored in a buffer of recent values
			if(serial.isDataAvail())
			{
				tStr = serial.getLastData();
				// Handle Data
				if(tStr.contains("A"))
				{
					// A = Analog Channel 0
					analog0Data = getShort(tStr);
					newAnalog0Data = true;
				}
				else if(tStr.contains("B"))
				{
					// B = Analog Channel 1
					analog1Data = getShort(tStr);
					newAnalog1Data = true;
				}
				else if(tStr.contains("C"))
				{
					// C = Analog Channel 2
					analog2Data = getShort(tStr);
					newAnalog2Data = true;
				}
				else if(tStr.contains("D"))
				{
					// D = Analog Channel 3
					analog3Data = getShort(tStr);
					newAnalog3Data = true;
				}
				else if(tStr.contains("L"))
				{
					// L = average loop time
					// 16.67 / L = how many samples per 60Hz cycle
					loopData = getShort(tStr);
					newLoopData = true;
				}
				else if(tStr.contains("S"))
				{
					// S = sample period (how long we sample for when trying to find the max voltage)
					// Sample period / 16.67 = how many 60Hz cycles we sampled
					sampleData = getShort(tStr);
					newSampleData = true;
				}
			}
			
			// Checks for whether we need to send a GMSEC message:
			// If we have new samples from all the analog channels:
			if (newAnalog0Data && newAnalog1Data && newAnalog2Data && newAnalog3Data)
			{
				setValues();
			}
			// If we have new loop and sample rate. The Arduino only sends this every second so no need to limit the GMSEC msg rate
			if (newLoopData & newSampleData)
			{
				// Send the GMSEC Data Message
				gPub.publish(msgFact.generateStatusMessage(DEVICE, ROLE, LOCATION, loopData, sampleData));
				// Reset the flags
				newLoopData = false;
				newSampleData = false;
				System.out.println("Sending status");
			}
		}
		
		
		log.LogMessage_High("Disconnecting from GMSEC bus");

		// Disconnect and cleanup the GMSEC connection
		gmsec.disconnect();

		return;
	}
	
	private static short getShort(String s)
	{
		// If we have at least 2 characters
		if(s.length()>1)
		{
			// check the 2nd through the end to make sure they're numbers
			for(int i=1; i<s.length(); i++)
				if(s.charAt(i)<'0'||s.charAt(i)>'9')
					return 0;
			return Short.parseShort(s.substring(1));
		}
		return 0;
	}
	
	private static void setValues()
	{
		// Every 85ms or so, we should receive the values from Arduino and call this function
		
		// Reset the flags
		newAnalog0Data = false;
		newAnalog1Data = false;
		newAnalog2Data = false;
		newAnalog3Data = false;
		
		/*
		// Store the latest values in recent data history for averaging
		analog0[index] = analog0Data;
		analog1[index] = analog1Data;
		analog2[index] = analog2Data;
		analog3[index] = analog3Data;
		index++;
		
		if (index>=DATA_AVERAGING)
			index = 0;		
		
		// Find the max data point in the last 11 analog data points
		short a0MAX = 0;
		for(int i=0; i<DATA_AVERAGING; i++)
			a0MAX = a0MAX < analog0[i] ? analog0[i] : a0MAX;

		short a1MAX = 0;
		for(int i=0; i<DATA_AVERAGING; i++)
			a1MAX = a1MAX < analog1[i] ? analog1[i] : a1MAX;
			
		short a2MAX = 0;
		for(int i=0; i<DATA_AVERAGING; i++)
			a2MAX = a2MAX < analog2[i] ? analog2[i] : a2MAX;
			
		short a3MAX = 0;
		for(int i=0; i<DATA_AVERAGING; i++)
			a3MAX = a3MAX < analog3[i] ? analog3[i] : a3MAX;
		
		// Normalize the data point around 512 and if it's small or negative, just set it to 0 
		a0MAX -= 512;
		if(a0MAX < 3)
			a0MAX = 0;
		a1MAX -= 512;
		if(a1MAX < 3)
			a1MAX = 0;
		a2MAX -= 512;
		if(a2MAX < 3)
			a2MAX = 0;
		a3MAX -= 512;
		if(a3MAX < 3)
			a3MAX = 0;
			*/
		
		// Normalize the data point around 512 and if it's small or negative, just set it to 0 
		analog0Data -= 512;
		if(analog0Data < 0)
			analog0Data = 0;
		analog1Data -= 512;
		if(analog1Data < 0)
			analog1Data = 0;
		analog2Data -= 512;
		if(analog2Data < 0)
			analog2Data = 0;
		analog3Data -= 512;
		if(analog3Data < 0)
			analog3Data = 0;
		
		//Continuously average the data over 1 full second (reduces the weight of the old value at the beginning of a new second)
		dataGaragePlugsAvg = (short) ((dataGaragePlugsAvg * dataAvgCount + analog0Data)/(dataAvgCount+1));
		dataGarageMainAvg = (short) ((dataGarageMainAvg * dataAvgCount + analog1Data + analog3Data)/(dataAvgCount+1));
		dataLaundryAvg = (short) ((dataLaundryAvg * dataAvgCount + analog2Data)/(dataAvgCount+1));
		dataAvgCount++;		
		
		// Check if a second or more has passed
		int now = Calendar.getInstance().get(Calendar.SECOND);
		if(now != lastSecond)
		{
			boolean needSave = false;
			if(lastSecond > now)
				needSave = true;
			// Store the data point in every slot up until "now" (compensates for freeze with the average)
			int sec = lastSecond;
			short gmAvg = convertToWatts(dataGarageMainAvg,LOW_POWER_OHM);
			short gpAvg = convertToWatts(dataGaragePlugsAvg,LOW_POWER_OHM);
			short lAvg = convertToWatts(dataLaundryAvg,LOW_POWER_OHM);
			while(lastSecond!=now)
			{
				// Save here so that during the last loop, sec isn't updated. This way, sec is always the second before now
				sec = lastSecond;
				dataGarageMainSeconds[lastSecond] = gmAvg;
				dataGaragePlugsSeconds[lastSecond] = gpAvg;
				dataLaundrySeconds[lastSecond] = lAvg;
				lastSecond++;
				if(lastSecond>=60)
					lastSecond=0;
			}
			
			// EVERY SECOND PUBLISH YOUR BASIC DATA MESSAGE
			// Send the GMSEC Data Message
			gPub.publish(msgFact.generateSingleDataMessage(DEVICE, ROLE, LOCATION, sec, gmAvg, gpAvg, lAvg));
			// If the last second we measured was at the end of the last minute and currently
			// we're at the beginning, save the data
			if(needSave)
			{
				needSave = false;
				// Total up the watts this minute and save it into the data
				int x = sumArray(dataGarageMainSeconds);
				//x = x/10;
				int y = sumArray(dataGaragePlugsSeconds);
				//y = y/10;
				int z = sumArray(dataLaundrySeconds);
				//z = z/10;
				dataLog.saveData(x,y,z);
			}
			

			// Send any other periodic GMSEC messages
			//gPub.publish(msgFact.generateSingleDataMessage(DEVICE, LOCATION, ROLE, dataGarageMainAvg, dataGaragePlugsAvg, dataLaundryAvg));
			
			dataAvgCount = 1; // reset the data averaging counter
			lastSecond = now;
		}
	}
	
	private static short convertToWatts(short val, int resistance)
	{
		// Convert to wattage manually (decided through trial and error)
		// then clip all the max values to the nearest multiple of 5
		//short clip = 5;
		// Formula: (a0*5/1024 - 2.5V) * 120v * CURRENT_RATIO / RESISTANCE
		// the 2.5V (512 analog reading) is already taken off the value outside this conversion)
		short result = (short) ((val*5*120*CURRENT_RATIO)/(1024*resistance));
		//result = (short) (result/clip*clip);
		return result;
	}
	
	private static int sumArray(short[] s)
	{
		int sum = 0;
		for(int i=0;i<s.length;i++)
		{
			if(s[i] > 0)
				sum += s[i];
		}
		return sum;
	}
	
	public static void sendFullData(String data_name)
	{
		String gm = dataLog.getValue(data_name);
		gPub.publish(msgFact.generateDataMessage(DEVICE, ROLE, LOCATION, data_name, gm));
	}
}
