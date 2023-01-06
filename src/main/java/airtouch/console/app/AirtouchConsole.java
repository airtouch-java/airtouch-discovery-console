package airtouch.console.app;

import static org.fusesource.jansi.Ansi.ansi;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.YELLOW;
import static org.jline.builtins.Completers.TreeCompleter.node;

import java.io.IOException;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

import org.fusesource.jansi.AnsiConsole;
import org.jline.builtins.Completers.TreeCompleter;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.LineReader;
import org.jline.reader.LineReaderBuilder;
import org.jline.reader.UserInterruptException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import airtouch.console.data.AirtouchStatus;
import airtouch.console.event.AirtouchResponseEventListener;
import airtouch.console.event.AirtouchStatusEventListener;
import airtouch.console.service.AirtouchHeartbeatThread.HeartbeatSecondEventHandler;
import airtouch.console.service.AirtouchService;
import airtouch.v4.Response;
import airtouch.v4.builder.GroupControlRequestBuilder;
import airtouch.v4.constant.AirConditionerControlConstants.AcPower;
import airtouch.v4.constant.GroupControlConstants.GroupControl;
import airtouch.v4.constant.GroupControlConstants.GroupPower;
import airtouch.v4.constant.GroupControlConstants.GroupSetting;
import airtouch.v4.handler.AirConditionerControlHandler;
import airtouch.v4.handler.GroupControlHandler;
import airtouch.v4.model.AirConditionerAbilityResponse;
import airtouch.v4.model.AirConditionerStatusResponse;
import airtouch.v4.model.ConsoleVersionResponse;
import airtouch.v4.model.GroupNameResponse;
import airtouch.v4.model.GroupStatusResponse;

@SuppressWarnings("java:S106") // Tell Sonar not to worry about System.out. We need to use it.
public class AirtouchConsole {

	private final Logger log = LoggerFactory.getLogger(AirtouchConsole.class);
	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");
	private boolean running = true;
	private AirTouchStatusUpdater airTouchStatusUpdater;

	public void begin() throws InterruptedException, IOException {

		AnsiConsole.systemInstall();

		Completer completer = new TreeCompleter(
				node("ac",
					node("0", "1", "2", "3",
						node("power",
							node("on","off")
						)
					)
				),
				node("group",
					node("0", "1", "2", "3",
						node("target-temp",
								node("20","21","22","23","24","25") // TODO, this should be the valid temps as determined by AcStatus
						),
						node("power",
								node("on","off")
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

		airTouchStatusUpdater = new AirTouchStatusUpdater(reader);

		AirtouchService service = new AirtouchService().confgure(null, null, airTouchStatusUpdater).start();
		service.startHeartbeat(new HeartbeatSecondEventHandler() {

			@Override
			public void handleSecondEvent() {
//				System.out.print(ansi()
//						.saveCursorPosition()
//						.eraseScreen()
//						.cursorMove(38, 2)
//						.a("/")
//						.restoreCursorPosition());
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
			service.sendRequest(
				GroupControlHandler.requestBuilder(Integer.valueOf(list.get(2)))
					.power(determineGroupPower(list.get(1)))
					.build(service.getNextCounter()));
			break;
		}
	}

	private void handleGroupInput(AirtouchService service, List<String> list) throws NumberFormatException, IOException {
		// group 0/1/2/3 target-temp temp
		// group 0/1/2/3 power on/off
		switch (list.get(2).toLowerCase()) {
		case "target-temp":
			service.sendRequest(
				GroupControlHandler.requestBuilder(Integer.valueOf(list.get(1)))
				.setting(GroupSetting.SET_TARGET_SETPOINT)
				.settingValue(determineAndValidateSettingValue(GroupSetting.SET_TARGET_SETPOINT, list.get(3)))
				.build(service.getNextCounter()));
			break;
		case "power":
			service.sendRequest(
					GroupControlHandler.requestBuilder(Integer.valueOf(list.get(1)))
					.power(determineGroupPower(list.get(2)))
					.build(service.getNextCounter()));
			break;
		}
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
		return "on".equalsIgnoreCase(groupPowerStr) ? GroupPower.POWER_ON : GroupPower.POWER_OFF;
	}

	private AcPower determineAcPower(String acPowerStr) {
		return "on".equalsIgnoreCase(acPowerStr) ? AcPower.POWER_ON : AcPower.POWER_OFF;
	}

	public static class AirTouchStatusUpdater implements AirtouchStatusEventListener {

		LineReader reader;
		public AirTouchStatusUpdater(LineReader reader) {
			this.reader = reader;
		}

		@Override
		public void eventReceived(AirtouchStatus status) {

			if (reader != null && reader.getParsedLine() != null && !reader.getParsedLine().words().isEmpty()) {
				return;
			}

			System.out.println(ansi()
					.eraseScreen()
					.fg(YELLOW)
					.a("╔══════════════════════════════════════════════════════════════════════════════╗")
					);
			System.out.println(ansi()
					.fg(YELLOW).a("║")
					.fg(GREEN)
					.a(String.format(" AirTouch Console(s) - versions: %1$-27s", status.getConsoleVersion() != null ? status.getConsoleVersion().getVersions() : "Unknown"))
					.a(String.format("Updated: %1$8s ", LocalTime.now().format(formatter)))
					.fg(YELLOW).a("║")
					);
			for (AirConditionerStatusResponse acStatus : status.getAcStatuses()) {
				System.out.println(ansi()
						.a("╠══════════════════════════════════════════════════════════════════════════════╣")
						.reset()
						);
				System.out.println(ansi()
						.fg(YELLOW).a("║").reset()
						.a(leftPaddedBox(20,
								String.format("AC unit: %s", status.getAcAbilities().get(acStatus.getAcNumber()).getAcName())
							)
						)
						.fg(YELLOW).a("║").reset()
						.a(leftPaddedBox(15,String.format("Power: %s ", acStatus.getPowerstate())))
						.fg(YELLOW).a("║").reset()
						.a(leftPaddedBox(24,String.format("Fan Speed: %s ", acStatus.getFanSpeed())))
						.fg(YELLOW).a("║").reset()
						.a(leftPaddedBox(20,String.format("Temperature: %d°C ", acStatus.getCurrentTemperature())))
						.fg(YELLOW).a("║").reset()
						);
				System.out.println(ansi()
						.fg(YELLOW).a("║").reset()
						.a(leftPaddedBox(20,""))
						.fg(YELLOW).a("║").reset()
						.a(leftPaddedBox(15,String.format("Mode: %s ", acStatus.getMode())))
						.fg(YELLOW).a("║").reset()
						.a(leftPaddedBox(24,String.format("Target temp: %s°C", acStatus.getTargetSetpoint())))
						.fg(YELLOW).a("║").reset()
						.a(leftPaddedBox(20,String.format("ErrorCode: %s", acStatus.getErrorCode())))
						.fg(YELLOW).a("║").reset()
						);

			}
			for ( GroupStatusResponse groupStatus :  status.getGroupStatuses()) {
				System.out.println(ansi()
						.fg(YELLOW)
						.a("╠══════════════════════════════════════════════════════════════════════════════╣")
						.reset()
						);
				System.out.println(ansi()
						.fg(YELLOW).a("║").reset()
						.a(leftPaddedBox(20, String.format("Group: %d (%s) ", groupStatus.getGroupNumber(), status.getGroupNames().getOrDefault(groupStatus.getGroupNumber(), "Unknown"))))
						.fg(YELLOW).a("║").reset()
						.a(leftPaddedBox(39, String.format("Control Method: %s ", groupStatus.getControlMethod())))
						.fg(YELLOW).a("║").reset()
						.a(leftPaddedBox(20, String.format("Temperature: %d°C ", groupStatus.getCurrentTemperature())))
						.fg(YELLOW).a("║").reset()
						);
				System.out.println(ansi()
						.fg(YELLOW).a("║").reset()
						.a(leftPaddedBox(20, ""))
						.fg(YELLOW).a("║").reset()
						.a(leftPaddedBox(20, String.format("Damper Open: %d%% ", groupStatus.getOpenPercentage())))
						.fg(YELLOW).a("║").reset()
						.a(leftPaddedBox(19, String.format("Power state: %s ", groupStatus.getPowerstate())))
						.fg(YELLOW).a("║").reset()
						.a(leftPaddedBox(20, String.format("Target temp: %s°C ", groupStatus.getTargetSetpoint())))
						.fg(YELLOW).a("║").reset()
						);

			}

			System.out.println(ansi()
					.fg(YELLOW)
					.a("╠══════════════════════════════════════════════════════════════════════════════╣")
					.reset()
					);

			System.out.println(ansi()
					.fg(YELLOW).a("║").reset()
					.a(leftPaddedBox(79, "Commands: Q - Quit, G - Group Target Temp "))
					.fg(YELLOW).a("║").reset()
					);
			System.out.println(ansi()
					.fg(YELLOW).a("║").reset()
					.a(leftPaddedBox(79, "Press a letter: "))
					.fg(YELLOW).a("║").reset()
					);
			System.out.println(ansi()
					.fg(YELLOW)
					.a("╚══════════════════════════════════════════════════════════════════════════════╝")
					.reset()
					);
		}

		private String leftPaddedBox(int width, String inputString) {
			return String.format(" %1$-" + (width -2) + "s", inputString);
		}

		private String rightPaddedBox(int width, String inputString) {
			return String.format(" %1$" + (width -2) + "s", inputString);
		}

	}

}
