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
import airtouch.Response;
import airtouch.console.data.Airtouch5Status;
import airtouch.console.event.AirtouchResponseEventListener;
import airtouch.console.event.AirtouchStatusEventListener;
import airtouch.Request;
import airtouch.ResponseCallback;
import airtouch.v5.connector.AirtouchConnector;
import airtouch.v5.constant.MessageConstants;
import airtouch.v5.constant.MessageConstants.Address;
import airtouch.v5.handler.AirConditionerAbilityHandler;
import airtouch.v5.handler.AirConditionerStatusHandler;
import airtouch.v5.handler.ConsoleVersionHandler;
import airtouch.v5.handler.ZoneNameHandler;
import airtouch.v5.handler.ZoneStatusHandler;
import airtouch.v5.model.AirConditionerAbilityResponse;
import airtouch.v5.model.AirConditionerStatusResponse;
import airtouch.v5.model.ConsoleVersionResponse;
import airtouch.v5.model.ZoneNameResponse;
import airtouch.v5.model.ZoneStatusResponse;

public class Airtouch5Service implements AirtouchService<MessageConstants.Address>, AirtouchResponseEventListener {

    private final Logger log = LoggerFactory.getLogger(Airtouch5Service.class);

	private AirtouchConnector airtouchConnector;
	private AirtouchStatusEventListener<Airtouch5Status> eventListener;
	private AtomicInteger counter = new AtomicInteger(0);

	private Map<Integer,Boolean> responseReceived = new HashMap<>();

	private String hostName;

	private Integer portNumber;


	public Airtouch5Service confgure(String hostName, Integer portNumber, AirtouchStatusEventListener<Airtouch5Status> eventListener) {
		if (hostName != null) {
			this.hostName = hostName;
		}
		if (portNumber != null) {
			this.portNumber = portNumber;
		}
		this.eventListener = eventListener;
		return this;
	}

	public AirtouchService<Address> start() throws IOException {

		this.airtouchConnector = new AirtouchConnector(this.hostName, this.portNumber, new ResponseCallback() {
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
		if (this.counter.get() >= 120) {
			this.counter.set(0);
		}
		this.responseReceived.put(counter.incrementAndGet(), Boolean.FALSE);
		this.airtouchConnector.sendRequest(ZoneStatusHandler.generateRequest(counter.get(), null));
		this.responseReceived.put(counter.incrementAndGet(), Boolean.FALSE);
		this.airtouchConnector.sendRequest(ZoneNameHandler.generateRequest(counter.get(), null));
		this.responseReceived.put(counter.incrementAndGet(), Boolean.FALSE);
		this.airtouchConnector.sendRequest(AirConditionerStatusHandler.generateRequest(counter.get()));
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

	public void sendRequest(Request<MessageConstants.Address> request) throws IOException {
		this.airtouchConnector.sendRequest(request);
	}

	private Airtouch5Status status = new Airtouch5Status();

	public Airtouch5Status getStatus() {
		return status;
	}

	@SuppressWarnings({ "unchecked"})
	public void eventReceived(Response response) {

		switch (response.getMessageType()) {
		case "AC_STATUS":
			status.setAcStatuses((List<AirConditionerStatusResponse>) response.getData());
			break;
		case "ZONE_STATUS":
			status.setZoneStatuses((List<ZoneStatusResponse>) response.getData());
			break;
		case "ZONE_NAME":
			status.setZoneNames(
					((List<ZoneNameResponse>) response.getData())
					.stream()
					.collect(Collectors.toMap(ZoneNameResponse::getZoneNumber, ZoneNameResponse::getName)));
			break;
		case "AC_ABILITY":
			status.setAcAbilities(
					((List<AirConditionerAbilityResponse>) response.getData())
					.stream()
					.collect(Collectors.toMap(AirConditionerAbilityResponse::getAcNumber, r -> r))
					);
			break;
		case "CONSOLE_VERSION":
			status.setConsoleVersion((ConsoleVersionResponse) response.getData()
					.stream()
					.findFirst()
					.orElse(null));
			break;
		case "EXTENDED":
			break;
		case "CONTROL_OR_STATUS":
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
