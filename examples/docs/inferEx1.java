package docs;

import ch.idsia.crema.factor.credal.vertex.VertexFactor;
import ch.idsia.crema.inference.Inference;
import ch.idsia.crema.inference.ve.CredalVariableElimination;
import ch.idsia.crema.model.ObservationBuilder;
import ch.idsia.crema.model.Strides;
import ch.idsia.crema.model.graphical.SparseModel;


public class inferEx1 {
public static void main(String[] args) throws InterruptedException {

// define the structure
SparseModel cnet = new SparseModel();
int X0 = cnet.addVariable(2);
int X1 = cnet.addVariable(3);
cnet.addParent(X0,X1);
// add credal set K(B)
VertexFactor fb = new VertexFactor(cnet.getDomain(X1), Strides.empty());
fb.addVertex(new double[]{0.2, 0.5, 0.3});
fb.addVertex(new double[]{0.3, 0.4, 0.3});
fb.addVertex(new double[]{0.3, 0.2, 0.5});
cnet.setFactor(X1,fb);
// add credal set K(A|B)
VertexFactor fa = new VertexFactor(cnet.getDomain(X0), cnet.getDomain(X1));
fa.addVertex(new double[]{0.5, 0.5}, 0);
fa.addVertex(new double[]{0.6, 0.4}, 0);
fa.addVertex(new double[]{0.3, 0.7}, 1);
fa.addVertex(new double[]{0.4, 0.4}, 1);
fa.addVertex(new double[]{0.2, 0.8}, 2);
fa.addVertex(new double[]{0.1, 0.9}, 2);
cnet.setFactor(X0,fa);
// set up the inference and run the queries
Inference inf = new CredalVariableElimination(cnet);
VertexFactor res1 = (VertexFactor) inf.query(X1, ObservationBuilder.observe(X0, 0));
VertexFactor res2 = (VertexFactor) inf.query(X0);

double[][][] vertices = res1.getData();


}
}
