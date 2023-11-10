package airtouch.console.app;

import static org.fusesource.jansi.Ansi.ansi;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.jline.builtins.Completers.TreeCompleter.node;

import java.io.IOException;
import java.util.List;
import java.util.regex.Pattern;

import org.fusesource.jansi.AnsiConsole;
import org.jline.builtins.Completers.TreeCompleter;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import airtouch.AirtouchVersion;
import airtouch.console.service.Airtouch4Service;
import airtouch.console.service.Airtouch5Service;
import airtouch.console.service.AirtouchHeartbeatThread.HeartbeatSecondEventHandler;
import airtouch.console.service.AirtouchService;
import airtouch.constant.AirConditionerControlConstants.Mode;
import airtouch.constant.AirConditionerControlConstants.AcPower;
import airtouch.constant.ZoneControlConstants.ZoneControl;
import airtouch.constant.ZoneControlConstants.ZonePower;
import airtouch.constant.ZoneControlConstants.ZoneSetting;
import airtouch.discovery.AirtouchDiscoverer;
import airtouch.discovery.AirtouchDiscoveryBroadcastResponseCallback;
import airtouch.exception.AirtouchMessagingException;
import airtouch.v4.handler.AirConditionerControlHandler;
import airtouch.v4.handler.GroupControlHandler;

@SuppressWarnings("java:S106") // Tell Sonar not to worry about System.out. We need to use it.
public class AirtouchConsole {

	private static final Logger log = LoggerFactory.getLogger(AirtouchConsole.class);

	private static final Pattern IP_PATTERN = Pattern.compile("^((?:(?:2(?:[0-4][0-9]|5[0-5])|[0-1]?[0-9]?[0-9])\\.){3}(?:(?:2([0-4][0-9]|5[0-5])|[0-1]?[0-9]?[0-9])))$");
	private static final Pattern MAC_PATTERN = Pattern.compile("^([0-9a-f][0-9a-f]([:-])[0-9a-f][0-9a-f](\\2[0-9a-f][0-9a-f]){4,8})$");

	private String hostName = System.getenv("AIRTOUCH_HOST");
	private boolean running = true;
	private int secondsSinceStarted = 0;
	private AirtouchDiscoverer airtouch4Discoverer;
	private AirtouchDiscoverer airtouch5Discoverer;

	public void begin() throws IOException {

		String version = System.getProperty("java.runtime.version");
		String vendor = System.getProperty("java.vendor");
		String vm = System.getProperty("java.vm.name");
		log.info("Starting AirtouchConsole using {} :: Version: {} :: Vendor: {}" , vm, version, vendor);
		AnsiConsole.systemInstall();
		LineReader reader = initialiseCompleter();
		if (this.hostName == null) {

			System.out.println("Attemping to auto-discover airtouch on the network using UDP Broadcast.");
			System.out.println("Depending on your network, this might take a few minutes to discover.");
			System.out.println("To specify the IP or hostname to use, set an environment variabled named AIRTOUCH_HOST");
			airtouch4Discoverer = new AirtouchDiscoverer(AirtouchVersion.AIRTOUCH4, new AirtouchDiscoveryBroadcastResponseCallback() {
				@Override
				public void handleResponse(AirtouchDiscoveryBroadcastResponse response) {
					try {
						System.out.println(String.format("Found '%s' at '%s' with id '%s'",
								response.getAirtouchVersion(),
								response.getHostAddress(),
								response.getAirtouchId()));
						stopDiscoverers();
						startUI(reader, AirtouchVersion.AIRTOUCH4, response.getHostAddress(), response.getPortNumber());
					} catch (IOException e) {
						log.warn("failed to auto start", e);
					}
				}
			});
			airtouch4Discoverer.start();

			airtouch5Discoverer = new AirtouchDiscoverer(AirtouchVersion.AIRTOUCH5, new AirtouchDiscoveryBroadcastResponseCallback() {
				@Override
				public void handleResponse(AirtouchDiscoveryBroadcastResponse response) {
					try {
						System.out.println(String.format("Found '%s' at '%s' with id '%s'",
								response.getAirtouchVersion(),
								response.getHostAddress(),
								response.getAirtouchId()));
						startUI(reader, AirtouchVersion.AIRTOUCH5, response.getHostAddress(), response.getPortNumber());
					} catch (IOException e) {
						log.warn("failed to auto start", e);
					}
				}
			});
			airtouch5Discoverer.start();
		} else {
			try {
				System.out.println(String.format("Attempting to connect to host '%s' for Airtouch5 connection.", hostName));
				startUI(reader, AirtouchVersion.AIRTOUCH5, hostName, AirtouchVersion.AIRTOUCH5.getListeningPort());
			} catch (IOException | AirtouchMessagingException e) {
				System.out.println("Failed to start Airtouch5. Trying Airtouch4");
				log.debug("Failed to start Airtouch5. Trying Airtouch4", e);
			}
			try {
				System.out.println(String.format("Attempting to connect to host '%s' for Airtouch4 connection.", hostName));
				startUI(reader, AirtouchVersion.AIRTOUCH4, hostName, AirtouchVersion.AIRTOUCH4.getListeningPort());
			} catch (IOException | AirtouchMessagingException e) {
				System.out.println("Failed to start Airtouch4. Giving up.  :-(     See log for more details");
				log.debug("Failed to start Airtouch4. ", e);
			}
		}
	}

	private void stopDiscoverers() {
		if (this.airtouch4Discoverer != null && this.airtouch4Discoverer.isRunning()) {
			log.info("Stopping discoverer for AirTouch4");
			this.airtouch4Discoverer.shutdown();
		}
		if (this.airtouch5Discoverer != null && this.airtouch5Discoverer.isRunning()) {
			log.info("Stopping discoverer for AirTouch5");
			this.airtouch5Discoverer.shutdown();
		}
	}

	@SuppressWarnings("deprecation")
	private void startUI(LineReader reader, AirtouchVersion airtouchVersion, String hostName, Integer portNumber) throws IOException {

		String prompt = "";


		AnsiConsole.out.println(ansi().eraseScreen().fg(GREEN).a("AirTouch Console").reset());
		System.out.println(ansi().fg(GREEN).a("Fetching Airtouch data....").reset());

		AirtouchService service = null;

		if (AirtouchVersion.AIRTOUCH4.equals(airtouchVersion)) {
			AirTouchStatusUpdater airTouchStatusUpdater = new AirTouchStatusUpdater(reader);

			service = new Airtouch4Service().confgure(AirtouchVersion.AIRTOUCH4, hostName, portNumber, airTouchStatusUpdater).start();
			service.startHeartbeat(new HeartbeatSecondEventHandler() {

				@Override
				public void handleSecondEvent() {
					secondsSinceStarted++;

					if (secondsSinceStarted > 30 && airtouch4Discoverer != null && airtouch4Discoverer.isRunning()) {
						airtouch4Discoverer.shutdown();
					}
				}
			});
		} else if (AirtouchVersion.AIRTOUCH5.equals(airtouchVersion)) {
			AirTouchStatusUpdater airTouchStatusUpdater = new AirTouchStatusUpdater(reader);

			service = new Airtouch5Service().confgure(AirtouchVersion.AIRTOUCH5, hostName, portNumber, airTouchStatusUpdater).start();
			service.startHeartbeat(new HeartbeatSecondEventHandler() {

				@Override
				public void handleSecondEvent() {
					secondsSinceStarted++;

					if (secondsSinceStarted > 30 && airtouch5Discoverer != null && airtouch5Discoverer.isRunning()) {
						airtouch5Discoverer.shutdown();
					}
				}
			});
		}


		while (running) {
			try {
				reader.readLine(prompt);
			} catch (UserInterruptException e) {
				// Ignore
			} catch (EndOfFileException e) {
				return;
			}
			if (reader != null && reader.getParsedLine() != null && !reader.getParsedLine().words().isEmpty()) {
				log.debug("Words: {}", reader.getParsedLine().words());
				handleInput(service, reader.getParsedLine().words());
			} else {
				System.out.println(reader.getParsedLine().words());
			}
		}

		service.stop();
	}

	private LineReader initialiseCompleter() {
		Completer completer = new TreeCompleter(
				node("ac",
					node("0", "1", "2", "3",
						node("power",
							node("on", "off")
						),
						node("mode",
							node("auto", "cool", "heat", "dry", "fan")
						)
					)
				),
				node("zone",
					node("0", "1", "2", "3",
						node("target-temp",
							node("20","21","22","23","24","25") // TODO, this should be the valid temps as determined by AcStatus
						),
						node("open-percentage",
							node("0", "5", "10", "15", "20", "25", "30", "35", "40", "45","50", "55", "60", "65", "70", "75", "80", "85", "90", "95", "100")
						),
						node("power",
							node("on", "off", "turbo")
						),
						node("control",
							node("temperature", "percentage")
						)
					)
				),
				node("quit")
				);

		return LineReaderBuilder
				.builder()
				.completer(completer)
				.build();
	}

	private void handleInput(AirtouchService service, List<String> list) throws NumberFormatException, IOException {
		switch(list.get(0).toLowerCase()) {
		case "quit":
			this.running = false;
			System.exit(0);
			break;
		case "ac":
			handleAcInput(service, list);
			break;
		case "zone":
			handleZoneInput(service, list);
			break;
		}
	}

	private void handleZoneInput(AirtouchService service, List<String> list) throws NumberFormatException, IOException {
		// zone 0/1/2/3 target-temp temp
		// zone 0/1/2/3 power on/off
		// zone 0/1/2/3 control temperature/percentage
		switch (list.get(2).toLowerCase()) {
		case "target-temp":
			if (AirtouchVersion.AIRTOUCH4.equals(service.getAirtouchVersion())) {
				service.sendRequest(
					airtouch.v4.handler.GroupControlHandler.requestBuilder(Integer.valueOf(list.get(1)))
					.setting(ZoneSetting.SET_TARGET_SETPOINT)
					.settingValue(determineAndValidateSettingValue(ZoneSetting.SET_TARGET_SETPOINT, list.get(3)))
					.build(service.getNextCounter()));
			} else if (AirtouchVersion.AIRTOUCH5.equals(service.getAirtouchVersion())) {
			/*	service.sendRequest(
						airtouch.v5.handler.ZoneControlHandler.requestBuilder(Integer.valueOf(list.get(1)))
						.setting(ZoneSetting.SET_TARGET_SETPOINT)
						.settingValue(determineAndValidateSettingValue(ZoneSetting.SET_TARGET_SETPOINT, list.get(3)))
						.build(service.getNextCounter()));*/
			}
			break;
		case "open-percentage":
			service.sendRequest(
					GroupControlHandler.requestBuilder(Integer.valueOf(list.get(1)))
					.setting(ZoneSetting.SET_OPEN_PERCENTAGE)
					.settingValue(determineAndValidateSettingValue(ZoneSetting.SET_OPEN_PERCENTAGE, list.get(3)))
					.build(service.getNextCounter()));
			break;
		case "power":
			service.sendRequest(
					GroupControlHandler.requestBuilder(Integer.valueOf(list.get(1)))
					.power(determineGroupPower(list.get(3)))
					.build(service.getNextCounter()));
			break;
		case "control":
			service.sendRequest(
					GroupControlHandler.requestBuilder(Integer.valueOf(list.get(1)))
					.control(determineGroupControl(list.get(3)))
					.build(service.getNextCounter()));
			break;
		}
	}

	private ZoneControl determineGroupControl(String groupControlStr) {
		return "temperature".equalsIgnoreCase(groupControlStr) ? ZoneControl.TEMPERATURE_CONTROL : ZoneControl.PERCENTAGE_CONTROL;
	}

	private int determineAndValidateSettingValue(ZoneSetting zoneSetting, String settingValue) {
		int value = Integer.parseInt(settingValue);
		if (ZoneSetting.SET_TARGET_SETPOINT.equals(zoneSetting) && isValidTemperatureSetPoint(value)) {
			return value;
		} else if (ZoneSetting.SET_OPEN_PERCENTAGE.equals(zoneSetting) && isValidOpenPercentage(value)){
			return value;
		} else {
			throw new IllegalArgumentException("Value is outside allowable range.");
		}
	}

	private boolean isValidOpenPercentage(int value) {
		return value >= 0 && value <= 100 && value % 5 == 0;
	}

	private boolean isValidTemperatureSetPoint(int value) {
		return true; // TODD: Fix impl
	}

	private void handleAcInput(AirtouchService service, List<String> list) throws IOException {
		// ac 0/1/2/3 power on/off
		switch (list.get(2).toLowerCase()) {
		case "power":
			service.sendRequest(
				AirConditionerControlHandler.requestBuilder()
				.acNumber(Integer.valueOf(list.get(1)))
				.acPower(determineAcPower(list.get(3)))
				.build(service.getNextCounter()));
			break;
		case "mode":
			service.sendRequest(
					AirConditionerControlHandler.requestBuilder()
					.acNumber(Integer.valueOf(list.get(1)))
					.acMode(determineAcMode(list.get(3)))
					.build(service.getNextCounter()));
			break;
		}
	}

	private ZonePower determineGroupPower(String groupPowerStr) {
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

	private AcPower determineAcPower(String acPowerStr) {
		return "on".equalsIgnoreCase(acPowerStr) ? AcPower.POWER_ON : AcPower.POWER_OFF;
	}

	private Mode determineAcMode(String acModeStr) {
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

}
