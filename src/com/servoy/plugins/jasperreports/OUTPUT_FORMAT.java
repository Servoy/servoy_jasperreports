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
 * The <code>OUTPUT_FORMAT</code> class defines constants for various JasperReports export formats, 
 * including PDF, HTML, XML, XLS, CSV, and JSON. These constants enable developers to specify the 
 * desired output format when generating reports in Servoy.
 */
@ServoyDocumented(category = ServoyDocumented.PLUGINS)
public abstract class OUTPUT_FORMAT implements IConstantsObject 
{
	/**
	 * @sample
	 * //plugins.jasperReports.runReport(myServer,'myCustomerReport.jrxml','c:/myReport.html',OUTPUT_FORMAT.XHTML,null);
	 * //plugins.jasperReports.runReport(myServer,'myCustomerReport.jrxml','c:/myReport.html',OUTPUT_FORMAT.HTML,null);
	 * //plugins.jasperReports.runReport(myServer,'myCustomerReport.jrxml','c:/myReport.rtf',OUTPUT_FORMAT.RTF,null);
	 * //plugins.jasperReports.runReport(myServer,'myCustomerReport.jrxml','c:/myReport.txt',OUTPUT_FORMAT.TXT,null);
	 * //plugins.jasperReports.runReport(myServer,'myCustomerReport.jrxml','c:/myReport.csv',OUTPUT_FORMAT.CSV,null);
	 * //plugins.jasperReports.runReport(myServer,'myCustomerReport.jrxml','c:/myReport.xml',OUTPUT_FORMAT.XML,null);
	 * //plugins.jasperReports.runReport(myServer,'myCustomerReport.jrxml','c:/myReport.pdf',OUTPUT_FORMAT.PDF,null);
	 * //plugins.jasperReports.runReport(myServer,'myCustomerReport.jrxml','c:/myReport.xls',OUTPUT_FORMAT.XLS,null);
	 * //plugins.jasperReports.runReport(myServer,'myCustomerReport.jrxml','c:/myReport.xls',OUTPUT_FORMAT.XLS_1_SHEET,null);
	 * //plugins.jasperReports.runReport(myServer,'myCustomerReport.jrxml','c:/myReport.xlsx',OUTPUT_FORMAT.XLSX,null);
	 * //plugins.jasperReports.runReport(myServer,'myCustomerReport.jrxml','c:/myReport.xls',OUTPUT_FORMAT.EXCEL,null);
	 * //plugins.jasperReports.runReport(myServer,'myCustomerReport.jrxml','c:/myReport.odt',OUTPUT_FORMAT.ODT,null);
	 * //plugins.jasperReports.runReport(myServer,'myCustomerReport.jrxml','c:/myReport.ods',OUTPUT_FORMAT.ODS,null);
	 * //plugins.jasperReports.runReport(myServer,'myCustomerReport.jrxml','c:/myReport.docx',OUTPUT_FORMAT.DOCX,null);
	 * //plugins.jasperReports.runReport(myServer,'myCustomerReport.jrxml','c:/myReport.json',OUTPUT_FORMAT.JSON,null);
	 * //plugins.jasperReports.runReport(myServer,'myCustomerReport.jrxml','c:/myReport.csv',OUTPUT_FORMAT.CSV_METADATA,null);
	 * //plugins.jasperReports.runReport(myServer,'myCustomerReport.jrxml','c:/myReport.jrprint',OUTPUT_FORMAT.JRPRINT,null);
	 * //plugins.jasperReports.runReport(myServer,'myCustomerReport.jrxml',null,OUTPUT_FORMAT.VIEW,null);
	 * //plugins.jasperReports.runReport(myServer,'myCustomerReport.jrxml',null,OUTPUT_FORMAT.PRINT,null);
	 */
	public static final String XHTML = "xhtml";
	
	/**
	 * @sampleas XHTML
	 */
	public static final String HTML = "html";
	
	/**
	 * @sampleas XHTML
	 */
	public static final String RTF = "rtf";
	
	/**
	 * @sampleas XHTML
	 */
	public static final String TXT = "txt";
	
	/**
	 * @sampleas XHTML
	 */
	public static final String CSV = "csv";
	
	/**
	 * @sampleas XHTML
	 */
	public static final String XML = "xml";
	
	/**
	 * @sampleas XHTML
	 */
	public static final String PDF = "pdf";
	
	/**
	 * @sampleas XHTML
	 */
	public static final String XLS = "xls";
	
	/**
	 * @sampleas XHTML
	 */
	public static final String XLSX = "xlsx";
	
	/**
	 * @sampleas XHTML
	 */
	public static final String XLS_1_SHEET = "xls_1_sheet";
	
	/**
	 * @sampleas XHTML
	 */
	public static final String EXCEL = "excel";
	
	/**
	 * @sampleas XHTML
	 */
	public static final String ODS = "ods";
	
	/**
	 * @sampleas XHTML
	 */
	public static final String ODT = "odt";
	
	/**
	 * @sampleas XHTML
	 */
	public static final String DOCX = "docx";
	
	/**
	 * @sampleas XHTML
	 */
	public static final String JRPRINT = "jrprint";
	
	/**
	 * @sampleas XHTML
	 */
	public static final String VIEW = "view";
	
	/**
	 * @sampleas XHTML
	 */
	public static final String PRINT = "print";
	
	/**
	 * @sampleas XHTML
	 */
	public static final String JSON_METADATA = "json_metadata";
	
	/**
	 * @sampleas XHTML
	 */
	public static final String CSV_METADATA = "csv_metadata";	
}
