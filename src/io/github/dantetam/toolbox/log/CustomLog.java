package io.github.dantetam.toolbox.log;

import java.text.DecimalFormat;
import java.text.NumberFormat;

public class CustomLog {

	public static final PrintMode mode = PrintMode.OUT;
	
	public static void outPrintSameLine() {
		outPrintSameLine("");
	}
	public static void outPrintSameLine(Object object) {
		if (object == null)
			outPrintSameLine("null");
		else 
			outPrintSameLine(object.toString());
	}
	public static void outPrintSameLine(String string) {
		if (mode.equals(PrintMode.OUT)) {
			System.out.print(string);
		}
	}
	
	public static void outPrintln() {
		outPrintln("");
	}
	public static void outPrintln(Object object) {
		if (object == null)
			outPrintln("null");
		else 
			outPrintln(object.toString());
	}
	public static void outPrintln(String string) {
		if (mode.equals(PrintMode.OUT)) {
			System.out.println(string);
		}
	}
	
	public static void outPrintlnArr(int[][] arr) {
		int maxDigits = 0;
		for (int r = 0; r < arr.length; r++) {
			for (int c = 0; c < arr[0].length; c++) {
				maxDigits = Math.max((int) Math.floor(arr[r][c]), maxDigits);
			}
		}
		NumberFormat formatter = new DecimalFormat("0".repeat(maxDigits));
		
		for (int r = 0; r < arr.length; r++) {
			for (int c = 0; c < arr[0].length; c++) {
				CustomLog.outPrintSameLine(formatter.format(arr[r][c]) + " ");
			}
			CustomLog.outPrintln();
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
