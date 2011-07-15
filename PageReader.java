import java.util.*;
import java.io.*;
import net.sf.samtools.util.BlockCompressedFilePointerUtil;

public class PageReader {

    public PageReader(final String filename){
	index = new PageIndexer(filename);
	//invalid = index.invalid;
	parser = ParseFactory.NewParse(filename);
    }    

    public boolean update(){
	return index.update();
    }
    public int progress(){
	return index.progress();
    }
    public void finish(){
	index.finish();
	invalid = index.invalid;
	//parser = ParseFactory.NewParse(filename);
    }

    public String getColumnName(int column){
	if(invalid) return "";
	return parser.getColumnName(column);
    }
    public int getNumColumnLabels(){
	if(invalid) return 0;
	return parser.getNumColumnLabels();
    }

    public String getToolTip(final String value, int row, int col, final String[] other_values){
	if(invalid) return "";
	return parser.getToolTip(value, row, col, other_values);
    }

    public int getNumPages(){
	if(index.invalid) return 0;
	return index.numPages();
    }

    public void jumpToPage(final int page_no) throws IOException{
	line_count = 0;
	if(parser == null) throw new IOException("Page out of bounds");
	parser.seek(index.pageToOffset(page_no));
    }

    public String getHeader(){
	if(parser == null) return "";
	return parser.getHeader();
    }

    public String getFilename(){
	if(parser == null) return "";
	return parser.getFilename();
    }

    public String[] getNextRecord() { 
	if(parser != null && line_count < PageIndexer.PAGE_SIZE){
	    line_count++;
	    return parser.getNextRecord();
	}
	return null;
    }
    
    public boolean invalid = true;
    
    private int line_count = 0;
    private BaseParse parser = null;
    private PageIndexer index = null;
        
    private class PageIndexer {
	public static final int PAGE_SIZE = 1000;
	BaseParse parser = null;

	File indexfile = null;
	boolean validindex = false;
	DataOutputStream dos = null;
	boolean writableIndex = true;
	boolean already_recorded = false;
	boolean indexing_on = true;

	public PageIndexer(final String filename){
	    indexing_on = Global.indexing_on;
	    System.out.println("now is "+ indexing_on);
	    indexfile = new File(filename + ".lfidx");
	    try{
		if(indexing_on && indexfile.lastModified() > (new File(filename)).lastModified()){
		    DataInputStream dis = new DataInputStream(new FileInputStream(indexfile));
		    byte arr[] = new byte[8];
		    dis.readFully(arr);
		    String magic = new String(arr);
		    if(magic.equals("LFIDX001")){
			validindex = true;
			while(true){
			    long idx = dis.readLong();
			    pages.add(idx);
			}
		    }
		    dis.close();
		}
	    }catch(EOFException e){

	    }catch(IOException e){
		validindex = false;
	    }
	    
	    parser = ParseFactory.NewParse(filename);
	    done = false;
	    
	    invalid = (parser == null);
	    
	    //Open index for writing.  If valid index does not exist, create one.
	    try{
		if(!invalid && indexing_on){
		    if(validindex){
			dos = new DataOutputStream(new FileOutputStream(indexfile,true));
		    }else{
			dos = new DataOutputStream(new FileOutputStream(indexfile,false));
			dos.writeBytes("LFIDX001");
		    }

		    if(pages.size() > 0){
			parser.seek(pages.get(pages.size()-1));
			already_recorded = true;
		    }

		}
	    }catch(IOException e){
		writableIndex = false;
	    }

	}
	
	public boolean update(){
	    if(invalid) return false;
	    if(done) return false;
	    
	    long offset = 0;
	    for(int count = 0; count < PAGE_SIZE; count++){
		if((offset = parser.getNextRecordIndex()) >= 0){
		    if(count == 0){
			if(!already_recorded){
			    pages.add(offset);
			    try {
				if(dos != null && writableIndex){
				    dos.writeLong(offset);
				}
			    }catch(IOException e){
				//cannot write to index file, oh well.
			    }
			}
			already_recorded = false;
		    }
		}else{
		    done = true;
		    return false;
		}
	    }
	    return true;
	}
	
	public int progress(){
	    return (int)(100*(parser.getProgress()));
	}
	public void finish(){
	    //flush file
	    try{
		if(dos != null){
		    dos.close();
		}
	    }catch(IOException e){}

	    done = true;
	    parser = null;
	}

	public long pageToOffset(int page_no){
	    if(numPages() < 1) return -1;
	    if(page_no > numPages()) page_no = numPages();
	    return pages.get(page_no-1);
	}

	public int numPages(){
	    return pages.size();
	}

	public boolean invalid = true;
	private boolean done = false;

	public ArrayList<Long> pages = new ArrayList<Long>();
	
    }
}
