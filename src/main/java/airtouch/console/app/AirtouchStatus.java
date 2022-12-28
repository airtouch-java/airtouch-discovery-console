package airtouch.console.app;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import airtouch.v4.model.AirConditionerAbilityResponse;
import airtouch.v4.model.AirConditionerStatusResponse;
import airtouch.v4.model.ConsoleVersionResponse;
import airtouch.v4.model.GroupStatusResponse;
import lombok.Data;

@Data
public class AirtouchStatus {
	private List<AirConditionerStatusResponse> acStatuses = new ArrayList<>();
	private Map<Integer, AirConditionerAbilityResponse> acAbilities = new HashMap<>();
	private List<GroupStatusResponse> groupStatuses = new ArrayList<>();
	private Map<Integer, String> groupNames = new HashMap<>();
	private ConsoleVersionResponse consoleVersion = null;
	//private
}
