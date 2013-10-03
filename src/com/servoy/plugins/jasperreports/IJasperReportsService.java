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

	public boolean jasperCompile(String clientID, String report, String destination, String repdir) throws RemoteException, Exception;

	public boolean writeFile(String clientID, String filenm, Object obj, String repdir) throws RemoteException, Exception;
	
	public boolean deleteFile(String clientID, String filenm, String repdir) throws RemoteException, Exception;

	public byte[] readFile(String clientID, String filenm, String repdir) throws RemoteException, Exception;

	public String getReportDirectory() throws RemoteException, Exception;
	
	public String getExtraDirectories() throws RemoteException, Exception;

	public String[] getReports(String clientID, boolean compiled, boolean uncompiled) throws RemoteException, Exception;
	
	public String[] getReports(String clientID, String filter) throws RemoteException, Exception;

	public JSDataSet getReportParameters(String clientID, String report, String repdir) throws RemoteException, Exception;

	public JasperReport getJasperReport(String clientID, String report, String repdir) throws RemoteException, Exception;
	
	public byte[] getJasperBytes(String clientID, String type, JasperPrint jasperPrint, String extraDirs, Map exporterParameters) throws RemoteException, Exception;

	public byte[] loadImage(String clientID, String image) throws RemoteException, Exception;
	
	public JasperReport loadReport(String clientID, String location) throws RemoteException, Exception;

	public String getCheckedRelativeReportsPath(String reportPath) throws RemoteException, Exception;
	 
	public String getCheckedExtraDirectoriesRelativePath(String extraDirsPath) throws RemoteException, Exception;

	public JRTemplate loadTemplate(String jasperReportsClientId, String name) throws RemoteException, Exception;

}
