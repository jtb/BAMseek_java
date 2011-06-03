import java.util.*;
import java.io.*;

public class SAMParse extends AlignParse {
    
    private static final int BUFF_SIZE = 1024*128;
    private static final byte[] buffer = new byte[BUFF_SIZE];
    private int buffer_pos;
    private int buffer_size;

    public SAMParse(final String filename){
	super(filename);
	buffer_pos = 0;
	file_pos = 0;

	try {
	    filein = new RandomAccessFile(filename, "r");
	    filesize = filein.length();
	    parseHeader();
	    file_pos = filein.getFilePointer();
	}catch(IOException ie){}
		
    }

    public void seek(final long offset) throws IOException{
	filein.seek(offset);
    }
    
    public long getNextRecordIndex() { 
	long offset = file_pos + buffer_pos;
	
	try {
	    while(true){
		if(buffer_size == 0 || buffer_pos >= buffer_size){
		    file_pos = filein.getFilePointer();
		    buffer_size = filein.read(buffer);
		    buffer_pos = 0;
		}
		if(buffer_size < 1) return -1;
		if(buffer[buffer_pos] == '\n'){
		    buffer_pos++;
		    return offset;
		}
		buffer_pos++;
	    }
	}catch(IOException ie){
	    return -1;
	}
    }
    
    public String[] getNextRecord() {
	try {
	    String s = filein.readLine();
	    if(s == null) return null;
	    return s.split("\\t");
	}catch(IOException e){
	    return null;
	}
    }

    public double getProgress() { 
	try {
	    return 1.0*filein.getFilePointer()/(1.0*filein.length()); 
	} catch (IOException e){
	    return 1.0;
	}
    }

    private void parseHeader(){
	String line = null;
	header = "";
	long pos = -1;
	
	try {
	    pos = filein.getFilePointer();
	    while((line = filein.readLine()) != null){
		String tag = line.substring(0, 3);
		if(tag.equals("@HD") || tag.equals("@SQ") || tag.equals("@RG") || tag.equals("@PG") || tag.equals("@CO")){
		    header += line;
		    header += "\n";
		    pos = filein.getFilePointer();
		}else{
		    break;
		}
	    }
	    
	    if(pos >=0){
		filein.seek(pos);
	    }
	    
	}catch(IOException ie){}
	
	
    }

    private long filesize;
    private long file_pos;

    private RandomAccessFile filein;

    
}
