package edu.ucsf.rbvi.cdtReader.internal.tasks;

import java.io.InputStream;

import org.cytoscape.io.CyFileFilter;
import org.cytoscape.io.read.AbstractInputStreamTaskFactory;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.TaskIterator;

public class CDTReaderTaskFactory extends AbstractInputStreamTaskFactory implements TaskFactory {
	public final CyFileFilter cdtFilter;
	public final CyServiceRegistrar cyRegistrar;

	public CDTReaderTaskFactory(final CyServiceRegistrar cyRegistrar, final CyFileFilter cdtFilter) {
		super(cdtFilter);
		this.cdtFilter = cdtFilter;
		this.cyRegistrar = cyRegistrar;
	}

	@Override
	public TaskIterator createTaskIterator(InputStream is, String inputName) {
		CyNetworkViewFactory viewFactory = cyRegistrar.getService(CyNetworkViewFactory.class);
		CyNetworkFactory netFactory = cyRegistrar.getService(CyNetworkFactory.class);
		CyNetworkManager netManager = cyRegistrar.getService(CyNetworkManager.class);
		CyRootNetworkManager netRootManager = cyRegistrar.getService(CyRootNetworkManager.class);

		TaskIterator ti = new TaskIterator(new CDTReaderTask(is, inputName, viewFactory, netFactory,
		                                                     netManager, netRootManager));
		return ti;
	}

	@Override
	public TaskIterator createTaskIterator() {
		CyNetworkFactory netFactory = cyRegistrar.getService(CyNetworkFactory.class);
		CyNetworkManager netManager = cyRegistrar.getService(CyNetworkManager.class);
		CyRootNetworkManager netRootManager = cyRegistrar.getService(CyRootNetworkManager.class);
		return new TaskIterator(new CDTImporterTask(netFactory, netManager, netRootManager));
	}

	@Override
	public boolean isReady() { return true; }

}
