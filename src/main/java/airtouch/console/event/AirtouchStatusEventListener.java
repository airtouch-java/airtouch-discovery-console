package airtouch.console.event;

import airtouch.console.data.AirtouchStatus;

public interface AirtouchStatusEventListener<T extends AirtouchStatus> {

	public void eventReceived(T status);

}
