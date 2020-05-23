package fr.lip6.puck.core.ui;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;

import fr.lip6.puck.core.PuckGraph;
import fr.lip6.puck.core.PuckGraph.Rule;

public class ProblemMarkerManager {

	public static void addErrorMarkers(PuckGraph graph) throws JavaModelException, CoreException {
		for (Rule rule : graph.getRules()) {
			Set<Integer> from = new HashSet<>(rule.from);
			Set<Integer> hide = new HashSet<>(rule.hide);
			
			from.removeAll(hide);
			
			for (Integer interloper : from) {
				for (Integer secret : hide) {
					if (graph.getUseGraph().hasEdge(secret, interloper)) {
						List<ASTNode> explains = graph.getUseGraph().getReasons(secret,interloper); 
						for (ASTNode reason : explains) {
							ASTNode root = reason.getRoot();
							if (root.getNodeType() == ASTNode.COMPILATION_UNIT) {
								CompilationUnit cu = (CompilationUnit) root;
								IMarker marker = cu.getJavaElement().getCorrespondingResource().createMarker(IMarker.PROBLEM);
								marker.setAttribute(IMarker.CHAR_START, reason.getStartPosition());
								marker.setAttribute(IMarker.CHAR_END, reason.getStartPosition() + reason.getLength());
								marker.setAttribute(IMarker.MESSAGE, "Violates Puck rule :"+ rule.text);
								marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
								marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
							}
						}
					}
				}
			}
		}
	
	}

}
