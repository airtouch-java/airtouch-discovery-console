package airtouch.console.event;

import airtouch.v4.Response;

public interface AirtouchResponseEventListener {

	public void eventReceived(Response<?> response);

}
