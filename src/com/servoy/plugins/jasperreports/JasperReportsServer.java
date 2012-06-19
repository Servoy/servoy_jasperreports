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
/*
 * Modified 2008.October.06 Tom Parry (TP)
 * 
 */
package com.servoy.plugins.jasperreports;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.commons.io.IOCase;
import org.apache.commons.io.filefilter.WildcardFileFilter;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRParameter;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

import com.servoy.j2db.dataprocessing.BufferedDataSet;
import com.servoy.j2db.dataprocessing.JSDataSet;
import com.servoy.j2db.plugins.IServerAccess;
import com.servoy.j2db.plugins.IServerPlugin;
import com.servoy.j2db.plugins.PluginException;
import com.servoy.j2db.preference.PreferencePanel;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * Iserver impl.
 */
@SuppressWarnings("rawtypes")
public class JasperReportsServer implements IJasperReportsService, IServerPlugin {
	
	private Properties settings;
	private IServerAccess application;

	// must have default constructor
	public JasperReportsServer() {
	}
	
	public Properties getProperties() {
		Properties props = new Properties();
		props.put(DISPLAY_NAME, "jasperPluginRMI");
		return props;
	}

	public void load() throws PluginException {
	}

	public void initialize(IServerAccess app) throws PluginException {
		application = app;
		settings = app.getSettings();

		// Code to move reports directory setting from
		// "directorie.jasper.report" to "directory.jasper.report"
		if (settings.containsKey("directorie.jasper.report")) {
			settings.setProperty("directory.jasper.report", settings.getProperty("directorie.jasper.report"));
			settings.remove("directorie.jasper.report");
		}
		try {
			app.registerRMIService("servoy.IJasperReportService", this);
		} catch (Exception e) {
			throw new PluginException(e);
		}
	}

	public void unload() throws PluginException {
		settings = null;
	}

	public JasperReport getJasperReport(String clientID, String report, String repdir) throws Exception { 
		
		repdir = getCheckedRelativeReportsPath(repdir);
		
		if (!hasAccess(clientID)) return null;
		
		JasperReport jasperReport = null;

		if (report == null) {
			throw new IllegalArgumentException("No jasperReport <null> has been found or loaded");
		}

		String servoyDir = System.getProperty("user.dir");
		while (servoyDir.indexOf('\\') != -1) {
			servoyDir = servoyDir.replace('\\', '/');
		}

		Debug.trace("JasperTrace: Directory: " + repdir);

		// make directory unix style
		String jasperDirectory = repdir;
		while (jasperDirectory.indexOf('\\') != -1) {
			jasperDirectory = jasperDirectory.replace('\\', '/');
		}

		// make report unix style
		while (report.indexOf('\\') != -1) {
			report = report.replace('\\', '/');
		}
		
		//check if report is inside report directory
		String repSrc = jasperDirectory + (jasperDirectory.endsWith("/") ? "" : "/")+ report;
		if (fileIsOutsideReportsDirectory(new File(repSrc), jasperDirectory))
			throw new IllegalArgumentException("No jasperReport " + report + " has been found or loaded in directory " + jasperDirectory);
		
		// First, try absolute
		String reportCompiled = new String(report);
		if (reportCompiled.endsWith("jrxml"))
			reportCompiled = reportCompiled.substring(0, reportCompiled.length() - 5) + "jasper";
		else if (reportCompiled.endsWith("jasper"))
			reportCompiled = new String(reportCompiled);

		// Try relative
		File tempFile = new File(reportCompiled);
		if (!tempFile.exists() || !tempFile.isFile()) {
			if (jasperDirectory.endsWith("/"))
				reportCompiled = jasperDirectory + reportCompiled;
			else
				reportCompiled = jasperDirectory + '/' + reportCompiled;
			tempFile = new File(reportCompiled);
		}
		if (tempFile.exists()) {
			jasperReport = (JasperReport) JRLoader.loadObjectFromLocation(reportCompiled);
		} else {

			String reportSource = new String(report);

			tempFile = new File(reportSource);
			if (!tempFile.exists() || !tempFile.isFile()) {
				if (jasperDirectory.endsWith("/"))
					reportSource = jasperDirectory + reportSource;
				else
					reportSource = jasperDirectory + '/' + reportSource;
				tempFile = new File(reportSource);
			}
			Debug.trace("JasperDebug: find source " + reportSource);

			if (tempFile.exists()) {
				JasperDesign jasperDesign = JRXmlLoader.load(reportSource);
				jasperReport = JasperCompileManager.compileReport(jasperDesign);
			} else {
				throw new IllegalArgumentException("No jasperReport " + report + " has been found or loaded in directory " + jasperDirectory);
			}
		}

		if (jasperReport == null) {
			throw new IllegalArgumentException("No jasperReport " + report + " has been found or loaded in directory " + jasperDirectory);
		}
		return jasperReport;
	}

	public byte[] getJasperBytes(String clientID, String type, JasperPrint jasperPrint, String extraDirs, Map exporterParams) throws Exception { 
		
		if (!hasAccess(clientID)) return null;
		
		extraDirs = getCheckedExtraDirectoriesRelativePath(extraDirs); 
				
		boolean serviceSet = false;
		if (JasperReportsProvider.jasperReportsLocalService.get() == null) {
			// called from smart client over rmi
			serviceSet = true;
			JasperReportsProvider.jasperReportsLocalService.set(this);
		}
		
		boolean clientIdSet = false; //clientId setting for the server threadLocal
		if (JasperReportsProvider.jasperReportsLocalClientID.get() == null) {
			clientIdSet = true;
			JasperReportsProvider.jasperReportsLocalClientID.set(clientID);
		}
		
		try {
			return JasperReportRunner.getJasperBytes(type, jasperPrint,	extraDirs, exporterParams);
		} finally {
			
			if (serviceSet) {
				JasperReportsProvider.jasperReportsLocalService.set(null);
			}
			if (clientIdSet) {
				JasperReportsProvider.jasperReportsLocalClientID.set(null);
			}
		}
	}
	
	public JasperPrint getJasperPrint(String clientID, Object source, String txid, String report, Map parameters, String repdir, String extraDirs) throws Exception {
		
		repdir = getCheckedRelativeReportsPath(repdir);
		
		String dbalias = null;
		JRDataSource jrds = null;
		
		if (source == null) {
			throw new IllegalArgumentException("No model or db connection <null> has been found or loaded");
		}

		if (source instanceof String) {
			dbalias = (String)source;
		} else if (source instanceof JRDataSource) {
			jrds = (JRDataSource) source;
		}

		Connection conn = null;

		if (report == null) {
			throw new IllegalArgumentException("No jasperReport <null> has been found or loaded");
		}
		
		boolean serviceSet = false;
		if (JasperReportsProvider.jasperReportsLocalService.get() == null) {
			serviceSet = true;
			JasperReportsProvider.jasperReportsLocalService.set(this);
		}

		boolean clientIdSet = false; //clientId setting for the server threadLocal
		if (JasperReportsProvider.jasperReportsLocalClientID.get() == null) {
			clientIdSet = true;
			JasperReportsProvider.jasperReportsLocalClientID.set(clientID);
		}
		
		try {
			if (dbalias != null) {
				Debug.trace("JasperTrace: getconnection for: " + dbalias);
				conn = application.getDBServerConnection(dbalias, txid);
				if (conn == null) {
					throw new IllegalArgumentException("No connection returned for database: " + dbalias);
				}
			}

			Debug.trace("JasperTrace: Directory: " + repdir);
			
			JasperReport jasperReport = getJasperReport(clientID, report, repdir);
			
			return JasperReportRunner.getJasperPrint(jasperReport, conn, jrds, parameters, repdir, getCheckedExtraDirectoriesRelativePath(extraDirs));

		} finally {
			if (conn != null)
				if (txid != null) {
					Utils.releaseConnection(conn);
				} else {
					Utils.closeConnection(conn);
				}
				
			if (serviceSet) {
				JasperReportsProvider.jasperReportsLocalService.set(null);
			}
			
			if (clientIdSet) {
				JasperReportsProvider.jasperReportsLocalClientID.set(null);
			}
		}
	}

	private String getAbsolutePath(String path, String relativeDir)
	{
		// Check if file is relative or absolute
		File tempFile = new File(path);
		if (!tempFile.isAbsolute()) {
			if (relativeDir.endsWith("/"))
				path = relativeDir + path;
			else
				path = relativeDir + '/' + path;
		}

		return path;
	}

	public boolean jasperCompile(String clientID, String report, String destination, String repdir) throws Exception {

		repdir = getCheckedRelativeReportsPath(repdir);
		
		if (!hasAccess(clientID)) return false;
		
		String compiledFile = null;

		if (report == null) {
			throw new IllegalArgumentException("No jasperReport <null> has been found or loaded");
		}

		// make directory unix style
		String jasperDirectory = JasperReportRunner.adjustFileUnix(repdir);

		// make report unix style
		report = JasperReportRunner.adjustFileUnix(report);

		report = getAbsolutePath(report, jasperDirectory);
		File tempFile = new File(report);

		if (fileIsOutsideReportsDirectory(tempFile, jasperDirectory))
			throw new IllegalArgumentException("No jasperReport " + report + " has been found or loaded");
		
		if (tempFile.exists()) {
			if (destination == null)
				compiledFile = JasperCompileManager.compileReportToFile(report);
			else
			{
				// make the destination unix style
				destination = JasperReportRunner.adjustFileUnix(destination);

				// make absolute path
				destination = getAbsolutePath(destination, jasperDirectory);
				if (fileIsOutsideReportsDirectory(new File(destination), jasperDirectory))
					throw new IllegalArgumentException("Writing to " + destination + " is not allowed");

				//compile the report;
				JasperCompileManager.compileReportToFile(report, destination);

				File tempDestFile = new File(destination);
				if (tempDestFile.exists()) compiledFile = destination;
			}
		} else {
			throw new IllegalArgumentException("No jasperReport " + report + " has been found or loaded in directory " + jasperDirectory);
		}

		return (compiledFile != null);
	}

	public boolean writeFile(String clientID, String filenm, Object obj, String repdir) throws Exception {
		
		repdir = getCheckedRelativeReportsPath(repdir);
		
		if (!hasAccess(clientID)) return false;
		
		// make directory unix style
		String jasperDirectory = JasperReportRunner.adjustFileUnix(repdir);

		String report = JasperReportRunner.adjustFileUnix(jasperDirectory + '/' + filenm);

		File file = new File(report);
		if (fileIsOutsideReportsDirectory(file, jasperDirectory))
			throw new IllegalArgumentException("Jasper writefile: File " + filenm + " is illegal");
		if (file.exists() && !file.canWrite())
			throw new IOException("Jasper writefile: File " + filenm + " can't be written");
		file.createNewFile();
		if (!file.isFile())
			throw new IOException("Jasper writefile: File " + filenm + " is illegal");
		FileOutputStream fileoutputstream = new FileOutputStream(file);
		fileoutputstream.write((byte[]) obj);
		if (fileoutputstream != null)
			fileoutputstream.close();
		return true;
	}
	
	public boolean deleteFile(String clientID, String filenm, String repdir) throws Exception {
		
		repdir = getCheckedRelativeReportsPath(repdir);
		
		if (!hasAccess(clientID)) return false;
		
		// make directory unix style
		String jasperDirectory = JasperReportRunner.adjustFileUnix(repdir);
		String report = JasperReportRunner.adjustFileUnix(jasperDirectory + '/' + filenm);
		File file = new File(report);
		
		if (fileIsOutsideReportsDirectory(file, jasperDirectory))
			throw new IllegalArgumentException("Jasper deleteFile: File " + filenm + " is illegal");

		if (!file.exists())
			throw new IOException("Jasper deleteFile: File " + filenm + " does not exist");
		
		if (file.exists() && !file.canWrite())
			throw new IOException("Jasper deleteFile: File " + filenm + " is write protected");
		
		boolean success = file.delete();
		
		if (!success)
			throw new IOException("Jasper deleteFile: File " + filenm + " could not be deleted");
		
		return true;
	}

	private boolean fileIsOutsideReportsDirectory(File f, String reportDirectory) throws IOException
	{
		String unixFilePath = JasperReportRunner.adjustFileUnix(f.getCanonicalPath());
		if (!unixFilePath.startsWith(JasperReportRunner.adjustFileUnix(reportDirectory))) return true;
		else return false;
	}
	
	public byte[] readFile(String clientID, String filenm, String repdir) throws Exception {
		
		if (!hasAccess(clientID)) return null;
		
		// make directory unix style
		repdir = getCheckedRelativeReportsPath(repdir);
		String jasperDirectory = JasperReportRunner.adjustFileUnix(repdir);

		String report = JasperReportRunner.adjustFileUnix(jasperDirectory + '/' + filenm);

		File file = new File(report);
		if (fileIsOutsideReportsDirectory(file, jasperDirectory))
			throw new IllegalArgumentException("Jasper readfile: File " + filenm + " is illegal");
		if (!file.exists())
			throw new IllegalArgumentException("Jasper readfile: File " + filenm + " not found");
		if (!file.isFile())
			throw new IllegalArgumentException("Jasper readfile: File " + filenm + " is illegal");
		if (!file.canRead())
			throw new IllegalArgumentException("Jasper readfile: File " + filenm + " can't be read");

		byte buffer[] = new byte[(int) file.length()];
		BufferedInputStream input = new BufferedInputStream(new FileInputStream(file));
		try {
			input.read(buffer, 0, buffer.length);
		}
		finally {
			input.close();
		}
		return buffer;
	}

	private boolean checkParentDir(File f, File defaultFolder)
	{
		if (f == null) return false;
		if (defaultFolder.equals(f) || defaultFolder.equals(f.getParentFile()))
		{
			return true;
		}
		else
		{
			return checkParentDir(f.getParentFile(), defaultFolder);
		}
	}
	
	private String absolutePathFormatting(String path) throws IOException
	{
		if (System.getProperty("os.name").startsWith("Windows"))
		{
			File f = new File(path);
			if (f.isAbsolute()) return f.getCanonicalPath();  
		}
		return path;
	}
	
	/**
	 * Return the adjusted and checked absolute path in the report directory directory. 
	 * 
	 * @param pathToReportDir the path to the client-set report directory, relative to the server-side setting
	 * 
	 * @return the absolute path to the relative report directory (will start with the server side report directory path)
	 */
	public String getCheckedRelativeReportsPath(String pathToReportDir) throws RemoteException, Exception {

		if (getReportDirectory() == null) throw new FileNotFoundException("Report directory not found");
		
		if (pathToReportDir == null || pathToReportDir.length() == 0) return getReportDirectory();

		pathToReportDir = absolutePathFormatting(pathToReportDir);
		if (pathToReportDir.startsWith(getReportDirectory())) 
		{
			if (!(new File(pathToReportDir)).exists()) throw new FileNotFoundException("Relative path to report directory not found");
			else return adjustFile(pathToReportDir);
		}
		else 
		{
			if ((new File(pathToReportDir)).isAbsolute()) throw new SecurityException("Absolute directory setting not allowed");
			
			File dir = new File(getReportDirectory(), pathToReportDir);
			File parent = new File(getReportDirectory());
			if (!checkParentDir(dir.getCanonicalFile(), parent))
			{
				throw new SecurityException(pathToReportDir + " is not a valid path in the report directory");
			}
			if (!dir.exists()) throw new FileNotFoundException("Relative path to report directory not found");
			else return adjustFile(dir.getCanonicalPath()); 
		}
	}
	
	/**
	 * Getting the plugin's reports directory which has been set from the AdminServer page.
	 * 
	 * @return the path to the reports' directory
	 */
	public String getReportDirectory() throws Exception {
		String repDir = settings.getProperty("directory.jasper.report");
		if (repDir == null)
		{
			//default to appserver_dir/server/reports
//			repDir = adjustFile(ApplicationServer.get().getServoyApplicationServerDirectory() + "/server/reports");
//			setReportDirectory(repDir);
			return null;
		}
		return absolutePathFormatting(repDir);
	}
	
	/**
	 * Setting the (absolute) server-side path to the plugin's reports directory 
	 * 
	 * @param jasperDirectory absolute path to the plugin's reports directory on the server
	 */
	public void setReportDirectory(String jasperDirectory) {
		settings.setProperty("directory.jasper.report", adjustFile(jasperDirectory));
	}
	
	/**
	 * This method is similar to {@linkplain #getCheckedRelativeReportsPath(String)} and returns the absolute path (relative to the 
	 * server-side extra directories setting) to the specified (relative) extra directories path.
	 * 
	 * @param relativePathToExtraDirs extra directories path relative to the server-side setting 
	 * 
	 * @return absolute and corrected path to the server-side extra directories 
	 */
	public String getCheckedExtraDirectoriesRelativePath(String relativePathsToExtraDirs) throws RemoteException, Exception {
		
		if (getExtraDirectories() == null)
		{
			// extra directories not set 
			if (relativePathsToExtraDirs != null)
			{
				// trying to set to some path, but with no server side extra dirs => exception
				if (relativePathsToExtraDirs.length() > 0) throw new FileNotFoundException("Extra directories not set");
				else if ("".equals(relativePathsToExtraDirs)) return null;
			}
			else return null;
		}
			
		if (relativePathsToExtraDirs == null || relativePathsToExtraDirs.length() == 0) return getExtraDirectories();
		
		// format all relative extra paths
		String[] xtraPaths = relativePathsToExtraDirs.split(",");
		String[] extraDirs = getExtraDirectories().split(",");
		for (String xtradir : extraDirs)
		{
			xtradir = adjustFile(absolutePathFormatting(xtradir));
			for (String relpath : xtraPaths)
			{
				relpath = adjustFile(absolutePathFormatting(relpath));
				if (relpath.startsWith(xtradir))
				{
					if (!(new File(relpath)).exists()) throw new FileNotFoundException("Relative path to extra directory not found");
					else return relpath;
				}
				else
				{
					if ((new File(relpath)).isAbsolute()) throw new SecurityException("Absolute directory setting on server not allowed");
					
					File dir = new File(xtradir, relpath);
					if (!dir.exists()) throw new FileNotFoundException("Relative path to extra directory not found");
					
					File parent = new File(xtradir);
					if (!checkParentDir(dir.getCanonicalFile(),parent))
					{
						throw new SecurityException(relpath + " is not a valid path in any extra directory");
					}
					return adjustFile(dir.getCanonicalPath());
				}
			}
		}
		
		return null;
	}
	
	/**
	 * Getting the additional resource directories, needed for handling different report needed resources. 
	 * This value can be set from the AdminServer page.
	 * 
	 * @return the path to the additional resource directories
	 */
	public String getExtraDirectories() throws Exception {
		String xtraDirs = settings.getProperty("directories.jasper.extra");
		if (xtraDirs == null)
		{
			return null;
		}
		return xtraDirs;
	}

	public void setExtraDirectories(String extraDirectories) {
		settings.setProperty("directories.jasper.extra", adjustFile(extraDirectories));
	}

	public static String adjustFile(String file) {
		/* if (java.io.File.separator == "/") */
		if (!System.getProperty("os.name").startsWith("Windows")) {
			while (file.indexOf('\\') != -1) {
				file = file.replace('\\', '/');
			}
		} else {
			while (file.indexOf('/') != -1) {
				file = file.replace('/', '\\');
			}
		}
		return file;
	}

	public static Object getAsRightType(Object value, String className) {

		Object result = null;
		String valueString = "";

		if (value == null)
			return result;

		valueString = value.toString().trim();

		//TODO: Hardcoded dutch values. Check if code is used at all. If so i18n or english
		if (className.equalsIgnoreCase("java.lang.Integer")) {
			if (valueString.equals(""))
				result = Integer.valueOf("0");
			else if (valueString.equalsIgnoreCase("Ja"))
				result = Integer.valueOf("1");
			else if (valueString.equalsIgnoreCase("Nee"))
				result = Integer.valueOf("0");
			else {
				result = Integer.valueOf(valueString);
			}
		} else if (className.equalsIgnoreCase("numeric")) {
			if (value.equals(""))
				result = Float.valueOf("0");
			else {
				/*
				 * String numericformat = "#,##"; DecimalFormat
				 * decimalformat = new DecimalFormat(numericformat); Number
				 * num = decimalformat.parse(columnValue);
				 * ps.setDouble(fieldInfo.queryPos,
				 * Double.valueOf(num.doubleValue()));
				 */
				value = valueString.replace(",".charAt(0), ".".charAt(0));
				result = Double.valueOf(valueString);
			}
		} else if (className.equalsIgnoreCase("float")) {
			if (valueString.equals(""))
				result = Float.valueOf("0");
			else {
				/*
				 * String numericformat = "#,##"; DecimalFormat
				 * decimalformat = new DecimalFormat(numericformat); Number
				 * num = decimalformat.parse(columnValue);
				 * ps.setDouble(fieldInfo.queryPos,
				 * Double.valueOf(num.doubleValue()));
				 */
				valueString = valueString.replace(",".charAt(0), ".".charAt(0));
				result = Float.valueOf(valueString);
			}
		} else if (className.equalsIgnoreCase("double")) {
			if (value.equals(""))
				result = Double.valueOf("0");
			else {
				/*
				 * String numericformat = "#,##"; DecimalFormat
				 * decimalformat = new DecimalFormat(numericformat); Number
				 * num = decimalformat.parse(columnValue);
				 * ps.setDouble(fieldInfo.queryPos,
				 * Double.valueOf(num.doubleValue()));
				 */
				value = valueString.replace(",".charAt(0), ".".charAt(0));
				result = Double.valueOf(valueString);
			}
		} else {
			result = value;
		}

		return result;
	}

	/*
	 * @see IPlugin#getPreferencePanels()
	 */
	public PreferencePanel[] getPreferencePanels() {
		return null;
	}

	public Map<String, String> getRequiredPropertyNames() {
		Map<String, String> req = new HashMap<String, String>();
		req.put("directory.jasper.report", "Reports Directory");
		req.put("directories.jasper.extra", "Extra Directories needed by the Servoy JasperReports Plugin. Paths can be set on the client relative to the server side setting path; for multiple entries, separate with commas.");
		return req;
	}

	public String[] getReports(String clientID, String filePattern) throws Exception {
		String reportsdir = getReportDirectory();
		List<File> files = getFileListing(new File(reportsdir), filePattern);
		return getListOfFiles(files);
	}
	
	public List<File> getFileListing(File startingDirectory, String filePattern) throws FileNotFoundException
	{
		checkStartingDirectory(startingDirectory);
		FileFilter wildcardFileFilter = new WildcardFileFilter(filePattern,IOCase.INSENSITIVE);
		File[] filesAndDirs = startingDirectory.listFiles(wildcardFileFilter);
		List<File> filesDirs = Arrays.asList(filesAndDirs);
		List<File> result = new ArrayList<File>();
		
		for (int i = 0; i < filesDirs.size(); i++) {
			File file = filesDirs.get(i);
			if (!file.isFile()) {
				List<File> deeperList = getFileListing(file, filePattern);
				result.addAll(deeperList);
			} else {
				result.add(file);
			}
		}
		return result;
	}
	
	public String[] getReports(String clientID, boolean compiled, boolean uncompiled) throws Exception {
		String reportsdir = getReportDirectory();
		List<File> files = getFileListing(new File(reportsdir), compiled, uncompiled);
		return getListOfFiles(files);
	}
	
	private List<File> getFileListing(File startingDirectory, boolean compiled,
			boolean uncompiled) throws FileNotFoundException {

		checkStartingDirectory(startingDirectory);
		
		List<File> result = new ArrayList<File>();

		ArrayList<String> extensions = new ArrayList<String>();
		if (compiled)
			extensions.add(".JASPER");
		if (uncompiled)
			extensions.add(".JRXML");

		FileFilter filter = new JasperFilter(extensions);
		File[] filesAndDirs = startingDirectory.listFiles(filter);
		List<File> filesDirs = Arrays.asList(filesAndDirs);

		for (int i = 0; i < filesDirs.size(); i++) {
			File file = filesDirs.get(i);
			if (!file.isFile()) {
				List<File> deeperList = getFileListing(file, compiled, uncompiled);
				result.addAll(deeperList);
			} else {
				result.add(file);
			}
		}

		return result;
	}
	
	private void checkStartingDirectory(File startingDirectory) throws FileNotFoundException
	{
		if (startingDirectory == null) {
			throw new IllegalArgumentException("Directory should not be null.");
		}
		if (!startingDirectory.exists()) {
			throw new FileNotFoundException("Directory does not exist: "
					+ startingDirectory);
		}
		if (!startingDirectory.isDirectory()) {
			throw new IllegalArgumentException("Is not a directory: "
					+ startingDirectory);
		}
		if (!startingDirectory.canRead()) {
			throw new IllegalArgumentException("Directory cannot be read: "
					+ startingDirectory);
		}
	}
	
	public String[] getListOfFiles(List<File> files) throws Exception
	{
		String reportsdir = getReportDirectory();
		String[] list = new String[files.size()];

		for (int i = 0; i < files.size(); i++) {

			String filepath = files.get(i).getPath();
			String filen = filepath.substring(reportsdir.length());

			if (filen.startsWith("\\"))
				filen = filen.substring(1);
			
			list[i] = filen;
		}
		return list;
	}

	private class JasperFilter implements FileFilter {

		private ArrayList<String> jrext;

		public JasperFilter(ArrayList<String> ext) {
			this.jrext = ext;
		}

		public boolean accept(File file) {
			boolean acc = false;

			for (int i = 0; i < jrext.size(); i++) {
				String ext = jrext.get(i);
				String uname = file.getName().toUpperCase();

				if (uname.endsWith(ext)) {
					acc = true;
					break;
				}

				if (file.isDirectory())
					acc = true;
			}
			return (acc);
		}
	}

	public JSDataSet getReportParameters(String clientID, String report, String repdir) throws Exception {

		JasperReport jasperReport = getJasperReport(clientID, report, repdir);

		BufferedDataSet bds = new BufferedDataSet(new String[]{"Name","Type","Description"}, new ArrayList<Object[]>());

		JRParameter[] params = jasperReport.getParameters();

		for (int i = 0; i < params.length; i++) {
			JRParameter param = params[i];

			if (!param.isSystemDefined()) {

				String paramName = param.getName();
				String paramDesc = param.getDescription();
				String paramClass = param.getValueClassName();

				Object[] oa = new Object[3];
				oa[0] = paramName;
				oa[1] = paramClass;
				oa[2] = paramDesc;
				bds.addRow(oa);
				oa = null;
			}
		}

		JSDataSet jsds = new JSDataSet(bds);
		return jsds;
	}

	/**
	 * TODO: add security checks
	 */
	public byte[] loadImage(String clientID, String img) throws Exception {
		
		String[] xtraDirs = this.getExtraDirectories().split(",");
		String filePath2LoadFrom = null;
		
		boolean foundImage = false;
		for (String xtraDir : xtraDirs)
		{
			xtraDir = JasperReportRunner.adjustFileUnix(absolutePathFormatting(xtraDir));
			filePath2LoadFrom = xtraDir + (xtraDir.endsWith("/") ? "" : "/") + img ;
			if (!fileIsOutsideReportsDirectory(new File(filePath2LoadFrom), xtraDir))
			{
				foundImage = true;
				break;
			}
		}
		
		if (!foundImage) throw new IllegalArgumentException("No image file: " + img + " has been found in extra resources directories");
		
		return JRLoader.loadBytesFromLocation(filePath2LoadFrom);
	}
	
	/**
	 * TODO: add security checks
	 */
	public JasperReport loadReport(String clientID, String location)
			throws Exception {
		File file = new File(location);
		if (fileIsOutsideReportsDirectory(file, getReportDirectory()))
			throw new IllegalArgumentException("No jasperReport " + location + " has been found or loaded in directory " + getReportDirectory());
		
		Object obj = JRLoader.loadObject(file);
		if (obj instanceof JasperReport) {
			return  (JasperReport) obj;
		}
		return null;
	}
	
	/**
	 * Checking if the client application has access for server side operations execution.
	 * 
	 * @param clientId the client application's id
	 * 
	 * @return <b>true</b>, if either we are not in a 5.2.x container, or if we are (in 5.2.x or above) and have access<br>
	 * 	<b>false</b>, otherwise (if we are in a 5.2.x environment and do not have access)
	 */
	private final boolean hasAccess(String clientId) throws Exception {
		
		if (application.isServerProcess(clientId) || application.isAuthenticated(clientId)) { 
			return true; // have access
		}
		else {
			return false; //NO access
		}
	}
	
}
