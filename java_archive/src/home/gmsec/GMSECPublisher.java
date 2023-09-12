package home.gmsec;

import gov.nasa.gsfc.gmsec.api.GMSEC_Exception;
import gov.nasa.gsfc.gmsec.api.Message;
import gov.nasa.gsfc.gmsec.api.mist.ConnectionManager;
import gov.nasa.gsfc.gmsec.api.util.Log;

public class GMSECPublisher {
	
	ConnectionManager connMgr;
	
	public GMSECPublisher(ConnectionManager cm)
	{
		connMgr = cm;
	}
	
	public void publish(Message m)
	{		
		String topic = m.getSubject().toString();
		Log.info("Publishing to the topic: " + topic);
		
		// TODO: Implement REPLY type message handling
		if(m.getKind() == Message.MessageKind.PUBLISH)
		{
			try {
				connMgr.publish(m); // request(message, timeout_ms [0 if no CB defined here], callback, republish_ms)
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (GMSEC_Exception e) {
				e.printStackTrace();
			}
		} else if(m.getKind() == Message.MessageKind.REPLY)
		{
			// TBD
		} else if(m.getKind() == Message.MessageKind.REQUEST)
		{
			try {
				connMgr.request(m, 0, -1); // request(message, timeout_ms [0 if no CB defined here], callback, republish_ms)
			} catch (IllegalArgumentException e) {
				e.printStackTrace();
			} catch (GMSEC_Exception e) {
				e.printStackTrace();
			}
		}
	}
}

