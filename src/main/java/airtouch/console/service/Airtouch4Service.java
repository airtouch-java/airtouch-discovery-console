package airtouch.console.service;

import java.io.IOException;

import airtouch.Response;
import airtouch.ResponseCallback;
import airtouch.connector.AirtouchConnector;
import airtouch.connector.AirtouchConnectorThreadFactory;
import airtouch.v4.connector.Airtouch4ConnectorThreadFactory;
import airtouch.v4.constant.MessageConstants;
import airtouch.v4.handler.AirConditionerAbilityHandler;
import airtouch.v4.handler.AirConditionerStatusHandler;
import airtouch.v4.handler.ConsoleVersionHandler;
import airtouch.v4.handler.GroupNameHandler;
import airtouch.v4.handler.GroupStatusHandler;

public class Airtouch4Service extends AirtouchService<MessageConstants.Address> {

	protected void requestUpdate() throws IOException {
		this.responseReceived.clear();
		if (this.counter.get() >= 120) {
			this.counter.set(0);
		}
		this.responseReceived.put(counter.incrementAndGet(), Boolean.FALSE);
		this.airtouchConnector.sendRequest(GroupStatusHandler.generateRequest(counter.get(), null));
		this.responseReceived.put(counter.incrementAndGet(), Boolean.FALSE);
		this.airtouchConnector.sendRequest(GroupNameHandler.generateRequest(counter.get(), null));
		this.responseReceived.put(counter.incrementAndGet(), Boolean.FALSE);
		this.airtouchConnector.sendRequest(AirConditionerStatusHandler.generateRequest(counter.get(), null));
		this.responseReceived.put(counter.incrementAndGet(), Boolean.FALSE);
		this.airtouchConnector.sendRequest(ConsoleVersionHandler.generateRequest(counter.get()));
		this.responseReceived.put(counter.incrementAndGet(), Boolean.FALSE);
		this.airtouchConnector.sendRequest(AirConditionerAbilityHandler.generateRequest(counter.get(), null));
	}

	@Override
	public AirtouchService<MessageConstants.Address> start() throws IOException {

		AirtouchConnectorThreadFactory threadFactory = new Airtouch4ConnectorThreadFactory();
		this.airtouchConnector = new AirtouchConnector<MessageConstants.Address>(threadFactory, this.hostName, this.portNumber, new ResponseCallback() {
			public void handleResponse(Response response) {
				eventReceived(response);
			}
		});
		this.airtouchConnector.start();
		this.requestUpdate();
		return this;
	}

}
