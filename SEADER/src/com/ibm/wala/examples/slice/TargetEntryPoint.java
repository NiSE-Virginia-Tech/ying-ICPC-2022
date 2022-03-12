package com.ibm.wala.examples.slice;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.ArgumentTypeEntrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;

import java.util.HashSet;
import java.util.Iterator;

public class TargetEntryPoint extends HashSet<Entrypoint> {

    public TargetEntryPoint(AnalysisScope scope, IClassHierarchy cha, IClass kclass) {
        if (cha == null) {
            throw new IllegalArgumentException("cha is null");
        } else {
                Iterator var5 = kclass.getDeclaredMethods().iterator();
                while(var5.hasNext()) {
                    IMethod method = (IMethod)var5.next();
                    if (!method.isAbstract()) {
                        if(method.toString().contains("main")){
                            this.add(new ArgumentTypeEntrypoint(method, cha));
                            return;
                        }
                        else
                            this.add(new ArgumentTypeEntrypoint(method, cha));

                    }
                }
            }
        }

    private static boolean isApplicationClass(AnalysisScope scope, IClass klass) {
        return scope.getApplicationLoader().equals(klass.getClassLoader().getReference());
    }
}
