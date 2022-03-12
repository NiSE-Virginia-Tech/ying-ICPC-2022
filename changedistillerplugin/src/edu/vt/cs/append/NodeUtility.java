package edu.vt.cs.append;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;

public class NodeUtility {
	List<String> lines = null;
	List<Node> nList = null;

	public NodeUtility(Node node) {
		nList = new ArrayList<Node>();
		lines = new ArrayList<String>();
		Enumeration<Node> nEnum = node.preorderEnumeration();
		Node tmp = null;
		while(nEnum.hasMoreElements()) {
			tmp = nEnum.nextElement();
			nList.add(tmp);
			lines.add(tmp.toString());
		}
	}
}
