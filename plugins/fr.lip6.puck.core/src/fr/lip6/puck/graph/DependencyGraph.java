package fr.lip6.puck.graph;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.ASTNode;

import android.util.SparseIntArray;
import fr.lip6.move.gal.util.GraphUtils;
import fr.lip6.move.gal.util.MatrixCol;

public class DependencyGraph {
	private MatrixCol graph;
	private Map<Integer,Map<Integer,List<ASTNode>>> reasons = new HashMap<>();
	
	public DependencyGraph(int nbnodes) {
		graph = new MatrixCol(nbnodes,nbnodes);
	}
	
	public void addEdge(int indexDst, int indexSrc, ASTNode reason) {
		graph.set(indexDst, indexSrc, 1);
		reasons.computeIfAbsent(indexDst, k -> new HashMap<>()).computeIfAbsent(indexSrc, k -> new ArrayList<>()).add(reason);
	}
	public void collectSuffix (Set<Integer> nodes) {
		GraphUtils.collectSuffix(nodes, graph);
	}
	
	public void dotExport(PrintWriter out, String style) {
		for (int coli=0,colie=graph.getColumnCount(); coli<colie;coli++ ) {
			SparseIntArray col = graph.getColumn(coli);
			for (int i=0,ie=col.size();i<ie;i++) {
				int colj = col.keyAt(i);
				out.println("  n"+coli+ " -> n" + colj + " " + style + " ;");
			}
		}
	}

	public boolean hasEdge(Integer dest, Integer source) {
		return graph.get(dest, source) != 0;
	}

	public List<ASTNode> getReasons(Integer dest, Integer source) {
		return reasons.getOrDefault(dest, Collections.emptyMap()).getOrDefault(source, Collections.emptyList());
	}
}
