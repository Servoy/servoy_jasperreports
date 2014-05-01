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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Map;

import net.sf.jasperreports.engine.JRTemplate;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import com.servoy.j2db.dataprocessing.JSDataSet;

/**
 * RMI interface
 */
public interface IJasperReportsService extends IJasperReportRunner, Remote {

	/**
	 * This method compiles the jasper report. Compilation is done on the server, regardless of report type (sql, foundset etc.).
	 * If the destination of the compiled report is specified, it will be saved on the server.
	 * 
	 * @param clientID the client ID
	 * @param report the jasper template report to compile
	 * @param destination the destination file for the compiled report
	 * @param repdir the (relative) report directory
	 * @return true if the report compilation is successful, false otherwise
	 * @throws RemoteException
	 * @throws Exception
	 */
	public boolean jasperCompile(String clientID, String report, String destination, String repdir) throws RemoteException, Exception;

	/**
	 * This method writes a file on the server.
	 * 
	 * @param clientID the client ID
	 * @param fileName the file name (and relative location) to write to
	 * @param obj the file object to be written
	 * @param repdir the (relative) report directory
	 * @return true if the file has been written successfully, false otherwise
	 * @throws RemoteException
	 * @throws Exception
	 */
	public boolean writeFile(String clientID, String fileName, Object obj, String repdir) throws RemoteException, Exception;
	
	/**
	 * This method deletes a file from the server.
	 * 
	 * @param clientID the client ID
	 * @param fileName the file name (and relative location) to delete
	 * @param repdir the (relative) report directory
	 * @return true if the file has been deleted successfully, false otherwise
	 * @throws RemoteException
	 * @throws Exception
	 */
	public boolean deleteFile(String clientID, String fileName, String repdir) throws RemoteException, Exception;

	/**
	 * Reads a file and returns the corresponding byte array.
	 * 
	 * @param clientID the client ID
	 * @param fileName the file name (and relative location) to read from
	 * @param repdir the (relative) report directory
	 * @return the corresponding byte array
	 * @throws RemoteException
	 * @throws Exception
	 */
	public byte[] readFile(String clientID, String fileName, String repdir) throws RemoteException, Exception;

	/**
	 * Returns the report directory as set on the server.
	 * The client may set an own directory relative to this.
	 * NOTE: this is to be used internally and not exposed to the client
	 * 
	 * @return the report directory location on the server
	 * @throws RemoteException
	 * @throws Exception
	 */
	public String getReportDirectory() throws RemoteException, Exception;
	
	/**
	 * Returns the list of extra directories as set on the server.
	 * The client may set own directories relative to any of the server's.
	 * NOTE: this is to be used internally and not exposed to the client
	 * 
	 * @return the report extra directories locations on the server
	 * @throws RemoteException
	 * @throws Exception
	 */
	public String getExtraDirectories() throws RemoteException, Exception;

	/**
	 * Returns an array of available reports, based on the reports directory.
	 * 
	 * @param clientID the client ID
	 * @param compiled true if to report compiled reports, false otherwise
	 * @param uncompiled true if to report uncompiled reports, false other wise
	 * @return an array with the names of the requested types of reports
	 * @throws RemoteException
	 * @throws Exception
	 */
	public String[] getReports(String clientID, boolean compiled, boolean uncompiled) throws RemoteException, Exception;
	
	/**
	 * Returns an array of available reports, based on the reports directory.
	 * 
	 * @param clientID the client ID
	 * @param filter a specific filter to base the results of the search on (COMPILED, NONCOMPILED or a naming criteria)
	 * @return an array with the names of the requested types of reports
	 * @throws RemoteException
	 * @throws Exception
	 */
	public String[] getReports(String clientID, String filter) throws RemoteException, Exception;

	/**
	 * Retrieve a JSDataSet with the report parameters, except the system defined ones.
	 * 
	 * @param clientID the client ID
	 * @param report the report file name
	 * @param repdir the (relative) report directory
	 * @return the data set of custom report parameters
	 * @throws RemoteException
	 * @throws Exception
	 */
	public JSDataSet getReportParameters(String clientID, String report, String repdir) throws RemoteException, Exception;

	/**
	 * This method loads the report and compiles it on the server. It returns a compiled document of the 
	 * JasperReport type. 
	 * NOTE: this method is usually called internally and wrapped within getJasperPrint calls.
	 * 
	 * @param clientID the client ID
	 * @param report the report file name
	 * @param repdir the (relative) report directory
	 * @return the compiled JasperReport type document
	 * @throws RemoteException
	 * @throws Exception
	 */
	public JasperReport getJasperReport(String clientID, String report, String repdir) throws RemoteException, Exception;
	
	/**
	 * This method processes the compiled and filled JasperPrint document and exports it considering the provided type and parameters.
	 * 
	 * @param clientID the client ID
	 * @param type the export type (see OUTPUT_FORMAT class)
	 * @param jasperPrint the filled jasper print document
	 * @param extraDirs the (relative) extra directories
	 * @param exporterParameters the parameters for the exporter
	 * @return the exported jasper print in the form of a byte array
	 * @throws RemoteException
	 * @throws Exception
	 */
	public byte[] getJasperBytes(String clientID, String type, JasperPrint jasperPrint, String extraDirs, Map<String, Object> exporterParameters) throws RemoteException, Exception;

	/**
	 * This method will be used to load a server side image needed for as an additional resource for a report.
	 * 
	 * @param clientID the client ID
	 * @param image the image file (relative) location or just the name
	 * @return the image as a byte array
	 * @throws RemoteException
	 * @throws Exception
	 */
	public byte[] loadImage(String clientID, String image) throws RemoteException, Exception;
	
	/**
	 * This method will be used to load a server side report.
	 * 
	 * @param clientID the client ID
	 * @param location the report name, with or without its relative location to the server report directory
	 * @return the requested report document as a JasperReport type object
	 * @throws RemoteException
	 * @throws Exception
	 */
	public JasperReport loadReport(String clientID, String location) throws RemoteException, Exception;

	/**
	 * Receives a relative report path and returns the adjusted absolute path, containing the server report directory location.
	 * NOTE: only for internal server usage.
	 * 
	 * @param reportPath the relative report path
	 * @return the adjusted and checked absolute path in the report directory directory
	 * @throws RemoteException
	 * @throws Exception
	 */
	public String getCheckedRelativeReportsPath(String reportPath) throws RemoteException, Exception;
	 
	/**
	 * Receives a extra directory relative path and returns the adjusted path, representing an absolute path (with respect to the 
	 * server-side extra directories setting).
	 * NOTE: only for internal server usage.
	 * 
	 * @param extraDirsPath the extra directory relative path
	 * @return the absolute and corrected path to the server-side extra directories 
	 * @throws RemoteException
	 * @throws Exception
	 */
	public String getCheckedExtraDirectoriesRelativePath(String extraDirsPath) throws RemoteException, Exception;

	/**
	 * This method will be used to load a server side style template needed for as an additional resource for a report.
	 * 
	 * @param jasperReportsClientId the client ID
	 * @param name the name of the style template
	 * @return the template as a JRTemplate object
	 * @throws RemoteException
	 * @throws Exception
	 */
	public JRTemplate loadTemplate(String jasperReportsClientId, String name) throws RemoteException, Exception;

}
