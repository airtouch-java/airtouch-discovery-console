package airtouch.console.app;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Buffer;
import org.jline.reader.Candidate;
import org.jline.reader.Completer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.Expander;
import org.jline.reader.Highlighter;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.MaskingCallback;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.UserInterruptException;
import org.jline.reader.Widget;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;
import org.junit.Test;

import airtouch.console.data.AirtouchStatus;
import airtouch.constant.AirConditionerControlConstants.FanSpeed;
import airtouch.constant.AirConditionerControlConstants.Mode;
import airtouch.model.AirConditionerAbilityResponse;

public class CustomCompleterTest {

	@Test
	public void testGetCustomCompleter() throws IOException {
		LineReader lineReader = new TestLineReader();
		ParsedLine parsedLine = new TestParsedLine();
		AirtouchStatus airtouchStatus = new AirtouchStatus();
		airtouchStatus.setAcAbilities(getAcAbilities());
		airtouchStatus.setZoneNames(getZoneNames());
		Completer c = CustomCompleter.getCustomCompleter(airtouchStatus);
		List<Candidate> candidates = new ArrayList<>();
		//c.complete(lineReader, parsedLine, candidates);
	}

	private Map<Integer, String> getZoneNames() {
		Map<Integer, String> m = new HashMap<>();
		m.put(0, "Zone1");
		m.put(1, "Zone2");
		m.put(2, "Zone3");
		m.put(3, "Zone4");
		return m;
	}

	private Map<Integer, AirConditionerAbilityResponse> getAcAbilities() {
		Map<Integer, AirConditionerAbilityResponse> m = new HashMap<>();
		AirConditionerAbilityResponse r = new AirConditionerAbilityResponse();
		r.addSupportedFanSpeed(FanSpeed.AUTO);
		r.addSupportedFanSpeed(FanSpeed.HIGH);
		r.addSupportedFanSpeed(FanSpeed.LOW);
		r.addSupportedMode(Mode.AUTO);
		r.addSupportedMode(Mode.COOL);
		r.addSupportedMode(Mode.DRY);
		r.addSupportedMode(Mode.HEAT);
		r.addSupportedMode(Mode.COOL);
		m.put(0, r);
		return m;
	}

	private static class TestParsedLine implements ParsedLine {

		@Override
		public String word() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int wordCursor() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public int wordIndex() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public List<String> words() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String line() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int cursor() {
			// TODO Auto-generated method stub
			return 0;
		}
	}
	
	public static class TestLineReader implements LineReader {

		@Override
		public Map<String, KeyMap<Binding>> defaultKeyMaps() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String readLine() throws UserInterruptException, EndOfFileException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String readLine(Character mask) throws UserInterruptException, EndOfFileException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String readLine(String prompt) throws UserInterruptException, EndOfFileException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String readLine(String prompt, Character mask) throws UserInterruptException, EndOfFileException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String readLine(String prompt, Character mask, String buffer)
				throws UserInterruptException, EndOfFileException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String readLine(String prompt, String rightPrompt, Character mask, String buffer)
				throws UserInterruptException, EndOfFileException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String readLine(String prompt, String rightPrompt, MaskingCallback maskingCallback, String buffer)
				throws UserInterruptException, EndOfFileException {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void printAbove(String str) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void printAbove(AttributedString str) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean isReading() {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public LineReader variable(String name, Object value) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public LineReader option(Option option, boolean value) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void callWidget(String name) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Map<String, Object> getVariables() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Object getVariable(String name) {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setVariable(String name, Object value) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public boolean isSet(Option option) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public void setOpt(Option option) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void unsetOpt(Option option) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public Terminal getTerminal() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Map<String, Widget> getWidgets() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Map<String, Widget> getBuiltinWidgets() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Buffer getBuffer() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getAppName() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void runMacro(String macro) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public MouseEvent readMouseEvent() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public History getHistory() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Parser getParser() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Highlighter getHighlighter() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Expander getExpander() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public Map<String, KeyMap<Binding>> getKeyMaps() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getKeyMap() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public boolean setKeyMap(String name) {
			// TODO Auto-generated method stub
			return false;
		}

		@Override
		public KeyMap<Binding> getKeys() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public ParsedLine getParsedLine() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getSearchTerm() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public RegionType getRegionActive() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public int getRegionMark() {
			// TODO Auto-generated method stub
			return 0;
		}

		@Override
		public void addCommandsInBuffer(Collection<String> commands) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void editAndAddInBuffer(File file) throws Exception {
			// TODO Auto-generated method stub
			
		}

		@Override
		public String getLastBinding() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public String getTailTip() {
			// TODO Auto-generated method stub
			return null;
		}

		@Override
		public void setTailTip(String tailTip) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public void setAutosuggestion(SuggestionType type) {
			// TODO Auto-generated method stub
			
		}

		@Override
		public SuggestionType getAutosuggestion() {
			// TODO Auto-generated method stub
			return null;
		}
		
	}
}
