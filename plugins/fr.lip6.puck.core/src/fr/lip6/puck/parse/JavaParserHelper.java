package fr.lip6.puck.parse;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.CompilationUnit;

public class JavaParserHelper {

	/**
	 * Go for a full parse+load into memory, with Binding resolution activated for the set of sources.
	 * This one pass parse ensures that IBindings resolve to a single instance in memory for a given object
	 * therefore we can lookup and store IBinding easily in the code (hash, equals, == all OK).
	 * 
	 * @param project input Java project carrying important classpath/JLS version etc...
	 * @param compilationUnits The set of input files we are considering as our scope.
	 * @param monitor a progress monitor, just forwarded to the parser.
	 * @return a set of fully resolved and parsed JDT DOM style document model for Java AST. We can use "node.resolveBinding()" in the resulting CompilationUnit.
	 */
	public static List<CompilationUnit> parseSources(IJavaProject project, ICompilationUnit[] compilationUnits, IProgressMonitor monitor) {
		
		// Java 8 setting by default, but this setting is overruled by setProject below so irrelevant anyway.
		ASTParser parser = ASTParser.newParser(AST.JLS8);
		// parse java files
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
		// Important non default setting : resolve name/type bindings for us ! yes, please, thanks !
		parser.setResolveBindings(true);
		// actually this overrides a bunch of settings, adds the correct classpath etc... to the underlying ASTresolver/JDT compiler.
		parser.setProject(project);
		// Parse the set of input files in one big pass, collect results in parsedCu
		List<CompilationUnit> parsedCu = new ArrayList<>();
		parser.createASTs(compilationUnits, new String[0], new ASTRequestor() {
			@Override
			public void acceptAST(ICompilationUnit source, CompilationUnit ast) {
				parsedCu.add(ast);
				super.acceptAST(source, ast);
			}
		}, monitor);
		return parsedCu;
	}

	/**
	 * Scan a Java project looking for Puck rule definition files.
	 * If any are found, callback.
	 * @param project
	 * @param graph
	 * @throws JavaModelException
	 * @throws CoreException
	 */
	public static void findRuleFiles(IJavaProject project, Function<IFile,Void> todo) throws JavaModelException, CoreException {
		for (IPackageFragmentRoot fragment : project.getAllPackageFragmentRoots()) {
			if (fragment.getKind() == IPackageFragmentRoot.K_SOURCE) {
				IResource res = fragment.getCorrespondingResource();
				if (res instanceof IFolder) {
					IFolder folder = (IFolder) res;
					folder.accept(new IResourceProxyVisitor() {						
						@Override
						public boolean visit(IResourceProxy proxy) throws CoreException {
							if (proxy.getType() == IResource.FOLDER) {
								return true;
							} else if (proxy.getType() == IResource.FILE) {
								if (proxy.getName().endsWith(".wld")) {
									IResource member = proxy.requestResource();
									todo.apply((IFile) member);
								}
							}
							return false;
						}
	
					}, IResource.DEPTH_INFINITE);
				}
			}
		}
	}

}
