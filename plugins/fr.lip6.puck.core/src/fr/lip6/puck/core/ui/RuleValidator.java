package fr.lip6.puck.core.ui;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceProxy;
import org.eclipse.core.resources.IResourceProxyVisitor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IPackageFragment;
import org.eclipse.jdt.core.IPackageFragmentRoot;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.compiler.CompilationParticipant;
import org.eclipse.jdt.core.dom.CompilationUnit;

import fr.lip6.puck.core.ExtractGraph;
import fr.lip6.puck.core.GraphBuilder;

public class RuleValidator extends CompilationParticipant {

	public RuleValidator() {
	}

	@Override
	public boolean isActive(IJavaProject project) {
		try {
			for (IPackageFragmentRoot fragment : project.getAllPackageFragmentRoots()) {
				if (fragment.getKind() == IPackageFragmentRoot.K_SOURCE) {
					IResource res = fragment.getCorrespondingResource();
					if (res instanceof IFolder) {
						IFolder folder = (IFolder) res;
						boolean [] result = new boolean[1];
						folder.accept(new IResourceProxyVisitor() {						
							@Override
							public boolean visit(IResourceProxy proxy) throws CoreException {
								if (proxy.getType() == IResource.FOLDER) {
									if (! result[0])
										return true;
								} else if (proxy.getType() == IResource.FILE) {
									if (proxy.getName().endsWith(".wld")) {
										result[0] = true;
									}
								}
								return false;
							}
						}
						, IResource.DEPTH_INFINITE);
						if (result[0]) return true;
					}
				}
			}
		} catch (CoreException e) {
			// TODO Auto-generated catch block
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
			List<CompilationUnit> parsedCu = ExtractGraph.parseSources(project, sources.toArray(new ICompilationUnit[sources.size()]), null);
			GraphBuilder gb = GraphBuilder.collectGraph(parsedCu);
			ExtractGraph.collectRules(project, gb);
			
			gb.addErrorMarkers();
//			gb.getNodes().get(0).getJavaElement().getJavaModel();
			
		} catch (CoreException e) {
			e.printStackTrace();
		}
		return super.aboutToBuild(project);
	}
}
