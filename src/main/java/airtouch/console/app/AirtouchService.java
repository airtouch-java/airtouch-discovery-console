package airtouch.console.app;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import airtouch.console.app.AirtouchHeartbeatThread.HeartbeatMinuteEventHandler;
import airtouch.console.app.AirtouchHeartbeatThread.HeartbeatSecondEventHandler;
import airtouch.v4.Response;
import airtouch.v4.ResponseCallback;
import airtouch.v4.connector.AirtouchConnector;
import airtouch.v4.handler.AirConditionerAbilityHandler;
import airtouch.v4.handler.AirConditionerStatusHandler;
import airtouch.v4.handler.ConsoleVersionHandler;
import airtouch.v4.handler.GroupNameHandler;
import airtouch.v4.handler.GroupStatusHandler;

public class AirtouchService {

    private final Logger log = LoggerFactory.getLogger(AirtouchService.class);

	private String hostName = System.getenv("AIRTOUCH_HOST");
	private int portNumber = 9004;
	private AirtouchConnector airtouchConnector;
	private AirtouchServiceEventListener eventListener;
	private AirtouchHeartbeatThread heartbeatThread;
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
		this.requestUpdate();
		return this;
	}

	private void requestUpdate() throws IOException {
		this.airtouchConnector.sendRequest(GroupStatusHandler.generateRequest(counter.incrementAndGet(), null));
		this.airtouchConnector.sendRequest(GroupNameHandler.generateRequest(counter.incrementAndGet(), null));
		this.airtouchConnector.sendRequest(AirConditionerStatusHandler.generateRequest(counter.incrementAndGet(), null));
		this.airtouchConnector.sendRequest(ConsoleVersionHandler.generateRequest(counter.incrementAndGet()));
		this.airtouchConnector.sendRequest(AirConditionerAbilityHandler.generateRequest(counter.incrementAndGet(), null));
	}

	public AirtouchService startHeartbeat(HeartbeatSecondEventHandler heartbeatSecondEventHandler) throws IOException {
		this.heartbeatThread = new AirtouchHeartbeatThread(heartbeatSecondEventHandler, new HeartbeatMinuteEventHandler() {
			@Override
			public void handleMinuteEvent() {
				try {
					requestUpdate();
				} catch (IOException e) {
					log.warn(e.getMessage());
					try {
						airtouchConnector.shutdown();
					} catch (Exception ex) {
						log.warn("Failed to shutdown airtouchConnector: {}", e.getMessage(), e);
					}

					try {
						airtouchConnector.start();
					} catch (Exception ex) {
						log.warn("Failed to start airtouchConnector: {}", e.getMessage(), e);
					}
				}
			}
		});
		this.heartbeatThread.start();
		return this;
	}

	public void stop() {
		this.airtouchConnector.shutdown();
	}


}
