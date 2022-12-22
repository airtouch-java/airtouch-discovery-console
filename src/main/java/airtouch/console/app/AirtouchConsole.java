package airtouch.console.app;

import static org.fusesource.jansi.Ansi.*;
import static org.fusesource.jansi.Ansi.Color.*;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.fusesource.jansi.AnsiConsole;

import airtouch.v4.Response;
import airtouch.v4.ResponseCallback;
import airtouch.v4.connector.AirtouchConnector;
import airtouch.v4.model.AirConditionerStatusResponse;
import airtouch.v4.model.GroupNameResponse;
import airtouch.v4.model.GroupStatusResponse;

public class AirtouchConsole {

	public void begin() throws InterruptedException, IOException {
		AnsiConsole.systemInstall();

		System.out.println(ansi().eraseScreen().fg(GREEN).a("AirTouch Console").reset());
		System.out.println(ansi().fg(GREEN).a("Fetching Airtouch data....").reset());

		AirtouchService service = new AirtouchService().confgure(null, null, new AirTouchStatusUpdater()).start();


		Thread.sleep(60 * 1000L);

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
				break;
			case CONSOLE_VERSION:
				break;
			case EXTENDED:
				break;
			case GROUP_CONTROL:
				break;
			default:
				break;
			}

			System.out.println(ansi().eraseScreen().fg(GREEN).a("AirTouch Console").reset());
			System.out.println(ansi().a("Found AC units: " + status.getAcStatuses().size()));
			for (AirConditionerStatusResponse acStatus : status.getAcStatuses()) {
				System.out.println(ansi()
						.a(String.format("| AC unit: %d ", acStatus.getAcNumber()))
						.a(String.format("| AirTouch Console Version: %s ", acStatus.getPowerstate()))
						.a(String.format("| Power: %s ", acStatus.getPowerstate()))
						.a(String.format("| Fan Speed: %s ", acStatus.getFanSpeed()))
						.a(String.format("| Temperature: %d ", acStatus.getCurrentTemperature()))
						.a(String.format("| Mode: %s ", acStatus.getMode()))
						.a(String.format("| Target Setpoint: %s ", acStatus.getTargetSetpoint()))
						.a(String.format("| ErrorCode: %s ", acStatus.getErrorCode()))
						.a("|")
						);

			}
			for ( GroupStatusResponse groupStatus :  status.getGroupStatuses()) {
				System.out.println(ansi()
						.a(String.format("| Group: %d (%s) ", groupStatus.getGroupNumber(), status.getGroupNames().getOrDefault(groupStatus.getGroupNumber(), "Unknown")))
						.a(String.format("| Control Method: %s ", groupStatus.getControlMethod()))
						.a(String.format("| Temperature: %d° C ", groupStatus.getCurrentTemperature()))
						.a(String.format("| Damper Open: %d%% ", groupStatus.getOpenPercentage()))
						.a(String.format("| Power state: %s ", groupStatus.getPowerstate()))
						.a(String.format("| Target Setpoint: %s ", groupStatus.getTargetSetpoint()))
						);
			}
		}

	}

}
