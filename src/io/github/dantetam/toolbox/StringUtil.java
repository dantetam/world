package io.github.dantetam.toolbox;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

public class StringUtil {

	//Sourced from https://stackoverflow.com/questions/41107/how-to-generate-a-random-alpha-numeric-string

    public static final String upper = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    public static final String lower = upper.toLowerCase();
    public static final String digits = "0123456789";
    public static final String alphanum = upper + lower + digits;
    private static Map<Character, Integer> alphanumIndices = new HashMap<>();
    
    public static void init() {
    	for (int i = 0; i < alphanum.length(); i++) {
    		alphanumIndices.put(alphanum.charAt(i), i);
    	}
    }
    
    /**
     * Generate a random string.
     */
	public static String genAlphaNumericStr(int length) {
		Random random = new Random();
    	char[] symbols = alphanum.toCharArray();
        char[] buf = new char[length];
        for (int idx = 0; idx < buf.length; ++idx)
            buf[idx] = symbols[random.nextInt(symbols.length)];
        return new String(buf);
    }
	
	public static List<String> genAlphaNumericStrList(int length, int times) {
		List<String> list = new ArrayList<>();
		for (int i = 0; i < times; i++) {
			list.add(genAlphaNumericStr(length));
		}
		return list;
	}

	public static char getNextCharOffset(char base, int offset) {
		int index = getIndexOfChar(base);
		index = MathUti.trueMod(index + offset, alphanum.length());
		return alphanum.charAt(index);
	}
	
	public static int getIndexOfChar(char base) {
		if (!alphanumIndices.containsKey(base)) {
			throw new IllegalArgumentException("This character offset method requires alphanumeric char, given: " + base);
		}
		return alphanum.indexOf(base);
	}

	public static String mutateAlphaNumStr(String string) {
		int rotateAmt = Math.random() < 0.5 ? 1 : -1;
		int randIndex = (int) (Math.random() * string.length());
		char rotateChar = StringUtil.getNextCharOffset(string.charAt(randIndex), rotateAmt);
		return string.substring(0,randIndex) + rotateChar + string.substring(randIndex+1);
	}

	public static String randMergeStrs(String race, String otherRace) {
		return randMergeStrs(race, otherRace, 0.5);
	}
	
	public static String randMergeStrs(String race, String otherRace, double weightToFirst) {
		String newRace = "";
		for (int index = 0; index < race.length(); index++) {
			if (Math.random() < weightToFirst) {
				newRace += race.charAt(index);
			}
			else {
				newRace += otherRace.charAt(index);
			}
		}
		return newRace;
	}
	
	/**
	 * 
	 * @param record
	 * @param keys
	 * @return true if the record contains 
	 */
	public static boolean validateCsvMap(Map<String, String> record, Iterable<String> keys) {
		for (String key: keys) {
			if (!record.containsKey(key) || record.get(key).isBlank()) {
				return false;
			}
		}
		return true;
	}
	
}
