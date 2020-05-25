package fr.lip6.puck.parse;

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

import fr.lip6.puck.graph.DependencyNodes;
import fr.lip6.puck.graph.PuckGraph;


/**
 * Traverses a set of input Java files to build the underlying containment and usage graphs.
 * @author Yann
 *
 */
public final class GraphBuilder  {

	/**
	 * Visits the parsed AST of these compilation units to first collect a set of nodes,
	 * then add all usage and containment edges to this graph.
	 * @param parsedCu A list of parsed Java DOM representations of sources.
	 * @return a graph containing the analysis results.
	 */
	public static PuckGraph collectGraph(List<CompilationUnit> parsedCu) {
		// The nodes we will collect in a first pass.
		DependencyNodes nodes = new DependencyNodes();		

		// First pass with a visitor.
		// This visitor is not stateful so we used an anonymous class.
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

		// Step 2 : we traverse the AST again, but this time we add edges to the picture
		// if either source or destination for a potential edge is not in the set of nodes
		// that we have collected above, the edge is discarded.

		// Let's initialize the graph with the context nodes we have collected.
		PuckGraph graph = new PuckGraph(nodes);

		// Now the graph builder; because it is stateful (it keeps track of which node is current owner) it is implemented as a separate Visitor class.
		EdgeCollector edgeCollector = new EdgeCollector(graph);

		// now build the graph dependency links using our visitor.
		for (CompilationUnit unit : parsedCu) {
			unit.accept(edgeCollector);
		}

		return graph;
	}

	/**
	 * A visitor for AST that collects edges for any usage or containment 
	 * where both nodes belong to the graph.
	 *
	 */
	private static class EdgeCollector extends ASTVisitor {
		private PuckGraph graph;

		private Stack<Integer> currentOwner = new Stack<>();

		public EdgeCollector(PuckGraph graph) {
			this.graph = graph;
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
			// is there a containment edge ?
			int cur = graph.getNodes().findIndex(type);
			if (!currentOwner.isEmpty() && cur != -1) {
				graph.getComposeGraph().addEdge(cur, currentOwner.peek(), node);
			}
			// also add that this is contained in its package
			// so a direct containment link from package to any type definition.
			int parent = graph.findIndex(type.getPackage());
			graph.getComposeGraph().addEdge(cur, parent, node);		
			// we are new owner
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
			if (!currentOwner.isEmpty() && cur != -1) {
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
				if (!currentOwner.isEmpty() && cur != -1) {
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

	}

	private GraphBuilder() {}
}
