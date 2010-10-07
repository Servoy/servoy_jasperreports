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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.rmi.Remote;
import java.rmi.RemoteException;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;

import com.servoy.j2db.dataprocessing.JSDataSet;

/**
 * RMI interface
 */
public interface IJasperReportsService extends IJasperReportRunner, Remote {

	public boolean jasperCompile(String report, String destination, String repdir) throws RemoteException, JRException;

	public boolean writeFile(String filenm, Object obj, String repdir) throws RemoteException, IOException;
	
	public boolean deleteFile(String filenm, String repdir) throws RemoteException, IOException;

	public byte[] readFile(String filenm, String repdir) throws RemoteException, IOException;

	public String getReportDirectory() throws RemoteException;
	
	public String getExtraDirectories() throws RemoteException;

	public String[] getReports(boolean compiled, boolean uncompiled) throws RemoteException, FileNotFoundException;

	public JSDataSet getReportParameters(String report, String repdir) throws RemoteException, JRException;

	public void saveByteArrayToFile(String filename, byte[] buffer, String reportsDir) throws RemoteException, IOException;

	public JasperReport getJasperReport(String report, String repdir) throws RemoteException, JRException;
	
	public byte[] getJasperBytes(String type, JasperPrint jasperPrint, String extraDirs) throws IOException, JRException, RemoteException;

	public byte[] loadImage(String image) throws JRException, RemoteException;
}
