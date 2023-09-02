package airtouch.console.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import airtouch.v5.model.AirConditionerAbilityResponse;
import airtouch.v5.model.AirConditionerStatusResponse;
import airtouch.v5.model.ConsoleVersionResponse;
import airtouch.v5.model.ZoneStatusResponse;
import lombok.Data;

@Data
public class Airtouch5Status implements AirtouchStatus {
	private List<AirConditionerStatusResponse> acStatuses = new ArrayList<>();
	private Map<Integer, AirConditionerAbilityResponse> acAbilities = new HashMap<>();
	private List<ZoneStatusResponse> zoneStatuses = new ArrayList<>();
	private Map<Integer, String> zoneNames = new HashMap<>();
	private ConsoleVersionResponse consoleVersion = null;
}
