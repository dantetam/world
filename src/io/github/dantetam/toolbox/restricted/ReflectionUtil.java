package io.github.dantetam.toolbox.restricted;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Big big warning:
 * 
 * This is only for convenient class/subclass method reading to format for the console.
 * Don't use this to actually modify any data across the software
 * 
 * @author Dante
 *
 */

public class ReflectionUtil {

	public static String getDeclaredToString(Object object) {
		Class<?> type = object.getClass();
		String string = type.getSimpleName() + ": ";
		List<Field> fields = getDeclaredFields(type);
		try {
			for (Field field: fields) {
				field.setAccessible(true);
				String fName = field.getName();
				string += "(" + field.getType().getSimpleName() + ") " + fName + " = " + field.get(object) + ", ";
			}
		}
		catch (IllegalAccessException e) {
			string += " null (bad toString)";
			e.printStackTrace();
		}
		return string;
	}
	
	public static List<Field> getDeclaredFields(Class<?> type) {
		return getDeclaredFields(new ArrayList<>(), type);
	}
	private static List<Field> getDeclaredFields(List<Field> fields, Class<?> type) {
	    fields.addAll(Arrays.asList(type.getDeclaredFields()));
	    return fields;
	}
	
}
