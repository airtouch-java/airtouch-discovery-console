package airtouch.console.service;

import java.io.IOException;

import airtouch.Request;
import airtouch.console.service.AirtouchHeartbeatThread.HeartbeatSecondEventHandler;

public interface AirtouchService<T,A> {

	void startHeartbeat(HeartbeatSecondEventHandler heartbeatSecondEventHandler) throws IOException;

	int getNextCounter();

	void sendRequest(Request<T,A> build) throws IOException;

	void stop();

}
