package io.github.dantetam.toolbox;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

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

	public static char getNextCharOffset(char base, int offset) {
		int index = getIndexOfChar(base);
		index = (index + offset) % alphanum.length();
		return alphanum.charAt(index);
	}
	
	public static int getIndexOfChar(char base) {
		if (alphanumIndices.containsKey(base)) {
			throw new IllegalArgumentException("This character offset method requires alphanumeric char");
		}
		return alphanum.indexOf(base);
	}

	public static String mutateAlphaNumStr(String newRace) {
		int rotateAmt = Math.random() < 0.5 ? 1 : -1;
		int randIndex = (int) (Math.random() * newRace.length());
		char rotateChar = StringUtil.getNextCharOffset(newRace.charAt(randIndex), rotateAmt);
		return newRace.substring(0,randIndex) + rotateChar + newRace.substring(randIndex+1);
	}
	
}
