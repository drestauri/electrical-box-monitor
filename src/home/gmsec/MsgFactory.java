package home.gmsec;

import gov.nasa.gsfc.gmsec.api.Message;

public class MsgFactory {
	
	// This method generates a single data point message that is sent every second
	public Message generateSingleDataMessage(String device, String role, String loc, int second, int garage_main, int garage_plugs, int laundry)
	{
		//GMSEC.<SOURCE-DEVICE>.<SOURCE-APP>.<SOURCE-LOCATION>.<TYPE>.<SUBTYPE>.{DEST-DEVICE}.{DEST-APP}.{DEST-LOCATION}
		String messageSubject = "GMSEC." + device.toUpperCase() + "." + role.toUpperCase() + "." + loc.toUpperCase() + ".DATA.SINGLE";
		// E.g. "GMSEC.PI.POWER-MONITOR.GARAGE.DATA.SINGLE";
		
		Message m = new Message(messageSubject, Message.MessageKind.PUBLISH);
		m.addField("LOCATION", loc); // Eg. Garage
		m.addField("ROLE", role); // Eg. Power-Monitor
		m.addField("TYPE", "DATA"); // Eg. Data, status, etc
		m.addField("SUB-TYPE", "SINGLE"); // Eg. single, long term
		m.addField("SECOND", second);
		m.addField("GARAGE-MAIN", garage_main);
		m.addField("GARAGE-PLUGS", garage_plugs);
		m.addField("LAUNDRY", laundry);
		return m;
	}

	// This method generates a long term data message that is sent only in response to a request for the data
	public Message generateDataMessage(String device, String role, String loc, String data_name, String data)
	{
		//GMSEC.<SOURCE-DEVICE>.<SOURCE-APP>.<SOURCE-LOCATION>.<TYPE>.<SUBTYPE>.{DEST-DEVICE}.{DEST-APP}.{DEST-LOCATION}
				String messageSubject = "GMSEC." + device.toUpperCase() + "." + role.toUpperCase() + "." + loc.toUpperCase() + ".DATA.LONG";
				// E.g. "GMSEC.PI.POWER-MONITOR.GARAGE.DATA.SINGLE";
		
		Message m = new Message(messageSubject, Message.MessageKind.PUBLISH);
		m.addField("LOCATION", loc); // Eg. Garage
		m.addField("ROLE", role); // Eg. Power-Monitor
		m.addField("TYPE", "DATA"); // Eg. Status
		m.addField("SUB-TYPE", "LONG"); // Eg. single, long term
		m.addField("DATA-NAME", data_name);
		m.addField("DATA", data);
		return m;
	}
	
	// This method generates a simple status message that is sent every second
	public Message generateStatusMessage(String soft_version, String device, String role, String loc, int avg_loop, int sample_period)
	{
		//GMSEC.<SOURCE-DEVICE>.<SOURCE-APP>.<SOURCE-LOCATION>.<TYPE>.<SUBTYPE>.{DEST-DEVICE}.{DEST-APP}.{DEST-LOCATION}
				String messageSubject = "GMSEC." + device.toUpperCase() + "." + role.toUpperCase() + "." + loc.toUpperCase() + ".STATUS";
				// E.g. "GMSEC.PI.POWER-MONITOR.GARAGE.DATA.SINGLE";
		
		Message m = new Message(messageSubject, Message.MessageKind.PUBLISH);
		m.addField("LOCATION", loc); // Eg. Garage
		m.addField("ROLE", role); // Eg. Power-Monitor
		m.addField("TYPE", "STATUS"); // Eg. Status
		//m.addField("SUB-TYPE", "FULL"); // Eg. single, long term
		m.addField("LOOP-TIME", avg_loop);
		m.addField("SAMPLE-PERIOD", sample_period);
		m.addField("SOFTWARE-VERSION", soft_version);
		return m;
	}

	// This method generates a long term data message that is sent only in response to a request for the data
	public Message generateWarningMessage(String device, String role, String loc, String text)
	{
		//GMSEC.<SOURCE-DEVICE>.<SOURCE-APP>.<SOURCE-LOCATION>.<TYPE>.<SUBTYPE>.{DEST-DEVICE}.{DEST-APP}.{DEST-LOCATION}
		String messageSubject = "GMSEC." + device.toUpperCase() + "." + role.toUpperCase() + "." + loc.toUpperCase() + ".WARNING";
		// E.g. "GMSEC.PI.POWER-MONITOR.GARAGE.DATA.SINGLE";
		
		Message m = new Message(messageSubject, Message.MessageKind.PUBLISH);
		m.addField("LOCATION", loc);
		m.addField("ROLE", role);
		m.addField("TYPE", "WARNING");
		//m.addField("SUB-TYPE", "FULL"); // Eg. single, long term
		m.addField("MESSAGE", text);
		return m;
	}
}