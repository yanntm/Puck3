package fr.lip6.puck.graph;

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

import org.eclipse.jdt.core.dom.IBinding;

import java.util.Map.Entry;


public class PuckGraph {
	private Map<String,Set<Integer>> setDeclarations = new HashMap<>();
	private List<Rule> rules = new ArrayList<>();
	private DependencyNodes nodes;
	private DependencyGraph useGraph;
	private DependencyGraph composeGraph;

	public PuckGraph(DependencyNodes nodes) {
		this.nodes = nodes;
		this.useGraph = new DependencyGraph(nodes.size());
	}
	
	public DependencyGraph getUseGraph() {
		return useGraph;
	}
	
	public DependencyNodes getNodes() {
		return nodes;
	}
	
	public DependencyGraph getComposeGraph() {
		return composeGraph;
	}
	
	public void addSetDeclaration(String name, Set<Integer> nodes) {
		setDeclarations.put(name, nodes);
	}
	
	public Set<Integer> getSetDeclaration (String name) {
		return setDeclarations.getOrDefault(name, Collections.emptySet());
	}

	public List<Rule> getRules() {
		return rules;
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
		nodes.dotExport(out);
		
		// usage edges
		useGraph.dotExport(out, "");
		
		// containment edges 
		composeGraph.dotExport(out, "[style=dotted]");
		
		boolean doRedArcs=true;
		if (! doRedArcs) {
			// named sets
			for (Entry<String, Set<Integer>> ent : setDeclarations.entrySet()) {
				out.println("  "+ent.getKey()+ " [color=blue] ;");
				for (Integer i : ent.getValue()) {
					out.println("  "+ent.getKey()+ " -> n" + i + " [color=blue] ;");				
				}
			}

			// broken right now
//			for (Rule rule : rules) {
//				out.println("  "+rule.hide+ " -> " + rule.from + " [color=red] ;");							
//			}
		} else {
			for (Rule rule : rules) {
				Set<Integer> from = new HashSet<>(rule.from);
				Set<Integer> hide = new HashSet<>(rule.hide);
				
				from.removeAll(hide);
				
				for (Integer interloper : from) {
					for (Integer secret : hide) {
						if (useGraph.hasEdge(secret, interloper) || composeGraph.hasEdge(secret, interloper)) {
							out.println("  n"+interloper+ " -> n" + secret + " [color=red] ;");														
						}
					}
				}
			}
		}

		out.println("}");
		out.close();
	}

	
	public static class Rule {
		public String text;
		public final Set<Integer> hide;
		public final Set<Integer> from;
		public Rule(Set<Integer> hide, Set<Integer> from, String text) {
			this.hide = hide;
			this.from = from;
			this.text = text;
		}
	}
	public void addRule (Set<Integer> hide, Set<Integer> from, String text) {
		this.rules.add (new Rule(hide,from, text));
	}
	
	public int findIndex(IBinding key) {
		return nodes.findIndex(key);
	}
	
	public int findIndex(String key) {
		return nodes.findIndex(key);
	}

}
