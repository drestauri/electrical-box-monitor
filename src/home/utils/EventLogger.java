package home.utils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;


/********** NOTES ***********
 * This is a COMPLETE event logger class.
 *  
 ***************************/

/********** TODO ************
 * Later:
 * > Add ability to retrieve logs (e.g. by date or date range, etc)
 ***************************/

/*
 * Example Message Format:
 * Fri Sep 14 07:57:38 PDT 2018 hostname AppName: OptionalTag: Message
 * Fri Sep 14 07:57:38 PDT 2018 ProdPi MyApp: Sent message: <SOME TEXT HERE>		
 * Fri Sep 14 07:57:38 PDT 2018 ProdPi MyApp: Closed dialog box
 * Fri Sep 14 07:57:38 PDT 2018 ProdPi MyApp: Received response: <SOME TEXT HERE>		
 * 
 */



public class EventLogger {

	// For writing to the data log
	private FileWriter log_out_file = null;
	private BufferedWriter writer = null;
	
	// For reading from the data log
	private BufferedReader reader = null;
	private FileReader log_in_file = null;
	private String file_name = "log.txt"; // set a default log file name 
	private String rot_file_name = "log.txt"; // a modified version of the filename (for rotating logs)
	private int day = 0; // the day of the week which is used to determine if rotating a log is necessary
	
	private Date date = new Date();
	
	private String hostname = "";
	private String app_name = "";
	
	//public boolean m_bIncludeTimeStamp = true;
	private boolean rotate_logs = true;
	
	public void SetFilename(String s)
	{
		file_name = s;
	}
	
	public void SetRotateLogs(boolean b)
	{
		rotate_logs = b;
	}
	
	public void SetHostName(String hn)
	{
		hostname = hn;
	}
	
	public void SetAppName(String an)
	{
		app_name = an;
	}
	
	private void OpenLogFile(boolean _for_writing)
	{
		if(rotate_logs)
			CheckFileName();
		else
			rot_file_name = file_name;
			
		if(_for_writing)
		{
			try {
				log_out_file = new FileWriter(rot_file_name, true);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			writer = new BufferedWriter(log_out_file);
		}
		else
		{
			try {
				log_in_file = new FileReader(rot_file_name);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
			reader = new BufferedReader(log_in_file);
		}
	}
	
	private void CloseLogFile()
	{
		if (log_out_file!=null)
		{
				try {
					writer.close();
					log_out_file.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				log_out_file = null;
				writer = null;
		}
		
		if (log_in_file!=null)
		{
				try {
					reader.close();
					log_in_file.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				log_in_file = null;
				reader = null;
		}
	}
	
	public void LogMessage(String msg)
	{
		LogMessageWithTag(msg, "");
	}
	
	public void LogMessageWithTag(String msg, String tag)
	{
		// Write message in log with high priority tag
		OpenLogFile(true);
		
		// Date objects don't update with the current time so they need to be reinitialized
		date = new Date();
		
		// Fri Sep 14 07:57:38 PDT 2018 hostname AppName: OptionalTag: Message
		StringBuilder sb = new StringBuilder();
		sb.append(date.toString());
		sb.append(" ");
		if(hostname != "")
		{
			sb.append(hostname);
			sb.append(" ");
		}
		if(app_name != "")
		{
			sb.append(app_name);
			sb.append(": ");
		}
		if(tag != "")
		{
			sb.append(tag);
			sb.append(": ");
		}
		sb.append(msg);

		// move to next line and write the message
		if (log_out_file != null)
		{
			try {
				writer.newLine();
				writer.write(sb.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		CloseLogFile();
	}
	
	private void CheckFileName()
	{
		// Determine the pieces of the target filename:
		// > filename
		// > day number
		// > extension
		int loc = file_name.lastIndexOf('.');
		String fn = file_name.substring(0, loc);
		String ext = file_name.substring(loc);
		int tmp_day = Calendar.getInstance().get(Calendar.DAY_OF_WEEK);
		//int tmp_day = Calendar.getInstance().get(Calendar.SECOND)/10; // FOR DEBUGGING: Rotate log every 10 seconds
		
		// Set the target file name:
		rot_file_name = fn + Integer.toString(tmp_day) + ext;
		
		// If the day has changed or we rebooted, we need to potentially rotate the log by deleting the contents of the new target log
		if(tmp_day != day)
		{
			day = tmp_day;
			
			// Check if the last entry in the log file is for today (in case we rebooted mid-day)
			// Just in case the file doesn't exist, open for writing and close it
			OpenLogFile(true);
			CloseLogFile();
			
			// EXAMPLE LOG:  Fri Sep 14 07:57:38 PDT 2018 hostname AppName: OptionalTag: Message			
			String tmp = GetLastMessage();
			String d = new Date().toString();
			d = d.substring(0,10);
			
			if(!tmp.contains(d))
			{
				// If not today, delete the contents of the target log
				// Erase the file contents
				System.out.println("DEBUG: Deleting content of:" + rot_file_name);
				try {
					BufferedWriter tmp_writer = new BufferedWriter(new FileWriter(rot_file_name));
					tmp_writer.close();
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else
			{
				System.out.println("DEBUG: Same day. Not deleting log file content");
			}
		}
	}
	
	public String GetLastMessage()
	{	
		String sCurrentLine="";
		String sLastLine="";
		
		// Open file for reading
		OpenLogFile(false);
		
		try {
		    while ((sCurrentLine = reader.readLine()) != null) {
		        //System.out.println(sCurrentLine);
				sLastLine = sCurrentLine;
		    }
		} catch (IOException e) {
		    e.printStackTrace();
		} 
		
		CloseLogFile();
		
		return sLastLine;
	}
	
	
	// IMPLEMENTATION TBD. WHEN IMPELEMENTED, CHANGE TO PUBLIC
	// Starting from the most recent message, returns the most recent message at the indicated index that meets the priority requirements
	private String GetLogMessage(int _index)
	{
		String s="";
		
		// Open log file for reading
		OpenLogFile(false);

		// TBD
		
		CloseLogFile();
		
		return s;
	}
}

