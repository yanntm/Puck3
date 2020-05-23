package fr.lip6.puck.core;

import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class GraphBuilder extends ASTVisitor {
	private PuckGraph graph;
	private Stack<Integer> currentOwner = new Stack<>();

	public GraphBuilder(DependencyNodes nodes) {
		this.graph = new PuckGraph(nodes);
	}

	/**
	 * The actual visit a "Name" case, resolve the node it points to, and add a dependency to it.
	 * @param node every node in the full AST gets this method invoked on it; the other visit methods are more selective, this is a catch-all.
	 */
	@Override
	public void preVisit(ASTNode node) {
		if (node instanceof Name) {
			Name name = (Name) node;
			IBinding b = name.resolveBinding();
			addDependency(b,node);
		}
		super.preVisit(node);
	}	

	/**
	 * Add a dependency from top of curentOwner stack (if any) to the node referred to in the binding (if any).
	 * @param tb A @see {@link IBinding} resolved name that might point to a node of the graph.
	 * @param node 
	 */
	private void addDependency(IBinding tb, ASTNode node) {
		if (!currentOwner.isEmpty()) {
			int indexSrc = currentOwner.peek();
			int indexDst = graph.getNodes().findIndex(tb);
			if (indexDst != indexSrc && indexDst >= 0 && indexSrc >= 0) {
				graph.getUseGraph().addEdge(indexDst, indexSrc, node);
			}
		}
	}	
	
	/**
	 * Owner becomes the current type declaration.
	 */
	@Override
	public boolean visit(TypeDeclaration node) {
		ITypeBinding type = node.resolveBinding();
		int cur = graph.getNodes().findIndex(type);
		if (!currentOwner.isEmpty()) {
			graph.getComposeGraph().addEdge(cur, currentOwner.peek(), node);
		}
		int parent = graph.findIndex(type.getPackage());
		graph.getComposeGraph().addEdge(cur, parent, node);		
		currentOwner.push(cur);		
		return true;
	}

	@Override
	public void endVisit(TypeDeclaration node) {
		currentOwner.pop();		
	}

	/**
	 * Current owner becomes the method declaration.
	 */
	@Override
	public boolean visit(MethodDeclaration node) {
		int cur = graph.getNodes().findIndex(node.resolveBinding());
		if (!currentOwner.isEmpty()) {
			graph.getComposeGraph().addEdge(cur, currentOwner.peek(), node);
		}
		currentOwner.push(cur);
		return true;
	}

	@Override
	public void endVisit(MethodDeclaration node) {
		currentOwner.pop(); 
	}

	/**
	 * Current owner becomes the field declaration.
	 */
	@Override
	public boolean visit(VariableDeclarationFragment node) {
		if (node.getParent() instanceof FieldDeclaration) {
			int cur = graph.getNodes().findIndex(node.resolveBinding());
			if (!currentOwner.isEmpty()) {
				graph.getComposeGraph().addEdge(cur, currentOwner.peek(), node);
			}
			currentOwner.push(cur);
		}
		return true;
	}

	@Override
	public void endVisit(VariableDeclarationFragment node) {
		if (node.getParent() instanceof FieldDeclaration) {
			currentOwner.pop();
		} 
	}

	/**
	 * Accessor for user : the result of traversing the compilation units.
	 * @return
	 */
	public PuckGraph getGraph() {
		return graph;
	}
	
//	/**
//	 * Accessor for user : the result of traversing the compilation units.
//	 * @return
//	 */
//	public MatrixCol getComposeGraph() {
//		if (composeGraph == null) {
//			for (ITypeBinding type : types) {
//				IPackageBinding pkg = type.getPackage();
//				if (!packages.contains(pkg)) {
//					packages.add(pkg);
//					nodes.add(pkg);
//				}
//			}
//			composeGraph = new MatrixCol(nodes.size(), nodes.size());
//			// containment edges
//			for (IMethodBinding meth: methods) {
//				int elt = findIndex(nodes, meth);
//				int parent = findIndex(nodes, meth.getDeclaringClass());
//				if (elt >= 0 && parent >= 0) {
//					composeGraph.set(elt, parent, 1);
//				}
//			}
//			for (IVariableBinding meth: attributes) {
//				int elt = findIndex(nodes, meth);
//				int parent = findIndex(nodes, meth.getDeclaringClass());
//				if (elt >= 0 && parent >= 0) {
//					composeGraph.set(elt, parent, 1);
//				}
//			}
//			for (ITypeBinding type : types) {
//				int elt = findIndex(nodes, type);
//				int parent = findIndex(nodes, type.getPackage());
//				if (elt >= 0 && parent >= 0) {
//					composeGraph.set(elt, parent, 1);
//				}
//			}
//		}
//		return composeGraph;
//	}

	public static PuckGraph collectGraph(List<CompilationUnit> parsedCu) {
		DependencyNodes nodes = new DependencyNodes();		
		
		// actual traversal to find all relevant nodes we will consider within the scope of our graph. 
		for (CompilationUnit unit : parsedCu) {

			unit.accept(new ASTVisitor() {
				/**
				 * Deal with TypeDeclaration : classes + interfaces : add nodes for them, their methods, their attributes.
				 *  
				 */
				@Override
				public void endVisit(TypeDeclaration node) {
					ITypeBinding itb = node.resolveBinding();
					nodes.addType(itb);
					for (MethodDeclaration meth : node.getMethods()) {
						IMethodBinding mtb = meth.resolveBinding();
						nodes.addMethod(mtb);
					}

					for (FieldDeclaration att : node.getFields()) {
						for (Object toc : att.fragments()) {
							VariableDeclarationFragment vdf = (VariableDeclarationFragment) toc;
							IVariableBinding ivb = vdf.resolveBinding();
							nodes.addAttribute(ivb);
						}
					}
					super.endVisit(node);
				}

				@Override
				public void endVisit(PackageDeclaration node) {
					nodes.addPackage(node.resolveBinding());
					super.endVisit(node);
				}				
			});
		}
		
		System.out.println("Found " + nodes.size() + " nodes : " + nodes);
		
		// Now the graph builder; because it is stateful (it keeps track of which node is current owner) it is implemented as a separate Visitor class.
		// Let's give it the context we have collected.
		GraphBuilder gb = new GraphBuilder(nodes);
		
		// now build the graph dependency links.
		for (CompilationUnit unit : parsedCu) {
			unit.accept(gb);
		}
		return gb.graph;
	}

}
