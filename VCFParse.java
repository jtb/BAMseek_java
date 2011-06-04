import java.util.*;
import java.io.*;

public class VCFParse extends BaseParse {
    protected HashMap<String, String> tagmap = new HashMap<String, String>(16);
    private static final int BUFF_SIZE = 1024*128;
    private static final byte[] buffer = new byte[BUFF_SIZE];
    private int buffer_pos;
    private int buffer_size;

    private long file_pos;
    private RandomAccessFile filein;

    String col_names[] =  {
	"Chromosome",
	"Position",
	"ID",
	"Reference Base(s)",
	"Alternate non-reference alleles",
	"Quality",
	"Filter",
	"Info"
    };

    public VCFParse(final String filename){
	super(filename);
	initTags();

	buffer_pos = 0;
	file_pos = 0;

	try {
            filein = new RandomAccessFile(filename, "r");
            parseHeader();
	    parseColumnLabels();
            file_pos = filein.getFilePointer();
	}catch(IOException ie){}
    }

    private void parseColumnLabels(){
	try {
	    long pos = filein.getFilePointer();
	    String line = filein.readLine();
	    if(line != null && line.length() > 1 && line.substring(0,1).equals("#")){
		col_names = line.substring(1).split("\\t");
	    }else{
		//Go back to beginning of the line
		filein.seek(pos);
	    }
	}catch(IOException ie){}
    }

    private void parseHeader(){
	String line = null;
	header = "";
	long pos = 0;

	try {
	    pos = filein.getFilePointer();
	    while((line = filein.readLine()) != null){
		String tag = line.substring(0,2);
		if(tag.equals("##")){
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

    public void seek(final long offset) throws IOException{
	filein.seek(offset);
    }

    public long getNextRecordIndex(){
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

    public String[] getNextRecord(){
	try {
            String s = filein.readLine();
            if(s == null) return null;
            return s.split("\\t");
        }catch(IOException e){
            return null;
        }
    }

    public double getProgress(){
	try {
            return 1.0*filein.getFilePointer()/(1.0*filein.length());
        } catch (IOException e){
            return 1.0;
        }
    }
    
    public String getToolTip(final String value, int row, int col, final String[] other_values){
	if(col == 7){
	    return prettyPrintInfo(value);
	}
	if(col > 8){
	    String ref = other_values[3];
	    String alt = other_values[4];
	    String format = other_values[8];
	    return prettyPrintSample(value, format, ref, alt);
	}

	return value;
    }
    
    private String prettyPrintInfo(String value){
	String ans = "<html>";
	String fields[] = value.split(";");
	if(fields.length < 1) return value;

	for(int i = 0; i < fields.length; i++){
	    String tags[] = fields[i].split("=");
	    if(tagmap.get(tags[0]) == null){
		ans += tags[0];
	    }else{
		ans += tagmap.get(tags[0]);
	    }
	    if(tags.length > 1){
		ans += (" = " + tags[1]);
	    }
	    ans += "<br>";
	}
	ans += "</html>";
	return ans;
    }

    private int getInt(String value){
	try{
	    return Integer.parseInt(value);
	}catch(NumberFormatException e){
	    return -1;
	}
    }

    private String prettyPrintSample(String value, String format, String reference, String alt){
	String formats[] = format.split(":");
	String values[] = value.split(":");
	String alts[] = alt.split(",");
	if(values.length < 1) return value;
	if(formats.length < values.length) return value;

	String ans = "";

	for(int i = 0; i < values.length; i++){
	    if(formats[i].equals("GT")){
		ans += "Genotype";
		String alleles[] = values[i].split("/");
		if(alleles.length != 2){
		    alleles = values[i].split("\\|");
		    if(alleles.length == 2){
			ans += " (Phased) ";
		    }
		}else{
		    ans += " (Unphased) ";
		}
		
		if(alleles.length != 2){
		    ans += " " + values[i];
		}else{
		    int val0 = getInt(alleles[0]);
		    int val1 = getInt(alleles[1]);
		    if(val0 == 0){
			ans += (reference + ",");
		    }
		    else if(val0 < 1 || val0 > alts.length){
			ans += "unknown,";
		    }else{
			ans += (alts[val0-1] + ",");
		    }
		    
		    if(val1 == 0){
			ans += reference;
		    }
		    else if(val1 < 1 || val1 > alts.length){
			ans += "unknown";
		    }else{
			ans += alts[val1-1];
		    }
		}
		
	    }
	    else if(formats[i].equals("DP")){
		ans += ("Sample Depth " + values[i]);
		
	    }
	    else if(formats[i].equals("FT")){
		ans += ("Genotype Filter " + values[i]);
	    }
	    else if(formats[i].equals("GL")){
		ans += "Log10-scaled Likelihoods ";
		String nums[] = values[i].split(",");
		if(nums.length != 3){
		    ans += values[i];
		}else{
		    ans += ("AA:" + nums[0] + ",");
		    ans += ("AB:" + nums[1] + ",");
		    ans += ("BB:" + nums[2]);
		}
		
	    }
	    else if(formats[i].equals("GQ")){
		ans += ("Phred Genotype Quality " + values[i]);
	    }
	    else if(formats[i].equals("HQ")){
		ans += ("Phred Haplotype Qualities " + values[i]);
	    }
	    ans += "BREAKEDLINED";
	}
	
	ans = ans.replaceAll(">", "&gt;");
	ans = ans.replaceAll("<", "&lt;");
	ans = ans.replaceAll("BREAKEDLINED", "<br>");
	return ("<html>" + ans + "</html>");
	
    }

    public String getColumnName(int col){ 
	if(col >= col_names.length){
	    return "Sample";
	} 
	if(col < 0){
            return "Unknown";
        }
	return col_names[col];
    }
    
    public int getNumColumnLabels() { 
	return col_names.length;
    }

    private void initTags(){
	tagmap.put("AA", "Ancestral allele");
	tagmap.put("AC", "Allele count");
	tagmap.put("AF", "Allele frequency");
	tagmap.put("AN", "Total number of alleles");
	tagmap.put("BQ", "RMS base quality");
	tagmap.put("CIGAR", "Alignment description");
	tagmap.put("DB", "dbSNP membership");
	tagmap.put("DP", "Combined depth across samples");
	tagmap.put("END", "End position of variant");
	tagmap.put("H2", "hapmap2 membership");
	tagmap.put("MQ", "RMS mapping quality");
	tagmap.put("MQ0", "Number of reads with MAPQ of 0");
	tagmap.put("NS", "Number of samples");
	tagmap.put("SB", "Strand bias");
	tagmap.put("SOMATIC", "Somatic mutation");
	tagmap.put("VALIDATED", "Follow-up validation");
    }

}
