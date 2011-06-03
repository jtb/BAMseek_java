import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.beans.*;
import java.io.*;
//import javax.imageio.ImageIO;
//import java.awt.image.BufferedImage;
import javax.swing.ImageIcon;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import javax.swing.table.TableColumn;
import javax.swing.event.*;

import java.util.ArrayList;

public class BAMwindow extends JFrame implements PropertyChangeListener {
    PageControl pages = null;
    PagingModel pm = null;
    JTable table = null;
    JTextArea header = null;
    JSplitPane jsp = null;
    String pwd = null;
        
    ProgressMonitor progressMonitor = null;
    Task task = null;
    
    class Task extends SwingWorker<Void, Void> {
	String file = null;
	Task(final String filename){
	    file = filename;
	}
	@Override
	    public Void doInBackground(){
	    int progress = 0;
	    setProgress(0);
	    //do progress
	    pm = new PagingModel(file);
	    while(!isCancelled() && pm.update()){
		setProgress(pm.progress());
	    }
	    //pm.finish();
	        
	    return null;
	}
	@Override
	    public void done(){
	    pm.finish();
	    pages = new PageControl();
	    getContentPane().removeAll();
	    getContentPane().add(jsp, BorderLayout.CENTER);
	    getContentPane().add(pages, BorderLayout.SOUTH);
	    String header_text = pm.getHeader();
	    if(header_text != null){
		header.setText(header_text);
	    }
	    header.setCaretPosition(0);
	    
	    table.setModel(pm);
	    setTitle(file);

	    for(int i = 0; i < Math.min(pm.getColumnCount(), pm.col_sizes.length); i++){
		TableColumn col = table.getColumnModel().getColumn(i);
		col.setPreferredWidth(pm.col_sizes[i]*8+10);
	    }
	    
	    //jsp.setDividerLocation(.2);
	    //pack();
	    setVisible(true);
	    //slide.setValue(slide.getMinimum());

	    progressMonitor.setProgress(100);

	    if(header_text == null){
		JOptionPane.showMessageDialog(BAMwindow.this, "Error: Unable to recognize file as BAM or SAM.");
	    }
	}
    }

    protected void openData(final String pathname){
	progressMonitor = new ProgressMonitor(BAMwindow.this,
					      "Indexing file.  You may cancel to view the first few lines.",
					      "", 0, 100);
        progressMonitor.setProgress(0);

	task = new Task(pathname);
	task.addPropertyChangeListener(this);
	task.execute();
    }

    public void propertyChange(PropertyChangeEvent evt){
	if("progress" == evt.getPropertyName()){
	    int progress = (Integer) evt.getNewValue();
	    progressMonitor.setProgress(progress);
	    String message = String.format("Completed %d%%.\n", progress);
	    progressMonitor.setNote(message);
	    
	    if(progressMonitor.isCanceled()){
		task.cancel(true);
	    }
	}
    }

    BAMwindow(){
	super("BAMseek: Large BAM/SAM File Viewer");
	
	setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
	initMenu();

	table = new JTable(pm){
		public String getToolTipText(MouseEvent e) {
		    java.awt.Point p = e.getPoint();
		    int colIndex = columnAtPoint(p);
		    int rowIndex = rowAtPoint(p);
		    int realColumnIndex = convertColumnIndexToModel(colIndex);
		    int realRowIndex = convertRowIndexToModel(rowIndex);
		    //return "(" + realRowIndex + "," + realColumnIndex + ")";
		    return pm.getToolTip(realRowIndex, realColumnIndex);
		    //return pm.getValueAt(realRowIndex, realColumnIndex).toString();
		}
		
	    };
	    
	table.getTableHeader().setForeground(Color.blue);

	table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);

	JTable rowTable = new RowNumberTable(table);
	JPanel content = new JPanel();
	content.setLayout(new BorderLayout());
	header = new JTextArea("Welcome!\n\nBAMseek allows you to scroll through large SAM/BAM alignment files.  Please go to \'File > Open File ...' to get started.\n\nFor updates, visit http://code.google.com/p/bamseek/");
	header.setEditable(false);
	JScrollPane scrollHeader = new JScrollPane(header);
	JScrollPane scrollTable = new JScrollPane(table);
	scrollTable.setRowHeaderView(rowTable);
	scrollTable.setCorner(JScrollPane.UPPER_LEFT_CORNER, rowTable.getTableHeader());
	content.add(scrollHeader, BorderLayout.CENTER);

	ImageIcon icon = createImageIcon("images/BAMseek.png", "bamicon");
	JLabel pictLabel = new JLabel(icon);
	content.add(pictLabel, BorderLayout.WEST);
	
	jsp = new JSplitPane(JSplitPane.VERTICAL_SPLIT, content, scrollTable);
	getContentPane().add(jsp, BorderLayout.CENTER);
	
	pack();
	setVisible(true);
    }

    protected ImageIcon createImageIcon(String path,
					String description) {
	java.net.URL imgURL = getClass().getResource(path);
	if (imgURL != null) {
	    return new ImageIcon(imgURL, description);
	} else {
	    System.err.println("Couldn't find file: " + path);
	    return null;
	}
    }

    private void initMenu(){
	JMenuBar menuBar = new JMenuBar();
	
	JMenu fileMenu = new JMenu("File");
	menuBar.add(fileMenu);
	JMenuItem item = new JMenuItem("Open File...");
	item.addActionListener(new OpenAction());
	fileMenu.add(item);
	
	JMenu helpMenu = new JMenu("Help");
	menuBar.add(helpMenu);
	
	/**
	JMenuItem bam_item = new JMenuItem("Open example BAM file");
	bam_item.addActionListener(new OpenBAM("examples/ex1.bam"));
	helpMenu.add(bam_item);
	JMenuItem sam_item = new JMenuItem("Open example SAM file");
	sam_item.addActionListener(new OpenBAM("examples/ex1.sam"));
	helpMenu.add(sam_item);
	helpMenu.addSeparator();
	**/

	JMenuItem splash = new JMenuItem("Show splash screen");
	splash.addActionListener(new ShowSplash());
	helpMenu.add(splash);

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
	    spinmodel = new SpinnerNumberModel(1, 1, Math.max(1, pm.numPages()), 1);
	    spin = new JSpinner(spinmodel);

	    spinmodel.addChangeListener(new ChangeListener() {
		    public void stateChanged(ChangeEvent e) {
			page_no = spinmodel.getNumber().intValue();
			slide.setValue(page_no);
		    }
		});
	    add(spin);//, BorderLayout.WEST);
	    numpages = new Label(" / " + pm.numPages());
	    add(numpages);
	    
	    slide = new JSlider(JSlider.HORIZONTAL, 1, Math.max(1, pm.numPages()), 1);
	    slidemodel = slide.getModel();
	    slidemodel.addChangeListener(new ChangeListener() {
		    public void stateChanged(ChangeEvent e) {
			page_no = slidemodel.getValue();
			spin.setValue(new Integer(page_no));
		    }
		});

	    add(slide);//, BorderLayout.CENTER);
	    slide.addChangeListener(this);
	    //slide.setValue(slide.getMinimum());
	}

	//change labels
	public void stateChanged(ChangeEvent e){

	    JSlider event = (JSlider)(e.getSource());
	    if(!event.getValueIsAdjusting()){
		pm.jumpToPage(page_no);
	    }
	}
    }

    class ShowSplash implements ActionListener {
	public void actionPerformed(ActionEvent ae){
	    JFrame splash = new JFrame("About BAMseek");
	    JPanel panel = new JPanel();
	    ImageIcon icon = createImageIcon("images/logo.png", "logo");
	    JLabel pictLabel = new JLabel(icon);
	    panel.add(pictLabel);
	    splash.getContentPane().add(panel);
	    
	    splash.pack();
	    splash.setVisible(true);
	}
    }

    class OpenBAM implements ActionListener {
	String pathname = "";
	OpenBAM(String filename){
	    super();
	    try{
		pathname = (new File(filename)).getCanonicalPath();
	    }catch(IOException e){
		e.printStackTrace();
	    }
	}

	public void actionPerformed(ActionEvent ae){
	    if(pm == null || pm.filename.equals("") || pm.getHeader() == null){
		openData(pathname);
	    }else{
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
			    BAMwindow bw = new BAMwindow();
			    bw.openData(pathname);
			}
		    });
	    }
	}
    }

    class OpenAction implements ActionListener {
	public void actionPerformed(ActionEvent ae){
	    
	    JFileChooser choose;
	    if(pwd == null){
		choose = new JFileChooser();
	    }else{
		choose = new JFileChooser(pwd);
	    }
	    if(choose.showOpenDialog(BAMwindow.this) == JFileChooser.APPROVE_OPTION){
		
		try {

		    final String pathname = choose.getSelectedFile().getCanonicalPath();
		    pwd = pathname;
		    if(pm == null || pm.filename.equals("") || pm.getHeader() == null){
			openData(pathname);
		    }else{
			javax.swing.SwingUtilities.invokeLater(new Runnable() {
				public void run() {
				    BAMwindow bw = new BAMwindow();
				    bw.openData(pathname);
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
    public int col_sizes[] = null;
    public String filename = "";
    protected PageReader pr = null;
    protected int column_count = 0;
    
    public PagingModel(String filename){
	this.filename = filename;
	if(filename.equals("")) return;
	pr = new PageReader(filename);
    }

    public String getColumnName(int column){
	return pr.getColumnName(column);
    }

    public boolean update(){
	if(filename.equals("")) return false;
	return pr.update();
    }
    public int progress(){
	if(filename.equals("")) return 100;
	return pr.progress();
    }
    
    public void finish(){
	pr.finish();
	jumpToPage(1);

	col_sizes = new int[pr.getNumColumnLabels()];
	for(int i = 0; i < col_sizes.length; i++){
	    //col_sizes[i] = pr.getNumColumnLabels();
	    col_sizes[i] = pr.getColumnName(i).length();
	}

	for(int r = 0; r < data.size(); r++){
	    for(int c = 0; c < Math.min(col_sizes.length, data.get(r).length); c++){
		
		if(data.get(r)[c].length() > col_sizes[c]){
		    col_sizes[c] = data.get(r)[c].length();
		}
	    }
	}
    }
    
    public Object getValueAt(int row, int col) {
    	if(data.get(row).length <= col) return "";
        return data.get(row)[col];
    }

    public String getToolTip(int row, int col) {
	String value = getValueAt(row, col).toString();
	String next_value = "";
	if(getColumnCount() > col + 1){
	    next_value = getValueAt(row, col+1).toString();
	}
	return pr.getToolTip(value, row, col, next_value);
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
	
        if(pr == null || pr.invalid){
	    return null;
	}
	return pr.getHeader();
    }
	
}
