package home.utils;

import java.io.BufferedReader;
import java.io.InputStreamReader;
//import java.io.OutputStream;
import java.util.Enumeration;
import java.util.concurrent.TimeUnit;

import com.fazecast.jSerialComm.SerialPort;
import com.fazecast.jSerialComm.SerialPortDataListener;
import com.fazecast.jSerialComm.SerialPortEvent;

import home.App;

// TODO:
// > Add functionality to measure time between data points, possibly last 8 data points?


// More info on this code:
// https://github.com/Fazecast/jSerialComm/wiki/Event-Based-Reading-Usage-Example

// SETUP:
// Get the jSerialComm JAR from:
//	https://fazecast.github.io/jSerialComm/
// Add it to the build path of the project and all should work ok.
// I read some stuff only works on windows due to the way Linux does serial comm so
// this may only work on windows

public class Serial implements SerialPortDataListener{
	private static final int DATA_BUFFER_SIZE = 16;	// max number of characters to pull from the serial interface
	private static final int DATA_HISTORY_SIZE = 8; // max last values to save
	//private static final int TIME_OUT = 2000;
	private static final int DATA_RATE = 115200;
	private static String PORT_NAME;
	
	private SerialPort serialPort = null;
	private char[] dataBuffer = new char[DATA_BUFFER_SIZE];
	private int bufIndex = 0;
	private int startDataIndex = 0;
	private int curDataIndex = 0;
	private BufferedReader input;
	//private static OutputStream output;
	private String[] dataHistory = new String[DATA_HISTORY_SIZE];
	private boolean dataOverflow = false;
	
	private int count = 0;
	private long lastTime = System.currentTimeMillis();

	
	// Constructor. Takes a string if there's a specific comm port to use
	public Serial(String com){
		PORT_NAME = com;
		initialize();
		Thread t=new Thread() {
			public void run() {
				try {Thread.sleep(1000000);} catch (InterruptedException ie) {}
			}
		};
		
		if (serialPort != null)
		{
			t.start();
			System.out.println("Serial Comms Started");
		}
	}
	
	
	public void initialize() {
		// Get a list of the current ports on this machine
		SerialPort[] ports = SerialPort.getCommPorts();
		
		// Check each port for a match to the port indicated at command line
		System.out.println("Found Ports:");
		for (SerialPort port : ports) 
		{
			String currPortId = port.getSystemPortName();
			System.out.println(currPortId);

			if (currPortId.equals(PORT_NAME))
			{
				serialPort = port;
				break;
			}
		}
		
		
		// If we couldn't find a matching port
		if (serialPort == null) {
			System.out.println("Could not find desired COM port.");
			App.log.LogMessage_High("Could not find desired COM port.");
			return;
		}

		// Open the port and set the comm parameters, get the input and output streams
		try 
		{
			System.out.print("Attempting to open port: ");
			System.out.println(serialPort.getSystemPortName());
			if(serialPort.openPort())
			{
			System.out.println("Port opened successfully.");
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			}
			else {
				System.out.println("Unable to open the port.");
			}
		            
			serialPort.setComPortParameters(DATA_RATE, 8, 1, SerialPort.NO_PARITY);
			serialPort.setComPortTimeouts(SerialPort.TIMEOUT_READ_BLOCKING, 0, 0);
		
			input = new BufferedReader(new InputStreamReader(serialPort.getInputStream()));
			//output = serialPort.getOutputStream();
		
			serialPort.addDataListener(this);
			serialPort.notifyAll();
		}
		catch (Exception e) {
			System.err.print("Error 1 (this always gets called once for some reason):");
			System.err.println(e.toString());
		}
	}

	
	@Override
	public int getListeningEvents() 
	{
		// LISTENING_EVENT_DATA_AVAILABLE = any data has been received
		// LISTENING_EVENT_DATA_RECEIVED = data read from serial port
		return SerialPort.LISTENING_EVENT_DATA_AVAILABLE;
	}
  
	
	// Callback for if data is on the serial port
	public synchronized void serialEvent(SerialPortEvent oEvent) {
		// LISTENING_EVENT_DATA_AVAILABLE = any data has been received
		// LISTENING_EVENT_DATA_RECEIVED = data read from serial port
		if(oEvent.getEventType() != SerialPort.LISTENING_EVENT_DATA_AVAILABLE)
			return;

		processDataAvailable();
	}
   
	
	// CLose the serial port
	public synchronized void close() {
		if (serialPort != null) {
			serialPort.removeDataListener();
			serialPort.closePort();
		}
	}
	
	
	private void processDataAvailable()
	{
		// Create a buffer the size of the bytes available
		byte[] newData = new byte[serialPort.bytesAvailable()];
		// Fill the buffer with data off the serial bus
		serialPort.readBytes(newData, newData.length);
		// Step through each character in the newData buffer
		for (int i = 0; i < newData.length; ++i)
		{
			// Convert the value to character
			char c = (char)newData[i];

			// Strings of data come with ASCII white space characters with integer values 13 & 10 at the end of each line
			// Goal is to add all values to the dataBuffer and then when we have the whole thing store the result
			// as a string. The user can then obtain and process the string manually.
			//===========
			// Check if we found the newline characters and if so, convert the data to a string and reset the buffer
			// If not, continue adding characters to the buffer
			if(c == 10 || c == 13)
			{
				// If there hasn't been a data buffer overflow & we have at least 1 value
				if(!dataOverflow && bufIndex>0)
				{
					// Grab the substring from 0 for bufIndex number of characters
					storeData(String.valueOf(dataBuffer, 0, bufIndex));

					/*
					// For Testing Data Rate
					if(lastData.contains("A1"));
						count++;
					long now = System.currentTimeMillis();
					if(now-lastTime > 1000)
					{
						System.out.println("Count: " + count);
						lastTime = now;
						count = 0;
					}*/
					
				}
				
				// If we detect new line characters, and had either a buffer overflow or no new characters 
				// reset the buffer index without processing the data. Otherwise, reset the buffer after processing
				bufIndex = 0;
				dataOverflow = false;
			}
			else
			{
				// If we receive anything other than 10 or 13 (ACSII) put the character in the buffer
				if(bufIndex >= DATA_BUFFER_SIZE)
				{
					App.log.LogMessage_High("Error: Data buffer overflow. Current buffer: " + String.valueOf(dataBuffer, 0, DATA_BUFFER_SIZE-1));
					System.err.println("Error: Data buffer overflow. Current buffer: " + String.valueOf(dataBuffer, 0, DATA_BUFFER_SIZE-1));
					dataOverflow = true;
				}
				else
				{
					dataBuffer[bufIndex] = c;
					bufIndex++;
				}
			}
		}
	}
	
	
	// Store data in the next slot of the data history array & check for overflow
	private void storeData(String s)
	{
		// Put it in the current slot & increment
		dataHistory[curDataIndex++] = s;
		
		// Make sure it doesn't exceed the array bounds
		if (curDataIndex >= DATA_HISTORY_SIZE)
			curDataIndex = 0;
		
		// If we exceed the array size, start overwriting data
		// Increment start data index and make sure it doesn't exceed the array bounds
		if(curDataIndex == startDataIndex)
		{
			startDataIndex++;
			if(startDataIndex >= DATA_HISTORY_SIZE)
				startDataIndex=0;
		}
	}
	
	
	// Get data from the start of the recent data points and check for underflow
	public String getLastData()
	{
		// If start = current, then we have an empty data array
		if(startDataIndex!=curDataIndex)
		{
			String tStr = dataHistory[startDataIndex++];
			
			if(startDataIndex>=DATA_HISTORY_SIZE)
				startDataIndex=0;
			return tStr;
		}
		return "";
	}
	
	
	// Look at the last data point but don't extract it from the queue
	public String peekLastData()
	{
		// If start = current, then we have an empty data array
		if(startDataIndex != curDataIndex)
		{
			return dataHistory[startDataIndex];
		}
		return "";
	}
	
	public void printDataBuffer()
	{
		for(int j=0;j<bufIndex;j++)
		{
			System.out.print(dataBuffer[j]);
		}	
		System.out.println();
	}

	
	public int getLastDataAsInteger()
	{
		String tStr = getLastData();
		for(int i = 0;i<tStr.length();i++)
		{
			// Make sure all the characters of the string are numbers
			if(tStr.charAt(i) < '0' || tStr.charAt(i) > '9')
			{
				App.log.LogMessage_High("ERROR: Attempted to convert non integer data point to an integer");
				System.err.println("ERROR: Attempted to convert non integer data point to an integer");
				return -1;
			}
		}
		return Integer.parseInt(tStr);
	}
	
	
	public boolean isConnected()
	{
		if(serialPort == null)
			return false;
		return true;
	}

	public boolean isDataAvail()
	{
		// If start index isn't on top of current data index, then we have data
		if(startDataIndex != curDataIndex)
		{
			return true;
		}
		else
			return false;
	}
}
