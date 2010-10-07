package com.servoy.plugins.jasperreports;

import java.util.ArrayList;

public class JasperReportsUtil {

	public static ArrayList<String> StringToArrayList(String sourceString) {
		
		if (sourceString == null || sourceString.equals(""))
			return null;
		
		ArrayList<String> resultList = new ArrayList<String>();	
		String[] sourceList = sourceString.split(",");
		String aux = null;
		
		for (int x = 0; x < sourceList.length; x++)
		{
			aux = sourceList[x].trim();
			if (!aux.equals("")) 
				resultList.add(aux);
		}			
		
		return resultList;
	}
}
