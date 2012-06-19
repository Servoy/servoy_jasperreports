/*
 * ============================================================================
 * GNU Lesser General Public License
 * ============================================================================
 *
 * Servoy - Smart Technology For Smart Clients.
 * Copyright © 1997-2009 Servoy BV http://www.servoy.com
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

import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import javax.print.PrintService;
import javax.swing.WindowConstants;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRPrintElement;
import net.sf.jasperreports.engine.JRPrintPage;
import net.sf.jasperreports.engine.JRPrintText;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperPrintManager;
import net.sf.jasperreports.engine.util.JRClassLoader;
import net.sf.jasperreports.engine.util.JRSaver;
import net.sf.jasperreports.view.JRSaveContributor;
import net.sf.jasperreports.view.JRViewer;

import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * IScriptObject impl. For external library dependencies, see:
 * http://www.jasperforge
 * .org/jaspersoft/opensource/business_intelligence/jasperreports
 * /requirements.html
 */
public class JasperReportsProvider implements IScriptObject {
	private JasperReportsPlugin plugin;

	protected static ThreadLocal<IJasperReportsService> jasperReportsLocalService = new ThreadLocal<IJasperReportsService>();
	protected static ThreadLocal<String> jasperReportsLocalClientID = new ThreadLocal<String>();

	private static final int PARAMETER = 1;

	private static final int TOOLTIP = 2;

	private static final int EXAMPLE = 3;

	private String[] viewerExportFormats = null;

	private static final String PROPERTIES[][][] = {
			// methodname, parameters, tooltip, example
			{
					{ "runReport" },
					{
							"source(serverName|foundset|dataset)",
							"reportFileName ",
							"exportFileName / boolean showPrintDialog / printerName",
							"outputFormat", "parameters", "[locale]",
							"[moveTableOfContents]" }, { "Execute a Report" },
					{} },
			{
					{ "writeFileToReportsDir" },
					{ "reportFileName", "JSFile reportFile" },
					{ "Store a reportFile on the Server" },
					{ "// .jasper or .jrxml files can be used\n"
							+ "var file = plugins.file.readFile('e:\\\\temp\\\\sample.jasper');\n"
							+ "plugins.jasperPluginRMI.writeFileToReportsDir('myCustomerReport.jasper', file);\n"
							+ "// Writes to a subfolder from the reports directory. All the folders from the path must exist.\n"
							+ "plugins.jasperPluginRMI.writeFileToReportsDir('\\\\subdir\\\\myCustomerReport.jasper', file);\n" } },
			{
					{ "deleteFileFromReportsDir" },
					{ "reportFileName" },
					{ "Delete a reportFile from the Server" },
					{ "var reportFile2Delete = 'myCustomerReport.jrxml';\n"
							+ "plugins.jasperPluginRMI.deleteFileFromReportsDir(reportFile2Delete);\n" } },
			{
					{ "readFileFromReportsDir" },
					{ "reportFileName" },
					{ "Retrieve a reportFile from the Server" },
					{ "var reportFileArray = plugins.jasperPluginRMI.readFileFromReportsDir('myCustomerReport.jasper');\n"
							+ "// Subfolders can be used to read files.\n"
							+ "var reportFileArray = plugins.jasperPluginRMI.readFileFromReportsDir('\\\\subdir\\\\myCustomerReport.jasper');" } },
			{
					{ "compileReport" },
					{ "reportFileName", "[destinationFileName]" },
					{ "Compile a Jasper Reports .jrxml file to a .jasper file." },
					{ "// Compile the .jrxml jasper report file to a .jasper file. The name of the compiled file is given by the report name.\n"
							+ "// The report name as an absolute path. Results the compiled c:\\temp\\samplereport.jasper file.\n"
							+ "var success = plugins.jasperPluginRMI.compileReport('c:\\\\temp\\\\samplereport.jrxml');\n"
							+ "// The report name as a relative path. The file will be searched relative to the ReportDirectory.\n"
							+ "var success = plugins.jasperPluginRMI.compileReport('myCustomerReport1.jrxml');\n"
							+ "var success = plugins.jasperPluginRMI.compileReport('\\\\subdir\\\\myCustomerReport2.jrxml');\n"
							+ "// To specify a different destination file than the original filaname, the second parameter can be incouded.\n"
							+ "// If it is relative, the file will be created relative to the ReportDirectory.\n"
							+ "var success = plugins.jasperPluginRMI.compileReport('c:\\\\temp\\\\samplereport.jrxml', 'd:\\\\temp2\\\\destreport.jasper');" } },
			{
					{ "getReports" },
					{ "filter" },
					{ "Retrieve a String array of available reports, based on the reports directory." },
					{ "// COMPILED - only compiled reports, NONCOMPILED - only non-compiled reports\n// No parameter returns all the reports\nvar result = plugins.jasperPluginRMI.getReports('NONCOMPILED');\napplication.output(result[0]);\n " 
						+ "\n // using a string as the search filter\n//var result = plugins.jasperPluginRMI.getReports('*criteria*');\n//for(var i=0; i<result.length; i++)\n//application.output(result[i]);\n " } },
			{
					{ "getReportParameters" },
					{ "report" },
					{ "Retrieve a JSDataSet with the parameters except the system defined ones." },
					{ "var ds = plugins.jasperPluginRMI.getReportParameters('sample.jrxml');\nvar csv = ds.getAsText(',','\\n','\"',true);\napplication.output(csv);" } },
			{
					{ "reportDirectory" },
					{},
					{ "Property for retrieving the reports directory from the server." },
					{ "// By defaul the value is read from the adim page Server Plugins, the directory.jasper.report property.\n// If the client modifies the reportDirectory property, this value will be used instead of the default one for the whole client session and only for this client. Each client session has it's own reportDirectory value." } },
			{
					{ "relativeExtraDirectories" },
					{},
					{ "Property for retrieving and setting the paths to the extra resources directories. The paths are set per client and are relative to the server corresponding directories setting." },
					{ "// By defaul the value is read from the adim page Server Plugins, the directories.jasper.extra property.\n// If the client modifies the default property, this value will be used instead of the \n// default one for the whole client session and only for this client. \n// Each client session has it's own extraDirectories value.\n// NOTE: Extra directories are not searched recursively." } },		
			{
					{ "relativeReportsDirectory" },
					{},
					{ "Property for retrieving and setting the path to the reports directory, set by the current client, relative to the server reports directory." },
					{ "// By default the value is read from the adim page Server Plugins, the directory.jasper.report property.\n//A client is only able to set a path relative to the server report directory. \n//If the client modifies this property, its value will be used instead of the default one, for the whole client session and only for this client. \n//Each client session has it's own relativeReportDirectory value." } },
			{
					{ "viewerExportFormats" },
					{},
					{ "Get or set the Jasper Viewer's export formats" },
					{ "var defaultExportFormats = plugins.jasperPluginRMI.viewerExportFormats;\n"
							+ "application.output(defaultExportFormats);\n"
							+ "// use the default export constants of the plugin, of the OUTPUT_FORMAT constants node;\n"
							+ "// the following formats are availabe for setting the viewer export formats:\n "
							+ "// PDF, JRPRINT, RTF, ODT, HTML, XLS_1_SHEET, XLS, CSV, XML\n"
							+ "// and there is an extra Xml with Embedded Images export type available for the Viewer, denoted by 'xml_embd_img' \n"
							+ "// the first export format in the list will be the default one displayed in the Save dialog of the Viewer.\n"
							+ "plugins.jasperPluginRMI.viewerExportFormats = [OUTPUT_FORMAT.PDF, OUTPUT_FORMAT.RTF, 'xml_embd_img'];" } } };

	private String relativeReportsDir;
	private String relativeExtraDirs;
	
	JasperReportsProvider(JasperReportsPlugin p) throws Exception {
		plugin = p;
		relativeReportsDir = "";
		relativeExtraDirs = "";
	}

	public String[] getProperty(String methodName, int prop) {
		for (int i = 0; i < PROPERTIES.length; i++) {
			String as[][] = PROPERTIES[i];
			if (as[0][0].equals(methodName)) {
				return as[prop];
			}
		}
		return null;
	}

	public boolean isDeprecated(String methodName) {
		if (methodName == null)
			return false;
		else if ("".equals(methodName)) {
			return true;
		} else if ("jasperReport".equals(methodName)) {
			return true;
		} else if ("jasperCompile".equals(methodName)) {
			return true;
		} else if ("writeFile".equals(methodName)) {
			return true;
		} else if ("readFile".equals(methodName)) {
			return true;
		} else if ("reportDirectory".equals(methodName)) {
			return true;
		} else if ("extraDirectories".equals(methodName)) {
			return true;
		}
			
		return false;
	}

	public String[] getParameterNames(String methodName) {
		return getProperty(methodName, PARAMETER);
	}

	public String getSample(String methodName) {
		if (methodName == null)
			return null;
		else if ("runReport".equals(methodName)) {
			StringBuffer retval = new StringBuffer();
			retval.append("// The method runs a client report specified by the second parameter acording to the output format.\n ");
			retval.append("// The report can be a compiled jasper file or a jrxml file from a relative path to the reportDirectory or an absolute one.\n\n");
			retval.append("/* To view the result of the customers report in the Jasper Report viewer in the SmartClient or as PDF in the WebClient\n");
			retval.append(" * Note: the parameters argument is used to send additional parameters into the report. For example:\n");
			retval.append(" * {pcustomerid: forms.customers.customer_id} to send just 1 parameter called pcustomerid, which contains the value of dataprovider customer_id in the selected record on the customers form\n");
			retval.append(" * The parameters argument is an Object, which can be instantiated in two ways:\n");
			retval.append(" * var o = new Object();\n");
			retval.append(" * o.pcustomerid = forms.customers.customer_id;\n");
			retval.append(" * or:\n");
			retval.append(" * var o = {pcustomerid: forms.customers.customer_id};\n");
			retval.append(" */\n");
			retval.append("application.updateUI(); //to make sure the Servoy window doesn't grab focus after showing the Jasper Viewer\n");
			retval.append("plugins.jasperPluginRMI.runReport(forms.customers.controller.getServerName(),'myCustomerReport.jasper',null,OUTPUT_FORMAT.VIEW,{pcustomerid: forms.customers.customer_id});\n");
			retval.append("\n");
			retval.append("/* To request a report in a different Language than the current language of the client, it's possible to specify a Locale string as the locale argument. For example: 'en' or 'es' or 'nl'\n");
			retval.append(" * When the locale argument is not specified, the report will be in the current langauge of the Client\n");
			retval.append(" * i18n keys of Servoy can be used inside Jasper Reports using the $R{i18n-key} notation\n");
			retval.append(" */\n");
			retval.append("//plugins.jasperPluginRMI.runReport(forms.customers.controller.getServerName(),'myCustomerReport.jasper',null,OUTPUT_FORMAT.VIEW,{pcustomerid: forms.customers.customer_id},'nl');\n");
			retval.append("\n");
			retval.append("/* To print the result of the customers report in the SmartClient (to a specified printer), the outputType should be specified as 'print' (OUTPUT_FORMAT.PRINT).\n");
			retval.append(" * The third parameter can contain the name of the printer to which the report needs to be printed\n");
			retval.append(" * or can contain true (boolean value) to show a printdialog before printing.\n");
			retval.append(" * If false (boolean value) or null is specified, it will print without showing the print dialog to the default printer.\n");
			retval.append(" * Note: In the WebClient a PDF will be pushed to the Client when the outputType is specified as 'print' (OUTPUT_FORMAT.PRINT).\n");
			retval.append(" */\n");
			retval.append("//plugins.jasperPluginRMI.runReport(forms.customers.controller.getServerName(),'myCustomerReport.jasper',null,OUTPUT_FORMAT.PRINT,{pcustomerid: forms.customers.customer_id});\n");
			retval.append("\n");
			retval.append("/* To generate the report in the specified output format and save the result to 'myReport.html' in the root of the C drive:\n");
			retval.append(" * Supported output formats are: xhtml, html, pdf, excel( or xls), xls_1_sheet (1 page per sheet), ods, rtf, txt, csv, odt, docx, jrprint and xml.\n");
			retval.append(" * These are available as constants in the OUTPUT_FORMAT node of the plugin's tree.\n");
			retval.append(" * Note: in the WebClient, the file will be saved serverside, so the specified path needs to be valid serverside\n");
			retval.append(" */\n");
			retval.append("//plugins.jasperPluginRMI.runReport(forms.customers.controller.getServerName(),'myCustomerReport.jasper','c:/myReport.xhtml',OUTPUT_FORMAT.XHTML,{pcustomerid: forms.customers.customer_id});\n");
			retval.append("//plugins.jasperPluginRMI.runReport(forms.customers.controller.getServerName(),'myCustomerReport.jasper','c:/myReport.html',OUTPUT_FORMAT.HTML,{pcustomerid: forms.customers.customer_id});\n");
			retval.append("//plugins.jasperPluginRMI.runReport(forms.customers.controller.getServerName(),'myCustomerReport.jasper','c:/myReport.pdf',OUTPUT_FORMAT.PDF,{pcustomerid: forms.customers.customer_id});\n");
			retval.append("//plugins.jasperPluginRMI.runReport(forms.customers.controller.getServerName(),'myCustomerReport.jasper','c:/myReport.rtf',OUTPUT_FORMAT.RTF,{pcustomerid: forms.customers.customer_id});\n");
			retval.append("//plugins.jasperPluginRMI.runReport(forms.customers.controller.getServerName(),'myCustomerReport.jasper','c:/myReport.jrprint',OUTPUT_FORMAT.JRPRINT,{pcustomerid: forms.customers.customer_id});\n");
			retval.append("//plugins.jasperPluginRMI.runReport(forms.customers.controller.getServerName(),'myCustomerReport.jasper','c:/myReport.txt',OUTPUT_FORMAT.TXT,{pcustomerid: forms.customers.customer_id});\n");
			retval.append("//plugins.jasperPluginRMI.runReport(forms.customers.controller.getServerName(),'myCustomerReport.jasper','c:/myReport.csv',OUTPUT_FORMAT.CSV,{pcustomerid: forms.customers.customer_id});\n");
			retval.append("//plugins.jasperPluginRMI.runReport(forms.customers.controller.getServerName(),'myCustomerReport.jasper','c:/myReport.odt',OUTPUT_FORMAT.ODT,{pcustomerid: forms.customers.customer_id});\n");
			retval.append("//plugins.jasperPluginRMI.runReport(forms.customers.controller.getServerName(),'myCustomerReport.jasper','c:/myReport.docx',OUTPUT_FORMAT.DOCX,{pcustomerid: forms.customers.customer_id});\n");
			retval.append("//plugins.jasperPluginRMI.runReport(forms.customers.controller.getServerName(),'myCustomerReport.jasper','c:/myReport.xls',OUTPUT_FORMAT.XLS /*or excel OUTPUT_FORMAT.EXCEL*/,{pcustomerid: forms.customers.customer_id});\n");
			retval.append("//plugins.jasperPluginRMI.runReport(forms.customers.controller.getServerName(),'myCustomerReport.jasper','c:/myReport.xls',OUTPUT_FORMAT.XLS_1_SHEET,{pcustomerid: forms.customers.customer_id});\n");
			retval.append("//plugins.jasperPluginRMI.runReport(forms.customers.controller.getServerName(),'myCustomerReport.jasper','c:/myReport.ods',OUTPUT_FORMAT.ODS,{pcustomerid: forms.customers.customer_id});\n");
			retval.append("//plugins.jasperPluginRMI.runReport(forms.customers.controller.getServerName(),'myCustomerReport.jasper','c:/myReport.xml',OUTPUT_FORMAT.XML,{pcustomerid: forms.customers.customer_id});\n");
			retval.append("\n");
			retval.append("/* Jasper Reports supports queries with IN operators through the following notation: X${IN,columnName,parameterName} like 'select * from customers where X$(IN,customer_id,pcustomeridlist)\n");
			retval.append(" * When using this notation, the pcustomeridlist parameter needs to contain one or more values in the following way:\n");
			retval.append(" */\n");
			retval.append("//var idlist = new Array()\n");
			retval.append("//idlist[0] = 1\n");
			retval.append("//idlist[1] = 26\n");
			retval.append("//plugins.jasperPluginRMI.jasperReport(forms.customers.controller.getServerName(),'myCustomerReport.jasper',null,OUTPUT_FORMAT.VIEW,{pcustomeridlist: idlist});\n");
			retval.append("\n//The return value is a byte array with the content of the file generated that can be further used.\n");
			retval.append("//var res = plugins.jasperPluginRMI.runReport(currentcontroller.getServerName(),'samplereport.jrxml', null, OUTPUT_FORMAT.PDF, null);\n");
			retval.append("//plugins.file.writeFile('e:\\\\sample.pdf', res); \n");
			retval.append("\n/* In order to run the report and move the table of contents(marked with the string: \"HIDDEN TEXT TO MARK THE BEGINNING OF THE TABEL OF CONTENTS\") \n");
			retval.append(" * to the Insert page, which has to be identified by the string: \"HIDDEN TEXT TO MARK THE INSERT PAGE\" \n");
			retval.append(" */\n");
			retval.append("// plugins.jasperPluginRMI.runReport(forms.customers.controller.getServerName(),'myCustomerReport.jasper','c:/myReport.xml',OUTPUT_FORMAT.XML,{pcustomerid: forms.customers.customer_id}, null, true);\n");
			retval.append("\n/* NOTE: in Servoy 5.x and above, instead of forms.customers.controller.getServerName() use code as the following, to get the server name: */\n");
			retval.append("// var ds = foundset.getDataSource();\n");
			retval.append("// var d = ds.split('/);\n");
			retval.append("// var myServer = d[1]; \n");
			return retval.toString();
		} else {
			String[] as = getProperty(methodName, EXAMPLE);
			if (as != null)
				return as[0];
		}
		return null;
	}

	public String getToolTip(String methodName) {
		String[] as = getProperty(methodName, TOOLTIP);
		if (as != null)
			return as[0];
		return null;
	}

	public Class<?>[] getAllReturnedTypes() {
		return new Class[] { OUTPUT_FORMAT.class, JR_SVY_VIEWER_DISPLAY_MODE.class };
	}

	/**
	 * Fix for bug ID 6255588 from Sun bug database
	 * 
	 * @param job
	 *            print job that the fix applies to
	 */
	public static void initPrinterJobFields(PrinterJob job) {
		Class<?> clazz = job.getClass();
		try {
			Class<?> printServiceClass = Class.forName("javax.print.PrintService");
			Method method = clazz.getMethod("getPrintService", (Class[]) null);
			Object printService = method.invoke(job, (Object[]) null);
			method = clazz.getMethod("setPrintService",
					new Class[] { printServiceClass });
			method.invoke(job, new Object[] { printService });
		} catch (Exception e) {
			Debug.error(e);
		}
	}

	public boolean setPrintService(PrinterJob printJob, String printer)
			throws Exception {
		PrintService[] service = PrinterJob.lookupPrintServices();
		// enable if on list otherwise error
		boolean match = false;
		int count = service.length;
		try {
			for (int i = 0; i < count; i++) {
				if (service[i].getName().indexOf(printer) != -1) {
					printJob.setPrintService(service[i]);
					i = count;
					match = true;
				}
			}
		} catch (PrinterException e) {
			Debug.error(e);
			throw new Exception(e.getMessage());
		}
		return match;
	}

	/**
	 * @deprecated
	 * @see js_runReport(String dbalias, String report, Object arg, String type,
	 *      Object parameters)
	 */
	public byte[] js_jasperReport(String dbalias, String report, Object arg,
			String type, Object parameters) throws Exception {
		return js_runReport(dbalias, report, arg, type, parameters, null);
	}

	/**
	 * @deprecated
	 * @see js_runReport(String dbalias, String report, Object arg, String type,
	 *      Object parameters, String localeString)
	 */
	public byte[] js_jasperReport(String dbalias, String report, Object arg,
			String type, Object parameters, String localeString)
			throws Exception {
		return js_runReport(dbalias, report, arg, type, parameters,
				localeString);
	}

	public byte[] js_runReport(Object source, String report, Object arg,
			String type, Object parameters) throws Exception {
		return js_runReport(source, report, arg, type, parameters, null);
	}

	public byte[] js_runReport(Object source, String report, Object arg,
			String type, Object parameters, String localeString)
			throws Exception {
		return js_runReport(source, report, arg, type, parameters,
				localeString, Boolean.FALSE);
	}

	public byte[] js_runReport(Object source, String report, Object arg,
			String type, Object parameters, String localeString,
			Boolean moveTableOfContent) throws Exception {
		return runReport(source, report, arg, type, parameters, localeString,
				Boolean.TRUE.equals(moveTableOfContent), false);
	}

	// public, but not scriptable - just for the corresponding Bean's usage
	public byte[] runReportForBean(Object source, String report, Object arg,
			String type, Object parameters, String localeString,
			Boolean moveTableOfContent) throws Exception {
		return runReport(source, report, arg, type, parameters, localeString,
				Boolean.TRUE.equals(moveTableOfContent), true);
	}
	
	private byte[] runReport(Object source, String report, Object arg,
			String exportFormat, Object parameters, String localeString,
			boolean moveTableOfContent, boolean returnJustJasperPrint) throws Exception {

		// Check if the directory.jasper.report setting has not yet been set.
		String pluginReportsDirectory = plugin.getJasperReportsDirectory();
		if (pluginReportsDirectory == null
				|| (pluginReportsDirectory != null && ("")
						.equals(pluginReportsDirectory.trim()))) {
			String noPluginDirMsg = "Your jasper.report.directory setting has not been set.\nReport running will abort.";
			Debug.error(noPluginDirMsg);
			throw new Exception(noPluginDirMsg);
		}

		String type = exportFormat.toLowerCase();
		
		// unwrapping of arguments
		source = JSArgumentsUnwrap.unwrapJSObject(source, plugin.getIClientPluginAccess());
		Map params = (Map) JSArgumentsUnwrap.unwrapJSObject(parameters, plugin.getIClientPluginAccess());
		if (params == null)
			params = new HashMap();
		
		boolean showPrintDialog = false;
		String file = "";
		boolean nooutput = false;
		if (arg instanceof String) {
			file = arg.toString();
		} else if (arg instanceof Boolean) {
			showPrintDialog = Utils.getAsBoolean(arg);
		} else {
			if (arg != null)
				file = arg.toString(); // To support passing in a JSFile object
			else
				nooutput = true;
		}

		if (source == null) {
			throw new Exception("No data source <null> has been provided");
		}
		Debug.trace("JasperTrace: JasperReport initialize");

		IJasperReportsService jasperReportService = plugin.connectJasperService();

		// check out type of data source (and how to run reports)
		IJasperReportRunner jasperReportRunner;
		String txid = null;
		if (source instanceof String) {
			txid = plugin.getIClientPluginAccess().getTransactionID((String)source);
			jasperReportRunner = jasperReportService; // run report remote
		} else if (source instanceof JRDataSource) {
			jasperReportRunner = new JasperReportRunner(jasperReportService); // run reports in client
		} else {
			throw new Exception("Unsupported data source: " + source.getClass());
		}

		// in case the server is not started in developer
		if (jasperReportService != null) {

			// needed before filling
			jasperReportsLocalService.set(jasperReportService);
			jasperReportsLocalClientID.set(this.getPluginClientID());

			ClassLoader savedCl = Thread.currentThread().getContextClassLoader();
			try {
				Thread.currentThread().setContextClassLoader(plugin.getIClientPluginAccess().getPluginManager().getClassLoader());
				Debug.trace("JasperTrace: JasperReport service found");

				// handle i18n
				int applicationType = plugin.getIClientPluginAccess().getApplicationType();
				JasperReportsI18NHandler.appendI18N(params,	applicationType == IClientPluginAccess.WEB_CLIENT, plugin.getIClientPluginAccess(), localeString); 

				// Fill the report and get the JasperPrint instance.
				// Also modify the JasperPrint in case you want to move the table of contents.
				JasperPrint jp = jasperReportRunner.getJasperPrint(plugin
							.getIClientPluginAccess().getClientID(), source,
							txid, report, params, relativeReportsDir,
							relativeExtraDirs);
				
				if (moveTableOfContent) {
					int iP = getInsertPage(jp);
					jp = moveTableOfContents(jp, iP);
				}
				
				// this is for the JasperViewerServoyBean
				if (returnJustJasperPrint)
				{
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					JRSaver.saveObject(jp, baos);
					return baos.toByteArray();
				}
				
				Map exporterParams = createExporterParametersMap(params);
				byte[] jsp = null;

				// 1. WebClient
				if (applicationType == IClientPluginAccess.WEB_CLIENT) {

					String mimeType = "application/octet-stream";
					if (type.equals(OUTPUT_FORMAT.VIEW) || type.equals(OUTPUT_FORMAT.PRINT)) {
						mimeType = JasperReportsWebViewer.MIME_TYPE_PDF;
					} else if (type.equals(OUTPUT_FORMAT.PDF)) {
						mimeType = JasperReportsWebViewer.MIME_TYPE_PDF;
					} else if (type.equals(OUTPUT_FORMAT.CSV)) {
						mimeType = JasperReportsWebViewer.MIME_TYPE_CSV;
					} else if (type.equals(OUTPUT_FORMAT.DOCX)) {
						mimeType = JasperReportsWebViewer.MIME_TYPE_DOCX;
					} else if (type.equals(OUTPUT_FORMAT.EXCEL)) {
						mimeType = JasperReportsWebViewer.MIME_TYPE_XLS;
					} else if (type.equals(OUTPUT_FORMAT.HTML)) {
						mimeType = JasperReportsWebViewer.MIME_TYPE_HTML;
					} else if (type.equals(OUTPUT_FORMAT.ODS)) {
						mimeType = JasperReportsWebViewer.MIME_TYPE_ODS;
					} else if (type.equals(OUTPUT_FORMAT.ODT)) {
						mimeType = JasperReportsWebViewer.MIME_TYPE_ODT;
					} else if (type.equals(OUTPUT_FORMAT.RTF)) {
						mimeType = JasperReportsWebViewer.MIME_TYPE_RTF;
					} else if (type.equals(OUTPUT_FORMAT.TXT)) {
						mimeType = JasperReportsWebViewer.MIME_TYPE_TXT;
					} else if (type.equals(OUTPUT_FORMAT.XHTML)) {
						mimeType = JasperReportsWebViewer.MIME_TYPE_XHTML;
					} else if (type.equals(OUTPUT_FORMAT.XLS)) {
						mimeType = JasperReportsWebViewer.MIME_TYPE_XLS;
					} else if (type.equals(OUTPUT_FORMAT.XLS_1_SHEET)) {
						mimeType = JasperReportsWebViewer.MIME_TYPE_XLS;
					} else if (type.equals(OUTPUT_FORMAT.XML)) {
						mimeType = JasperReportsWebViewer.MIME_TYPE_XML;
					} else {
						throw new Exception("JasperTrace: Jasper Exception: Unsupported web client output format");
					}

					if (type.equals(OUTPUT_FORMAT.VIEW) || type.equals(OUTPUT_FORMAT.PRINT)) {
						jsp = JasperReportRunner.getJasperBytes("pdf", jp, 
								jasperReportService.getCheckedExtraDirectoriesRelativePath(relativeExtraDirs),exporterParams);
						JasperReportsWebViewer.show(plugin.getIClientPluginAccess(), jsp, file, "pdf", 	mimeType);
					} else {
						jsp = JasperReportRunner.getJasperBytes(type, jp, 
								jasperReportService.getCheckedExtraDirectoriesRelativePath(relativeExtraDirs),exporterParams);
						if (nooutput) {
							JasperReportsWebViewer.show(plugin.getIClientPluginAccess(), jsp, file, type, mimeType);
						} else {
							saveByteArrayToFile(file, jsp);
						}
					}
				}

				// 2. SmartClient
				else {

					// a. SmartClient "view"
					if (type.toLowerCase().startsWith("view")) {
						CustomizedJasperViewer jasperviewer;

						if (localeString != null) {
							jasperviewer = new CustomizedJasperViewer(jp, false, new Locale(localeString));
						} else {
							jasperviewer = new CustomizedJasperViewer(jp, false);
						}

						if (viewerExportFormats != null)
							setViewerSaveContributors(jasperviewer.getJRViewer(), viewerExportFormats);

						if (jp != null && jp.getPages() != null
								&& jp.getPages().size() > 0) {
							jasperviewer.setVisible(true);
							jasperviewer.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						} else {
							jasperviewer.dispose();
						}
					}

					// b. SmartClient "print"
					// Printing only supported in SC. Printing in WC handled
					// previously though PDF push.
					else if (type.toLowerCase().startsWith("print")) {
						// Shows print dialog before printing (if arg is true/"true")
						if (showPrintDialog || file.equalsIgnoreCase("true")
								|| file == null) {
							JasperPrintManager.printReport(jp, true);
						}
						// Skips print dialog (if arg is false/"false" or null)
						else if ((!showPrintDialog && file.equals("")) // also equivalent to arg == null
								|| file.equalsIgnoreCase("false")
								|| file.equalsIgnoreCase("default")) {
							JasperPrintManager.printReport(jp, false);
						}
						// Assumes parameter file contains a printerName
						else {
							Debug.trace("JasperTrace: printer: " + file);
							PrinterJob printJob = PrinterJob.getPrinterJob();
							// fix for bug ID 6255588 from Sun bug database
							initPrinterJobFields(printJob);
							/*
							 * or another workaround try {
							 * printerJob.setPrintService
							 * (printerJob.getPrintService()); } catch
							 * (PrinterException e) {}
							 */

							if (setPrintService(printJob, file)) {
								JasperReportsPrinter.printPages(jp, printJob);
							} else {
								Debug.trace("JasperTrace: unable to specify printer: " + file);
							}
						}
						// 3. SmartClient other output formats
					} else {
						jsp = jasperReportService.getJasperBytes(plugin.getIClientPluginAccess().getClientID(),
								type, jp, relativeExtraDirs, exporterParams);
						if (!nooutput) {
							saveByteArrayToFile(file, jsp);
						}
					}
				}
				Debug.trace("JasperTrace: JasperReport finished");
				return jsp;
			} catch (Exception e) {
				Debug.error(e);
				throw new Exception(e.getMessage());
			} finally {
				Thread.currentThread().setContextClassLoader(savedCl);
				// cleanup
				jasperReportsLocalService.set(null);
				jasperReportsLocalClientID.set(null);
			}
		}
		Debug.error("JasperTrace: Jasper Exception: No service running");
		throw new Exception("JasperTrace: Jasper Exception: No service running");
	}

	public void saveByteArrayToFile(String filename, byte[] buffertje)
			throws Exception {
		if (filename != null && filename.trim().length() > 0) {
			FileOutputStream fos = new FileOutputStream(filename);
			fos.write(buffertje);
			fos.flush();
			fos.close();
		}
	}

	public boolean js_compileReport(String report) throws Error, Exception {
		return js_compileReport(report, null);
	}

	public boolean js_compileReport(String report, String destination)
			throws Error, Exception {

		Debug.trace("JasperTrace: JasperCompile initialize");
		boolean compiled = false;

		try {
			Debug.trace("JasperTrace: JasperCompile starting");
			compiled = plugin.connectJasperService().jasperCompile(plugin.getIClientPluginAccess().getClientID(), report, destination, relativeReportsDir);
			Debug.trace("JasperTrace: JasperCompile finished");
		} catch (Error err) {
			Debug.error(err);
			throw new Exception(err.getMessage());
		} catch (Exception ex) {
			Debug.error(ex);
			throw new Exception(ex.getMessage());
		}

		return compiled;
	}

	/**
	 * @deprecated
	 * @see js_compileReport(String report)
	 */
	public boolean js_jasperCompile(String report) throws Error, Exception {
		return js_compileReport(report);
	}

	/**
	 * @param forceRecompile
	 * @deprecated
	 * @see js_compileReport(String report)
	 */
	public boolean js_jasperCompile(String report, boolean forceRecompile)
			throws Error, Exception {
		return js_compileReport(report);
	}

	/**
	 * @deprecated
	 * @see js_writeFileToReportsDir(String filenm, Object obj)
	 */
	public boolean js_writeFile(String filenm, Object obj) throws Exception {
		return js_writeFileToReportsDir(filenm, obj);
	}

	public boolean js_writeFileToReportsDir(String filenm, Object obj)
			throws Exception {

		try {
			Debug.trace("JasperTrace: JasperWriteFile starting");
			boolean b = plugin.connectJasperService().writeFile(plugin.getIClientPluginAccess().getClientID(), filenm, obj, relativeReportsDir);
			Debug.trace("JasperTrace: JasperWriteFile finished");
			return b;
		} catch (Exception e) {
			Debug.error(e);
			return false;
		}
	}

	public boolean js_deleteFileFromReportsDir(String filenm) throws Exception {

		try {
			Debug.trace("JasperTrace: JasperDeleteFileFromReportsDir starting");
			boolean b = plugin.connectJasperService().deleteFile(plugin.getIClientPluginAccess().getClientID(), filenm, relativeReportsDir);
			Debug.trace("JasperTrace: JasperDeleteFileFromReportsDir finished");
			return b;
		} catch (Exception e) {
			Debug.error(e);
			return false;
		}
	}

	/**
	 * @deprecated
	 * @see js_readFileFromReportsDir(String filenm)
	 */
	public byte[] js_readFile(String filenm) throws Exception {
		return js_readFileFromReportsDir(filenm);
	}

	public byte[] js_readFileFromReportsDir(String filenm) throws Exception {

		byte[] b = null;

		try {
			Debug.trace("JasperTrace: JasperReadFile starting");
			b = plugin.connectJasperService().readFile(plugin.getIClientPluginAccess().getClientID(), filenm, relativeReportsDir);
			Debug.trace("JasperTrace: JasperReadFile finished");
		} catch (Exception e) {
			Debug.error(e);
			throw new Exception(e.getMessage());
		}
		return b;
	}

	/**
	 * @deprecated
	 */
	public String js_getReportDirectory() throws Exception {
		return plugin.getJasperReportsDirectory();
	}

	/**
	 * @deprecated
	 */
	public void js_setReportDirectory(String jasperDirectorie) throws Exception {
		js_setRelativeReportsDirectory(jasperDirectorie);
	}
	
	public String js_getRelativeReportsDirectory() throws Exception {
		return relativeReportsDir;
	}
	
	public void js_setRelativeReportsDirectory(String relativeReportDirectory) throws Exception {
		String checkedPath = plugin.connectJasperService().getCheckedRelativeReportsPath(relativeReportDirectory);
		relativeReportsDir = relativeReportDirectory;
		plugin.setJasperReportsDirectory(checkedPath);
	}

	/**
	 * 
	 * @deprecated
	 */
	public String js_getExtraDirectories() throws Exception {
		return plugin.getJasperExtraDirectories();
	}

	public String js_getRelativeExtraDirectories() throws Exception {
		return relativeExtraDirs;
	}
	
	/**
	 * @deprecated
	 */
	public void js_setExtraDirectories(String extraDirectories) throws Exception {
		js_setRelativeExtraDirectories(extraDirectories);
	}
	
	public void js_setRelativeExtraDirectories(String extraDirectories) throws Exception {
		//plugin.setJasperExtraDirectories(extraDirectories);
		String extraDirsRelPath = plugin.connectJasperService().getCheckedExtraDirectoriesRelativePath(extraDirectories);
		relativeExtraDirs = extraDirectories;
		plugin.setJasperExtraDirectories(extraDirsRelPath);
	}

	public String[] js_getReports() throws Exception {
		return getReports(true, true);
	}

	public String[] js_getReports(String filter) throws Exception {
		if (filter.toUpperCase().compareTo("COMPILED") == 0) {
			return getReports(true, false);
		} else if (filter.toUpperCase().compareTo("NONCOMPILED") == 0) {
			return getReports(false, true);
		} else return getReports(filter);
	}
	
	private String[] getReports(String filter)  throws Exception {
		String[] reports = null;

		try {
			Debug.trace("JasperTrace: getReports starting");
			reports = plugin.connectJasperService().getReports(plugin.getIClientPluginAccess().getClientID(), filter);
			Debug.trace("JasperTrace: getReports finished");
		} catch (Exception e) {
			Debug.error(e);
			throw new Exception(e.getMessage());
		}
		return reports;
	}
	
	private String[] getReports(boolean compiled, boolean uncompiled)
			throws Exception {

		String[] reports = null;

		try {
			Debug.trace("JasperTrace: getReports starting");
			reports = plugin.connectJasperService().getReports(plugin.getIClientPluginAccess().getClientID(), compiled, uncompiled);
			Debug.trace("JasperTrace: getReports finished");
		} catch (Exception e) {
			Debug.error(e);
			throw new Exception(e.getMessage());
		}

		return reports;
	}

	public JSDataSet js_getReportParameters(String report) throws Exception {

		JSDataSet ds = null;

		try {
			Debug.trace("JasperTrace: getReportParameters starting");
			ds = plugin.connectJasperService().getReportParameters(plugin.getIClientPluginAccess().getClientID(), report, relativeReportsDir);
			Debug.trace("JasperTrace: getReportParameters finished");
		} catch (Exception e) {
			Debug.error(e);
			throw new Exception(e.getMessage());
		}

		return ds;
	}

	public String js_getPluginVersion() {
		return "4.0.0 b1";

		/*
		 * Added destination optional parameter for compileReport method Renamed
		 * jasperReport -> runReport, jasperCompile -> compileReport, readFile
		 * -> readFileFromReportsDir, writeFile -> writeFileToReportsDir methods
		 * and deprecated the old ones. Updated methods comments to reflect
		 * changes and functionality. Changed jasperReport to accept null output
		 * for using only the array return value.
		 */
	}

	/**
	 * @param ver
	 */
	public void js_setPluginVersion(String ver) {
		// DO NOTHING. READ ONLY PROPERTY.
	}

	/**
	 * This sets the Jasper Viewer's save contributors, that is the (only)
	 * export formats of the Jasper Viewer. The first item in the list be the
	 * default selected export format. Feature request from <a href=
	 * "http://code.google.com/p/servoy-jasperreports-plugin/issues/detail?id=35"
	 * >Google issue 35</a>.
	 * 
	 * @param saveContribs
	 *            the list of desired export formats; the first one will be the
	 *            default export format.
	 */
	public void js_setViewerExportFormats(String[] saveContribs)
			throws Exception {
		viewerExportFormats = saveContribs;
	}

	/**
	 * Sets the save contributors for the JasperViewer's JRViewer instance.
	 * 
	 * @param jrv
	 *            the JRViewer for which we set the save contributors
	 * @param saveContributors
	 *            the save contributors to be set; the first one will be the
	 *            (default) first one in the "Save as type" list of the "Save"
	 *            dialog.
	 */
	public static void setViewerSaveContributors(JRViewer jrv,
			String[] saveContributors) {

		List<String> defContribs = new ArrayList<String>();
		for (String s : saveContributors) {
			if (OUTPUT_FORMAT.PDF.equals(s))
				defContribs.add("net.sf.jasperreports.view.save.JRPdfSaveContributor");
			else if (OUTPUT_FORMAT.JRPRINT.equals(s))
				defContribs.add("net.sf.jasperreports.view.save.JRPrintSaveContributor");
			else if (OUTPUT_FORMAT.ODT.equals(s))
				defContribs.add("net.sf.jasperreports.view.save.JROdtSaveContributor");
			else if (OUTPUT_FORMAT.RTF.equals(s))
				defContribs.add("net.sf.jasperreports.view.save.JRRtfSaveContributor");
			else if (OUTPUT_FORMAT.HTML.equals(s))
				defContribs.add("net.sf.jasperreports.view.save.JRHtmlSaveContributor");
			else if (OUTPUT_FORMAT.XLS_1_SHEET.equals(s))
				defContribs.add("net.sf.jasperreports.view.save.JRSingleSheetXlsSaveContributor");
			else if (OUTPUT_FORMAT.XLS.equals(s))
				defContribs.add("net.sf.jasperreports.view.save.JRMultipleSheetsXlsSaveContributor");
			else if (OUTPUT_FORMAT.CSV.equals(s))
				defContribs.add("net.sf.jasperreports.view.save.JRCsvSaveContributor");
			else if (OUTPUT_FORMAT.XML.equals(s))
				defContribs.add("net.sf.jasperreports.view.save.JRXmlSaveContributor");
			else if ("xml_embd_img".equals(s))
				defContribs.add("net.sf.jasperreports.view.save.JREmbeddedImagesXmlSaveContributor");
		}

		// the default save contributors
		String[] DEFAULT_CONTRIBUTORS = new String[defContribs.size()];
		DEFAULT_CONTRIBUTORS = defContribs.toArray(DEFAULT_CONTRIBUTORS);

		JRSaveContributor[] jrSaveContribs = new JRSaveContributor[DEFAULT_CONTRIBUTORS.length];
		for (int i = 0; i < DEFAULT_CONTRIBUTORS.length; i++) {
			try {
				Class<? extends JRSaveContributor> saveContribClass = (Class<? extends JRSaveContributor>) JRClassLoader.loadClassForName(DEFAULT_CONTRIBUTORS[i]);
				ResourceBundle jrViewerResBundel = ResourceBundle.getBundle(
						"net/sf/jasperreports/view/viewer", jrv.getLocale());
				Constructor<? extends JRSaveContributor>  constructor = saveContribClass.getConstructor(new Class[] { Locale.class, ResourceBundle.class });
				JRSaveContributor saveContrib = constructor.newInstance(new Object[] { jrv.getLocale(), jrViewerResBundel });
				jrSaveContribs[i] = saveContrib;
			} catch (Exception e) {
				Debug.error(e);
			}
		}

		jrv.setSaveContributors(jrSaveContribs);
	}

	/**
	 * Returns the list of export formats for the Jasper Viewer.
	 * 
	 * @return the list of export formats for the Jasper Viewer
	 */
	public String[] js_getViewerExportFormats() throws Exception {
		return viewerExportFormats;
	}

	/**
	 * 
	 * @param jasperPrint
	 * @return the Page, where the moved page(s) will be inserted
	 * @performs Iterates over the JasperPrint pages searching for the FIRST
	 *           appearence of the String:
	 *           "HIDDEN TEXT TO MARK THE INSERT PAGE"; and returning that
	 *           particular page the Pages to move will be placed in the right
	 *           order beginning at this page
	 */
	private static int getInsertPage(JasperPrint jasperPrint) {
		if (jasperPrint != null) {
			List pages = jasperPrint.getPages();
			if (pages != null && pages.size() > 0) {
				String key = "HIDDEN TEXT TO MARK THE INSERT PAGE";
				JRPrintPage page = null;
				JRPrintElement element = null;
				int i = pages.size() - 1;
				int k = 0;
				boolean isFoundPageIndex = false;
				for (k = 1; k <= i + 1;) {
					while (!isFoundPageIndex) {

						page = (JRPrintPage) pages.get(k);
						Collection<JRPrintElement> elements = page.getElements();

						if (elements != null && elements.size() > 0) {
							Iterator<JRPrintElement> it = elements.iterator();
							while (it.hasNext() && !isFoundPageIndex) {
								element = it.next();
								if (element instanceof JRPrintText) {
									if (key.equals(((JRPrintText) element)
											.getText())) {
										isFoundPageIndex = true;
										break;
									}
								}
							}
						}
						k++;

					}
					if (isFoundPageIndex) {
						break;
					}
				}

				if (isFoundPageIndex) {
					return k - 1;
				}

			}
		}
		return -1;
	}

	/**
	 * 
	 * @param JasperPrint
	 *            Object - jasperPrint
	 * @param int - insertPage (where to insert)
	 * @return JasperPrint Object
	 * @performs The Moving
	 */
	private static JasperPrint moveTableOfContents(JasperPrint jasperPrint, int insertPage) {
		if (jasperPrint != null) {
			List<JRPrintPage> pages = jasperPrint.getPages();
			if (pages != null && pages.size() > 0) {
				// finding WHAT to insert
				String key = "HIDDEN TEXT TO MARK THE BEGINNING OF THE TABEL OF CONTENTS";
				JRPrintPage page = null;
				JRPrintElement element = null;
				int i = pages.size() - 1;
				boolean isFound = false;
				while (i >= 0 && !isFound) {
					page = (JRPrintPage) pages.get(i);
					Collection<JRPrintElement> elements = page.getElements();
					if (elements != null && elements.size() > 0) {
						Iterator<JRPrintElement> it = elements.iterator();
						while (it.hasNext() && !isFound) {
							element = it.next();
							if (element instanceof JRPrintText) {
								if (key.equals(((JRPrintText) element)
										.getText())) {
									isFound = true;
									break;
								}
							}
						}
					}
					i--;
				}

				if (isFound) {
					for (int j = i + 1; j < pages.size(); j++) {
						jasperPrint.addPage(insertPage, jasperPrint.removePage(j));
						insertPage++;
					}
				}
			}
		}

		return jasperPrint;
	}

	public static byte[] getBytes(Object obj) throws java.io.IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		ObjectOutputStream oos = new ObjectOutputStream(bos);
		oos.writeObject(obj);
		oos.flush();
		oos.close();
		bos.close();
		byte[] data = bos.toByteArray();
		return data;
	}

	public String getPluginClientID() {
		return plugin.getIClientPluginAccess().getClientID();
	}

	private Map<String, Integer> createExporterParametersMap(Map p)
	{
		Map<String, Integer> aux = new HashMap<String, Integer>();
		if (p == null) return null;
		
		if (p.containsKey(EXPORTER_PARAMETERS.OFFSET_X)) aux.put(EXPORTER_PARAMETERS.OFFSET_X, new Integer(((Double)p.get(EXPORTER_PARAMETERS.OFFSET_X)).intValue()));
		if (p.containsKey(EXPORTER_PARAMETERS.OFFSET_Y)) aux.put(EXPORTER_PARAMETERS.OFFSET_Y, new Integer(((Double)p.get(EXPORTER_PARAMETERS.OFFSET_Y)).intValue()));
		
		if (p.containsKey(EXPORTER_PARAMETERS.PAGE_INDEX)) aux.put(EXPORTER_PARAMETERS.PAGE_INDEX, new Integer(((Double)p.get(EXPORTER_PARAMETERS.PAGE_INDEX)).intValue()));
		if (p.containsKey(EXPORTER_PARAMETERS.START_PAGE_INDEX)) aux.put(EXPORTER_PARAMETERS.START_PAGE_INDEX, new Integer(((Double)p.get(EXPORTER_PARAMETERS.START_PAGE_INDEX)).intValue()));
		if (p.containsKey(EXPORTER_PARAMETERS.END_PAGE_INDEX)) aux.put(EXPORTER_PARAMETERS.END_PAGE_INDEX, new Integer(((Double)p.get(EXPORTER_PARAMETERS.END_PAGE_INDEX)).intValue())); 

		return aux;
	}
	
}