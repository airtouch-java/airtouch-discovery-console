package airtouch.console.app;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import static org.fusesource.jansi.Ansi.ansi;
import static org.fusesource.jansi.Ansi.Color.GREEN;
import static org.fusesource.jansi.Ansi.Color.YELLOW;
import org.jline.reader.LineReader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import airtouch.console.data.AirtouchStatus;
import airtouch.console.event.AirtouchStatusEventListener;
import airtouch.v4.model.AirConditionerStatusResponse;
import airtouch.v4.model.GroupStatusResponse;

public class AirTouchStatusUpdater implements AirtouchStatusEventListener {
	
	private final static Logger log = LoggerFactory.getLogger(AirTouchStatusUpdater.class);

	private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");


		LineReader reader;
		public AirTouchStatusUpdater(LineReader reader) {
			this.reader = reader;
		}

		@Override
		public void eventReceived(AirtouchStatus status) {

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

//			System.out.println(ansi()
//					.fg(YELLOW)
//					.a("╠══════════════════════════════════════════════════════════════════════════════╣")
//					.reset()
//					);
//
//			System.out.println(ansi()
//					.fg(YELLOW).a("║").reset()
//					.a(leftPaddedBox(79, "Commands: Q - Quit, G - Group Target Temp "))
//					.fg(YELLOW).a("║").reset()
//					);
//			System.out.println(ansi()
//					.fg(YELLOW).a("║").reset()
//					.a(leftPaddedBox(79, "Press a letter: "))
//					.fg(YELLOW).a("║").reset()
//					);
			System.out.println(ansi()
					.fg(YELLOW)
					.a("╚══════════════════════════════════════════════════════════════════════════════╝")
					.reset()
					);
			System.out.println(ansi()
			.fg(YELLOW).a("Tab completion is enabled. Press tab at any time to show options")
			);
		}

		private String leftPaddedBox(int width, String inputString) {
			return String.format(" %1$-" + (width -2) + "s", inputString);
		}

		private String rightPaddedBox(int width, String inputString) {
			return String.format(" %1$" + (width -2) + "s", inputString);
		}

	}
