package fr.lip6.puck.core.ui;

import java.util.List;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaModelMarker;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
// import org.eclipse.jdt.internal.ui.refactoring.reorg.RenameRefactoringWizard;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

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
				//				IMarker marker = cu.getJavaElement().getCorrespondingResource().createMarker(IMarker.PROBLEM);
				IMarker marker = cu.getJavaElement().getCorrespondingResource().createMarker("fr.lip6.puck.core.violationMarker");
				marker.setAttribute(IMarker.CHAR_START, reason.getStartPosition());
				marker.setAttribute(IMarker.CHAR_END, reason.getStartPosition() + reason.getLength());
				marker.setAttribute(IMarker.MESSAGE, message);
				marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
				marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
				marker.setAttribute(IJavaModelMarker.ID, 666666);
				rename(reason);
			}
		}
	}

	private static void rename(ASTNode reason) {
		String reasonName = reason.toString();
//		System.err.println("compilation unit" + "("+ ASTNode.COMPILATION_UNIT+")");
//		System.err.println("method declaration" + "("+ ASTNode.METHOD_DECLARATION+")");
//		System.err.println("method invocation" + "("+ ASTNode.METHOD_INVOCATION+")");
//		System.err.println("field declaration" + "("+ ASTNode.FIELD_DECLARATION+")");
//		System.err.println("field access" + "("+ ASTNode.FIELD_ACCESS+")");
//		System.err.println("type declaration" + "("+ ASTNode.TYPE_DECLARATION+")");
//		System.err.println("type literal" + "("+ ASTNode.TYPE_LITERAL+")");
		System.err.println("reasonName = " + reasonName + " / " + "Reason NodeType = " + ASTNode.nodeClassForType(reason.getParent().getNodeType()).getSimpleName());
		switch (reason.getParent().getNodeType()) {
		case ASTNode.COMPILATION_UNIT : 
			System.err.println("compilation unit" + "("+ ASTNode.COMPILATION_UNIT+")" + reasonName);
			break;
		case ASTNode.METHOD_DECLARATION :
			System.err.println("method declaration" + "("+ ASTNode.METHOD_DECLARATION+")" + reasonName);
			break;
		case ASTNode.METHOD_INVOCATION : 
			System.err.println("method invocation" + "("+ ASTNode.METHOD_INVOCATION+")" + reasonName);
			break;
		case ASTNode.FIELD_DECLARATION : 
			System.err.println("field declaration" + "("+ ASTNode.FIELD_DECLARATION+")" + reasonName);
			break;
		case ASTNode.FIELD_ACCESS :  
			System.err.println("field access" + "("+ ASTNode.FIELD_ACCESS+")" + reasonName);
			break;
		case ASTNode.TYPE_LITERAL :  
			System.err.println("type literal" + "("+ ASTNode.TYPE_LITERAL+")" + reasonName);
			break;
		case ASTNode.SIMPLE_TYPE :  
			System.err.println("Simple type" + "("+ ASTNode.SIMPLE_TYPE+")" + reasonName);
			RefactoringContribution contribution =
					RefactoringCore.getRefactoringContribution(IJavaRefactorings.RENAME_TYPE);
			RenameJavaElementDescriptor descriptor =
					(RenameJavaElementDescriptor) contribution.createDescriptor();
			descriptor.setNewName(reasonName+"__CM"); // new name for a Class
			descriptor.setProject(((CompilationUnit) reason.getRoot()).getJavaElement().getElementName());
			SimpleType treason = (SimpleType) reason.getParent();
			System.err.println("treason = " + treason.toString());
			descriptor.setJavaElement(treason.resolveBinding().getJavaElement());
			RefactoringStatus status = new RefactoringStatus();
			try {
				Refactoring refactoring = descriptor.createRefactoring(status);

				IProgressMonitor monitor = new NullProgressMonitor();
				refactoring.checkInitialConditions(monitor);
				refactoring.checkFinalConditions(monitor);
				Change change = refactoring.createChange(monitor);
				change.perform(monitor);

			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}			
			break;
		default :
			System.err.println("No type?");
		}
	}
}
