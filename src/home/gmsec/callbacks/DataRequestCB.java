package home.gmsec.callbacks;

import home.App_EBM;
import gov.nasa.gsfc.gmsec.api.GMSEC_Exception;
import gov.nasa.gsfc.gmsec.api.Message;
import gov.nasa.gsfc.gmsec.api.mist.ConnectionManager;
import gov.nasa.gsfc.gmsec.api.mist.ConnectionManagerCallback;
//import gov.nasa.gsfc.gmsec.api.util.Log;

// Reproduce this class in its own Java file for each
// message that needs unique handling

public class DataRequestCB extends ConnectionManagerCallback{

	@Override
	public void onMessage(ConnectionManager cm, Message msg) {
		//Log.info("[ExampleCallback::onMessage] Received:\n" + msg.toXML());
		// This callback is called when we receive a request for the long term data.
		// The seconds data is sent as a single point every second and is not stored locally.
		// The minute, hour, day, and month data is generated and stored at roll over timing
		// but not broadcast unless requested
		try {
			App_EBM.sendFullData(msg.getStringValue("DATA-NAME"));
		} catch (GMSEC_Exception e) {
			e.printStackTrace();
		}
	}


}