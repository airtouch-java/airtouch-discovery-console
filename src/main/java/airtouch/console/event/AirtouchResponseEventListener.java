package airtouch.console.event;

import airtouch.Response;

public interface AirtouchResponseEventListener<T> {

	public void eventReceived(Response<T> response);

}
