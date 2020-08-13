package ch.idsia.crema.model.io.uai;

import ch.idsia.crema.IO;
import ch.idsia.crema.factor.credal.linear.SeparateHalfspaceFactor;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.SparseModel;
import ch.idsia.crema.utility.ConstraintsUtil;
import ch.idsia.crema.utility.IndexIterator;
import org.apache.commons.math3.optim.linear.LinearConstraint;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;

public class HCredalUAIWriter extends NetUAIWriter<SparseModel>{

    public HCredalUAIWriter(SparseModel target, String file) throws IOException {
        this.target = target;
        TYPE = UAITypes.HCREDAL;
        this.writer = initWriter(file);

    }
    public HCredalUAIWriter(SparseModel target, BufferedWriter writer){
        this.target = target;
        TYPE = UAITypes.HCREDAL;
        this.writer = writer;
    }


    @Override
    protected void sanityChecks() {
        super.sanityChecks();
        if(!isCompatible(target))
            throw new IllegalArgumentException("Target is not compatible with writer");
    }

    @Override
    protected void writeFactors() throws IOException {
        for(int v : target.getVariables()) {

            SeparateHalfspaceFactor f = (SeparateHalfspaceFactor) target.getFactor(v);

            tofileln("");
            // get a reordered iterator as UAI stores data with inverted variables compared to Crema
            Strides paDomain = target.getDomain(target.getParents(v)).reverseDomain();
            IndexIterator iter = paDomain.getReorderedIterator(target.getParents(v));

            Collection<LinearConstraint> K = new ArrayList<>();

            int paComb = paDomain.getCombinations();
            int vSize = target.getSize(v);
            int offset = 0;

            // Transform constraints
            int j = 0;
            while(iter.hasNext()) {
                j = iter.next();
                Collection<LinearConstraint> Kj = HCredalUAIWriter.processConstraints(f.getLinearProblemAt(j).getConstraints());
                Kj = ConstraintsUtil.expandCoeff(Kj, paComb*vSize, offset);
                K.addAll(Kj);
                offset += vSize;
            }

            // Write coefficients
            tofileln(paComb*vSize*K.size());
            for(LinearConstraint c : K)
                tofileln(c.getCoefficients().toArray());
            //Write values
            tofileln(K.size());
            for(Object c : K)
                tofile(((LinearConstraint)c).getValue());
            tofileln("");

        }

    }

    @Override
    protected void writeTarget() throws IOException {
        writeType();
        writeVariablesInfo();
        writeDomains();
        writeFactors();
    }


    public static Collection<LinearConstraint> processConstraints(Collection<LinearConstraint> set){
        return ConstraintsUtil.changeGEQtoLEQ(
                    ConstraintsUtil.changeEQtoLEQ(
                            ConstraintsUtil.removeNormalization(
                                    ConstraintsUtil.removeNonNegative(set))));
    }


    protected static boolean isCompatible(Object object){

        if(!(object instanceof SparseModel))
            return false;

        for(int v : ((SparseModel) object).getVariables())
            if(!(((SparseModel) object).getFactor(v) instanceof  SeparateHalfspaceFactor))
                return false;
        return true;
    }


    public static void main(String[] args) throws IOException {
        String fileName = "./models/simple-hcredal";

        SparseModel model;

        model = (SparseModel) UAIParser.read(fileName+".uai");
        UAIWriter.write(model,fileName+"2.uai");

        model = (SparseModel) UAIParser.read(fileName+"2.uai");
        IO.write(model,fileName+"3.uai");

    }



}
