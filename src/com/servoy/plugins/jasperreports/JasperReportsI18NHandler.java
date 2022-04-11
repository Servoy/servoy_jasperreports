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

import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;

import org.apache.wicket.Session;

/**
 * Created on Aug 3, 2007
 */
import com.servoy.j2db.plugins.IClientPluginAccess;

/**
 * i18n support
 */

public class JasperReportsI18NHandler {
	
	private static Locale stringToLocale(String localeString) {
		String[] sa = localeString.split("_");
		String language = (sa[0] == null ? "" : sa[0]);
		if (sa.length == 1) {
			return new Locale(language);
		}
		String country = (sa[1] == null ? "" : sa[1]);
		if (sa.length == 2) {
			return new Locale(language, country);
		}

		String variant = "";
		for (int i = 2; i < sa.length; i++) {
			variant += "_" + (sa[i] == null ? "" : sa[i]);
		}
		return new Locale(language, country, variant);
	}

	public static Map<String, Object> appendI18N(Map<String, Object> parameters, boolean isWebClient, IClientPluginAccess application, String localeString) {
		Locale l = null;
		if (localeString != null && !localeString.equals("")) {
			l = stringToLocale(localeString);
			parameters.put("REPORT_LOCALE", l);
		} else if (!parameters.containsKey("REPORT_LOCALE")) {
			if(application.getReleaseNumber() > 3742)
			{
				l = getClientLocale(application);
			}
			else
			{
				l = Locale.getDefault();
				if (isWebClient) {
					//Get Webclient locale settings (Returns incorrect value < 3.5.1)
					//<= 3.5.2 the resourcebundle only contains the Default Language when called in the Webclient
					l = Session.get().getLocale();
				}	
			}
			parameters.put("REPORT_LOCALE", l);
		}

		ResourceBundle rb = application.getResourceBundle(l);
		parameters.put("REPORT_RESOURCE_BUNDLE", rb);
		return parameters;
	}
	
	private static Locale getClientLocale(IClientPluginAccess application)
	{
		return application.getLocale();
	}
}
