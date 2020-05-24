package fr.lip6.puck.core.ui;

import java.io.IOException;
import java.util.List;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.internal.ui.fix.AbstractCleanUp;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

import fr.lip6.puck.graph.PuckGraph;
import fr.lip6.puck.parse.GraphBuilder;
import fr.lip6.puck.parse.JavaParserHelper;
import fr.lip6.puck.parse.PuckInterpreter;

//@SuppressWarnings("restriction") // yes, AbstractCleanup is kind of internal API to JDT, but for this use case it's fine.
public class ExtractGraph extends AbstractCleanUp implements ICleanUp {

	@Override
	public RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits,
			IProgressMonitor monitor) throws CoreException {

		List<CompilationUnit> parsedCu = JavaParserHelper.parseSources(project, compilationUnits,monitor);
		
		// first traversal : grab packages, types, method declarations, attribute declarations
		
		PuckGraph graph = GraphBuilder.collectGraph(parsedCu);
		
		// let's have a look at it !
		//System.out.println(graph);		
		
		PuckInterpreter.findAndParsePuckFiles(project, graph);
		
		// let's build a graphviz file for it
		try {
			graph.exportDot(project.getProject().getLocation().toFile().getCanonicalPath() + "/graph.dot");
		} catch (IOException e) {
			e.printStackTrace();
		}

		
		return new RefactoringStatus();
	}

}
