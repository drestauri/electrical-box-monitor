package home.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Calendar;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

import home.App_EBM;

/*********** TODO **************
 *  MAJOR: Implement support for more than seconds 
 *  >> Was this completed?
 *  - Try to make this class implementation independent
 *    > For example, instead of specifying the name of the data to save,
 *  	try to take a String array from the user and use that to set and parse data
 *  - Check/Finish the TODO items below
 *  - Implement better error handling for data that doesn't exist/is incorrectly spelled
 *    > Such as converting to all caps
 *    > Returning an error string and handling in the main code
 * ****************************/

public class DataLogger {

	private static Properties props;
	private static File dataFile;
	private final String EMPTY_MIN_60 = "-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1";
	private final String EMPTY_HR_MO_24 = "-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1";
	private final String EMPTY_DAY_31 = "-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1";
	private int yearChange = 0; 
	private int monthChange = 0;
	private int dayChange = 0; 
	private int hourChange = 0;
	private int minChange = 0;
	
	public DataLogger()
	{
		props = new Properties();
	}
	
	public void setFile(String file)
	{
		dataFile=new File(file);
	}
	
	public void loadData()
	{
		File tmpDir = new File("data.properties");
		App_EBM.log.LogMessage_High("Props file length: " + tmpDir.length());
		if(!tmpDir.exists())
		{
			App_EBM.log.LogMessage_High("Props file not found");
			genDefaultDataFile();
		}else if(tmpDir.length() < 1000)
		{
			App_EBM.log.LogMessage_High("Props file empty or invalid");
			genDefaultDataFile();
		}
		
		InputStream is;
		try {
			is = new FileInputStream(dataFile);
			if (is!=null)
				props.loadFromXML(is);
		} catch (FileNotFoundException e) {
			System.out.println("Props file not found");
			App_EBM.log.LogMessage_High("Error: Props file not found");
			e.printStackTrace();
		} catch (InvalidPropertiesFormatException e) {
			System.out.println("Props file invalid");
			App_EBM.log.LogMessage_High("Error: Props file invalid");
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Props file IO error");
			App_EBM.log.LogMessage_High("Error: Props file IO error");
			e.printStackTrace();
		}
	}
	
	public void saveData(int garage_main_red, int garage_main_black, int garage_plugs, int laundry)
	{
		// This function is called once per minute to save the data
		// It also checks if the hours, days, or months rolled over so the data can be updated appropriately
		
		// TODO: All arrays should not include the current time so a "to date" value can be calculated when the
		// 		mode is switched and maintained locally. The stored "to date" value can be calculated at roll over
		// TODO: Implement compensation for gaps in time. check the last time it was saved and insert 0's or average
		// NOTE: the time is updated at the end of this method so that the last save time can be determined
		// For example, if the last time saved was before the end of the hour and it is now after the end of the hour
		// we can assume a data point is needed. Make sure to consider the case that we haven't saved in days. Fill missing
		// data with 0s except for maybe the minutes (needs some thought)
		
		// Leap years: 2020, 2024, 2028. If it's the year after a leap year, I want to add 366 days not 365
		int leapYear = (Calendar.getInstance().get(Calendar.YEAR) - 2021)%4 == 0 ? 1 : 0;
		yearChange = Calendar.getInstance().get(Calendar.YEAR) - Integer.parseInt(props.getProperty("YEAR"));
		monthChange = Calendar.getInstance().get(Calendar.MONTH)+1 - Integer.parseInt(props.getProperty("MONTH")) + yearChange*12;
		dayChange = Calendar.getInstance().get(Calendar.DAY_OF_YEAR) - Integer.parseInt(props.getProperty("DAY_OF_YEAR")) + yearChange*365 + leapYear;
		hourChange = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) - Integer.parseInt(props.getProperty("HOUR")) + dayChange*24;
		minChange = Calendar.getInstance().get(Calendar.MINUTE) - Integer.parseInt(props.getProperty("MINUTE")) + hourChange*60;
		
		// Each update will check if it rolled over or if there was excessive time that passed that requires the next
		// higher unit to be updated
		
		// Update mins if less than 60 mins have passed since we last updated (there's still some valid minute data)
		// otherwise, reset the minutes data
		if(minChange<60)
			updateMins(garage_main_red, garage_main_black, garage_plugs, laundry);
		else
		{
			App_EBM.log.LogMessage_High("ERROR minChange: " + Integer.toString(minChange));
			resetMins();
		}
		// Similarly if the hours have changed since we last update and at least some of the data is still valid, update
		// The update methods for each set any invalid data to 0 and then pull the most recent data from the smaller unit in
		if(hourChange>0 && hourChange<24)
			updateHours();
		else if(hourChange>=24)
		{
			App_EBM.log.LogMessage_High("ERROR hourChange: " + Integer.toString(hourChange));
			resetHours();
		}
		if(dayChange>0 && dayChange<30)
			updateDays();
		else if(dayChange>=30)
		{
			App_EBM.log.LogMessage_High("ERROR dayChange: " + Integer.toString(dayChange));
			resetDays();
		}
		if(monthChange>0 && monthChange<24)
			updateMonths();
		else if(monthChange>=24)
		{
			App_EBM.log.LogMessage_High("ERROR monthChange: " + Integer.toString(monthChange));
			resetMonths();
		}
		
		// Set the time stamp
		props.setProperty("YEAR", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
		props.setProperty("MONTH", Integer.toString(Calendar.getInstance().get(Calendar.MONTH)+1));
		props.setProperty("DAY_OF_MONTH", Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));
		props.setProperty("DAY_OF_YEAR", Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_YEAR)));
		props.setProperty("HOUR", Integer.toString(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)));
		props.setProperty("MINUTE", Integer.toString(Calendar.getInstance().get(Calendar.MINUTE)));
		props.setProperty("SECOND", Integer.toString(Calendar.getInstance().get(Calendar.SECOND)));
	
		// Write the data to the file
		OutputStream os;
		try {
			os = new FileOutputStream(dataFile);
			props.storeToXML(os, "Minutes are in watt-seconds, hours and days are in kW-seconds, months are kW-minutes", "UTF-8");
		} catch (FileNotFoundException e) {
			App_EBM.log.LogMessage_High("Error: Can't save. Props file not found");
			e.printStackTrace();
		} catch (IOException e) {
			App_EBM.log.LogMessage_High("Error: Can't save. Other error");
			e.printStackTrace();
		}
	}
	
	private void updateMonths()
	{
		// Similarly if the hours have changed since we last update and at least some of the data is still valid, update
		// The update methods for each set any invalid data to 0 and then pull the most recent data from the smaller unit in			
		int[] d1 = getIntData("GARAGE-MAIN-RED-MONTHS");	
		int[] d2 = getIntData("GARAGE-MAIN-BLACK-MONTHS");
		int[] d3 = getIntData("GARAGE-PLUGS-MONTHS");
		int[] d4 = getIntData("LAUNDRY-MONTHS");
		

		// If more than 1 month has passed, set the missed data to -1
		// Get the current month. Months go 0-30 and we want to track for 24 months so use the odd/eveness of the
		// month to calculate the index. Also subtract 1 so we are pointing at last month.
		int curVal = Calendar.getInstance().get(Calendar.MONTH)+12*(Calendar.getInstance().get(Calendar.YEAR)%2)-1;
		if (curVal<0)
			curVal = d1.length-1;
		int j = curVal;
		int k = 1;
		for(int i = 1; i<monthChange; i++)
		{
			if(j-k < 0)
			{
				// if we reach the begin of the array, start from the end
				j = d1.length-1;
				k=0;
			}
			d1[j-k] = -1;
			d2[j-k] = -1;
			d3[j-k] = -1;
			d4[j-k] = -1;
			k++;
		}
		
		// Sum the days data (kW-seconds) and store it. Divide by 60 to put it in kW-minutes
		d1[curVal] = sumDataPoints("GARAGE-MAIN-RED-DAYS",60);
		d2[curVal] = sumDataPoints("GARAGE-MAIN-BLACK-DAYS",60);
		d3[curVal] = sumDataPoints("GARAGE-PLUGS-DAYS",60);
		d4[curVal] = sumDataPoints("LAUNDRY-DAYS",60);
		
		App_EBM.log.LogMessage_High("Saving months. curVal: " + curVal + ", gm-red: " + d1[curVal] + ", gm-black: " + d2[curVal] + ", g-plug: " + d3[curVal] + ", laundry: " + d4[curVal]);
		// save the data
		props.setProperty("GARAGE-MAIN-RED-MONTHS", dataToString(d1));
		props.setProperty("GARAGE-MAIN-BLACK-MONTHS", dataToString(d2));
		props.setProperty("GARAGE-PLUGS-MONTHS", dataToString(d3));
		props.setProperty("LAUNDRY-MONTHS", dataToString(d4));

	}
	
	private void updateDays()
	{		
		
		// Similarly if the days have changed since we last update and at least some of the data is still valid, update
		// The update methods for each set any invalid data to 0 and then pull the most recent data from the smaller unit in			
		int[] d1 = getIntData("GARAGE-MAIN-RED-DAYS");		
		int[] d2 = getIntData("GARAGE-MAIN-BLACK-DAYS");
		int[] d3 = getIntData("GARAGE-PLUGS-DAYS");
		int[] d4 = getIntData("LAUNDRY-DAYS");
		
		// Determine max days last month so we can set the invalid data
		int leapYear = (Calendar.getInstance().get(Calendar.YEAR)-2020)%4;
		int lastMax = Calendar.getInstance().get(Calendar.MONTH);
		switch(lastMax)
		{
		case Calendar.JANUARY:
			lastMax = 31;
			break;
		case Calendar.FEBRUARY:
			lastMax = 31;
			break;
		case Calendar.MARCH:
			if(leapYear == 0)
				lastMax = 29;
			else
				lastMax = 28;
			break;
		case Calendar.MAY:
			lastMax = 30;
			break;
		case Calendar.JULY:
			lastMax = 30;
			break;
		case Calendar.AUGUST:
			lastMax = 31;
			break;
		case Calendar.OCTOBER:
			lastMax = 30;
			break;
		case Calendar.DECEMBER:
			lastMax = 30;
			break;
		default:
			lastMax = 31;
			// April, June, September, November
		}
		
		// DAY_OF_MONTH is from 1-31 so need to subtract 1 to get correct index and another 1 save the data in last day
		int curVal = Calendar.getInstance().get(Calendar.DAY_OF_MONTH)-2;
		if (curVal < 0)
			curVal = lastMax-1; // might not go to the end of the array for days of month
		
		// First, make sure that all the invalid data are set as invalid
		// no need to -1 because the max index will point at the 1st invalid date
		// i.e. 28 points to day 29 (so 29-31 are invalid and will be marked as such)
		for (int i = lastMax; i<d1.length; i++)
		{
			// For example if last month had 28 days in it, only index 0-27 are valid.
			// So 28 to the end of the array should be set to -1, but only if we aren't currently 
			// past that index with the new month's data!
			if(curVal < i)
			{
				d1[i] = -1;
				d2[i] = -1;
				d3[i] = -1;
				d4[i] = -1;
			}
		}

		// If more than 1 day has passed, set the missed data to -1
		int j = curVal;
		int k = 1;
		for(int i = 1; i<dayChange; i++)
		{
			if(j-k < 0)
			{
				// if we reach the begin of the array, start from where the last month's
				// data would have ended
				j = lastMax;
				k=0;
			}
			d1[j-k] = -1;
			d2[j-k] = -1;
			d3[j-k] = -1;
			d4[j-k] = -1;
			k++;
		}
		
		// Sum the hours data (kW-seconds) and store it. Does not reduce
		d1[curVal] = sumDataPoints("GARAGE-MAIN-RED-HOURS",1);
		d2[curVal] = sumDataPoints("GARAGE-MAIN-BLACK-HOURS",1);
		d3[curVal] = sumDataPoints("GARAGE-PLUGS-HOURS",1);
		d4[curVal] = sumDataPoints("LAUNDRY-HOURS",1);
		
		App_EBM.log.LogMessage_High("Saving days. curVal: " + curVal + ", gm-red: " + d1[curVal] + ", gm-black: " + d2[curVal] + ", g-plug: " + d3[curVal] + ", laundry: " + d4[curVal]);
		// save the data
		props.setProperty("GARAGE-MAIN-RED-DAYS", dataToString(d1));
		props.setProperty("GARAGE-MAIN-BLACK- DAYS", dataToString(d2));
		props.setProperty("GARAGE-PLUGS-DAYS", dataToString(d3));
		props.setProperty("LAUNDRY-DAYS", dataToString(d4));
	}
	
	private void updateHours()
	{
		// Similarly if the hours have changed since we last update and at least some of the data is still valid, update
		// The update methods for each set any invalid data to 0 and then pull the most recent data from the smaller unit in			
		int[] d1 = getIntData("GARAGE-MAIN-RED-HOURS");
		int[] d2 = getIntData("GARAGE-MAIN-BLACK-HOURS");
		int[] d3 = getIntData("GARAGE-PLUGS-HOURS");
		int[] d4 = getIntData("LAUNDRY-HOURS");
		

		// If more than 1 hour has passed, set the missed data to -1
		// Hours are 0-23 so subtract just 1 to point at the last hour
		int curVal = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)-1;
		if (curVal < 0)
			curVal = d1.length-1;
		int j = curVal;
		int k = 1;
		for(int i = 1; i<hourChange; i++)
		{
			if(j-k < 0)
			{
				// if we reach the begin of the array, start from the end
				j = d1.length-1;
				k=0;
			}
			d1[j-k] = -1;
			d2[j-k] = -1;
			d3[j-k] = -1;
			d4[j-k] = -1;
			k++;
		}
		
		// Sum the minutes data (w-seconds) and store it. Divides it by 1000 to put it in kW-seconds
		d1[curVal] = sumDataPoints("GARAGE-MAIN-RED-MINUTES",1000);
		d2[curVal] = sumDataPoints("GARAGE-MAIN-BLACK-MINUTES",1000);
		d3[curVal] = sumDataPoints("GARAGE-PLUGS-MINUTES",1000);
		d4[curVal] = sumDataPoints("LAUNDRY-MINUTES",1000);
		
		App_EBM.log.LogMessage_High("Saving hours. curVal: " + curVal + ", gm-red: " + d1[curVal] + ", gm-black: " + d2[curVal] + ", g-plug: " + d3[curVal] + ", laundry: " + d4[curVal]);
		// save the data
		props.setProperty("GARAGE-MAIN-RED-HOURS", dataToString(d1));
		props.setProperty("GARAGE-MAIN-BLACK-HOURS", dataToString(d2));
		props.setProperty("GARAGE-PLUGS-HOURS", dataToString(d3));
		props.setProperty("LAUNDRY-HOURS", dataToString(d4));
	}
	
	private void updateMins(int garage_main_red, int garage_main_black, int garage_plugs, int laundry)
	{
		int[] d1 = getIntData("GARAGE-MAIN-RED-MINUTES");
		int[] d2 = getIntData("GARAGE-MAIN-BLACK-MINUTES");
		int[] d3 = getIntData("GARAGE-PLUGS-MINUTES");
		int[] d4 = getIntData("LAUNDRY-MINUTES");
		

		// If more than 1 minute has passed, set the missed data to -1
		// Minutes are 0-59 so subtract 1 to point at last value
		int curVal = Calendar.getInstance().get(Calendar.MINUTE)-1;
		if (curVal < 0)
			curVal = d1.length-1;
		int j = curVal;
		int k = 1;
		for(int i = 1; i<minChange; i++)
		{
			if(j-k < 0)
			{
				// if we reach the begin of the array, start from the end
				j = d1.length-1;
				k=0;
			}
			d1[j-k] = -1;
			d2[j-k] = -1;
			d3[j-k] = -1;
			d4[j-k] = -1;
			k++;
		}

		d1[curVal] = garage_main_red;
		d2[curVal] = garage_main_black;
		d3[curVal] = garage_plugs;
		d4[curVal] = laundry;
		
		// Saves the data as is (same units as seconds: Watt-seconds)
		props.setProperty("GARAGE-MAIN-RED-MINUTES", dataToString(d1));
		props.setProperty("GARAGE-MAIN-BLACK-MINUTES", dataToString(d2));
		props.setProperty("GARAGE-PLUGS-MINUTES", dataToString(d3));
		props.setProperty("LAUNDRY-MINUTES", dataToString(d4));
	}
	
	
	public String getStringValue(String data_name)
	{
		return props.getProperty(data_name);
	}
	
	public int[] getIntData(String data_name)
	{
		// Get the string containing the value for the specified data field
		String str = props.getProperty(data_name);
		// make sure the data has commas, otherwise it's not data (should use get value)
		String[] s;
		
		if(str==null)
		{
			System.err.println("******************** getData returned null: " + data_name);
			App_EBM.log.LogMessage_High("ERROR: getData returned null: " + data_name);
		}
		if(str.contains(","))
			s = str.split(",");
		else
			return null;
		
		// Create an array of ints
		int[] d = new int[s.length];
		// Parse s into the data array and return it
		for(int i=0;i<s.length;i++)
			d[i]=Integer.parseInt(s[i]);
		return d;
	}
	
	public String getUnits(String data_name)
	{
		//Minutes are in Watt-seconds, hours and days are in kW-seconds, months are kW-minutes
		if(data_name.contains("MINUTES"))
			return "W-Seconds";
		else if(data_name.contains("HOURS"))
			return "kW-Seconds";
		else if(data_name.contains("DAYS"))
			return "kW-Seconds";
		else if(data_name.contains("MONTHS"))
			return "kW-Minutes";
		
		App_EBM.log.LogMessage_High("Invalid data type (units): " + data_name);
		return "invalid";
	}
	
	private void resetMins()
	{
		App_EBM.log.LogMessage_High("Resetting mins");
		props.setProperty("GARAGE-MAIN-RED-MINUTES", EMPTY_MIN_60);
		props.setProperty("GARAGE-MAIN-BLACK-MINUTES", EMPTY_MIN_60);
		props.setProperty("GARAGE-PLUGS-MINUTES", EMPTY_MIN_60);
		props.setProperty("LAUNDRY-MINUTES", EMPTY_MIN_60);
	}
	
	private void resetHours()
	{
		App_EBM.log.LogMessage_High("Resetting hours");
		props.setProperty("GARAGE-MAIN-RED-HOURS", EMPTY_HR_MO_24);
		props.setProperty("GARAGE-MAIN-BLACK-HOURS", EMPTY_HR_MO_24);
		props.setProperty("GARAGE-PLUGS-HOURS", EMPTY_HR_MO_24);
		props.setProperty("LAUNDRY-HOURS", EMPTY_HR_MO_24);
	}
	
	private void resetDays()
	{
		App_EBM.log.LogMessage_High("Resetting days");
		props.setProperty("GARAGE-MAIN-RED-DAYS", EMPTY_DAY_31);
		props.setProperty("GARAGE-MAIN-BLACK-DAYS", EMPTY_DAY_31);
		props.setProperty("GARAGE-PLUGS-DAYS", EMPTY_DAY_31);
		props.setProperty("LAUNDRY-DAYS", EMPTY_DAY_31);
	}
	
	private void resetMonths()
	{
		App_EBM.log.LogMessage_High("Resetting months");
		props.setProperty("GARAGE-MAIN-RED-MONTHS", EMPTY_HR_MO_24);
		props.setProperty("GARAGE-MAIN-BLACK-MONTHS", EMPTY_HR_MO_24);
		props.setProperty("GARAGE-PLUGS-MONTHS", EMPTY_HR_MO_24);
		props.setProperty("LAUNDRY-MONTHS", EMPTY_HR_MO_24);
	}
	
	public void genDefaultDataFile()
	{
		App_EBM.log.LogMessage_High("Generating new data properties file");
		props.setProperty("YEAR", Integer.toString(Calendar.getInstance().get(Calendar.YEAR)));
		props.setProperty("MONTH", Integer.toString(Calendar.getInstance().get(Calendar.MONTH)+1));
		props.setProperty("DAY_OF_MONTH", Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_MONTH)));
		props.setProperty("DAY_OF_YEAR", Integer.toString(Calendar.getInstance().get(Calendar.DAY_OF_YEAR)));
		props.setProperty("HOUR", Integer.toString(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)));
		props.setProperty("MINUTE", Integer.toString(Calendar.getInstance().get(Calendar.MINUTE)));
		props.setProperty("SECOND", Integer.toString(Calendar.getInstance().get(Calendar.SECOND)));
		resetMonths();
		resetDays();
		resetHours();
		resetMins();
		
		// Write the data to the file
		OutputStream os;
		try {
			os = new FileOutputStream(dataFile);
			props.storeToXML(os, "Minutes are in watt-seconds, hours and days are in kW-seconds, months are kW-minutes", "UTF-8");
		} catch (FileNotFoundException e) {
			App_EBM.log.LogMessage_High("Error: Can't save newly generated props file due to file not found error.");
			e.printStackTrace();
		} catch (IOException e) {
			App_EBM.log.LogMessage_High("Error: Can't save newly generated props file.");
			e.printStackTrace();
		}
	}
	
	private String dataToString(int[] data)
	{
		String s = Integer.toString(data[0]);
		for(int i = 1; i<data.length; i++)
		{
			s += "," + Integer.toString(data[i]);
		}
		
		return s;
	}

	private int sumDataPoints(String data_name, int divisor)
	{
		String[] sa = props.getProperty(data_name).split(",");
		int sum = 0;
		int val = 0;
		for(int i=0;i<sa.length;i++)
		{
			// -1 is used for invalid data. And no point of adding 0...
			val = Integer.parseInt(sa[i]);
			if(val > 0)
				sum += val; 
		}
		sum = sum/divisor;
		return sum;
	}
}
