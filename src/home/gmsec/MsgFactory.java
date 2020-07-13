package home.gmsec;

import gov.nasa.gsfc.gmsec.api.Message;

public class MsgFactory {
	
	// This method generates a single data point message that is sent every second
	public Message generateSingleDataMessage(String device, String role, String loc, int second, int garage_main_red, int garage_main_black, int garage_plugs, int laundry, String units)
	{
		//GMSEC.<SOURCE-DEVICE>.<SOURCE-APP>.<SOURCE-LOCATION>.<TYPE>.<SUBTYPE>.{DEST-DEVICE}.{DEST-APP}.{DEST-LOCATION}
		String messageSubject = "GMSEC." + device.toUpperCase() + "." + role.toUpperCase() + "." + loc.toUpperCase() + ".DATA.SINGLE";
		// E.g. "GMSEC.PI.POWER-MONITOR.GARAGE.DATA.SINGLE";
		
		Message m = new Message(messageSubject, Message.MessageKind.PUBLISH);
		m.addField("DEVICE", device); // Eg. PI
		m.addField("LOCATION", loc); // Eg. Garage
		m.addField("ROLE", role); // Eg. Power-Monitor
		m.addField("TYPE", "DATA"); // Eg. Data, status, etc
		m.addField("SUB-TYPE", "SINGLE"); // Eg. single, long term
		m.addField("SECOND", second);
		m.addField("GARAGE-MAIN-RED", garage_main_red);
		m.addField("GARAGE-MAIN-BLACK", garage_main_black);
		m.addField("GARAGE-PLUGS", garage_plugs);
		m.addField("LAUNDRY", laundry);
		m.addField("UNITS", units);
		return m;
	}

	// This method generates a long term data message that is sent only in response to a request for the data
	public Message generateDataMessage(String device, String role, String loc, String data_name, String data, String req_id, String sec, String min, String hr, String day, String mon, String yr, String units)
	{// device, role, loc, name, data, req, sec, min, hr, d,m,y
		//GMSEC.<SOURCE-DEVICE>.<SOURCE-APP>.<SOURCE-LOCATION>.<TYPE>.<SUBTYPE>.{DEST-DEVICE}.{DEST-APP}.{DEST-LOCATION}
				String messageSubject = "GMSEC." + device.toUpperCase() + "." + role.toUpperCase() + "." + loc.toUpperCase() + ".DATA.LONG";
				// E.g. "GMSEC.PI.POWER-MONITOR.GARAGE.DATA.SINGLE";
		
		Message m = new Message(messageSubject, Message.MessageKind.PUBLISH);
		m.addField("DEVICE", device); // Eg. PI
		m.addField("LOCATION", loc); // Eg. Garage
		m.addField("ROLE", role); // Eg. Power-Monitor
		m.addField("TYPE", "DATA"); // Eg. Status
		m.addField("SUB-TYPE", "LONG"); // Eg. single, long term
		m.addField("DATA-NAME", data_name);
		m.addField("DATA", data);
		m.addField("UNITS", units);
		m.addField("REQUEST-ID", req_id);
		m.addField("SECOND", sec);
		m.addField("MINUTE", min);
		m.addField("HOUR", hr);
		m.addField("DAY", day);
		m.addField("MONTH", mon);
		m.addField("YEAR", yr);
		m.addField("UNITS", units);
		return m;
	}
	
	// This method generates a simple status message that is sent every second
	public Message generateStatusMessage(String soft_version, String device, String role, String loc, String run_duration, int avg_loop, int sample_period)
	{
		//GMSEC.<SOURCE-DEVICE>.<SOURCE-APP>.<SOURCE-LOCATION>.<TYPE>.<SUBTYPE>.{DEST-DEVICE}.{DEST-APP}.{DEST-LOCATION}
				String messageSubject = "GMSEC." + device.toUpperCase() + "." + role.toUpperCase() + "." + loc.toUpperCase() + ".STATUS";
				// E.g. "GMSEC.PI.POWER-MONITOR.GARAGE.DATA.SINGLE";
		
		Message m = new Message(messageSubject, Message.MessageKind.PUBLISH);
		m.addField("DEVICE", device); // Eg. PI
		m.addField("LOCATION", loc); // Eg. Garage
		m.addField("ROLE", role); // Eg. Power-Monitor
		m.addField("TYPE", "STATUS"); // Eg. Status
		//m.addField("SUB-TYPE", "FULL"); // Eg. single, long term
		m.addField("LOOP-TIME", avg_loop);
		m.addField("SAMPLE-PERIOD", sample_period);
		m.addField("SOFTWARE-VERSION", soft_version);
		m.addField("RUN-DURATION", run_duration);
		return m;
	}

	// This method generates a long term data message that is sent only in response to a request for the data
	public Message generateWarningMessage(String device, String role, String loc, String text)
	{
		//GMSEC.<SOURCE-DEVICE>.<SOURCE-APP>.<SOURCE-LOCATION>.<TYPE>.<SUBTYPE>.{DEST-DEVICE}.{DEST-APP}.{DEST-LOCATION}
		String messageSubject = "GMSEC." + device.toUpperCase() + "." + role.toUpperCase() + "." + loc.toUpperCase() + ".WARNING";
		// E.g. "GMSEC.PI.POWER-MONITOR.GARAGE.DATA.SINGLE";
		
		Message m = new Message(messageSubject, Message.MessageKind.PUBLISH);
		m.addField("DEVICE", device); // Eg. PI
		m.addField("LOCATION", loc);
		m.addField("ROLE", role);
		m.addField("TYPE", "WARNING");
		//m.addField("SUB-TYPE", "FULL"); // Eg. single, long term
		m.addField("MESSAGE", text);
		return m;
	}
}