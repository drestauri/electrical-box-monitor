package home.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.InvalidPropertiesFormatException;
import java.util.Properties;

//import home.App;

/********** NOTES ***********
 * This is a COMPLETE configuration loader class.
 * It is to be used to load or create a default .properties
 * file, provide a few simple wrappers on set/getProperty,
 * and make other useful functionality for loading/saving
 * available to the main application.  
 *  
 ***************************/

/********** TODO ************
 *		
 ***************************/

public class AppConfiguration {
	private String filename;
	private Properties config;
	private String comment;
	private boolean auto_save = true;
	
	// Default constructor
	public AppConfiguration()
	{
		this("config.properties"); // Use the default filename
	}
	
	// Allow user to specify the filename
	public AppConfiguration(String fn)
	{

		config = new Properties();
		filename = fn;
		
		File config_file;
		config_file = new File(filename);
		
		// Perform some quick checks on the file
		if(!config_file.exists())
		{
			// TODO: If the file doesn't exist, generate a new file
			//App.log.LogMessage_High("Configuration file not found");
		}else if(config_file.length() < 10)
		{
			// Or if the file appears to be invalid/blank, take some action?
			//App.log.LogMessage_High("Configuration file empty or invalid");
		}
		
		InputStream in_stream;
		try {
			in_stream = new FileInputStream(config_file);
			if (in_stream!=null)
				config.loadFromXML(in_stream);
		} catch (FileNotFoundException e) {
			System.out.println("Config file not found. Generating Config.");
			GenerateConfigFile(filename);
			//App.log.LogMessage_High("Error: Config file not found after checking and generating new file");
			//e.printStackTrace();
		} catch (InvalidPropertiesFormatException e) {
			System.out.println("Config file invalid");
			//App.log.LogMessage_High("Error: Config file invalid after checking and generating new file");
			//e.printStackTrace();
		} catch (IOException e) {
			System.out.println("Config file IO error");
			//App.log.LogMessage_High("Error: Config file IO error after checking and generating new file");
			//e.printStackTrace();
		}
	}
	
	public void GenerateConfigFile()
	{
		GenerateConfigFile("config.properties");
	}
	
	public void GenerateConfigFile(String name)
	{
		// Add a couple default config settings
		config.setProperty("HOSTNAME", "default_hostname");
		config.setProperty("APP_NAME", "default_appname");
		//GMSEC.<SOURCE-DEVICE>.<SOURCE-APP>.<SOURCE-LOCATION>.<TYPE>.<SUBTYPE>.{DEST-DEVICE}.{DEST-APP}.{DEST-LOCATION}
		config.setProperty("HEARTBEAT_TOPIC", "GMSEC.DEVICE.APP.LOCATION.MSG.HEARTBEAT");
		
		save("New config file");
	}
	
	public void save()
	{
		save(comment);
	}

	public void save(String cmt)
	{
		comment = cmt;
		// Generate the specified file
		File config_file;
		config_file = new File(filename);
		OutputStream out_stream;
		try {
			out_stream = new FileOutputStream(config_file);
			if (out_stream!=null)
				config.storeToXML(out_stream, comment);
		} catch (IOException e) {
			System.out.println("Error saving config file");
		}
	}
	
	public void saveAs(String fn)
	{
		filename = fn;
		save();
	}
	
	public String getPropertyAsString(String prop)
	{
		return config.getProperty(prop,"");
	}

	public void disableAutoSave()
	{
		auto_save = false;
	}
	
	public void enableAutoSave()
	{
		auto_save = true;
	}
	
	public void setComment(String cmt)
	{
		comment = cmt;
		if(auto_save)
			save();
	}
	
	public int getPropertyAsInt(String prop)
	{
		String s = config.getProperty(prop,"");
		int i = 0;
		try {
			i = Integer.parseInt(s);
		}
		catch (NumberFormatException e){
			System.out.println("WARNING: Error converting property \"" + prop + "\" to integer. Returning 0");
		}
		return i;
	}
	
	public void setProperty(String name, int val)
	{
		setProperty(name, Integer.toString(val));
		
		if(auto_save)
			save();
	}
	
	public void setProperty(String name, String val)
	{
		config.setProperty(name, val);
		
		if(auto_save)
			save();
	}
}
