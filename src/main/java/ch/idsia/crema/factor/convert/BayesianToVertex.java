package ch.idsia.crema.factor.convert;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.factor.credal.vertex.ExtensiveVertexFactor;
import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.model.Converter;
import ch.idsia.crema.model.Strides;
import com.google.common.primitives.Doubles;

import java.util.List;

public class BayesianToVertex implements Converter<BayesianFactor, VertexFactor> {
	public static final BayesianToVertex INSTANCE = new BayesianToVertex();
	
	@Override
	public VertexFactor apply(BayesianFactor cpt, Integer var) {

		Strides left = Strides.as(var, cpt.getDomain().getCardinality(var));
		Strides right = cpt.getDomain().remove(var);
		VertexFactor factor = new VertexFactor(left, right);
		int left_var_size = cpt.getDomain().getCardinality(var);
		List cpt_data = Doubles.asList(cpt.getData());

		for(int i=0; i<right.getCombinations(); i++){
			double[] v = Doubles.toArray(cpt_data.subList(i*left_var_size, (i+1)*left_var_size));
			factor.addVertex(v, i);
		}

		return factor;
	}

	@Override
	public Class<VertexFactor> getTargetClass() {
		return VertexFactor.class;
	}

	@Override
	public Class<BayesianFactor> getSourceClass() {
		return BayesianFactor.class;
	}
}
