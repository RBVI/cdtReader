package edu.ucsf.rbvi.cdtReader.internal.tasks;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

import org.cytoscape.io.read.AbstractCyNetworkReader;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.view.model.CyNetworkViewFactory;

import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;

public class CDTReaderTask extends AbstractCyNetworkReader {
	public final BufferedReader input;

	public CyNetwork finishedNetwork = null;

	private final CDTImporter cdtImporter;
	private final String inputName;

	public CDTReaderTask(final InputStream inputStream, final String name, final CyNetworkViewFactory cyNetworkViewFactory,
	                     final CyNetworkFactory cyNetworkFactory, final CyNetworkManager cyNetworkManager,
											 final CyRootNetworkManager cyRootNetworkManager) {
		super(inputStream, cyNetworkViewFactory, cyNetworkFactory, cyNetworkManager, cyRootNetworkManager);
		try {
			input = new BufferedReader(new InputStreamReader(inputStream, "UTF-8"));
		} catch(UnsupportedEncodingException e) {
			// Should never happen!
			throw new RuntimeException(e.getMessage());
		}

		this.inputName = name;
		cdtImporter = new CDTImporter(cyNetworkFactory);
	}

	@Override
	public void run(TaskMonitor taskMonitor) {
		networks = new CyNetwork[1];
		networks[0] = cdtImporter.readCDT(taskMonitor, input, inputName, null);
	}
 
	@Override
	public CyNetworkView buildCyNetworkView(CyNetwork network) {
		// Nothing fancy
		return cyNetworkViewFactory.createNetworkView(network);
	}

	@ProvidesTitle
	public String getTitle() {return "CDTReader";}
}
