package airtouch.console.app;

import static org.junit.Assert.*;

import org.junit.Test;

public class AirTouchStatusUpdaterTest {

	@Test
	public void testDoubleValueFormatter() {
		assertEquals("24.3", AirTouchStatusUpdater.doubleValueFormatter(24.3d));
		assertEquals(" 2.3", AirTouchStatusUpdater.doubleValueFormatter(2.3d));
		assertEquals(" 2.0", AirTouchStatusUpdater.doubleValueFormatter(2d));
		assertEquals("99.9", AirTouchStatusUpdater.doubleValueFormatter(102.0d));
	}

}
