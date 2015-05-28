package edu.ucsf.rbvi.cdtReader.internal.tasks;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
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

import org.cytoscape.work.TaskMonitor;

public class CDTImporter {
	public static String AID = "AID";
	public static String EWEIGHT = "EWEIGHT";
	public static String GID = "GID";
	public static String GWEIGHT = "GWEIGHT";
	public static String NAME = "NAME";

	private final CyNetworkFactory cyNetworkFactory;

	private Map<String, String> aidMap = null;
	private Map<String, String> gidMap = null;
	private List<String> arrayList = null;
	private List<String> geneList = null;
	private String delimiter = null;

	public CDTImporter(final CyNetworkFactory cyNetworkFactory) {
		this.cyNetworkFactory = cyNetworkFactory;
	}

	public CyNetwork readCDT(TaskMonitor taskMonitor, BufferedReader reader,
	                         String inputName, Boolean columnsAreNodes) {
		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Reading CDT file '"+inputName+"'");

		Map<String, CyNode> nodeMap = new HashMap<>();
		gidMap = new HashMap<>();
		arrayList = new ArrayList<>();
		geneList = new ArrayList<>();

		// Create our network
		CyNetwork network = cyNetworkFactory.createNetwork();
		network.getRow(network).set(CyNetwork.NAME, inputName);

		try {
			// Read in the first three rows
			String[] headerRow = readRow(reader);
			
			int gWeightColumn = findColumn(headerRow, GWEIGHT);
			int nameColumn = findColumn(headerRow, NAME);

			// If we don't have a GWEIGHT column, it's not a CDT file
			if (gWeightColumn < 0) {
				taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Can't find GWEIGHT column");
				throw new RuntimeException("Can't find GWEIGHT column");
			}

			// OK, we probably have a CDT file, Find the end of the
			// headers
			boolean aidSeen = false;
			boolean eWeightSeen = false;
			boolean symmetric = false;
			int rowNumber = 0;
			String[] aidRow;

			String[] row;
			while ((row = readRow(reader)) != null) {
				if (!aidSeen && row[0].equals(AID)) {
					aidSeen = true;
					aidRow = row;
					aidMap = createAIDMap(aidRow, headerRow, gWeightColumn);
					continue;
				} else if (aidSeen && !eWeightSeen && row[0].equals(EWEIGHT)) {
					eWeightSeen = true;
					continue;
				} else if (!aidSeen)
					continue;

				if (rowNumber == 0) {
					// Edge Data?
					if (columnsAreNodes != Boolean.FALSE && row[nameColumn].equals(headerRow[gWeightColumn+1])) {
						// yes
						symmetric = true;
						columnsAreNodes = true;
						createAllNodes(network, headerRow, gWeightColumn, nodeMap);
					} else if (columnsAreNodes == Boolean.TRUE) {
						symmetric = false;
						createAllNodes(network, headerRow, gWeightColumn, nodeMap);
					}

					createColumns(network, headerRow, gWeightColumn, nameColumn, columnsAreNodes);
				}

				gidMap.put(row[0], row[nameColumn]);
				createRow(network, headerRow, row, gWeightColumn, nameColumn, columnsAreNodes, nodeMap);

				rowNumber++;
			}

		} catch(IOException ioe) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, 
			                        "Failed to read file '"+inputName+"': "+ioe.getMessage());
			return null;
		}

		taskMonitor.showMessage(TaskMonitor.Level.INFO, 
		                        "Imported network has "+network.getNodeCount()+
														" nodes and "+network.getEdgeCount()+" edges");
		
		return network;
	}

	public List<String> getGeneList() {return geneList;}
	public List<String> getArrayList() {return arrayList;}

	public List<String> readGTR(TaskMonitor taskMonitor, BufferedReader reader, String name) {
		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Reading GTR file '"+name+"'");

		if (gidMap == null || gidMap.size() == 0) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "No GID Map: did CDT file import fail?");
			return null;
		}

		try {
			List<String> treeList = readTree(reader, gidMap);
			taskMonitor.showMessage(TaskMonitor.Level.INFO, "Read "+treeList.size()+" tree elements");
			return treeList;
		} catch (IOException ioe) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Error reading ATR file: "+ioe.getMessage());
		}

		return null;
	}

	public List<String> readATR(TaskMonitor taskMonitor, BufferedReader reader, String name) {
		taskMonitor.showMessage(TaskMonitor.Level.INFO, "Reading ATR file '"+name+"'");

		if (aidMap == null || aidMap.size() == 0) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "No AID Map: did CDT file import fail?");
			return null;
		}

		try {
			List<String> treeList = readTree(reader, aidMap);
			taskMonitor.showMessage(TaskMonitor.Level.INFO, "Read "+treeList.size()+" tree elements");
			return treeList;
		} catch (IOException ioe) {
			taskMonitor.showMessage(TaskMonitor.Level.ERROR, "Error reading ATR file: "+ioe.getMessage());
		}

		return null;
	}

	void createAllNodes(CyNetwork network, String[] headerRow, int gWeightColumn, 
	                    Map<String, CyNode> nodeMap) {
		CyTable nodeTable = network.getDefaultNodeTable();
		for (int i = gWeightColumn+1; i < headerRow.length; i++) {
			createNode(network, headerRow[i], nodeMap);
		}
	}

	void createColumns(CyNetwork net, String[] headerRow, int gWeightColumn, int nameColumn, boolean nodeColumns) {
		CyTable nodeTable = net.getDefaultNodeTable();
		for (int i = 0; i < gWeightColumn; i++) {
			if (i == nameColumn)
				continue;
			nodeTable.createColumn(headerRow[i], String.class, false);
		}

		if (nodeColumns) {
			CyTable edgeTable = net.getDefaultEdgeTable();
			edgeTable.createColumn("weight", Double.class, false);
			net.getDefaultNetworkTable().createColumn("__clusterEdgeWeight", String.class, false);
			net.getRow(net).set("__clusterEdgeWeight", "edge.weight");
		}

		for (int i = gWeightColumn+1; i < headerRow.length; i++) {
			arrayList.add(headerRow[i]);
			if (!nodeColumns)
				nodeTable.createColumn(headerRow[i], Double.class, false);
		}
	}

	void createRow(CyNetwork net, String[] headerRow, String[] dataRow, int gWeightColumn, 
	               int nameColumn, boolean nodeColumns, Map<String, CyNode> nodeMap) {
		String sourceName = dataRow[nameColumn];
		geneList.add(sourceName);
		CyNode sourceNode = null;
		if (nodeMap.containsKey(sourceName))
			sourceNode = nodeMap.get(sourceName);
		else
			sourceNode = createNode(net, sourceName, nodeMap);

		if (sourceNode == null) return; //??

		// Write the "standard" information
		for (int i = 0; i < gWeightColumn; i++) {
			if (i == nameColumn) 
				continue;
			String column = headerRow[i];
			String data = dataRow[i];
			net.getRow(sourceNode).set(column, data);
		}

		if (nodeColumns) {
			// Now create the edges
			for (int i = gWeightColumn+1; i < headerRow.length; i++) {
				CyNode targetNode = nodeMap.get(headerRow[i]);
				if (targetNode == null) continue;
				if (dataRow[i] != null && dataRow[i].length() > 1) {
					CyEdge edge = net.addEdge(sourceNode, targetNode, false);
					net.getRow(edge).set("weight", new Double(dataRow[i]));
					net.getRow(edge).set(CyNetwork.NAME, sourceName+" (weight) "+headerRow[i]);
					net.getRow(edge).set(CyRootNetwork.SHARED_NAME, sourceName+" (weight) "+headerRow[i]);
				}
			}
		} else {
			for (int i = gWeightColumn+1; i < headerRow.length; i++) {
				net.getRow(sourceNode).set(headerRow[i], new Double(dataRow[i]));
			}
		}
	}

	private Map<String, String> createAIDMap(String[] aidRow, String[] headerRow, int gWeightColumn) {
		Map<String,String> map = new HashMap<>();
		for (int i = gWeightColumn+1; i < headerRow.length; i++) {
			map.put(aidRow[i], headerRow[i]);
		}
		return map;
	}

	// The tree files, in general have the format:
	//     NODEnnnX\tID\tID\tValue
	// where "nnn" is a tree node number and ID is either a GID/AID or a tree node.
	// We want to convert that to:
	//     GROUPnnnX\tNAME\tNAME\tValue
	// where "nnn" is the tree node number and NAME is either the Node/Column name
	// or a tree node.
	private List<String> readTree(BufferedReader reader, Map<String, String> idMap) throws IOException {
		List<String> treeList = new ArrayList<>();

		String[] columns = null;
		while ((columns = readRow(reader)) != null) {
			String group = getGroup(columns[0]);
			String id1 = getID(columns[1], idMap);
			String id2 = getID(columns[2], idMap);
			String value = columns[3];
			treeList.add(group+"\t"+id1+"\t"+id2+"\t"+value);
		}
		return treeList;
	}

	String getGroup(String node) {
		if (!node.startsWith("NODE"))
			return null;
		int start = 4;
		int end= node.indexOf("X");
		return "GROUP"+node.substring(start, end)+"X";
	}

	String getID(String id, Map<String, String> idMap) {
		// id is either a GID/AID or a NODE
		if (idMap.containsKey(id))
			return idMap.get(id);
		return getGroup(id);
	}

	CyNode createNode(CyNetwork net, String name, Map<String, CyNode>nodeMap) {
		CyNode node = net.addNode();
		net.getRow(node).set(CyNetwork.NAME, name);
		net.getRow(node).set(CyRootNetwork.SHARED_NAME, name);
		nodeMap.put(name, node);
		return node;
	}

	String[] readRow(BufferedReader input) throws IOException {
		String row = input.readLine();
		// System.out.println("Row: "+row);
		if (row == null) return null;
		String[] columns;
		if (delimiter != null)
			columns = row.split(delimiter, -1);
		else {
			delimiter = "\t";
			columns = row.split(delimiter, -1);
			if (columns.length == 1) {
				delimiter = ",";
				columns = row.split(delimiter, -1);
				if (columns.length == 1) {
					delimiter = null;
					throw new RuntimeException("Only tabs and commas are supported column delimiters");
				}
			}
		}
		return columns;
	}

	int findColumn(String[] columns, String header) {
		for (int i = 0; i < columns.length; i++) {
			if (columns[i].equals(header))
				return i;
		}
		return -1;
	}
}
