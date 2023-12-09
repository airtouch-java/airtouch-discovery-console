package airtouch.console.app;

import static org.fusesource.jansi.Ansi.ansi;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.RED;
import static org.fusesource.jansi.Ansi.Color.YELLOW;

import java.time.Instant;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import org.jline.reader.LineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import airtouch.console.data.AirtouchStatus;
import airtouch.console.event.AirtouchStatusEventListener;
import airtouch.console.service.AirtouchHeartbeatThread;
import airtouch.console.service.AirtouchHeartbeatThread.HeartbeatMinuteEventHandler;
import airtouch.console.service.AirtouchHeartbeatThread.HeartbeatSecondEventHandler;
import airtouch.model.AirConditionerStatusResponse;
import airtouch.model.ZoneStatusResponse;
import lombok.AllArgsConstructor;
import lombok.Data;

public class AirTouchStatusUpdater implements AirtouchStatusEventListener<AirtouchStatus> {

	private static final Logger log = LoggerFactory.getLogger(AirTouchStatusUpdater.class);

	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");


	private LineReader reader;

	private CustomCompleter completer;

	private AirtouchStatusWrapper status;
	
	private Instant lastUiUpdate = Instant.now();

	public AirTouchStatusUpdater(LineReader reader, CustomCompleter completer) {
		this.reader = reader;
		this.completer = completer;
		new AirtouchHeartbeatThread(
				new HeartbeatSecondEventHandler() {
					
					@Override
					public void handleSecondEvent() {
						if (status != null && status.getUpdateTime()!= null && status.getUpdateTime().isAfter(lastUiUpdate)){
							updateUi();
						}
					}
				},
				new HeartbeatMinuteEventHandler() {
					
					@Override
					public void handleMinuteEvent() {
					}
				}).start();
	}

	@Override
	@SuppressWarnings("java:S106") // Allow System.out.println as that is the console "UI".
	public void eventReceived(AirtouchStatus status) {
		this.status = new AirtouchStatusWrapper(status, Instant.now());
	}
	
	private void updateUi() {
		this.lastUiUpdate = Instant.now();
		int count = 0;
		log.debug("reader: ", reader);
		if (reader != null && reader.getParsedLine() != null) {
			log.debug("getParsedLine: ", reader.getParsedLine().words());
		}


		if (reader != null && reader.getParsedLine() != null && !reader.getParsedLine().words().isEmpty()) {
			log.debug("UI updates paused. User input field is not blank.");
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
				.a(String.format(" AirTouch Console(s) - versions: %1$-27s", status.status.getConsoleVersion() != null ? status.status.getConsoleVersion().getVersions() : "Unknown"))
				.a(String.format("Updated: %1$8s ", LocalTime.now().format(formatter)))
				.fg(YELLOW).a("║")
				);
		for (AirConditionerStatusResponse acStatus : status.status.getAcStatuses()) {
			System.out.println(ansi()
					.a("╠═══════════════════╦══════════════╦═══════════════════════╦═══════════════════╣")
					.reset()
					);
			System.out.println(ansi()
					.fg(YELLOW).a("║").reset()
					.a(leftPaddedBox(20,
							String.format("AC unit: %s", status.status.getAcAbilities().get(acStatus.getAcNumber()).getAcName())
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
		for ( ZoneStatusResponse groupStatus :  status.status.getZoneStatuses()) {
			if (count++ == 0) {
				System.out.println(ansi()
						.fg(YELLOW)
						.a("╠═══════════════════╬══════════════╩═══════════════════════╬═══════════════════╣")
						.reset()
						);
			} else {
				System.out.println(ansi()
						.fg(YELLOW)
						.a("╠═══════════════════╬═══════════════════╩══════════════════╬═══════════════════╣")
						.reset()
						);
			}
			System.out.println(ansi()
					.fg(YELLOW).a("║").reset()
					.a(leftPaddedBox(20, String.format("Group: %d (%s) ", groupStatus.getZoneNumber(), status.status.getZoneNames().getOrDefault(groupStatus.getZoneNumber(), "Unknown"))))
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
				.a("╚═══════════════════╩═══════════════════╩══════════════════╩═══════════════════╝")
				.reset()
				);
		if (status.status.getUserError() != null) {
			System.out.println(ansi().fg(RED).a(status.status.getUserError()).reset());
		}
		System.out.println(ansi()
		.fg(YELLOW).a("Tab completion is enabled. Press tab at any time to show options").reset()
		);
	}

	private String leftPaddedBox(int width, String inputString) {
		return String.format(" %1$-" + (width -2) + "s", inputString); //NOSONAR
	}

	@SuppressWarnings("unused")
	private String rightPaddedBox(int width, String inputString) {
		return String.format(" %1$" + (width -2) + "s", inputString); //NOSONAR
	}

	@Override
	public void bootStrapEventReceived(AirtouchStatus status) {
		this.completer.setCompleter(CustomCompleter.getCustomCompleter(status));
	}
	
	@Data @AllArgsConstructor
	public class AirtouchStatusWrapper {
		private AirtouchStatus status;
		private Instant updateTime;

	}

}
