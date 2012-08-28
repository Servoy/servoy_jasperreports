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
package com.servoy.plugins.jasperreports.bean;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.swing.JPanel;
import javax.swing.border.Border;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JRViewer;

import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataui.IServoyAwareBean;
import com.servoy.j2db.documentation.ServoyDocumented;
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IScriptable;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.plugins.jasperreports.IJasperReportsService;
import com.servoy.plugins.jasperreports.JR_SVY_VIEWER_DISPLAY_MODE;
import com.servoy.plugins.jasperreports.JasperReportsPlugin;
import com.servoy.plugins.jasperreports.JasperReportsProvider;

/**
 * JasperReportsServoyViewer: the real bean component.
 * 
 * Implements all necessary scripting functions and all get/set methods for indicated properties.
 * 
 * @author acostache
 *
 */
@ServoyDocumented(category= "beans", publicName = JasperReportsServoyViewerBeanInfo.BEAN_NAME, scriptingName = "beans." + JasperReportsServoyViewerBeanInfo.BEAN_NAME)
public class JasperReportsServoyViewer extends JPanel implements IScriptable, IServoyAwareBean
{
	private static final long serialVersionUID = 1L;
	private IJasperReportsService service = null;
	private JasperReportsPlugin jasper = null;
	private JasperReportsProvider provider = null;
	protected String currentDisplayMode = null;
	private String[] beanViewerExportFormats = null;
	
	public JasperReportsServoyViewer()
	{
		setLayout(new BorderLayout());
		setPreferredSize(new Dimension(700,550));
	}
	
	public boolean isReadOnly() {
		return false;
	}

	public void setValidationEnabled(boolean arg0) {
	}

	public boolean stopUIEditing(boolean arg0) {
		return true;
	}

	public void initialize(IClientPluginAccess app) {
		try {
			service = (IJasperReportsService) app.getServerService("servoy.IJasperReportService");
			jasper = (JasperReportsPlugin)app.getPluginManager().getPlugin(IClientPlugin.class,"jasperPluginRMI");
			provider = (JasperReportsProvider)jasper.getScriptObject();
		} catch (Exception e) {
			Debug.error(e);
		}
	}

	public void setSelectedRecord(IRecord arg0) {
	}
	
	/*
	 * (non-Javadoc)
	 * @see com.servoy.j2db.scripting.IScriptObject#getAllReturnedTypes()
	 */
	@SuppressWarnings("rawtypes")
	public Class[] getAllReturnedTypes() {
		return null;
	}

	/*
	 * Getters and Setters
	 */
	public String getName() {
		return super.getName();
	}

	public void setName(String name) {
		super.setName(name);
	}

	public Border getBorder() {
		return super.getBorder();
	}

	public void setBorder(Border border) {
		super.setBorder(border);
	}

	public Color getForeground() {
		return super.getForeground();
	}

	public void setForeground(Color foreground) {
		super.setForeground(foreground);
	}

	public Color getBackground() {
		return super.getBackground();
	}

	public void setBackground(Color background) {
		super.setBackground(background);
	}

	public boolean isTransparent() {
		return !super.isOpaque();
	}

	public void setTransparent(boolean transparent) {
		super.setOpaque(!transparent);
	}

	public Font getFont() {
		return super.getFont();
	}

	public void setFont(Font font) {
		super.setFont(font);
	}

	public Point getLocation() {
		return super.getLocation();
	}

	public void setLocation(int x, int y) {
		super.setLocation(x, y);
	}

	public Dimension getSize() {
		return super.getSize();
	}

	public void setSize(int width, int height) {
		super.setSize(width, height);
	}
	
	public void js_setName(String name) {
		setName(name);
	}
	
	/**
	 * Gets or sets the name of the Bean.
	 * 
	 * @sample
	 * var beanName = %%elementName%%.name;
	 * 
	 * @return the name of the Bean
	 */
	public String js_getName() {
		return getName();
	}
	
	public void js_setBackground(String background) {
		setBackground(PersistHelper.createColor(background));
	}

	/**
	 * Sets or gets the background color of the Bean.
	 * 
	 * @sample
	 * %%elementName%%.background='#00ff00';
	 * 
	 * @return the border color of the bean
	 */
	public String js_getBackground() {
		return (getBackground() == null) ? null : PersistHelper.createColorString(getBackground());
	}
	
	public void js_setBorder(String border) {
		setBorder(ComponentFactoryHelper.createBorder(border));
	}
	
	/**
	 * Sets or gets the border type, width and color.
	 * 
	 * @sample
	 * %%elementName%%.border='LineBorder,4,#000000';
	 * 
	 * @return border type, width and color
	 */
	public String js_getBorder() {
		return ComponentFactoryHelper.createBorderString(getBorder());
	}
	
	/**
	 * Sets or gets the display mode of the viewer in the Bean.
	 * 
	 * @sample
	 * %%elementName%%.displayMode = JR_SVY_VIEWER_DISPLAY_MODE.FIT_WIDTH;
	 * %%elementName%%.showReport(myDataSource,"myReport.jrxml",null);
	 * 
	 * @return the display mode of the viewer
	 */
	public String js_getDisplayMode() {
		//make a pretty name for the display mode type
		String disp = "";
		for (String d : currentDisplayMode.split("_")) 
		{ 
			d = d.substring(0, 1).toUpperCase() + d.substring(1);
			disp = disp + d + " ";
		}
		return disp.substring(0, disp.length() - 1);
	}
	
	public void js_setDisplayMode(String displayMode) {
		this.currentDisplayMode = displayMode;
	}
	
	public void js_setFont(String fontString) {
		if (fontString != null) {
			setFont(PersistHelper.createFont(fontString));
		}
	}
	
	/**
	 * Sets or gets the font type of the Bean's viewer.
	 * 
	 * @sample
	 * %%elementName%%.font='Tahoma,0,14';
	 * 
	 * @return the font type used in the viewer
	 */
	public String js_getFont() {
		return PersistHelper.createFontString(getFont());
	}

	public void js_setForeground(String foregroundString) {
		setForeground(PersistHelper.createColor(foregroundString));
	}
	
	/**
	 * Sets or gets the foreground color.
	 * 
	 * @sample
	 * %%elementName%%.foreground='#000000';
	 * 
	 * @return the foreground color
	 */
	public String js_getForeground() {
		return (getForeground() == null) ? null : 
			PersistHelper.createColorString(getForeground());
	}
	
	public void js_setLocation(int x, int y) {
		setLocation(x,y);
	}
	
	/**
	 * Gets the x-coordinate of the Bean's top-left corner location.
	 * 
	 * @sample
	 * var x = %%elementName%%.getLocationX();
	 * 
	 * @return the x-coordinate of the Bean's top-left corner location
	 */
	public int js_getLocationX() {
		return getLocation().x;
	}

	/**
	 * Gets the y-coordinate of the Bean's top-left corner location.
	 * 
	 * @sample
	 * var y = %%elementName%%.getLocationY();
	 * 
	 * @return the y-coordinate of the Bean's top-left corner location
	 */
	public int js_getLocationY() {
		return getLocation().y;
	}
	
	/**
	 * Sets the size of the Bean.
	 * 
	 * @sample
	 * %%elementName%%.setSize(800,600);
	 * 
	 * @param width the width of the bean
	 * @param height the height of the bean
	 */
	public void js_setSize(int width, int height) {
		setSize(width,height);
	}
	
	/**
	 * Gets the width of the Bean.
	 * 
	 * @sample
	 * var w = %%elementName%%.getWidth();
	 * 
	 * @return the width of the Bean
	 */
	public int js_getWidth() {
		return getSize().width;
	}
	
	/**
	 * Gets the height of the Bean.
	 * 
	 * @sample
	 * var h = %%elementName%%.getHeight();
	 * 
	 * @return the height of the Bean
	 */
	public int js_getHeight() {
		return getSize().height;
	}
	
	public void js_setTransparent(boolean transparent) {
		setTransparent(true);
	}
	
	public boolean js_getTransparent() {
		return isTransparent();
	}
	
	/**
	 * @deprecated
	 */
	@Deprecated
	public void js_setReportsDirectory(String reportsDirectory) throws Exception {
		provider.js_setReportDirectory(reportsDirectory);
	}
	
	/**
	 * @deprecated replaced by the relativeReportsDirectory property
	 */
	@Deprecated
	public String js_getReportsDirectory() throws Exception {
		return provider.js_getReportDirectory();
	}
	
	public void js_setRelativeReportsDirectory(String relativeReportsDirectory) throws Exception {
		provider.js_setRelativeReportsDirectory(relativeReportsDirectory);
	}
	
	/**
	 * Property for retrieving and setting the path to the reports directory, set by the current client, relative to the server reports directory
	 * of the Servoy JasperReports plugin.
	 * 
	 * Please refer to the same property of the Servoy JasperReports plugin for more details.
	 * 
	 * @sample
	 * %%elementName%%.relativeReportsDirectory = 'relativePath/to/serverReportsDirectory';
	 * 
	 * @return the location of the client set reports directory, relative to the server set path
	 * 
	 * @throws Exception
	 */
	public String js_getRelativeReportsDirectory() throws Exception {
		return provider.js_getRelativeReportsDirectory();
	}

	public void js_setRelativeExtraDirectories(String relativeExtraDirectories) throws Exception {
		provider.js_setRelativeExtraDirectories(relativeExtraDirectories);
	}
	
	/**
	 * Get or set the relative path or comma separated paths to the extra resource directories of the Servoy JasperReports plugin.
	 * The paths are set per client and are relative to the server corresponding directories setting.
	 * 
	 * Please refer to the same property of the Servoy JasperReports plugin for more details.
	 * 
	 * @sample
	 * %%elementName%%.extraDirectories='relative/path/to/client/extraDirectory1';
	 * 
	 * @return the relative path or paths to the client set extra directory/directories
	 * 
	 * @throws Exception
	 */
	public String js_getRelativeExtraDirectories() throws Exception {
		return provider.js_getRelativeExtraDirectories();
	}
	
	
	/**
	 * @deprecated
	 */
	@Deprecated
	public void js_setExtraDirectories(String extraDirectories) throws Exception {
		provider.js_setExtraDirectories(extraDirectories);
	}
	
	/**
	 * @deprecated replaced by the relativeExtraDirectories property
	 */
	@Deprecated
	public String js_getExtraDirectories() throws Exception {
		return provider.js_getExtraDirectories();
	}
	
	/**
	 * @clonedesc js_showReport(Object, String, Object, String, Boolean)
	 * @sampleas js_showReport(Object, String, Object, String, Boolean)
	 * 
	 * @param source the datasource (the server name, foundset or dataset) to run the report on
	 * @param report the report file to export and preview (relative to the reports directory)
	 * @param parameters the map of parameters to be used when previewing the report
	 * 
	 * @throws Exception
	 */
	public void js_showReport(Object source, String report, Object parameters) throws Exception {
		js_showReport(source, report, parameters, null);
	}

	/**
	 * @clonedesc js_showReport(Object, String, Object, String, Boolean)
	 * @sampleas js_showReport(Object, String, Object, String, Boolean)
	 * 
	 * @param source the datasource (the server name, foundset or dataset) to run the report on
	 * @param report the report file to export and preview (relative to the reports directory)
	 * @param parameters the map of parameters to be used when previewing the report
	 * @param localeString the string which specifies the locale
	 * 
	 * @throws Exception
	 */
	public void js_showReport(Object source, String report, Object parameters, String localeString)
			throws Exception {
		js_showReport(source, report, parameters,localeString, Boolean.valueOf(false));
	}
	
	/**
	 * Shows the indicated report in a JasperReports Viewer (in the Bean).
	 * 
	 * @sample
	 * var params = new Object();
	 * params.SUBREPORT_DIR = "./Subreport_Tests/";
	 * var report = %%elementName%%.showReport(customers_to_orders,"/Subreport_Tests/main_report_fs.jrxml",params);
	 * 
	 * @param source the datasource (the server name, foundset or dataset) to run the report on
	 * @param report the report file to export and preview (relative to the reports directory)
	 * @param parameters the map of parameters to be used when previewing the report
	 * @param localeString the string which specifies the locale
	 * @param moveTableOfContent true in order to move the table of contents, false otherwise
	 * 
	 * @throws Exception
	 */
	public void js_showReport(Object source, String report, Object parameters, String localeString,
			Boolean moveTableOfContent) throws Exception{
		showReport(source, report, parameters, localeString, moveTableOfContent);
	}
	
	public void js_setBeanViewerExportFormats(String[] saveContribs) throws Exception {
		beanViewerExportFormats = saveContribs;
	}
	
	/**
	 * Gets or gets the file save/export formats of the Bean's viewer.
	 * 
	 * @sample
	 * //also see plugins.jasperPluginRMI.viewerExportFormats
	 * %%elementName%%.beanViewerExportFormats=[OUTPUT_FORMAT.PDF, OUTPUT_FORMAT.XLS];
	 * 
	 * @return the file save/export formats of the viewer
	 * 
	 * @throws Exception
	 */
	public String[] js_getBeanViewerExportFormats() throws Exception {
		return beanViewerExportFormats;
	}
	
	protected JRViewerWrapper jrv;
	private class JRViewerWrapper extends JRViewer
	{
		private static final long serialVersionUID = 1L;

		public JRViewerWrapper(JasperPrint jp)
		{
			super(jp);
		}
		
		public void btnFitPagePressed()
		{
			btnFitPage.setSelected(true);
			btnActualSize.setSelected(false);
			btnFitWidth.setSelected(false);
			cmbZoom.setSelectedIndex(-1);
		}
		
		public boolean isBtnFitPagePressed()
		{
			return btnFitPage.isSelected();
		}
		
		public void selectBtnFitPage()
		{
			btnFitPage.setSelected(true);
		}
		
		
		public void btnFitWidthPressed()
		{
			btnFitWidth.setSelected(true);
			btnFitPage.setSelected(false);
			btnActualSize.setSelected(false);
			cmbZoom.setSelectedIndex(-1);
		}
		
		public boolean isBtnFitWidthPressed()
		{
			return btnFitWidth.isSelected();
		}
		
		public void selectBtnFitWidth()
		{
			btnFitWidth.setSelected(true);
		}
		
		public void btnActualSizePressed()
		{
			btnActualSize.setSelected(true);
			btnFitPage.setSelected(false);
			btnFitWidth.setSelected(false);
			cmbZoom.setSelectedIndex(-1);
			setZoomRatio(1);
		}
		
		public boolean isBtnActualSizePressed()
		{
			return btnActualSize.isSelected();
		}
		
		public void selectBtnActualSize()
		{
			btnActualSize.setSelected(true);
		}
	}
	
	// main functionality
	public void showReport(Object source, String report, Object parameters, String localeString,
			Boolean moveTableOfContent) throws Exception{
		
		byte[] jasperPrintAsByteArray = provider.runReportForBean(source, report, null, "view", parameters, localeString, moveTableOfContent);
		InputStream jasperPrintAsStream = new ByteArrayInputStream(jasperPrintAsByteArray);
		JasperPrint jp = (JasperPrint) JRLoader.loadObject(jasperPrintAsStream);
		jrv = new JRViewerWrapper(jp);
		if (getComponents().length > 0) removeAll();
		add(jrv, BorderLayout.CENTER);
		
		jrv.addComponentListener(new ComponentListener() {
			public void componentResized(ComponentEvent e) { 
				// no buttons selected (yet)
				if (!jrv.isBtnActualSizePressed() && !jrv.isBtnFitPagePressed() && !jrv.isBtnFitWidthPressed())
				{ 
					if (JR_SVY_VIEWER_DISPLAY_MODE.FIT_PAGE.equals(currentDisplayMode)) jrv.selectBtnFitPage();
					else if (JR_SVY_VIEWER_DISPLAY_MODE.FIT_WIDTH.equals(currentDisplayMode)) jrv.selectBtnFitWidth();
					else if (JR_SVY_VIEWER_DISPLAY_MODE.ACTUAL_PAGE_SIZE.equals(currentDisplayMode)) jrv.selectBtnActualSize();
				}

				// if display mode set via scripting && button has been pressed, we resize accordingly
				// else we resize with respect to the button pressed (we let the viewer do its job alone) 
				if (JR_SVY_VIEWER_DISPLAY_MODE.FIT_PAGE.equals(currentDisplayMode) && jrv.isBtnFitPagePressed())
				{
					jrv.setFitPageZoomRatio();
					jrv.btnFitPagePressed();
				}
				else if (JR_SVY_VIEWER_DISPLAY_MODE.FIT_WIDTH.equals(currentDisplayMode) && jrv.isBtnFitWidthPressed())
				{
					jrv.setFitWidthZoomRatio();
					jrv.btnFitWidthPressed();
				}
				else if (JR_SVY_VIEWER_DISPLAY_MODE.ACTUAL_PAGE_SIZE.equals(currentDisplayMode) && jrv.isBtnActualSizePressed())
				{
					jrv.btnActualSizePressed();
				}
			}

			public void componentMoved(ComponentEvent e) {
			}

			public void componentShown(ComponentEvent e) {
			}

			public void componentHidden(ComponentEvent e) {
			}});
		
		if (beanViewerExportFormats != null)
			JasperReportsProvider.setViewerSaveContributors(jrv, beanViewerExportFormats);
		
		this.validate();
	}

	/**
	 * This is a readonly property which returns the bean version.
	 * The bean version indicates which version of the Servoy JasperReports plugin the bean should be used with.
	 * 
	 * @sample
	 * application.output(%%elementName%%.beanVersion);
	 * 
	 * @return the version of the bean; this should be in sync with the version of the plugin used
	 */
	public String js_getBeanVersion()
	{
		return "Bean version: " + provider.js_getPluginVersion() + ".\nThis version number indicates the bean should be used together with the Servoy JasperReports plugin version " + provider.js_getPluginVersion() + "."; 
	}
	
	public void js_setBeanVersion(String version)
	{
		//do nothing
	}
}
