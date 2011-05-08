import java.util.*;
import java.io.*;

public class PageReader {

    public PageReader(final String filename){
	index = new PageIndexer(filename);
	invalid = index.invalid;
	parser = ParseFactory.NewParse(filename);
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

	public PageIndexer(final String filename){
	    BaseParse parser = ParseFactory.NewParse(filename);
	    	    
	    if(parser == null){
		invalid = true;
		return;
	    }
	    
	    long offset = 0;
	    int count = 0;
	    while((offset = parser.getNextRecordIndex()) >= 0){
		if(count == 0){
		    pages.add(offset);
		    //progress.setValue(100*(parser.getProgress()));
		    //....
		    
		}
		count++;
		if(count == PAGE_SIZE) count = 0;
	    }
	    //progress.setValue(100);
	    invalid = false;
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

	public ArrayList<Long> pages = new ArrayList<Long>();
	
    }
}