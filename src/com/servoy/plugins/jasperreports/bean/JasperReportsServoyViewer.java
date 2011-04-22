package com.servoy.plugins.jasperreports.bean;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.io.ByteArrayInputStream;
import java.io.InputStream;

import javax.swing.JPanel;
import javax.swing.border.Border;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.util.JRLoader;
import net.sf.jasperreports.view.JRViewer;

import com.servoy.j2db.dataprocessing.IRecord;
import com.servoy.j2db.dataui.IServoyAwareBean;
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.plugins.jasperreports.CustomizedJasperViewer;
import com.servoy.plugins.jasperreports.IJasperReportsService;
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
public class JasperReportsServoyViewer extends JPanel implements IScriptObject, IServoyAwareBean
{
	private static final long serialVersionUID = 1L;
	private IJasperReportsService service = null;
	private JasperReportsPlugin jasper = null;
	private JasperReportsProvider provider = null;
	
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
	 * (non-Javadoc)
	 * @see com.servoy.j2db.scripting.IScriptObject#getParameterNames(java.lang.String)
	 */
	public String[] getParameterNames(String methodName) {
		if ("showReport".equals(methodName))
		{
			return new String[] { "source", "report","parameters", "localeString", "moveTableOfContent" };
		}
		else if ("background".equals(methodName))
		{
			return new String[] { "background" };
		}
		else if ("border".equals(methodName))
		{
			return new String[] { "border" };
		}
		else if ("extraDirectories".equals(methodName))
		{
			return new String[] { "extraDirectories" };
		}
		else if ("font".equals(methodName))
		{
			return new String[] { "fontString" };
		}
		else if ("foreground".equals(methodName))
		{
			return new String[] { "foregroundString" };
		}
		else if ("name".equals(methodName))
		{
			return new String[] { "name" };
		}
		else if ("reportsDirectory".equals(methodName))
		{
			return new String[] { "reportsDirectory" };
		}
		else if ("setSize".equals(methodName))
		{
			return new String[] { "width", "height" };
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.servoy.j2db.scripting.IScriptObject#getSample(java.lang.String)
	 */
	public String getSample(String methodName) {
		if ("showReport".equals(methodName))
		{
			StringBuffer retval = new StringBuffer();
			retval.append("//"); //$NON-NLS-1$
			retval.append(getToolTip(methodName));
			retval.append("\n"); //$NON-NLS-1$
			retval.append("var params = new Object();\n");
			retval.append("params.SUBREPORT_DIR = \"./Subreport_Tests/\";\n");
			retval.append("var report = %%elementName%%.showReport(customers_to_orders,'/Subreport_Tests/main_report_fs.jrxml',params);\n");
			retval.append("\n"); //$NON-NLS-1$
			return retval.toString();
		}
		else if ("background".equals(methodName))
		{
			StringBuffer retval = new StringBuffer();
			retval.append("//"); //$NON-NLS-1$
			retval.append(getToolTip(methodName));
			retval.append("\n"); //$NON-NLS-1$
			retval.append("%%elementName%%.background='#00ff00';\n");
			return retval.toString();
		}
		else if ("border".equals(methodName))
		{
			StringBuffer retval = new StringBuffer();
			retval.append("//"); //$NON-NLS-1$
			retval.append(getToolTip(methodName));
			retval.append("\n"); //$NON-NLS-1$
			retval.append("%%elementName%%.border='LineBorder,4,#000000';\n");
			return retval.toString();
		}
		else if ("extraDirectories".equals(methodName))
		{
			StringBuffer retval = new StringBuffer();
			retval.append("//"); //$NON-NLS-1$
			retval.append(getToolTip(methodName));
			retval.append("\n"); //$NON-NLS-1$
			retval.append("%%elementName%%.extraDirectories='path/to/extraDirectories';\n");
			return retval.toString();
		}
		else if ("font".equals(methodName))
		{
			StringBuffer retval = new StringBuffer();
			retval.append("//"); //$NON-NLS-1$
			retval.append(getToolTip(methodName));
			retval.append("\n"); //$NON-NLS-1$
			retval.append("%%elementName%%.font='Tahoma,0,14';\n");
			return retval.toString();
		}
		else if ("foreground".equals(methodName))
		{
			StringBuffer retval = new StringBuffer();
			retval.append("//"); //$NON-NLS-1$
			retval.append(getToolTip(methodName));
			retval.append("\n"); //$NON-NLS-1$
			retval.append("%%elementName%%.foreground='#000000';\n");
			return retval.toString();
		}
		else if ("getHeight".equals(methodName))
		{
			StringBuffer retval = new StringBuffer();
			retval.append("//"); //$NON-NLS-1$
			retval.append(getToolTip(methodName));
			retval.append("\n"); //$NON-NLS-1$
			retval.append("var h = %%elementName%%.getHeight;\n");
			return retval.toString();
		}
		else if ("getLocationX".equals(methodName))
		{
			StringBuffer retval = new StringBuffer();
			retval.append("//"); //$NON-NLS-1$
			retval.append(getToolTip(methodName));
			retval.append("\n"); //$NON-NLS-1$
			retval.append("var x = %%elementName%%.getLocationX;\n");
			return retval.toString();
		}
		else if ("getLocationY".equals(methodName))
		{
			StringBuffer retval = new StringBuffer();
			retval.append("//"); //$NON-NLS-1$
			retval.append(getToolTip(methodName));
			retval.append("\n"); //$NON-NLS-1$
			retval.append("var y = %%elementName%%.getLocationY;\n");
			return retval.toString();
		}
		else if ("name".equals(methodName))
		{
			StringBuffer retval = new StringBuffer();
			retval.append("//"); //$NON-NLS-1$
			retval.append(getToolTip(methodName));
			retval.append("\n"); //$NON-NLS-1$
			retval.append("var beanName = %%elementName%%.name;\n");
			return retval.toString();
		}
		else if ("reportsDirectory".equals(methodName))
		{
			StringBuffer retval = new StringBuffer();
			retval.append("//"); //$NON-NLS-1$
			retval.append(getToolTip(methodName));
			retval.append("\n"); //$NON-NLS-1$
			retval.append("%%elementName%%.reportsDirectory = 'path/to/reportsDirectory';\n");
			return retval.toString();
		}
		else if ("setSize".equals(methodName))
		{
			StringBuffer retval = new StringBuffer();
			retval.append("//"); //$NON-NLS-1$
			retval.append(getToolTip(methodName));
			retval.append("\n"); //$NON-NLS-1$
			retval.append("%%elementName%%.setSize(800,600);\n");
			return retval.toString();
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.servoy.j2db.scripting.IScriptObject#getToolTip(java.lang.String)
	 */
	public String getToolTip(String methodName) {
		if ("showReport".equals(methodName))
		{
			return "Shows the indicated report in a JasperReports Viewer (in the Bean).";
		}
		else if ("background".equals(methodName))
		{
			return "Sets or gets the background color of the Bean.";
		}
		else if ("border".equals(methodName))
		{
			return "Sets or gets the border type, thickness and color of the Bean.";
		}
		else if ("extraDirectories".equals(methodName))
		{
			return "Sets or gets the path to the extra directories for the JasperReports plugin.";
		}
		else if ("font".equals(methodName))
		{
			return "Sets or gets the font type of the Bean.";
		}
		else if ("foreground".equals(methodName))
		{
			return "Sets or gets the foreground color.";
		}
		else if ("getHeight".equals(methodName))
		{
			return "Gets the height of the Bean.";
		}
		else if ("getLocationX".equals(methodName))
		{
			return "Gets the x-coordinate of the Bean's top-left corner location.";
		}
		else if ("getLocationY".equals(methodName))
		{
			return "Gets the y-coordinate of the Bean's top-left corner location.";
		}
		else if ("name".equals(methodName))
		{
			return "Gets or sets the name of the Bean.";
		}
		else if ("reportsDirectory".equals(methodName))
		{
			return "Gets or sets the path to the reports directory of the JasperReports plugin.";
		}
		else if ("setSize".equals(methodName))
		{
			return "Sets the size of the Bean.";
		}
		return null;
	}

	/*
	 * (non-Javadoc)
	 * @see com.servoy.j2db.scripting.IScriptObject#isDeprecated(java.lang.String)
	 */
	public boolean isDeprecated(String arg0) {
		return false;
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
	
	public String js_getName() {
		return getName();
	}
	
	public void js_setBackground(String background) {
		setBackground(PersistHelper.createColor(background));
	}

	public String js_getBackground() {
		return (getBackground() == null) ? null : PersistHelper.createColorString(getBackground());
	}
	
	public void js_setBorder(String border) {
		setBorder(ComponentFactoryHelper.createBorder(border));
	}
	
	public String js_getBorder() {
		return ComponentFactoryHelper.createBorderString(getBorder());
	}
	
	public void js_setFont(String fontString) {
		if (fontString != null) {
			setFont(PersistHelper.createFont(fontString));
		}
	}
	
	public String js_getFont() {
		return PersistHelper.createFontString(getFont());
	}

	public void js_setForeground(String foregroundString) {
		setForeground(PersistHelper.createColor(foregroundString));
	}
	
	public String js_getForeground() {
		return (getForeground() == null) ? null : 
			PersistHelper.createColorString(getForeground());
	}
	
	public void js_setLocation(int x, int y) {
		setLocation(x,y);
	}
	
	public int js_getLocationX() {
		return getLocation().x;
	}

	public int js_getLocationY() {
		return getLocation().y;
	}
	
	public void js_setSize(int width, int height) {
		setSize(width,height);
	}
	
	public int js_getWidth() {
		return getSize().width;
	}
	
	public int js_getHeight() {
		return getSize().height;
	}
	
	public void js_setTransparent(boolean transparent) {
		setTransparent(true);
	}
	
	public boolean js_getTransparent() {
		return isTransparent();
	}
	
	private void checkReportsDirectory() throws Exception
	{
		if (provider.js_getReportDirectory() == null) {
			String serverSetting = service.getReportDirectory();
			if (serverSetting == null || (serverSetting != null && ("").equals(serverSetting.trim()))) {
				String noReportsDirMsg = "Your jasper.report.directory setting has not been set.\nReport running will abort.";
				Debug.error(noReportsDirMsg);
				throw new Exception(noReportsDirMsg);
			}
			provider.js_setReportDirectory(serverSetting);
		}
	}
	
	public void js_setReportsDirectory(String reportsDirectory) throws Exception {
		provider.js_setReportDirectory(reportsDirectory);
	}
	
	public String js_getReportsDirectory() throws Exception {
		return provider.js_getReportDirectory();
	}
	
	public void js_setExtraDirectories(String extraDirectories) throws Exception {
		provider.js_setExtraDirectories(extraDirectories);
	}
	
	public String js_getExtraDirectories() throws Exception {
		return provider.js_getExtraDirectories();
	}
	
	public void js_showReport(Object source, String report, Object parameters) throws Exception {
		js_showReport(source, report, parameters, null);
	}

	public void js_showReport(Object source, String report, Object parameters, String localeString)
			throws Exception {
		js_showReport(source, report, parameters,localeString, false);
	}
	
	public void js_showReport(Object source, String report, Object parameters, String localeString,
			Boolean moveTableOfContent) throws Exception{
		showReport(source, report, parameters, localeString, moveTableOfContent);
	}
	
	// main functionality
	public void showReport(Object source, String report, Object parameters, String localeString,
			Boolean moveTableOfContent) throws Exception{
		
		checkReportsDirectory();
		byte[] jasperPrintAsByteArray = provider.runReportForBean(source, report, null, "view", parameters, localeString, moveTableOfContent);
		InputStream jasperPrintAsStream = new ByteArrayInputStream(jasperPrintAsByteArray);
		JasperPrint jp = (JasperPrint) JRLoader.loadObject(jasperPrintAsStream);
		CustomizedJasperViewer myViewer = new CustomizedJasperViewer(jp, false);
		JRViewer jrv = myViewer.getJRViewer();
		add(jrv,BorderLayout.CENTER);
	}

	

}
