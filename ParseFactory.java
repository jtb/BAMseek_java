
public class ParseFactory{
    public static BaseParse NewParse(final String filename){
	
	if(ParseFactory.isBAM(filename)){
	    return null;
	}
	if(ParseFactory.isSAM(filename)){
	    return new SAMParse(filename);
	}
	
	return null;
    }

    public static boolean isBAM(final String filename){
	return false;
    }

    public static boolean isSAM(final String filename){
	return true;
    }
    
}