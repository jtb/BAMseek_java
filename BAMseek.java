import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.io.*;

public class BAMseek {


    
    public static void main(String args[]) {
	if (System.getProperty("os.name").equals("Mac OS X")){
	    System.setProperty("apple.laf.useScreenMenuBar", "true");
	    try{
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
	    }catch(Exception e){}
	}
	
	BAMwindow bw = new BAMwindow("");
	bw.pack();
	bw.setVisible(true);
    }
}
