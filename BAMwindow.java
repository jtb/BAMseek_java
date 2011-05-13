import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
//import javax.swing.table.TableColumn;
import javax.swing.event.*;

import java.util.ArrayList;

public class BAMwindow extends JFrame {
    PageControl pages = null;
    PagingModel pm = null;
    JTable table = null;
    JTextArea header = null;
    JSplitPane jsp = null;
    
    BAMwindow(final String filename){
	super(filename);
	
	setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	
	initMenu();
		
	pm = new PagingModel(filename);
	table = new JTable(pm);
	
	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	/*
	try{
	    //rowTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	    int vColIndex = 1;
	    TableColumn col = table.getColumnModel().getColumn(vColIndex);
	    int width = 300;
	    col.setPreferredWidth(width);
	}catch(Exception e){}
	*/

	JTable rowTable = new RowNumberTable(table);
	
	JPanel content = new JPanel();
	content.setLayout(new BorderLayout());
	header = new JTextArea(pm.getHeader());
	header.setEditable(false);
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

	jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, content, scrollTable);
	getContentPane().add(jsp, BorderLayout.CENTER);
	
	pages = new PageControl();
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
    
    class PageControl extends JPanel implements ChangeListener {
	JSpinner spin = null;
	JSlider slide = null;
	Label numpages = null;
	SpinnerNumberModel spinmodel = null;
	BoundedRangeModel slidemodel = null;
	int page_no = 0;

	PageControl(){
	    //setLayout(new BorderLayout());
	    add(new Label("Page Number "));
	    spinmodel = new SpinnerNumberModel(1, 1, pm.numPages(), 1);
	    spin = new JSpinner(spinmodel);

	    spinmodel.addChangeListener(new ChangeListener() {
		    public void stateChanged(ChangeEvent e) {
			page_no = spinmodel.getNumber().intValue();
			slide.setValue(page_no);
		    }
		});
	    add(spin);//, BorderLayout.WEST);
	    numpages = new Label("/" + pm.numPages());
	    add(numpages);
	    
	    slide = new JSlider(JSlider.HORIZONTAL, 1, pm.numPages(), 1);
	    slidemodel = slide.getModel();
	    slidemodel.addChangeListener(new ChangeListener() {
		    public void stateChanged(ChangeEvent e) {
			page_no = slidemodel.getValue();
			spin.setValue(new Integer(page_no));
		    }
		});

	    add(slide);//, BorderLayout.CENTER);
	    slide.addChangeListener(this);
	}

	public void stateChanged(ChangeEvent e){

	    JSlider event = (JSlider)(e.getSource());
	    if(!event.getValueIsAdjusting()){
		pm.jumpToPage(page_no);
	    }
	}
    }
    
    class OpenAction implements ActionListener {
	public void actionPerformed(ActionEvent ae){
	    
	    JFileChooser choose = new JFileChooser();
	    if(choose.showOpenDialog(BAMwindow.this) == JFileChooser.APPROVE_OPTION){
		
		try {

		    final String pathname = choose.getSelectedFile().getCanonicalPath();
		    if(pm == null || pm.filename.equals("")){
			pm = new PagingModel(pathname);
			pages = new PageControl();
			getContentPane().removeAll();
			getContentPane().add(jsp, BorderLayout.CENTER);
			getContentPane().add(pages, BorderLayout.SOUTH);
			header.setText(pm.getHeader());
			table.setModel(pm);
			setVisible(true);
		    }else{
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
				    BAMwindow bw = new BAMwindow(pathname);
				    bw.pack();
				    bw.jsp.setDividerLocation(.2);
				    bw.setVisible(true);
				}
			    });
		    }
		}catch(IOException e){}
	    }
	}
    }
}

class PagingModel extends AbstractTableModel {

    protected ArrayList<String[]> data;
    public String filename = "";
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
    	if(data.get(row).length <= col) return "";
        return data.get(row)[col];
    }

    public int getColumnCount() {
	return column_count;
    }

    public int getRowCount() {
	if(pr == null) return 0;
	return Math.min(1000, data.size());
    }

    public boolean jumpToPage(int page_no){
	if(pr == null) return false;
	data = new ArrayList<String []>();
	
	try{
	    pr.jumpToPage(page_no);
	    String[] fields;
	    while((fields = pr.getNextRecord()) != null){
		data.add(fields);
		if(column_count < fields.length) column_count = fields.length;
	    }
	    fireTableDataChanged();
	    return true;
	}catch(IOException e){
	    return false;
	}
    }
    
    public int numPages(){
	if(pr == null) return 1;
	return pr.getNumPages();
    }

    public String getHeader(){
	if(filename.equals("")){
            return "BAMseek allows you to scroll through large SAM/BAM alignment files.  Please go to \'File > Open\' File to get started.";
	}
	
        if(pr == null){
            return "Unable to recognize file as BAM or SAM";
	}

        return pr.getHeader();
    }

}
