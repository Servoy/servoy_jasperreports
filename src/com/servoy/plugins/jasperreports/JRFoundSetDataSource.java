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

import java.sql.Time;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRRewindableDataSource;

import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyException;
import com.servoy.j2db.util.Utils;

/**
 * JRDataSource wrapper for foundsets.
 * @author Rob Gansevles
 *
 */
public class JRFoundSetDataSource implements JRRewindableDataSource {

	private final IClientPluginAccess pluginAccess;
	private final FoundsetWithIndex foundSet;

	private Map<String, FoundsetWithIndex> relatedFoundSets;

	public  JRFoundSetDataSource (IClientPluginAccess pluginAccess, IFoundSet foundSet)
	{
		this.pluginAccess = pluginAccess;
		this.foundSet = new FoundsetWithIndex(foundSet);
		moveFirst();
	}

	public boolean next() {
	
		FoundsetWithIndex deepestFoundSet = foundSet;
		String deepestPath = null;

		if (relatedFoundSets == null)
		{
			// initial (or re-wound) state
			relatedFoundSets = new HashMap<String, FoundsetWithIndex>();
		}
		else
		{
			// find the deepest foundset and progress that one
			Iterator<Map.Entry<String, FoundsetWithIndex>> iterator = relatedFoundSets.entrySet().iterator();
			int maxDepth = 0;
			while (iterator.hasNext())
			{
				Map.Entry<String, FoundsetWithIndex> entry = iterator.next();
				int length = ((String)entry.getKey()).split("\\.").length;
				if (length > maxDepth)
				{
					maxDepth = length;
					deepestFoundSet = entry.getValue();
					deepestPath = (String) entry.getKey();
				}
			}
		}
		
		// progress 1 record
		if (deepestFoundSet.advance())
		{
			// more data in the deepest foundSet
			return true;
		}
		
		// we are at the end of the deepest foundSset
		if (deepestPath == null)
		{
			// we are in the main foundSet
			return false;
		}
		// else we completed a related foundSet, progress the next deepest one
		relatedFoundSets.remove(deepestPath);
		return next();
	}

	public Object getFieldValue(JRField jrField) throws JRException {

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
		
		return JSArgumentsUnwrap.unwrapJSObject(convertToFieldValueClass(value,jrField), pluginAccess);
	}
	
	@SuppressWarnings("boxing")
	private Object convertToFieldValueClass(Object value, JRField jrf)
	{
		if (value == null) return value;
		if ("java.lang.Boolean".equals(jrf.getValueClassName()) && !(value instanceof Boolean)) return Utils.getAsBoolean(value);
		else if ("java.lang.Byte".equals(jrf.getValueClassName()) && !(value instanceof Byte)) return Byte.valueOf(value.toString());
		else if ("java.lang.Double".equals(jrf.getValueClassName()) && !(value instanceof Double)) return Utils.getAsDouble(value);
		else if ("java.lang.Float".equals(jrf.getValueClassName()) && !(value instanceof Float)) return Utils.getAsFloat(value);
		else if ("java.lang.Integer".equals(jrf.getValueClassName()) && !(value instanceof Integer)) return Utils.getAsInteger(value);
		else if ("java.lang.Long".equals(jrf.getValueClassName()) && !(value instanceof Long)) return Utils.getAsLong(value);
		else if ("java.lang.Short".equals(jrf.getValueClassName()) && !(value instanceof Short)) return Short.valueOf(value.toString());
		else if ("java.lang.String".equals(jrf.getValueClassName()) && !(value instanceof String)) return value.toString();
		else if ("java.lang.Date".equals(jrf.getValueClassName()) && !(value instanceof Date))
			try {
				return DateFormat.getDateInstance().parse(value.toString());
			} catch (ParseException e) {
				Debug.error(e);
				return value;
			}
		else if ("java.sql.Timestamp".equals(jrf.getValueClassName()) && !(value instanceof Timestamp)) return Timestamp.valueOf(value.toString());
		else if ("java.sql.Time".equals(jrf.getValueClassName()) && !(value instanceof Time)) return Time.valueOf(value.toString());
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

	/**
	 * Get the value of the dataProvider, may be prefixed with relation name.
	 * @param dataProviderId
	 * @return
	 * @throws ServoyException 
	 */
	protected Object getDataProviderValue(String dataProviderId) throws ServoyException {

		FoundsetWithIndex searchFoundSet = foundSet;
		String searchName = dataProviderId;
		if (!dataProviderId.startsWith("globals.")) // globals are not related fields
		{
			// orders_to_order_details.order_details_to_product.product_name
			String[] parts = dataProviderId.split("\\.");
			String path = null;
			for (int i = 0; i < parts.length-1; i++)
			{
				path = (path==null)?parts[i]:(path + '.' + parts[i]);
				FoundsetWithIndex relatedFoundSet = relatedFoundSets.get(path);
				if (relatedFoundSet == null)
				{
					IRecord record = searchFoundSet.getCurrentRecord();
					if (record == null)
					{
						return null;
					}
					IFoundSet rfs = record.getRelatedFoundSet(parts[i], null);
					if (rfs == null)
					{
						return null;
					}
					relatedFoundSets.put(path, relatedFoundSet = new FoundsetWithIndex(rfs));
					relatedFoundSet.advance(); // go to first record
				}
				searchFoundSet = relatedFoundSet;
			}
			searchName = parts[parts.length-1];
		}

		IRecord record = searchFoundSet.getCurrentRecord();
		if (record == null)
		{
			return null;
		}			
		return record.getValue(searchName);
	}

	public void moveFirst() {
		relatedFoundSets = null;
		foundSet.rewind();
	}

	/**
	 * 	Wrapper for foundset with current index.
	 * 
	 * @author rgansevles
	 *
	 */
	public static class FoundsetWithIndex {

		public final IFoundSet foundSet;
		private int index;

		public FoundsetWithIndex(IFoundSet foundSet) {
			this.foundSet = foundSet;
			rewind();
		}
		
		public void rewind() {
			this.index = -1;
		}

		public IRecord getCurrentRecord() {
			return foundSet.getRecord(index);
		}

		public boolean advance()
		{
			if (foundSet.getSize() > index+1)
			{
				index++;
				return true;
			}
			return false;
		}
	}
}
