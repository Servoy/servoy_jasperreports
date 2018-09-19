package com.servoy.plugins.jasperreports;

import net.sf.jasperreports.engine.JRDataSource;
import net.sf.jasperreports.engine.JRException;
import net.sf.jasperreports.engine.JRRewindableDataSource;

/**
 * This is a helper class for datasources
 *
 * @author gboros
 *
 */
public class JasperReportsDataSource
{
	/**
	 * This is a helper method for rewinding the used (rewindable) datasource.
	 * A datasource is rewindable if it implements net.sf.jasperreports.engine.JRRewindableDataSource.
	 * This method is needed when using the same datasource for two subreports.
	 * See also http://community.jaspersoft.com/questions/521291/can-2-subreports-share-same-datasource
	 */
	public static JRRewindableDataSource rewindJRDataSource(JRRewindableDataSource dataSource) throws JRException {
		dataSource.moveFirst();
		return dataSource;
	}	
	
	/**
	 * Helper method to create related datasource. 
	 * @param dataSource parent datasource
	 * @param relationName relation name
	 * @return realated datasource
	 * @throws JRException
	 */
	public static JRDataSource createRelatedJRDataSource(JRDataSource dataSource, String relationName) throws JRException {
		return dataSource instanceof JRFoundSetDataSource ? ((JRFoundSetDataSource)dataSource).createRelatedJRDataSource(relationName) : null;
	}
}
