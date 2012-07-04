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
package com.servoy.plugins.jasperreports;

import java.awt.Graphics;
import java.awt.print.Book;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;

import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRExporterParameter;
import net.sf.jasperreports.engine.JRReport;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.export.JRGraphics2DExporter;
import net.sf.jasperreports.engine.export.JRGraphics2DExporterParameter;
import net.sf.jasperreports.engine.util.JRGraphEnvInitializer;

/**
 * @author Teodor Danciu (teodord@users.sourceforge.net)
 * @version $Id$
 */
public class JasperReportsPrinter implements Printable {

    private JasperPrint jasperPrint = null;

    private int pageOffset = 0;

    protected JasperReportsPrinter(JasperPrint jrPrint) throws JRException {
	JRGraphEnvInitializer.initializeGraphEnv();

	jasperPrint = jrPrint;
    }

    public static boolean printPages(JasperPrint jrPrint, PrinterJob printJob) throws JRException {
	JasperReportsPrinter printer = new JasperReportsPrinter(jrPrint);
	return printer.printPages(printJob, 0, jrPrint.getPages().size() - 1);
    }

    private boolean printPages(PrinterJob printJob, int firstPageIndex, int lastPageIndex) throws JRException {
	boolean isOK = true;

	if (firstPageIndex < 0 || firstPageIndex > lastPageIndex || lastPageIndex >= jasperPrint.getPages().size()) {
	    throw new JRException("Invalid page index range : " + firstPageIndex + " - " + lastPageIndex + " of " + jasperPrint.getPages().size());
	}

	pageOffset = firstPageIndex;

	PageFormat pageFormat = printJob.defaultPage();
	Paper paper = pageFormat.getPaper();

	printJob.setJobName("JasperReports - " + jasperPrint.getName());

	switch (jasperPrint.getOrientationValue()) {
	case LANDSCAPE: {
	    pageFormat.setOrientation(PageFormat.LANDSCAPE);
	    paper.setSize(jasperPrint.getPageHeight(), jasperPrint.getPageWidth());
	    paper.setImageableArea(0, 0, jasperPrint.getPageHeight(), jasperPrint.getPageWidth());
	    break;
	}
	case PORTRAIT:
	default: {
	    pageFormat.setOrientation(PageFormat.PORTRAIT);
	    paper.setSize(jasperPrint.getPageWidth(), jasperPrint.getPageHeight());
	    paper.setImageableArea(0, 0, jasperPrint.getPageWidth(), jasperPrint.getPageHeight());
	}
	}

	pageFormat.setPaper(paper);

	Book book = new Book();
	book.append(this, pageFormat, lastPageIndex - firstPageIndex + 1);
	printJob.setPageable(book);
	try {
		printJob.print();
	} catch (Exception ex) {
	    throw new JRException("Error printing report.", ex);
	}

	return isOK;
    }

    public int print(Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException {
	if (Thread.currentThread().isInterrupted()) {
	    throw new PrinterException("Current thread interrupted.");
	}

	pageIndex += pageOffset;

	if (pageIndex < 0 || pageIndex >= jasperPrint.getPages().size()) {
	    return Printable.NO_SUCH_PAGE;
	}

	try {
	    JRGraphics2DExporter exporter = new JRGraphics2DExporter();
	    exporter.setParameter(JRExporterParameter.JASPER_PRINT, this.jasperPrint);
	    exporter.setParameter(JRGraphics2DExporterParameter.GRAPHICS_2D, graphics);
	    exporter.setParameter(JRExporterParameter.PAGE_INDEX, new Integer(pageIndex));
	    exporter.exportReport();
	} catch (JRException e) {
	    e.printStackTrace();
	    throw new PrinterException(e.getMessage());
	}

	return Printable.PAGE_EXISTS;
    }

}
