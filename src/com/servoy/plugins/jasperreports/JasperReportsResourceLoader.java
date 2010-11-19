package com.servoy.plugins.jasperreports;

import java.awt.Image;
import java.rmi.RemoteException;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.util.JRImageLoader;

public class JasperReportsResourceLoader {

	public static Image loadImage(String image) throws Exception {
		
		IJasperReportsService jasperReportsService = JasperReportsProvider.jasperReportsLocalService.get();
		String jasperReportsClientId = JasperReportsProvider.jasperReportsLocalClientID.get();
		
		if (jasperReportsService == null || jasperReportsClientId == null)
		{	
			return null;
		}
		
		byte[] imgBytes = jasperReportsService.loadImage(jasperReportsClientId, image);
		
		return JRImageLoader.loadImage(imgBytes);
	}
	
}
