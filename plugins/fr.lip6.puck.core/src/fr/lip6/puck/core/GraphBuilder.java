package fr.lip6.puck.core;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.Stack;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.ASTVisitor;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.IPackageBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.Name;
import org.eclipse.jdt.core.dom.PackageDeclaration;
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
	private MatrixCol composeGraph = null;
	private Map<String,Set<Integer>> setDeclarations = new HashMap<>();
	private List<Rule> rules = new ArrayList<>();

	public GraphBuilder(List<IBinding> nodes, List<IPackageBinding> packages, List<ITypeBinding> types,
			List<IMethodBinding> methods, List<IVariableBinding> attributes) {
		this.nodes = nodes;
		this.packages = packages;
		this.types = types;
		this.methods = methods;
		this.attributes = attributes;
		nodes.addAll(packages);
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
			addDependency(b,node);
		}
		super.preVisit(node);
	}	

	private Map<Integer,Map<Integer,List<ASTNode>>> reasons = new HashMap<>();
	
	public Map<Integer, Map<Integer, List<ASTNode>>> getReasons() {
		return reasons;
	}
	/**
	 * Add a dependency from top of curentOwner stack (if any) to the node referred to in the binding (if any).
	 * @param tb A @see {@link IBinding} resolved name that might point to a node of the graph.
	 * @param node 
	 */
	private void addDependency(IBinding tb, ASTNode node) {
		if (!currentOwner.isEmpty()) {
			int indexSrc = currentOwner.peek();
			int indexDst = findIndex(nodes, tb);
			if (indexDst != indexSrc && indexDst >= 0 && indexSrc >= 0) {
				useGraph.set(indexDst, indexSrc, 1);
				reasons.computeIfAbsent(indexDst, k -> new HashMap<>()).computeIfAbsent(indexSrc, k -> new ArrayList<>()).add(node);
			}
		}
	}
	
	
	public static void collectPrefix(Set<Integer> safeNodes, MatrixCol graph) {
		// work with predecessor relationship
		MatrixCol tgraph = graph.transpose();
		collectSuffix(safeNodes, tgraph);
	}
		
	public static void collectSuffix(Set<Integer> safeNodes, MatrixCol graph) {

		Set<Integer> seen = new HashSet<>();
		List<Integer> todo = new ArrayList<>(safeNodes);
		while (! todo.isEmpty()) {
			List<Integer> next = new ArrayList<>();
			seen.addAll(todo);
			for (int n : todo) {
				SparseIntArray succ = graph.getColumn(n);
				for (int i=0; i < succ.size() ; i++) {
					int pre = succ.keyAt(i);
					if (seen.add(pre)) {
						next.add(pre);
					}
				}
			}
			todo = next;			
		}
		safeNodes.addAll(seen);
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
			IBinding elt = nodes.get(i);
			if (elt instanceof ITypeBinding) {
				ITypeBinding tb = (ITypeBinding) elt;
				String id = tb.getQualifiedName(); 
				if (id.equals(tname)) {
					return i;
				}
			} else if (elt instanceof IPackageBinding) {
				IPackageBinding pkg = (IPackageBinding) elt;
				String id = pkg.getName(); 
				if (id.equals(tname)) {
					return i;
				}
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
	 * Accessor for user : the result of traversing the compilation units.
	 * @return
	 */
	public MatrixCol getComposeGraph() {
		if (composeGraph == null) {
			for (ITypeBinding type : types) {
				IPackageBinding pkg = type.getPackage();
				if (!packages.contains(pkg)) {
					packages.add(pkg);
					nodes.add(pkg);
				}
			}
			composeGraph = new MatrixCol(nodes.size(), nodes.size());
			// containment edges
			for (IMethodBinding meth: methods) {
				int elt = findIndex(nodes, meth);
				int parent = findIndex(nodes, meth.getDeclaringClass());
				if (elt >= 0 && parent >= 0) {
					composeGraph.set(elt, parent, 1);
				}
			}
			for (IVariableBinding meth: attributes) {
				int elt = findIndex(nodes, meth);
				int parent = findIndex(nodes, meth.getDeclaringClass());
				if (elt >= 0 && parent >= 0) {
					composeGraph.set(elt, parent, 1);
				}
			}
			for (ITypeBinding type : types) {
				int elt = findIndex(nodes, type);
				int parent = findIndex(nodes, type.getPackage());
				if (elt >= 0 && parent >= 0) {
					composeGraph.set(elt, parent, 1);
				}
			}
		}
		return composeGraph;
	}

	/**
	 * A visual representation for our graphs.
	 * @param path where will we build this dot file 
	 * @throws IOException if we couldn't write to that place.
	 */
	public void exportDot (String path) throws IOException {
		getComposeGraph();
		PrintWriter out = new PrintWriter(new File(path));
		out.println("digraph  G {");
		int index = 0;
		for (IBinding elt : nodes) {
			if (elt instanceof ITypeBinding) {
				ITypeBinding tb = (ITypeBinding) elt;
				out.println("  n"+index+ " [shape=box,label=\""+tb.getQualifiedName()+"\"]");
			} else if (elt instanceof IVariableBinding) {
				IVariableBinding vb = (IVariableBinding) elt;
				out.println("  n"+index+ " [shape=doubleellipse,label=\"" + vb.getDeclaringClass().getQualifiedName()+"."+vb.getName()+"\"];");
			} else if (elt instanceof IMethodBinding) {
				IMethodBinding mb = (IMethodBinding) elt;
				out.println("  n"+index+ " [shape=octagon,label=\"" + mb.getDeclaringClass().getQualifiedName()+"."+mb.getName()+"\"];");
			} else if (elt instanceof IPackageBinding) {
				IPackageBinding pkg = (IPackageBinding) elt;
				out.println("  n"+index+ " [shape=folder,label=\"" + pkg.getName() +"\"];");				
			}
			index++;
		}
		
		// usage edges
		for (int coli=0,colie=useGraph.getColumnCount(); coli<colie;coli++ ) {
			SparseIntArray col = useGraph.getColumn(coli);
			for (int i=0,ie=col.size();i<ie;i++) {
				int colj = col.keyAt(i);
				out.println("  n"+coli+ " -> n" + colj + " ;");
			}
		}
		
		// containment edges 
		for (int coli=0,colie=getComposeGraph().getColumnCount(); coli<colie;coli++ ) {
			SparseIntArray col = getComposeGraph().getColumn(coli);
			for (int i=0,ie=col.size();i<ie;i++) {
				int colj = col.keyAt(i);
				out.println("  n"+coli+ " -> n" + colj + " [style=dotted] ;");
			}
		}
		
		boolean doRedArcs=true;
		if (! doRedArcs) {
			// named sets
			for (Entry<String, Set<Integer>> ent : setDeclarations.entrySet()) {
				out.println("  "+ent.getKey()+ " [color=blue] ;");
				for (Integer i : ent.getValue()) {
					out.println("  "+ent.getKey()+ " -> n" + i + " [color=blue] ;");				
				}
			}

			for (Rule rule : rules) {
				out.println("  "+rule.hide+ " -> " + rule.from + " [color=red] ;");							
			}
		} else {
			for (Rule rule : rules) {
				Set<Integer> from = new HashSet<>(setDeclarations.get(rule.from));
				Set<Integer> hide = new HashSet<>(setDeclarations.get(rule.hide));
				
				from.removeAll(hide);
				
				for (Integer interloper : from) {
					for (Integer secret : hide) {
						if (useGraph.get(secret, interloper) != 0 || composeGraph.get(secret, interloper) != 0) {
							out.println("  n"+interloper+ " -> n" + secret + " [color=red] ;");														
						}
					}
				}
			}
		}

		out.println("}");
		out.close();
	}

	public void addErrorMarkers() throws JavaModelException, CoreException {
		getComposeGraph();
		for (Rule rule : rules) {
			Set<Integer> from = new HashSet<>(setDeclarations.get(rule.from));
			Set<Integer> hide = new HashSet<>(setDeclarations.get(rule.hide));
			
			from.removeAll(hide);
			
			for (Integer interloper : from) {
				for (Integer secret : hide) {
					if (useGraph.get(secret, interloper) != 0) {
						List<ASTNode> explains = reasons.getOrDefault(secret, Collections.emptyMap()).getOrDefault(interloper, Collections.emptyList());
						for (ASTNode reason : explains) {
							ASTNode root = reason.getRoot();
							if (root.getNodeType() == ASTNode.COMPILATION_UNIT) {
								CompilationUnit cu = (CompilationUnit) root;
								IMarker marker = cu.getJavaElement().getCorrespondingResource().createMarker(IMarker.PROBLEM);
								marker.setAttribute(IMarker.CHAR_START, reason.getStartPosition());
								marker.setAttribute(IMarker.CHAR_END, reason.getStartPosition() + reason.getLength());
								marker.setAttribute(IMarker.MESSAGE, "Violates Puck rule : hide " + rule.hide + " from " + rule.from);
								marker.setAttribute(IMarker.PRIORITY, IMarker.PRIORITY_HIGH);
								marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
							}
						}
					}
				}
			}
		}

	}
	
	public void addSetDeclarations(Map<String, Set<Integer>> sets, Map<String, Set<Integer>> except, boolean andChildren) {
		for (Entry<String, Set<Integer>> ent : sets.entrySet()) {
			Set<Integer> basis = new HashSet<>(ent.getValue());
			if (andChildren) {
				collectSuffix(basis, getComposeGraph());
				Set<Integer> ex = except.get(ent.getKey());
				if (ex != null) {
					ex = new HashSet<> (ex);
					if (andChildren) {
						collectSuffix(ex, getComposeGraph());						
					}
					basis.removeAll(ex);
				}
			}
			setDeclarations.put(ent.getKey(), basis);
		}
	}
	
	
	public static GraphBuilder collectGraph(List<CompilationUnit> parsedCu) {
		// this holds the actual nodes, at their proper index. All nodes are here, this is the union of types+methods+attributes.
		// packages are currently excluded from this list.
		List<IBinding> nodes = new ArrayList<>();

		// these separate lists of nodes by subtype also benefit from tighter type constraints, for refined analysis/use.
		// they form a partion of nodes + packages.
		List<IPackageBinding> packages = new ArrayList<>();
		List<ITypeBinding> types = new ArrayList<>();
		List<IMethodBinding> methods = new ArrayList<>();
		List<IVariableBinding> attributes = new ArrayList<>();
		
		
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
					nodes.add(itb);
					types.add(itb);
					for (MethodDeclaration meth : node.getMethods()) {
						IMethodBinding mtb = meth.resolveBinding();
						nodes.add(mtb);
						methods.add(mtb);
					}

					for (FieldDeclaration att : node.getFields()) {
						for (Object toc : att.fragments()) {
							VariableDeclarationFragment vdf = (VariableDeclarationFragment) toc;
							IVariableBinding ivb = vdf.resolveBinding();
							nodes.add(ivb);
							attributes.add(ivb);
						}
					}
					super.endVisit(node);
				}

				/**
				 * Currently separately collecting package nodes. They are actually unused currently. 
				 */
				@Override
				public void endVisit(PackageDeclaration node) {
					IPackageBinding ipb = node.resolveBinding();
					if (! packages.contains(ipb))
						packages.add(ipb);
					super.endVisit(node);
				}				
			});
		}
		
		System.out.println("Found " + nodes.size() + " nodes : " + printNodes(nodes));
		
		// Now the graph builder; because it is stateful (it keeps track of which node is current owner) it is implemented as a separate Visitor class.
		// Let's give it the context we have collected.
		GraphBuilder gb = new GraphBuilder(nodes,packages,types,methods,attributes);
		
		// now build the graph dependency links.
		for (CompilationUnit unit : parsedCu) {
			unit.accept(gb);
		}
		return gb;
	}

	
	private static class Rule {
		public final String hide;
		public final String from;
		public Rule(String hide, String from) {
			this.hide = hide;
			this.from = from;
		}
	}
	public void addRule (String hide, String from) {
		this.rules .add (new Rule(hide,from));
	}

	
	/**
	 * Debug method used for printing graph in console mode.
	 * @param nodes the list of nodes we built
	 * @return a nice-ish String representation for these nodes.
	 */
	public static String printNodes(List<? extends IBinding> nodes) {
		StringBuilder sb = new StringBuilder();
		boolean first = true;
		for (IBinding b : nodes) {
			if (first) {
				first = false;
			} else {
				sb.append(", ");
			}
			if (b instanceof ITypeBinding) {
				ITypeBinding tb = (ITypeBinding) b;
				sb.append("TYPE:"+tb.getQualifiedName());				
			} else if (b instanceof IMethodBinding) {
				IMethodBinding mb = (IMethodBinding) b;
				sb.append("METHOD:"+mb.getDeclaringClass().getQualifiedName()+ "::"  +mb);				
			} else if (b instanceof IPackageBinding) {
				IPackageBinding pb = (IPackageBinding) b;
				sb.append("PACKAGE:"+pb.getName());
			} else if (b instanceof IVariableBinding) {
				IVariableBinding vb = (IVariableBinding) b;
				sb.append("FIELD:"+vb.getDeclaringClass().getQualifiedName()+ "::"  +vb);
			}
		}
		return sb.toString();		
	}	

}
