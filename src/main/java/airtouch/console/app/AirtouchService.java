package airtouch.console.app;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import airtouch.v4.Response;
import airtouch.v4.ResponseCallback;
import airtouch.v4.connector.AirtouchConnector;
import airtouch.v4.handler.AirConditionerStatusHandler;
import airtouch.v4.handler.GroupNameHandler;
import airtouch.v4.handler.GroupStatusHandler;

public class AirtouchService {

	private String hostName = System.getenv("AIRTOUCH_HOST");
	private int portNumber = 9004;
	private AirtouchConnector airtouchConnector;
	private AirtouchServiceEventListener eventListener;
	private AirtouchStatus airtouchStatus;
	private AtomicInteger counter = new AtomicInteger(0);


	public AirtouchService confgure(String hostName, Integer portNumber, AirtouchServiceEventListener eventListener) {
		if (hostName != null) {
			this.hostName = hostName;
		}
		if (portNumber != null) {
			this.portNumber = portNumber;
		}
		this.eventListener = eventListener;
		return this;
	}

	public AirtouchService start() throws IOException {

		this.airtouchConnector = new AirtouchConnector(this.hostName, this.portNumber, new ResponseCallback() {
			@SuppressWarnings("rawtypes")
			public void handleResponse(Response response) {
				eventListener.eventReceived(response);
			}
		});

		this.airtouchConnector.start();
		this.airtouchConnector.sendRequest(GroupStatusHandler.generateRequest(counter.incrementAndGet(), null));
		this.airtouchConnector.sendRequest(GroupNameHandler.generateRequest(counter.incrementAndGet(), null));
		this.airtouchConnector.sendRequest(AirConditionerStatusHandler.generateRequest(counter.incrementAndGet(), null));
		this.airtouchConnector.sendRequest(AirConditionerStatusHandler.generateRequest(counter.incrementAndGet(), null));
		return this;
	}

	public void stop() {
		this.airtouchConnector.shutdown();
	}
}
