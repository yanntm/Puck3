package fr.lip6.puck.core.ui;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

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
			
			for (Integer interloper : rule.from) {
				for (Integer secret : rule.hide) {
					if (graph.getUseGraph().hasEdge(secret, interloper)) {
						List<ASTNode> explains = graph.getUseGraph().getReasons(secret,interloper); 
						addMarker(explains, "Violates Puck rule :"+ rule.text);
					}
					if (graph.getComposeGraph().hasEdge(secret, interloper)) {
						List<ASTNode> explains = graph.getUseGraph().getReasons(secret,interloper); 
						addMarker(explains, "Violates Puck containment rule :"+ rule.text);
					}
				}
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
