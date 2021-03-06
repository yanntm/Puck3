package fr.lip6.puck.core.ui;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import android.util.SparseIntArray;
import fr.lip6.puck.graph.DependencyGraph;
import fr.lip6.puck.graph.PuckGraph;
import fr.lip6.puck.graph.PuckGraph.Rule;

/**
 * This utility class manages building actual markers corresponding 
 * to the information computed in a PuckGraph.
 * @author Yann
 *
 */
public class ProblemMarkerManager {

	public static void addErrorMarkers(PuckGraph graph) throws JavaModelException, CoreException {
		for (Rule rule : graph.getRules()) {

			SparseIntArray tohide = new SparseIntArray();
			for (int index : rule.hide) {
				tohide.put(index, 1);
			}

			for (Integer interloper : rule.from) {
				addViolations(graph.getUseGraph(), ("Violates Puck rule :"+ rule.text).replaceAll("\n", ""), tohide, interloper);
				addViolations(graph.getComposeGraph(), ("Violates Puck containment rule :"+ rule.text).replaceAll("\n", ""), tohide, interloper);				
			}
		}
	}


	private static void addViolations(DependencyGraph dg, String message, SparseIntArray tohide, int interloper) throws CoreException {
		SparseIntArray arcs = dg.getGraph().getColumn(interloper);
		forEachIntersect(tohide, arcs, interloper, dg, message);
	}
	
    public static void forEachIntersect (SparseIntArray s1, SparseIntArray s2, int interloper, DependencyGraph dg, String message) throws CoreException {
    	if (s1.size() == 0 || s2.size() == 0) {
			return;
		}				
		
		for (int j = 0, i = 0 , ss1 =  s1.size() , ss2 = s2.size() ; i < ss1 && j < ss2 ; ) {
			int sk1 = s1.keyAt(i); 
			int sk2 = s2.keyAt(j); 
			if (sk1 == sk2) {
				// do it
				List<ASTNode> explains = dg.getReasons(sk1,interloper); 
				addMarker(explains, message);
				// end action
				i++;
				j++;
			} else if (sk1 > sk2) {
				j++;
			} else {
				i++;
			}
		}
    }

	
	
	private static void addMarker(List<ASTNode> explains, String message) throws CoreException, JavaModelException {
		for (ASTNode reason : explains) {
			ASTNode root = reason.getRoot();
			if (root.getNodeType() == ASTNode.COMPILATION_UNIT) {
				CompilationUnit cu = (CompilationUnit) root;
				IMarker marker = cu.getJavaElement().getCorrespondingResource().createMarker(IMarker.PROBLEM);
				marker.setAttribute(IMarker.CHAR_START, reason.getStartPosition());
				marker.setAttribute(IMarker.CHAR_END, reason.getStartPosition() + reason.getLength());
				marker.setAttribute(IMarker.MESSAGE, message);
				marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
			}
		}
	}

}
