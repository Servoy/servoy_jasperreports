/*
 * ============================================================================ GNU Lesser General Public License
 * ============================================================================
 * 
 * Servoy - Smart Technology For Smart Clients. Copyright © 1997-2012 Servoy BV http://www.servoy.com
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
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
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
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IReturnedTypesProvider;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * IScriptObject impl. For external library dependencies, see: http://www.jasperforge
 * .org/jaspersoft/opensource/business_intelligence/jasperreports /requirements.html
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

	JasperReportsProvider(JasperReportsPlugin p) throws Exception
	{
		plugin = p;
		relativeReportsDir = "";
		relativeExtraDirs = "";
	}

	public Class<?>[] getAllReturnedTypes()
	{
		return new Class[] { OUTPUT_FORMAT.class, JR_SVY_VIEWER_DISPLAY_MODE.class };
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
	 * @clonedesc js_runReport(Object,String,Object,String,Object,String,Boolean)
	 * @sampleas js_runReport(Object,String,Object,String,Object,String,Boolean)
	 * 
	 * @param source the server name or foundset to run the report on
	 * @param report the report file (relative to the reports directory)
	 * @param arg the output file (must specify an absolute path) or null if not needed
	 * @param type the output format; use the constants node for available output formats
	 * @param parameters a parameter map to be used when running the report
	 * 
	 * @return the generated reported as a byte array
	 */
	public byte[] js_runReport(Object source, String report, Object arg, String type, Object parameters) throws Exception
	{
		return js_runReport(source, report, arg, type, parameters, null);
	}

	/**
	 * @clonedesc js_runReport(Object,String,Object,String,Object,String,Boolean)
	 * @sampleas js_runReport(Object,String,Object,String,Object,String,Boolean)
	 * 
	 * @param source the server name or foundset to run the report on
	 * @param report the report file (relative to the reports directory)
	 * @param arg the output file (must specify an absolute path) or null if not needed
	 * @param type the output format; use the constants node for available output formats
	 * @param parameters a parameter map to be used when running the report
	 * @param localeString the string which specifies the locale
	 * 
	 * @return the generated reported as a byte array
	 */
	public byte[] js_runReport(Object source, String report, Object arg, String type, Object parameters, String localeString) throws Exception
	{
		return js_runReport(source, report, arg, type, parameters, localeString, Boolean.FALSE);
	}

	/**
	 * This method runs a specified (client) report according to the output format, parameters and locale. If using a
	 * table of contents and if needed, the table of contents can be moved to a specified page. Please refer to the
	 * sample code for more details.
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
	 * // as the locale argument. For example: 'en' or 'es' or 'nl' 
	 * // When the locale argument is not specified, the report will be in the current langauge of the Client 
	 * // i18n keys of Servoy can be used inside Jasper Reports using the $R{i18n-key} notation 
	 * //plugins.jasperPluginRMI.runReport(myServer,'myCustomerReport.jasper',null,OUTPUT_FORMAT.VIEW,{pcustomerid: forms.customers.customer_id},'nl');
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
	 * // Supported output formats are: xhtml, html, pdf, excel( or xls), xls_1_sheet (1 page per sheet), ods, rtf, txt, csv, odt, docx, jrprint and xml. 
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
	 * //params["EXPORTER_PARAMETER:net.sf.jasperreports.engine.export.JRPdfExporterParameter.METADATA_TITLE"] =  "Test title"; 
	 * //params["EXPORTER_PARAMETER:net.sf.jasperreports.engine.export.JRPdfExporterParameter.METADATA_AUTHOR" ] = "Test Author";
	 * //var r = plugins.jasperPluginRMI.runReport("myServer","someReport.jrxml","path/to/someReportExported.pdf",OUTPUT_FORMAT.PDF,params);
	 * 
	 * @param source the server name or foundset to run the report on
	 * @param report the report file (relative to the reports directory)
	 * @param arg the output file (must specify an absolute path) or null if not needed
	 * @param type the output format; use the constants node for available output formats
	 * @param parameters a parameter map to be used when running the report
	 * @param localeString the string which specifies the locale
	 * @param moveTableOfContent true in order to move the table of contents, false otherwise
	 * 
	 * @return the generated reported as a byte array
	 */
	public byte[] js_runReport(Object source, String report, Object arg, String type, Object parameters, String localeString, Boolean moveTableOfContent) throws Exception
	{
		return runReport(source, report, arg, type, parameters, localeString, Boolean.TRUE.equals(moveTableOfContent), false);
	}

	// public, but not scriptable - just for the corresponding Bean's usage
	public byte[] runReportForBean(Object source, String report, Object arg, String type, Object parameters, String localeString, Boolean moveTableOfContent) throws Exception
	{
		return runReport(source, report, arg, type, parameters, localeString, Boolean.TRUE.equals(moveTableOfContent), true);
	}

	private byte[] runReport(Object source, String report, Object arg, String exportFormat, Object parameters, String localeString, boolean moveTableOfContent, boolean returnJustJasperPrint) throws Exception
	{

		// Check if the directory.jasper.report setting has not yet been set.
		String pluginReportsDirectory = plugin.getJasperReportsDirectory();
		if (pluginReportsDirectory == null || (pluginReportsDirectory != null && ("").equals(pluginReportsDirectory.trim())))
		{
			String noPluginDirMsg = "Your jasper.report.directory setting has not been set.\nReport running will abort.";
			Debug.error(noPluginDirMsg);
			throw new Exception(noPluginDirMsg);
		}

		String type = exportFormat.toLowerCase();

		// unwrapping of arguments
		source = JSArgumentsUnwrap.unwrapJSObject(source, plugin.getIClientPluginAccess());
		Map<String, Object> params = (Map<String, Object>) JSArgumentsUnwrap.unwrapJSObject(parameters, plugin.getIClientPluginAccess());
		if (params == null) params = new HashMap<String, Object>();

		boolean showPrintDialog = false;
		String file = "";
		boolean nooutput = false;
		if (arg instanceof String)
		{
			file = arg.toString();
		}
		else if (arg instanceof Boolean)
		{
			showPrintDialog = Utils.getAsBoolean(arg);
		}
		else
		{
			if (arg != null) file = arg.toString(); // To support passing in a
													// JSFile object
			else nooutput = true;
		}

		if (source == null)
		{
			throw new Exception("No data source <null> has been provided");
		}
		Debug.trace("JasperTrace: JasperReport initialize");

		IJasperReportsService jasperReportService = plugin.connectJasperService();

		// check out type of data source (and how to run reports)
		IJasperReportRunner jasperReportRunner;
		String txid = null;
		if (source instanceof String)
		{
			txid = plugin.getIClientPluginAccess().getTransactionID((String) source);
			jasperReportRunner = jasperReportService; // run report remote
		}
		else if (source instanceof JRDataSource)
		{
			jasperReportRunner = new JasperReportRunner(jasperReportService); // run
																				// reports
																				// in
																				// client
		}
		else
		{
			throw new Exception("Unsupported data source: " + source.getClass());
		}

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

				// Fill the report and get the JasperPrint instance.
				// Also modify the JasperPrint in case you want to move the
				// table of contents.
				JasperPrint jp = jasperReportRunner.getJasperPrint(plugin.getIClientPluginAccess().getClientID(), source, txid, report, params, relativeReportsDir, relativeExtraDirs);

				if (moveTableOfContent)
				{
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

				Map<String, Object> exporterParams = createExporterParametersMap(params);
				byte[] jsp = null;

				// 1. WebClient
				if (applicationType == IClientPluginAccess.WEB_CLIENT)
				{

					String mimeType = "application/octet-stream";
					if (type.equals(OUTPUT_FORMAT.VIEW) || type.equals(OUTPUT_FORMAT.PRINT))
					{
						mimeType = JasperReportsWebViewer.MIME_TYPE_PDF;
					}
					else if (type.equals(OUTPUT_FORMAT.PDF))
					{
						mimeType = JasperReportsWebViewer.MIME_TYPE_PDF;
					}
					else if (type.equals(OUTPUT_FORMAT.CSV))
					{
						mimeType = JasperReportsWebViewer.MIME_TYPE_CSV;
					}
					else if (type.equals(OUTPUT_FORMAT.DOCX))
					{
						mimeType = JasperReportsWebViewer.MIME_TYPE_DOCX;
					}
					else if (type.equals(OUTPUT_FORMAT.EXCEL))
					{
						mimeType = JasperReportsWebViewer.MIME_TYPE_XLS;
					}
					else if (type.equals(OUTPUT_FORMAT.HTML))
					{
						mimeType = JasperReportsWebViewer.MIME_TYPE_HTML;
					}
					else if (type.equals(OUTPUT_FORMAT.ODS))
					{
						mimeType = JasperReportsWebViewer.MIME_TYPE_ODS;
					}
					else if (type.equals(OUTPUT_FORMAT.ODT))
					{
						mimeType = JasperReportsWebViewer.MIME_TYPE_ODT;
					}
					else if (type.equals(OUTPUT_FORMAT.RTF))
					{
						mimeType = JasperReportsWebViewer.MIME_TYPE_RTF;
					}
					else if (type.equals(OUTPUT_FORMAT.TXT))
					{
						mimeType = JasperReportsWebViewer.MIME_TYPE_TXT;
					}
					else if (type.equals(OUTPUT_FORMAT.XHTML))
					{
						mimeType = JasperReportsWebViewer.MIME_TYPE_XHTML;
					}
					else if (type.equals(OUTPUT_FORMAT.XLS))
					{
						mimeType = JasperReportsWebViewer.MIME_TYPE_XLS;
					}
					else if (type.equals(OUTPUT_FORMAT.XLS_1_SHEET))
					{
						mimeType = JasperReportsWebViewer.MIME_TYPE_XLS;
					}
					else if (type.equals(OUTPUT_FORMAT.XML))
					{
						mimeType = JasperReportsWebViewer.MIME_TYPE_XML;
					}
					else
					{
						throw new Exception("JasperTrace: Jasper Exception: Unsupported web client output format");
					}

					if (type.equals(OUTPUT_FORMAT.VIEW) || type.equals(OUTPUT_FORMAT.PRINT))
					{
						jsp = JasperReportRunner.getJasperBytes("pdf", jp, jasperReportService.getCheckedExtraDirectoriesRelativePath(relativeExtraDirs), exporterParams);
						JasperReportsWebViewer.show(plugin.getIClientPluginAccess(), jsp, file, "pdf", mimeType);
					}
					else
					{
						jsp = JasperReportRunner.getJasperBytes(type, jp, jasperReportService.getCheckedExtraDirectoriesRelativePath(relativeExtraDirs), exporterParams);
						if (nooutput)
						{
							JasperReportsWebViewer.show(plugin.getIClientPluginAccess(), jsp, file, type, mimeType);
						}
						else
						{
							saveByteArrayToFile(file, jsp);
						}
					}
				}

				// 2. SmartClient
				else
				{

					// a. SmartClient "view"
					if (type.toLowerCase().startsWith("view"))
					{
						CustomizedJasperViewer jasperviewer;

						if (localeString != null)
						{
							jasperviewer = new CustomizedJasperViewer(jp, false, new Locale(localeString));
						}
						else
						{
							jasperviewer = new CustomizedJasperViewer(jp, false);
						}

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

						if (jp != null && jp.getPages() != null && jp.getPages().size() > 0)
						{
							jasperviewer.setVisible(true);
							jasperviewer.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
						}
						else
						{
							jasperviewer.dispose();
						}
					}

					// b. SmartClient "print"
					// Printing only supported in SC. Printing in WC handled
					// previously though PDF push.
					else if (type.toLowerCase().startsWith("print"))
					{
						// Shows print dialog before printing (if arg is
						// true/"true")
						if (showPrintDialog || file.equalsIgnoreCase("true") || file == null)
						{
							JasperPrintManager.printReport(jp, true);
						}
						// Skips print dialog (if arg is false/"false" or null)
						else if ((!showPrintDialog && file.equals("")) // also
																		// equivalent
																		// to
																		// arg
																		// ==
																		// null
								|| file.equalsIgnoreCase("false") || file.equalsIgnoreCase("default"))
						{
							JasperPrintManager.printReport(jp, false);
						}
						// Assumes parameter file contains a printerName
						else
						{
							Debug.trace("JasperTrace: printer: " + file);
							PrinterJob printJob = PrinterJob.getPrinterJob();
							// fix for bug ID 6255588 from Sun bug database
							initPrinterJobFields(printJob);
							/*
							 * or another workaround try { printerJob.setPrintService (printerJob.getPrintService()); } catch (PrinterException e) {}
							 */

							if (setPrintService(printJob, file))
							{
								JasperReportsPrinter.printPages(jp, printJob);
							}
							else
							{
								Debug.trace("JasperTrace: unable to specify printer: " + file);
							}
						}
						// 3. SmartClient other output formats
					}
					else
					{
						if (!nooutput)
						{
							String fileLocation = new File(file).getParent();
							exporterParams.put("REPORT_FILE_LOCATION", fileLocation);
						}
						jsp = jasperReportService.getJasperBytes(plugin.getIClientPluginAccess().getClientID(), type, jp, relativeExtraDirs, exporterParams);
						if (!nooutput)
						{
							saveByteArrayToFile(file, jsp);
						}
					}
				}
				Debug.trace("JasperTrace: JasperReport finished");
				return jsp;
			}
			catch (Exception e)
			{
				Debug.error(e);
				throw new Exception(e.getMessage());
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
	 * By default the value is read from the adim page Server Plugins, the directory.jasper.report property. A client is
	 * only able to set a path relative to the server report directory. If the client modifies this property, its value
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
	public JSDataSet js_getReportParameters(String report) throws Exception
	{

		JSDataSet ds = null;

		try
		{
			Debug.trace("JasperTrace: getReportParameters starting");
			ds = plugin.connectJasperService().getReportParameters(plugin.getIClientPluginAccess().getClientID(), report, relativeReportsDir);
			Debug.trace("JasperTrace: getReportParameters finished");
		}
		catch (Exception e)
		{
			Debug.error(e);
			throw new Exception(e.getMessage());
		}

		return ds;
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
		return "4.0.0 b1";

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
	public static void setViewerSaveContributors(JRViewer jrv, String[] saveContributors)
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

		jrv.setSaveContributors(jrSaveContribs);
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
	 * 
	 * @param jasperPrint
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
									if (key.equals(((JRPrintText) element).getText()))
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
	 * 
	 * @param JasperPrint Object - jasperPrint
	 * @param int - insertPage (where to insert)
	 * @return JasperPrint Object
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
								if (key.equals(((JRPrintText) element).getText()))
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

	public String getPluginClientID()
	{
		return plugin.getIClientPluginAccess().getClientID();
	}

	private Map<String, Object> createExporterParametersMap(Map<String, Object> parameters)
	{
		if (parameters == null) return null;

		Map<String, Object> aux = new HashMap<String, Object>();

		// leaving these in just for legacy purposes
		if (parameters.containsKey(EXPORTER_PARAMETERS.OFFSET_X)) aux.put(EXPORTER_PARAMETERS.OFFSET_X, new Integer(((Double) parameters.get(EXPORTER_PARAMETERS.OFFSET_X)).intValue()));
		if (parameters.containsKey(EXPORTER_PARAMETERS.OFFSET_Y)) aux.put(EXPORTER_PARAMETERS.OFFSET_Y, new Integer(((Double) parameters.get(EXPORTER_PARAMETERS.OFFSET_Y)).intValue()));
		if (parameters.containsKey(EXPORTER_PARAMETERS.PAGE_INDEX)) aux.put(EXPORTER_PARAMETERS.PAGE_INDEX, new Integer(((Double) parameters.get(EXPORTER_PARAMETERS.PAGE_INDEX)).intValue()));
		if (parameters.containsKey(EXPORTER_PARAMETERS.START_PAGE_INDEX)) aux.put(EXPORTER_PARAMETERS.START_PAGE_INDEX, new Integer(((Double) parameters.get(EXPORTER_PARAMETERS.START_PAGE_INDEX)).intValue()));
		if (parameters.containsKey(EXPORTER_PARAMETERS.END_PAGE_INDEX)) aux.put(EXPORTER_PARAMETERS.END_PAGE_INDEX, new Integer(((Double) parameters.get(EXPORTER_PARAMETERS.END_PAGE_INDEX)).intValue()));

		// copy over only the exporter parameters
		for (Map.Entry<String, Object> entry : parameters.entrySet())
		{
			if (entry.getKey().startsWith("EXPORTER_PARAMETER:")) aux.put(entry.getKey(), entry.getValue());
		}

		return aux;
	}

}