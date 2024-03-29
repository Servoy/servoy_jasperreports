/*
 * ============================================================================
 * GNU Lesser General Public License
 * ============================================================================
 *
 * Servoy - Smart Technology For Smart Clients.
 * Copyright � 1997-2015 Servoy BV http://www.servoy.com
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

import java.io.Serializable;

import net.sf.jasperreports.engine.JasperPrint;

public class JasperPrintResult implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private final JasperPrint jasperPrint;
	
	private final GarbageMan garbageMan;
	
	public JasperPrintResult(JasperPrint jasperPrint) {
		this(jasperPrint, null);
	}
	
	public JasperPrintResult(JasperPrint jasperPrint, GarbageMan garbageMan) {
		this.jasperPrint = jasperPrint;
		this.garbageMan = garbageMan;
	}
	
	public JasperPrint getJasperPrint() {
		return jasperPrint;
	}
	
	public GarbageMan getGarbageMan() {
		return garbageMan;
	}

}
