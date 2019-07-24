package io.github.dantetam.toolbox.log;

public class CustomLog {

	public static final PrintMode mode = PrintMode.ERR;
	
	public static void outPrintln() {
		outPrintln("");
	}
	public static void outPrintln(Object object) {
		outPrintln(object.toString());
	}
	public static void outPrintln(String string) {
		if (mode.equals(PrintMode.OUT)) {
			System.out.println(string);
		}
	}
	
	public static void errPrintln() {
		errPrintln("");
	}
	public static void errPrintln(Object object) {
		outPrintln(object.toString());
	}
	public static void errPrintln(String string) {
		if (!mode.equals(PrintMode.NONE))
			System.err.println(string);
	}
	
	public enum PrintMode { 
		OUT, //Show out and more urgent
		ERR, //Show err (most urgent)
		NONE //No messages except forcing errors
	}
	
}
