package airtouch.console.data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import airtouch.model.AirConditionerAbilityResponse;
import airtouch.model.AirConditionerStatusResponse;
import airtouch.model.ConsoleVersionResponse;
import airtouch.model.ZoneStatusResponse;
import lombok.Data;

@Data
public class AirtouchStatus {
	private List<AirConditionerStatusResponse> acStatuses = new ArrayList<>();
	private Map<Integer, AirConditionerAbilityResponse> acAbilities = new HashMap<>();
	private List<ZoneStatusResponse> zoneStatuses = new ArrayList<>();
	private Map<Integer, String> zoneNames = new HashMap<>();
	private ConsoleVersionResponse consoleVersion = null;
	private String userError = null;
	private LocalDateTime lastUpdate = null;
}
