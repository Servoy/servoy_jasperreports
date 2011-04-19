package com.servoy.plugins.jasperreports;

import java.util.Locale;

import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.view.JRViewer;
import net.sf.jasperreports.view.JasperViewer;

/**
 * Modified JasperViewer, so that we have access to the JRViewer instance
 * inside.
 * 
 */
public class CustomizedJasperViewer extends JasperViewer {

	private static final long serialVersionUID = 1L;

	public CustomizedJasperViewer(JasperPrint jasperPrint, boolean isExitOnClose, Locale locale) {
		super(jasperPrint, isExitOnClose, locale);
	}

	public CustomizedJasperViewer(JasperPrint jasperPrint, boolean isExitOnClose) {
		super(jasperPrint, isExitOnClose);
	}

	public void setJRViewer(JRViewer jrv) {
		super.viewer = jrv;
	}

	public JRViewer getJRViewer() {
		return super.viewer;
	}
}