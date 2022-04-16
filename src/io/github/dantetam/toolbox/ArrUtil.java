package io.github.dantetam.toolbox;

public class ArrUtil {

	public static <T> T safeIndex(T[] arr, int index, T backup) {
		if (index >= 0 && index < arr.length) return arr[index];
		return backup;
	}
	
}
