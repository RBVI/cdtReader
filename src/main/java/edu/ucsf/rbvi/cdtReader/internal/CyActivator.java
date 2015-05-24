package edu.ucsf.rbvi.cdtReader.internal;

import static org.cytoscape.work.ServiceProperties.COMMAND;
import static org.cytoscape.work.ServiceProperties.COMMAND_DESCRIPTION;
import static org.cytoscape.work.ServiceProperties.COMMAND_NAMESPACE;
import static org.cytoscape.work.ServiceProperties.ID;
import static org.cytoscape.work.ServiceProperties.IN_MENU_BAR;
import static org.cytoscape.work.ServiceProperties.INSERT_SEPARATOR_BEFORE;
import static org.cytoscape.work.ServiceProperties.MENU_GRAVITY;
import static org.cytoscape.work.ServiceProperties.PREFERRED_MENU;
import static org.cytoscape.work.ServiceProperties.TITLE;

import java.util.Properties;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.BasicCyFileFilter;
import org.cytoscape.io.DataCategory;
import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.util.StreamUtil;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.work.TaskFactory;

import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import edu.ucsf.rbvi.cdtReader.internal.tasks.CDTReaderTaskFactory;

public class CyActivator extends AbstractCyActivator {

	public CyActivator() {
		super();
	}

	public void start(BundleContext bc) {
		final StreamUtil streamUtil = getService(bc, StreamUtil.class);
		final CyServiceRegistrar serviceRegistrar = getService(bc, CyServiceRegistrar.class);

		// This is for the basic reader.  Note that we'll also load a more advanced one below
		final BasicCyFileFilter cdtFileFilter = new BasicCyFileFilter(new String[] { "cdt" },
		                              new String[] { "application/cdt" }, "CDT", DataCategory.NETWORK, streamUtil);
		final CDTReaderTaskFactory cdtReaderFactory = new CDTReaderTaskFactory(serviceRegistrar, cdtFileFilter);

		Properties cdtReaderProps = new Properties();
		cdtReaderProps.put(ID, "cdtNetworkReaderFactory");
		registerService(bc, cdtReaderFactory, InputStreamTaskFactory.class, cdtReaderProps);

		Properties cdtImporterProps = new Properties();
		cdtImporterProps.setProperty(PREFERRED_MENU, "Apps.CDTImporter");
		cdtImporterProps.setProperty(TITLE, "Import CDT files");
		registerService(bc, cdtReaderFactory, TaskFactory.class, cdtImporterProps);

	}
}
