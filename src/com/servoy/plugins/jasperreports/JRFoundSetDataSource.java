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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.servoy.j2db.dataprocessing.IFoundSet;
import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.util.ServoyException;

/**
 * JRDataSource wrapper for foundsets.
 * @author rgansevles
 *
 */
public class JRFoundSetDataSource extends AbstractServoyDataSource {

	private final FoundsetWithIndex foundSet;
	private Map<String, FoundsetWithIndex> relatedFoundSets;

	public  JRFoundSetDataSource (IClientPluginAccess pluginAccess, IFoundSet foundSet)
	{
		super(pluginAccess);
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

	/**
	 * Get the value of the dataProvider, may be prefixed with relation name.
	 * @param dataProviderId
	 * @return the dataprovider value for the specified dataprovider id
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

	@Override
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
