package ch.idsia.crema.factor.credal.vertex.algebra;

import java.util.Arrays;

import ch.idsia.crema.factor.credal.vertex.ExtensiveVertexFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.math.Operation;
import ch.idsia.crema.model.vertex.Collector;
import ch.idsia.crema.model.vertex.Filter;
import ch.idsia.crema.model.vertex.LogMarginal;
import ch.idsia.crema.model.vertex.LogVertexOperation;
import ch.idsia.crema.model.vertex.Marginal;
import ch.idsia.crema.model.vertex.SimpleVertexOperation;
import ch.idsia.crema.model.vertex.VertexOperation;


public abstract class OnlineConvexHullAlgebra implements Operation<ExtensiveVertexFactor> {
	
	
	protected abstract boolean canAddVector(ExtensiveVertexFactor factor, double[] vector);
	
	
	@Override
	public ExtensiveVertexFactor combine(ExtensiveVertexFactor one, ExtensiveVertexFactor two) {

		final Strides target = one.getDomain().union(two.getDomain());
		final int length = target.getSize();

		final int[] limits = new int[length];

		final long[] stride = new long[length];
		final long[] reset = new long[length];

		for (int vindex = 0; vindex < one.getDomain().getSize(); ++vindex) {
			int offset = Arrays.binarySearch(target.getVariables(), one.getDomain().getVariables()[vindex]);
			if (offset >= 0) {
				stride[offset] = one.getDomain().getStrides()[vindex];
			}
		}

		for (int vindex = 0; vindex < two.getDomain().getSize(); ++vindex) {
			int offset = Arrays.binarySearch(target.getVariables(), two.getDomain().getVariables()[vindex]);
			if (offset >= 0) {
				stride[offset] += ((long) two.getDomain().getStrides()[vindex] << 32l);
			}
		}

		for (int i = 0; i < length; ++i) {
			limits[i] = target.getSizes()[i] - 1;
			reset[i] = limits[i] * stride[i];
		}

		final int our_tables = one.getInternalVertices().size();
		final int his_tables = two.getInternalVertices().size();

		ExtensiveVertexFactor target_factor = new ExtensiveVertexFactor(target, one.isLog(), our_tables * his_tables);
		final int table_size = target.getCombinations();

		VertexOperation ops = target_factor.isLog() ? new LogVertexOperation() : new SimpleVertexOperation();
		
		for (int our_table = 0; our_table < our_tables; ++our_table) {
			for (int his_table = 0; his_table < his_tables; ++his_table) {
				final double[] our_data = one.getInternalVertices().get(our_table);
				final double[] his_data = two.getInternalVertices().get(his_table);

				final double[] result = ops.combine(our_data, his_data, table_size, stride, reset, limits);
				
				if (canAddVector(target_factor, result)) {
					target_factor.addInternalVertex(result);
				}
			}
		}

		return target_factor;
	}
	

	@Override
	public ExtensiveVertexFactor filter(final ExtensiveVertexFactor factor, int variable, int state) {
		int offset = factor.getDomain().indexOf(variable);
		return collect(factor, offset, new Filter(factor.getDomain().getStrideAt(offset), state));
	}
	
	@Override
	public ExtensiveVertexFactor marginalize(final ExtensiveVertexFactor factor, int variable) {
		final Strides domain = factor.getDomain();
		final int offset = domain.indexOf(variable);
		
		if (factor.isLog())
			return collect(factor, offset, new LogMarginal(domain.getSizeAt(offset), domain.getStrideAt(offset)));
		else
			return collect(factor, offset, new Marginal(domain.getSizeAt(offset), domain.getStrideAt(offset)));
	}

	
	private ExtensiveVertexFactor collect(final ExtensiveVertexFactor factor, final int offset, final Collector collector) {
		final Strides domain = factor.getDomain();
		final int stride = domain.getStrideAt(offset);
		final int size = domain.getSizeAt(offset);
		final int jump = stride * (size - 1);
		final int reset = size * stride;

		final Strides target_domain = domain.removeAt(offset);
		final ExtensiveVertexFactor result = new ExtensiveVertexFactor(target_domain, factor.isLog());
		
		// marginalize all the vertices of the source factor
		for (double[] vertex : factor.getInternalVertices()) {
			int source = 0;
			int next = stride;

			final double[] new_data = new double[target_domain.getCombinations()];

			for (int target = 0; target < target_domain.getCombinations(); ++target, ++source) {
				if (source == next) {
					source += jump;
					next += reset;
				}

				new_data[target] = collector.collect(vertex, source);
			}

			// ask if the vertex should be added
			if (canAddVector(result, new_data)) {
				result.addInternalVertex(new_data);
			}
		}

		return result;
	}
	
	@Override
	public ExtensiveVertexFactor divide(ExtensiveVertexFactor one, ExtensiveVertexFactor other) {
		throw new UnsupportedOperationException();
	}
}
