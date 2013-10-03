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

import net.sf.jasperreports.engine.fill.JRBaseFiller;
import net.sf.jasperreports.engine.fill.JRFillSubreport;
import net.sf.jasperreports.engine.fill.JRSubreportRunResult;
import net.sf.jasperreports.engine.fill.JRSubreportRunner;
import net.sf.jasperreports.engine.fill.JRSubreportRunnerFactory;
import net.sf.jasperreports.engine.fill.JRThreadSubreportRunner;

import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.J2DBGlobals;

/**
 * JRSubreportRunnerFactory extension that copies thread-related settings to the sub report runner thread.
 * 
 * @author rgansevles
 *
 */
public class ServoyThreadSubreportRunnerFactory implements JRSubreportRunnerFactory {

	private ClassLoader pluginContextClassLoader;
	private IJasperReportsService jasperReportsService;
	private String jasperReportsClientId;
	private IServiceProvider application;
	
	public JRSubreportRunner createSubreportRunner(JRFillSubreport fillSubreport, JRBaseFiller subreportFiller)
	{
		return new JRThreadSubreportRunner(fillSubreport, subreportFiller){

			@Override
			public JRSubreportRunResult start()
			{
				pluginContextClassLoader = Thread.currentThread().getContextClassLoader();
				jasperReportsService = JasperReportsProvider.jasperReportsLocalService.get();
				jasperReportsClientId = JasperReportsProvider.jasperReportsLocalClientID.get();
				// internal Servoy API, needed for in memory data sources
				application = J2DBGlobals.getServiceProvider();
				return super.start();
			}

			@Override
			public void run()
			{
				Thread.currentThread().setContextClassLoader(pluginContextClassLoader);
				JasperReportsProvider.jasperReportsLocalService.set(jasperReportsService);
				JasperReportsProvider.jasperReportsLocalClientID.set(jasperReportsClientId);
				J2DBGlobals.setServiceProvider(application);
				super.run();
			}};
	}

}
