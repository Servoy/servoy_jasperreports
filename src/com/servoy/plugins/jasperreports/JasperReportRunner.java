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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import net.sf.jasperreports.engine.DefaultJasperReportsContext;
import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JRVirtualizer;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRCsvDataSource;
import net.sf.jasperreports.engine.data.JRXmlDataSource;
import net.sf.jasperreports.engine.export.FileHtmlResourceHandler;
import net.sf.jasperreports.engine.export.HtmlExporter;
import net.sf.jasperreports.engine.export.HtmlResourceHandler;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRCsvMetadataExporter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.JRTextExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXmlExporter;
import net.sf.jasperreports.engine.export.JsonExporter;
import net.sf.jasperreports.engine.export.JsonMetadataExporter;
import net.sf.jasperreports.engine.export.oasis.JROdsExporter;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.export.ooxml.JRXlsxExporter;
import net.sf.jasperreports.engine.fill.JRAbstractLRUVirtualizer;
import net.sf.jasperreports.engine.fill.JRFileVirtualizer;
import net.sf.jasperreports.engine.fill.JRGzipVirtualizer;
import net.sf.jasperreports.engine.fill.JRSwapFileVirtualizer;
import net.sf.jasperreports.engine.util.JRSaver;
import net.sf.jasperreports.engine.util.JRSwapFile;
import net.sf.jasperreports.engine.util.LocalJasperReportsContext;
import net.sf.jasperreports.engine.util.SimpleFileResolver;
import net.sf.jasperreports.export.Exporter;
import net.sf.jasperreports.export.ExporterInput;
import net.sf.jasperreports.export.ExporterOutput;
import net.sf.jasperreports.export.SimpleExporterInput;
import net.sf.jasperreports.export.SimpleHtmlExporterOutput;
import net.sf.jasperreports.export.SimpleJsonExporterOutput;
import net.sf.jasperreports.export.SimpleOutputStreamExporterOutput;
import net.sf.jasperreports.export.SimpleReportExportConfiguration;
import net.sf.jasperreports.export.SimpleTextReportConfiguration;
import net.sf.jasperreports.export.SimpleWriterExporterOutput;
import net.sf.jasperreports.export.SimpleXlsReportConfiguration;
import net.sf.jasperreports.export.SimpleXlsxReportConfiguration;
import net.sf.jasperreports.export.SimpleXmlExporterOutput;

import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;

public class JasperReportRunner implements IJasperReportRunner
{
	private static final int TEXT_PAGE_WIDTH_IN_CHARS = 120;
	private static final int TEXT_PAGE_HEIGHT_IN_CHARS = 60;

	private static final String VIRTUALIZER_FILE = "file";
	private static final String VIRTUALIZER_SWAP_FILE = "swapFile";
	private static final String VIRTUALIZER_GZIP = "gZip";

	private final IJasperReportsService jasperReportsService;
	
	private static final List <VirtualizerState>virtualizers = new ArrayList<VirtualizerState>();
	
	public JasperReportRunner(IJasperReportsService jasperReportsService)
	{
		this.jasperReportsService = jasperReportsService;
	}
	
	public JasperPrintResult getJasperPrint(String clientID, String inputType, Object inputSource, String inputOptions, String txid, String reportName, Map<String, Object> parameters, String repdir, String extraDirs) throws RemoteException, Exception
	{
		if (inputSource == null)
		{
			throw new IllegalArgumentException("no data source");
		}

		if (reportName == null)
		{
			throw new IllegalArgumentException("No jasperReport <null> has been found or loaded");
		}

		Debug.trace("JasperTrace: Directory: " + repdir);

		JasperReport jasperReport = jasperReportsService.getJasperReport(clientID, reportName, repdir);

		return getJasperPrint(inputType, null, inputSource, jasperReport, parameters, jasperReportsService.getCheckedRelativeReportsPath(repdir), jasperReportsService.getCheckedExtraDirectoriesRelativePath(extraDirs));
	}
	
	@Override
	public void cleanupJasperPrint(GarbageMan garbageMan) {
		garbageMan.cleanup();
	}
	
	/**
	 * Check if the type matches a text of char based export type (i.e. HTML, RTF, CSV, TXT, XML export)
	 * 
	 * @param type the indicated export type
	 * @return true if the indicated export type is a text or char based export type, false otherwise
	 */
	private static boolean isTextOrCharBasedExport(String type) {
		return (type.equalsIgnoreCase(OUTPUT_FORMAT.HTML) || 
				type.equalsIgnoreCase(OUTPUT_FORMAT.XHTML) ||
				type.equalsIgnoreCase(OUTPUT_FORMAT.RTF) ||
				type.equalsIgnoreCase(OUTPUT_FORMAT.CSV) ||
				type.equalsIgnoreCase(OUTPUT_FORMAT.TXT) ||
				type.equalsIgnoreCase(OUTPUT_FORMAT.XML) ||
				type.equalsIgnoreCase(OUTPUT_FORMAT.JSON_METADATA) ||
				type.equalsIgnoreCase(OUTPUT_FORMAT.CSV_METADATA));
	}
	
	/**
	 * Check if the type matches a binary export type (i.e. PDF and XLS export)
	 * 
	 * @param type the indicated export type
	 * @return true if the indicated export type is a binary export type, false otherwise
	 */
	private static boolean isBinaryExport(String type) {
		return (type.equalsIgnoreCase(OUTPUT_FORMAT.PDF) || type.equalsIgnoreCase(OUTPUT_FORMAT.XLS) ||
				type.equalsIgnoreCase(OUTPUT_FORMAT.XLSX) || type.equalsIgnoreCase(OUTPUT_FORMAT.XLS_1_SHEET) ||
				type.equalsIgnoreCase(OUTPUT_FORMAT.EXCEL)|| type.equalsIgnoreCase(OUTPUT_FORMAT.DOCX) ||
				type.equalsIgnoreCase(OUTPUT_FORMAT.ODS) || type.equalsIgnoreCase(OUTPUT_FORMAT.ODT)); 
	}
	
//	//if (Graphics2D and Java Print Service exporters) { // export directly to graphic devices
//	private static boolean isDirectGraphicExport(String type) {
//		// should not get here: unsupported currently, in this design
//		return (type.equalsIgnoreCase(OUTPUT_FORMAT.PRINT)); 
//	}
//	
//	// jr print / view (jr print should have been handled already)
//	private static boolean isRawPrint(String type) {
//		// for view, we should not get here: unsupported currently, in this design
//		return type.equalsIgnoreCase(OUTPUT_FORMAT.JRPRINT) /*|| type.equalsIgnoreCase(OUTPUT_FORMAT.VIEW)*/;
//	}
	
	/**
	 * This is the main method which does the exporting of the report. 
	 * 
	 * @param type the export type indicated in the runReport call
	 * @param jasperPrint the filled jasper print
	 * @param extraDirs a list of extra directories
	 * @param exporterParameters a list of parameters for the export process 
	 * @return an array of bytes representing the exported report
	 * @throws IOException
	 * @throws JRException
	 */
	@SuppressWarnings("unchecked")
	public static byte[] getJasperBytes(String type, JasperPrint jasperPrint, String extraDirs, Map<String, Object> exporterParameters) throws IOException, JRException
	{
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		
		// exporting the report
		if (type.equalsIgnoreCase(OUTPUT_FORMAT.JRPRINT))
		{
			JRSaver.saveObject(jasperPrint, baos);
			return baos.toByteArray();
		}
		
		Exporter exporter;
		SimpleReportExportConfiguration reportExportConfiguration = null;
		ExporterInput exporterInput = null;
		ExporterOutput exporterOutput;

		/**
		 *  Exporter type and Exporter Output
		 *  1. create the exporter type
		 *  2. set the exporter output
		 */
		// html/xhtml/rtf/csv/txt/xml
		if (isTextOrCharBasedExport(type)) 
		{
			exporterOutput = new SimpleWriterExporterOutput(baos,"UTF-8");
			if (type.equalsIgnoreCase(OUTPUT_FORMAT.HTML) || type.equalsIgnoreCase(OUTPUT_FORMAT.XHTML)) 
			{
				exporter = new HtmlExporter();
				exporterOutput = new SimpleHtmlExporterOutput(baos,"UTF-8");
				String location = (exporterParameters != null ? adjustFileUnix((String) exporterParameters.get("REPORT_FILE_LOCATION")) : null);
				if (location != null)
				{
					location = (location.endsWith("/") ? location : location + "/");
					File outFile = new File(location + "/" + jasperPrint.getName() + ".html_files/");
					// we need a resource handler with both parent folder and file pattern 
					// because we are exporting to a byte array output stream and not to a file, we must specify the file pattern needed for image location
					// see net.sf.jasperreports.engine.export.HtmlExporter.writeImage(JRPrintImage, TableCell), where the "img src" is added
					HtmlResourceHandler imageHandler = new FileHtmlResourceHandler(outFile, outFile.getAbsolutePath() + "/{0}");
					((SimpleHtmlExporterOutput)exporterOutput).setImageHandler(imageHandler);
					//IS_USING_IMAGES_TO_ALIGN - by default it should be true anyway
				}
			} 
			else if (type.equalsIgnoreCase(OUTPUT_FORMAT.RTF)) 
			{
				exporter = new JRRtfExporter();
			} 
			else if (type.equalsIgnoreCase(OUTPUT_FORMAT.CSV)) 
			{
				exporter = new JRCsvExporter();
			} 
			else if (type.equalsIgnoreCase(OUTPUT_FORMAT.TXT)) 
			{
				exporter = new JRTextExporter();
				reportExportConfiguration = new SimpleTextReportConfiguration();
				((SimpleTextReportConfiguration)reportExportConfiguration).setPageWidthInChars(new Integer(TEXT_PAGE_WIDTH_IN_CHARS));
				((SimpleTextReportConfiguration)reportExportConfiguration).setPageHeightInChars( new Integer(TEXT_PAGE_HEIGHT_IN_CHARS));
				exporter.setConfiguration(reportExportConfiguration);
			} 
			else if (type.equalsIgnoreCase(OUTPUT_FORMAT.XML)) 
			{
				exporter = new JRXmlExporter();
				exporterOutput = new SimpleXmlExporterOutput(baos,"UTF-8");
				((SimpleXmlExporterOutput)exporterOutput).setEmbeddingImages(Boolean.TRUE);
			}
			else if (type.equalsIgnoreCase(OUTPUT_FORMAT.JSON_METADATA)) 
			{
				exporter = new JsonMetadataExporter();
			}
			else if (type.equalsIgnoreCase(OUTPUT_FORMAT.CSV_METADATA)) 
			{
				exporter = new JRCsvMetadataExporter();
			}
			// should not get here
			else throw new IllegalStateException("unexpected text based report type " + type);
		} 
		else if (isBinaryExport(type)) 
		{
            // pdf, xls/xlsx/excel, docx, ods, odt 
			if (type.equalsIgnoreCase(OUTPUT_FORMAT.PDF))
			{
				exporter = new JRPdfExporter();
			}
			else if (type.equalsIgnoreCase(OUTPUT_FORMAT.ODT))
			{
				exporter = new JROdtExporter();
			}
			else if (type.equalsIgnoreCase(OUTPUT_FORMAT.ODS))
			{
				exporter = new JROdsExporter();
			}
			else if (type.equalsIgnoreCase(OUTPUT_FORMAT.EXCEL) || type.equalsIgnoreCase(OUTPUT_FORMAT.XLS) || type.equalsIgnoreCase(OUTPUT_FORMAT.XLS_1_SHEET) || type.equalsIgnoreCase(OUTPUT_FORMAT.XLSX))
			{
				if (!type.equalsIgnoreCase(OUTPUT_FORMAT.XLSX)) 
				{
					exporter = new JRXlsExporter();
					reportExportConfiguration = new SimpleXlsReportConfiguration();
					if (type.equalsIgnoreCase(OUTPUT_FORMAT.XLS_1_SHEET))
					{
						((SimpleXlsReportConfiguration)reportExportConfiguration).setOnePagePerSheet(Boolean.TRUE);
					}
					else
					{
						String maxRowsPerSheet = jasperPrint.getProperty("MAXIMUM_ROWS_PER_SHEET");
						((SimpleXlsReportConfiguration)reportExportConfiguration).setMaxRowsPerSheet(Integer.valueOf(maxRowsPerSheet));
					}
					((SimpleXlsReportConfiguration)reportExportConfiguration).setDetectCellType(Boolean.TRUE);
					((SimpleXlsReportConfiguration)reportExportConfiguration).setWhitePageBackground(Boolean.FALSE);
					((SimpleXlsReportConfiguration)reportExportConfiguration).setRemoveEmptySpaceBetweenRows(Boolean.TRUE);
				}
				else
				{
					exporter = new JRXlsxExporter();
					reportExportConfiguration = new SimpleXlsxReportConfiguration();
					((SimpleXlsxReportConfiguration)reportExportConfiguration).setOnePagePerSheet(Boolean.FALSE);
					String maxRowsPerSheet = jasperPrint.getProperty("MAXIMUM_ROWS_PER_SHEET");
					((SimpleXlsxReportConfiguration)reportExportConfiguration).setMaxRowsPerSheet(Integer.valueOf(maxRowsPerSheet));
					((SimpleXlsxReportConfiguration)reportExportConfiguration).setDetectCellType(Boolean.TRUE);
					((SimpleXlsxReportConfiguration)reportExportConfiguration).setWhitePageBackground(Boolean.FALSE);
					((SimpleXlsxReportConfiguration)reportExportConfiguration).setRemoveEmptySpaceBetweenRows(Boolean.TRUE);
				}
			}
			else if (type.equalsIgnoreCase(OUTPUT_FORMAT.DOCX))
			{
				exporter = new JRDocxExporter();
			}
			// should not get here
			else throw new IllegalStateException("unexpected binary report type " + type);
			
			exporterOutput = new SimpleOutputStreamExporterOutput(baos);
		} 
		else 
		{
			// if (isDirectGraphicExport(type) || isRawPrint(type)) 
			// 	Graphics2DExporterOutput: exporterOutput = new SimpleGraphics2DExporterOutput();
			//  raw print: exporterOutput = new SimpleWriterExporterOutput(baos,"UTF-8");
			throw new JRException("Unsupported output format: " + type);
		}

		// single point of setting the exporter Output
		exporter.setExporterOutput(exporterOutput);
		

		/**
		 * Export Input: set export input in the same way for all exporters for now
		 */
		//exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		if (exporterInput == null)
		{
			exporterInput = new SimpleExporterInput(jasperPrint);
		}
		exporter.setExporterInput(exporterInput); 
				
		
		/**
		 * EXTRA export configuration parameters
		 */
		// add all received JRExporterParameters to the exporter
		if (reportExportConfiguration == null)
		{
			reportExportConfiguration = new SimpleReportExportConfiguration();
		}
		if (exporterParameters != null)
		{
			// leaving these 5 for legacy purposes
			if (exporterParameters.containsKey(EXPORTER_PARAMETERS.PAGE_INDEX)) 
			{
				reportExportConfiguration.setPageIndex((Integer)exporterParameters.get(EXPORTER_PARAMETERS.PAGE_INDEX));
			}
			if (exporterParameters.containsKey(EXPORTER_PARAMETERS.START_PAGE_INDEX)) 
			{
				reportExportConfiguration.setStartPageIndex((Integer)exporterParameters.get(EXPORTER_PARAMETERS.START_PAGE_INDEX));
			}
			if (exporterParameters.containsKey(EXPORTER_PARAMETERS.END_PAGE_INDEX)) 
			{
				reportExportConfiguration.setEndPageIndex((Integer)exporterParameters.get(EXPORTER_PARAMETERS.END_PAGE_INDEX));
			}
			if (exporterParameters.containsKey(EXPORTER_PARAMETERS.OFFSET_X)) 
			{
				reportExportConfiguration.setOffsetX((Integer)exporterParameters.get(EXPORTER_PARAMETERS.OFFSET_X));
			}
			if (exporterParameters.containsKey(EXPORTER_PARAMETERS.OFFSET_Y))
			{
				reportExportConfiguration.setOffsetY((Integer)exporterParameters.get(EXPORTER_PARAMETERS.OFFSET_Y));
			}

			// add more report properties (for different exporter types)
			// the report properties must have the EXPORTER_PARAMETER prefix specified in the runReport call
			for (Map.Entry<String, Object> entry : exporterParameters.entrySet())
			{
				String key = entry.getKey();
				Object value = entry.getValue();
				if (key.startsWith("EXPORTER_PARAMETER:"))
				{
					String propertyName = key.substring(key.lastIndexOf(":") + 1);
					if (!propertyName.trim().isEmpty())
					{
						// add the param to the properties map
						jasperPrint.setProperty(propertyName, (String) value);
					}
				}
			}
		}

		ArrayList<String> al = JasperReportsUtil.StringToArrayList(extraDirs);
		if (al != null)
		{
			ArrayList<File> dirList = new ArrayList<File>();
			String aux = null;
			for (String st : al)
			{
				aux = adjustFileUnix(st);
				dirList.add(new File(aux));
			}
			LocalJasperReportsContext localJasperReportsContext = new LocalJasperReportsContext(DefaultJasperReportsContext.getInstance());
			localJasperReportsContext.setFileResolver(new SimpleFileResolver(dirList));
		}

		exporter.exportReport();

		return baos.toByteArray();
	}

	/**
	 * This function returns the page oriented document, using the provided compiled report template.
	 * The result of this function can be viewed, printed, exported.
	 * 
	 * @param inputType the type of the datasource, as one of the constants in INPUT_TYPE 
	 * @param conn the database specific connection
	 * @param dataSource the jasperreports speific datasource object
	 * @param jasperReport the compiled report template object
	 * @param parameters the parameters map for report filling
	 * @param repdir the relative reports directory
	 * @param extraDirs the relative reports directory
	 * @return the result as a JasperPrint, which can be viewed, printed, exported
	 * 
	 * @throws JRException
	 */
	public static JasperPrintResult getJasperPrint(String inputType, Connection conn, Object dataSource, JasperReport jasperReport, Map<String, Object> parameters, String repdir, String extraDirs) throws JRException
	{
		//  TODO: possible fixes for 'xpath2' query language usage
//		jasperReport.setProperty(
//				"net.sf.jasperreports.query.executer.factory.plsql",
//				"com.jaspersoft.jrx.query.PlSqlQueryExecuterFactory");
//
//		// Maybe this too, but not positive
//		JRProperties.setProperty(
//				JRQueryExecuterFactory.QUERY_EXECUTER_FACTORY_PREFIX + "plsql",
//				"com.jaspersoft.jrx.query.PlSqlQueryExecuterFactory");
		
		Connection connection = conn;
		
		// input = connection || jrdatasource
		if (dataSource == null) {
			throw new IllegalArgumentException("No model or db connection <null> has been found or loaded");
		}
		
		// client - fill (the compiled) report
		Debug.trace("JasperTrace: Directory: " + repdir);

		// make directory unix style
		String jasperDirectory = adjustFileUnix(repdir);

		if (parameters == null) parameters = new HashMap<String, Object>();

		parameters.put("report_directory", jasperDirectory);
		if (!jasperDirectory.endsWith("/")) jasperDirectory = jasperDirectory + '/';

		boolean shouldSetSubReportDir = true;
		String subReportDir = (String) parameters.get("SUBREPORT_DIR");
		if (subReportDir == null || subReportDir.equals(""))
		{
			for(JRParameter p : jasperReport.getParameters())
			{
				if("SUBREPORT_DIR".equals(p.getName()))
				{
					shouldSetSubReportDir = p.getDefaultValueExpression() == null;
					break;
				}
			}	
			// if the subreport directory is not set
			if(shouldSetSubReportDir) subReportDir = jasperDirectory;
		}
		else
		{
			// if the path is relative
			if (!(new File(subReportDir)).isAbsolute())
			{
				subReportDir = adjustFileUnix(jasperDirectory + subReportDir);
			}
			else
			{
				//SUBREPORT_DIR value is an absolute path - this is not allowed
				throw new JRException("SUBREPORT_DIR cannot be specified as an absolute location; please use a location relative to the reports directory");
			}
		}
		if(shouldSetSubReportDir) parameters.put("SUBREPORT_DIR", subReportDir);

		Debug.trace("JasperTrace: Extra Directories: " + extraDirs);
		ArrayList<String> al = JasperReportsUtil.StringToArrayList(extraDirs);
		if (al != null)
		{
			String aux = null;
			for (int x = 0; x < al.size(); x++)
			{
				aux = adjustFileUnix(al.get(x));
				parameters.put("extra_directory_" + (x + 1), aux);
			}
		}

		String virtualizerType = null;
		String pageOutDir = System.getProperty("java.io.tmpdir");
		Number maxRowsPerSheet = null;

		for (Iterator<String> iter = parameters.keySet().iterator(); iter.hasNext();)
		{
			Object key = iter.next();
			Object value = parameters.get(key);
			if (key.equals("VIRTUALIZER_TYPE"))
			{
				virtualizerType = (String) value;
				iter.remove();
			}
			if (key.equals("PAGE_OUT_DIR") && value != null)
			{
				pageOutDir = (String) value;
				iter.remove();
			}
			if (key.equals("MAXIMUM_ROWS_PER_SHEET") && value != null)
			{
				maxRowsPerSheet = (Number) value;
				iter.remove();
			}
		}

		// explicit check if pageOutDir exists
		String virtErrMsg = null;
		File testDir = new File(pageOutDir);
		if (!testDir.exists())
		{
			virtErrMsg = "The indicated PAGE_OUT_DIR path: '" + pageOutDir + "' is not a valid path. This folder is used, when" +
				" the VIRTUALIZER_TYPE is set to 'file' or 'swapFile' in the params of the 'runReport' api call, as a temporary" +
				" folder - so it needs to exist and be writable. If PAGE_OUT_DIR is not set in the params, it uses as default the temp folder" +
				" of the JVM (java.io.tmpdir).";
			Debug.error(virtErrMsg);
			throw new JRException(virtErrMsg);
		}
		else testDir = null;

		// cleanup old virtualizers
		cleanupVirtualizers(null);
		
		JRAbstractLRUVirtualizer virtualizer = null;
		
		if (VIRTUALIZER_FILE.equalsIgnoreCase(virtualizerType))
		{
			virtualizer = new JRFileVirtualizer(2, pageOutDir);
		}
		else if (VIRTUALIZER_SWAP_FILE.equalsIgnoreCase(virtualizerType))
		{
			JRSwapFile swapFile = new JRSwapFile(pageOutDir, 1024, 1024);
			virtualizer = new JRSwapFileVirtualizer(2, swapFile, true);
		}
		else if (VIRTUALIZER_GZIP.equalsIgnoreCase(virtualizerType))
		{
			virtualizer = new JRGzipVirtualizer(2);
		}
		else if (virtualizerType != null)
		{
			// virtualizer type has been specified but is not of a supported type
			virtErrMsg = "The virtualizer type specified '" + virtualizerType + "' is not supported.";
			Debug.error(virtErrMsg);
			throw new JRException(virtErrMsg);
		}
		if (virtualizer != null)
		{
			parameters.put(JRParameter.REPORT_VIRTUALIZER, virtualizer);
		}

		JRParameter[] params = jasperReport.getParameters();
		// Check to see if the report has parameters that should be prompted
		for (int i = 0; i < params.length; i++)
		{
			JRParameter param = params[i];
			if (!param.isSystemDefined())
			{
				String paramName = param.getName();
				String paramDesc = param.getDescription();
				String paramClass = param.getValueClassName();
				Object value = parameters.get(paramName);
				if ((value != null) && (value.getClass().getName().equalsIgnoreCase("java.lang.Double")) && (paramClass.equalsIgnoreCase("java.lang.Integer")))
				{
					value = new Integer(((Double) value).intValue());
					parameters.put(paramName, value);
				}
				if (value != null)
				{
					Debug.trace("JasperDebug: Parameter: " + ((paramDesc != null) ? paramDesc : "") + " " + paramName + " (" + paramClass + "): " + value.toString());
				}
				else
				{
					Debug.trace("JasperDebug: Parameter: " + ((paramDesc != null) ? paramDesc : "") + " " + paramName + " (" + paramClass + "): null");
				}
				if (!parameters.containsKey(paramName))
				{
					Debug.trace("JasperDebug: Warning parameter " + ((paramDesc != null) ? paramDesc : "") + " " + paramName + " (" + paramClass + ") has no value");
				}
			}
		}
		
		// create JasperPrint using fillReport() method
		JasperPrint jp;
		// switch to the client classloader when filling the report (needed for spring to properly load fontextensions in webclient)
		ClassLoader savedCl = Thread.currentThread().getContextClassLoader();
		try
		{
			Thread.currentThread().setContextClassLoader(JasperReportRunner.class.getClassLoader()); // get the client class loader (is this "bulletproof"?)
			// new approach, using the input type
			if (INPUT_TYPE.DB.equalsIgnoreCase(inputType) && dataSource instanceof Connection) {
				// Connection connection, JRDataSource jrDataSource
				connection = (Connection) dataSource;
				jp = JasperFillManager.fillReport(jasperReport, parameters, connection);
			} else if (INPUT_TYPE.XML.equalsIgnoreCase(inputType) && dataSource instanceof JRXmlDataSource) {
				JRXmlDataSource jrXMLSource = (JRXmlDataSource) dataSource;
				jp = JasperFillManager.fillReport(jasperReport, parameters, jrXMLSource);
			} else if (INPUT_TYPE.CSV.equalsIgnoreCase(inputType) && dataSource instanceof JRCsvDataSource) {
				JRCsvDataSource jrCSVSource = (JRCsvDataSource) dataSource;
				jp = JasperFillManager.fillReport(jasperReport, parameters, jrCSVSource);
			} else if (INPUT_TYPE.JRD.equalsIgnoreCase(inputType) && dataSource instanceof JRDataSource) {
				JRDataSource jrDataSource = (JRDataSource) dataSource;
				jp = JasperFillManager.fillReport(jasperReport, parameters, jrDataSource);
			} else {
				// for old/legacy behavior
				if (connection != null) {
					jp = JasperFillManager.fillReport(jasperReport, parameters, connection);
				} else if (dataSource instanceof JRDataSource) {
					jp = JasperFillManager.fillReport(jasperReport, parameters, (JRDataSource)dataSource);
				} 
			    else {
			    	throw new JRException("Input type " + inputType + " has been used with an incorrect datasource of type: " + dataSource.getClass());
			    }
			}
		}
		finally
		{
			Thread.currentThread().setContextClassLoader(savedCl);
		}

		if (virtualizer != null)
		{
			virtualizer.setReadOnly(true);
		}

		// adding MAXIMUM_ROWS_PER_SHEET property
		if (maxRowsPerSheet == null) jp.setProperty("MAXIMUM_ROWS_PER_SHEET", String.valueOf(65535));
		else jp.setProperty("MAXIMUM_ROWS_PER_SHEET", String.valueOf(maxRowsPerSheet.intValue()));

		GarbageMan virtualizerCleaner = null;
		if (virtualizer != null)
		{
			// install garbage man to cleanup virtualizer
			final UUID uuid = UUID.randomUUID();
			synchronized (virtualizers) {
				virtualizers.add(new VirtualizerState(uuid, System.currentTimeMillis(), virtualizer));
			}
			virtualizerCleaner = new GarbageMan() {

				private static final long serialVersionUID = 1L;

				@Override
				public void cleanup() {
					cleanupVirtualizers(uuid);
				}
			};
		}
		
		return new JasperPrintResult(jp, virtualizerCleaner);
	}

	protected static void cleanupVirtualizers(UUID uuid) {

		// Virtualizers
		// cleanup, if virtualizers have been used (NOTE: file virtualizer does	cleanup itself)

		synchronized (virtualizers) {
			Iterator<VirtualizerState> it = virtualizers.iterator();
			while (it.hasNext()) {
				VirtualizerState virt = it.next();
				// cleanup this virtualizer or old ones (older then 1 hour)
				if (virt.uuid.equals(uuid) || System.currentTimeMillis() - virt.timestamp > 60*60*1000) {
					it.remove();
					try {
						virt.virtualizer.cleanup();
					}
					catch (Exception e) {
						Debug.error(e);
					}
				}
			}

		}
	}

	public static String adjustFileUnix(String file)
	{
		if (file == null) return null;
		while (file.indexOf('\\') != -1)
		{
			file = file.replace('\\', '/');
		}
		return file;
	}

	public static class VirtualizerState {

		public final UUID uuid;
		public final long timestamp;
		public final JRVirtualizer virtualizer;

		public VirtualizerState(UUID uuid, long timestamp, JRVirtualizer virtualizer) {
			this.uuid = uuid;
			this.timestamp = timestamp;
			this.virtualizer = virtualizer;
		}
	}


}
