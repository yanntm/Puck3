package fr.lip6.puck.core.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.jdt.core.dom.CompilationUnit;

import fr.lip6.puck.graph.PuckGraph;
import fr.lip6.puck.parse.GraphBuilder;
import fr.lip6.puck.parse.JavaParserHelper;
import fr.lip6.puck.parse.PuckInterpreter;

/**
 * This action is registered to be called on each recompilation, and will
 * generate the error markers corresponding to rule violations.
 * @author Yann
 *
 */
public class RuleValidator extends CompilationParticipant {

	public RuleValidator() {
	}

	@Override
	public boolean isActive(IJavaProject project) {
		try {
			boolean [] res = new boolean[1];
			JavaParserHelper.findRuleFiles(project, x -> {res[0]=true;return null;});
			return res[0];
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public int aboutToBuild(IJavaProject project) {
		try {
			List<ICompilationUnit> sources = new ArrayList<>();
			for (IPackageFragment pkg : project.getPackageFragments()) {
				if (pkg.getKind() == IPackageFragmentRoot.K_SOURCE) {
					for (IJavaElement elt : pkg.getChildren()) {
						if (elt.getElementType() == IJavaElement.COMPILATION_UNIT) {
							ICompilationUnit cu = (ICompilationUnit) elt	;
							cu.getCorrespondingResource().deleteMarkers(IMarker.PROBLEM, 
								      true, IResource.DEPTH_INFINITE); 
							sources.add(cu);
						}
					}
				}
			}
			List<CompilationUnit> parsedCu = JavaParserHelper.parseSources(project, sources.toArray(new ICompilationUnit[sources.size()]), null);
			PuckGraph graph = GraphBuilder.collectGraph(parsedCu);
			PuckInterpreter.findAndParsePuckFiles(project, graph);

			ProblemMarkerManager.addErrorMarkers(graph);
//			gb.getNodes().get(0).getJavaElement().getJavaModel();
			
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return super.aboutToBuild(project);
	}
}
