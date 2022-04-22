package edu.vt.cs.append;

import java.util.ArrayList;
import java.util.List;

public class LongestCommonSubsequence<T> {

	protected static final int UP = 1;
	protected static final int LEFT = 2;
	protected static final int DIAG = 3;

	protected List<Integer> leftCSIndexes, rightCSIndexes;

	public int getLCS(List<T> left, List<T> right) {
		int m = left.size();
		int n = right.size();
		int[][] c = new int[m + 1][n + 1];
		int[][] b = new int[m + 1][n + 1];

		for (int i = 0; i <= m; i++) {
			c[i][0] = 0;
			b[i][0] = 0;
		}
		for (int i = 0; i <= n; i++) {
			c[0][i] = 0;
			b[0][i] = 0;
		}

		for (int i = 1; i <= m; i++) {
			for (int j = 1; j <= n; j++) {
				if (equivalent(left.get(i - 1), right.get(j - 1), i - 1, j - 1)) {
					c[i][j] = c[i - 1][j - 1] + 1;
					b[i][j] = DIAG;
				} else if (c[i - 1][j] >= c[i][j - 1]) {
					c[i][j] = c[i - 1][j];
					b[i][j] = UP;
				} else {
					c[i][j] = c[i][j - 1];
					b[i][j] = LEFT;
				}
			}
		}
		leftCSIndexes = new ArrayList<Integer>();
		rightCSIndexes = new ArrayList<Integer>();
		// for(int i = 1; i <=m; i++){
		// for(int j = 1; j <= n; j++){
		// System.out.print(c[i][j] + "  ");
		// }
		// System.out.println("\n");
		// }
		extractLCS(b, left, right, m, n);
		return c[m][n];
	}

	protected boolean equivalent(T l, T r, int i, int j) {
		return l.equals(r);
	}

	private void extractLCS(int[][] b, List<T> l, List<T> r, int i, int j) {
		if ((i != 0) && (j != 0)) {
			if (b[i][j] == DIAG) {
				leftCSIndexes.add(0, i - 1);
				rightCSIndexes.add(0, j - 1);
				extractLCS(b, l, r, i - 1, j - 1);
			} else if (b[i][j] == UP) {
				extractLCS(b, l, r, i - 1, j);
			} else {
				extractLCS(b, l, r, i, j - 1);
			}
		}
	}

	public List<Integer> getLeftCSIndexes() {
		return leftCSIndexes;
	}

	public List<Integer> getRightCSIndexes() {
		return rightCSIndexes;
	}
}
