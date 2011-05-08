import java.util.*;
import java.io.*;

public abstract class BaseParse {
    
    String filename;
    String header="";

    public BaseParse(final String filename){
	this.filename = filename;
    }
    
    public abstract void seek(final long offset) throws IOException;
    public abstract long getNextRecordIndex();
    public abstract String[] getNextRecord();

    public abstract double getProgress();

    public String getHeader() { return header; }
    public String getFilename() {return filename; }

}