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

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.jasperreports.engine.JRException;

import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;

/**
 * Helper function to unwrap Rhino Js objects to Java 
 */

public class JSArgumentsUnwrap {
	public static Object unwrapJSObject(Object o, IClientPluginAccess clientPluginAccess) throws JRException {
		
		if (o instanceof Wrapper) {
			o = ((Wrapper) o).unwrap();

			if (o instanceof Object[]) {
				o = Arrays.asList(((Object[]) o));
			}
			return unwrapJSObject(o, clientPluginAccess);
		}
		
		if (o instanceof IFoundSet) {
			// foundSets are used as data sources for sub-reports
			return new JRFoundSetDataSource(clientPluginAccess, (IFoundSet)o);
		}
		
		if (o instanceof ScriptableObject) {
			Map<String, Object> params = new HashMap<String, Object>();
			ScriptableObject so = (ScriptableObject) o;
			Object[] oa = so.getIds();
			for (int i = 0; i < oa.length; i++) {
				params.put(oa[i].toString(), so.get(oa[i] + "", null));
			}
			o = params;
		}

		if (o instanceof Map) {

			Map<Object, Object> params = new HashMap<Object, Object>();
			Map h = (Map)o;
			Iterator it = h.keySet().iterator();
			while (it.hasNext()) {
				Object key = it.next();
				params.put(key, unwrapJSObject(h.get(key), clientPluginAccess));
			}
			o = params;
		}
		return o;
	}
}