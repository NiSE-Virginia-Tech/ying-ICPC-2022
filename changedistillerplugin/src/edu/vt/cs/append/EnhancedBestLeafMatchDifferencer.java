package edu.vt.cs.append;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.incava.util.diff.Diff;
import org.incava.util.diff.Difference;

import ch.uzh.ifi.seal.changedistiller.treedifferencing.LeafPair;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.NodePair;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.TreeMatcher;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.matching.measure.NodeSimilarityCalculator;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.matching.measure.StringSimilarityCalculator;

public class EnhancedBestLeafMatchDifferencer implements TreeMatcher {
	
	public static final List<String> labelList = Arrays.asList("TRY_STATEMENT", "CATCH_CLAUSE", "SWITCH_CASE", "SWITCH_STATEMENT",
			"SYNCHRONIZED_STATEMENT", "THROW_STATEMENT");

	private StringSimilarityCalculator fLeafGenericStringSimilarityCalculator;
	private double fLeafGenericStringSimilarityThreshold;

	private NodeSimilarityCalculator fNodeSimilarityCalculator;
	private double fNodeSimilarityThreshold;

	private StringSimilarityCalculator fNodeStringSimilarityCalculator;
	private double fNodeStringSimilarityThreshold;
	private final double fWeightingThreshold = 0.8;
	private final double fDynamicNodeStringThreshold = 0.8;

	private boolean fDynamicEnabled;
	private int fDynamicDepth;
	private double fDynamicThreshold;

	private Map<Node, Set<NodePair>> fLeftToRightMatchNodes,
			fRightToLeftMatchNodes;
	private Map<Node, Set<LeafPair>> fLeftToRightMatchLeafs,
			fRightToLeftMatchLeafs;
	private List<LeafPair> basicLeafPairs;

	private double DIST = Math.pow(0.1, 6);

	private Set<NodePair> fMatch;
	
	@Override
	public void init(StringSimilarityCalculator leafStringSimilarityCalculator,
			double leafStringSimilarityThreshold,
			NodeSimilarityCalculator nodeSimilarityCalculator,
			double nodeSimilarityThreshold) {
		fLeafGenericStringSimilarityCalculator = leafStringSimilarityCalculator;
        fLeafGenericStringSimilarityThreshold = leafStringSimilarityThreshold;
        fNodeStringSimilarityCalculator = leafStringSimilarityCalculator;
        fNodeStringSimilarityThreshold = leafStringSimilarityThreshold;
        fNodeSimilarityCalculator = nodeSimilarityCalculator;
        fNodeSimilarityThreshold = nodeSimilarityThreshold;
	}

	@Override
	public void init(StringSimilarityCalculator leafStringSimilarityCalculator,
			double leafStringSimilarityThreshold,
			StringSimilarityCalculator nodeStringSimilarityCalculator,
			double nodeStringSimilarityThreshold,
			NodeSimilarityCalculator nodeSimilarityCalculator,
			double nodeSimilarityThreshold) {
		init(leafStringSimilarityCalculator, leafStringSimilarityThreshold, nodeSimilarityCalculator,
				nodeStringSimilarityThreshold);
		fNodeStringSimilarityCalculator = nodeStringSimilarityCalculator;
		fNodeStringSimilarityThreshold = nodeSimilarityThreshold;
		
	}

	@Override
	public void match(Node left, Node right) {
		textualDiff(left, right);		
//		matchLeaves(left, right);
//		matchInnerNodes(left, right);
//		matchInnerAndLeaf(left, right);
	}
	
	
	
	public void matchInnerAndLeaf(Node left, Node right) {
		Node x, y;
		// match inner nodes and leaf nodes when possible
		for (Enumeration<Node> leftNodes = left.postorderEnumeration(); leftNodes
				.hasMoreElements();) {
			x = leftNodes.nextElement();
			if (!x.isMatched() && isSpecial(x)) {
				for (Enumeration<Node> rightNodes = right
						.postorderEnumeration(); rightNodes.hasMoreElements();) {
					y = rightNodes.nextElement();
					if (!y.isMatched() && isSpecial(y) && equal3(x, y)) {
						addMatch(x, y);
					}
				}
			}
		}
	}
	
	private void addMatch(Node x, Node y) {
		x.enableMatched();
		y.enableMatched();
		fMatch.add(new NodePair(x, y));
	}
	
	public void matchInnerNodes(Node left, Node right) {
		Node x, y;
		// the roots match each each
		if (!fMatch.contains(new NodePair(left, right))) {
			addMatch(left, right);
		}
		
		left.enableMatched();
		right.enableMatched();
		for (Enumeration<Node> leftNodes = left.postorderEnumeration(); leftNodes
				.hasMoreElements();) {
			x = leftNodes.nextElement();
			if (!x.isMatched()/* && !x.isLeaf() */&& !isSpecial(x)) {
				List<Node> candidates = new ArrayList<Node>();
				for (Enumeration<Node> rightNodes = right
						.postorderEnumeration(); rightNodes.hasMoreElements();) {
					y = rightNodes.nextElement();
					if ((!y.isMatched()) /* && !y.isLeaf() */&& !isSpecial(y)
							&& (equal(x, y) || equal2(x, y))) {// we try to
																// match an
																// inner node
																// with another
																// inner node or
																// a leaf
																// simultaneously
						candidates.add(y);
					}
				}
				if (candidates.size() == 0) {
					continue;
				} else {
					if (candidates.size() == 1) {
						y = candidates.get(0);
					} else {// to look for the possibly best matched inner node
						y = candidates.get(0);
						double maxPercent = countMatchedChild(x, y);
						for (int i = 1; i < candidates.size(); i++) {
							Node yy = candidates.get(i);
							double yyPercent = countMatchedChild(x, yy);
							if (yyPercent > maxPercent) {
								y = yy;
								maxPercent = yyPercent;
							}
						}
					}
					if (checkMapPossibility(x, y)) {
						addMatch(x, y);
					}
				}
			}
		}
	}
	
	/**
	 * Position matching leaves are preferred to contenting matching leaves
	 * 
	 * @param matchedLeafs
	 * @return
	 */
	public void matchLeaves(Node left, Node right) {
		List<LeafPair> leafPairs = new ArrayList<LeafPair>();
		Node x, y;
		for (Enumeration<Node> leftNodes = left.postorderEnumeration(); leftNodes
				.hasMoreElements();) {
			x = leftNodes.nextElement();
			if (x.isLeaf() && !x.isMatched()) {
				for (Enumeration<Node> rightNodes = right
						.postorderEnumeration(); rightNodes.hasMoreElements();) {
					y = rightNodes.nextElement();
					if (y.isLeaf() && !y.isMatched()) {
						if (x.getLabel().equals(y.getLabel())) {
							double similarity = fLeafGenericStringSimilarityCalculator
									.calculateSimilarity(x.getValue(),
											y.getValue());
							// Important! Otherwhise nodes that match poorly
							// will make it into final matching set,
							// if no better matches are found!
							if (similarity >= fLeafGenericStringSimilarityThreshold) {
								leafPairs.add(new LeafPair(x, y, similarity));
							}
						}
					}
				}
			}
		}
		Collections.sort(leafPairs);
		for (LeafPair pair : leafPairs) {
			x = pair.getLeft();
			y = pair.getRight();
			if (!(x.isMatched() || y.isMatched())) {
				addMatch(x, y);
			}
		}
	}
	
	public void textualDiff(Node left, Node right) {
		NodeUtility lu = new NodeUtility(left);
		NodeUtility ru = new NodeUtility(right);
		String[] fromLines = lu.lines.toArray(new String[lu.lines.size()]);
		String[] toLines = ru.lines.toArray(new String[ru.lines.size()]);
		List<Difference> diffs = (new Diff(fromLines, toLines)).diff();
		Set<Integer> deletedLines = new HashSet<Integer>();
		Set<Integer> addedLines = new HashSet<Integer>();
		List<Node> lList = lu.nList;
		List<Node> rList = ru.nList;
		for (Difference diff : diffs) {
			int delStart = diff.getDeletedStart();
			int delEnd = diff.getDeletedEnd();
			int addStart = diff.getAddedStart();
			int addEnd = diff.getAddedEnd();
			boolean isMod = (delEnd != Difference.NONE && addEnd != Difference.NONE);
			if (isMod) {
				addMatch(lList.get(delStart), rList.get(addStart));
			}
	
			for (int i = delStart; i <= delEnd; i++) {
				deletedLines.add(i);
			}
			for (int i = addStart; i <= addEnd; i++) {
				addedLines.add(i);
			}
		}	
		int i = 0;
		int j = 0;
		
		int lsize = fromLines.length;
		int rsize = toLines.length;		
		while (i < lsize && j < rsize) {
			while (deletedLines.contains(i)) {
				i++;
			}
			while (addedLines.contains(j)) {
				j++;
			}
			if (i < lsize && j < rsize) {
				addMatch(lList.get(i), rList.get(j));
				i++;
				j++;
			} 
		}
	}

	
	private boolean isSpecial(Node node) {
		String lab = node.getLabel().toString();		
		return lab.equals("THEN_STATEMENT") || lab.equals("ELSE_STATEMENT")
				|| lab.equals("TRY_STATEMENT") || lab.equals("FINALLY");
	}


	@Override
	public void setMatchingSet(Set<NodePair> matchingSet) {
		fMatch = matchingSet;
	}

	@Override
	public void enableDynamicThreshold(int depth, double threshold) {
		fDynamicDepth = depth;
		fDynamicThreshold = threshold;
		fDynamicEnabled = true;		
	}

	@Override
	public void disableDynamicThreshold() {
		fDynamicEnabled = false;
	}
	
	private boolean checkMapPossibility(Node tempLeft, Node tempRight) {
		String lLab = tempLeft.getLabel().toString();
		String rLab = tempRight.getLabel().toString();
		return lLab.equals(rLab)
				|| (!labelList.contains(lLab) && !labelList.contains(rLab));
	}
	
	private double countMatchedChild(Node x, Node y) {
		int totalCounter = 0;
		int counter = 0;
		if (x.isLeaf() || y.isLeaf())
			return counter;
		Enumeration<Node> enumeration = y.postorderEnumeration();
		while (enumeration.hasMoreElements()) {
			Node child = enumeration.nextElement();
			totalCounter++;
			// if the matched node of this y's child is also the child of x
			if (child.isMatched()) {
				Node left = getMatchedLeftNode(child);
				if (left != null && left.isNodeAncestor(x))
					counter++;
			}
		}
		return counter * 1.0 / totalCounter;
	}

	private boolean equal(Node x, Node y) {
		// inner nodes
		if (!x.isLeaf() && !y.isLeaf()) {
			String xVal = x.getValue();
			String yVal = y.getValue();
			String xLab = x.getLabel().toString();
			String yLab = y.getLabel().toString();
//			if (xLab.equals("IF_STATEMENT")
//					&& (yVal.equals("THEN") || yVal.equals("ELSE"))
//					|| (xVal.equals("THEN") || xVal.equals("ELSE"))
//					&& yLab.equals("IF_STATEMENT")) {
//				return false;
//			}
//			if (xLab.equals("WHILE_STATEMENT")
//					&& yLab.equals("IF_STATEMENT")
//					|| xLab.equals("IF_STATEMENT")
//					&& yLab.equals("WHILE_STATEMENT")) {
//				return false;
//			}
		
			double t = fNodeSimilarityThreshold;
			double t2 = t;
			if (fDynamicEnabled
					&& ((x.getLeafCount() < fDynamicDepth) || (y.getLeafCount() < fDynamicDepth))) {
				t = fDynamicThreshold;
				t2 = t / 2;// maybe very different in structure
			}
			double simNode = fNodeSimilarityCalculator
					.calculateSimilarity(x, y);
			double simString = fNodeStringSimilarityCalculator
					.calculateSimilarity(xVal, yVal);
			if (simNode >= fWeightingThreshold) {
				// the structures are very similar, but the node strings are
				// very different, this may be a renaming operation
				return true;
			}
			if ((simString >= fNodeStringSimilarityThreshold) && (simNode >= t)) {
				// the structures are less similar, but the node strings are
				// more similar to each other
				return true;
			} else {
				// the structures are very different, but the node strings are
				// quite similar
				// this may be a big insertion
				return (simNode >= t2)
						&& (simString >= fDynamicNodeStringThreshold);
			}
		}
		return false;
	}

	private boolean equal2(Node x, Node y) {
		try {
			String xLab = x.getLabel().toString();
			String yLab = y.getLabel().toString();
			String xVal = x.getValue();
			String yVal = y.getValue();
			if (xLab.equals(yLab)) {
				double similarity = fLeafGenericStringSimilarityCalculator
						.calculateSimilarity(xVal, yVal);
				if (similarity >= fNodeStringSimilarityThreshold) {
					return true;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * precondition: isSpecial(x) == isSpecial(y) == true
	 * @param x
	 * @param y
	 * @return
	 */
	private boolean equal3(Node x, Node y) {
		try {
			String xLab = x.getLabel().toString();
			String yLab = y.getLabel().toString();
			if (xLab.equals(yLab)) {
				Node rightParent = (Node) y.getParent();
				Node leftParent = getMatchedLeftNode((rightParent));
				return leftParent != null && fMatch.contains(new NodePair(leftParent, rightParent));// for these special nodes, the following process
// does not help since they can only return true
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
	}
	
	
	private Node getMatchedLeftNode(Node node) {
		Node matchedNode = null;
		if (node == null || !node.isMatched()) {// this case should be
												// impossible
			// do nothing
		} else {
			for (NodePair np : fMatch) {
				if (np.getRight().equals(node))
					return np.getLeft();
			}
		}
		return matchedNode;
	}
}
