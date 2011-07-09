import java.util.*;
import java.io.*;


public class SFFParse extends BaseParse {

    public SFFParse(final String filename){
	super(filename);
	
	try {
            filein = new RandomAccessFile(filename, "r");
            parseHeader();
	}catch(IOException ie){}
    }

    public void seek(final long offset) throws IOException{
	filein.seek(offset);
    }
    
    public long getNextRecordIndex(){
	return 0;
    }
    
    public String[] getNextRecord(){
	return null;
	
    }

    public double getProgress(){
	try {
            return 1.0*filein.getFilePointer()/(1.0*filein.length());
        } catch (IOException e){
            return 1.0;
        }
    }

    private void parseHeader(){

    }
    

    private RandomAccessFile filein;

}