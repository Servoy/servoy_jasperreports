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

import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.scripting.IConstantsObject;

@ServoyDocumented(category = ServoyDocumented.PLUGINS)
public abstract class JR_SVY_VIEWER_DISPLAY_MODE implements IConstantsObject 
{
	/**
	 * @sample
	 * elements.jrbean.displayMode = JR_SVY_VIEWER_DISPLAY_MODE.FIT_WIDTH;
	 * elements.jrbean.showReport(myDataSource,\"/myReport.jrxml\",null);
	 */
	public static final String FIT_WIDTH = "display_mode_fit_width";
	
	/**
	 * @sample
	 * elements.jrbean.displayMode = JR_SVY_VIEWER_DISPLAY_MODE.FIT_PAGE;
	 * elements.jrbean.showReport(myDataSource,\"/myReport.jrxml\",null);
	 */
	public static final String FIT_PAGE = "display_mode_fit_page";
	
	/**
	 * @sample
	 * elements.jrbean.displayMode = JR_SVY_VIEWER_DISPLAY_MODE.ACTUAL_PAGE_SIZE;
	 * elements.jrbean.showReport(myDataSource,\"/myReport.jrxml\",null);
	 */
	public static final String ACTUAL_PAGE_SIZE = "acutal_page_size";
}
