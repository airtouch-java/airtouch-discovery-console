package airtouch.console.data;

import airtouch.v4.constant.AirConditionerStatusConstants.FanSpeed;
import airtouch.v4.constant.AirConditionerStatusConstants.Mode;
import airtouch.v4.constant.AirConditionerStatusConstants.PowerState;
import lombok.Data;

@Data
public class Airtouch4AcStatus  implements AirtouchAcStatus{
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
