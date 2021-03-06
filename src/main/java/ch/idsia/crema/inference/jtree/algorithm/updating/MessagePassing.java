package ch.idsia.crema.inference.jtree.algorithm.updating;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.jtree.algorithm.cliques.Clique;
import ch.idsia.crema.utility.VertexPair;
import gnu.trove.map.TIntIntMap;
import gnu.trove.set.TIntSet;
import gnu.trove.set.hash.TIntHashSet;
import org.jgrapht.Graph;
import org.jgrapht.graph.DefaultWeightedEdge;

import java.util.HashMap;
import java.util.Map;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: CreMA
 * Date:    14.02.2018 10:03
 */
public class MessagePassing {

	private Graph<Clique, DefaultWeightedEdge> model;

	private Map<Integer, BayesianFactor> factors = new HashMap<>();

	private TIntIntMap evidence;

	private Map<VertexPair<Clique>, Separator> separators = new HashMap<>();

	public void setModel(Graph<Clique, DefaultWeightedEdge> model) {
		this.model = model;
	}

	public void addFactor(Integer i, BayesianFactor factor) {
		this.factors.put(i, factor);
	}

	public void addFactor(Map<Integer, BayesianFactor> factors) {
		this.factors.putAll(factors);
	}

	public void setEvidence(TIntIntMap evidence) {
		this.evidence = evidence;
	}

	public void exec() {
		if (model == null) throw new IllegalArgumentException("No model available");
		if (factors == null) throw new IllegalArgumentException("No factors available");

		// build junction tree
		separators = new HashMap<>();
		for (DefaultWeightedEdge edge : model.edgeSet()) {
			Clique source = model.getEdgeSource(edge);
			Clique target = model.getEdgeTarget(edge);
			Separator S = new Separator(source, target);
			separators.put(new VertexPair<>(source, target), S);
			separators.put(new VertexPair<>(target, source), S);
		}

		// collect messages towards a root
		Clique j = model.vertexSet().iterator().next();

		// collect messages from incoming messages
		for (DefaultWeightedEdge edge : model.edgesOf(j)) {
			Clique source = model.getEdgeSource(edge);
			Clique target = model.getEdgeTarget(edge);

			Clique i = j == source ? target : source;

			// combine message with phi
			collect(/* other */ i, /* this */j);
		}

		// distribute message from root to nodes
		VertexPair<Clique> key;
		for (DefaultWeightedEdge outEdge : model.edgesOf(j)) {
			Clique source = model.getEdgeSource(outEdge);
			Clique target = model.getEdgeTarget(outEdge);

			Clique i = j == source ? target : source;

			BayesianFactor phi = phi(j);

			for (DefaultWeightedEdge inEdge : model.edgesOf(j)) {
				Clique inSource = model.getEdgeSource(inEdge);
				Clique inTarget = model.getEdgeTarget(inEdge);

				Clique k = j == inSource ? inTarget : inSource;

				// we are computing the message Mij from Mkj where k =/= i
				if (k.equals(i)) continue;

				key = new VertexPair<>(k, j);
				BayesianFactor Mkj = separators.get(key).getMessage(k);
				phi.combine(Mkj);
			}

			key = new VertexPair<>(i, j);
			Separator S = separators.get(key);
			BayesianFactor Mij = project(phi, S);

			// distribute the message M(root,j) to node j
			distribute(j, i, Mij);
		}
	}

	/**
	 * @param j from this node
	 * @param k to this node
	 * @return the message Mjk
	 */
	private BayesianFactor collect(Clique j, Clique k) {
		// collect phi(j)
		BayesianFactor phi = phi(j);

		for (DefaultWeightedEdge edge : model.edgesOf(j)) {
			Clique source = model.getEdgeSource(edge);
			Clique target = model.getEdgeTarget(edge);

			// if we have inbound edges for this node j, we compute the message
			Clique i = j == source ? target : source;

			// ignore messages inbound from k
			if (i.equals(k)) continue;

			BayesianFactor Mij = collect(i, j);

			phi.combine(Mij);
		}

		// compute the message by projecting phi over the separator(i, j)
		Separator S = separators.get(new VertexPair<>(j, k));
		BayesianFactor Mjk = project(phi, S);
		S.setMessage(j, Mjk);

		return Mjk;
	}

	/**
	 * @param k   from this node
	 * @param i   to this node
	 * @param Mki the message Mki
	 */
	private void distribute(Clique k, Clique i, BayesianFactor Mki) {
		VertexPair<Clique> key = new VertexPair<>(k, i);
		separators.get(key).setMessage(k, Mki);

		BayesianFactor phi = phi(i).combine(Mki);

		// if we have nodes that need the message from this node i
		for (DefaultWeightedEdge edge : model.edgesOf(i)) {
			Clique source = model.getEdgeSource(edge);
			Clique target = model.getEdgeTarget(edge);

			Clique j = i == source ? target : source;

			// ignore message outgoing to k
			if (j.equals(k)) continue;

			key = new VertexPair<>(i, j);
			BayesianFactor Mij = project(phi, separators.get(key));

			distribute(i, j, Mij);
		}
	}

	private BayesianFactor phi(Clique clique) {
		int[] variables = clique.getVariables();

		BayesianFactor factor = factors.get(variables[0]);
		for (int i = 1; i < variables.length; i++) {
			int v = variables[i];

			factor = factor.combine(factors.get(v));
		}

		return factor;
	}

	private BayesianFactor project(BayesianFactor phi, Separator S) {

		TIntSet variables = new TIntHashSet(phi.getDomain().getVariables());
		for (int s : S.getVariables()) {
			variables.remove(s);
		}

		for (int v : variables.toArray())
			phi = phi.marginalize(v);

		return phi;
	}
}
