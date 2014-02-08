/*
 * ============================================================================
 * GNU Lesser General Public License
 * ============================================================================
 *
 * Servoy - Smart Technology For Smart Clients.
 * Copyright � 1997-2012 Servoy BV http://www.servoy.com
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

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;
import net.sf.jasperreports.engine.JRTemplate;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.util.JRImageLoader;

/**
 * This is a helper class for loading resources from the server.
 * It is used (mainly) for resource loading in the context of foundset based reports.
 *
 * @author acostache
 *
 */
public class JasperReportsResourceLoader {

	/**
	 * This methods loads the indicated image from the server resource directory.
	 * @param image the name of the image to be loaded
	 * @return the requested image as a java.awt.Image object
	 * @throws Exception
	 */
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
	
	/**
	 * This methods loads the report from the indicated (relative) location from the server.
	 * @param location relative location of the requested report
	 * @return the report as a JasperReport object
	 * @throws Exception
	 */
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
	
	/**
	 * This method loads the named style from the server.
	 * @param name the name of the needed style to load
	 * @return the requested style as a JRTemplate object
	 * @throws Exception
	 */
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
	
	/**
	 * This is a helper method for rewinding the used (rewindable) datasource.
	 * A datasource is rewindable if it implements net.sf.jasperreports.engine.JRRewindableDataSource.
	 * This method is needed when using the same datasource for two subreports.
	 * See also http://community.jaspersoft.com/questions/521291/can-2-subreports-share-same-datasource
	 */
	public static JRRewindableDataSource rewindServoyDataSource(AbstractServoyDataSource dataSource) throws JRException {
		dataSource.moveFirst();
		return dataSource;
	}
}
