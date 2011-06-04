import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;

public class BAMseek {


    
    public static void main(String args[]) {
	if (System.getProperty("os.name").equals("Mac OS X")){
	    System.setProperty("apple.laf.useScreenMenuBar", "true");
	}
	try{
	    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	}catch(Exception e){}
	
	javax.swing.SwingUtilities.invokeLater(new Runnable() {
		public void run() {
		    ToolTipManager.sharedInstance().setDismissDelay(60000);
		    BAMwindow bw = new BAMwindow();
		}
	    });
    }
}
