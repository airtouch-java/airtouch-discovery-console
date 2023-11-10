package airtouch.console;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import airtouch.console.app.AirtouchConsole;

public class App {

	private static final Logger log = LoggerFactory.getLogger(App.class);


	public static void main(String[] args) throws Exception {
		AirtouchConsole console = new AirtouchConsole();
		try {
			console.begin();
		} catch (Exception e) {
			log.error("Exception in App. Exiting....", e);
			throw e;
		}
	}
}
