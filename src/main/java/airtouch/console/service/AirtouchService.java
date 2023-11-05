package airtouch.console.service;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import airtouch.AirtouchVersion;
import airtouch.Request;
import airtouch.Response;
import airtouch.connector.AirtouchConnector;
import airtouch.console.data.AirtouchStatus;
import airtouch.console.event.AirtouchStatusEventListener;
import airtouch.console.service.AirtouchHeartbeatThread.HeartbeatMinuteEventHandler;
import airtouch.console.service.AirtouchHeartbeatThread.HeartbeatSecondEventHandler;
import airtouch.model.AirConditionerAbilityResponse;
import airtouch.model.AirConditionerStatusResponse;
import airtouch.model.ConsoleVersionResponse;
import airtouch.model.ZoneNameResponse;
import airtouch.model.ZoneStatusResponse;

public abstract class AirtouchService<T> {

    private final Logger log = LoggerFactory.getLogger(AirtouchService.class);

	protected AirtouchConnector<T> airtouchConnector;
	private AirtouchStatusEventListener<AirtouchStatus> eventListener;
	protected AtomicInteger counter = new AtomicInteger(0);

	protected Map<Integer,Boolean> responseReceived = new HashMap<>();

	protected String hostName;

	protected Integer portNumber;

	private AirtouchVersion airtouchVersion;


	public AirtouchService<T> confgure(AirtouchVersion airtouchVersion, String hostName, Integer portNumber, AirtouchStatusEventListener<AirtouchStatus> eventListener) {
		this.airtouchVersion = airtouchVersion;
		if (hostName != null) {
			this.hostName = hostName;
		}
		if (portNumber != null) {
			this.portNumber = portNumber;
		}
		this.eventListener = eventListener;
		return this;
	}

	
	
	protected abstract void requestUpdate() throws IOException;
	public abstract AirtouchService<T> start() throws IOException;


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

	public void sendRequest(Request<T> request) throws IOException {
		this.airtouchConnector.sendRequest(request);
	}

	private AirtouchStatus status = new AirtouchStatus();

	public AirtouchStatus getStatus() {
		return this.status;
	}
	
	public AirtouchVersion getAirtouchVersion() {
		return this.airtouchVersion;
	}

	@SuppressWarnings({ "unchecked"})
	public void eventReceived(Response response) {

		switch (response.getMessageType()) {
		case AC_STATUS:
			status.setAcStatuses((List<AirConditionerStatusResponse>) response.getData());
			break;
		case ZONE_STATUS:
			status.setZoneStatuses((List<ZoneStatusResponse>) response.getData());
			break;
		case ZONE_NAME:
			status.setZoneNames(
					((List<ZoneNameResponse>) response.getData())
					.stream()
					.collect(Collectors.toMap(ZoneNameResponse::getZoneNumber, ZoneNameResponse::getName)));
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
		case ZONE_CONTROL:
			break;
		default:
			break;
		}

		if (this.responseReceived.containsKey(response.getMessageId())) {
			this.responseReceived.put(response.getMessageId(), Boolean.TRUE);
		}

		if (!this.responseReceived.containsValue(Boolean.FALSE)) {
			log.debug("Expected events received: {}. Sending update to listeners.", this.responseReceived);
			this.eventListener.eventReceived(getStatus());
		} else {
			log.debug("Not all events received yet: {}", this.responseReceived);
		}
	}




}
