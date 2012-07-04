/*
 * ============================================================================
 * GNU Lesser General Public License
 * ============================================================================
 *
 * Servoy - Smart Technology For Smart Clients.
 * Copyright © 1997-2012 Servoy BV http://www.servoy.com
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307, USA.
 * 
 * Servoy B.V.
 * De Brand 26
 * 3823 LJ Amersfoort
 * The Netherlands
 * http://www.servoy.com
 */
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
