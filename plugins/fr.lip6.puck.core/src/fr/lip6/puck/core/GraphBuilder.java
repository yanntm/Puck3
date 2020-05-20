package fr.lip6.puck.core;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Stack;

import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import android.util.SparseIntArray;
import fr.lip6.move.gal.util.MatrixCol;

public class GraphBuilder extends ASTVisitor {
	private List<IBinding> nodes;
	private List<IPackageBinding> packages;
	private List<ITypeBinding> types;
	private List<IMethodBinding> methods;
	private List<IVariableBinding> attributes;
	private MatrixCol useGraph;
	private Stack<Integer> currentOwner = new Stack<>();

	public GraphBuilder(List<IBinding> nodes, List<IPackageBinding> packages, List<ITypeBinding> types,
			List<IMethodBinding> methods, List<IVariableBinding> attributes) {
		this.nodes = nodes;
		this.packages = packages;
		this.types = types;
		this.methods = methods;
		this.attributes = attributes;
		this.useGraph = new MatrixCol(nodes.size(), nodes.size());
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
			addDependency(b);
		}
		super.preVisit(node);
	}	

	/**
	 * Add a dependency from top of curentOwner stack (if any) to the node referred to in the binding (if any).
	 * @param tb A @see {@link IBinding} resolved name that might point to a node of the graph.
	 */
	private void addDependency(IBinding tb) {
		if (!currentOwner.isEmpty()) {
			int indexSrc = currentOwner.peek();
			int indexDst = findIndex(nodes, tb);
			if (indexDst != indexSrc && indexDst >= 0 && indexSrc >= 0) {
				useGraph.set(indexDst, indexSrc, 1);
			}
		}
	}
	
	
	/**
	 * Owner becomes the current type declaration.
	 */
	@Override
	public boolean visit(TypeDeclaration node) {
		currentOwner.push(nodes.indexOf(node.resolveBinding()));		
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
		currentOwner.push(nodes.indexOf(node.resolveBinding()));		
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
			currentOwner.push(nodes.indexOf(node.resolveBinding()));
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
	 * Find the index of a node or -1 if not found.
	 * TODO : we should really use a Map<IBinding,Integer> 
	 * @param nodes the list
	 * @param tb the object
	 * @return the index of object in list or -1 if not found.
	 */
	public int findIndex(List<IBinding> nodes, IBinding tb) {
		int indexDst = nodes.indexOf(tb);
		return indexDst;
	}

	/**
	 * Find the index of a node or -1 if not found.
	 * TODO : we should really use a Map<IBinding,Integer> 
	 * @param nodes the list
	 * @param tb the object
	 * @return the index of object in list or -1 if not found.
	 */
	public int findIndex(List<IBinding> nodes, String tname) {
		int indexDst = -1;
		for (int i=0; i < nodes.size(); i++) {
			if (nodes.get(i).getName().equals(tname)) {
				return i;
			}
		}
		return indexDst;
	}

	public List<IBinding> getNodes() {
		return nodes;
	}
	/**
	 * Accessor for user : the result of traversing the compilation units.
	 * @return
	 */
	public MatrixCol getUseGraph() {
		return useGraph;
	}										

	/**
	 * A visual representation for our graphs.
	 * @param path where will we build this dot file 
	 * @throws IOException if we couldn't write to that place.
	 */
	public void exportDot (String path) throws IOException {
		PrintWriter out = new PrintWriter(new File(path));
		out.println("digraph  G {");
		int index = 0;
		for (IBinding elt : nodes) {
			if (elt instanceof ITypeBinding) {
				ITypeBinding tb = (ITypeBinding) elt;
				out.println("  n"+index+ " [shape=box,label=\""+tb.getQualifiedName()+"\"]");
			} else if (elt instanceof IVariableBinding) {
				IVariableBinding vb = (IVariableBinding) elt;
				out.println("  n"+index+ " [shape=diamond,label=\"" + vb.getDeclaringClass().getQualifiedName()+"."+vb.getName()+"\"];");
			} else if (elt instanceof IMethodBinding) {
				IMethodBinding mb = (IMethodBinding) elt;
				out.println("  n"+index+ " [shape=ellipse,label=\"" + mb.getDeclaringClass().getQualifiedName()+"."+mb.getName()+"\"];");
			}
			index++;
		}
		for (int coli=0,colie=useGraph.getColumnCount(); coli<colie;coli++ ) {
			SparseIntArray col = useGraph.getColumn(coli);
			for (int i=0,ie=col.size();i<ie;i++) {
				int colj = col.keyAt(i);
				out.println("  n"+coli+ " -> n" + colj + " ;");
			}
		}
		// containment edges
		for (IMethodBinding meth: methods) {
			int indexDst = findIndex(nodes, meth);
			int indexSrc = findIndex(nodes, meth.getDeclaringClass());
			if (indexDst >= 0 && indexSrc >= 0) {
				out.println("  n"+indexSrc+ " -> n" + indexDst + " [style=dotted] ;");
			}
		}
		for (IVariableBinding meth: attributes) {
			int indexDst = findIndex(nodes, meth);
			int indexSrc = findIndex(nodes, meth.getDeclaringClass());
			if (indexDst >= 0 && indexSrc >= 0) {
				out.println("  n"+indexSrc+ " -> n" + indexDst + " [style=dotted] ;");
			}
		}

		out.println("}");
		out.close();
	}

}
