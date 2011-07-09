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
	
	long offset = 0;
	try{
	    offset = filein.getFilePointer();
	    if(offset == index_offset){
		filein.seek(offset + index_length);
	    }

	    byte buffer[] = new byte[2];
	    filein.read(buffer);
	    int read_header_length = BytesHelper.shortToInt(buffer);
	    offset = filein.getFilePointer() + read_header_length - 2;
	    buffer = new byte[4];
	    filein.read(buffer);
	    long number_of_bases = BytesHelper.toUint(buffer);
	    
	    filein.seek(offset);
	    long read_data_size = FLOWGRAM_BYTES_PER_FLOW*number_of_flows_per_read + 3*number_of_bases;
	    if((read_data_size & 8) > 0){
		read_data_size = (((read_data_size >> 3)+1) <<3);
	    }
	    
	    filein.seek(offset + read_data_size);
	    return (offset + read_data_size);
	}catch(Exception e){
	    return -1;
	}
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
	header = "";
	try{
	    byte bytes[] = new byte[4];
	    filein.read(bytes);
	    header += ("Magic: " + new String(bytes) + "\n");
	}catch(IOException ie){
            System.out.println("Error in Parsing Header");
        }
    }
    

    private RandomAccessFile filein;
    long index_offset = 0;
    long index_length = 0;
    int number_of_flows_per_read = 0;
    static int FLOWGRAM_BYTES_PER_FLOW = 2;
    
}