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

import java.beans.PropertyChangeEvent;
import java.util.Properties;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.plugins.PluginException;
import com.servoy.j2db.preference.PreferencePanel;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.util.Debug;

/**
 * IClientPlugin impl.
 */
public class JasperReportsPlugin implements IClientPlugin {

	public static final String PLUGIN_NAME = "jasperReports";
	
	private IClientPluginAccess application;

	private JasperReportsProvider impl;

	private String jasperReportsDirectory;
	
	private String jasperExtraDirectories;

	private IJasperReportsService jasperReportService;

	public String getJasperReportsDirectory(){
		connectJasperService(); // is initialized with server setting
		return jasperReportsDirectory;
	}
	
	public String getJasperExtraDirectories() {
		connectJasperService(); // is initialized with server setting
		return jasperExtraDirectories;
	}

	public void setJasperReportsDirectory(String dir){
		jasperReportsDirectory = dir;
	}
	
	public void setJasperExtraDirectories(String dirs) {
		jasperExtraDirectories = dirs;
	}

	public void initialize(IClientPluginAccess app) throws PluginException {
		application = app;
	}
	
	public IJasperReportsService connectJasperService()  {

		Debug.trace("JasperTrace: service connection initialize");
		// create if not yet created
		if (jasperReportService == null) {
			ClassLoader savedCl = Thread.currentThread().getContextClassLoader();
			try
			{
				Thread.currentThread().setContextClassLoader(getIClientPluginAccess().getPluginManager().getClassLoader());
				jasperReportService = (IJasperReportsService) application.getServerService("servoy.IJasperReportService");
				if (jasperReportsDirectory == null) jasperReportsDirectory = jasperReportService.getReportDirectory();
				if (jasperExtraDirectories == null) jasperExtraDirectories = jasperReportService.getExtraDirectories();
			}
			catch (Exception ex)
			{
				Debug.error(ex);
				throw new RuntimeException(
						"JasperTrace: Jasper Exception: Cannot connect to service-server");
			}
			finally
			{
				Thread.currentThread().setContextClassLoader(savedCl);
			}
		}

		// in case the server is not started in developer
		if (jasperReportService == null) {
			System.err.println("JasperTrace: Jasper Exception: No service running");
			throw new RuntimeException("JasperTrace: Jasper Exception: No service running");
		}
		Debug.trace("JasperTrace: service connection found");
		return jasperReportService;
	}

	IClientPluginAccess getIClientPluginAccess()
	{
		return application;
	}

	public PreferencePanel[] getPreferencePanels() {
		return null;// none
	}

	public String getName() {
		return "jasperReports";
	}

	public Icon getImage() {
		java.net.URL iconUrl = this.getClass().getResource("images/jasper16x16.gif");
		if (iconUrl != null) {
			return new ImageIcon(iconUrl);
		}
		return null;
	}

	public IScriptable getScriptObject(){
		try{

			if (impl == null)
				impl = new JasperReportsProvider(this);
		}
		catch (Exception e){
			Debug.error(e);
		}
		return impl;
	}

	public void load() throws PluginException {
	}

	public void unload() throws PluginException {
		application = null;
		impl = null;
	}

	public Properties getProperties() {
		Properties props = new Properties();
		props.put(DISPLAY_NAME, getName());
		return props;
	}

	/* (non-Javadoc)
	 * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
	 */
	public void propertyChange(PropertyChangeEvent evt) {
		// ignore
	}

}
