package home.gmsec;

import gov.nasa.gsfc.gmsec.api.Message;

public class MsgFactory {
	
	// This method generates a predefined DATA message with all the fields at default
	// values or as indicated by function's arguments
	public Message generateDataMessage(String device, String loc, String role, int analog0, int analog1, int analog2, int analog3)
	{
		// Alternatively, create the topic in this class if it never changes. For example:
		String messageSubject = "GMSEC." + device.toUpperCase() + "." + loc.toUpperCase() + "." + role.toUpperCase() + ".DATA";
		// E.g. "GMSEC.PI.GARAGE.POWER-MONITOR.DATA";
		
		Message m = new Message(messageSubject, Message.MessageKind.PUBLISH);
		// TODO: add your required fields
		m.addField("LOCATION", loc); // Eg. Garage
		m.addField("ROLE", role); // Eg. Power-Monitor
		m.addField("TOPIC", "DATA"); // Eg. Status
		m.addField("ANALOG0", analog0);
		m.addField("ANALOG1", analog1);
		m.addField("ANALOG2", analog2);
		m.addField("ANALOG3", analog3);
		return m;
	}
	
	// This method generates a predefined STATUS message with all the fields at default
		// values or as indicated by function's arguments
	public Message generateStatusMessage(String device, String loc, String role, int avg_loop, int sample_period)
	{
		// Alternatively, create the topic in this class if it never changes. For example:
		String messageSubject = "GMSEC." + device.toUpperCase() + "." + loc.toUpperCase() + "." + role.toUpperCase() + ".STATUS";
		// E.g. "GMSEC.PI.GARAGE.POWER-MONITOR.STATUS";
		
		Message m = new Message(messageSubject, Message.MessageKind.PUBLISH);
		// TODO: add your required fields
		m.addField("LOCATION", loc); // Eg. Garage
		m.addField("ROLE", role); // Eg. Power-Monitor
		m.addField("TOPIC", "STATUS"); // Eg. Status
		m.addField("LOOP-TIME", avg_loop);
		m.addField("SAMPLE-PERIOD", sample_period);
		return m;
	}

	// This method generates a predefined WARNING message with all the fields at default
		// values or as indicated by function's arguments
	public Message generateWarningMessage(String device, String loc, String role, String text)
	{
		// Alternatively, create the topic in this class if it never changes. For example:
		String messageSubject = "GMSEC." + device.toUpperCase() + "." + loc.toUpperCase() + "." + role.toUpperCase() + ".WARNING";
		// E.g. "GMSEC.PI.GARAGE.POWER-MONITOR.WARNING";
		
		Message m = new Message(messageSubject, Message.MessageKind.PUBLISH);
		// TODO: add your required fields
		m.addField("LOCATION", loc);
		m.addField("ROLE", role);
		m.addField("TOPIC", "WARNING");
		m.addField("MESSAGE", text);
		return m;
	}
}