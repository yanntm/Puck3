package fr.lip6.puck.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.ASTRequestor;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.internal.ui.fix.AbstractCleanUp;
import org.eclipse.jdt.ui.cleanup.ICleanUp;
import org.eclipse.ltk.core.refactoring.RefactoringStatus;


import fr.lip6.move.gal.util.MatrixCol;
import fr.lip6.puck.dsl.puck.AllReference;
import fr.lip6.puck.dsl.puck.NodeReference;
import fr.lip6.puck.dsl.puck.NodeSet;
import fr.lip6.puck.dsl.puck.PackageReference;
import fr.lip6.puck.dsl.puck.PuckModel;
import fr.lip6.puck.dsl.puck.Rule;
import fr.lip6.puck.dsl.puck.SetDeclaration;
import fr.lip6.puck.dsl.puck.SetReference;
import fr.lip6.puck.dsl.puck.TypeReference;
import fr.lip6.puck.dsl.serialization.SerializationUtil;

//@SuppressWarnings("restriction") // yes, AbstractCleanup is kind of internal API to JDT, but for this use case it's fine.
public class ExtractGraph extends AbstractCleanUp implements ICleanUp {

	@Override
	public RefactoringStatus checkPreConditions(IJavaProject project, ICompilationUnit[] compilationUnits,
			IProgressMonitor monitor) throws CoreException {

		List<CompilationUnit> parsedCu = parseSources(project, compilationUnits,monitor);
		
		// first traversal : grab packages, types, method declarations, attribute declarations
		
		GraphBuilder gb = GraphBuilder.collectGraph(parsedCu);
		
		// let's have a look at it !
		MatrixCol useGraph = gb.getUseGraph();
		System.out.println(useGraph);		
		
		collectRules(project, gb);
		
		// let's build a graphviz file for it
		try {
			gb.exportDot(project.getProject().getLocation().toFile().getCanonicalPath() + "/graph.dot");
		} catch (IOException e) {
			e.printStackTrace();
		}

		
		return new RefactoringStatus();
	}

	public static void collectRules(IJavaProject project, GraphBuilder gb) throws JavaModelException, CoreException {
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
									try {
										IResource member = proxy.requestResource();
										String file = member.getLocation().toFile().getCanonicalPath();
										PuckModel pm  = SerializationUtil.resourceToPuckModel(member);
										System.out.println("Found a file " + file + " containing " + pm.getRules().size() + " rules.");
										Map<String,Set<Integer>> sets = new HashMap<>();
										Map<String,Set<Integer>> excepts = new HashMap<>();
										for (SetDeclaration set : pm.getNamedSets()){
											Set<Integer> nodes = new HashSet<>();
											collectSet(gb, set.getNodes(), nodes);
											
											if (set.getExcept() != null) {
												Set<Integer> except =  new HashSet<>();
												collectSet(gb, set.getExcept(), except);
												nodes.removeAll(except);
											}
											gb.addSetDeclaration(set.getName(), nodes);
										}
										System.out.println("Parsed " + sets);
										for (Rule rule : pm.getRules()) {
											gb.addRule(rule.getHide().getName(), rule.getFrom().getName());
										}
									} catch (IOException e) {
										e.printStackTrace();
									}
								}
							}
							return false;
						}

						private void collectSet(GraphBuilder gb, NodeSet set, Set<Integer> nodes) {
							for (NodeReference elt : set.getNodes()) {
								if (elt instanceof TypeReference) {
									TypeReference tref = (TypeReference) elt;
									String key = tref.getType().getIdentifier();
									//IJvm
									int index = gb.findIndex(gb.getNodes(), key);
									if (index != -1) {
										nodes.add(index);
									} else {
										System.out.println(" not found " + key);
									}
								} else if (elt instanceof PackageReference) {
									PackageReference pkg = (PackageReference) elt;
									String key = pkg.getPackage();
									//IJvm
									int index = gb.findIndex(gb.getNodes(), key);
									if (index != -1) {
										nodes.add(index);
									} else {
										System.out.println(" not found " + key);
									}
									
								} else if (elt instanceof AllReference) {
									for (int index=0, ie=gb.getNodes().size() ; index < ie ; index++) {
										nodes.add(index);
									}
								} else if (elt instanceof SetReference) {
									SetReference sref = (SetReference) elt;
									nodes.addAll(gb.getSetDeclaration(sref.getRef().getName()));
								}
							}
							GraphBuilder.collectSuffix(nodes, gb.getComposeGraph());
						}
					}, IResource.DEPTH_INFINITE);
				}
			}
		}
	}

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

}
