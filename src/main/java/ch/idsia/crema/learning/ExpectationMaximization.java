package ch.idsia.crema.learning;

import ch.idsia.crema.factor.bayesian.BayesianFactor;
import ch.idsia.crema.inference.JoinInference;
import ch.idsia.crema.inference.ve.FactorVariableElimination;
import ch.idsia.crema.inference.ve.order.MinFillOrdering;
import ch.idsia.crema.model.GraphicalModel;
import ch.idsia.crema.model.graphical.specialized.BayesianNetwork;
import ch.idsia.crema.preprocess.CutObserved;
import ch.idsia.crema.preprocess.RemoveBarren;
import ch.idsia.crema.utility.ArraysUtil;
import ch.idsia.crema.utility.IndexIterator;
import com.google.common.primitives.Ints;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.TIntObjectMap;
import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.util.stream.IntStream;

/**
 * Author:  Claudio "Dna" Bonesana
 * Project: CreMA
 * Date:    22.03.2017 14:24
 */
public class ExpectationMaximization {

    private final JoinInference<BayesianFactor, BayesianFactor> inferenceEngine;
    private GraphicalModel<BayesianFactor> model;

    private double regularization = 0.00001;

    public ExpectationMaximization(GraphicalModel<BayesianFactor> model,
                                   JoinInference<BayesianFactor, BayesianFactor> inferenceEngine, boolean inline) {

        this.inferenceEngine = inferenceEngine;

        if(!inline)
            this.model = model.copy();
        else
            this.model = model;


    }

    public ExpectationMaximization(GraphicalModel<BayesianFactor> model,
                                   JoinInference<BayesianFactor, BayesianFactor> inferenceEngine) {
        this(model,inferenceEngine,false);

    }


    public ExpectationMaximization(GraphicalModel<BayesianFactor> model, int[] elimSeq){
        this(model,(m, query, obs) -> {
            CutObserved co = new CutObserved();
            GraphicalModel coModel = co.execute(m, obs);

            RemoveBarren rb = new RemoveBarren();
            GraphicalModel infModel = rb.execute(coModel, query, obs);
            rb.filter(elimSeq);

            FactorVariableElimination  fve = new FactorVariableElimination(elimSeq);
            fve.setNormalize(true);

            return (BayesianFactor) fve.apply(infModel, query, obs);

        });

    }

    public ExpectationMaximization(GraphicalModel<BayesianFactor> model) {
        this(model, (new MinFillOrdering()).apply(model));
    }


    private TIntObjectMap<BayesianFactor> expectation(TIntIntMap[] observations) throws InterruptedException {

        TIntObjectMap<BayesianFactor> counts = new TIntObjectHashMap<>();
        for (int variable : model.getVariables()) {
            counts.put(variable, new BayesianFactor(model.getFactor(variable).getDomain(), false));
        }

        for (TIntIntMap observation : observations) {

            for (int var : model.getVariables()) {

                int[] relevantVars = ArraysUtil.addToSortedArray(model.getParents(var), var);
                int[] hidden =  IntStream.of(relevantVars).filter(x -> !observation.containsKey(x)).toArray();
                int[] obsVars = IntStream.of(relevantVars).filter(x -> observation.containsKey(x)).toArray();


                if(hidden.length>0){
                    // Case with missing data
                    BayesianFactor phidden_obs = inferenceEngine.apply(model, hidden, observation);
                    if(obsVars.length>0)
                        phidden_obs = phidden_obs.combine(
                                BayesianFactor.getJoinDeterministic(model.getDomain(obsVars), observation));

                    counts.put(var, counts.get(var).addition(phidden_obs));
                }else{
                    //fully-observable case
                    for(int index : counts.get(var).getDomain().getCompatibleIndexes(observation)){
                        double x = counts.get(var).getValueAt(index) + 1;
                        counts.get(var).setValueAt(x, index);
                    }
                }
            }
        }

        return counts;
    }

    private void maximization(TIntObjectMap<BayesianFactor> counts){
        for (int var : model.getVariables()) {
            BayesianFactor countVar = counts.get(var);

            if(regularization>0.0)
                for(int k=0; k<countVar.getData().length;k++)
                    counts.get(var).setValueAt(counts.get(var).getValue(k)+regularization, k);

            BayesianFactor f = countVar.divide(counts.get(var).marginalize(var));
            model.setFactor(var, f);
        }
    }

    public void run(TIntIntMap[] observations, int iterations) throws InterruptedException {
        for(int i=0; i<iterations; i++) {
            // E-stage
            TIntObjectMap<BayesianFactor> counts = expectation(observations);
            // M-stage
            maximization(counts);
        }

    }


    public GraphicalModel<BayesianFactor> getPosterior() {
        return model;
    }

    public void setRegularization(double regularization) {
        this.regularization = regularization;
    }

    public double getRegularization() {
        return regularization;
    }


    public static void main(String[] args) throws InterruptedException {


        // https://www.cse.ust.hk/bnbook/pdf/l07.h.pdf
        BayesianNetwork model = new BayesianNetwork();


        for(int i=0;i<3;i++)
            model.addVariable(2);

        int[] X = model.getVariables();


        model.addParent(X[1],X[0]);
        model.addParent(X[2],X[1]);


        model.setFactor(X[0], new BayesianFactor(model.getDomain(X[0]), new double[]{0.5,0.5}));
        model.setFactor(X[1], new BayesianFactor(model.getDomain(X[1],X[0]), new double[]{2./3, 1./3, 1./3, 2./3}));
        model.setFactor(X[2], new BayesianFactor(model.getDomain(X[2],X[1]), new double[]{2./3, 1./3, 1./3, 2./3}));



        int[][] dataX = {
                {0, 0, 0},
                {1, 1, 1},
                {0, -1, 0},
                {1, -1, 1}
        };

        TIntIntMap[] observations = new TIntIntMap[dataX.length];
        for(int i=0; i<observations.length; i++) {
            observations[i] = new TIntIntHashMap();
            for(int j=0; j<dataX[i].length; j++) {
                if(dataX[i][j]>=0)
                    observations[i].put(X[j], dataX[i][j]);
            }
            System.out.println(observations[i]);
        }


        ExpectationMaximization inf = new ExpectationMaximization(model);
        inf.setRegularization(0.0);

        inf.run(observations,1);


        System.out.println("Posterior:");

        System.out.println(inf.getPosterior().getFactor(X[0])); //[0.5, 0.5]
        System.out.println("--");
        System.out.println(inf.getPosterior().getFactor(X[1]).filter(X[0],0)); //[0.9, 0.1]
        System.out.println(inf.getPosterior().getFactor(X[1]).filter(X[0],1)); // [0.1, 0.9]
        System.out.println("--");
        System.out.println(inf.getPosterior().getFactor(X[2]).filter(X[1],0)); // [0.9, 0.1]
        System.out.println(inf.getPosterior().getFactor(X[2]).filter(X[1],1)); // [0.1, 0.9]

    }

}
