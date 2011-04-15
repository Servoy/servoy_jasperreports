package com.servoy.plugins.jasperreports;

import java.awt.Image;

import net.sf.jasperreports.engine.JasperReport;
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
	
	public static JasperReport loadReport(String location) throws Exception {
		
		IJasperReportsService jasperReportsService = JasperReportsProvider.jasperReportsLocalService.get();
		String jasperReportsClientId = JasperReportsProvider.jasperReportsLocalClientID.get();
		
		if (jasperReportsService == null || jasperReportsClientId == null)
		{	
			return null;
		}
		
		JasperReport report = jasperReportsService.loadReport(jasperReportsClientId, location);
				
		return report;
	}
	
}
