package io.github.dantetam.world.dataparse;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import io.github.dantetam.localdata.ConstantData;

/*
 * For use in this package for finding items that have missing texture files
 */

public class ItemTextureFilesAudit {

	public static void main(String[] args) {
		System.err.println();
		List<String> missingItemNames = getMissingItemTextures();
		Collections.sort(missingItemNames, new Comparator<String>() {
			@Override
			public int compare(String o1, String o2) {
				int id1 = ItemData.getIdFromName(o1), id2 = ItemData.getIdFromName(o2);
				return id1 - id2;
			}
		});
		StringBuilder javascriptSb = new StringBuilder();
		javascriptSb.append("const queries = [");
		for (int i = 0; i < missingItemNames.size(); i++) {
			javascriptSb.append("\"");
			javascriptSb.append(missingItemNames.get(i));
			javascriptSb.append("\"");
			if (i != missingItemNames.size() - 1)
				javascriptSb.append(",");
		}
		javascriptSb.append("];");
		System.err.println(javascriptSb.toString());
	}
	
	public static List<String> getMissingItemTextures() {
		List<String> missingItemNames = new ArrayList<>();
		ItemCSVParser.init();
		for (String itemName : ItemData.itemNamesToIds.keySet()) {
			String pathStr = ConstantData.getItemTexturePath(itemName);
			File imageFile = new File(pathStr);
			if (!imageFile.exists()) {
				missingItemNames.add(itemName);
			}
		}
		return missingItemNames;
	}
	
}
