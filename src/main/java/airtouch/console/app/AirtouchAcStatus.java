package airtouch.console.app;

import airtouch.v4.constant.AcStatusConstants.FanSpeed;
import airtouch.v4.constant.AcStatusConstants.Mode;
import airtouch.v4.constant.AcStatusConstants.PowerState;
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
