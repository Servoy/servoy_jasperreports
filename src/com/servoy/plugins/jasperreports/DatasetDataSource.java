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

import java.util.Arrays;

import net.sf.jasperreports.engine.JRException;

import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.util.ServoyException;

/**
 * JRDataSource wrapper for IDataSet.
 * 
 * @author rgansevles
 *
 */
public class DatasetDataSource extends AbstractServoyDataSource {
	private final IDataSet dataSet;
	int index;

	public DatasetDataSource(IClientPluginAccess pluginAccess, IDataSet dataSet) {
		super(pluginAccess);
		this.dataSet = dataSet;
		moveFirst();
	}

	public boolean next() throws JRException {
		if (index + 1 >= dataSet.getRowCount()) {
			return false;
		}
		index++;
		return true;
	}

	public void moveFirst() {
		index = -1;
	}

	@Override
	protected Object getDataProviderValue(String dataProviderId)
			throws ServoyException {
		if (index < 0 || index >= dataSet.getRowCount()) {
			// should not happen, next() checked this
			return null;
		}
		int col = Arrays.asList(dataSet.getColumnNames()).indexOf(
				dataProviderId);
		if (col < 0) {
			// try dataprovider id as column number (1-based)
			try {
				col = Integer.valueOf(dataProviderId).intValue() - 1;
			} catch (NumberFormatException e) {
				// not a number
			}
		}
		if (col >= 0) {
			Object[] row = dataSet.getRow(index);
			if (row != null && col < row.length) {
				return row[col];
			}
		}
		return null;
	}
}
