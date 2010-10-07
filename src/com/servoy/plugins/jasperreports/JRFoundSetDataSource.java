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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRRewindableDataSource;

import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.util.ServoyException;

/**
 * JRDataSource wrapper for foundsets.
 * @author Rob Gansevles
 *
 */
public class JRFoundSetDataSource implements JRRewindableDataSource {

	private final IClientPluginAccess pluginAccess;
	private final IFoundSet foundSet;

	private Map<String, IFoundSet> relatedFoundSets;

	public  JRFoundSetDataSource (IClientPluginAccess pluginAccess, IFoundSet foundSet)
	{
		this.pluginAccess = pluginAccess;
		this.foundSet = foundSet;
		moveFirst();
	}

	public boolean next() {
	
		IFoundSet deepestFoundSet = foundSet;
		String deepestPath = null;

		int next;
		if (relatedFoundSets == null)
		{
			// initial (or re-wound) state
			relatedFoundSets = new HashMap<String, IFoundSet>();
			next = 0;
		}
		else
		{
			// find the deepest foundset and progress that one
			Iterator<Map.Entry<String, IFoundSet>> iterator = relatedFoundSets.entrySet().iterator();
			int maxDepth = 0;
			while (iterator.hasNext())
			{
				Map.Entry<String, IFoundSet> entry = iterator.next();
				int length = ((String)entry.getKey()).split("\\.").length;
				if (length > maxDepth)
				{
					maxDepth = length;
					deepestFoundSet = (IFoundSet) entry.getValue();
					deepestPath = (String) entry.getKey();
				}
			}

			// progress 1 record
			next = deepestFoundSet.getSelectedIndex()+1;
		}

		if (next >= deepestFoundSet.getSize())
		{
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

		deepestFoundSet.setSelectedIndex(next);
		if (next != deepestFoundSet.getSelectedIndex())
		{
			// could not progress to next record
			return false;
		}

		// more data in the deepest foundSet
		return true;
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

		return JSArgumentsUnwrap.unwrapJSObject(value, pluginAccess);
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

		IFoundSet searchFoundSet = foundSet;
		String searchName = dataProviderId;
		if (!dataProviderId.startsWith("globals.")) // globals are not related fields
		{
			// orders_to_order_details.order_details_to_product.product_name
			String[] parts = dataProviderId.split("\\.");
			String path = null;
			for (int i = 0; i < parts.length-1; i++)
			{
				path = (path==null)?parts[i]:(path + '.' + parts[i]);
				IFoundSet relatedFoundSet = (IFoundSet) relatedFoundSets.get(path);
				if (relatedFoundSet == null)
				{
					IRecord record = searchFoundSet.getRecord(searchFoundSet.getSelectedIndex());
					if (record == null)
					{
						return null;
					}
					relatedFoundSet = record.getRelatedFoundSet(parts[i], null);
					if (relatedFoundSet == null)
					{
						return null;
					}
					relatedFoundSet = relatedFoundSet.copy(false);
					relatedFoundSet.setSelectedIndex(0);
					relatedFoundSets.put(path, relatedFoundSet);
				}
				searchFoundSet = relatedFoundSet;
			}
			searchName = parts[parts.length-1];
		}

		IRecord record = searchFoundSet.getRecord(searchFoundSet.getSelectedIndex());
		if (record == null)
		{
			return null;
		}			
		return record.getValue(searchName);
	}

	public void moveFirst() {
		relatedFoundSets = null;
	}

}
