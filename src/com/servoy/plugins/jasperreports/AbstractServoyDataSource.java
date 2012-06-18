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
 * http://www.servoy.com
 */

package com.servoy.plugins.jasperreports;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRRewindableDataSource;

import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * Abstract base class for data sources that use Servoy data.
 * 
 * @author rgansevles
 *
 */
public abstract class AbstractServoyDataSource implements JRRewindableDataSource {

	private final IClientPluginAccess pluginAccess;

	public  AbstractServoyDataSource (IClientPluginAccess pluginAccess)
	{
		this.pluginAccess = pluginAccess;
	}

	public abstract  boolean next() throws JRException;

	/**
	 * Get the value of the dataProvider.
	 * @param dataProviderId
	 * @return
	 * @throws ServoyException 
	 */
	protected abstract Object getDataProviderValue(String dataProviderId) throws ServoyException ;

	/**
	 * Get the value of the field, may be a value or a global method call (when it has braces).
	 * @param dataProviderId
	 * @return
	 * @throws ServoyException 
	 */
	public final Object getFieldValue(JRField jrField) throws JRException {

		String name = jrField.getName();

		Object value;
		try {
			if (name.indexOf('(') > 0)
			{
				// a global method
				value = getGlobalMethodResult(name);
			}
			else
			{
				// a data provider
				value =  getDataProviderValue(name);
			}
		} catch (Exception e) {
			throw new JRException(e);
		}

		return convertToFieldValueClass(JSArgumentsUnwrap.unwrapJSObject(value, pluginAccess), jrField);
	}

	protected Object convertToFieldValueClass(Object value, JRField jrf)
	{
		if (value == null) return value;
		if ("java.lang.Boolean".equals(jrf.getValueClassName()) && !(value instanceof Boolean)) return Boolean.valueOf(Utils.getAsBoolean(value));
		if ("java.lang.Byte".equals(jrf.getValueClassName()) && !(value instanceof Byte))
		{
			if (value instanceof Number) return Byte.valueOf(((Number)value).byteValue());
			return Byte.valueOf(value.toString());
		}
		if ("java.lang.Double".equals(jrf.getValueClassName()) && !(value instanceof Double)) return Double.valueOf(Utils.getAsDouble(value));
		if ("java.lang.Float".equals(jrf.getValueClassName()) && !(value instanceof Float)) return Float.valueOf(Utils.getAsFloat(value));
		if ("java.lang.Integer".equals(jrf.getValueClassName()) && !(value instanceof Integer)) return Integer.valueOf(Utils.getAsInteger(value));
		if ("java.lang.Long".equals(jrf.getValueClassName()) && !(value instanceof Long)) return Long.valueOf(Utils.getAsLong(value));
		if ("java.lang.Short".equals(jrf.getValueClassName()) && !(value instanceof Short))
		{
			if (value instanceof Number) return Short.valueOf(((Number)value).shortValue());
			return Short.valueOf(value.toString());
		}
		if ("java.lang.String".equals(jrf.getValueClassName()) && !(value instanceof String)) return value.toString();
		return value;
	}

	/**
	 * Call the global method, arguments may be specified.
	 * @param name
	 * @return
	 * @throws Exception 
	 */	
	protected Object getGlobalMethodResult(String name) throws Exception {

		String[] split = name.trim().split("[(,)]");
		Object[] args = new Object[split.length-1];
		for (int i = 1; i < split.length; i++)
		{
			args[i-1] = getDataProviderValue(split[i].trim());
		}
		return pluginAccess.executeMethod(null, split[0].trim(), args, false);
	}

	public abstract void moveFirst();
}
