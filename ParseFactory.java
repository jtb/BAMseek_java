import java.io.*;
import net.sf.samtools.util.BlockCompressedInputStream;

public class ParseFactory{
    public static BaseParse NewParse(final String filename){
	if(ParseFactory.isBAM(filename)){
	    return new BAMParse(filename);
	}
	if(ParseFactory.isSAM(filename)){
	    return new SAMParse(filename);
	}
	if(ParseFactory.isVCF(filename)){
	    return new VCFParse(filename);
	}
	if(ParseFactory.isFASTQ(filename)){
	    return new FASTQParse(filename);
	}
	return null;
    }

    public static boolean isBAM(final String filename){
	try{
	    BlockCompressedInputStream bgzf = new BlockCompressedInputStream(new File(filename));
	    bgzf.seek(0);
	    byte arr[] = new byte[4];
	    	       
	    bgzf.read(arr);
	    String magic = new String(arr);
	    bgzf.close();
	    return magic.equals("BAM\1");
	}catch(Exception ie){
	    return false;
	}catch(Throwable t){
	    return false;
	}
    }

    public static boolean isSAM(final String filename){
	try{
	    BufferedReader in = new BufferedReader(new FileReader(filename));
		    
	    String line = null;
	    while((line = in.readLine()) != null){
		String tag = line.substring(0, 3);
		if(!tag.equals("@HD") && !tag.equals("@SQ") && !tag.equals("@RG") && !tag.equals("@PG") && !tag.equals("@CO")){
		    break;
		}
	    }
	    in.close();
	    if(line == null) return true;

	    //First non-header line;
	    String[] fields = line.split("\\t");
	    if(fields.length < 11) return false;
	    if(fields[10].equals("*") || fields[10].length() == fields[9].length()){
		return true;
	    }
	}catch(Exception ie){
	    return false;
	}
	
	return false;
    }

    public static boolean isVCF(final String filename){
	try {
	    LargeFileReader in = new LargeFileReader(filename);
	    //BufferedReader in = new BufferedReader(new FileReader(filename));
	    String line = in.readLine();
	    in.close();
	    if(line != null && line.indexOf("##fileformat=") >= 0){
		return true;
	    }
	}catch(Exception ie){
	    return false;
	}
	return false;
    }
    
    public static boolean isFASTQ(final String filename){
	
	try {
	    BufferedReader in = new BufferedReader(new FileReader(filename));

            String line = null;
            while((line = in.readLine()) != null){
                String tag = line.substring(0, 1);
                if(tag.equals("#")){
                    continue;
                }

		//identification line
		if(!tag.equals("@")){
		    break;
		}
		if((line = in.readLine()) == null) break;
		//Sequence line
		//String seq = "";
		while((line = in.readLine()) != null){
		    //http://illumina.ucr.edu/ht/documentation/standardized-fastq-format-aka-fastq2
		    if(!line.matches("[ACTGNacgtnURYSWKMBDHVN.-]*")){
			break;
		    }
		}
		//Qual id
		if(!line.substring(0,1).equals("+")){
		    break;
		}
		return true;
		   
            }
            	
	    in.close();
	}catch(Exception e){
	    return false;
	}
	
	return false;
    }


}