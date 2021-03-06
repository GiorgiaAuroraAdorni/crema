package ch.idsia.crema.inference;

import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.model.GraphicalModel;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

/**
 * Common interface of inference algorithm. Any model preprocessing requirements of the final 
 * inference algorithm must be performed byt the doQuery method.
 * 
 * @author davidhuber
 *
 * @param <M> The model 
 * @param <F> The actual Factor type
 */
public interface Inference<M extends GraphicalModel<?>, F extends GenericFactor> {
	F query(int target, TIntIntMap evidence) throws InterruptedException;
	default F query(int target) throws InterruptedException {
		return query(target, new TIntIntHashMap());
	}

	M getInferenceModel(int target, TIntIntMap evidence);
}
