import java.util.*;
import java.io.*;
import net.sf.samtools.util.BlockCompressedInputStream;
import net.sf.samtools.util.BlockCompressedFilePointerUtil;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BAMParse extends BaseParse {
    BlockCompressedInputStream bgzf = null;
    HashMap<Integer, String> refmap = null;
    int block_size = 0;
    long file_size = 1;

    public BAMParse(final String filename){
	super(filename);

	try{
	    file_size = (new File(filename)).length();
	}catch(Exception e){
	    file_size = 1;
	}

	try {
	    bgzf = new BlockCompressedInputStream(new File(filename));
	    if(!isBAM()){
		bgzf.close();
	    }else{
		parseHeader();
		getReferences();
	    }
	}catch(IOException ie){}	
    }

    private boolean isBAM(){
	try {
	    bgzf.seek(0);
	    byte arr[] = new byte[4];
	    bgzf.read(arr);
	    String magic = new String(arr);
	    return magic.equals("BAM\1");
	}catch(IOException ie){
	    return false;
	}
    }
    
    public void seek(final long offset) throws IOException{
	bgzf.seek(offset);
    }

    public long getNextRecordIndex() {
	long offset = 0;
	try {
	    offset = bgzf.getFilePointer();
	    byte buffer[] = new byte[4];
	    if(bgzf.read(buffer) != 4) return -1;
	    block_size = BytesHelper.toInt(buffer);
	    byte record[] = new byte[block_size];
	    bgzf.read(record);
	    return offset;
	}catch(Exception e){
	    return -1;
	}
    }

    public String[] getNextRecord() {
	ArrayList<String> fields = new ArrayList<String>();
	try{
	    byte shorty[] = new byte[2];
	    byte buffer[] = new byte[36];
	    if(bgzf.read(buffer) != 36) return null;
	    ByteBuffer buf = ByteBuffer.wrap(buffer);
	    buf.order(ByteOrder.LITTLE_ENDIAN);
	    block_size = buf.getInt();
	  
	    int refID = buf.getInt();
	    String RNAME = null;
	    if((RNAME = refmap.get(new Integer(refID))) == null){
	   	RNAME = new String("*");
	    }


	    int POS = buf.getInt() + 1;
	    
	    int l_read_name = (buf.get() & 0xFF);
	    int MAPQ = (buf.get() & 0xFF);
	    buf.get(); buf.get();

	    buf.get(shorty);
	    int n_cigar_op = (((shorty[1] & 0xFF) << 8)+ (shorty[0] & 0xFF));
	    
	    buf.get(shorty);
	    int FLAG = (((shorty[1] & 0xFF) << 8)+ (shorty[0] & 0xFF));
	    
	    int l_seq = buf.getInt();
	    int next_refID = buf.getInt();
	    int PNEXT = buf.getInt() + 1;
	    String RNEXT = null;
	    if(refID == next_refID){
		RNEXT = "=";
	    }else{
		if((RNEXT = refmap.get(new Integer(next_refID))) == null){
		    RNEXT = new String("*");
		}
	    }
	    
	    int TLEN = buf.getInt();
	    block_size -= 32;

	    
	    byte read_name[] = new byte[l_read_name-1];
	    bgzf.read(read_name);
	    bgzf.read(new byte[1]);//null terminated
	    String QNAME = new String(read_name);
	    block_size -= l_read_name;
	    
	    String CIGAR = "";
	    byte cigar[] = new byte[4];
	    for(int i = 0; i < n_cigar_op; i++){
		bgzf.read(cigar);
		int cig = BytesHelper.toInt(cigar);
		CIGAR += ((cig >> 4) & 0xFFFFFFF);
		CIGAR += getOp(cig & 0xF);
	    }
	    if(CIGAR.length() < 1) CIGAR = "*";
	    block_size -= (4*n_cigar_op);

	    int len = (l_seq+1)/2;
	    byte seq[] = new byte[len];
	    bgzf.read(seq);
	    String SEQ  = "";
	    for(int i = 0; i < len; i++){
		SEQ += getBase(((seq[i] >> 4) & 0xF));
		if((2*i + 1) < l_seq){
		    SEQ += getBase((seq[i] & 0xF));
		}
	    }
	    if(SEQ.length() < 1) SEQ = "*";
	    
	    block_size -= len;
	    	   
	    String QUAL = new String("*");
	    if(l_seq != 0){
		boolean absent = true;
		byte qual[] = new byte[l_seq];
		bgzf.read(qual);
		for(int i = 0; i < qual.length; i++){
		    if(qual[i] != -1) absent = false;
		    qual[i] += 33;
		}
		if(!absent) QUAL = new String(qual);
	    }
	    block_size -= l_seq;
	    
	    fields.add(QNAME);
	    fields.add("" + FLAG);
	    fields.add(RNAME);
	    fields.add("" + POS);
	    fields.add("" + MAPQ);
	    fields.add(CIGAR);
	    fields.add(RNEXT);
	    fields.add("" + PNEXT);
	    fields.add("" + TLEN);
	    fields.add(SEQ);
	    fields.add(QUAL);

	    while(block_size > 0){
		byte tag[] = new byte[2];
		bgzf.read(tag);
		String ans = new String(tag) + ":";
		block_size -= 2;

		byte val_type[] = new byte[1];
		bgzf.read(val_type);
		block_size -= 1;
		String value = getVal(val_type);
		
		if(val_type[0] != 'A' && val_type[0] != 'B' && val_type[0] != 'f' && val_type[0] != 'Z' && val_type[0] != 'H'){
		    val_type[0] = 'i';
		}
		ans  = ans + new String(val_type) + ":" + value;
		fields.add(ans);
	    }

	    //byte remains[] = new byte[block_size];
	    //bgzf.read(remains);

	}catch(Exception e){
	    return null;
	}
	String fa[] = new String[fields.size()];
	return fields.toArray(fa);
    }
    
    public double getProgress() {
	return (1.0*BlockCompressedFilePointerUtil.getBlockAddress(bgzf.getFilePointer()))/(1.0*file_size);
    }

    private void parseHeader(){
	header = "";
	try{
	    byte bytes[] = new byte[4];
	    bgzf.read(bytes);
	    int headerTextLength = BytesHelper.toInt(bytes);
	    bytes = new byte[headerTextLength];
	    bgzf.read(bytes);
	    header = new String(bytes);
	}catch(IOException ie){
	    System.out.println("Error in Parsing Header");
	}
    }

    private void getReferences(){
	try{
	    byte buffer[] = new byte[4];
	    bgzf.read(buffer);
	    int n_ref = BytesHelper.toInt(buffer);
	    refmap = new HashMap<Integer, String>(n_ref);
	    refmap.put(new Integer(-1), "*");

	    for(int i = 0; i < n_ref; i++){
		bgzf.read(buffer);
		int l_name = BytesHelper.toInt(buffer);
		byte name[] = new byte[l_name-1];
		bgzf.read(name);
		refmap.put(new Integer(i), new String(name));
		bgzf.read(new byte[5]);//read null-terminator and length of reference sequence
		
	    }

	}catch(IOException ie){
	    System.out.println("Error in getting references");
	}
    }
    
    private char getBase(int val){
	if(val <= 0) return '=';
	if(val <= 1) return 'A';
	if(val <= 2) return 'C';
	if(val <= 4) return 'G';
	if(val <= 8) return 'T';
	return 'N';
    }

    private char getOp(int val){
	switch(val){
	case 0 : return 'M';
	case 1 : return 'I';
	case 2 : return 'D';
	case 3 : return 'N';
	case 4 : return 'S';
	case 5 : return 'H';
	case 6 : return 'P';
	case 7 : return '=';
	}
	return 'X';
    }

    private String getVal(byte[] val_type){
	String ans = null;;
	byte buffer[] = null;
	int num = 0;
	try {
	    switch(val_type[0]){
	    case 'A':
		buffer = new byte[1];
		bgzf.read(buffer);
		block_size -= 1;
		ans = new String(buffer);
		break;
	    case 'c':
		buffer = new byte[1];
                bgzf.read(buffer);
		block_size -= 1;
		ans = new String(buffer);
                break;
	    case 'C':
		buffer = new byte[1];
		bgzf.read(buffer);
		block_size -= 1;
		num = ((int)buffer[0] & 0xFF);
		ans = "" + num;
		break;
	    case 's':
		buffer = new byte[2];
		bgzf.read(buffer);
		block_size -= 2;
		ans = "" + BytesHelper.shortToInt(buffer);
		break;
	    case 'S':
		buffer = new byte[2];
		bgzf.read(buffer);
		block_size -= 2;
		ans = "" + BytesHelper.shortToUint(buffer);
		break;
	    case 'i':
		buffer = new byte[4];
		bgzf.read(buffer);
		block_size -= 4;
		ans = "" + BytesHelper.toInt(buffer);
		break;
	    case 'I':
		buffer = new byte[4];
		bgzf.read(buffer);
		block_size -= 4;
		ans = "" + BytesHelper.toUint(buffer);
		break;
	    case 'f':
		buffer = new byte[4];
		block_size -= bgzf.read(buffer);
		ans = "" + Float.intBitsToFloat(BytesHelper.toInt(buffer));
		break;
	    case 'Z':
		ans = getNullTerminatedString();
		break;
	    case 'H':
		ans = getHexString();
		break;
	    case 'B':
		buffer = new byte[1];
		block_size -= bgzf.read(buffer);
		ans = new String(buffer);
		byte lenbuff[] = new byte[4];
		block_size -= bgzf.read(lenbuff);
		int len = BytesHelper.toInt(lenbuff);
		for(int i = 0; i < len; i++){
		    ans += ",";
		    ans += getVal(buffer);
		}
		break;
	    
	    }
	}catch(Exception e){
	    return ans;
	}

	return ans;
    }


    private String getNullTerminatedString(){
	byte buffer[] = new byte[1];
	String ans = "";
	try{
	    while(bgzf.read(buffer) > 0){
		block_size--;
		if(buffer[0] == 0) break;
		ans += (char)buffer[0];
	    }
	}catch(Exception e){
	    return ans;
	}
	return ans;
    }

    private String getHexString(){
	byte buffer[] = new byte[1];
	String ans = "";
	int val = 0;
	try {
	    while(bgzf.read(buffer) > 0){
		block_size--;
		val = ((buffer[0] >> 4) & 0xF);
		if(val == 0) break;
		ans += Integer.toHexString(val);
		
		val = (buffer[0] & 0xF);
		if(val == 0) break;
		ans += Integer.toHexString(val);
	    }
	}catch(Exception e){
	    return ans;
	}
	return ans;
    }
}