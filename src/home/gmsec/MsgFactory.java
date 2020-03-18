package home.gmsec;

import gov.nasa.gsfc.gmsec.api.Message;

public class MsgFactory {

	// TODO: Recreate this method for as many predefined message as you want, if you want.
	
	// This method should generate a predefined message with all the fields at default
	// values or as indicated by function's arguments
	public Message generateMessage(String topic, int value)
	{
		// Alternatively, create the topic in this class if it never changes. For example:
		//String messageSubject = "GMSEC.PI." + user.toUpperCase() + ".REQ.DIR.VEHICLE.MOVE";
		
		Message m = new Message(topic, Message.MessageKind.PUBLISH);
		// TODO: add your required fields
		m.addField("Value", value);
		
		return m;
	}
}