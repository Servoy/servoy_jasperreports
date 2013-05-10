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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporter;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.export.JRCsvExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporter;
import net.sf.jasperreports.engine.export.JRHtmlExporterParameter;
import net.sf.jasperreports.engine.export.JRPdfExporter;
import net.sf.jasperreports.engine.export.JRRtfExporter;
import net.sf.jasperreports.engine.export.JRTextExporter;
import net.sf.jasperreports.engine.export.JRTextExporterParameter;
import net.sf.jasperreports.engine.export.JRXhtmlExporter;
import net.sf.jasperreports.engine.export.JRXlsExporter;
import net.sf.jasperreports.engine.export.JRXlsExporterParameter;
import net.sf.jasperreports.engine.export.JRXmlExporter;
import net.sf.jasperreports.engine.export.JRXmlExporterParameter;
import net.sf.jasperreports.engine.export.oasis.JROdsExporter;
import net.sf.jasperreports.engine.export.oasis.JROdtExporter;
import net.sf.jasperreports.engine.export.ooxml.JRDocxExporter;
import net.sf.jasperreports.engine.fill.JRAbstractLRUVirtualizer;
import net.sf.jasperreports.engine.fill.JRFileVirtualizer;
import net.sf.jasperreports.engine.fill.JRGzipVirtualizer;
import net.sf.jasperreports.engine.fill.JRSwapFileVirtualizer;
import net.sf.jasperreports.engine.util.JRSaver;
import net.sf.jasperreports.engine.util.JRSwapFile;
import net.sf.jasperreports.engine.util.SimpleFileResolver;

import com.servoy.j2db.util.Debug;

public class JasperReportRunner implements IJasperReportRunner
{

	private static final int TEXT_PAGE_WIDTH_IN_CHARS = 120;
	private static final int TEXT_PAGE_HEIGHT_IN_CHARS = 60;

	private static final String VIRTUALIZER_FILE = "file";
	private static final String VIRTUALIZER_SWAP_FILE = "swapFile";
	private static final String VIRTUALIZER_GZIP = "gZip";

	private final IJasperReportsService jasperReportsService;

	public JasperReportRunner(IJasperReportsService jasperReportsService)
	{
		this.jasperReportsService = jasperReportsService;
	}

	public JasperPrint getJasperPrint(String clientID, Object source, String txid, String report, Map parameters, String repdir, String extraDirs) throws RemoteException, Exception
	{
		if (source == null)
		{
			throw new IllegalArgumentException("no data source");
		}
		if (!(source instanceof JRDataSource))
		{
			throw new IllegalArgumentException("non-datasource argument: " + source.getClass());
		}

		if (report == null)
		{
			throw new IllegalArgumentException("No jasperReport <null> has been found or loaded");
		}

		Debug.trace("JasperTrace: Directory: " + repdir);

		JasperReport jasperReport = jasperReportsService.getJasperReport(clientID, report, repdir);

		return getJasperPrint(jasperReport, null, (JRDataSource) source, parameters, repdir, jasperReportsService.getCheckedExtraDirectoriesRelativePath(extraDirs));
	}

	public static byte[] getJasperBytes(String type, JasperPrint jasperPrint, String extraDirs, Map<String, Object> exporterParameters) throws IOException, JRException
	{
		// exporting the report
		if (type.equalsIgnoreCase(OUTPUT_FORMAT.JRPRINT))
		{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			JRSaver.saveObject(jasperPrint, baos);
			return baos.toByteArray();
		}

		JRExporter exporter;
		if (type.equalsIgnoreCase(OUTPUT_FORMAT.PDF))
		{
			exporter = new JRPdfExporter();

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
			exporter.setParameter(JRTextExporterParameter.PAGE_WIDTH, new Integer(TEXT_PAGE_WIDTH_IN_CHARS));
			exporter.setParameter(JRTextExporterParameter.PAGE_HEIGHT, new Integer(TEXT_PAGE_HEIGHT_IN_CHARS));

		}
		else if (type.equalsIgnoreCase(OUTPUT_FORMAT.ODT))
		{
			exporter = new JROdtExporter();

		}
		else if (type.equalsIgnoreCase(OUTPUT_FORMAT.ODS))
		{
			exporter = new JROdsExporter();

		}
		else if (type.equalsIgnoreCase(OUTPUT_FORMAT.HTML))
		{
			exporter = new JRHtmlExporter();
			String location = (exporterParameters != null ? adjustFileUnix((String) exporterParameters.get("REPORT_FILE_LOCATION")) : null);
			if (location != null)
			{
				location = (location.endsWith("/") ? location : location + "/");
				exporter.setParameter(JRHtmlExporterParameter.IS_OUTPUT_IMAGES_TO_DIR, Boolean.TRUE);
				exporter.setParameter(JRHtmlExporterParameter.IMAGES_DIR_NAME, location + "/" + jasperPrint.getName() + ".html_files/");
				exporter.setParameter(JRHtmlExporterParameter.IMAGES_URI, jasperPrint.getName() + ".html_files/"); // backslash is important
				exporter.setParameter(JRHtmlExporterParameter.IS_USING_IMAGES_TO_ALIGN, Boolean.TRUE);
			}

		}
		else if (type.equalsIgnoreCase(OUTPUT_FORMAT.XHTML))
		{
			exporter = new JRXhtmlExporter();

		}
		else if (type.equalsIgnoreCase(OUTPUT_FORMAT.XML))
		{
			exporter = new JRXmlExporter();
			exporter.setParameter(JRXmlExporterParameter.IS_EMBEDDING_IMAGES, Boolean.TRUE);

		}
		else if (type.equalsIgnoreCase(OUTPUT_FORMAT.EXCEL) || type.equalsIgnoreCase(OUTPUT_FORMAT.XLS) || type.equalsIgnoreCase(OUTPUT_FORMAT.XLS_1_SHEET))
		{
			// coding For Excel:
			exporter = new JRXlsExporter();
			// default is multiple sheets per page
			exporter.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.FALSE);
			if (type.equalsIgnoreCase(OUTPUT_FORMAT.XLS_1_SHEET))
			{
				exporter.setParameter(JRXlsExporterParameter.IS_ONE_PAGE_PER_SHEET, Boolean.TRUE);
			}
			else
			{
				String maxRowsPerSheet = jasperPrint.getProperty("MAXIMUM_ROWS_PER_SHEET");
				exporter.setParameter(JRXlsExporterParameter.MAXIMUM_ROWS_PER_SHEET, Integer.valueOf(maxRowsPerSheet));
			}
			/*
			 * next line is deprecated so will use the suggested replacement 
			 * exporterXLS.setParameter(JRXlsExporterParameter.IS_AUTO_DETECT_CELL_TYPE,Boolean.TRUE);
			 */
			exporter.setParameter(JRXlsExporterParameter.IS_DETECT_CELL_TYPE, Boolean.TRUE);

			exporter.setParameter(JRXlsExporterParameter.IS_WHITE_PAGE_BACKGROUND, Boolean.FALSE);
			exporter.setParameter(JRXlsExporterParameter.IS_REMOVE_EMPTY_SPACE_BETWEEN_ROWS, Boolean.TRUE);

		}
		else if (type.equalsIgnoreCase(OUTPUT_FORMAT.DOCX))
		{
			exporter = new JRDocxExporter();

		}
		else
		{
			throw new IllegalArgumentException("Undefined output type:" + type);
		}

		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		exporter.setParameter(JRExporterParameter.JASPER_PRINT, jasperPrint);
		exporter.setParameter(JRExporterParameter.OUTPUT_STREAM, baos);
		exporter.setParameter(JRExporterParameter.CHARACTER_ENCODING, "UTF-8");

		// add all received JRExporterParameters to the exporter
		if (exporterParameters != null)
		{
			// leaving these 5 for legacy purposes
			if (exporterParameters.containsKey(EXPORTER_PARAMETERS.PAGE_INDEX)) exporter.setParameter(JRExporterParameter.PAGE_INDEX, exporterParameters.get(EXPORTER_PARAMETERS.PAGE_INDEX));
			if (exporterParameters.containsKey(EXPORTER_PARAMETERS.START_PAGE_INDEX)) exporter.setParameter(JRExporterParameter.START_PAGE_INDEX, exporterParameters.get(EXPORTER_PARAMETERS.START_PAGE_INDEX));
			if (exporterParameters.containsKey(EXPORTER_PARAMETERS.END_PAGE_INDEX)) exporter.setParameter(JRExporterParameter.END_PAGE_INDEX, exporterParameters.get(EXPORTER_PARAMETERS.END_PAGE_INDEX));
			if (exporterParameters.containsKey(EXPORTER_PARAMETERS.OFFSET_X)) exporter.setParameter(JRExporterParameter.OFFSET_X, exporterParameters.get(EXPORTER_PARAMETERS.OFFSET_X));
			if (exporterParameters.containsKey(EXPORTER_PARAMETERS.OFFSET_Y)) exporter.setParameter(JRExporterParameter.OFFSET_Y, exporterParameters.get(EXPORTER_PARAMETERS.OFFSET_Y));

			// add all fully qualified named jasperreports export paramters
			for (Map.Entry<String, Object> entry : exporterParameters.entrySet())
			{
				String key = entry.getKey();
				Object value = entry.getValue();
				if (key.startsWith("EXPORTER_PARAMETER:"))
				{
					String className = key.substring(key.lastIndexOf(":") + 1, key.lastIndexOf("."));
					try
					{
						Class<?> clz = Class.forName(className);
						if (clz != null)
						{
							String fieldName = key.substring(key.lastIndexOf(".") + 1);
							if (fieldName != null)
							{
								Field field = clz.getField(fieldName);
								if (field != null)
								{
									JRExporterParameter exporterParameter = (JRExporterParameter) field.get(JRExporterParameter.class);
									exporter.setParameter(exporterParameter, value);
								}
							}
						}
					}
					catch (ClassNotFoundException e)
					{
						Debug.log(e);
					}
					catch (SecurityException e)
					{
						Debug.log(e);
					}
					catch (NoSuchFieldException e)
					{
						Debug.log(e);
					}
					catch (IllegalArgumentException e)
					{
						Debug.log(e);
					}
					catch (IllegalAccessException e)
					{
						Debug.log(e);
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
			exporter.setParameter(JRExporterParameter.FILE_RESOLVER, new SimpleFileResolver(dirList));
		}

		exporter.exportReport();

		// cleanup, if virtualizers have been used (NOTE: file virtualizer does
		// cleanup itself)
		if (virtualizer != null)
		{
			virtualizer.cleanup();
			virtualizer = null;
		}

		return baos.toByteArray();
	}

	private static JRAbstractLRUVirtualizer virtualizer = null;

	public static JasperPrint getJasperPrint(JasperReport jasperReport, Connection connection, JRDataSource jrDataSource, Map parameters, String repdir, String extraDirs) throws JRException
	{
		// client - fill (the compiled) report
		Debug.trace("JasperTrace: Directory: " + repdir);

		// make directory unix style
		String jasperDirectory = adjustFileUnix(repdir);

		if (parameters == null) parameters = new HashMap();

		parameters.put("report_directory", jasperDirectory);
		if (!jasperDirectory.endsWith("/")) jasperDirectory = jasperDirectory + '/';

		String subReportDir = (String) parameters.get("SUBREPORT_DIR");
		if (subReportDir == null || subReportDir.equals(""))
		{
			// if the subreport directory is not set
			parameters.put("SUBREPORT_DIR", jasperDirectory);
		}
		else
		{
			// if the path is relative
			if (!(new File(subReportDir)).isAbsolute())
			{
				subReportDir = (jasperDirectory != null && jasperDirectory.trim().length() > 0 ? (jasperDirectory.endsWith("/") ? "" : "/") + subReportDir : subReportDir);
				subReportDir = adjustFileUnix(subReportDir);
				parameters.put("SUBREPORT_DIR", subReportDir);
			}
			else
			{
				//SUBREPORT_DIR value is an absolute path - this is not allowed
				Debug.warn("SUBREPORT_DIR cannot be specified as an absolute location; please use a location relative to the reports directory");
			}
		}

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

		for (Iterator iter = parameters.keySet().iterator(); iter.hasNext();)
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
			virtErrMsg = "The indicated PAGE_OUT_DIR path: '" + pageOutDir + "' is not a valid path.";
			Debug.error(virtErrMsg);
			throw new JRException(virtErrMsg);
		}
		else testDir = null;

		// Virtualizers
		virtualizer = null;
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
			// virtualizer type has been specified but is not of a supported
			// type
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
		JasperPrint jp = null;

		if (connection == null && jrDataSource == null) throw new IllegalArgumentException("No model or db connection <null> has been found or loaded");
		else try
		{
			if (connection != null)
			{
				jp = JasperFillManager.fillReport(jasperReport, parameters, connection);
			}
			else if (jrDataSource != null)
			{
				jp = JasperFillManager.fillReport(jasperReport, parameters, jrDataSource);
			}
			// else
			// throw new
			// IllegalArgumentException("No model or db connection <null> has been found or loaded");
		}
		catch (Exception e)
		{
			Debug.log("Cause: " + e.getCause() +"\nMessage:  " + e.getMessage());
			throw new JRException("Cause: " + e.getCause() +"\nMessage:  " + e.getMessage());
		}

		if (virtualizer != null)
		{
			virtualizer.setReadOnly(true);
		}

		// adding MAXIMUM_ROWS_PER_SHEET property
		if (maxRowsPerSheet == null) jp.setProperty("MAXIMUM_ROWS_PER_SHEET", String.valueOf(65535));
		else jp.setProperty("MAXIMUM_ROWS_PER_SHEET", String.valueOf(maxRowsPerSheet.intValue()));

		return jp;
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

}
