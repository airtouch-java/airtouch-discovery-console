
## airtouch-discovery-console
A console application written in java to exercise the [airtouch-utils](https://github.com/airtouch-java/airtouch-utils) Java library.

The library provides the following:
- Airtouch4 and Airtouch5 UDP broadcast discovery to try to find the Airtouch on the network
- Handlers to generate and handle read and write events when communicating with an Airtouch control unit.
- Threads and services wrapped around the above to ease the communication with the Airtouch unit.

### Requirements
- Java 11 or higher
- TCP connection from computer running this code to Airtouch unit on port 9004 (Airtouch4) or 9005 (Airtouch5)
  No internet connectivity required
- If you want to auto-discover the Airtouch unit, you will also need UDP broadcast enabled, and be situated on the same subnet as the Airtouch unit.

### Example usage

`java -jar airtouch-discovery-console-0.0.4-shaded.jar` See [releases](https://github.com/airtouch-java/airtouch-discovery-console/releases) to download the latest jar file.

Auto discovery should find the Airtouch4 or Airtouch5 if it's on the same network and UDP broadcasts are working correctly on your network.

If not discovered, you can try setting an environment variable first. eg, on Linux and Mac `export AIRTOUCH_HOST=192.168.1.200`

### Example UI
```
╔══════════════════════════════════════════════════════════════════════════════╗
║ AirTouch Console(s) - versions: [1.2.1]                    Updated: 12:01:00 ║
╠═══════════════════╦══════════════╦═══════════════════════╦═══════════════════╣
║ AC unit: Fujitsu  ║ Power: OFF   ║ Fan Speed: AUTO       ║ Temperature: 25°C ║
║                   ║ Mode: COOL   ║ Target temp: 23°C     ║ ErrorCode: 0      ║
╠═══════════════════╬══════════════╩═══════════════════════╬═══════════════════╣
║ Group: 0 (Master) ║ Control Method: TEMPERATURE_CONTROL  ║ Temperature: 25°C ║
║                   ║ Damper Open: 0%   ║ Power state: ON  ║ Target temp: 23°C ║
╠═══════════════════╬═══════════════════╩══════════════════╬═══════════════════╣
║ Group: 1 (Lounge) ║ Control Method: TEMPERATURE_CONTROL  ║ Temperature: 24°C ║
║                   ║ Damper Open: 15%  ║ Power state: ON  ║ Target temp: 23°C ║
╠═══════════════════╬═══════════════════╩══════════════════╬═══════════════════╣
║ Group: 2 (Study)  ║ Control Method: TEMPERATURE_CONTROL  ║ Temperature: 26°C ║
║                   ║ Damper Open: 10%  ║ Power state: ON  ║ Target temp: 23°C ║
╠═══════════════════╬═══════════════════╩══════════════════╬═══════════════════╣
║ Group: 3 (Bed 2)  ║ Control Method: TEMPERATURE_CONTROL  ║ Temperature: 24°C ║
║                   ║ Damper Open: 15%  ║ Power state: ON  ║ Target temp: 23°C ║
╚═══════════════════╩═══════════════════╩══════════════════╩═══════════════════╝
Tab completion is enabled. Press tab at any time to show options
zone Master target-temp 22
```

### Logging
A log file named `airtouch-discovery-console.log` will be created in the current working directory.
Look in there to help determine any issues you may be facing. Please include the log when raising any issues.


