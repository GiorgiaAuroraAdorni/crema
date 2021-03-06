package ch.idsia.crema.model.io.uai;

import ch.idsia.crema.IO;
import gnu.trove.map.TIntIntMap;
import gnu.trove.map.hash.TIntIntHashMap;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Parser for evidence files in UAI format
 *  @author Rafael Cabañas
 */


public class EvidUAIParser extends UAIParser<TIntIntMap[]> {

    private int[][][] evid;

    public EvidUAIParser(String file) throws FileNotFoundException {
        this.bufferedReader = initReader(file);
    }
    public EvidUAIParser(BufferedReader reader) {
        this.bufferedReader = reader;
    }

    @Override
    protected void processFile() {
        int numEvidences = popInteger();
        evid = new int[numEvidences][][];
        for(int i=0; i<numEvidences;i++){
            int numObserved = popInteger();
            evid[i] = new int[numObserved][2];
            for(int j=0; j<numObserved;j++){
                evid[i][j][0] = popInteger();
                evid[i][j][1] = popInteger();
            }
        }

    }

    @Override
    protected TIntIntMap[] build() {
        TIntIntMap[] parsedObject = new TIntIntMap[evid.length];
        for(int i=0; i<evid.length; i++){
            parsedObject[i] = new TIntIntHashMap();
            for(int j=0; j<evid[i].length; j++){
                parsedObject[i].put(evid[i][j][0], evid[i][j][1]);
            }
        }

        return parsedObject;
    }

    public static void main(String[] args) throws IOException {
        // .uai.do and .uai.evid are parsed so far in the same way
        TIntIntMap[] evidences = (TIntIntMap[]) IO.read("./models/simple.uai.do");

        for(TIntIntMap ev : evidences)
            System.out.println(ev);

    }

}
