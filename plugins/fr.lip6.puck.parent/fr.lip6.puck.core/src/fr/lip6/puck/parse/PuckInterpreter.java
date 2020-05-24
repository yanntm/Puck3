package fr.lip6.puck.parse;

import java.util.HashSet;
import java.util.Set;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.IJavaProject;

import fr.lip6.puck.dsl.puck.AllReference;
import fr.lip6.puck.dsl.puck.NodeReference;
import fr.lip6.puck.dsl.puck.NodeSet;
import fr.lip6.puck.dsl.puck.PackageReference;
import fr.lip6.puck.dsl.puck.PuckModel;
import fr.lip6.puck.dsl.puck.Rule;
import fr.lip6.puck.dsl.puck.SetDeclaration;
import fr.lip6.puck.dsl.puck.SetDefinition;
import fr.lip6.puck.dsl.puck.SetReference;
import fr.lip6.puck.dsl.puck.TypeReference;
import fr.lip6.puck.dsl.serialization.SerializationUtil;
import fr.lip6.puck.graph.PuckGraph;

/**
 * PuckInterpreter translates a PuckModel to operational rules on a PuckGraph.
 * 
 * Given a PuckGraph representing some sources, interpret a given PuckModel
 * (i.e. content of a WLD file parsed throug Xtext) to add the set declarations
 * and rules to te graph.
 * 
 * @author Yann
 *
 */
public class PuckInterpreter {

	/**
	 * Find all .wld rule files in the project and read them int the graph.
	 * @param project a Java project to scan
	 * @param graph a graph to add rules to
	 * @throws CoreException if some file/resource issue happens.
	 */
	public static void findAndParsePuckFiles(IJavaProject project, PuckGraph graph) throws CoreException {
		JavaParserHelper.findRuleFiles(project, member -> {
			PuckModel pm  = SerializationUtil.resourceToPuckModel(member);
			PuckInterpreter.parsePuckModel(pm, graph);
			return null;
		});
	}

	/**
	 * For a given PuckModel (a rule file), do the treatment.
	 * @param pm
	 * @param graph
	 */
	private static void parsePuckModel (PuckModel pm, PuckGraph graph) {
		// parse all named set declarations
		for (SetDeclaration set : pm.getNamedSets()){
			Set<Integer> nodes = parseSetDefinition(graph, set.getDef());
			graph.addSetDeclaration(set.getName(), nodes);
		}
		// deal with rules
		// System.out.println("Parsed " + sets);
		for (Rule rule : pm.getRules()) {
			Set<Integer> from = parseSetDefinition(graph, rule.getFrom());
			Set<Integer> hide = parseSetDefinition(graph, rule.getHide());
			// semantics is we don't hide from ourselves
			from.removeAll(hide);
			if (! from.isEmpty() && ! hide.isEmpty()) {
				graph.addRule(hide, from,SerializationUtil.toText(rule));
			}
		}
	}
	
	/**
	 * Treat a SetDefinition, i.e. compute resulting set from (nodes setminus except). 
	 * @param graph context we are working in
	 * @param sdef the set definition we want a resolved set for
	 * @return the set of indexes of the nodes in this SetDefinition
	 */
	private static Set<Integer> parseSetDefinition(PuckGraph graph, SetDefinition sdef) {
		Set<Integer> nodes = new HashSet<>();
		collectSet(graph, sdef.getNodes(), nodes);
		
		if (sdef.getExcept() != null) {
			Set<Integer> except =  new HashSet<>();
			collectSet(graph, sdef.getExcept(), except);
			nodes.removeAll(except);
		}
		return nodes;
	}

	/**
	 * Interpret a set of NodeReference to a set of integer index.
	 * @param graph the context
	 * @param set the set we are looking at
	 * @param nodes we should add all nodes found to this set.
	 */
	private static void collectSet(PuckGraph graph, NodeSet set, Set<Integer> nodes) {
		for (NodeReference elt : set.getNodes()) {
			if (elt instanceof TypeReference) {
				TypeReference tref = (TypeReference) elt;
				String key = tref.getType().getIdentifier();
				//IJvm
				int index = graph.findIndex(key);
				if (index != -1) {
					nodes.add(index);
				} else {
					System.out.println(" not found " + key);
				}
			} else if (elt instanceof PackageReference) {
				PackageReference pkg = (PackageReference) elt;
				String key = pkg.getPackage();
				//IJvm
				int index = graph.findIndex(key);
				if (index != -1) {
					nodes.add(index);
				} else {
					System.out.println(" not found " + key);
				}
				
			} else if (elt instanceof AllReference) {
				for (int index=0, ie=graph.getNodes().size() ; index < ie ; index++) {
					nodes.add(index);
				}
			} else if (elt instanceof SetReference) {
				SetReference sref = (SetReference) elt;
				nodes.addAll(graph.getSetDeclaration(sref.getRef().getName()));
			}
		}
		graph.getComposeGraph().collectSuffix(nodes);
	}


}
