package edu.vt.cs.append;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;

import ch.uzh.ifi.seal.changedistiller.model.classifiers.java.JavaEntityType;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.NodePair;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.TreeMatcher;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.matching.measure.NodeSimilarityCalculator;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.matching.measure.StringSimilarityCalculator;

public class TopDownTreeMatcher implements TreeMatcher {

	Map<Node, Node> leftToRightUnmatched = null;
	Map<Node, Node> rightToLeftUnmatched = null;
	public Node left_pattern_original_node = null;
	public Node right_pattern_original_node = null;
	List<List<Integer>> left_ans_list = null;
	List<List<Integer>> right_ans_list = null;
	
	@Override
	public void init(StringSimilarityCalculator leafStringSimilarityCalculator,
			double leafStringSimilarityThreshold,
			NodeSimilarityCalculator nodeSimilarityCalculator,
			double nodeSimilarityThreshold) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void init(StringSimilarityCalculator leafStringSimilarityCalculator,
			double leafStringSimilarityThreshold,
			StringSimilarityCalculator nodeStringSimilarityCalculator,
			double nodeStringSimilarityThreshold,
			NodeSimilarityCalculator nodeSimilarityCalculator,
			double nodeSimilarityThreshold) {
		// TODO Auto-generated method stub
		
	}
	
	public Map<Node, Node> getUnmatchedLeftToRight() {
		return leftToRightUnmatched;
	}
	
	public Map<Node, Node> getUnmatchedRightToLeft() {
		return rightToLeftUnmatched;
	}

	/**
	 * Match trees in a top-down manner
	 */
	@Override
	public void match(Node left, Node right) {
		leftToRightUnmatched = new HashMap<Node, Node>();
		rightToLeftUnmatched = new HashMap<Node, Node>();
		Queue<Node> lQueue = new LinkedList<Node>();
		Queue<Node> rQueue = new LinkedList<Node>();
		lQueue.add(left);
		rQueue.add(right);
		Node lTmp = null, rTmp = null;		
		while(!lQueue.isEmpty() && !rQueue.isEmpty()) {
			lTmp = lQueue.remove();
			rTmp = rQueue.remove();	
			if (lTmp == null || rTmp == null) {
				CommonValue.Exp_Sniffer = 1;
				return;
			}
			if (lTmp.getLabel().equals(rTmp.getLabel())
					&& lTmp.getChildCount() == rTmp.getChildCount()) {
				//Fahad: Added string lateral to find consider changes in string parameters
				if (lTmp.getLabel().equals(JavaEntityType.METHOD) ||
						lTmp.getLabel().equals(JavaEntityType.TYPE_PARAMETER) ||
						lTmp.getLabel().equals(JavaEntityType.SIMPLE_NAME) ||
						lTmp.getLabel().equals(JavaEntityType.CLASS_INSTANCE_CREATION) ||
						lTmp.getLabel().equals(JavaEntityType.STRING_LITERAL)) {// the invoked methods are different, so the parent nodes (MethodInvocation) are considered unmatched
					if (!lTmp.getValue().equals(rTmp.getValue())) {
						Node lParent = (Node)lTmp.getParent(); 
						Node rParent = (Node)rTmp.getParent();
						
						leftToRightUnmatched.put(lParent, rParent);
						rightToLeftUnmatched.put(rParent, lParent);
						//added by shengzhe to void exploring the subtree of a unmatched node
						continue;
					}
				} 
				Enumeration<Node> lEnum = lTmp.children();
				Enumeration<Node> rEnum = rTmp.children();
				while(lEnum.hasMoreElements()) {
					lQueue.add(lEnum.nextElement());
					rQueue.add(rEnum.nextElement());
				}							
			} else {
				leftToRightUnmatched.put(lTmp, rTmp);
				rightToLeftUnmatched.put(rTmp, lTmp);
			}
		}	
		
		if (leftToRightUnmatched.size() > 1) {//refine the data to check if one node is in a subtree of another node
			Set<Node> covered = new HashSet<Node>();
			Set<Node> knownNodes = new HashSet<Node>(leftToRightUnmatched.keySet());
			for (Node tmp : knownNodes) {							
				Set<Node> ancestors = new HashSet<Node>();
				Node parent = (Node) tmp.getParent();
				while (parent != null) {
					ancestors.add(parent);
					
					parent = (Node) parent.getParent();
				}
				ancestors.retainAll(knownNodes);
				if (!ancestors.isEmpty()) {
					covered.add(tmp);
				}
			}
			for (Node tmp : covered) {
				Node tmpRight = leftToRightUnmatched.get(tmp);
				leftToRightUnmatched.remove(tmp);
				rightToLeftUnmatched.remove(tmpRight);
			}
		}	
	}
	
	/**
	 * Match trees in a top-down manner for pattern output usage
	 */
	public void match_pattern_tree(Node left, Node right) {
		leftToRightUnmatched = new HashMap<Node, Node>();
		rightToLeftUnmatched = new HashMap<Node, Node>();
		Queue<Node> lQueue = new LinkedList<Node>();
		Queue<Node> rQueue = new LinkedList<Node>();
		lQueue.add(left);
		rQueue.add(right);
		Node lTmp = null, rTmp = null;		
		while(!lQueue.isEmpty() && !rQueue.isEmpty()) {
			lTmp = lQueue.remove();
			rTmp = rQueue.remove();	
			if (lTmp == null || rTmp == null) {
				CommonValue.Exp_Sniffer = 1;
				return;
			}
			if (lTmp.getLabel().equals(rTmp.getLabel())
					&& lTmp.getChildCount() == rTmp.getChildCount()) {
				if (lTmp.getLabel().equals(JavaEntityType.STRING_LITERAL) || //Fahad: added STRING_LITERAL for string parameter change in APIs
						lTmp.getLabel().equals(JavaEntityType.METHOD) ||
						lTmp.getLabel().equals(JavaEntityType.TYPE_PARAMETER) ||
						lTmp.getLabel().equals(JavaEntityType.SIMPLE_NAME) ||
						lTmp.getLabel().equals(JavaEntityType.CLASS_INSTANCE_CREATION)) {// the invoked methods are different, so the parent nodes (MethodInvocation) are considered unmatched
					if (!lTmp.getValue().equals(rTmp.getValue())) {
						Node lParent = (Node)lTmp.getParent(); 
						Node rParent = (Node)rTmp.getParent();
						
						leftToRightUnmatched.put(lParent, rParent);
						rightToLeftUnmatched.put(rParent, lParent);
						//added by shengzhe to void exploring the subtree of a unmatched node
						continue;
					}
				} 
				Enumeration<Node> lEnum = lTmp.children();
				Enumeration<Node> rEnum = rTmp.children();
				while(lEnum.hasMoreElements()) {
					lQueue.add(lEnum.nextElement());
					rQueue.add(rEnum.nextElement());
				}							
			} else {
				leftToRightUnmatched.put(lTmp, rTmp);
				rightToLeftUnmatched.put(rTmp, lTmp);
			}
		}	
		
		if (leftToRightUnmatched.size() > 1) {//refine the data to check if one node is in a subtree of another node
			Set<Node> covered = new HashSet<Node>();
			Set<Node> knownNodes = new HashSet<Node>(leftToRightUnmatched.keySet());
			for (Node tmp : knownNodes) {							
				Set<Node> ancestors = new HashSet<Node>();
				Node parent = (Node) tmp.getParent();
				while (parent != null) {
					ancestors.add(parent);
					parent = (Node) parent.getParent();
				}
				ancestors.retainAll(knownNodes);
				if (!ancestors.isEmpty()) {
					covered.add(tmp);
				}
			}
			for (Node tmp : covered) {
				Node tmpRight = leftToRightUnmatched.get(tmp);
				leftToRightUnmatched.remove(tmp);
				rightToLeftUnmatched.remove(tmpRight);
			}
		}	
		
		if (leftToRightUnmatched.size() == 1) {
			for (Node x : leftToRightUnmatched.keySet()) {
				left_pattern_original_node = x;
			}
			for (Node y : leftToRightUnmatched.values()) {
				right_pattern_original_node = y;
			}
		}
		if (leftToRightUnmatched.size() > 1) {//find the LCA for both the left pattern tree and the right pattern tree									
			// first deal with left tree
			Set<Node> knownNodes = new HashSet<Node>(leftToRightUnmatched.keySet());
			left_ans_list = new ArrayList<List<Integer>>();
			for (Node tmp : knownNodes) {							
				Set<Node> ancestors = new HashSet<Node>();
				List<Integer> anc_tmp = new ArrayList<Integer>();
				Node parent = (Node) tmp.getParent();
				while (parent != null) {
					ancestors.add(parent);
					anc_tmp.add(parent.id);
					parent = (Node) parent.getParent();
				}
				left_ans_list.add(anc_tmp);
			}
			boolean check_label = true;
			int last_i = 0;
			List<Integer> tmp_0 = left_ans_list.get(0);
			while (check_label) {
				last_i ++;
				for (List<Integer> tmp_x : left_ans_list) {
					// because we already delete the covered nodes, so there must be a LCA instead of meet bug
					if (tmp_x.get(tmp_x.size() - last_i) != tmp_0.get(tmp_0.size() - last_i)) {
						check_label = false;
						break;
					}
				}
			}
			for (Node tmp : knownNodes) {// once meet one, break							
				Node parent = (Node) tmp.getParent();
				while (parent != null) {
					if (parent.id == tmp_0.get(tmp_0.size() - last_i)) {
						left_pattern_original_node = parent;
						break;
					}
					parent = (Node) parent.getParent();
				}
				break;
			}
			// second deal with right tree
						knownNodes = new HashSet<Node>(leftToRightUnmatched.values());
						right_ans_list = new ArrayList<List<Integer>>();
						for (Node tmp : knownNodes) {							
							Set<Node> ancestors = new HashSet<Node>();
							List<Integer> anc_tmp = new ArrayList<Integer>();
							Node parent = (Node) tmp.getParent();
							while (parent != null) {
								ancestors.add(parent);
								anc_tmp.add(parent.id);
								parent = (Node) parent.getParent();
							}
							right_ans_list.add(anc_tmp);
						}
						check_label = true;
						last_i = 0;
						tmp_0 = right_ans_list.get(0);
						while (check_label) {
							last_i ++;
							for (List<Integer> tmp_x : right_ans_list) {
								// because we already delete the covered nodes, so there must be a LCA instead of meet bug
								if (tmp_x.get(tmp_x.size() - last_i) != tmp_0.get(tmp_0.size() - last_i)) {
									check_label = false;
									break;
								}
							}
						}
						for (Node tmp : knownNodes) {// once meet one, break							
							Node parent = (Node) tmp.getParent();
							while (parent != null) {
								if (parent.id == tmp_0.get(tmp_0.size() - last_i)) {
									right_pattern_original_node = parent;
									break;
								}
								parent = (Node) parent.getParent();
							}
							break;
						}			
		}	
	}
	
	/**
	 * Match trees in a top-down manner
	 */
	public void match_filter(Node left, Node right) {
		leftToRightUnmatched = new HashMap<Node, Node>();
		rightToLeftUnmatched = new HashMap<Node, Node>();
		Queue<Node> lQueue = new LinkedList<Node>();
		Queue<Node> rQueue = new LinkedList<Node>();
		lQueue.add(left);
		rQueue.add(right);
		Node lTmp = null, rTmp = null;		
		while(!lQueue.isEmpty() && !rQueue.isEmpty()) {
			lTmp = lQueue.remove();
			rTmp = rQueue.remove();	
			if (lTmp == null || rTmp == null) continue;
			if (lTmp.getLabel().equals(rTmp.getLabel())
					&& lTmp.getChildCount() == rTmp.getChildCount()) {
				if (lTmp.getLabel().equals(JavaEntityType.METHOD) ||
						lTmp.getLabel().equals(JavaEntityType.TYPE_PARAMETER) ||
						lTmp.getLabel().equals(JavaEntityType.SIMPLE_NAME) ||
						lTmp.getLabel().equals(JavaEntityType.CLASS_INSTANCE_CREATION)) {// the invoked methods are different, so the parent nodes (MethodInvocation) are considered unmatched
					if (!lTmp.getValue().equals(rTmp.getValue())) {
						Node lParent = (Node)lTmp.getParent(); 
						Node rParent = (Node)rTmp.getParent();
						
						leftToRightUnmatched.put(lParent, rParent);
						rightToLeftUnmatched.put(rParent, lParent);
					}
				} 
				Enumeration<Node> lEnum = lTmp.children();
				Enumeration<Node> rEnum = rTmp.children();
				while(lEnum.hasMoreElements()) {
					//shengzhe's if block filter
					Node le = lEnum.nextElement();
					Node re = rEnum.nextElement();
					if (lTmp.getLabel() == JavaEntityType.IF_STATEMENT
							&& le.getLabel() == JavaEntityType.BLOCK
							&& re.getLabel() == JavaEntityType.BLOCK) {
						continue;
					}
					lQueue.add(le);
					rQueue.add(re);
				}							
			} else {
				leftToRightUnmatched.put(lTmp, rTmp);
				rightToLeftUnmatched.put(rTmp, lTmp);
			}
		}	
		
		if (leftToRightUnmatched.size() > 1) {//refine the data
			Set<Node> covered = new HashSet<Node>();
			Set<Node> knownNodes = new HashSet<Node>(leftToRightUnmatched.keySet());
			for (Node tmp : knownNodes) {							
				Set<Node> ancestors = new HashSet<Node>();
				Node parent = (Node) tmp.getParent();
				while (parent != null) {
					ancestors.add(parent);
					parent = (Node) parent.getParent();
				}
				ancestors.retainAll(knownNodes);
				if (!ancestors.isEmpty()) {
					covered.add(tmp);
				}
			}
			for (Node tmp : covered) {
				Node tmpRight = leftToRightUnmatched.get(tmp);
				leftToRightUnmatched.remove(tmp);
				rightToLeftUnmatched.remove(tmpRight);
			}
		}
	}

	@Override
	public void setMatchingSet(Set<NodePair> matchingSet) {
		// TODO Auto-generated method stub
		
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
