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
import home.gmsec.MsgFactory;
import home.gmsec.callbacks.DataRequestCB;
import home.utils.Configuration;
import home.utils.DataLogger;
import home.utils.Logger;
import home.utils.Serial;

/************ TODO *****************
 * - Find a way to make the data.properties file more robust. I assume I cut power/rebooted while the file was open and it ended up getting corrupted
 * 		> Create a copy whenever you open it and delete it whenever you close. If it's invalid, restore from the copy (or reset if no copy)?
 * - Implement a message subscriber that sets the system date and time (just print the command if windows or execute if Linux) 
 * - Separate out some often used status functions (like "getRunTime" and "syncDateAndTime") for reuse
 * - Change to send last data point and status every second instead of only if new data is available
 *  
 ************* NOTES *****************
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
 *  TEST FOR JENKINS!!!
 */

public class App_EBM {
	private static final String VERSION = "2020.8.1.1500";
	private static final long START_TIME_MILLIS = System.currentTimeMillis();
	
	public static Logger log;
	private static DataLogger dataLog;
	private static SerialDataProcessor sdProcessor;
	public static GMSECConnection gmsec;
	private static GMSECSubscriber gSub;
	private static GMSECPublisher gPub;
	private static MsgFactory msgFact;
	private static DataRequestCB dataReqCB;

	private static boolean isFinished = false;	
	
	private static Configuration config;
	
	private static int lastSecond;
	private static short[] dataGarageMainRedSeconds = new short[60];
	private static short[] dataGarageMainBlackSeconds = new short[60];
	private static short[] dataGaragePlugsSeconds = new short[60];
	private static short[] dataLaundrySeconds = new short[60];
	private static short dataGarageMainRedAvg = 0; // for averaging the data every second
	private static short dataGarageMainBlackAvg = 0; // for averaging the data every second
	private static short dataGaragePlugsAvg = 0; // for averaging the data every second
	private static short dataLaundryAvg = 0; // for averaging the data every second
	private static short dataAvgCount = 1; // for averaging calculation
	
	public static void main(String[] args) throws Exception
	{
		log = new Logger();
		log.LogMessage_High("=============================");
		log.LogMessage_High("App started. Loading config.");
		
		config = new Configuration("config.properties");
		
		log.LogMessage_High("Version: " + VERSION);

		
		//======== GET COMMAND LINE ARGUMENTS ==========
		log.LogMessage_Low("Getting command line args");
		String commPort = config.getDefaultCommPort();
		String gmsec_args[] = {config.getGmsecMode(), "mw-id="+config.getGmsecMw(), config.getGmsecServer()};
		
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
		dataLog = new DataLogger("data.properties");
		dataLog.loadData();
		lastSecond = Calendar.getInstance().get(Calendar.SECOND);
		sdProcessor = new SerialDataProcessor();
		
		//============== Setup GMSEC ================
		// Send the arguments to the connection class
		// GMSECConnection() also starts the connection to the message bus
		gmsec = new GMSECConnection(gmsec_args);
		
		// Create a new publisher object that can send messages.
		gPub = new GMSECPublisher(gmsec.getConnMgr());
		
		dataReqCB = new DataRequestCB();
		gSub = new GMSECSubscriber(gmsec.getConnMgr());
		gSub.subscribe(config.getDataRequestTopic(), dataReqCB);
		
		// Create a new message factory for generating messages
		msgFact = new MsgFactory();
		
		// Setup the serial port to Arduino
		Serial serial = new Serial(commPort);
		if(!serial.isConnected())
		{
			// Send GMSEC Message
			gPub.publish(msgFact.generateWarningMessage(config.getDevice(), config.getRole(), config.getLocation(), "Could not find desired COM port."));
		}
		
		//================ LOOP =====================
		while(!isFinished)
		{
			// Here to add a loop delay if needed
			try {
				Thread.sleep(0,0);
			} catch (InterruptedException e) {
				log.LogMessage_High("isFinished error!");
				e.printStackTrace();
			}
			
			if(serial.isDataAvail())
				sdProcessor.handleData(serial.getLastData());
			
			if(sdProcessor.isDataReady())
				setValues();
			
			if(sdProcessor.isStatusReady())
				// Send the GMSEC Data Message
				gPub.publish(msgFact.generateStatusMessage(VERSION, config.getDevice(), config.getRole(), config.getLocation(), getRunTime(), sdProcessor.getLoopData(), sdProcessor.getSampleData()));
		}
		
		
		log.LogMessage_High("Disconnecting from GMSEC bus");

		// Disconnect and cleanup the GMSEC connection
		gmsec.disconnect();

		return;
	}
	
	private static String getRunTime()
	{
		// 000d:00h:00m:00s
		long time = System.currentTimeMillis() - START_TIME_MILLIS;
		time = time/10000; // convert to seconds  1000
		String s = "";
		
		long t;
		t = time/60/60/24; // to days 
		s+=Long.toString(t);
		s+="d:";
		
		time = time%(60*60*24); // subtract off days
		t = time/60/60; // to hours 
		s+=Long.toString(t);
		s+="h:";
		
		time = time%(60*60); // subtract off hours
		t = time/60; // to minutes 
		s+=Long.toString(t);
		s+="m:";
		
		time = time%60; // subtract off minutes
		s+=Long.toString(time);
		s+="s";
		
		return s;
	}
	
	private static void setValues()
	{
		/************************************************************
		 * Multiple times per second, we should receive the values
		 * from Arduino and call this function.
		 * The data will be averaged over 1 second then stored for
		 * graphing and calculating longer term data.
		 ************************************************************/
		short gRed = 0;
		short gBlack = 0;
		short gPlugs = 0;
		short gLaundry = 0;
		
		gRed = sdProcessor.getGarageMainRed();
		gBlack = sdProcessor.getGarageMainBlack();
		gPlugs = sdProcessor.getGaragePlugs();
		gLaundry = sdProcessor.getGarageLaundry();
		
		
		// Continuously average the data over 1 full second. dataAvgCount goes to 1, effectively reducing
		// the weight of the old value at the beginning of a new second.
		dataGarageMainRedAvg = (short) ((dataGarageMainRedAvg * dataAvgCount + gRed)/(dataAvgCount+1));
		dataGarageMainBlackAvg = (short) ((dataGarageMainBlackAvg * dataAvgCount + gBlack)/(dataAvgCount+1));
		dataGaragePlugsAvg = (short) ((dataGaragePlugsAvg * dataAvgCount + gPlugs)/(dataAvgCount+1));
		dataLaundryAvg = (short) ((dataLaundryAvg * dataAvgCount + gLaundry)/(dataAvgCount+1));
		dataAvgCount++;		
		
		
		// Check if a second or more has passed
		int now = Calendar.getInstance().get(Calendar.SECOND);
		if(now != lastSecond)
			setSingleSecondOfData(now);
	}
	
	
	private static void setSingleSecondOfData(int now)
	{
		boolean needSave = false;
		// Note that now should always be greater than lastSecond
		// except when we roll over from 59 to 0 (i.e. a minute has passed)
		// needSave is used later in this function to indicate it's time
		// to average and save the last minute's worth of data
		if(lastSecond > now)
			needSave = true;
		
		// Store the data point in every slot up until "now" (compensates for gaps using the average)
		int sec = lastSecond;
		short gmRedAvg = convertGMRedToWatts(dataGarageMainRedAvg);
		short gmBlackAvg = convertGMBlackToWatts(dataGarageMainBlackAvg);
		short gpAvg = convertGPlugsToWatts(dataGaragePlugsAvg);
		short lAvg = convertLaundryToWatts(dataLaundryAvg);
		while(lastSecond!=now)
		{
			// Save seconds here so that during the last loop, sec isn't updated. This way, sec is always the second before now.
			// i.e. "now" is a future data point since it's the second we are currently in. The data point we are saving here
			// is actually for the second before "now", and we only want to fill in gaps in data that are older than that.
			// We don't use now-1 because if now is 0, that's invalid value, and lastSecond is being modified here, so this
			// is just an easy way to get the value of the second before "now"
			sec = lastSecond;
			dataGarageMainRedSeconds[lastSecond] = gmRedAvg;
			dataGarageMainBlackSeconds[lastSecond] = gmBlackAvg;
			dataGaragePlugsSeconds[lastSecond] = gpAvg;
			dataLaundrySeconds[lastSecond] = lAvg;
			lastSecond++;
			if(lastSecond>=60)
				lastSecond=0;
		}
		
		// EVERY SECOND PUBLISH YOUR BASIC DATA MESSAGE
		// Send the GMSEC Data Message
		gPub.publish(msgFact.generateSingleDataMessage(config.getDevice(), config.getRole(), config.getLocation(), sec, gmRedAvg, gmBlackAvg, gpAvg, lAvg, "Watt-Seconds"));
		
		// If we are starting a new minute, the last minute's worth of data needs to be saved
		if(needSave)
		{
			needSave = false;
			// Total up the watts this past minute and save it into the data
			int t1 = sumArray(dataGarageMainRedSeconds);
			int t2 = sumArray(dataGarageMainBlackSeconds);
			int t3 = sumArray(dataGaragePlugsSeconds);
			int t4 = sumArray(dataLaundrySeconds);
			dataLog.saveData(t1,t2,t3,t4);
		}
		
		dataAvgCount = 1; // reset the data averaging counter used for averaging data received over each second
		lastSecond = now; // Now that 1 second has passed and we've processed the data, set lastSecond to now so we do it again after now increases
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
	
	
	public static void sendFullData(String data_name, String req_id)
	{
		// data_name = which string of data to retrieve from the dataLog (like "GARAGE-PLUGS-DAYS")
		// req_id = an ID used by the requester to determine "why" this data was requested. This results
		//			in different processing on that end.
		// Get the data from the dataLog and store in data
		String data = dataLog.getStringValue(data_name); 
		gPub.publish(msgFact.generateDataMessage(config.getDevice(), config.getRole(), config.getLocation(), data_name, data, req_id, 
				dataLog.getStringValue("SECOND"), dataLog.getStringValue("MINUTE"), dataLog.getStringValue("HOUR"),
				dataLog.getStringValue("DAY_OF_MONTH"), dataLog.getStringValue("MONTH"), dataLog.getStringValue("YEAR"),
				dataLog.getUnits(data_name)));
	}
	
/***********************************************
 * These functions were all determined using
 * actual data samples with a power meter and
 * then recording the raw value on the analog
 * channel. Then a best fit curve was applied
 * to the data and that's the formula you see
 * here. 	
 ***********************************************/
	private static short convertGMRedToWatts(short val)
	{
		// Apply best fit trend line from testing
		// y = -0.0015x2 + 7.4054x - 22.603
		double value = (double)val;
		value = 7.4054*value - 22.603 - 0.0015*value*value;
		if (value<0)
			value = 0;
		return (short)value;
	}
	
	private static short convertGMBlackToWatts(short val)
	{
		// Apply best fit trend line from testing
		// y = 0.0163x2 + 2.3141x - 54.265
		double value = (double)val;
		value = 0.0163*value*value + 2.3141*value - 54.265;
		if (value<0)
			value = 0;
		return (short)value;
	}
	
	private static short convertGPlugsToWatts(short val)
	{
		// Apply best fit trend line from testing
		// y = 0.0021x2 + 3.7582x - 60.912
		double value = (double)val;
		value = 0.0021*value*value + 3.7582*value - 60.912;
		if (value<0)
			value = 0;
		return (short)value;
	}
	
	private static short convertLaundryToWatts(short val)
	{
		// Apply best fit trend line from testing
		// y = 0.0081x2 + 2.9596x - 32.459
		double value = (double)val;
		value = 0.0081*value*value + 2.9596*value - 32.459;
		if (value<0)
			value = 0;
		return (short)value;
	}

}
