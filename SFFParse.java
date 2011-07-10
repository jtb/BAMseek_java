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
		offset = filein.getFilePointer();
	    }
	    	    
	    int read_header_length = filein.readUnsignedShort();
	    int name_length = filein.readUnsignedShort();
	    
	    byte buffer[] = new byte[4];
	    filein.read(buffer);
	    long number_of_bases = BytesHelper.toUintRev(buffer);
	    
	    filein.seek(offset + read_header_length);
	    long read_data_size = FLOWGRAM_BYTES_PER_FLOW*number_of_flows_per_read + 3*number_of_bases;
	    if((read_data_size & 7) > 0){
		read_data_size = (((read_data_size >> 3)+1) <<3);
	    }
	    
	    filein.seek(offset + read_header_length + read_data_size);
	    return offset;
	}catch(Exception e){
	    return -1;
	}
    }
    
    public String[] getNextRecord(){
	ArrayList<String> fields = new ArrayList<String>();
	
	try{
	    long offset = filein.getFilePointer();
	    int read_header_length = filein.readUnsignedShort();
	    int name_length = filein.readUnsignedShort();

	    int number_of_bases = filein.readInt();
	    
	    int clip_qual_left = filein.readUnsignedShort();
	    int clip_qual_right = filein.readUnsignedShort();
	    int clip_adapter_left = filein.readUnsignedShort();
	    int clip_adapter_right = filein.readUnsignedShort();
	    
	    int first_insert_base = Math.max(1, Math.max(clip_qual_left, clip_qual_right)) - 1;
	    int last_insert_base = Math.min((clip_qual_right==0 ? number_of_bases : clip_qual_right),(clip_adapter_right==0 ? number_of_bases : clip_adapter_right));
	    
	    //System.out.println(clip_qual_left + " " + clip_qual_right + " " + clip_adapter_left + " " + clip_adapter_right);

	    byte buffer[] = new byte[name_length];
	    filein.read(buffer);
            char Name[] = new String(buffer).toCharArray();

	    filein.seek(offset + read_header_length);
	    long read_data_size = FLOWGRAM_BYTES_PER_FLOW*number_of_flows_per_read + 3*number_of_bases;
            if((read_data_size & 7) > 0){
                read_data_size = (((read_data_size >> 3)+1) <<3);
            }

	    //???
	    //int values[] = new int[number_of_flows_per_read];
	    //for(int i = 0; i < values.length; i++){
	    //values[i] = filein.readUnsignedShort();
	    //}

	    //???
	    //filein.skipBytes(2*number_of_flows_per_read);
	    filein.seek(filein.getFilePointer() + 2*number_of_flows_per_read + number_of_bases);
	    
	    buffer = new byte[number_of_bases];
	    filein.read(buffer);
	    char Bases[] = new String(buffer).toCharArray();
	    for(int i = 0; i < first_insert_base; i++){
		Bases[i] = Character.toLowerCase(Bases[i]);
	    }
	    for(int i = last_insert_base; i < Bases.length; i++){
		Bases[i] = Character.toLowerCase(Bases[i]);
	    }
	    

	    byte qual[] = new byte[number_of_bases];
	    filein.read(qual);
	    for(int i = 0; i < qual.length; i++){
		qual[i] += 33;
	    }
	    
	    //Go to end of read block.
	    filein.seek(offset + read_header_length + read_data_size);
            
	    fields.add(new String(Name));
	    fields.add(new String(Bases));
	    fields.add(new String(qual));

	}catch(Exception e){
            return null;
        }
	
	String fa[] = new String[fields.size()];
	return fields.toArray(fa);
		
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
	    filein.read(bytes);
	    header += "Version: ";
	    for(int i = 0; i < bytes.length; i++){
		header += (int) bytes[i];
	    }
	    header += "\n";
	    
	    index_offset = filein.readLong();
	    header += ("Index Offset: " + index_offset + "\n");
	    
	    filein.read(bytes);
	    index_length = BytesHelper.toUintRev(bytes);
	    header += ("Index Length: " + index_length + "\n");
	    
	    filein.read(bytes);
	    header += ("# of Reads: " + BytesHelper.toUintRev(bytes) + "\n");

	    int header_length = filein.readUnsignedShort();
	    
	    int key_length = filein.readUnsignedShort();
	    number_of_flows_per_read = filein.readUnsignedShort();
	    
	    header += ("Format Code: " + filein.readUnsignedByte() + "\n");
	    
	    bytes = new byte[number_of_flows_per_read];
	    filein.read(bytes);
	    flow_chars = new String(bytes).toCharArray();
	    
	    bytes = new byte[key_length];
	    filein.read(bytes);
	    header += ("Key Sequence: " + new String(bytes) + "\n");
	    
	    filein.seek(header_length);

	}catch(IOException ie){
            System.out.println("Error in Parsing Header");
        }
    }
    

    private RandomAccessFile filein;
    long index_offset = 0;
    long index_length = 0;
    int number_of_flows_per_read = 0;
    static int FLOWGRAM_BYTES_PER_FLOW = 2;
    char flow_chars[] = null;

}