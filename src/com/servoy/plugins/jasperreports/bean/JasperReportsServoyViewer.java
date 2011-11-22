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
import com.servoy.j2db.plugins.IClientPlugin;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.plugins.jasperreports.JR_SVY_VIEWER_DISPLAY_MODE;
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
	protected String currentDisplayMode = null;
	
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
		else if ("displayMode".equals(methodName))
		{
			return new String[] { "displayMode" };
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
		else if ("displayMode".equals(methodName))
		{
			StringBuffer retval = new StringBuffer();
			retval.append("//"); //$NON-NLS-1$
			retval.append(getToolTip(methodName));
			retval.append("\n"); //$NON-NLS-1$
			retval.append("%%elementName%%.displayMode = JR_SVY_VIEWER_DISPLAY_MODE.FIT_WIDTH;\n");
			retval.append("%%elementName%%.showReport(myDataSource,\"/myReport.jrxml\",null);\n");
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
		else if ("displayMode".equals(methodName))
		{
			return "Sets or gets the display mode of the Bean.";
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
		
		this.validate();
	}

}
