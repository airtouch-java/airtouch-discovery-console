package airtouch.console;

import java.io.IOException;

import airtouch.console.app.AirtouchConsole;

public class App {



	public static void main(String[] args) throws InterruptedException, IOException {
		AirtouchConsole console = new AirtouchConsole();
		console.begin();
	}
}
