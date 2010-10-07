/*
 * ============================================================================
 * GNU Lesser General Public License
 * ============================================================================
 *
 * Servoy - Smart Technology For Smart Clients.
 * Copyright © 1997-2010 Servoy BV http://www.servoy.com
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
 
import com.servoy.j2db.scripting.IConstantsObject;

public abstract class OUTPUT_FORMAT implements IConstantsObject 
{
	public static final String XHTML = "xhtml";
	public static final String HTML = "html";
	public static final String RTF = "rtf";
	public static final String TXT = "txt";
	public static final String CSV = "csv";
	public static final String XML = "xml";
	public static final String PDF = "pdf";
	public static final String XLS = "xls";
	public static final String XLS_1_SHEET = "xls_1_sheet";
	public static final String EXCEL = "excel";
	public static final String ODS = "ods";
	public static final String ODT = "odt";
	public static final String DOCX = "docx";
	public static final String JRPRINT = "jrprint";
	public static final String VIEW = "view";
	public static final String PRINT = "print";

}
