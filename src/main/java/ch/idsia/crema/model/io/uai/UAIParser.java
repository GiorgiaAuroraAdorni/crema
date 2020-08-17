package ch.idsia.crema.model.io.uai;

import ch.idsia.crema.model.io.TypesIO;
import org.springframework.util.Assert;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

/**
 * Generic parser class for UAI format
 *  @author Rafael Cabañas
 */

public abstract class UAIParser<T extends  Object> {

    protected String[] elements = null;
    private int offset = 0;
    protected BufferedReader bufferedReader;
    private String parsedType="";

    protected TypesIO TYPE;

    public static Object read(String fileName) throws IOException {
        BufferedReader buff = null;
        TypesIO type = null;

        if(fileName.endsWith(".uai")) {
            // Extract the type to know the required parser
            type = UAITypes.valueOfLabel(getIOTypeStr(fileName));
            // rest the buffer
            buff = initReader(fileName);
        } else if(fileName.endsWith(".uai.evid") || fileName.endsWith(".uai.do")){
            type = UAITypes.EVID;
        }else{
            throw new IllegalArgumentException("Unknown file extension");
        }
        Object parsedObject = null;

            try{
                // Parse the file
                if (type == UAITypes.HCREDAL) {
                    parsedObject = new HCredalUAIParser(buff).parse();
                } else if (type == UAITypes.VCREDAL) {
                    parsedObject = new VCredalUAIParser(buff).parse();
                } else if (type == UAITypes.BAYES) {
                    parsedObject = new BayesUAIParser(buff).parse();
                } else if (type == UAITypes.EVID){
                    parsedObject = new EvidUAIParser(buff).parse();
                }else {
                    throw new IllegalArgumentException("Unknown type to be parsed");
                }
            }catch (Exception e){
                buff.close();
                throw e;
            }

        return parsedObject;

    }

    public void closeReader() throws IOException {
        if(this.bufferedReader != null)
            this.bufferedReader.close();
    }


    public static String getIOTypeStr(String fileName) throws IOException {

        BufferedReader buff = initReader(fileName);
        String type = null;

        try {
            // Extract the type to know the required parser
            String str = buff.readLine().replaceAll("[ \\t\\n]+", "");
            int i = str.indexOf(" ");
            if (i > 0) type = str.substring(0, i);
            else type = str;
        }catch (Exception e) {
            buff.close();
            throw e;
        }
        return type;

    }


    public static  BufferedReader initReader(String fileName) throws FileNotFoundException {
        // Opening the .cn file
        FileReader fileReader = new FileReader(fileName);
        return new BufferedReader(fileReader);

    }

    protected void readFile() throws IOException {

        StringBuilder stringBuilder = new StringBuilder();
        String line = null;
        while ((line = bufferedReader.readLine()) != null) {
            stringBuilder.append(line + " ");
        }
        bufferedReader.close();
        // Splitting file elements (spaces, tabs and newlines have the same effect)
        elements = stringBuilder.toString().split("[ \\t\\n]+");

    }

    protected abstract void processFile();

    protected abstract T build();

    public T parse() throws IOException {
        T parsedObj = null;
        readFile();
        processFile();
        sanityChecks();
        parsedObj = build();
        return parsedObj;
    }

    protected void sanityChecks(){
        Assert.isTrue(TYPE.getLabel().equals(parsedType), "Wrong type "+parsedType+" instead of "+TYPE);
    }


    protected void incrementOffset(int n){
        this.offset += n;
    }

    protected int getOffset() {
        return offset;
    }

    protected void setOffset(int offset) {
        this.offset = offset;
    }

    protected String popElement(){
        incrementOffset(1);
        return elements[getOffset()-1];
    }

    protected void parseType(){
        parsedType = popElement();
    }

    protected int popInteger(){
        return Integer.parseInt(popElement());
    }
    protected double popDouble(){
        return Double.parseDouble(popElement());
    }

}
