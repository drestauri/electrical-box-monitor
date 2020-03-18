package home.utils;

import java.awt.EventQueue;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Date;

/*
 * Example Message Format:
 * [HI] [Fri Sep 14 07:57:38 PDT 2018] Connected to host						// [HI], [LO], etc indicates priority level
 * [HI] [Fri Sep 14 07:57:38 PDT 2018] Sent message: <SOME TEXT HERE>		
 * [LO] [Fri Sep 14 07:57:38 PDT 2018] Closed dialog box
 * [HI] [Fri Sep 14 07:57:38 PDT 2018] Received response: <SOME TEXT HERE>		
 * 
 * Consider adding info about the module that sent the message
 */



public class DataLogger {

	// For writing to the data log
	private FileWriter m_logOutFile = null;
	private BufferedWriter m_writer = null;
	
	// For reading from the data log
	private BufferedReader m_reader = null;
	private FileReader m_logInFile = null;
	private String m_fileData = null; // 
	
	// Used to filter out lower priority messages
	private boolean m_bShowHighPriorityOnly = false;
	
	private Date m_Date = new Date();
	
	public boolean m_bIncludeTimeStamp = true;
	
	private void OpenLogFile(boolean _for_writing)
	{
		if(_for_writing)
		{
			try {
				m_logOutFile = new FileWriter("log.txt", true);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			m_writer = new BufferedWriter(m_logOutFile);
		}
		else
		{
			try {
				m_logInFile = new FileReader("log.txt");
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			}
			
			m_reader = new BufferedReader(m_logInFile);
		}
	}
	
	private void CloseLogFile()
	{
		if (m_logOutFile!=null)
		{
			try {
				m_writer.close();
				m_logOutFile.close();
			} catch (IOException e) {
					e.printStackTrace();
				}
				m_logOutFile = null;
				m_writer = null;
		}
		
		if (m_logInFile!=null)
		{
				try {
					m_reader.close();
					m_logInFile.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				m_logInFile = null;
				m_reader = null;
		}
	}
	
	public void LogMessage_Low(String s)
	{
		// Write message in log with low priority tag
		OpenLogFile(true);

		// Date objects don't update with the current time so they need to be reinitialized
		m_Date = new Date();
		
		// write [LO], append message, append new line
		if (m_logOutFile != null)
		{			
			try {
				m_writer.newLine();
				m_writer.write("[LO] [" + m_Date.toString() + "] " + s);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		CloseLogFile();
	}
	
	public void LogMessage_High(String s)
	{
		// Write message in log with high priority tag
		OpenLogFile(true);
		
		// Date objects don't update with the current time so they need to be reinitialized
		m_Date = new Date();

		// write [HI], append message, append new line
		if (m_logOutFile != null)
		{
			try {
				m_writer.newLine();
				m_writer.write("[HI] [" + m_Date.toString() + "] " + s);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		
		CloseLogFile();
	}
	
	
	public String GetLastMessage()
	{	
		String sCurrentLine="";
		String sLastLine="";
		
		// Open file for reading
		OpenLogFile(false);
		
		try {
		    while ((sCurrentLine = m_reader.readLine()) != null) {
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

