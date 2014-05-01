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

import com.servoy.j2db.scripting.IConstantsObject;

/**
 * Constants (class) for exporting parameters.
 * 
 * @author acostache
 * 
 */
public abstract class EXPORTER_PARAMETERS implements IConstantsObject 
{
	public static final String PAGE_INDEX = "PAGE_INDEX";
	public static final String START_PAGE_INDEX = "START_PAGE_INDEX";
	public static final String END_PAGE_INDEX = "END_PAGE_INDEX";
	public static final String OFFSET_X = "OFFSET_X";
	public static final String OFFSET_Y = "OFFSET_Y"; 
}
