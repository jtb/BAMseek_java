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

	public PageIndexer(final String filename){
	    parser = ParseFactory.NewParse(filename);
	    done = false;
	    
	    invalid = (parser == null);
	}
	
	public boolean update(){
	    if(invalid) return false;
	    if(done) return false;
	    
	    long offset = 0;
	    for(int count = 0; count < PAGE_SIZE; count++){
		if((offset = parser.getNextRecordIndex()) >= 0){
		    if(count == 0){
			pages.add(offset);
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