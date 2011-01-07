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
 * http://www.servoy.com
 */
package com.servoy.plugins.jasperreports;

import java.util.Calendar;
import java.util.TimeZone;

import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.server.headlessclient.IWebClientPluginAccess;
import com.servoy.j2db.server.headlessclient.IPageContributor;

/**
 * WebClient view logic 
 */

public class JasperReportsWebViewer {

	public static final String MIME_TYPE_XHTML = "text/html";
	public static final String MIME_TYPE_HTML = "text/html";
	public static final String MIME_TYPE_RTF = "text/richtext";
	public static final String MIME_TYPE_TXT = "text/plain";
	public static final String MIME_TYPE_CSV = "text/comma-separated-values";
	public static final String MIME_TYPE_XML = "text/xml";
	public static final String MIME_TYPE_PDF = "application/pdf";
	public static final String MIME_TYPE_XLS = "application/vnd.ms-excel";
	public static final String MIME_TYPE_ODS = "application/x-vnd.oasis.opendocument.spreadsheet";
	public static final String MIME_TYPE_ODT = "application/x-vnd.oasis.opendocument.text";
	public static final String MIME_TYPE_DOCX = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
	//	public static final String MIME_TYPE_XLSX = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
	
	public static void show(IClientPluginAccess application, byte[] jsp, String file, String ext, String mimeType) {
		IWebClientPluginAccess wapp = (IWebClientPluginAccess) application;
		IPageContributor pc = wapp.getPageContributor();
		if (pc != null) {
			if (file == null || file.length() == 0) {
				Calendar cal = Calendar.getInstance(TimeZone.getDefault());
				String DATE_FORMAT = "yyyyMMddHHmmss";
				java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat(DATE_FORMAT);
				sdf.setTimeZone(TimeZone.getDefault());
				file = "report_" + sdf.format(cal.getTime()) + "." + ext;
			}
			String url = wapp.serveResource(file, jsp, mimeType);
			wapp.showURL(url, "_self", null, 0);
		}
	}
}
