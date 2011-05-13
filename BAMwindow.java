import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import java.util.ArrayList;

public class BAMwindow extends JFrame implements ChangeListener {
    JSpinner spin;
    JSlider slide;
    PagingModel pm = null;
    JTable table = null;
    JTextArea header = null;

    BAMwindow(final String filename){
	super(filename);
	
	setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	
	initMenu();
		
	pm = new PagingModel(filename);
	table = new JTable(pm);
	
	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
	
	JTable rowTable = new RowNumberTable(table);
	header = new JTextArea(pm.getHeader());
	header.setEditable(false);
	
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

	spin.addChangeListener(this);
	slide.addChangeListener(this);

	getContentPane().add(pages, BorderLayout.SOUTH);

    }

    public void stateChanged(ChangeEvent e){
	BoundedRangeModel bound = (BoundedRangeModel)(e.getSource());
	if(!bound.getValueIsAdjusting()){
	    
	    pm.jumpToPage(bound.getValue());
	}
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
		
		try {

		    final String pathname = choose.getSelectedFile().getCanonicalPath();
		    if(pm == null || pm.filename.equals("")){
			pm = new PagingModel(pathname);
			header.setText(pm.getHeader());
			table.setModel(pm);
		    }else{
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
				    BAMwindow bw = new BAMwindow(pathname);
				    bw.pack();
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

    private boolean jumpToPage(int page_no){
	if(pr == null) return false;
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
            return "BAMseek allows you to scroll through large SAM/BAM alignment files.  Please go to \'File > Open\' File to get started.";
	}
	
        if(pr == null){
            return "Unable to recognize file as BAM or SAM";
	}

        return pr.getHeader();
    }

}
