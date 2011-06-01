import java.util.HashMap;
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
import java.util.regex.Pattern;

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
	super("Welcome to BAMseek");
	
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
	header = new JTextArea("BAMseek allows you to scroll through large SAM/BAM alignment files.  Please go to \'File > Open\' File to get started.");
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
    public int col_sizes[] = new int[11];
    public String filename = "";
    protected PageReader pr = null;
    protected int column_count = 0;
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

    public PagingModel(String filename){
	initTags();
	this.filename = filename;
	if(filename.equals("")) return;
	pr = new PageReader(filename);
    }

    public String getColumnName(int column){
	if(column >= col_names.length){
	    return "Tag";
	}
	if(column < 0) return "Unknown";
	return col_names[column];
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

	
	for(int i = 0; i < col_sizes.length; i++){
	    col_sizes[i] = col_names[i].length();
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
	String ans = getValueAt(row, col).toString();
	
	if(col == 1){//Flag
	    int n = Integer.parseInt(ans);
	    return prettyPrintFlag(n);
	}
	if(col == 5){//Cigar
	    return prettyPrintCigar(ans);
	}
	if(col == 9 && getColumnCount() > 10){//BaseQual
	    return prettyPrintBaseQual(ans, getValueAt(row, 10).toString());
	}
	if(col > 10){//Tag
	    return prettyPrintTag(ans);
	}

	return ans;
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

    private String prettyPrintFlag(int flag){
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

    private String prettyPrintCigar(String cigar){
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
    
    private String prettyPrintBaseQual(String bases, String quals){
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

    private String prettyPrintTag(String tag){
	String[] fields = tag.split(":");
	if(fields.length < 3) return tag;
	String ans = "";
	
	String descript = null;
	if((descript = tagmap.get(fields[0])) == null){
	    descript = fields[0];
	}
	return (descript + ": " + fields[2]);
    }

    private void initTags(){
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
	
}
