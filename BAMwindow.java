import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import java.util.ArrayList;

public class BAMwindow extends JFrame {
    JSpinner spin;
    JSlider slide;

    BAMwindow(final String filename){
	super(filename);
	
	setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	
	initMenu();
		
	PagingModel pm = new PagingModel(filename);
	JTable table = new JTable(pm);
	JTable rowTable = new RowNumberTable(table);

	JTextArea header = new JTextArea(pm.getHeader());
	
	JPanel content = new JPanel();
	content.setLayout(new BorderLayout());
	JScrollPane scrollHeader = new JScrollPane(header);
	JScrollPane scrollTable = new JScrollPane(table);
	scrollTable.setRowHeaderView(rowTable);
	scrollTable.setCorner(JScrollPane.UPPER_LEFT_CORNER, rowTable.getTableHeader());
	content.add(scrollHeader, BorderLayout.CENTER);
	
	try {
	BufferedImage pict = ImageIO.read(new File("BAMseek.png"));
	JLabel pictLabel = new JLabel(new ImageIcon(pict));
	content.add(pictLabel, BorderLayout.WEST);
	} catch(IOException e){}

	JSplitPane jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, content, scrollTable);
	getContentPane().add(jsp, BorderLayout.CENTER);
	
	JPanel pages = new JPanel();
	pages.setLayout(new BorderLayout());
	SpinnerNumberModel spinmodel = new SpinnerNumberModel(1, 1, 100, 1);
	spin = new JSpinner(spinmodel);
	pages.add(spin, BorderLayout.WEST);

	slide = new JSlider(JSlider.HORIZONTAL, 1, 100, 1);
	pages.add(slide, BorderLayout.CENTER);

	getContentPane().add(pages, BorderLayout.SOUTH);

    }
    
    private void initMenu(){
	JMenuBar menuBar = new JMenuBar();
	JMenu fileMenu = new JMenu("File");
	menuBar.add(fileMenu);
	JMenuItem item = new JMenuItem("Open File...");
	item.addActionListener(new OpenAction());
	fileMenu.add(item);
	setJMenuBar(menuBar);
    }
    
    
    
    class OpenAction implements ActionListener {
	public void actionPerformed(ActionEvent ae){
	    
	    JFileChooser choose = new JFileChooser();
	    if(choose.showOpenDialog(BAMwindow.this) == JFileChooser.APPROVE_OPTION){
		System.out.println("got here too!");
		
		try{
		    BAMwindow bw = new BAMwindow(choose.getSelectedFile().getCanonicalPath());
		    bw.pack();
		    bw.setVisible(true);
		}catch(IOException e){}
	    }
	}
    }
}

class PagingModel extends AbstractTableModel {

    protected ArrayList<String[]> data;
    protected String filename = "";
    protected PageReader pr = null;
    protected int column_count = 0;

    public PagingModel(String filename){
	this.filename = filename;
	if(filename.equals("")) return;
	pr = new PageReader(filename);
	this.pr = pr;
	jumpToPage(1);
    }

    public Object getValueAt(int row, int col) {
        //int realRow = row + (pageOffset * pageSize);                                                                                        
        //return data[realRow].getValueAt(col);
	if(data.get(row).length <= col) return "";
        return data.get(row)[col];
    }

    public int getColumnCount() {
	if(pr == null) return 0;
	return column_count;
    }

    public int getRowCount() {
	if(pr == null) return 0;
	return Math.min(1000, data.size());
        //return Math.min(pageSize, data.length);                                                                                             
    }

    private boolean jumpToPage(int page_no){
	if(pr == null) return false;
	//data.clear();
	data = new ArrayList<String []>();
	try{
	    pr.jumpToPage(page_no);
	    String[] fields;
	    while((fields = pr.getNextRecord()) != null){
		data.add(fields);
		if(column_count < fields.length) column_count = fields.length;
	    }
	    return true;
	}catch(IOException e){
	    return false;
	}
    }
    
    public String getHeader(){
	if(filename.equals("")){
            return "Welcome to BAMseek.";
	}
	
        pr = new PageReader(filename);
        if(pr == null){
            return "Unable to recognize file as BAM or SAM";
	}

        return pr.getHeader();
    }

}
