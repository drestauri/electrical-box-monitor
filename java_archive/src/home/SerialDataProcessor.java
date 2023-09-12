package home;

public class SerialDataProcessor {

	private static short analog0Data = 0;
	private static short analog1Data = 0;
	private static short analog2Data = 0;
	private static short analog3Data = 0;
	private static short loopData = 0;
	private static short sampleData = 0;
	
	private static boolean newAnalog0Data = false;
	private static boolean newAnalog1Data = false;
	private static boolean newAnalog2Data = false;
	private static boolean newAnalog3Data = false;
	private static boolean newLoopData = false;
	private static boolean newSampleData = false;
	
	// Check if there's data available and if so, store it in the
	// appropriate local variable and set the flag that data is available.
	public void handleData(String s)
	{
		// Handle Data
		if(s.contains("A"))
		{
			// A = Analog Channel 0
			analog0Data = getShort(s);
			newAnalog0Data = true;
		}
		else if(s.contains("B"))
		{
			// B = Analog Channel 1
			analog1Data = getShort(s);
			newAnalog1Data = true;
		}
		else if(s.contains("C"))
		{
			// C = Analog Channel 2
			analog2Data = getShort(s);
			newAnalog2Data = true;
		}
		else if(s.contains("D"))
		{
			// D = Analog Channel 3
			analog3Data = getShort(s);
			newAnalog3Data = true;
		}
		else if(s.contains("L"))
		{
			// L = average loop time
			// 16.67 / L = how many samples per 60Hz cycle
			loopData = getShort(s);
			newLoopData = true;
		}
		else if(s.contains("S"))
		{
			// S = sample period (how long we sample for when trying to find the max voltage)
			// Sample period / 16.67 = how many 60Hz cycles we sampled
			sampleData = getShort(s);
			newSampleData = true;
		}
	}
	

	private short getShort(String s)
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
	
	public boolean isDataReady()
	{
		// Checks for whether we need to send a GMSEC message:
		// If we have new samples from all the analog channels:
		if (newAnalog0Data && newAnalog1Data && newAnalog2Data && newAnalog3Data)
		{
			// Reset the flags
			newAnalog0Data = false;
			newAnalog1Data = false;
			newAnalog2Data = false;
			newAnalog3Data = false;
			return true;	//setValues();
		}
		else 
			return false;
	}
	
	public boolean isStatusReady()
	{
		// If we have new loop and sample rate. The Arduino only sends this every second so no need to limit the GMSEC msg rate
		// unless we want to send it less often.
		if (newLoopData & newSampleData)
		{
			// Reset the flags
			newLoopData = false;
			newSampleData = false;
			return true;
		}
		else
			return false;
	}
	
	public short getLoopData()
	{
		return loopData;
	}
	
	public short getSampleData()
	{
		return sampleData;
	}
	
	public short getGarageMainRed()
	{
		// Normalize the data point around 512 and if it's small or negative, just set it to 0 
		analog3Data -= 512;
		if(analog3Data < 0)
			analog3Data = 0;
		return analog3Data;
	}
	
	public short getGarageMainBlack()
	{
		// Normalize the data point around 512 and if it's small or negative, just set it to 0 
		analog1Data -= 512;
		if(analog1Data < 0)
			analog1Data = 0;
		return analog1Data;
	}
	
	public short getGaragePlugs()
	{
		// Normalize the data point around 512 and if it's small or negative, just set it to 0 
		analog0Data -= 512;
		if(analog0Data < 0)
			analog0Data = 0;
		return analog0Data;
	}
	
	public short getGarageLaundry()
	{
		// Normalize the data point around 512 and if it's small or negative, just set it to 0 
		analog2Data -= 512;
		if(analog2Data < 0)
			analog2Data = 0;
		return analog2Data;
	}
	
}
