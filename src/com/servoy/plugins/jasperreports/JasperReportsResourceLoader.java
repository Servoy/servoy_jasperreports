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

import java.awt.Image;

import net.sf.jasperreports.engine.JRTemplate;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRImageLoader;

public class JasperReportsResourceLoader {

	public static Image loadImage(String image) throws Exception {
		
		IJasperReportsService jasperReportsService = JasperReportsProvider.jasperReportsLocalService.get();
		String jasperReportsClientId = JasperReportsProvider.jasperReportsLocalClientID.get();
		
		if (jasperReportsService == null || jasperReportsClientId == null)
		{	
			return null;
		}
		
		byte[] imgBytes = jasperReportsService.loadImage(jasperReportsClientId, image);
		
		return JRImageLoader.loadImage(imgBytes);
	}
	
	public static JasperReport loadReport(String location) throws Exception {
		
		IJasperReportsService jasperReportsService = JasperReportsProvider.jasperReportsLocalService.get();
		String jasperReportsClientId = JasperReportsProvider.jasperReportsLocalClientID.get();
		
		if (jasperReportsService == null || jasperReportsClientId == null)
		{	
			return null;
		}
		
		JasperReport report = jasperReportsService.loadReport(jasperReportsClientId, location);
				
		return report;
	}
	
	public static JRTemplate loadStyle(String name) throws Exception {
		
		IJasperReportsService jasperReportsService = JasperReportsProvider.jasperReportsLocalService.get();
		String jasperReportsClientId = JasperReportsProvider.jasperReportsLocalClientID.get();
		
		if (jasperReportsService == null || jasperReportsClientId == null)
		{	
			return null;
		}
	
		JRTemplate style = jasperReportsService.loadTemplate(jasperReportsClientId, name);
		
		return style;
	}
}
