package airtouch.console.service;

import java.io.IOException;
import java.time.LocalDateTime;
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
import airtouch.constant.AirConditionerControlConstants.AcPower;
import airtouch.constant.AirConditionerControlConstants.Mode;
import airtouch.constant.ZoneControlConstants.ZoneControl;
import airtouch.constant.ZoneControlConstants.ZonePower;
import airtouch.constant.ZoneControlConstants.ZoneSetting;
import airtouch.model.AirConditionerAbilityResponse;
import airtouch.model.AirConditionerStatusResponse;
import airtouch.model.ConsoleVersionResponse;
import airtouch.model.ZoneNameResponse;
import airtouch.model.ZoneStatusResponse;

public abstract class AirtouchService<T> {

    private final Logger log = LoggerFactory.getLogger(AirtouchService.class);

	protected AirtouchConnector<T> airtouchConnector;
	protected AirtouchStatusEventListener<AirtouchStatus> eventListener;
	protected AtomicInteger counter = new AtomicInteger(0);

	protected Map<Integer,Boolean> responseReceived = new HashMap<>();
	protected boolean bootStrapDone = false;

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
					.map(z ->zoneRenamer(z))
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
		
		status.setLastUpdate(LocalDateTime.now());
		
		if (this.responseReceived.containsKey(response.getMessageId())) {
			this.responseReceived.put(response.getMessageId(), Boolean.TRUE);
		}

		if (!this.responseReceived.containsValue(Boolean.FALSE)) {
			log.debug("Expected events received: {}. Sending update to listeners.", this.responseReceived);
			this.eventListener.eventReceived(getStatus());
			if (!bootStrapDone) {
				bootStrapDone = true;
				this.eventListener.bootStrapEventReceived(getStatus());
			}
		} else {
			log.debug("Not all events received yet: {}", this.responseReceived);
		}
	}

	private ZoneNameResponse zoneRenamer(ZoneNameResponse z) {
		if (System.getenv("ZONE_" + z.getName().toUpperCase()) != null) {
			z.setName(System.getenv("ZONE_" + z.getName().toUpperCase()));
		}
		return z;
	}

	protected ZoneControl determineZoneControl(String groupControlStr) {
		return "temperature".equalsIgnoreCase(groupControlStr) ? ZoneControl.TEMPERATURE_CONTROL : ZoneControl.PERCENTAGE_CONTROL;
	}

	protected int determineAndValidateSettingValue(ZoneSetting zoneSetting, String settingValue) {
		int value = Integer.parseInt(settingValue);
		if (ZoneSetting.SET_TARGET_SETPOINT.equals(zoneSetting) && isValidTemperatureSetPoint(value)) {
			return value;
		} else if (ZoneSetting.SET_OPEN_PERCENTAGE.equals(zoneSetting) && isValidOpenPercentage(value)){
			return value;
		} else {
			throw new IllegalArgumentException("Value is outside allowable range.");
		}
	}
	
	protected ZonePower determineGroupPower(String groupPowerStr) {
		switch (groupPowerStr.toLowerCase()) {
		case "on":
			return ZonePower.POWER_ON;
		case "off":
			return ZonePower.POWER_OFF;
		case "turbo":
			return ZonePower.TURBO_POWER;
		default:
			return ZonePower.NO_CHANGE;
		}
	}

	private boolean isValidOpenPercentage(int value) {
		return value >= 0 && value <= 100 && value % 5 == 0;
	}

	private boolean isValidTemperatureSetPoint(int value) {
		return true; // TODD: Fix impl
	}

	protected int resolveNameToZoneIndex(String zoneName) throws UnresolvableZoneNameException {
		return this.status.getZoneNames().entrySet().stream()
				.filter(e -> e.getValue().equalsIgnoreCase(zoneName))
				.findFirst()
				.map(e -> e.getKey())
				.orElseThrow(() -> new UnresolvableZoneNameException(String.format("Unable to resolve '%s' to a valid zone", zoneName)));
	}
	
	protected AcPower determineAcPower(String acPowerStr) {
		return "on".equalsIgnoreCase(acPowerStr) ? AcPower.POWER_ON : AcPower.POWER_OFF;
	}

	protected Mode determineAcMode(String acModeStr) {
		switch (acModeStr.toLowerCase()) {
		case "auto":
			return Mode.AUTO;
		case "cool":
			return Mode.COOL;
		case "dry":
			return Mode.DRY;
		case "fan":
			return Mode.FAN;
		case "heat":
			return Mode.HEAT;
		default:
			return Mode.NO_CHANGE;
		}
	}
	
	public abstract void handleAcInput(List<String> commandParams) throws NumberFormatException, IOException;
	public abstract void handleZoneInput(List<String> commandParams) throws NumberFormatException, IOException;
}
