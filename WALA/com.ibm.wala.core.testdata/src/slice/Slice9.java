/*
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package slice;

import java.util.Random;

public class Slice9 {

  public static void main(String[] args) {
    Random r = null;
    r = new Random();
    int i = 42;
    i = r.nextInt();
    int j;
    j = 42 * i;
    doNothing(j);
  }

  static void doNothing(int x) {}
}
