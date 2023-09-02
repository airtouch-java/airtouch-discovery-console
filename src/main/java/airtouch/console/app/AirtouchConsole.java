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
import airtouch.console.service.AirtouchHeartbeatThread.HeartbeatSecondEventHandler;
import airtouch.console.service.AirtouchService;
import airtouch.console.service.Airtouch4Service;
import airtouch.v4.constant.AirConditionerControlConstants.AcPower;
import airtouch.v4.constant.GroupControlConstants.GroupControl;
import airtouch.v4.constant.GroupControlConstants.GroupPower;
import airtouch.v4.constant.GroupControlConstants.GroupSetting;
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
	private int airTouch4PortNumber = 9004;
	private int airTouch5PortNumber = 9005;
	private boolean running = true;
	private int secondsSinceStarted = 0;
	private AirtouchDiscoverer airtouch4Discoverer;
	private AirtouchDiscoverer airtouch5Discoverer;

	public void begin() throws IOException {

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
						startUI(AirtouchVersion.AIRTOUCH4, response.getHostAddress(), response.getPortNumber());
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
						startUI(AirtouchVersion.AIRTOUCH5, response.getHostAddress(), response.getPortNumber());
					} catch (IOException e) {
						log.warn("failed to auto start", e);
					}
				}
			});
			airtouch5Discoverer.start();
		} else {
			try {
				startUI(AirtouchVersion.AIRTOUCH5, hostName, airTouch5PortNumber);
				System.out.println(String.format("Attmpting to connect to host '%s' for Airtouch connection.", hostName));
			} catch (IOException | AirtouchMessagingException e) {
				System.out.println("Failed to start Airtouch5. Trying Airtouch4");
			}
			try {
				startUI(AirtouchVersion.AIRTOUCH4, hostName, airTouch4PortNumber);
				System.out.println(String.format("Attmpting to connect to host '%s' for Airtouch connection.", hostName));
			} catch (IOException e) {
				System.out.println("Failed to start Airtouch4. :-(");
			}
		}
	}

	@SuppressWarnings("deprecation")
	private void startUI(AirtouchVersion airtouchVersion, String hostName, Integer portNumber) throws IOException {
		AnsiConsole.systemInstall();

		Completer completer = new TreeCompleter(
				node("ac",
					node("0", "1", "2", "3",
						node("power",
							node("on", "off")
						),
						node("mode",
							node("cool", "heat")
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

		LineReader reader = LineReaderBuilder
				.builder()
				.completer(completer)
				.build();
		String prompt = "";


		AnsiConsole.out.println(ansi().eraseScreen().fg(GREEN).a("AirTouch Console").reset());
		System.out.println(ansi().fg(GREEN).a("Fetching Airtouch data....").reset());

		AirTouch4StatusUpdater airTouchStatusUpdater = new AirTouch4StatusUpdater(reader);

		AirtouchService service = new Airtouch4Service().confgure(hostName, portNumber, airTouchStatusUpdater).start();
		service.startHeartbeat(new HeartbeatSecondEventHandler() {

			@Override
			public void handleSecondEvent() {
				secondsSinceStarted++;

				if (secondsSinceStarted > 30 && airtouch4Discoverer != null && airtouch4Discoverer.isRunning()) {
					airtouch4Discoverer.shutdown();
				}
			}
		});


		while (running) {
			try {
				reader.readLine(prompt);
			} catch (UserInterruptException e) {
				// Ignore
			} catch (EndOfFileException e) {
				return;
			}
			if (reader != null && reader.getParsedLine() != null && !reader.getParsedLine().words().isEmpty()) {
				handleInput(service, reader.getParsedLine().words());
			} else {
				System.out.println(reader.getParsedLine().words());
			}
		}

		service.stop();
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
		case "group":
			handleGroupInput(service, list);
			break;
		}
	}

	private void handleGroupInput(AirtouchService service, List<String> list) throws NumberFormatException, IOException {
		// group 0/1/2/3 target-temp temp
		// group 0/1/2/3 power on/off
		// group 0/1/2/3 control temperature/percentage
		switch (list.get(2).toLowerCase()) {
		case "target-temp":
			service.sendRequest(
				GroupControlHandler.requestBuilder(Integer.valueOf(list.get(1)))
				.setting(GroupSetting.SET_TARGET_SETPOINT)
				.settingValue(determineAndValidateSettingValue(GroupSetting.SET_TARGET_SETPOINT, list.get(3)))
				.build(service.getNextCounter()));
			break;
		case "open-percentage":
			service.sendRequest(
					GroupControlHandler.requestBuilder(Integer.valueOf(list.get(1)))
					.setting(GroupSetting.SET_OPEN_PERCENTAGE)
					.settingValue(determineAndValidateSettingValue(GroupSetting.SET_OPEN_PERCENTAGE, list.get(3)))
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

	private GroupControl determineGroupControl(String groupControlStr) {
		return "temperature".equalsIgnoreCase(groupControlStr) ? GroupControl.TEMPERATURE_CONTROL : GroupControl.PERCENTAGE_CONTROL;
	}

	private int determineAndValidateSettingValue(GroupSetting groupSetting, String settingValue) {
		int value = Integer.valueOf(settingValue);
		if (GroupSetting.SET_TARGET_SETPOINT.equals(groupSetting) && isValidTemperatureSetPoint(value)) {
			return value;
		} else if (GroupSetting.SET_OPEN_PERCENTAGE.equals(groupSetting) && isValidOpenPercentage(value)){
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
		}
	}

	private GroupPower determineGroupPower(String groupPowerStr) {
		switch (groupPowerStr.toLowerCase()) {
		case "on":
			return GroupPower.POWER_ON;
		case "off":
			return GroupPower.POWER_OFF;
		case "turbo":
			return GroupPower.TURBO_POWER;
		default:
			return GroupPower.NO_CHANGE;
		}
	}

	private AcPower determineAcPower(String acPowerStr) {
		return "on".equalsIgnoreCase(acPowerStr) ? AcPower.POWER_ON : AcPower.POWER_OFF;
	}

}
