/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package Slice;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.slicer.NormalStatement;
import com.ibm.wala.ipa.slicer.Statement;
import com.ibm.wala.ssa.*;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.strings.Atom;
import org.junit.AfterClass;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;

public class SlicerTest {

  private static AnalysisScope cachedScope;

  // more aggressive exclusions to avoid library blowup
  // in interprocedural tests
  private static final String EXCLUSIONS = "java\\/awt\\/.*\n" +
      "javax\\/swing\\/.*\n" +
      "sun\\/awt\\/.*\n" +
      "sun\\/swing\\/.*\n" +
      "com\\/sun\\/.*\n" +
      "sun\\/.*\n" +
      "org\\/netbeans\\/.*\n" +
      "org\\/openide\\/.*\n" +
      "com\\/ibm\\/crypto\\/.*\n" +
      "com\\/ibm\\/security\\/.*\n" +
      "org\\/apache\\/xerces\\/.*\n" +
      "java\\/security\\/.*\n" +
      "";

  private static AnalysisScope findOrCreateAnalysisScope() throws IOException {
    if (cachedScope == null) {
      cachedScope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA, null, SlicerTest.class.getClassLoader());
      cachedScope.setExclusions(new FileOfClasses(new ByteArrayInputStream(EXCLUSIONS.getBytes(StandardCharsets.UTF_8))));

    }
    return cachedScope;
  }

  private static IClassHierarchy cachedCHA;

  private static IClassHierarchy findOrCreateCHA(AnalysisScope scope) throws ClassHierarchyException {
    if (cachedCHA == null) {
      cachedCHA = ClassHierarchyFactory.make(scope);
    }
    return cachedCHA;
  }

  @AfterClass
  public static void afterClass() {
    cachedCHA = null;
    cachedScope = null;
  }

  public static int countAllocations(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSANewInstruction) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countApplicationAllocations(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSANewInstruction) {
          AnalysisScope scope = s.getNode().getClassHierarchy().getScope();
          if (scope.isApplicationLoader(s.getNode().getMethod().getDeclaringClass().getClassLoader())) {
            count++;
          }
        }
      }
    }
    return count;
  }

  public static int countThrows(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAAbstractThrowInstruction) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countAloads(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAArrayLoadInstruction) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countNormals(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        count++;
      }
    }
    return count;
  }

  public static int countApplicationNormals(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        AnalysisScope scope = s.getNode().getClassHierarchy().getScope();
        if (scope.isApplicationLoader(s.getNode().getMethod().getDeclaringClass().getClassLoader())) {
          count++;
        }
      }
    }
    return count;
  }
  public static int countConditionals(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAConditionalBranchInstruction) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countInvokes(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAAbstractInvokeInstruction) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countPutfields(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAPutInstruction) {
          SSAPutInstruction p = (SSAPutInstruction) ns.getInstruction();
          if (!p.isStatic()) {
            count++;
          }
        }
      }
    }
    return count;
  }

  public static int countReturns(Collection<Statement> slice) {
    int count = 0;
    for (Statement s: slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAReturnInstruction) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countGetfields(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAGetInstruction) {
          SSAGetInstruction p = (SSAGetInstruction) ns.getInstruction();
          if (!p.isStatic()) {
            count++;
          }
        }
      }
    }
    return count;
  }

  public static int countPutstatics(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAPutInstruction) {
          SSAPutInstruction p = (SSAPutInstruction) ns.getInstruction();
          if (p.isStatic()) {
            count++;
          }
        }
      }
    }
    return count;
  }

  public static int countGetstatics(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAGetInstruction) {
          SSAGetInstruction p = (SSAGetInstruction) ns.getInstruction();
          if (p.isStatic()) {
            count++;
          }
        }
      }
    }
    return count;
  }

  public static void dumpSlice(Collection<Statement> slice) {
    dumpSlice(slice, new PrintWriter(System.err));
  }

  public static void dumpSlice(Collection<Statement> slice, PrintWriter w) {
    w.println("SLICE:\n");
    int i = 1;
    for (Statement s : slice) {
      String line = (i++) + "   " + s;
      w.println(line);
      w.flush();
    }
  }

  public static void dumpSliceToFile(Collection<Statement> slice, String fileName) throws FileNotFoundException {
    File f = new File(fileName);
    FileOutputStream fo = new FileOutputStream(f);
    try (final PrintWriter w = new PrintWriter(fo)) {
      dumpSlice(slice, w);
    }
  }

  public static CGNode findMainMethod(CallGraph cg) {
    Descriptor d = Descriptor.findOrCreateUTF8("([Ljava/lang/String;)V");
    Atom name = Atom.findOrCreateUnicodeAtom("main");
    return findMethod(cg, d, name);
  }

  /**
   * @param cg
   * @param d
   * @param name
   */
  private static CGNode findMethod(CallGraph cg, Descriptor d, Atom name) {
    for (CGNode n : Iterator2Iterable.make(cg.getSuccNodes(cg.getFakeRootNode()))) {
      if (n.getMethod().getName().equals(name) && n.getMethod().getDescriptor().equals(d)) {
        return n;
      }
    }
    // if it's not a successor of fake root, just iterate over everything
    for (CGNode n : cg) {
      if (n.getMethod().getName().equals(name) && n.getMethod().getDescriptor().equals(d)) {
        return n;
      }
    }
    Assertions.UNREACHABLE("failed to find method " + name);
    return null;
  }

  public static CGNode findMethod(CallGraph cg, String name) {
    Atom a = Atom.findOrCreateUnicodeAtom(name);
    for (CGNode n : cg) {
      if (n.getMethod().getName().equals(a)) {
        return n;
      }
    }
    System.err.println("call graph " + cg);
    Assertions.UNREACHABLE("failed to find method " + name);
    return null;
  }

  public static Statement findCallTo(CGNode n, String methodName) {
    IR ir = n.getIR();
    for (SSAInstruction s : Iterator2Iterable.make(ir.iterateAllInstructions())) {
      if (s instanceof SSAInvokeInstruction) {
        SSAInvokeInstruction call = (SSAInvokeInstruction) s;
        if (call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName)) {
          IntSet indices = ir.getCallInstructionIndices(((SSAInvokeInstruction) s).getCallSite());
          Assertions.productionAssertion(indices.size() == 1, "expected 1 but got " + indices.size());
          return new NormalStatement(n, indices.intIterator().next());
        }
      }
    }
    Assertions.UNREACHABLE("failed to find call to " + methodName + " in " + n);
    return null;
  }

  public static Statement findFirstAllocation(CGNode n) {
    IR ir = n.getIR();
    for (int i = 0; i < ir.getInstructions().length; i++) {
      SSAInstruction s = ir.getInstructions()[i];
      if (s instanceof SSANewInstruction) {
        return new NormalStatement(n, i);
      }
    }
    Assertions.UNREACHABLE("failed to find allocation in " + n);
    return null;
  }

  private static Statement findCallToDoNothing(CGNode n) {
    return findCallTo(n, "doNothing");
  }
}
