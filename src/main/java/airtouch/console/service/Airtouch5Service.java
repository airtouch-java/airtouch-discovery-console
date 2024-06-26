package airtouch.console.service;

import java.io.IOException;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import airtouch.connector.AirtouchConnector;
import airtouch.connector.AirtouchConnectorThreadFactory;
import airtouch.constant.ZoneControlConstants.ZoneSetting;
import airtouch.v5.connector.Airtouch5ConnectorThreadFactory;
import airtouch.v5.constant.MessageConstants;
import airtouch.v5.handler.AirConditionerAbilityHandler;
import airtouch.v5.handler.AirConditionerControlHandler;
import airtouch.v5.handler.AirConditionerStatusHandler;
import airtouch.v5.handler.ConsoleVersionHandler;
import airtouch.v5.handler.ZoneControlHandler;
import airtouch.v5.handler.ZoneNameHandler;
import airtouch.v5.handler.ZoneStatusHandler;

public class Airtouch5Service extends AirtouchService<MessageConstants.Address> {
	
    private final Logger log = LoggerFactory.getLogger(Airtouch5Service.class);


	protected void requestUpdate() throws IOException {
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


	@Override
	public AirtouchService<MessageConstants.Address> start() throws IOException {

		AirtouchConnectorThreadFactory threadFactory = new Airtouch5ConnectorThreadFactory();
		this.airtouchConnector = new AirtouchConnector<MessageConstants.Address>(threadFactory, this.hostName, this.portNumber, this::eventReceived);
		this.airtouchConnector.start();
		this.requestUpdate();
		return this;
	}

	@Override
	public void handleAcInput(List<String> list) throws IOException {
		// ac 0/1/2/3 power on/off
		switch (list.get(2).toLowerCase()) {
		case "power":
			this.sendRequest(
				AirConditionerControlHandler.requestBuilder()
				.acNumber(Integer.valueOf(list.get(1)))
				.acPower(determineAcPower(list.get(3)))
				.build(this.getNextCounter()));
			break;
		case "mode":
			this.sendRequest(
					AirConditionerControlHandler.requestBuilder()
					.acNumber(Integer.valueOf(list.get(1)))
					.acMode(determineAcMode(list.get(3)))
					.build(this.getNextCounter()));
			break;
		}
	}
	
	@Override
	public void handleZoneInput(List<String> commandParams) throws NumberFormatException, IOException {
		// zone 0/1/2/3 target-temp temp
		// zone 0/1/2/3 power on/off
		// zone 0/1/2/3 control temperature/percentage
		int zoneIndex = -1;
		try {
			zoneIndex = resolveNameToZoneIndex(commandParams.get(1));
		} catch (UnresolvableZoneNameException ex) {
			this.getStatus().setUserError(ex.getMessage());
			eventListener.eventReceived(getStatus());
			log.debug("User error: {}", ex.getMessage());
			return;
		}
		switch (commandParams.get(2).toLowerCase()) {
		case "target-temp":
				sendRequest(
					ZoneControlHandler.requestBuilder(zoneIndex)
					.setting(ZoneSetting.SET_TARGET_SETPOINT)
					.settingValue(determineAndValidateSettingValue(ZoneSetting.SET_TARGET_SETPOINT, commandParams.get(3)))
					.build(getNextCounter()));
			break;
		case "open-percentage":
			sendRequest(
					ZoneControlHandler.requestBuilder(zoneIndex)
					.setting(ZoneSetting.SET_OPEN_PERCENTAGE)
					.settingValue(determineAndValidateSettingValue(ZoneSetting.SET_OPEN_PERCENTAGE, commandParams.get(3)))
					.build(getNextCounter()));
			break;
		case "power":
			sendRequest(
					ZoneControlHandler.requestBuilder(zoneIndex)
					.power(determineGroupPower(commandParams.get(3)))
					.build(getNextCounter()));
			break;
		case "control":
			sendRequest(
					ZoneControlHandler.requestBuilder(Integer.valueOf(commandParams.get(1)))
					.control(determineZoneControl(commandParams.get(3)))
					.build(getNextCounter()));
			break;
		}
	}
}
