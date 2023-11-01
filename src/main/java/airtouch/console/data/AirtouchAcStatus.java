package airtouch.console.data;

import airtouch.constant.AirConditionerStatusConstants.FanSpeed;
import airtouch.constant.AirConditionerStatusConstants.Mode;
import airtouch.constant.AirConditionerStatusConstants.PowerState;
import lombok.Data;

@Data
public class AirtouchAcStatus {
	private PowerState acPowerState;
	private Integer acNumber;
	private Mode acMode;
	private FanSpeed acFanSpeed;
	private Boolean acSpillActive;
	private Boolean acTimerActive;
	private Integer acSetpointTemperature;
	private	Integer acTemperture;
	private Integer acErrorCode;
	private Boolean acErrored;
}
