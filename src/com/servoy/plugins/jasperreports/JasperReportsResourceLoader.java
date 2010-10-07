package com.servoy.plugins.jasperreports;

import java.awt.Image;
import java.rmi.RemoteException;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.util.JRImageLoader;

public class JasperReportsResourceLoader {

	public static Image loadImage(String image) throws JRException, RemoteException {
		IJasperReportsService jasperReportsService = JasperReportsProvider.jasperReportLocalService.get();
		if (jasperReportsService == null)
		{	
			return null;
		}
		byte[] imgBytes = jasperReportsService.loadImage(image);
		return JRImageLoader.loadImage(imgBytes);
	}
	
}
