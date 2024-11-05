/*
 * ============================================================================ GNU Lesser General Public License
 * ============================================================================
 * 
 * Servoy - Smart Technology For Smart Clients. Copyright � 1997-2012 Servoy BV http://www.servoy.com
 * 
 * This library is free software; you can redistribute it and/or modify it under the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with this library; if not, write to the Free Software Foundation, Inc., 59
 * Temple Place, Suite 330, Boston, MA 02111-1307, USA.
 * 
 * Servoy B.V. De Brand 26 3823 LJ Amersfoort The Netherlands http://www.servoy.com
 */
package com.servoy.plugins.jasperreports;

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.print.PrintService;
import javax.swing.ImageIcon;
import javax.swing.WindowConstants;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.JRStyle;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.JasperReportsContext;
import net.sf.jasperreports.engine.type.OrientationEnum;
import net.sf.jasperreports.engine.util.JRClassLoader;
import net.sf.jasperreports.engine.util.JRSaver;
import net.sf.jasperreports.engine.util.JRStyledTextUtil;
import net.sf.jasperreports.swing.JRViewer;
import net.sf.jasperreports.swing.JRViewerToolbar;
import net.sf.jasperreports.view.JRSaveContributor;

import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * IScriptObject impl. For external library dependencies, see https://community.jaspersoft.com/wiki/jasperreports-library-requirements.
 */
@ServoyDocumented(category = ServoyDocumented.PLUGINS, publicName = JasperReportsPlugin.PLUGIN_NAME, scriptingName = "plugins." + JasperReportsPlugin.PLUGIN_NAME)
public class JasperReportsProvider implements IScriptable, IReturnedTypesProvider
{
	private final JasperReportsPlugin plugin;

	protected static ThreadLocal<IJasperReportsService> jasperReportsLocalService = new ThreadLocal<IJasperReportsService>();
	protected static ThreadLocal<String> jasperReportsLocalClientID = new ThreadLocal<String>();

	private String[] viewerExportFormats = null;
	private String viewerTitle;
	private String viewerIconURL;

	private String relativeReportsDir;
	private String relativeExtraDirs;

	JasperReportsProvider(JasperReportsPlugin p)
	{
		plugin = p;
		relativeReportsDir = "";
		relativeExtraDirs = "";
	}

    // default constructor, used for documentation generation only
	public JasperReportsProvider()
	{
		plugin = null;
		relativeReportsDir = "";
		relativeExtraDirs = "";
	}

	@Override
	public Class<?>[] getAllReturnedTypes()
	{
		return new Class[] { INPUT_TYPE.class, OUTPUT_FORMAT.class, JR_SVY_VIEWER_DISPLAY_MODE.class };
	}

	/**
	 * Fix for bug ID 6255588 from Sun bug database
	 * 
	 * @param job print job that the fix applies to
	 */
	public static void initPrinterJobFields(PrinterJob job)
	{
		Class<?> clazz = job.getClass();
		try
		{
			Class<?> printServiceClass = Class.forName("javax.print.PrintService");
			Method method = clazz.getMethod("getPrintService", (Class[]) null);
			Object printService = method.invoke(job, (Object[]) null);
			method = clazz.getMethod("setPrintService", new Class[] { printServiceClass });
			method.invoke(job, new Object[] { printService });
		}
		catch (Exception e)
		{
			Debug.error(e);
		}
	}

	public boolean setPrintService(PrinterJob printJob, String printer) throws Exception
	{
		PrintService[] service = PrinterJob.lookupPrintServices();
		// enable if on list otherwise error
		boolean match = false;
		int count = service.length;
		try
		{
			for (int i = 0; i < count; i++)
			{
				if (service[i].getName().indexOf(printer) != -1)
				{
					printJob.setPrintService(service[i]);
					i = count;
					match = true;
				}
			}
		}
		catch (PrinterException e)
		{
			Debug.error(e);
			throw new Exception(e.getMessage());
		}
		return match;
	}

	/**
	 * @param dbalias the data base alias (server name or foundset)
	 * @param report the report file
	 * @param arg the output file to write to
	 * @param type the output file format
	 * @param parameters the map of user specified parameters to use when running the report
	 * 
	 * @deprecated replaced by runReport(String, String, Object, String, Object)
	 */
	@Deprecated
	public byte[] js_jasperReport(String dbalias, String report, Object arg, String type, Object parameters) throws Exception
	{
		return js_runReport(dbalias, report, arg, type, parameters, null);
	}

	/**
	 * @param dbalias the data base alias (server name or foundset)
	 * @param report the report file
	 * @param arg the output file to write to
	 * @param type the output file format
	 * @param parameters the map of user specified parameters to use when running the report
	 * @param localeString the string which specifies the locale
	 * 
	 * @deprecated replaced by runReport(String, String, Object, String, Object, String)
	 */
	@Deprecated
	public byte[] js_jasperReport(String dbalias, String report, Object arg, String type, Object parameters, String localeString) throws Exception
	{
		return js_runReport(dbalias, report, arg, type, parameters, localeString);
	}

	/**
	 * @clonedesc js_runReport(String,Object,String,String,Object,String,Object,String,Boolean)
	 * @sampleas js_runReport(String,Object,String,String,Object,String,Object,String,Boolean)
	 * 
	 * @param reportDataSource the server name or foundset to run the report on
	 * @param report the report file (relative to the reports directory)
	 * @param outputOptions the output file (must specify an absolute path) or null if not needed
	 * @param outputType the output format; use the constants node for available output formats
	 * @param parameters a parameter map to be used when running the report
	 * 
	 * @return the generated reported as a byte array
	 */
	public byte[] js_runReport(Object reportDataSource, String report, Object outputOptions, String outputType, Object parameters) throws Exception
	{
		return js_runReport(null, reportDataSource, null, report, outputOptions, outputType, parameters, null);
	}
	
	/**
	 * @clonedesc js_runReport(String,Object,String,String,Object,String,Object,String,Boolean)
	 * @sampleas js_runReport(String,Object,String,String,Object,String,Object,String,Boolean)
	 * 
	 * @param inputType the type of the datasource, as one of the constants in INPUT_TYPE
	 * @param reportDataSource the server name or foundset to run the report on
	 * @param inputOptions additional input options (e.g. which node to iterate in the xml datasource document)
	 * @param report the report file (relative to the reports directory)
	 * @param outputOptions the output file (must specify an absolute path) or null if not needed
	 * @param outputType the output format; use the constants node for available output formats
	 * @param parameters a parameter map to be used when running the report
	 * @return the generated reported as a byte array
	 * @throws Exception
	 */
	public byte[] js_runReport(String inputType, Object reportDataSource, String inputOptions, String report, Object outputOptions, String outputType, Object parameters) throws Exception
	{
		return js_runReport(inputType, reportDataSource, inputOptions, report, outputOptions, outputType, parameters, null);
	}

	/**
	 * @clonedesc js_runReport(String,Object,String,String,Object,String,Object,String,Boolean)
	 * @sampleas js_runReport(String,Object,String,String,Object,String,Object,String,Boolean)
	 * 
	 * @param reportDataSource the server name or foundset to run the report on
	 * @param report the report file (relative to the reports directory)
	 * @param outputOptions the output file (must specify an absolute path) or null if not needed
	 * @param outputType the output format; use the constants node for available output formats
	 * @param parameters a parameter map to be used when running the report
	 * @param localeString the string which specifies the locale
	 * 
	 * @return the generated reported as a byte array
	 */
	public byte[] js_runReport(Object reportDataSource, String report, Object outputOptions, String outputType, Object parameters, String localeString) throws Exception
	{
		return js_runReport(null, reportDataSource, null, report, outputOptions, outputType, parameters, localeString, Boolean.FALSE);
	}
	
	/**
	 * @clonedesc js_runReport(String,Object,String,String,Object,String,Object,String,Boolean)
	 * @sampleas js_runReport(String,Object,String,String,Object,String,Object,String,Boolean)
	 * 
	 * @param inputType the type of the datasource, as one of the constants in INPUT_TYPE
	 * @param reportDataSource reportDataSource the server name or foundset to run the report on
	 * @param inputOptions additional input options (e.g. which node to iterate in the xml datasource document)
	 * @param report the report file (relative to the reports directory)
	 * @param outputOptions the output file (must specify an absolute path) or null if not needed
	 * @param outputType the output format; use the constants node for available output formats
	 * @param parameters a parameter map to be used when running the report
	 * @param localeString a string which indicates the locale
	 * @return the generated reported as a byte array
	 * @throws Exception
	 */
	public byte[] js_runReport(String inputType, Object reportDataSource, String inputOptions, String report, Object outputOptions, String outputType, Object parameters, String localeString) throws Exception
	{
		return js_runReport(inputType, reportDataSource, inputOptions, report, outputOptions, outputType, parameters, localeString, Boolean.FALSE);
	}

	/**
	 * @clonedesc js_runReport(String,Object,String,String,Object,String,Object,String,Boolean)
	 * @sampleas js_runReport(String,Object,String,String,Object,String,Object,String,Boolean) 
	 * 
	 * @param reportDataSource the server name or foundset to run the report on
	 * @param report the report file (relative to the reports directory)
	 * @param outputOptions the output file (must specify an absolute path) or null if not needed
	 * @param outputType the output format; use the constants node for available output formats
	 * @param parameters a parameter map to be used when running the report
	 * @param localeString the string which specifies the locale
	 * @param moveTableOfContent true in order to move the table of contents, false otherwise
	 * 
	 * @return the generated reported as a byte array
	 */
	public byte[] js_runReport(Object reportDataSource, String report, Object outputOptions, String outputType, Object parameters, String localeString, Boolean moveTableOfContent) throws Exception
	{
		return js_runReport(null, reportDataSource, null, report, outputOptions, outputType, parameters, localeString, Boolean.TRUE.equals(moveTableOfContent));
	}
	
	/**
	 * This method runs a specified (client) report according to the output format, parameters and locale. 
	 * If using a table of contents and if needed, the table of contents can be moved to a specified page. 
	 * Please refer to the sample code for more details.
	 * 
	 * @sample 
	 * // The method runs a client report specified by the second parameter acording to the output format. 
	 * // The report can be a compiled jasper file or a jrxml file from a relative path to the reportDirectory or an absolute one. 
	 * // To view the result of the customers report in the Jasper Report viewer in the SmartClient or as PDF in the WebClient. 
	 * // Note: the parameters argument is used to send additional parameters into the report. For example: 
	 * // {pcustomerid: forms.customers.customer_id} to send just 1 parameter called pcustomerid, which contains the value 
	 * // of dataprovider customer_id in the selected record on the customers form 
	 * // The parameters argument is an Object, which can be instantiated in two ways: 
	 * // var o = new Object(); 
	 * // o.pcustomerid = forms.customers.customer_id; 
	 * // or: 
	 * // var o = {pcustomerid: forms.customers.customer_id}; 
	 * application.updateUI(); //to make sure the Servoy window doesn't grab focus after showing the Jasper Viewer var
	 * ds = foundset.getDataSource(); 
	 * var d = ds.split('/); 
	 * var myServer = d[1];
	 * plugins.jasperPluginRMI.runReport(myServer, 'myCustomerReport.jasper',null,OUTPUT_FORMAT.VIEW,{pcustomerid: forms.customers.customer_id});
	 * 
	 * // To request a report in a different Language than the current language of the client, it's possible to specify a Locale string
	 * // as the locale argument. For example: 'en_US' or 'es_ES' or 'nl_NL' 
	 * // When the locale argument is not specified, the report will be in the current langauge of the Client 
	 * // i18n keys of Servoy can be used inside Jasper Reports using the $R{i18n-key} notation 
	 * //plugins.jasperPluginRMI.runReport(myServer,'myCustomerReport.jasper',null,OUTPUT_FORMAT.VIEW,{pcustomerid: forms.customers.customer_id},'nl_NL');
	 * 
	 * // To print the result of the customers report in the SmartClient (to a specified printer), 
	 * // the outputType should be specified as 'print' (OUTPUT_FORMAT.PRINT). 
	 * // The third parameter can contain the name of the printer to  which the report needs to be printed 
	 * // or can contain true (boolean value) to show a print dialog before printing. 
	 * // If false (boolean value) or null is specified, it will print without showing the print dialog to the default printer. 
	 * // Note: In the WebClient a PDF will be pushed to the Client when the outputType is specified as 'print' (OUTPUT_FORMAT.PRINT). 
	 * //plugins.jasperPluginRMI.runReport(myServer,'myCustomerReport.jasper',null,OUTPUT_FORMAT.PRINT,{pcustomerid: forms.customers.customer_id});
	 * 
	 * // To generate the report in the specified output format and save the result to 'myReport.html' in the root of the C drive: 
	 * // Supported output formats are: xhtml, html, pdf, excel( or xls), xls_1_sheet (1 page per sheet), xlsx, ods, rtf, txt, csv, odt, docx, jrprint and xml. 
	 * // These are available as constants in the OUTPUT_FORMAT node of the plugin's tree. 
	 * // Note: in the WebClient, the file will be saved serverside, so the specified path needs to be valid serverside 
	 * //plugins.jasperPluginRMI.runReport(myServer,'myCustomerReport.jasper','c:/myReport.xhtml',OUTPUT_FORMAT.XHTML,{pcustomerid: forms.customers.customer_id}); 
	 * //plugins.jasperPluginRMI.runReport(myServer,'myCustomerReport.jasper','c:/myReport.html',OUTPUT_FORMAT.HTML,{pcustomerid: forms.customers.customer_id});
	 * //plugins.jasperPluginRMI.runReport(myServer,'myCustomerReport.jasper','c:/myReport.pdf',OUTPUT_FORMAT.PDF,{pcustomerid: forms.customers.customer_id});
	 * //plugins.jasperPluginRMI.runReport(myServer,'myCustomerReport.jasper','c:/myReport.rtf',OUTPUT_FORMAT.RTF,{pcustomerid: forms.customers.customer_id});
	 * //plugins.jasperPluginRMI.runReport(myServer,'myCustomerReport.jasper','c:/myReport.jrprint',OUTPUT_FORMAT.JRPRINT,{pcustomerid: forms.customers.customer_id}); 
	 * //plugins.jasperPluginRMI.runReport(myServer,'myCustomerReport.jasper','c:/myReport.txt',OUTPUT_FORMAT.TXT,{pcustomeri d : forms.customers.customer_id});
	 * //plugins.jasperPluginRMI.runReport(myServer,'myCustomerReport.jasper','c:/myReport.csv',OUTPUT_FORMAT.CSV,{pcustomeri d : forms.customers.customer_id});
	 * //plugins.jasperPluginRMI.runReport(myServer,'myCustomerReport.jasper','c:/myReport.odt',OUTPUT_FORMAT.ODT,{pcustomeri d : forms.customers.customer_id});
	 * //plugins.jasperPluginRMI.runReport(myServer,'myCustomerReport.jasper','c:/myReport.docx',OUTPUT_FORMAT.DOCX,{pcustomerid: forms.customers.customer_id});
	 * //plugins.jasperPluginRMI.runReport(myServer,'myCustomerReport.jasper','c:/myReport.xls',OUTPUT_FORMAT.XLS,{pcustomerid: forms.customers.customer_id});
	 * //plugins.jasperPluginRMI.runReport(myServer,'myCustomerReport.jasper','c:/myReport.xlsx',OUTPUT_FORMAT.XLSX,{pcustomerid: forms.customers.customer_id});
	 * //plugins.jasperPluginRMI.runReport(myServer,'myCustomerReport.jasper','c:/myReport.xls',OUTPUT_FORMAT.XLS_1_SHEET,{pcustomerid: forms.customers.customer_id}); 
	 * //plugins.jasperPluginRMI.runReport(myServer,'myCustomerReport.jasper','c:/myReport.ods',OUTPUT_FORMAT.ODS,{pcustomerid: forms.customers.customer_id});
	 * //plugins.jasperPluginRMI.runReport(myServer,'myCustomerReport.jasper','c:/myReport.xml',OUTPUT_FORMAT.XML,{pcustomerid: forms.customers.customer_id});
	 * 
	 * // Jasper Reports supports queries with IN operators through the following notation: 
	 * // X${IN,columnName,parameterName} like 'select * from customers where X$(IN,customer_id,pcustomeridlist) 
	 * // When using this notation, the pcustomeridlist parameter needs to contain one or more values in the following way:
	 * //var idlist = new Array(); 
	 * //idlist[0] = 1; 
	 * //idlist[1] = 26 ;
	 * //plugins.jasperPluginRMI.jasperReport(myServer,'myCustomerReport.jasper',null,OUTPUT_FORMAT.VIEW,{pcustomeridlist: idlist}); 
	 * 
	 * //The return value is a byte array with the content of the file generated that can be further used. 
	 * //var res = plugins.jasperPluginRMI.runReport(myServer,'samplereport.jrxml', null, OUTPUT_FORMAT.PDF, null);
	 * //plugins.file.writeFile('e:\\\\sample.pdf', res);
	 * 
	 * // In order to run the report and move the table of contents(marked with the string: \"HIDDEN TEXT TO MARK THE BEGINNING OF THE TABEL OF CONTENTS\") 
	 * // to the Insert page, which has to be identified by the string: \"HIDDEN TEXT TO MARK THE INSERT PAGE\" 
	 * //plugins.jasperPluginRMI.runReport(myServer,'myCustomerReport.jasper','c:/myReport.xml',OUTPUT_FORMAT.XML,{pcustomerid: forms.customers.customer_id}, null,true);
	 * 
	 * // Pass exporter parameters to the export process in runReport 
	 * //var params = new Object();
	 * //params["EXPORTER_PARAMETER:net.sf.jasperreports.export.pdf.metadata.title"] =  "Test title"; 
	 * //params["EXPORTER_PARAMETER:net.sf.jasperreports.export.pdf.metadata.author" ] = "Test Author";
	 * //params["EXPORTER_PARAMETER:net.sf.jasperreports.export.pdf.metadata.creator"] =  "Test creator"; 
	 * //params["EXPORTER_PARAMETER:net.sf.jasperreports.export.pdf.metadata.keywords" ] = "Test keywords";
	 * //params["EXPORTER_PARAMETER:net.sf.jasperreports.export.pdf.metadata.subject"] =  "Test subject";
	 * //var r = plugins.jasperPluginRMI.runReport("myServer","someReport.jrxml","path/to/someReportExported.pdf",OUTPUT_FORMAT.PDF,params);
	 * 
	 * // Using an XML/CSV file as the datasource of the report.
	 * //var $parameters = null; //...
	 * //var $repfile = 'report.jrxml';
	 * //var $xmlDataCombined = plugins.file.readTXTFile('/path/to/datasource.xml');
	 * //var $locale = 'en';
	 * //plugins.jasperPluginRMI.runReport(plugins.jasperPluginRMI.INPUT_TYPE.XML, $xmlDataCombined, '/node/to/iterate/on', $repfile, null, OUTPUT_FORMAT.VIEW, $parameters, null)
	 * 
	 * @param inputType the type of the datasource, as one of the constants in INPUT_TYPE
	 * @param reportDataSource the server name or foundset to run the report on
	 * @param inputOptions additional input options (e.g. which node to iterate in the xml datasource document)
	 * @param report the report file (relative to the reports directory)
	 * @param outputOptions the output file (must specify an absolute path) or null if not needed
	 * @param outputType the output format; use the constants node for available output formats
	 * @param parameters a parameter map to be used when running the report
	 * @param localeString a string which indicates the locale
	 * @param moveTableOfContent true in order to move the table of contents, false otherwise 
	 * @return the generated reported as a byte array
	 * @throws Exception
	 */
	public byte[] js_runReport(String inputType, Object reportDataSource, String inputOptions, String report, Object outputOptions, String outputType, Object parameters, String localeString, Boolean moveTableOfContent) throws Exception
	{
		return runReport(inputType, reportDataSource, inputOptions, report, outputOptions, outputType, parameters, localeString, Boolean.TRUE.equals(moveTableOfContent), false);
	}
	
	/**
	 * @clonedesc js_runReport(String,Object,String,String,Object,String,Object,String,Boolean)
	 * @sampleas js_runReport(String,Object,String,String,Object,String,Object,String,Boolean)
	 * 
	 * @param reportDataSource the server name or foundset to run the report on
	 * @param report the report file (relative to the reports directory)
	 * @param outputOptions the output file (must specify an absolute path) or null if not needed
	 * @param outputType the output format; use the constants node for available output formats
	 * @param parameters a parameter map to be used when running the report
	 * @param localeString a string which indicates the locale
	 * @param moveTableOfContent true in order to move the table of contents, false otherwise
	 * @return the generated reported as a byte array
	 * @throws Exception
	 */
	// public, but not scriptable - just for the corresponding Bean's usage
	public byte[] runReportForBean(Object reportDataSource, String report, Object outputOptions, String outputType, Object parameters, String localeString, Boolean moveTableOfContent) throws Exception
	{
		return runReportForBean(null, reportDataSource, null, report, outputOptions, outputType, parameters, localeString, moveTableOfContent);
	}
	
	/**
	 * @clonedesc runReportForBean(Object, String, Object, String, Object, String, Boolean)
	 * @sampleas runReportForBean(Object, String, Object, String, Object, String, Boolean)
	 * 
	 * @param inputType the type of the datasource, as one of the constants in INPUT_TYPE
	 * @param reportDataSource the server name or foundset to run the report on
	 * @param inputOptions additional input options (e.g. which node to iterate in the xml datasource document)
	 * @param report the report file (relative to the reports directory)
	 * @param outputOptions the output file (must specify an absolute path) or null if not needed
	 * @param outputType the output format; use the constants node for available output formats
	 * @param parameters a parameter map to be used when running the report
	 * @param localeString a string which indicates the locale
	 * @param moveTableOfContent true in order to move the table of contents, false otherwise 
	 * 
	 * @return the generated reported as a byte array
	 * 
	 * @throws Exception
	 */
	public byte[] runReportForBean(String inputType, Object reportDataSource, String inputOptions, String report, Object outputOptions, String outputType, Object parameters, String localeString, Boolean moveTableOfContent) throws Exception
	{
		return runReport(inputType, reportDataSource, inputOptions, report, outputOptions, outputType, parameters, localeString, Boolean.TRUE.equals(moveTableOfContent), true);
	}

	/**
	 * @clonedesc runReportForBean(Object, String, Object, String, Object, String, Boolean)
	 * @sampleas runReportForBean(Object, String, Object, String, Object, String, Boolean)
	 * 
	 * @param inputType the type of the datasource, as one of the constants in INPUT_TYPE
	 * @param reportDataSource the server name or foundset to run the report on
	 * @param inputOptions additional input options (e.g. which node to iterate in the xml datasource document)
	 * @param reportName the report file (relative to the reports directory)
	 * @param outputOptions the output file (must specify an absolute path) or null if not needed
	 * @param outputType the output format; use the constants node for available output formats
	 * @param parameters a parameter map to be used when running the report
	 * @param localeString a string which indicates the locale
	 * @param moveTableOfContent true in order to move the table of contents, false otherwise
	 * @param returnJustJasperPrint true if we want to use the current functin for the bean viewer functionality
	 * 
	 * @return the generated reported as a byte array
	 * 
	 * @throws Exception
	 */
	private byte[] runReport(String inputType, Object reportDataSource, String inputOptions, String reportName, Object outputOptions, String outputType, Object parameters, String localeString, boolean moveTableOfContent, boolean returnJustJasperPrint) throws Exception
	{
		// Check if the directory.jasper.report setting has not yet been set.
		String pluginReportsDirectory = plugin.getJasperReportsDirectory();
		if (pluginReportsDirectory == null || (pluginReportsDirectory != null && ("").equals(pluginReportsDirectory.trim())))
		{
			String noPluginDirMsg = "Your jasper.report.directory setting has not been set.\nReport running will abort.";
			Debug.error(noPluginDirMsg);
			throw new Exception(noPluginDirMsg);
		}

		// unwrapping of datasource and parameters
		reportDataSource = JSArgumentsUnwrap.unwrapJSObject(reportDataSource, plugin.getIClientPluginAccess());
		@SuppressWarnings("unchecked")
		Map<String, Object> params = (Map<String, Object>) JSArgumentsUnwrap.unwrapJSObject(parameters, plugin.getIClientPluginAccess());
		if (params == null) {
			params = new HashMap<String, Object>();
		}
		
		if (reportDataSource == null)
		{
			throw new Exception("No data source <null> has been provided");
		}
		if(reportDataSource instanceof String)
		{
			reportDataSource = plugin.getIClientPluginAccess().getDatabaseManager().getSwitchedToServerName((String)reportDataSource);
		}
		Debug.trace("JasperTrace: JasperReport initialize");

		IJasperReportsService jasperReportService = plugin.connectJasperService();

		// decide the appropriate report runner (server/remote or client) 
		IJasperReportRunner jasperReportRunner = getReportRunner(jasperReportService, inputType, reportDataSource);

		// in case the server is not started in developer
		if (jasperReportService != null)
		{
			// needed before filling
			jasperReportsLocalService.set(jasperReportService);
			jasperReportsLocalClientID.set(this.getPluginClientID());

			ClassLoader savedCl = Thread.currentThread().getContextClassLoader();
			try
			{
				Thread.currentThread().setContextClassLoader(plugin.getIClientPluginAccess().getPluginManager().getClassLoader());
				Debug.trace("JasperTrace: JasperReport service found");

				// handle i18n
				int applicationType = plugin.getIClientPluginAccess().getApplicationType();
				JasperReportsI18NHandler.appendI18N(params, applicationType == IClientPluginAccess.WEB_CLIENT, plugin.getIClientPluginAccess(), localeString);

				// subreport running own factory setting
				DefaultJasperReportsContext.getInstance().setProperty("net.sf.jasperreports.subreport.runner.factory", "com.servoy.plugins.jasperreports.ServoyThreadSubreportRunnerFactory");

				// Fill the report and get the JasperPrint instance; also modify the JasperPrint in case you want to move the table of contents.
				// we only have a transaction id for the reports run on the server
				String txid = (reportDataSource instanceof String && jasperReportRunner instanceof IJasperReportsService) ? plugin.getIClientPluginAccess().getDatabaseManager().getTransactionID((String) reportDataSource) : null;
				JasperPrintResult jpResults = jasperReportRunner.getJasperPrint(plugin.getIClientPluginAccess().getClientID(), inputType, reportDataSource, inputOptions, txid, reportName, params, relativeReportsDir, relativeExtraDirs);
				JasperPrint jp = jpResults.getJasperPrint();

				try {
					if (moveTableOfContent)
					{
						int iP = getInsertPage(jp);
						jp = moveTableOfContents(jp, iP);
					}

					byte[] resultExportedJasperReport = null;
					if (returnJustJasperPrint)
					{
						// this is for the JasperViewerServoyBean
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						JRSaver.saveObject(jp, baos);
						resultExportedJasperReport = baos.toByteArray();
					}
					else 
					{
						// all other export formats come here
						resultExportedJasperReport = getExportedJasperReport(jasperReportService, jp, outputType, outputOptions, params, localeString, applicationType);
					}

					return resultExportedJasperReport;
					
				} finally {
					
					if (jpResults.getGarbageMan() != null) {
						jasperReportRunner.cleanupJasperPrint(jpResults.getGarbageMan());
					}
				}
				
			}
			catch (Exception e)
			{
				Debug.error(e);
				throw new Exception(e);
			}
			finally
			{
				Thread.currentThread().setContextClassLoader(savedCl);
				// cleanup
				jasperReportsLocalService.set(null);
				jasperReportsLocalClientID.set(null);
			}
		}
		Debug.error("JasperTrace: Jasper Exception: No service running");
		throw new Exception("JasperTrace: Jasper Exception: No service running");
	}
	
	/**
	 * This method decides the appropriate report runner (server/remote or client) based on the input type and data source.
	 * If throws an exception if the source does not match the input type or if the data source is unsupported.
	 * 
	 * @param jasperReportService the server service
	 * @param inputType the report input type
	 * @param reportDataSource the report data source
	 * 
	 * @return the report runner (client or server service)
	 * 
	 * @throws Exception
	 */
	private IJasperReportRunner getReportRunner(IJasperReportsService jasperReportService, String inputType, Object reportDataSource) throws Exception {
		IJasperReportRunner jasperReportRunner = null;
		if (INPUT_TYPE.JRD.equalsIgnoreCase(inputType))  {
			if (reportDataSource instanceof JRDataSource) {
				jasperReportRunner = new JasperReportRunner(jasperReportService); // run reports in clients
			} else { 
				throw new Exception("Source does not match inputtype: " + inputType + " / " + reportDataSource.getClass());
			}
		} else if (INPUT_TYPE.DB.equalsIgnoreCase(inputType)) {
			if (reportDataSource instanceof String) {
				jasperReportRunner = jasperReportService; // run report remote (on the server)
			}
			else { 
				throw new Exception("Unsupported data source: " + reportDataSource.getClass());
			}
		} else {
			// InpuType: XML or CSV
			if (inputType != null) { 
				// we have some report type..
				jasperReportRunner = jasperReportService; // run report remote (on the server)
			} else {
				// legacy/default behavior (inputType is null)
				if (reportDataSource instanceof String) {
					jasperReportRunner = jasperReportService; // run report remote
				} else if (reportDataSource instanceof JRDataSource) {
					jasperReportRunner = new JasperReportRunner(jasperReportService); // run reports in client
				} else {
					throw new Exception("Unsupported data source: " + reportDataSource.getClass());
				}
			}
		}
		return jasperReportRunner;
	}
	
	/**
	 * This method should do the "real" exporting, i.e. call the report service/runner to generate/exported the report, 
	 * considering the options and parameters provided.
	 * 
	 * @param jasperReportService the server service
	 * @param rawJasperPrint the raw jasper print
	 * @param outputType the type of export
	 * @param outputOptions the output options (i.e. file name to export to or specific options for the print output type)
	 * @param params the parameters for the report
	 * @param localeString the locale identifying string
	 * @param applicationType the application type (smart/web/headless client)
	 * 
	 * @return the exported jasper report as an array of bytes
	 * 
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private byte[] getExportedJasperReport(IJasperReportsService jasperReportService, JasperPrint rawJasperPrint, String outputType, Object outputOptions, Map<String, Object> params, String localeString, int applicationType) throws Exception 
	{
		Debug.trace("JasperTrace: JasperReport starting");
		
		// the result of the export
		byte[] exportResult = null;
		
		String type = outputType.toLowerCase();
		String fileName = null;
		String printerName = null;
		boolean showPrintDialog = false;
		// "nooutput" is true when (outputOptions == null)
		
		// process the output options
		Map<String,Object> auxArgs = new HashMap<String,Object>(); // FIXME are these args ever used? (the key/values?)
		if (outputOptions instanceof String)
		{
			fileName = outputOptions.toString();
			auxArgs.put("FILENAME", fileName);
		}
		else if (outputOptions instanceof Boolean)
		{
			showPrintDialog = Utils.getAsBoolean(outputOptions);
			auxArgs.put("SHOWPRINTDIALOG", showPrintDialog);
		}
		else	
		{
			if (outputOptions != null) 
			{
				// the map coming in here (new for XML/CSV dataources feature) 
				if (outputOptions instanceof Map) 
				{
					auxArgs = (Map<String,Object>) JSArgumentsUnwrap.unwrapJSObject(outputOptions, plugin.getIClientPluginAccess());
					if (auxArgs.containsKey("PRINTERNAME")) 
					{
						printerName = (String) auxArgs.get("PRINTERNAME");
					}
					if (auxArgs.containsKey("FILENAME"))
					{
						fileName = (String) auxArgs.get("FILENAME");
					}
					if (auxArgs.containsKey("SHOWPRINTDIALOG"))
					{
						showPrintDialog = (Boolean) auxArgs.get("SHOWPRINTDIALOG");
					}
				}
				else
				{
					// To support passing in a JSFile object
					fileName = outputOptions.toString();
				}
			}
		}
		
		// some backwards compatibility
		if (type.startsWith("print") ) 
		{ 
			if ((fileName == null) || fileName.equalsIgnoreCase("true"))
			{
				fileName = null; 
				showPrintDialog = true;
			}
			else if ((!showPrintDialog && fileName.equals("")) || fileName.equalsIgnoreCase("false") || fileName.equalsIgnoreCase("default"))
			{
				fileName = null; 
				showPrintDialog = false;
			}	
			else 
			{ 
				if (printerName == null)
				{
					printerName = fileName;
				}
			}
		}	
		
		// this may occur when calling getExportedJasperReport from mergeReport with no params map
		if (params == null) 
		{
			params = new HashMap<String, Object>();
		}
		
		// add the auxiliary arguments to the parameters map
		if (auxArgs != null && !auxArgs.isEmpty()) 
		{
			params.putAll(auxArgs);
		}
		Map<String, Object> exporterParams = createExporterParametersMap(params);
		// should not occur, but the call above may return null
		if (exporterParams == null) 
		{
			exporterParams = new HashMap<String, Object>();
		}
		if (outputOptions != null && fileName != null)
		{
			// we need the REPORT_FILE_LOCATION parameters for HTML/XHTML based exporting, in all client type scenarios 
			exporterParams.put("REPORT_FILE_LOCATION", new File(fileName).getParent());
		}
		
		// 1. WebClient
		if (isWebClient(applicationType))
		{
			exportResult = handleWebClientExport(jasperReportService, rawJasperPrint, type, outputOptions, fileName, exporterParams);
		}
		// 2. HeadlessClient
		else if (applicationType == IClientPluginAccess.HEADLESS_CLIENT)
		{
			exportResult = handleHeadlessClientExport(jasperReportService, rawJasperPrint, type, outputOptions, fileName, exporterParams);
		}
		// 3. SmartClient (default)
		else
		{
			exportResult = handleSmartClientExport(jasperReportService, rawJasperPrint, type, outputOptions, fileName, showPrintDialog, localeString, exporterParams);
		}
		Debug.trace("JasperTrace: JasperReport finished");
		
		return exportResult;
	}
	
	/**
	 * This method handles the web client specific export and returns the exported report as a byte array.
	 * 
	 * @param jasperReportService the report service (client or server)
	 * @param rawJasperPrint the raw (filled) jasper print to be exported
	 * @param outputType the export type
	 * @param outputOptions the output options (i.e. file name to export to or specific options for the print output type)
	 * @param fileName the name of the file to export to or the printer name
	 * @param exporterParams the parameters for the exporter
	 * 
	 * @return the exported report as a byte array
	 * 
	 * @throws Exception
	 */
	private byte[] handleWebClientExport(IJasperReportsService jasperReportService, JasperPrint rawJasperPrint, String outputType, Object outputOptions, String fileName, Map<String, Object> exporterParams) throws Exception 
	{
		byte[] exportResult = null;
		String mimeType = getWebClientMimeTypeUsingExportType(outputType);
		if (outputType.equals(OUTPUT_FORMAT.VIEW) || outputType.equals(OUTPUT_FORMAT.PRINT))
		{
			exportResult = JasperReportRunner.getJasperBytes("pdf", rawJasperPrint, jasperReportService.getCheckedExtraDirectoriesRelativePath(relativeExtraDirs), exporterParams);
			JasperReportsWebViewer.show(plugin.getIClientPluginAccess(), exportResult, fileName, "pdf", mimeType);
		}
		else
		{
			exportResult = JasperReportRunner.getJasperBytes(outputType, rawJasperPrint, jasperReportService.getCheckedExtraDirectoriesRelativePath(relativeExtraDirs), exporterParams);
			if (outputOptions == null || (outputOptions instanceof Map && ((Map)outputOptions).containsKey("DOWNLOAD")))
			{
				JasperReportsWebViewer.show(plugin.getIClientPluginAccess(), exportResult, fileName, outputType, mimeType);
			}
			else
			{
				saveByteArrayToFile(fileName, exportResult);
			}
		}	
		return exportResult;
	}
	
	/**
	 * This method handles the headless client specific export and returns the exported report as a byte array.
	 * 
	 * @param jasperReportService the report service (client or server)
	 * @param rawJasperPrint the raw (filled) jasper print to be exported
	 * @param outputType the export type
	 * @param outputOptions the output options (i.e. file name to export to or specific options for the print output type)
	 * @param fileName the name of the file to export to or the printer name
	 * @param exporterParams the parameters for the exporter
	 * 
	 * @return the exported report as a byte array
	 * 
	 * @throws Exception
	 */
	private byte[] handleHeadlessClientExport(IJasperReportsService jasperReportService, JasperPrint rawJasperPrint, String outputType, Object outputOptions, String fileName, Map<String, Object> exporterParams) throws Exception
	{
		byte[] exportResult = null;

		// for view and print we default to pdf - printing to file
		if (outputOptions == null) throw new Exception("JasperTrace: Jasper Exception: Please specify an output file when calling jasper from a headless client.");
		
		if (outputType.equals(OUTPUT_FORMAT.VIEW) || outputType.equals(OUTPUT_FORMAT.PRINT)) outputType = "pdf"; 
		
		exportResult = JasperReportRunner.getJasperBytes(outputType, rawJasperPrint, jasperReportService.getCheckedExtraDirectoriesRelativePath(relativeExtraDirs), exporterParams);
		saveByteArrayToFile(fileName, exportResult);
		
		return exportResult;
	}
	
	/**
	 * This method handles the smart client specific export. It also returns the exported report as a byte array.
	 * 
	 * @param jasperReportService the report service (client or server)
	 * @param rawJasperPrint the raw (filled) jasper print to be exported
	 * @param outputType the export type
	 * @param outputOptions the output options (i.e. file name to export to or specific options for the print output type)
	 * @param fileName the name of the file to export to or the printer name
	 * @param showPrintDialog true if the system print dialog is shown; false otherwise 
	 * @param localeString the locale identifying string
	 * @param exporterParams the parameters for the exporter
	 * 
	 * @return the exported report as a byte array
	 * 
	 * @throws Exception
	 */
	private byte[] handleSmartClientExport(IJasperReportsService jasperReportService, JasperPrint rawJasperPrint, String outputType, Object outputOptions, String fileName, boolean showPrintDialog, String localeString, Map<String, Object> exporterParams) throws Exception
	{
		byte[] exportResult = null;
		
		// a. SmartClient "view"
		if (outputType.startsWith(OUTPUT_FORMAT.VIEW))
		{
			CustomizedJasperViewer jasperviewer;
			if (localeString != null)
			{
				jasperviewer = new CustomizedJasperViewer(rawJasperPrint, false, new Locale(localeString));
			}
			else
			{
				jasperviewer = new CustomizedJasperViewer(rawJasperPrint, false);
			}

			// add our own preferences to the customized jasper viewer
			if (viewerExportFormats != null) setViewerSaveContributors(jasperviewer.getJRViewer(), viewerExportFormats);
			if (viewerTitle != null) jasperviewer.setTitle(viewerTitle);
			if (viewerIconURL != null)
			{
				URL mediaURL = null;
				try
				{
					mediaURL = new URL(viewerIconURL);
				}
				catch (MalformedURLException ex)
				{
					// fallback to media:///
					try
					{
						mediaURL = new URL("media:///" + viewerIconURL);
					}
					catch (MalformedURLException ex1)
					{
						Debug.error(ex1);
					}
				}
				if (mediaURL != null) jasperviewer.setIconImage(new ImageIcon(mediaURL).getImage());
			}

			if (rawJasperPrint != null && rawJasperPrint.getPages() != null && rawJasperPrint.getPages().size() > 0)
			{
				jasperviewer.setVisible(true);
				jasperviewer.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
			}
			else
			{
				jasperviewer.dispose();
			}
		}
		// b. SmartClient "print". Printing only supported in SC. Printing in WC handled previously though PDF push.
		else if (outputType.startsWith(OUTPUT_FORMAT.PRINT))
		{
			// Shows print dialog before printing (if arg is true/"true")
			if (showPrintDialog || fileName.equalsIgnoreCase("true") || fileName == null)
			{
				JasperPrintManager.printReport(rawJasperPrint, true);
			}
			// Skips print dialog (if arg is false/"false" or null)
			else if ((!showPrintDialog && fileName.equals("")) // also equivalent to arg == null
					|| fileName.equalsIgnoreCase("false") || fileName.equalsIgnoreCase("default"))
			{
				JasperPrintManager.printReport(rawJasperPrint, false);
			}
			// Assumes parameter file contains a printerName
			else
			{
				Debug.trace("JasperTrace: printer: " + fileName);
				PrinterJob printJob = PrinterJob.getPrinterJob();
				// fix for bug ID 6255588 from Sun bug database
				initPrinterJobFields(printJob);
				/*
				 * or another workaround try { printerJob.setPrintService (printerJob.getPrintService()); } catch (PrinterException e) {}
				 */

				if (setPrintService(printJob, fileName))
				{
					JasperReportsPrinter.printPages(rawJasperPrint, printJob);
				}
				else
				{
					Debug.trace("JasperTrace: unable to specify printer: " + fileName);
				}
			}
		}
		// c. SmartClient other output formats
		else
		{
			// check if we must force the export on the client
			if (forceClientSideExporting(outputType)) 
			{
				// in some cases we must render on the client, so that resource paths are correctly created (i.e. html/xhtml extra folders..)
				exportResult = JasperReportRunner.getJasperBytes(outputType, rawJasperPrint ,jasperReportService.getCheckedExtraDirectoriesRelativePath(relativeExtraDirs), exporterParams);
			} 
			else
			{
				// the following will be executed on the server (!)
				exportResult = jasperReportService.getJasperBytes(plugin.getIClientPluginAccess().getClientID(), outputType, rawJasperPrint, relativeExtraDirs, exporterParams);
			}
			
			if (outputOptions != null)
			{
				saveByteArrayToFile(fileName, exportResult);
			}
		}
		
		return exportResult;
	}
	
	/**
	 * Helper method to get the webclient mime type for the export.
	 * 
	 * @param exportType the report indicated export type
	 * 
	 * @return the corresponding web mime type
	 * 
	 * @throws Exception
	 */
	private String getWebClientMimeTypeUsingExportType(String exportType) throws Exception
	{
		String mimeType = null; //"application/octet-stream";
		// TODO refactor to use a switch (java 7 and above..) or use a map perhaps
		if (exportType.equals(OUTPUT_FORMAT.VIEW) || exportType.equals(OUTPUT_FORMAT.PRINT))
		{
			mimeType = JasperReportsWebViewer.MIME_TYPE_PDF;
		}
		else if (exportType.equals(OUTPUT_FORMAT.PDF))
		{
			mimeType = JasperReportsWebViewer.MIME_TYPE_PDF;
		}
		else if (exportType.equals(OUTPUT_FORMAT.CSV) || exportType.equals(OUTPUT_FORMAT.CSV_METADATA))
		{
			mimeType = JasperReportsWebViewer.MIME_TYPE_CSV;
		}
		else if (exportType.equals(OUTPUT_FORMAT.DOCX))
		{
			mimeType = JasperReportsWebViewer.MIME_TYPE_DOCX;
		} 
		else if (exportType.equals(OUTPUT_FORMAT.XLSX))
		{
			mimeType = JasperReportsWebViewer.MIME_TYPE_XLSX;
		}
		else if (exportType.equals(OUTPUT_FORMAT.EXCEL))
		{
			mimeType = JasperReportsWebViewer.MIME_TYPE_XLS;
		}
		else if (exportType.equals(OUTPUT_FORMAT.HTML))
		{
			mimeType = JasperReportsWebViewer.MIME_TYPE_HTML;
		}
		else if (exportType.equals(OUTPUT_FORMAT.ODS))
		{
			mimeType = JasperReportsWebViewer.MIME_TYPE_ODS;
		}
		else if (exportType.equals(OUTPUT_FORMAT.ODT))
		{
			mimeType = JasperReportsWebViewer.MIME_TYPE_ODT;
		}
		else if (exportType.equals(OUTPUT_FORMAT.RTF))
		{
			mimeType = JasperReportsWebViewer.MIME_TYPE_RTF;
		}
		else if (exportType.equals(OUTPUT_FORMAT.TXT))
		{
			mimeType = JasperReportsWebViewer.MIME_TYPE_TXT;
		}
		else if (exportType.equals(OUTPUT_FORMAT.XHTML))
		{
			mimeType = JasperReportsWebViewer.MIME_TYPE_XHTML;
		}
		else if (exportType.equals(OUTPUT_FORMAT.XLS))
		{
			mimeType = JasperReportsWebViewer.MIME_TYPE_XLS;
		}
		else if (exportType.equals(OUTPUT_FORMAT.XLS_1_SHEET))
		{
			mimeType = JasperReportsWebViewer.MIME_TYPE_XLS;
		}
		else if (exportType.equals(OUTPUT_FORMAT.XML))
		{
			mimeType = JasperReportsWebViewer.MIME_TYPE_XML;
		}
		else if (exportType.equals(OUTPUT_FORMAT.JSON_METADATA))
		{
			mimeType = JasperReportsWebViewer.MIME_TYPE_JSON;
		}
		else
		{
			throw new Exception("JasperTrace: Jasper Exception: Unsupported web client output format: " + exportType);
		}
		return mimeType;
	}
	
	/**
	 * Function to merge two or more reports into a single report file.
	 * The reports to merge must have been previously exported as JRPRINTs.
	 * 
	 * @sample
	 * // Merge two report files into a single output report file. Note that the list of reports to merge must jasper print exported reports.
	 * // var $jp1 = plugins.jasperPluginRMI.runReport(plugins.jasperPluginRMI.INPUT_TYPE.XML, $xmlDataCombined, '/node/to/iterate', 'report1.jrxml', null, plugins.jasperPluginRMI.OUTPUT_FORMAT.JRPRINT, $parameters, $locale);
	 * // var $jp2 = plugins.jasperPluginRMI.runReport(plugins.jasperPluginRMI.INPUT_TYPE.XML, $xmlDataCombined, '/node/to/iterate', 'report2.jrxml', null, plugins.jasperPluginRMI.OUTPUT_FORMAT.JRPRINT, $parameters, $locale);
	 * // var $list = new java.util.ArrayList();
	 * // $list.add($jp1);
	 * // $list.add($jp2);
	 * // var $jasper_result = plugins.jasperPluginRMI.mergeJasperPrint($list, 'landscape', plugins.jasperPluginRMI.OUTPUT_FORMAT.VIEW, null, $locale);
	 * 
	 * @param printList the list of reports to merge; the objects in the list must be jasperPrint objects (or reports exported as jasper print)
	 * @param orientation the orientation of the result report
	 * @param outputType the output type of the result report
	 * @param outputOptions the output options of the report, as provided for the runReport function
	 * @param localeString localeString the string which specifies the locale
	 * 
	 * @return the result report as an Object
	 * 
	 * @throws Exception
	 */
	public Object js_mergeJasperPrint(ArrayList<Object> printList, String orientation, String outputType, Object outputOptions, String localeString) throws Exception 
	{
		Object result = null;
		
		try {
			if ((printList != null) && (printList.size() > 0)) {

				JasperPrint mergedJRPrint = new JasperPrint();

				mergedJRPrint.setName("printedDocs");

				// NOTE: fixed size A4 here for merging, as no other formats used currently!!!
				int pageHeight = 842;
				int pageWidth = 595;
				OrientationEnum pageOrientation = OrientationEnum.PORTRAIT;

				// here is the point where orientation is decided
				if (OrientationEnum.LANDSCAPE.getName().equalsIgnoreCase(orientation)) {
					pageHeight = 595;
					pageWidth = 842;
					pageOrientation = OrientationEnum.LANDSCAPE;					
				}

				for (int i = 0; i < printList.size(); i++) 
				{
					Object aux = printList.get(i);
					JasperPrint jrPrint = null;
					// the reports to merge must have been exported as JRPRINTs
					if (aux instanceof byte[]) {
						ByteArrayInputStream in = new ByteArrayInputStream((byte[]) aux);
					    ObjectInputStream is = new ObjectInputStream(in);
						jrPrint = (JasperPrint) is.readObject();
					} else if (aux instanceof JasperPrint) {
						jrPrint = (JasperPrint)aux;
					}

					// try to adjust to the current Page-Size 
					// (acostache: try to fit the size) 
					if (pageHeight < jrPrint.getPageHeight()) {
						pageHeight = jrPrint.getPageHeight();
					}
					if (pageWidth < jrPrint.getPageWidth()) {
						pageWidth = jrPrint.getPageWidth();
					}

					Map<String, JRStyle> mergeStyles = mergedJRPrint.getStylesMap();
					Map<String, JRStyle> aktStyles = jrPrint.getStylesMap();

					Iterator<JRStyle> styleIt = aktStyles.values().iterator();
					while (styleIt.hasNext()) {
						JRStyle aktStyle = styleIt.next();
						if (!mergeStyles.keySet().contains(aktStyle.getName())) {
							mergedJRPrint.addStyle(aktStyle);
						}
					}

					java.util.List<JRPrintPage> pageList = jrPrint.getPages();
					for (int j = 0; j < pageList.size(); j++) {
						mergedJRPrint.addPage(pageList.get(j));
					}

				}

				mergedJRPrint.setPageHeight(pageHeight);
				mergedJRPrint.setPageWidth(pageWidth);
				mergedJRPrint.setOrientation(pageOrientation);
				
				// we need to explicitly add a MAXIMUM_ROWS_PER_SHEET for spreadsheet exports
				if (OUTPUT_FORMAT.EXCEL.equalsIgnoreCase(outputType) || OUTPUT_FORMAT.XLS.equalsIgnoreCase(outputType) 
						|| OUTPUT_FORMAT.XLS_1_SHEET.equalsIgnoreCase(outputType) || OUTPUT_FORMAT.XLSX.equalsIgnoreCase(outputType)) {
					// adding the maximum value, as there no user setting for it currently for the merged report
					mergedJRPrint.setProperty("MAXIMUM_ROWS_PER_SHEET", String.valueOf(65535));
				}

				result = getExportedJasperReport(plugin.connectJasperService(), mergedJRPrint, outputType, outputOptions, null, localeString, plugin.getIClientPluginAccess().getApplicationType());

			} else {
				Debug.error("List is empty");
				throw new Exception("List is empty");
			}
		}

		catch (Exception ex) {
			Debug.error(ex);
			throw new Exception(ex.getMessage());
		}
		
		return result;

	}
	
	/**
	 * In some situations, as html/xhtml exporting with additional resources, more files/folders need to be created
	 * on the client. This is needed for storing references or the additional resources themselves on the client,
	 * in order for the proper displaying of the html report. 
	 * Do note, that normally, exporting is done server-side. (this problem is not present for a Developer debug smart client)
	 * @param type the export type for which we need to defer rendering to the client
	 * @return true if exporting is to be done on the client, else will done on the server (or same client for the Developer) 
	 */
	private boolean forceClientSideExporting(String type) 
	{
		return type.equalsIgnoreCase(OUTPUT_FORMAT.HTML) || type.equalsIgnoreCase(OUTPUT_FORMAT.XHTML);
	}

	public void saveByteArrayToFile(String filename, byte[] buffertje) throws Exception
	{
		if (filename != null && filename.trim().length() > 0)
		{
			FileOutputStream fos = new FileOutputStream(filename);
			fos.write(buffertje);
			fos.flush();
			fos.close();
		}
	}

	/**
	 * Compile a Jasper Reports .jrxml file to a .jasper file.
	 * 
	 * @sample
	 * // Compile the .jrxml jasper report file to a .jasper file. The name of the compiled file is given by the report name 
	 * // The report name as an absolute path. Results the compiled c:\\temp\\samplereport.jasper file. 
	 * var success = plugins.jasperPluginRMI.compileReport('c:\\\\temp\\\\samplereport.jrxml'); 
	 * // The report name as a relative path. The file will be searched relative to the ReportDirectory. 
	 * var success = plugins.jasperPluginRMI.compileReport('myCustomerReport1.jrxml'); 
	 * var success = plugins.jasperPluginRMI.compileReport( '\\\\subdir\\\\myCustomerReport2.jrxml'); 
	 * // To specify a different destination file than the original filaname, the second parameter can be incouded. 
	 * // If it is relative, the file will be created relative to the ReportDirectory. 
	 * var success = plugins.jasperPluginRMI.compileReport('c:\\\\temp\\\\samplereport.jrxml', 'd:\\\\temp2\\\\destreport.jasper');
	 * 
	 * @param report the .jrxml jasper report file
	 * 
	 * @return the compiled jasper report file
	 */
	public boolean js_compileReport(String report) throws Error, Exception
	{
		return js_compileReport(report, null);
	}

	/**
	 * @clonedesc js_compileReport(String)
	 * @sampleas js_compileReport(String)
	 * 
	 * @param report the .jrxml jasper report file
	 * @param destination the destination file for the compiled report
	 * 
	 * @return the compiled jasper report file
	 */
	public boolean js_compileReport(String report, String destination) throws Error, Exception
	{

		Debug.trace("JasperTrace: JasperCompile initialize");
		boolean compiled = false;

		ClassLoader savedCl = Thread.currentThread().getContextClassLoader();
		try
		{
			Thread.currentThread().setContextClassLoader(plugin.getIClientPluginAccess().getPluginManager().getClassLoader());
			Debug.trace("JasperTrace: JasperCompile starting");
			compiled = plugin.connectJasperService().jasperCompile(plugin.getIClientPluginAccess().getClientID(), report, destination, relativeReportsDir);
			Debug.trace("JasperTrace: JasperCompile finished");
		}
		catch (Error err)
		{
			Debug.error(err);
			throw new Exception(err.getMessage());
		}
		catch (Exception ex)
		{
			Debug.error(ex);
			throw new Exception(ex.getMessage());
		}
		finally
		{
			Thread.currentThread().setContextClassLoader(savedCl);
		}

		return compiled;
	}

	/**
	 * @param report the .jrxml jasper report file
	 * 
	 * @deprecated replaced by compileReport(String)
	 */
	@Deprecated
	public boolean js_jasperCompile(String report) throws Error, Exception
	{
		return js_compileReport(report);
	}

	/**
	 * @param report the .jrxml jasper report file
	 * @param forceRecompile true in order to recompile (is no longer supported)
	 * 
	 * @deprecated replaced by compileReport(String)
	 */
	@Deprecated
	public boolean js_jasperCompile(String report, boolean forceRecompile) throws Error, Exception
	{
		return js_compileReport(report);
	}

	/**
	 * @param fileName the name and or relative path to the file to write to
	 * @param obj the object file to write
	 * 
	 * @deprecated replaced by writeFileToReportsDir(String, Object)
	 */
	@Deprecated
	public boolean js_writeFile(String fileName, Object obj) throws Exception
	{
		return js_writeFileToReportsDir(fileName, obj);
	}

	/**
	 * Store a reportFile on the Server.
	 * 
	 * @sample 
	 * // .jasper or .jrxml files can be used var 
	 * file = plugins.file.readFile('\\\\temp\\\\sample.jasper');
	 * plugins.jasperPluginRMI.writeFileToReportsDir('myCustomerReport.jasper', file); 
	 * // Writes to a subfolder from the reports directory. All the folders from the path must exist. 
	 * plugins.jasperPluginRMI.writeFileToReportsDir('\\\\subdir\\\\myCustomerReport.jasper', file);
	 * 
	 * @param fileName the name of the file to write to
	 * @param obj the object file to write
	 * 
	 * @return true if the write was successful, false otherwise
	 */
	public boolean js_writeFileToReportsDir(String fileName, Object obj) throws Exception
	{

		try
		{
			Debug.trace("JasperTrace: JasperWriteFile starting");
			boolean b = plugin.connectJasperService().writeFile(plugin.getIClientPluginAccess().getClientID(), fileName, obj, relativeReportsDir);
			Debug.trace("JasperTrace: JasperWriteFile finished");
			return b;
		}
		catch (Exception e)
		{
			Debug.error(e);
			return false;
		}
	}

	/**
	 * Delete a report file from the Server.
	 * 
	 * @sample 
	 * var reportFile2Delete = 'myCustomerReport.jrxml'; 
	 * plugins.jasperPluginRMI.deleteFileFromReportsDir(reportFile2Delete);
	 * 
	 * @param fileName the name of the report file to delete
	 * 
	 * @return true if the file was successfully deleted, false otherwise
	 */
	public boolean js_deleteFileFromReportsDir(String fileName) throws Exception
	{

		try
		{
			Debug.trace("JasperTrace: JasperDeleteFileFromReportsDir starting");
			boolean b = plugin.connectJasperService().deleteFile(plugin.getIClientPluginAccess().getClientID(), fileName, relativeReportsDir);
			Debug.trace("JasperTrace: JasperDeleteFileFromReportsDir finished");
			return b;
		}
		catch (Exception e)
		{
			Debug.error(e);
			return false;
		}
	}

	/**
	 * @param fileName the name of the file to read from the reports directory
	 * 
	 * @deprecated replaced by readFileFromReportsDir(String)
	 */
	@Deprecated
	public byte[] js_readFile(String fileName) throws Exception
	{
		return js_readFileFromReportsDir(fileName);
	}

	/**
	 * Retrieve a report file from the Server.
	 * 
	 * @sample 
	 * var reportFileArray = plugins.jasperPluginRMI.readFileFromReportsDir('myCustomerReport.jasper'); 
	 * // Subfolders can be used to read files. 
	 * var reportFileArray = plugins.jasperPluginRMI.readFileFromReportsDir('\\\\subdir\\\\myCustomerReport.jasper');
	 * 
	 * @param fileName the name of the file to read from the reports directory
	 * 
	 * @return the report file retrieved as a byte array
	 */
	public byte[] js_readFileFromReportsDir(String fileName) throws Exception
	{

		byte[] b = null;

		try
		{
			Debug.trace("JasperTrace: JasperReadFile starting");
			b = plugin.connectJasperService().readFile(plugin.getIClientPluginAccess().getClientID(), fileName, relativeReportsDir);
			Debug.trace("JasperTrace: JasperReadFile finished");
		}
		catch (Exception e)
		{
			Debug.error(e);
			throw new Exception(e.getMessage());
		}
		return b;
	}

	/**
	 * Property for retrieving the reports directory from the server.
	 * 
	 * NOTE: Setting the absolute path for the report directory on the server is no longer supported.
	 * 
	 * @deprecated replaced by the relativeReportsDirectory property
	 */
	@Deprecated
	public String js_getReportDirectory() throws Exception
	{
		return plugin.getJasperReportsDirectory();
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public void js_setReportDirectory(String jasperDirectorie) throws Exception
	{
		js_setRelativeReportsDirectory(jasperDirectorie);
	}

	/**
	 * Property for retrieving and setting the path to the reports directory, set by the current client, relative to the
	 * server reports directory.
	 * 
	 * By default the value is read from the admin page Server Plugins, the directory.jasper.report property. If no value
	 * has been set in the admin page for the report directory, the default location will be set to /path_to_install/application_server/server/reports.
	 * A client is only able to set a path relative to the server report directory. If the client modifies this property, its value
	 * will be used instead of the default one, for the whole client session and only for this client. Each client
	 * session has it's own relativeReportDirectory value.
	 * 
	 * @sample 
	 * plugins.jasperPluginRMI.relativeReportsDirectory = "/myReportsLocation";
	 * 
	 * @return the location of the reports directory, relative to the server set path
	 */
	public String js_getRelativeReportsDirectory() throws Exception
	{
		return relativeReportsDir;
	}

	public void js_setRelativeReportsDirectory(String relativeReportDirectory) throws Exception
	{
		String checkedPath = plugin.connectJasperService().getCheckedRelativeReportsPath(relativeReportDirectory);
		relativeReportsDir = relativeReportDirectory;
		plugin.setJasperReportsDirectory(checkedPath);
	}

	/**
	 * @deprecated replaced by the relativeExtraDirectories property
	 */
	@Deprecated
	public String js_getExtraDirectories() throws Exception
	{
		return plugin.getJasperExtraDirectories();
	}

	/**
	 * Property for retrieving and setting the paths to the extra resources directories. The paths are set per client
	 * and are relative to the server corresponding directories setting.
	 * 
	 * By default the value is read from the Admin Page: Server Plugins - the directories.jasper.extra property. If the
	 * client modifies the default property, this value will be used instead of the default one for the whole client
	 * session and only for this client. Each client session has it's own extraDirectories value.
	 * 
	 * NOTE: Extra directories are not searched recursively.
	 * 
	 * @sample 
	 * //setting the extra directories, relative to the server side location
	 * plugins.jasperPluginRMI.relativeExtraDirectories = "extraDir1,extraDir2";
	 * 
	 * @return the path to the extra directories relative to the server side set location
	 */
	public String js_getRelativeExtraDirectories() throws Exception
	{
		return relativeExtraDirs;
	}

	/**
	 * @deprecated
	 */
	@Deprecated
	public void js_setExtraDirectories(String extraDirectories) throws Exception
	{
		js_setRelativeExtraDirectories(extraDirectories);
	}

	public void js_setRelativeExtraDirectories(String extraDirectories) throws Exception
	{
		// plugin.setJasperExtraDirectories(extraDirectories);
		String extraDirsRelPath = plugin.connectJasperService().getCheckedExtraDirectoriesRelativePath(extraDirectories);
		relativeExtraDirs = extraDirectories;
		plugin.setJasperExtraDirectories(extraDirsRelPath);
	}

	/**
	 * Retrieve a String array of available reports, based on the reports directory.
	 * 
	 * @sample 
	 * // COMPILED - only compiled reports, NONCOMPILED - only non-compiled reports 
	 * // No parameter returns all the reports 
	 * var result = plugins.jasperPluginRMI.getReports('NONCOMPILED'); 
	 * application.output(result[0]); 
	 * //using a string as the search filter 
	 * //var result = plugins.jasperPluginRMI.getReports('*criteria*'); 
	 * //for(var i=0; i<result.length; i++) 
	 * //application.output(result[i]);
	 * 
	 * @return the String array of available reports
	 */
	public String[] js_getReports() throws Exception
	{
		return getReports(true, true);
	}

	/**
	 * @clonedesc js_getReports()
	 * @sampleas js_getReports()
	 * 
	 * @param filter the string to be used as a search filter
	 * 
	 * @return the String array of available reports
	 */
	public String[] js_getReports(String filter) throws Exception
	{
		if (filter.toUpperCase().compareTo("COMPILED") == 0)
		{
			return getReports(true, false);
		}
		else if (filter.toUpperCase().compareTo("NONCOMPILED") == 0)
		{
			return getReports(false, true);
		}
		else return getReports(filter);
	}

	/**
	 * Returns an array of available reports, based on the indicated filtering criteria.
	 * @param filter a specific filter to base the results of the search on (COMPILED, NONCOMPILED or a naming criteria)
	 * @return an array of available reports, based on the indicated filtering criteria 
	 * @throws Exception
	 */
	private String[] getReports(String filter) throws Exception
	{
		String[] reports = null;
		try
		{
			Debug.trace("JasperTrace: getReports starting");
			reports = plugin.connectJasperService().getReports(plugin.getIClientPluginAccess().getClientID(), filter);
			Debug.trace("JasperTrace: getReports finished");
		}
		catch (Exception e)
		{
			Debug.error(e);
			throw new Exception(e.getMessage());
		}
		return reports;
	}

	/**
	 * The internal method which gets the reports (compiled and/or not) from the jasper service.
	 * @param compiled true for getting compiled reports
	 * @param uncompiled true for getting uncompiled reports
	 * @return a list of report names
	 * @throws Exception
	 */
	private String[] getReports(boolean compiled, boolean uncompiled) throws Exception
	{
		String[] reports = null;
		try
		{
			Debug.trace("JasperTrace: getReports starting");
			reports = plugin.connectJasperService().getReports(plugin.getIClientPluginAccess().getClientID(), compiled, uncompiled);
			Debug.trace("JasperTrace: getReports finished");
		}
		catch (Exception e)
		{
			Debug.error(e);
			throw new Exception(e.getMessage());
		}
		return reports;
	}

	/**
	 * Retrieve a JSDataSet with the report parameters, except the system defined ones.
	 * 
	 * @sample 
	 * var ds = plugins.jasperPluginRMI.getReportParameters('sample.jrxml'); 
	 * var csv = ds.getAsText(',','\\n','\"',true); 
	 * application.output(csv);
	 * 
	 * @param report the name of the report file to get the parameters for
	 * 
	 * @return the JSDataSet with the report parameters
	 */
	public IDataSet js_getReportParameters(String report) throws Exception
	{
		try
		{
			return plugin.connectJasperService().getReportParameters(plugin.getIClientPluginAccess().getClientID(), report, relativeReportsDir);
		}
		catch (Exception e)
		{
			Debug.error(e);
			throw new Exception(e.getMessage());
		}
	}

	/**
	 * Get the version of the Servoy JasperReports Plugin.
	 * 
	 * @sample 
	 * application.output(plugins.jasperPluginRMI.pluginVersion);
	 * 
	 * @return the plugin's version
	 */
	public String js_getPluginVersion()
	{
		return "7.0.1";

		/*
		 * Added destination optional parameter for compileReport method Renamed jasperReport -> runReport, jasperCompile -> compileReport, readFile ->
		 * readFileFromReportsDir, writeFile -> writeFileToReportsDir methods and deprecated the old ones. Updated methods comments to reflect changes and
		 * functionality. Changed jasperReport to accept null output for using only the array return value.
		 */
	}

	/**
	 * @param ver
	 */
	public void js_setPluginVersion(String ver)
	{
		// DO NOTHING. READ ONLY PROPERTY.
	}

	/*
	 * Feature request from <a href= "http://code.google.com/p/servoy-jasperreports-plugin/issues/detail?id=35" >Google issue 35</a>.
	 */
	public void js_setViewerExportFormats(String[] saveContribs) throws Exception
	{
		viewerExportFormats = saveContribs;
	}

	/**
	 * @param viewerTitle the viewer's title text
	 */
	public void js_setViewerTitle(String viewerTitle)
	{
		this.viewerTitle = viewerTitle;
	}

	/**
	 * Sets or gets the Jasper Viewer's title text.
	 * 
	 * @sample 
	 * plugins.jasperPluginRMI.viewerTitle = 'My Title'
	 * 
	 */
	public String js_getViewerTitle()
	{
		return viewerTitle;
	}

	/**
	 * @param viewerIconURL icon URL
	 */
	public void js_setViewerIconURL(String viewerIconURL)
	{
		this.viewerIconURL = viewerIconURL;
	}

	/**
	 * Sets or gets the Jasper Viewer's icon URL.
	 * 
	 * @sample 
	 * plugins.jasperPluginRMI.viewerIconURL = 'myIcon.jpg'
	 * 
	 */
	public String js_getViewerIconURL()
	{
		return viewerIconURL;
	}

	/**
	 * Sets the save contributors for the JasperViewer's JRViewer instance.
	 * 
	 * @param jrv the JRViewer for which we set the save contributors
	 * @param saveContributors the save contributors to be set; the first one will be the (default) first one in the
	 * "Save as type" list of the "Save" dialog.
	 */
	public static void setViewerSaveContributors(javax.swing.JPanel jrv, String[] saveContributors)
	{

		List<String> defContribs = new ArrayList<String>();
		for (String s : saveContributors)
		{
			if (OUTPUT_FORMAT.PDF.equals(s)) defContribs.add("net.sf.jasperreports.view.save.JRPdfSaveContributor");
			else if (OUTPUT_FORMAT.JRPRINT.equals(s)) defContribs.add("net.sf.jasperreports.view.save.JRPrintSaveContributor");
			else if (OUTPUT_FORMAT.ODT.equals(s)) defContribs.add("net.sf.jasperreports.view.save.JROdtSaveContributor");
			else if (OUTPUT_FORMAT.RTF.equals(s)) defContribs.add("net.sf.jasperreports.view.save.JRRtfSaveContributor");
			else if (OUTPUT_FORMAT.HTML.equals(s)) defContribs.add("net.sf.jasperreports.view.save.JRHtmlSaveContributor");
			else if (OUTPUT_FORMAT.XLS_1_SHEET.equals(s)) defContribs.add("net.sf.jasperreports.view.save.JRSingleSheetXlsSaveContributor");
			else if (OUTPUT_FORMAT.XLS.equals(s)) defContribs.add("net.sf.jasperreports.view.save.JRMultipleSheetsXlsSaveContributor");
			else if (OUTPUT_FORMAT.CSV.equals(s)) defContribs.add("net.sf.jasperreports.view.save.JRCsvSaveContributor");
			else if (OUTPUT_FORMAT.XML.equals(s)) defContribs.add("net.sf.jasperreports.view.save.JRXmlSaveContributor");
			else if (OUTPUT_FORMAT.DOCX.equals(s)) defContribs.add("net.sf.jasperreports.view.save.JRDocxSaveContributor");
			else if ("xml_embd_img".equals(s)) defContribs.add("net.sf.jasperreports.view.save.JREmbeddedImagesXmlSaveContributor");
		}

		// the default save contributors
		String[] DEFAULT_CONTRIBUTORS = new String[defContribs.size()];
		DEFAULT_CONTRIBUTORS = defContribs.toArray(DEFAULT_CONTRIBUTORS);

		JRSaveContributor[] jrSaveContribs = new JRSaveContributor[DEFAULT_CONTRIBUTORS.length];
		for (int i = 0; i < DEFAULT_CONTRIBUTORS.length; i++)
		{
			try
			{
				Class<? extends JRSaveContributor> saveContribClass = (Class<? extends JRSaveContributor>) JRClassLoader.loadClassForName(DEFAULT_CONTRIBUTORS[i]);
				ResourceBundle jrViewerResBundel = ResourceBundle.getBundle("net/sf/jasperreports/view/viewer", jrv.getLocale());
				Constructor<? extends JRSaveContributor> constructor = saveContribClass.getConstructor(new Class[] { Locale.class, ResourceBundle.class });
				JRSaveContributor saveContrib = constructor.newInstance(new Object[] { jrv.getLocale(), jrViewerResBundel });
				jrSaveContribs[i] = saveContrib;
			}
			catch (Exception e)
			{
				Debug.error(e);
			}
		}

		// unfortunately "setSaveContributors" has been moved to the toolbar,
		// and there is no better way to have access to it, then finding it in the panel
		for(int i = 0; i < jrv.getComponentCount(); i++)
		{
			if(jrv.getComponent(i) instanceof JRViewerToolbar)
			{
				((JRViewerToolbar)jrv.getComponent(i)).setSaveContributors(jrSaveContribs);
			}
		}
	}

	/**
	 * Property used in order to get or set the Jasper Viewer's export formats.
	 * 
	 * @sample 
	 * var defaultExportFormats = plugins.jasperPluginRMI.viewerExportFormats;
	 * application.output(defaultExportFormats);
	 * 
	 * // use the default export constants of the plugin, of the OUTPUT_FORMAT constants node; 
	 * // the following formats are available for setting the viewer export formats: 
	 * // PDF, JRPRINT, RTF, ODT, HTML, XLS_1_SHEET, XLS, CSV, XML
	 * // and there is an extra Xml with Embedded Images export type available for the Viewer, denoted by 'xml_embd_img'
	 * // the first export format in the list will be the default one displayed in the Save dialog of the Viewer
	 * plugins.jasperPluginRMI.viewerExportFormats = [OUTPUT_FORMAT.PDF, OUTPUT_FORMAT.RTF, 'xml_embd_img'];
	 * 
	 * @return the list of desired export formats; the first one will be the default export format.
	 */
	public String[] js_getViewerExportFormats() throws Exception
	{
		return viewerExportFormats;
	}

	/**
	 * Finds and return the page to be inserted from the provided jasper print.
	 * @param jasperPrint the jasper print which contains the page to insert
	 * @return the Page, where the moved page(s) will be inserted
	 * @performs Iterates over the JasperPrint pages searching for the FIRST appearence of the String:
	 * "HIDDEN TEXT TO MARK THE INSERT PAGE"; and returning that particular page the Pages to move will be placed in the
	 * right order beginning at this page
	 */
	private static int getInsertPage(JasperPrint jasperPrint)
	{
		if (jasperPrint != null)
		{
			List<JRPrintPage> pages = jasperPrint.getPages();
			if (pages != null && pages.size() > 0)
			{
				String key = "HIDDEN TEXT TO MARK THE INSERT PAGE";
				JRPrintPage page = null;
				JRPrintElement element = null;
				int i = pages.size() - 1;
				int k = 0;
				boolean isFoundPageIndex = false;
				for (k = 1; k <= i + 1;)
				{
					while (!isFoundPageIndex)
					{

						page = pages.get(k);
						Collection<JRPrintElement> elements = page.getElements();

						if (elements != null && elements.size() > 0)
						{
							Iterator<JRPrintElement> it = elements.iterator();
							while (it.hasNext() && !isFoundPageIndex)
							{
								element = it.next();
								if (element instanceof JRPrintText)
								{
									if (key.equals(JRStyledTextUtil.getInstance(DefaultJasperReportsContext.getInstance()).getTruncatedText((JRPrintText) element)))
									{
										isFoundPageIndex = true;
										break;
									}
								}
							}
						}
						k++;

					}
					if (isFoundPageIndex)
					{
						break;
					}
				}

				if (isFoundPageIndex)
				{
					return k - 1;
				}

			}
		}
		return -1;
	}

	/**
	 * Moves the table of contents page, from the japsper print, to the page indicated.
	 * @param jasperPrint the jasperPrint object
	 * @param insertPage an integer indicating where to insert the page
	 * @return a resuting jasper print object with the inserted page 
	 * @performs The Moving
	 */
	private static JasperPrint moveTableOfContents(JasperPrint jasperPrint, int insertPage)
	{
		if (jasperPrint != null)
		{
			List<JRPrintPage> pages = jasperPrint.getPages();
			if (pages != null && pages.size() > 0)
			{
				// finding WHAT to insert
				String key = "HIDDEN TEXT TO MARK THE BEGINNING OF THE TABEL OF CONTENTS";
				JRPrintPage page = null;
				JRPrintElement element = null;
				int i = pages.size() - 1;
				boolean isFound = false;
				while (i >= 0 && !isFound)
				{
					page = pages.get(i);
					Collection<JRPrintElement> elements = page.getElements();
					if (elements != null && elements.size() > 0)
					{
						Iterator<JRPrintElement> it = elements.iterator();
						while (it.hasNext() && !isFound)
						{
							element = it.next();
							if (element instanceof JRPrintText)
							{
								if (key.equals(JRStyledTextUtil.getInstance(DefaultJasperReportsContext.getInstance()).getTruncatedText((JRPrintText) element)))
								{
									isFound = true;
									break;
								}
							}
						}
					}
					i--;
				}

				if (isFound)
				{
					for (int j = i + 1; j < pages.size(); j++)
					{
						jasperPrint.addPage(insertPage, jasperPrint.removePage(j));
						insertPage++;
					}
				}
			}
		}

		return jasperPrint;
	}

	/**
	 * Converts the provided object into an array of bytes.
	 * @param obj the input object
	 * @return the result array of bytes
	 * @throws java.io.IOException
	 */
	public static byte[] getBytes(Object obj) throws java.io.IOException
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(obj);
		oos.flush();
		oos.close();
		bos.close();
		byte[] data = bos.toByteArray();
		return data;
	}

	/**
	 * @return the id of the client plugin
	 */
	public String getPluginClientID()
	{
		return plugin.getIClientPluginAccess().getClientID();
	}

	/**
	 * Creates a map containing parameters for the exporter using the map provided as input. 
	 * This method converts the parameters provided to the run report function to parameters which are
	 * needed (and in a format which is understood) for the exporter.
	 * @param parameters the map with parameters for the exporter
	 * @return a map with specific exporter parameters
	 */
	private Map<String, Object> createExporterParametersMap(Map<String, Object> parameters)
	{
		if (parameters == null) return null;

		Map<String, Object> aux = new HashMap<String, Object>();

		// leaving these in just for legacy purposes
		if (parameters.containsKey(EXPORTER_PARAMETERS.OFFSET_X)) aux.put(EXPORTER_PARAMETERS.OFFSET_X, Integer.valueOf(((Double) parameters.get(EXPORTER_PARAMETERS.OFFSET_X)).intValue()));
		if (parameters.containsKey(EXPORTER_PARAMETERS.OFFSET_Y)) aux.put(EXPORTER_PARAMETERS.OFFSET_Y, Integer.valueOf(((Double) parameters.get(EXPORTER_PARAMETERS.OFFSET_Y)).intValue()));
		if (parameters.containsKey(EXPORTER_PARAMETERS.PAGE_INDEX)) aux.put(EXPORTER_PARAMETERS.PAGE_INDEX, Integer.valueOf(((Double) parameters.get(EXPORTER_PARAMETERS.PAGE_INDEX)).intValue()));
		if (parameters.containsKey(EXPORTER_PARAMETERS.START_PAGE_INDEX)) aux.put(EXPORTER_PARAMETERS.START_PAGE_INDEX, Integer.valueOf(((Double) parameters.get(EXPORTER_PARAMETERS.START_PAGE_INDEX)).intValue()));
		if (parameters.containsKey(EXPORTER_PARAMETERS.END_PAGE_INDEX)) aux.put(EXPORTER_PARAMETERS.END_PAGE_INDEX, Integer.valueOf(((Double) parameters.get(EXPORTER_PARAMETERS.END_PAGE_INDEX)).intValue()));

		// copy over only the exporter parameters
		for (Map.Entry<String, Object> entry : parameters.entrySet())
		{
			if (entry.getKey().startsWith("EXPORTER_PARAMETER:")) aux.put(entry.getKey(), entry.getValue());
		}

		return aux;
	}
	
	private static boolean isWebClient(int applicationType)
	{
		return (applicationType == IClientPluginAccess.WEB_CLIENT) || (applicationType == 9);	// 9 = NGClient
	}
}