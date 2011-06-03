import java.io.*;
import java.util.HashMap;
import java.util.regex.Pattern;

public abstract class AlignParse extends BaseParse {
    protected HashMap<String, String> tagmap = new HashMap<String, String>(35);
    
    String col_names[] = {
        "Query Name",
        "Flag",
        "Reference Name",
        "Position",
        "Map Quality",
        "Cigar",
	"Mate Reference",
        "Mate Position",
        "Template Length",
        "Read Sequence",
        "Read Quality"
    };
    
    public AlignParse(final String filename){
	super(filename);
	initTags();
    }

    public abstract void seek(final long offset) throws IOException;
    public abstract long getNextRecordIndex();
    public abstract String[] getNextRecord();

    public abstract double getProgress();
    
    public String getToolTip(final String value, int row, int col, final String next_value){
	if(col == 1){//Flag
	    int n = Integer.parseInt(value);
            return prettyPrintFlag(n);
        }
        if(col == 5){//Cigar
	    return prettyPrintCigar(value);
        }
        if(col == 9 && !next_value.equals("")){//BaseQual
	    return prettyPrintBaseQual(value, next_value);
	}
        if(col > 10){//Tag
	    return prettyPrintTag(value);
        }
	
        return value;
    }
    
    public String getColumnName(int col){ 
	if(col >= col_names.length){
            return "Tag";
        }
        if(col < 0) return "Unknown";
        return col_names[col];
    }
    public int getNumColumnLabels(){
	return col_names.length;
    }

    protected void initTags(){
        tagmap.put("AM", "Smallest template-independent mapping quality of fragments in the rest");
        tagmap.put("AS", "Alignment score");
        tagmap.put("BQ", "Offset to base alignment quality (BAQ)");
        tagmap.put("CC", "Reference name of the next hit");
        tagmap.put("CM", "Edit distance between the color sequence and the color reference");
        tagmap.put("CP", "Leftmost coordinate of the next hit");
        tagmap.put("CQ", "Color read quality");
        tagmap.put("CS", "Color read sequence");
        tagmap.put("E2", "The 2nd most likely base calls");
        tagmap.put("FI", "The index of fragment in the template");
        tagmap.put("FS", "Fragment suffix");
        tagmap.put("FZ", "Flow signal intensities");
        tagmap.put("LB", "Library");
        tagmap.put("H0", "Number of perfect hits");
        tagmap.put("H1", "Number of 1-difference hits");
        tagmap.put("H2", "Number of 2-difference hits");
        tagmap.put("HI", "Query hit index");
        tagmap.put("IH", "Number of stored alignments in SAM that contains the query in the current record");
        tagmap.put("MD", "String for mismatching positions");
        tagmap.put("MQ", "Mapping quality of the mate fragment");
        tagmap.put("NH", "Number of alignments of query read");
        tagmap.put("NM", "Edit distance to the reference");
        tagmap.put("OQ", "Original base quality");
        tagmap.put("OP", "Original mapping position");
        tagmap.put("OC", "Original CIGAR");
        tagmap.put("PG", "Program");
        tagmap.put("PQ", "Phred likelihood of the template");
        tagmap.put("PU", "Platform unit");
        tagmap.put("Q2", "Phred quality of the mate fragment");
        tagmap.put("R2", "Sequence of the mate fragment in the template");
        tagmap.put("RG", "Read group");
        tagmap.put("SM", "Template-independent mapping quality");
        tagmap.put("TC", "Number of fragments in the template");
        tagmap.put("U2", "Phred probility of the 2nd call being wrong conditional on the best being wrong");
        tagmap.put("UQ", "Phred likelihood of the fragment");
    }
    
    protected String prettyPrintFlag(int flag){
        if(flag<0) return "";
        boolean unmapped = false;
        boolean unmappedmate = false;
        boolean paired = false;
        String answer = "<html>";
        if(flag%2 != 0){
            answer+="Read is paired.<br>";
            paired = true;
        }
        flag = (flag >> 1);
        if(flag%2 != 0){
            answer+="Read mapped in proper pair.<br>";
        }
        flag = (flag >> 1);
        if(flag%2 != 0){
            answer+="Read is unmapped.<br>";
            unmapped = true;
        }
        flag = (flag >> 1);
        if(flag%2 != 0){
            answer+="Mate is unmapped.<br>";
            unmappedmate = true;
        }
        flag = (flag >> 1);
        if(flag%2 != 0){
            answer+="Read is on reverse strand.<br>";
        }else if(!unmapped){
            answer+="Read is on forward strand.<br>";
        }
        flag = (flag >> 1);
        if(flag%2 != 0){
            answer+="Mate is on reverse strand.<br>";
        }else if(paired && !unmappedmate){
            answer+="Mate is on forward strand.<br>";
        }
        flag = (flag >> 1);
        if(flag%2 != 0){
            answer+="Read is first in template.<br>";
        }
        flag = (flag >> 1);
        if(flag%2 != 0){
            answer+="Read is last in template.<br>";
        }
        flag = (flag >> 1);
        if(flag%2 != 0){
            answer+="Read is not primary alignment.<br>";
        }
        flag = (flag >> 1);
        if(flag%2 != 0){
            answer+="Read fails platform/vendor quality checks.<br>";
        }
        flag = (flag >> 1);
        if(flag%2 != 0){
            answer+="Read is PCR or optical duplicate.<br>";
        }

        answer += "</html>";
        return answer;
    }

    protected String prettyPrintCigar(String cigar){
        String ans = "<html>";
        if(cigar.equals("*")){
            ans += "No alignment information<br>";
        }else{
            Pattern p = Pattern.compile("\\D");
            String nums[] = p.split(cigar);
            p = Pattern.compile("\\d+");
            String vals[] = p.split(cigar);


            for(int i = 0; i < nums.length; i++){
                ans += (nums[i] + " ");
                switch(vals[i+1].charAt(0)){
                case 'M' : case 'm' : ans += "Match/Mismatch<br>"; break;
                case 'I' : case 'i' : ans += "Insertion to reference<br>"; break;
                case 'D' : case 'd' : ans += "Deletion from reference<br>"; break;
                case 'N' : case 'n' : ans += "Skipped region from reference<br>"; break;
                case 'S' : case 's' : ans += "Soft clipping (clipped sequence present)<br>"; break;
                case 'H' : case 'h' : ans += "Hard clipping (clipped sequence removed)<br>"; break;
                case 'P' : case 'p' : ans += "Padding (silent deletion from padded reference)<br>"; break;
                case '=' : ans += "Match<br>"; break;
                case 'X' : case 'x' : ans += "Mismatch<br>"; break;
                default : ans += (vals[i] + "<br>"); break;
                }
            }

        }

        ans += "</html>";
        return ans;
    }

    protected String prettyPrintBaseQual(String bases, String quals){
        if(bases.equals("*") || bases.length() != quals.length()) return ("<html><font size=\"5\">" + bases + "</font></html>");
        String hexcolor = "<html>";
        for(int i = 0; i < bases.length(); i++){
            hexcolor += "<font size=\"5\" color=\"";
            int c = (int)quals.charAt(i) - 33;
            if(c < 20) hexcolor += "#E9CFEC";else hexcolor += "#571B7e";

            hexcolor += "\">";
            hexcolor += bases.charAt(i);
            hexcolor += "</font>";
        }
        hexcolor += "</html>";
        return hexcolor;
    }

    protected String prettyPrintTag(String tag){
        String[] fields = tag.split(":");
        if(fields.length < 3) return tag;
        String ans = "";

        String descript = null;
        if((descript = tagmap.get(fields[0])) == null){
            descript = fields[0];
        }
        return (descript + ": " + fields[2]);
    }
    
}