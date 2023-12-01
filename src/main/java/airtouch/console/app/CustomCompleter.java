package airtouch.console.app;

import static org.jline.builtins.Completers.TreeCompleter.node;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.jline.builtins.Completers.TreeCompleter;
import org.jline.builtins.Completers.TreeCompleter.Node;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.LineReader;
import org.jline.reader.ParsedLine;

import airtouch.console.data.AirtouchStatus;
import airtouch.model.AirConditionerAbilityResponse;

public class CustomCompleter implements Completer {
    
    Completer delegate;

    @Override
    public void complete(LineReader reader, ParsedLine line, List<Candidate> candidates) {
        delegate.complete(reader, line, candidates);
    }

    public void setCompleter(Completer delegate) {
        this.delegate = delegate;
    }
    
    public static Completer getDefaultCompleter() {
		Completer completer = new TreeCompleter(
				/*node("ac",
					node("0", "1", "2", "3",
						node("power",
							node("on", "off")
						),
						node("mode",
							node("auto", "cool", "heat", "dry", "fan")
						)
					)
				),
				node("zone",
					node("0", "1", "2", "3",
						node("target-temp",
							node("20","21","22","23","24","25") // TODO, this should be the valid temps as determined by AcStatus
						),
						node("open-percentage",
							node("0", "5", "10", "15", "20", "25", "30", "35", "40", "45","50", "55", "60", "65", "70", "75", "80", "85", "90", "95", "100")
						),
						node("power",
							node("on", "off", "turbo")
						),
						node("control",
							node("temperature", "percentage")
						)
					)
				),*/
				node("quit")
				);
		return completer;
	}
	public static Completer getCustomCompleter(AirtouchStatus status) {
		Completer completer = new TreeCompleter(
				node("ac",
					buildAcNode(status.getAcAbilities())
				),
				node("zone",
					buildZoneNode(status.getAcAbilities(), status.getZoneNames())
				),
				node("quit")
				);

		return completer;
	}
	
	private static Node buildAcNode(Map<Integer, AirConditionerAbilityResponse> acAbilities) {
		return getAirConditionerIds(acAbilities,
				node("power",
						node("on", "off")
						),
				node("mode",
						getAcModes(acAbilities) // "auto", "cool", "heat", "dry", "fan"
						)
				);
	}
	
	private static Node getAirConditionerIds(Map<Integer, AirConditionerAbilityResponse> acAbilities, Node... nodes) {
		Set<String> ids = new TreeSet<>();
		acAbilities.forEach((i,v) -> ids.add(i.toString().toLowerCase()));
		return mergeNodes(ids, nodes);
	}

	private static Node getAcModes(Map<Integer, AirConditionerAbilityResponse> acAbilities) {
		Set<String> modes = new TreeSet<>();
		acAbilities.forEach((i,v) -> v.getSupportedModes().stream().forEach(m -> modes.add(m.toString().toLowerCase())));
		//return node((Object[])modes.toArray(new String[0]));
		return mergeNodes(modes);
	}

	private static Node buildZoneNode(Map<Integer, AirConditionerAbilityResponse> map, Map<Integer, String> zoneNames) {
		return getZoneNames(zoneNames,
				node("target-temp",
						node("20","21","22","23","24","25") // TODO, this should be the valid temps as determined by AcStatus
					),
					node("open-percentage",
						node("0", "5", "10", "15", "20", "25", "30", "35", "40", "45","50", "55", "60", "65", "70", "75", "80", "85", "90", "95", "100")
					),
					node("power",
						node("on", "off", "turbo")
					),
					node("control",
						node("temperature", "percentage")
					)
				);
	}
	
	private static Node mergeNodes (Collection<String> values, Node... nodes) {
		Object[] newNodes = new Object[nodes.length + values.size()];
		System.arraycopy((Object[])values.toArray(new String[0]), 0, newNodes, 0, values.size());
		System.arraycopy(nodes, 0, newNodes, values.size(), nodes.length);
		return node(newNodes);
	}

	private static Node getZoneNames(Map<Integer, String> zoneNames, Node... nodes) {
		return mergeNodes(zoneNames.values(), nodes);
	}
}