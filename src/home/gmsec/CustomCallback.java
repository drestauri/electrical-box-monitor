package home.gmsec;

import home.App;

import gov.nasa.gsfc.gmsec.api.Message;
import gov.nasa.gsfc.gmsec.api.mist.ConnectionManager;
import gov.nasa.gsfc.gmsec.api.mist.ConnectionManagerCallback;
import gov.nasa.gsfc.gmsec.api.util.Log;

// Reproduce this class in its own Java file for each
// message that needs unique handling

public class CustomCallback extends ConnectionManagerCallback{

	@Override
	public void onMessage(ConnectionManager cm, Message msg) {
		Log.info("[ExampleCallback::onMessage] Received:\n" + msg.toXML());
		
		// Example calling code outside of the main App class without including it directly
		// I wonder if an interface could be used here…
		//App.getOtherModule().handler("received message");
		
		// Set response received
		// TODO: Change this to do whatever you want when the message is received
		//App.setFinished(true);		
	}


}