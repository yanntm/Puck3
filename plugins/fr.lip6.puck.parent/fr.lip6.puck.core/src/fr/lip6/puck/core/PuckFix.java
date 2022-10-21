package fr.lip6.puck.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.refactoring.IJavaRefactorings;
import org.eclipse.jdt.core.refactoring.descriptors.RenameJavaElementDescriptor;
import org.eclipse.jdt.ui.text.java.IInvocationContext;
import org.eclipse.jdt.ui.text.java.IJavaCompletionProposal;
import org.eclipse.jdt.ui.text.java.IProblemLocation;
import org.eclipse.jdt.ui.text.java.IQuickFixProcessor;
import org.eclipse.jdt.ui.text.java.correction.ChangeCorrectionProposal;
import org.eclipse.ltk.core.refactoring.Change;
import org.eclipse.ltk.core.refactoring.Refactoring;
import org.eclipse.ltk.core.refactoring.RefactoringContribution;
import org.eclipse.ltk.core.refactoring.RefactoringCore;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;

public class PuckFix implements IQuickFixProcessor {

	@Override
	public boolean hasCorrections(ICompilationUnit unit, int problemId) {
		// TODO Auto-generated method stub
//		return false;
		return problemId == 666666;
//		return true;
	}

	@Override
	public IJavaCompletionProposal[] getCorrections(IInvocationContext context, IProblemLocation[] locations)
			throws CoreException {
		// TODO Auto-generated method stub

		ASTNode reason = context.getCoveredNode();
		String reasonName = reason.toString();
//		
		RefactoringContribution contribution =
				RefactoringCore.getRefactoringContribution(IJavaRefactorings.RENAME_TYPE);
		RenameJavaElementDescriptor descriptor =
				(RenameJavaElementDescriptor) contribution.createDescriptor();
		descriptor.setNewName(reasonName+"__CM"); // new name for a Class
		descriptor.setProject(((CompilationUnit) reason.getRoot()).getJavaElement().getElementName());
		SimpleType treason = (SimpleType) reason.getParent();
		System.out.println("treason = " + treason.toString());
		descriptor.setJavaElement(treason.resolveBinding().getJavaElement());
		RefactoringStatus status = new RefactoringStatus();
		Refactoring refactoring = descriptor.createRefactoring(status);
		IProgressMonitor monitor = new NullProgressMonitor();
		refactoring.checkInitialConditions(monitor);
		refactoring.checkFinalConditions(monitor);
		Change changeRename = refactoring.createChange(monitor);
		changeRename.perform(monitor);
		IJavaCompletionProposal proposals[] = new IJavaCompletionProposal[1];
		proposals[0] = new ChangeCorrectionProposal("Puck-Rename",changeRename,0);
//		proposals[0] = new ChangeCorrectionProposal("Puck-Rename",null,0);
		return proposals;
//		return null;
	}
}
