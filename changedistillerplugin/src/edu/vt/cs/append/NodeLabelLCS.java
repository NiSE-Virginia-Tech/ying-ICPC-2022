package edu.vt.cs.append;

import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;

public class NodeLabelLCS extends LongestCommonSubsequence<Node>{

	@Override
	protected boolean equivalent(Node left, Node right, int i, int j) {
		return left.getLabel().equals(right.getLabel());
	}
}
