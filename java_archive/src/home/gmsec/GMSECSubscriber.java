package home.gmsec;

//import home.App;

import gov.nasa.gsfc.gmsec.api.GMSEC_Exception;
import gov.nasa.gsfc.gmsec.api.Message;
import gov.nasa.gsfc.gmsec.api.mist.ConnectionManager;
import gov.nasa.gsfc.gmsec.api.mist.ConnectionManagerCallback;
import gov.nasa.gsfc.gmsec.api.util.Log;

/* A generic subscriber support class that needs no modification. The  
 * custom handling of messages should be done in a class that extends
 * ConnectionManagerCallback, and then an instance of that class can
 * be passed to the subscribe() function in this class.
 * */
public class GMSECSubscriber {

	public ConnectionManager connMgr = null;
	
	public GMSECSubscriber(ConnectionManager cm)
	{
		connMgr = cm;
	}
	
	public void subscribe(String topic, ConnectionManagerCallback cb)
	{
		Log.info("Subscribing to the topic: " + topic);
		try {
			
			// Subscribe to the topic and set it's callback function
			connMgr.subscribe(topic, cb);
			
			// Start the AutoDispatcher to begin asynchronously processing messages
			connMgr.startAutoDispatch();
			
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (GMSEC_Exception e) {
			e.printStackTrace();
		}	
	}	
}
