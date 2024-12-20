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

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IConstantsObject;

/**
 * This abstract class defines constants for specifying input types in the JasperReports plugin for Servoy, 
 * supporting formats like XML, CSV, custom datasources, and JDBC. These constants integrate with the 
 * <code>plugins.jasperReports.runReport</code> method to streamline report generation, enabling dynamic 
 * data handling and flexible compatibility with various data sources.
 */
@ServoyDocumented(category = ServoyDocumented.PLUGINS)
public abstract class INPUT_TYPE implements IConstantsObject {
	
	/**
	 * @sample
	 * var $parameters = null; //...
	 * var $repfile = 'report.jrxml';
	 * var $xmlDataCombined = plugins.file.readTXTFile('/path/to/datasource.xml');
	 * var $locale = 'en';
	 * plugins.jasperReports.runReport(
	 * 			plugins.jasperReports.INPUT_TYPE.XML,
	 * 			$xmlDataCombined,
	 * 			'/node/to/iterate/on',
	 * 			$repfile,
	 * 			null,
	 * 	    	OUTPUT_FORMAT.VIEW,
	 * 			$parameters,
	 * 			null)
	 * 
	 */
	public static final String XML = "xml";

	/**
	 * @sampleas XML
	 */
	public static final String CSV = "csv";
	/**
	 * @sampleas XML
	 */
	public static final String JRD = "jrdatasource";
	
	/**
	 * @sampleas XML
	 */
	public static final String DB = "JDBCdatabase";
}
