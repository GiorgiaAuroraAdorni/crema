package ch.idsia.crema.solver.commons;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;

import org.apache.commons.math3.optim.linear.LinearConstraint;
import org.apache.commons.math3.optim.linear.LinearConstraintSet;
import org.apache.commons.math3.optim.linear.Relationship;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.junit.Test;

import ch.idsia.crema.factor.GenericFactor;
import ch.idsia.crema.factor.convert.SeparateLinearToExtensiveHalfspaceFactor;
import ch.idsia.crema.factor.credal.linear.IntervalFactor;
import ch.idsia.crema.model.graphical.SparseModel;

public class FractionalSolverTest {

	@Test
	public void testSolve() {
		SparseModel<GenericFactor> model = new SparseModel<>();

		int n0 = model.addVariable(3);
		int n1 = model.addVariable(2);
		model.addVariable(2);

		// root
		IntervalFactor f0 = new IntervalFactor(model.getDomain(n0), model.getDomain(n1));
		f0.set(new double[] { 0.1, 0.3, 0.5 }, new double[] { 0.3, 0.8, 0.6 }, 0);
		f0.set(new double[] { 0.1, 0.5, 0.1 }, new double[] { 0.3, 0.8, 0.7 }, 1);

		double[] num = { 0.052, 0.12, 0.0, 0.0, 0.0, 0.0 };
		double[] denom = { 0.052, 0.12, 0.126, 0.28, 0.024, 0.0545};

		FractionalSolver solver = new FractionalSolver();
		solver.loadProblem(new SeparateLinearToExtensiveHalfspaceFactor().apply(f0), GoalType.MINIMIZE);
		solver.solve(num, 0, denom, 0);

		assertEquals(0.184175234689316, solver.getValue(), 0.000000001);

		double[] expected = { 0.1, 0.3, 0.6, 0.3, 0.5, 0.2 };// 0.7, 0.3, 0.1,
																// 0.9, 0.1, 0.9
																// };
		assertArrayEquals(expected, solver.getVertex(), 0.000000001);
		num = new double[] { 0.052, 0.12, 0.0, 0.0, 0.0, 0.0 };
		denom = new double[] { 0.052, 0.12, 0.126, 0.28, 0.024, 0.0545 };

		solver.solve(num, 0, denom, 0);

		// nothing changed just called solve with other signature
		assertEquals(0.184175234689316, solver.getValue(), 0.000000001);
		assertArrayEquals(expected, solver.getVertex(), 0.000000001);

	}

	@Test
	public void testExample2() {
		FractionalSolver solver = new FractionalSolver();
		ArrayList<LinearConstraint> constraints = new ArrayList<>();

		// -x0 + x1 <= 4
		LinearConstraint constraint = new LinearConstraint(new double[] { -1, 1 }, Relationship.LEQ, 4);
		constraints.add(constraint);

		// x1 <= 6
		constraint = new LinearConstraint(new double[] { 0, 1 }, Relationship.LEQ, 6);
		constraints.add(constraint);

		// x1, x0 >= 0 implicit in this fractional solver

		constraint = new LinearConstraint(new double[] { 2, 1 }, Relationship.LEQ, 14);
		constraints.add(constraint);

		double[] numerator = new double[] { -2, 1 };
		double[] denominator = new double[] { 1, 3 };

		solver.loadProblem(new LinearConstraintSet(constraints), GoalType.MINIMIZE);
		solver.solve(numerator, 2, denominator, 4);

		assertArrayEquals(new double[] { 7, 0 }, solver.getVertex(), 0.0000001);
		assertEquals(-1.090909090909, solver.getValue(), 0.0000001);

	}

}
