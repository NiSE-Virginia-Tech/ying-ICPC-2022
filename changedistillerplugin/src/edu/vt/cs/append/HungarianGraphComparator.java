package edu.vt.cs.append;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Set;

import ch.uzh.ifi.seal.changedistiller.model.classifiers.EntityType;
import ch.uzh.ifi.seal.changedistiller.model.classifiers.java.JavaEntityType;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.NodePair;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.TreeMatcher;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.matching.measure.NodeSimilarityCalculator;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.matching.measure.StringSimilarityCalculator;

public class HungarianGraphComparator implements TreeMatcher {

	private Set<NodePair> fMatch;
	
	private double[][] costMatrix;
	private List<Node> leftNodes = null;
	private List<Node> rightNodes = null;
	private final double PartCost = 0.5; 
	NodeLabelLCS lcsComparator = null;
	
	private StringSimilarityCalculator stringCalculator = null;
	
	
	private double calculateCost(Node lNode, Node rNode) {
		double inNodeCost = calculateInDegreeCost(lNode, rNode);
		double outNodeCost = calculateOutDegreeCost(lNode, rNode);
		double nodeCost = calculateNodeCost(lNode, rNode);
		return inNodeCost + outNodeCost + nodeCost;
	}
	
	private void calculateCostMatrix(boolean transfer) {
		if (transfer) {
			costMatrix = new double[rightNodes.size()][leftNodes.size()];
			Node lNode = null, rNode = null;
			for (int i = 0; i < rightNodes.size(); i++) {
				rNode = rightNodes.get(i);
				for (int j = 0; j < leftNodes.size(); j++) {
					lNode = leftNodes.get(j);
					costMatrix[i][j] = calculateCost(lNode, rNode);
 				}
			}						
		} else {
			costMatrix = new double[leftNodes.size()][rightNodes.size()];
			Node lNode = null, rNode = null;
			for (int i = 0; i < leftNodes.size(); i++) {
				lNode = leftNodes.get(i);
				for (int j = 0; j < rightNodes.size(); j++) {
					rNode = rightNodes.get(j);
					costMatrix[i][j] = calculateCost(lNode, rNode);				
				}
			}		
		}		
	}
	
	@Override
	public void init(StringSimilarityCalculator leafStringSimilarityCalculator,
			double leafStringSimilarityThreshold,
			NodeSimilarityCalculator nodeSimilarityCalculator,
			double nodeSimilarityThreshold) {
		stringCalculator = leafStringSimilarityCalculator;	
		lcsComparator = new NodeLabelLCS();
	}

	@Override
	public void init(StringSimilarityCalculator leafStringSimilarityCalculator,
			double leafStringSimilarityThreshold,
			StringSimilarityCalculator nodeStringSimilarityCalculator,
			double nodeStringSimilarityThreshold,
			NodeSimilarityCalculator nodeSimilarityCalculator,
			double nodeSimilarityThreshold) {
		stringCalculator = leafStringSimilarityCalculator;
		lcsComparator = new NodeLabelLCS();
	}

	@Override
	public void match(Node left, Node right) {
		leftNodes = new ArrayList<Node>();
		Enumeration<Node> iter = left.breadthFirstEnumeration();
		while(iter.hasMoreElements()) {
			leftNodes.add(iter.nextElement());
		}
		
		rightNodes = new ArrayList<Node>();
		iter = right.breadthFirstEnumeration();
		while(iter.hasMoreElements()) {
			rightNodes.add(iter.nextElement());
		}	
		boolean transfer = leftNodes.size() > rightNodes.size();
		calculateCostMatrix(transfer);
		HungarianAlgorithm ha = new HungarianAlgorithm();
		int[][] matching = ha.hgAlgorithm(costMatrix);
		if (transfer) {
			for (int i = 0; i < matching.length; i++) {
				fMatch.add(new NodePair(
						leftNodes.get(matching[i][1]),
						rightNodes.get(matching[i][0])));
			}
		} else {
			for (int i = 0; i < matching.length; i++) {			
				fMatch.add(new NodePair(leftNodes.get(matching[i][0]),
						rightNodes.get(matching[i][1])));
			}
		}		
	}


	
	private double calculateInDegreeCost(Node lNode, Node rNode) { 
		double inNodeCost = 0;
		Node lParent = (Node) lNode.getParent();
		Node rParent = (Node) rNode.getParent();
		if (lParent == null && rParent == null) {
			inNodeCost = 0;
		} else if (lParent != null && rParent != null) {
			if (lParent.getLabel().equals(rParent.getLabel())) {
				inNodeCost = 0;
			} else {
				inNodeCost = PartCost;
			}
		} else {
			inNodeCost = 1; // one nonnull parent and one null parent			
		}		
		return inNodeCost;
	}
	
	private double calculateNodeCost(Node lNode, Node rNode) {
		double sim = 0;
		double cost = 0;
		if (!lNode.getLabel().equals(rNode.getLabel())) {
			cost = 1.0;
		}
		sim = stringCalculator.calculateSimilarity(lNode.getValue(), rNode.getValue());
		return 1 - sim + cost;
	}
	
	private double calculateOutDegreeCost(Node lNode, Node rNode) {
		double outNodeCost = 1;
		int lCount = lNode.getChildCount();
		int rCount = rNode.getChildCount();
		if (lCount == 0)
			return outNodeCost;
		if (rCount == 0)
			return outNodeCost;
		List<Node> lchildren = new ArrayList<Node>();
		List<Node> rchildren = new ArrayList<Node>();
		Enumeration<Node> lEnum = lNode.children();
		while (lEnum.hasMoreElements()) {
			lchildren.add(lEnum.nextElement());
		}
		Enumeration<Node> rEnum = rNode.children();
		while (rEnum.hasMoreElements()) {
			rchildren.add(rEnum.nextElement());
		}		
		int common = lcsComparator.getLCS(lchildren, rchildren);
		outNodeCost = 1 - common * 1.0/Math.min(lCount, rCount);
		return outNodeCost;
	}
	
	
	@Override
	public void setMatchingSet(Set<NodePair> matchingSet) {		
		this.fMatch = matchingSet;
	}

	@Override
	public void enableDynamicThreshold(int depth, double threshold) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void disableDynamicThreshold() {
		// TODO Auto-generated method stub
		
	}

}
