package com.servoy.plugins.jasperreports.bean;

import java.awt.Image;
import java.beans.BeanInfo;
import java.beans.PropertyDescriptor;
import java.beans.SimpleBeanInfo;

import com.servoy.j2db.util.Debug;

/**
 * JasperReports Viewer Bean Info.
 * 
 * @author acostache
 *
 */
public class JasperReportsServoyViewerBeanInfo extends SimpleBeanInfo 
{
	protected Image icon16x16;
	protected Image icon32x32;

	public JasperReportsServoyViewerBeanInfo()
	{
		super();
		loadIcons();
	}
	
	
	@Override
	public Image getIcon(int iconKind)
	{
		switch (iconKind)
		{
			case BeanInfo.ICON_COLOR_16x16 :
				return icon16x16;
			case BeanInfo.ICON_COLOR_32x32 :
				return icon32x32;
		}
		return null;
	}

	protected void loadIcons()
	{
		icon16x16 = loadImage("images/_jasper_b_16x16.gif"); //$NON-NLS-1$
		icon32x32 = loadImage("images/_jasper_b_32x32.gif"); //$NON-NLS-1$
	}

	@Override
	public PropertyDescriptor[] getPropertyDescriptors() 
	{
		try
		{
			PropertyDescriptor name = new PropertyDescriptor("name", JasperReportsServoyViewer.class); //$NON-NLS-1$
			PropertyDescriptor border = new PropertyDescriptor("border", JasperReportsServoyViewer.class); //$NON-NLS-1$
			border.setDisplayName("borderType");
			PropertyDescriptor foreground = new PropertyDescriptor("foreground", JasperReportsServoyViewer.class); //$NON-NLS-1$
			PropertyDescriptor background = new PropertyDescriptor("background", JasperReportsServoyViewer.class); //$NON-NLS-1$
			PropertyDescriptor transparent = new PropertyDescriptor("transparent", JasperReportsServoyViewer.class); //$NON-NLS-1$
			PropertyDescriptor font = new PropertyDescriptor("font", JasperReportsServoyViewer.class); //$NON-NLS-1$
			font.setDisplayName("fontType");
			PropertyDescriptor loc = new PropertyDescriptor("location", JasperReportsServoyViewer.class); //$NON-NLS-1$
			PropertyDescriptor size = new PropertyDescriptor("size", JasperReportsServoyViewer.class); //$NON-NLS-1$
			PropertyDescriptor result[] = { name, border, foreground, background, transparent, font, loc, size };
			return result;
		}
		catch (Exception ex)
		{
			Debug.error("JasperReportsViewerBeanInfo: unexpected exeption: " + ex); //$NON-NLS-1$
			return null;
		}
	}
	
	

}
