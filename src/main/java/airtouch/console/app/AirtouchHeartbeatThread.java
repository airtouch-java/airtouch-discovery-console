package airtouch.console.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AirtouchHeartbeatThread extends Thread implements Runnable {
	private static final String DEFAULT_THREAD_NAME = AirtouchHeartbeatThread.class.getSimpleName();

	private final Logger log = LoggerFactory.getLogger(AirtouchHeartbeatThread.class);

	private boolean stopping;

	private HeartbeatSecondEventHandler eventSecondHandler;
	private HeartbeatMinuteEventHandler eventMinuteHandler;

	public void shutdown() {
		this.stopping = true;
	}

	public boolean isRunning() {
		return !this.stopping;
	}

	public AirtouchHeartbeatThread(HeartbeatEventHandler eventHandler) {
		super(DEFAULT_THREAD_NAME);
		this.eventSecondHandler = eventHandler;
		this.eventMinuteHandler = eventHandler;
	}
	public AirtouchHeartbeatThread(HeartbeatSecondEventHandler eventSecondHandler, HeartbeatMinuteEventHandler eventMinuteHandler) {
		super(DEFAULT_THREAD_NAME);
		this.eventSecondHandler = eventSecondHandler;
		this.eventMinuteHandler = eventMinuteHandler;
	}

	@Override
	public void run() {
		final int sleepAmount = 1000; // ms
		int iterationCounter = 0;
		final int iterations = 60000 / sleepAmount;
		while (isRunning()) {
			if (iterationCounter > iterations) {
				iterationCounter = 0;
				this.eventMinuteHandler.handleMinuteEvent();
			}
			this.eventSecondHandler.handleSecondEvent();
			try {
				Thread.sleep(sleepAmount);
			} catch (InterruptedException e) {
				log.debug("Heartbeat thread interrupted");
				// Restore interrupted state...
				Thread.currentThread().interrupt();
			}
			iterationCounter++;
		}
	}

	public static interface HeartbeatSecondEventHandler {
		public void handleSecondEvent();

	}
	public static interface HeartbeatMinuteEventHandler {
		public void handleMinuteEvent();

	}
	public static interface HeartbeatEventHandler extends HeartbeatSecondEventHandler, HeartbeatMinuteEventHandler {
	}
}
