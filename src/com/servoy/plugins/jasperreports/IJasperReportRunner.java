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

import java.rmi.RemoteException;
import java.util.Map;

import net.sf.jasperreports.engine.JasperPrint;

/**
 * Run reports.
 */
public interface IJasperReportRunner { 

	/**
	 * This method creates and fills the JasperPrint document. The resulting document can be viewed, printed or exported to other formats.
	 * Note: the filling of the report is done on the Server for SQL based reports and on the client for foundset based. 
	 *  
	 * @param clientID the ID of the client
	 * @param source the data source of the report
	 * @param txid the transaction ID
	 * @param report the report template 
	 * @param parameters the parameters for the report
	 * @param repdir the report directory
	 * @param extraDirs the list of extra directories
	 * @return the filled JasperPrint
	 * @throws RemoteException
	 * @throws Exception
	 */
	public JasperPrint getJasperPrint(String clientID, Object source, String txid, String report, Map<String, Object> parameters, String repdir, String extraDirs) throws RemoteException, Exception;
}
