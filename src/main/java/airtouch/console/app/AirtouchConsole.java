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

import airtouch.console.app.AirtouchHeartbeatThread.HeartbeatSecondEventHandler;
import airtouch.v4.Response;
import airtouch.v4.model.AirConditionerAbilityResponse;
import airtouch.v4.model.AirConditionerStatusResponse;
import airtouch.v4.model.ConsoleVersionResponse;
import airtouch.v4.model.GroupNameResponse;
import airtouch.v4.model.GroupStatusResponse;

@SuppressWarnings("java:S106") // Tell Sonar not to worry about System.out. We need to use it.
public class AirtouchConsole {

    private final Logger log = LoggerFactory.getLogger(AirtouchConsole.class);
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm:ss");


	public void begin() throws InterruptedException, IOException {
		boolean running = true;

		AnsiConsole.systemInstall();

		Completer completer = new TreeCompleter(
			    node("Command1",
			        node("Option1",
			            node("Param1", "Param2")),
			        node("Option2"),
			        node("Option3")),
			    node("quit")
			    );

	    LineReader reader = LineReaderBuilder
	    		.builder()
	    		.completer(completer)
	    		.build();
	    String prompt = "my prompt";


		AnsiConsole.out.println(ansi().eraseScreen().fg(GREEN).a("AirTouch Console").reset());
		System.out.println(ansi().fg(GREEN).a("Fetching Airtouch data....").reset());

		AirtouchService service = new AirtouchService().confgure(null, null, new AirTouchStatusUpdater()).start();
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


//        while (running) {
//	        while(scanner.hasNext()){
//	        	int line = System.in.read();
//	            System.out.printf("User input was: %s%n", line);
//	        }
//        }

        /*
        try {
            while (true) {
                System.out.println("Please input a line");
                long then = System.currentTimeMillis();
                int line = System.in.read();
                long now = System.currentTimeMillis();
                System.out.printf("Waited %.3fs for user input%n", (now - then) / 1000d);
                System.out.printf("User input was: %s%n", line);
            }
        } catch(IllegalStateException | NoSuchElementException e) {
            // System.in has been closed
            System.out.println("System.in was closed; exiting");
        }*/

	    while (running) {
	        String line = null;
	        try {
	            line = reader.readLine(prompt);
	        } catch (UserInterruptException e) {
	            // Ignore
	        } catch (EndOfFileException e) {
	            return;
	        }
	        if (line.equalsIgnoreCase("quit")) {
	        	running = false;
	        	System.exit(0);
	        }
	    }

		service.stop();

	}

	public static class AirTouchStatusUpdater implements AirtouchServiceEventListener {

		private AirtouchStatus status = new AirtouchStatus();

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
