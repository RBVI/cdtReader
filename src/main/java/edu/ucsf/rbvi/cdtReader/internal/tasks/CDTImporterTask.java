package edu.ucsf.rbvi.cdtReader.internal.tasks;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.cytoscape.io.read.AbstractCyNetworkReader;

import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;

public class CDTImporterTask extends AbstractTask {

	@Tunable (description="CDT file", required=true, params="input=true", gravity=1.0)
	public File cdtFile;

	@Tunable (description="Rows and columns are nodes", required=true, gravity=2.0)
	public boolean columnNodes = false;

	@Tunable (description="GTR file", required=false, params="input=true", gravity=3.0)
	public File gtrFile;

	@Tunable (description="ATR file", required=false, params="input=true", gravity=4.0)
	public File atrFile;

	final CyNetworkFactory cyNetworkFactory;
	final CyNetworkManager cyNetworkManager;
	final CDTImporter importer;

	public CDTImporterTask(final CyNetworkFactory cyNetworkFactory, final CyNetworkManager cyNetworkManager,
											   final CyRootNetworkManager cyRootNetworkManager) {
		this.cyNetworkFactory = cyNetworkFactory;
		this.cyNetworkManager = cyNetworkManager;
		importer = new CDTImporter(cyNetworkFactory);
	}

	@Override
	public void run(TaskMonitor taskMonitor) {
		// Get an input stream
		BufferedReader cdtReader;
	 
		try {
			cdtReader	= new BufferedReader(new FileReader(cdtFile));
		} catch(FileNotFoundException fnf) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Can't find the file '"+cdtFile.getName()+"'");
			return;
		}
		// Read our network from the CDT file
		CyNetwork network = importer.readCDT(taskMonitor, cdtReader, cdtFile.getName(), columnNodes);
		CyTable networkTable = network.getDefaultNetworkTable();

		// Get our array order
		List<String> arrayOrder = importer.getArrayList();
		if (arrayOrder != null && arrayOrder.size() > 0) {
			networkTable.createListColumn("__arrayOrder", String.class, false);
			network.getRow(network).set("__arrayOrder", arrayOrder);
		}
		// Get our node order
		List<String> nodeOrder = importer.getGeneList();
		if (nodeOrder != null && nodeOrder.size() > 0) {
			networkTable.createListColumn("__nodeOrder", String.class, false);
			network.getRow(network).set("__nodeOrder", nodeOrder);
		}

		// If we have one, read our gtrFile
		if (gtrFile != null) {
			BufferedReader gtrReader;
			try {
				gtrReader = new BufferedReader(new FileReader(gtrFile));
			} catch(FileNotFoundException fnf) {
				taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Can't find the file '"+gtrFile.getName()+"'");
				return;
			}
			List<String> gtr = importer.readGTR(taskMonitor, gtrReader, gtrFile.getName());
			// Update our cluster
			if (gtr != null && gtr.size() > 0) {
				networkTable.createListColumn("__nodeClusters", String.class, false);
				network.getRow(network).set("__nodeClusters", gtr);
				networkTable.createColumn("__clusterType", String.class, false);
				network.getRow(network).set("__clusterType", "hierarchical");
			}
		}
		// If we have one, read our atrFile
		if (atrFile != null) {
			BufferedReader atrReader;
			try {
				atrReader = new BufferedReader(new FileReader(atrFile));
			} catch(FileNotFoundException fnf) {
				taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Can't find the file '"+atrFile.getName()+"'");
				return;
			}
			List<String> atr = importer.readATR(taskMonitor, atrReader, atrFile.getName());
			// Update our cluster
			if (atr != null && atr.size() > 0) {
				networkTable.createListColumn("__attrClusters", String.class, false);
				network.getRow(network).set("__attrClusters", atr);
			}
		}

		cyNetworkManager.addNetwork(network);
	}

	@ProvidesTitle
	public String getTitle() {return "CDT Importer";}
}
