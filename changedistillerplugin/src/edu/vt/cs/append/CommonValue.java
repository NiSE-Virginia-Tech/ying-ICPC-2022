package edu.vt.cs.append;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;

import ch.uzh.ifi.seal.changedistiller.ast.java.JavaASTHelper;
import ch.uzh.ifi.seal.changedistiller.ast.java.JavaASTNodeTypeConverter;
import ch.uzh.ifi.seal.changedistiller.distilling.refactoring.AbstractRefactoringHelper;
import ch.uzh.ifi.seal.changedistiller.distilling.refactoring.MethodRefactoringHelper;
import ch.uzh.ifi.seal.changedistiller.model.entities.ClassHistory;
import ch.uzh.ifi.seal.changedistiller.structuredifferencing.StructureNode;
import ch.uzh.ifi.seal.changedistiller.structuredifferencing.java.JavaStructureNode;
import ch.uzh.ifi.seal.changedistiller.treedifferencing.Node;
//import edu.vt.cs.changes.api.APIResolver;

public class CommonValue {
	public static String common_old_version;
	public static String common_new_version;	
	public static String common_project_name;
	public static String common_commit_number;
	public static String possible_lib_name1;
	public static String possible_lib_name2;	
	public static String workspace = "/home/shengzhex/Documents/research_repo/luna_api_migration/";
	public static List<String> leftimport;
	public static List<String> rightimport;
	public static List<StructureNode> pureaddedmethod;
	public static List<StructureNode> puredeletedmethod;
	public static int Exp_Sniffer = 0;
	public static int same_prefix = 0;
	public static int[][] de_add_relation;
	public static int[][] dependencymap;

	public static void set_possible_name(String name) {
		possible_lib_name1 = name;
		possible_lib_name2 = "L" + join("/", possible_lib_name1.split("[.]"));
	}
	
	public static void resetpure() {
		pureaddedmethod = new ArrayList<StructureNode>();
		puredeletedmethod = new ArrayList<StructureNode>();
	}
	
	public static void pureanalysis(List<File> oldFiles, List<File> newFiles,
			Map<String, org.eclipse.jdt.core.dom.ASTNode> leftTreeMap,
			Map<String, org.eclipse.jdt.core.dom.ASTNode> rightTreeMap) {
		for (StructureNode x : puredeletedmethod) {
			for (StructureNode y : pureaddedmethod) {
//				AbstractRefactoringHelper hlp = new MethodRefactoringHelper();
//				hlp.setThreshold(0.5);
//				double sim = hlp.similarity(x, y);
//				boolean isref = hlp.isRefactoring(x, y);
//				System.out.println(x.getName() + "->" + y.getName() + ":" + sim);
//				JavaASTNodeTypeConverter jatc;
//				JavaASTHelper jvast;
//				Node leftroot = jvast.createDeclarationTree((JavaStructureNode)x);
//				Node rightroot = jvast.createDeclarationTree((JavaStructureNode)y);
//				
//				TopDownTreeMatcher matcher = new TopDownTreeMatcher();
//		        matcher.match_filter(leftroot, rightroot);
//		        
//		        Map<Node, Node> unmatchedLeftToRight = matcher
//		            .getUnmatchedLeftToRight();
//		        double leftcount = unmatchedLeftToRight.size();
//		        double rightcount = matcher.getUnmatchedRightToLeft().size();
//				double sim = leftcount/leftroot.getChildCount();
			}
		}
	}

	
	public static void pureadd(StructureNode x) {
		pureaddedmethod.add(x);
	}
	
	public static void puredelete(StructureNode x) {
		puredeletedmethod.add(x);
	}
	
	public static void resetimports() {
		leftimport = new ArrayList<String>();
		rightimport = new ArrayList<String>();
	}
	
//	public static void set_current_resolver(APIResolver inre) {
//		current_resolver = inre;
//	}
	
	public static boolean checkleft(String x) {
		for (int i = 0;i<leftimport.size();i++) {
			String yy = leftimport.get(i);
			String[] yyarray = yy.split(Pattern.quote("."));
			if (x.equals(yyarray[yyarray.length-1])) {
				return true;
			}
		}
		return false;
	}
	public static boolean checkright(String x) {
		for (int i = 0;i<rightimport.size();i++) {
			String yy = rightimport.get(i);
			String[] yyarray = yy.split(Pattern.quote("."));
			if (x.equals(yyarray[yyarray.length-1])) {
				return true;
			}
		}
		return false;
	}
	
	public static List<String> pat_hash = new ArrayList<String>();
	public static List<String> snp_hash = new ArrayList<String>();
	
	public static void pushpattern(String oldp, String newp, String oldv, String newv, String type, String level) {
		String newone = oldp+"," + newp +","+ oldv+","+newv+"," + type + "," + level;
		pat_hash.add(newone);
	}
	public static void pushsnippet(String old_code, String new_code, String project, String commitnum) {
		String newone = old_code+"," + new_code +","+ project+","+commitnum;
		snp_hash.add(newone);
	}
	
	public static boolean isInteger(String s) {
		boolean isValidInteger = false;
	      try
	      {
	         Integer.parseInt(s);
	         // s is a valid integer
	         isValidInteger = true;
	      }
	      catch (NumberFormatException ex)
	      {
	         // s is not an integer
	      }
	      return isValidInteger;
	}

	
	public static boolean check_num(String xx) {
		String[] xxarray = xx.split(Pattern.quote("."));
		for (int i=0;i<xxarray.length;i++) {
			if (!isInteger(xxarray[i])) {
				return false;
			}
		}
		return true;
	}
	public static boolean check_larger(String xx, String yy) {
		String[] xxarray = xx.split(Pattern.quote("."));
		String[] yyarray = yy.split(Pattern.quote("."));
		int len = Math.min(xxarray.length, yyarray.length);
		for (int i=0;i<len;i++) {
			if (Integer.parseInt(xxarray[i]) > Integer.parseInt(yyarray[i])) {
				return true;
			}
			else if (Integer.parseInt(xxarray[i]) < Integer.parseInt(yyarray[i])) {
				return false;
			}
		}
        return xxarray.length > yyarray.length;
    }
	
	public static int compareAPI(String v1,String v2){
        String [] xx=v1.split("\\.");
        String [] yy=v2.split("\\.");
        int a=0;
        try {
            for(int x=0,y=0;x<xx.length||y<yy.length;x++,y++){
                int left=(x<xx.length)?Integer.parseInt(xx[x]):0;
                int right=(y<yy.length)?Integer.parseInt(yy[y]):0;
                if(left>right){
                    a=1;
                }else if(left==right){
                    continue;
                }else{
                    a=-1;
                }
                a=0;
            }
        } catch (Exception e) {
            // TODO: handle exception
        }
        return a;
    }
	
	public static String join(String join,String[] strAry){

        StringBuffer sb=new StringBuffer();

        for(int i=0;i<strAry.length;i++){

             if(i==(strAry.length-1)){

                 sb.append(strAry[i]);

             }else{

                 sb.append(strAry[i]).append(join);

             }

        }       

        return new String(sb);
	}
	
	public static void flush() {

		DatabaseControl data1 = new DatabaseControl();
		for (int i=0;i<pat_hash.size();i++) {
			if (pat_hash.get(i)=="") continue;
			String left = pat_hash.get(i).split(",")[2];
			String right = pat_hash.get(i).split(",")[3];
			if (!check_num(left) || !check_num(right)) continue;
			
			List<String> left_hash = new ArrayList<String>();
			List<String> right_hash = new ArrayList<String>();
			left_hash.add(pat_hash.get(i).split(",")[2]);
			right_hash.add(pat_hash.get(i).split(",")[3]);
			String x = pat_hash.get(i);
			int cont = 1;
			for (int j=i+1;j<pat_hash.size();j++) {
				if (pat_hash.get(j)=="") continue;
				String y = pat_hash.get(j);
				if (x.split(",")[0].equals(y.split(",")[0])) {
					left_hash.add(pat_hash.get(j).split(",")[2]);
					right_hash.add(pat_hash.get(j).split(",")[3]);
					if (check_larger(pat_hash.get(j).split(",")[2], left)) {
						left = pat_hash.get(j).split(",")[2];
					}
					if (check_larger(right, pat_hash.get(j).split(",")[3])) {
						right = pat_hash.get(j).split(",")[3];
					}
					cont++;
				}
			}
			
			if (cont>=5 && check_larger(right, left)) {
				for (int j=i+1;j<pat_hash.size();j++) {
					if (pat_hash.get(j)=="") continue;
					String y = pat_hash.get(j);
					if (x.split(",")[0].equals(y.split(",")[0])) {
						String[] arr = y.split(",");
						int label = data1.insertpattern(arr[0], arr[1], arr[2], arr[3], arr[4], arr[5]);
						String[] arr2 = snp_hash.get(j).split(",");
						data1.insertsnippet(arr2[0], arr2[1], arr2[2], arr2[3], String.valueOf(label), CommonValue.common_old_version+"-"+CommonValue.common_new_version);	
					}
					pat_hash.set(j, "");
				}
				pat_hash.set(i, "");
			}
		}
	}

}
