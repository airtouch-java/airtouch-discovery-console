package airtouch.console.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import airtouch.console.service.AirtouchHeartbeatThread.HeartbeatMinuteEventHandler;
import airtouch.console.service.AirtouchHeartbeatThread.HeartbeatSecondEventHandler;
import airtouch.console.data.AirtouchStatus;
import airtouch.console.event.AirtouchResponseEventListener;
import airtouch.console.event.AirtouchStatusEventListener;
import airtouch.v4.Request;
import airtouch.v4.Response;
import airtouch.v4.ResponseCallback;
import airtouch.v4.connector.AirtouchConnector;
import airtouch.v4.handler.AirConditionerAbilityHandler;
import airtouch.v4.handler.AirConditionerStatusHandler;
import airtouch.v4.handler.ConsoleVersionHandler;
import airtouch.v4.handler.GroupNameHandler;
import airtouch.v4.handler.GroupStatusHandler;
import airtouch.v4.model.AirConditionerAbilityResponse;
import airtouch.v4.model.ConsoleVersionResponse;
import airtouch.v4.model.GroupNameResponse;

public class AirtouchService implements AirtouchResponseEventListener {

    private final Logger log = LoggerFactory.getLogger(AirtouchService.class);

	private String hostName = System.getenv("AIRTOUCH_HOST");
	private int portNumber = 9004;
	private AirtouchConnector airtouchConnector;
	private AirtouchStatusEventListener eventListener;
	private AtomicInteger counter = new AtomicInteger(0);

	private Map<Integer,Boolean> responseReceived = new HashMap<>();


	public AirtouchService confgure(String hostName, Integer portNumber, AirtouchStatusEventListener eventListener) {
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
				eventReceived(response);
			}
		});

		this.airtouchConnector.start();
		this.requestUpdate();
		return this;
	}

	private void requestUpdate() throws IOException {
		this.responseReceived.clear();
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

	public void startHeartbeat(HeartbeatSecondEventHandler heartbeatSecondEventHandler) throws IOException {
		new AirtouchHeartbeatThread(
				heartbeatSecondEventHandler,
				new HeartbeatMinuteEventHandler() {
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
		}).start();

	}

	public void stop() {
		this.airtouchConnector.shutdown();
	}

	public int getNextCounter() {
		return this.counter.incrementAndGet();
	}

	public void sendRequest(Request request) throws IOException {
		this.airtouchConnector.sendRequest(request);
	}

	private AirtouchStatus status = new AirtouchStatus();

	public AirtouchStatus getStatus() {
		return status;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	public void eventReceived(Response response) {

		switch (response.getMessageType()) {
		case AC_STATUS:
			status.setAcStatuses(response.getData());
			break;
		case GROUP_STATUS:
			status.setGroupStatuses(response.getData());
			break;
		case GROUP_NAME:
			status.setGroupNames(
					((List<GroupNameResponse>) response.getData())
					.stream()
					.collect(Collectors.toMap(GroupNameResponse::getGroupNumber, GroupNameResponse::getName)));
			break;
		case AC_ABILITY:
			status.setAcAbilities(
					((List<AirConditionerAbilityResponse>) response.getData())
					.stream()
					.collect(Collectors.toMap(AirConditionerAbilityResponse::getAcNumber, r -> r))
					);
			break;
		case CONSOLE_VERSION:
			status.setConsoleVersion((ConsoleVersionResponse) response.getData()
					.stream()
					.findFirst()
					.orElse(null));
			break;
		case EXTENDED:
			break;
		case GROUP_CONTROL:
			break;
		default:
			break;
		}

		if (this.responseReceived.containsKey(response.getMessageId())) {
			this.responseReceived.put(response.getMessageId(), Boolean.TRUE);
		}

		if (!this.responseReceived.containsValue(Boolean.FALSE)) {
			this.eventListener.eventReceived(getStatus());
		} else {
			log.debug("Not all events received yet: {}", this.responseReceived);
		}
	}

}
