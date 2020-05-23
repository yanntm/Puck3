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
import fr.lip6.puck.jdt.JavaParserHelper;

public class PuckInterpreter {

	public static void findAndParsePuckFiles(IJavaProject project, PuckGraph graph) throws CoreException {
		JavaParserHelper.findRuleFiles(project, member -> {
			PuckModel pm  = SerializationUtil.resourceToPuckModel(member);
			PuckInterpreter.parsePuckModel(pm, graph);
			return null;
		});
	}

	
	private static void parsePuckModel (PuckModel pm, PuckGraph graph) {
		for (SetDeclaration set : pm.getNamedSets()){
			Set<Integer> nodes = parseSetDeclaration(graph, set.getDef());
			graph.addSetDeclaration(set.getName(), nodes);
		}
		// System.out.println("Parsed " + sets);
		for (Rule rule : pm.getRules()) {
			Set<Integer> from = parseSetDeclaration(graph, rule.getFrom());
			Set<Integer> hide = parseSetDeclaration(graph, rule.getHide());
			// semantics is we don't hide from ourselves
			from.removeAll(hide);
			if (! from.isEmpty() && ! hide.isEmpty()) {
				graph.addRule(hide, from,SerializationUtil.toText(rule));
			}
		}
	}
	
	private static Set<Integer> parseSetDeclaration(PuckGraph graph, SetDefinition sdef) {
		Set<Integer> nodes = new HashSet<>();
		collectSet(graph, sdef.getNodes(), nodes);
		
		if (sdef.getExcept() != null) {
			Set<Integer> except =  new HashSet<>();
			collectSet(graph, sdef.getExcept(), except);
			nodes.removeAll(except);
		}
		return nodes;
	}

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
