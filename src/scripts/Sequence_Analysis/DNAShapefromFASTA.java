package scripts.Sequence_Analysis;

import java.awt.BorderLayout;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;

import charts.CompositePlot;
import util.DNAShapeReference;

@SuppressWarnings("serial")
public class DNAShapefromFASTA extends JFrame {
	private File OUTPUTPATH = null;
	private boolean[] OUTPUT_TYPE = null;
	private ArrayList<File> FASTA = null;

	private PrintStream OUT_M = null;
	private PrintStream OUT_P = null;
	private PrintStream OUT_H = null;
	private PrintStream OUT_R = null;
	
	static Map<String, List<Double>> STRUCTURE = null;
	
	final JLayeredPane layeredPane;
	final JTabbedPane tabbedPane;
	final JTabbedPane tabbedPane_Scatterplot;
	final JTabbedPane tabbedPane_Statistics;
	
	public DNAShapefromFASTA(ArrayList<File> b, File out, boolean[] type) {
		setTitle("DNA Shape Prediction Composite");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(150, 150, 800, 600);
		
		layeredPane = new JLayeredPane();
		getContentPane().add(layeredPane, BorderLayout.CENTER);
		SpringLayout sl_layeredPane = new SpringLayout();
		layeredPane.setLayout(sl_layeredPane);
		
		tabbedPane = new JTabbedPane(JTabbedPane.TOP);
		sl_layeredPane.putConstraint(SpringLayout.NORTH, tabbedPane, 6, SpringLayout.NORTH, layeredPane);
		sl_layeredPane.putConstraint(SpringLayout.WEST, tabbedPane, 6, SpringLayout.WEST, layeredPane);
		sl_layeredPane.putConstraint(SpringLayout.SOUTH, tabbedPane, -6, SpringLayout.SOUTH, layeredPane);
		sl_layeredPane.putConstraint(SpringLayout.EAST, tabbedPane, -6, SpringLayout.EAST, layeredPane);
		layeredPane.add(tabbedPane);
		
		tabbedPane_Scatterplot = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.addTab("DNA Shape Plot", null, tabbedPane_Scatterplot, null);
		
		tabbedPane_Statistics = new JTabbedPane(JTabbedPane.TOP);
		tabbedPane.addTab("DNA Shape Statistics", null, tabbedPane_Statistics, null);
		
		FASTA = b;
		OUTPUTPATH = out;
		OUTPUT_TYPE = type;
	}
	
	public void run() throws IOException, InterruptedException {
		STRUCTURE = DNAShapeReference.InitializeStructure();

		for(int x = 0; x < FASTA.size(); x++) {
			String NAME = FASTA.get(x).getName().split("\\.")[0];
			String time = getTimeStamp(); //Generate TimeStamp
			JTextArea STATS_MGW = null;
			JTextArea STATS_PropT = null;
			JTextArea STATS_HelT = null;
			JTextArea STATS_Roll = null;
			if(OUTPUT_TYPE[0]) {
				STATS_MGW = new JTextArea();
				STATS_MGW.setEditable(false);
				STATS_MGW.append(time + "\n" + NAME + "\n");	
			}
			if(OUTPUT_TYPE[1]) {
				STATS_PropT = new JTextArea();
				STATS_PropT.setEditable(false);
				STATS_PropT.append(time + "\n" + NAME + "\n");	
			}
			if(OUTPUT_TYPE[2]) {
				STATS_HelT = new JTextArea();
				STATS_HelT.setEditable(false);
				STATS_HelT.append(time + "\n" + NAME + "\n");	
			}
			if(OUTPUT_TYPE[3]) {
				STATS_Roll = new JTextArea();
				STATS_Roll.setEditable(false);
				STATS_Roll.append(time + "\n" + NAME + "\n");	
			}
			openOutputFiles(x);
			
			double[] AVG_MGW = null;
			double[] AVG_PropT = null;
			double[] AVG_HelT = null;
			double[] AVG_Roll = null;
			
			Scanner scan = new Scanner(FASTA.get(x));
			int counter = 0;
			while (scan.hasNextLine()) {
				String HEADER = scan.nextLine();
				if(HEADER.contains(">")) {
					HEADER = HEADER.substring(1, HEADER.length());
					String seq = scan.nextLine();
					if(!seq.contains("N")) {
						//Populate array for each FASTA line
						List<Double> MGW = new ArrayList<Double>();
						List<Double> PropT = new ArrayList<Double>();
						List<Double> HelT = new ArrayList<Double>();
						List<Double> Roll = new ArrayList<Double>();
						for(int z = 0; z < seq.length() - 4; z++) {
							String key = seq.substring(z, z + 5);
							List<Double> SCORES = STRUCTURE.get(key);
							if(OUTPUT_TYPE[0]) { MGW.add(SCORES.get(0)); }
							if(OUTPUT_TYPE[1]) { PropT.add(SCORES.get(1)); }
							if(OUTPUT_TYPE[2]) { 
								if(z == 0) {
									HelT.add(SCORES.get(2));
									HelT.add(SCORES.get(3));
								} else {
									HelT.set(HelT.size() - 1, (HelT.get(HelT.size() - 1) + SCORES.get(2)) / 2);
									HelT.add(SCORES.get(3));
								}
							}
							if(OUTPUT_TYPE[3]) {
								if(z == 0) {
									Roll.add(SCORES.get(4));
									Roll.add(SCORES.get(5));
								} else {
									Roll.set(Roll.size() - 1, (Roll.get(Roll.size() - 1) + SCORES.get(4)) / 2);
									Roll.add(SCORES.get(5));
								}
							}
						}
						
						if(OUTPUT_TYPE[0]) {
							if(counter == 0) {
								OUT_M.print("YORF\tNAME");
								for(int z = 0; z < MGW.size(); z++) { OUT_M.print("\t" + z); }
								OUT_M.println();
								AVG_MGW = new double[MGW.size()];
							}
							OUT_M.print(HEADER + "\t" + HEADER);
							for(int z = 0; z < MGW.size(); z++) {
								OUT_M.print("\t" + MGW.get(z));	
								AVG_MGW[z] += MGW.get(z);
							}
							OUT_M.println();
						}
						if(OUTPUT_TYPE[1]) {
							if(counter == 0) {
								OUT_P.print("YORF\tNAME");
								for(int z = 0; z < PropT.size(); z++) { OUT_P.print("\t" + z); }
								OUT_P.println();
								AVG_PropT = new double[PropT.size()];
							}
							OUT_P.print(HEADER + "\t" + HEADER);
							for(int z = 0; z < PropT.size(); z++) {
								OUT_P.print("\t" + PropT.get(z));
								AVG_PropT[z] += PropT.get(z);
							}
							OUT_P.println();
						}
						if(OUTPUT_TYPE[2]) {
							if(counter == 0) {
								OUT_H.print("YORF\tNAME");
								for(int z = 0; z < HelT.size(); z++) { OUT_H.print("\t" + z); }
								OUT_H.println();
								AVG_HelT = new double[HelT.size()];
							}
							OUT_H.print(HEADER + "\t" + HEADER);
							for(int z = 0; z < HelT.size(); z++) {
								OUT_H.print("\t" + HelT.get(z));	
								AVG_HelT[z] += HelT.get(z);
							}
							OUT_H.println();
						}
						if(OUTPUT_TYPE[3]) {
							if(counter == 0) {
								OUT_R.print("YORF\tNAME");
								for(int z = 0; z < Roll.size(); z++) { OUT_R.print("\t" + z); }
								OUT_R.println();
								AVG_Roll = new double[Roll.size()];
							}
							OUT_R.print(HEADER + "\t" + HEADER);
							for(int z = 0; z < Roll.size(); z++) {
								OUT_R.print("\t" + Roll.get(z));
								AVG_Roll[z] += Roll.get(z);
							}
							OUT_R.println();
						}
					}
					counter++;
				} else {
					System.out.println("ERROR: Invalid FASTA sequence\n" + HEADER);
				}
			}
			scan.close();
			
			//Convert average and statistics to output tabs panes
			if(OUTPUT_TYPE[0]) {
				OUT_M.close();
				double[] DOMAIN_MGW = new double[AVG_MGW.length];
				int temp = (int) (((double)AVG_MGW.length / 2.0) + 0.5);
				for(int z = 0; z < AVG_MGW.length; z++) {
					DOMAIN_MGW[z] = (double)(temp - (AVG_MGW.length - z));
					AVG_MGW[z] /= counter;
					STATS_MGW.append(DOMAIN_MGW[z] + "\t" + AVG_MGW[z] + "\n");
				}
				tabbedPane_Scatterplot.add("MGW", CompositePlot.createCompositePlot(DOMAIN_MGW, AVG_MGW, NAME + " MGW"));
				STATS_MGW.setCaretPosition(0);
				JScrollPane MGWpane = new JScrollPane(STATS_MGW, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				tabbedPane_Statistics.add("MGW", MGWpane);
			}
			if(OUTPUT_TYPE[1]) {
				OUT_P.close();
				double[] DOMAIN_PropT = new double[AVG_PropT.length];
				int temp = (int) (((double)AVG_PropT.length / 2.0) + 0.5);
				for(int z = 0; z < AVG_PropT.length; z++) {
					DOMAIN_PropT[z] = (double)(temp - (AVG_PropT.length - z));
					AVG_PropT[z] /= counter;
					STATS_PropT.append(DOMAIN_PropT[z] + "\t" + AVG_PropT[z] + "\n");
				}
				tabbedPane_Scatterplot.add("Propeller Twist", CompositePlot.createCompositePlot(DOMAIN_PropT, AVG_PropT, NAME + " PropT"));
				STATS_PropT.setCaretPosition(0);
				JScrollPane PropTpane = new JScrollPane(STATS_PropT, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				tabbedPane_Statistics.add("PropT", PropTpane);
			}
			if(OUTPUT_TYPE[2]) {
				OUT_H.close();
				double[] DOMAIN_HelT = new double[AVG_HelT.length];
				int temp = (int) (((double)AVG_HelT.length / 2.0) + 0.5);
				for(int z = 0; z < AVG_HelT.length; z++) {
					DOMAIN_HelT[z] = (double)(temp - (AVG_HelT.length - z));
					AVG_HelT[z] /= counter;
					STATS_HelT.append(DOMAIN_HelT[z] + "\t" + AVG_HelT[z] + "\n");
				}
				tabbedPane_Scatterplot.add("Helical Twist", CompositePlot.createCompositePlot(DOMAIN_HelT, AVG_HelT, NAME + " HelT"));
				STATS_HelT.setCaretPosition(0);
				JScrollPane HelTpane = new JScrollPane(STATS_HelT, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				tabbedPane_Statistics.add("HelT", HelTpane);
			}
			if(OUTPUT_TYPE[3]) {
				OUT_R.close();
				double[] DOMAIN_Roll = new double[AVG_Roll.length];
				int temp = (int) (((double)AVG_Roll.length / 2.0) + 0.5);
				for(int z = 0; z < AVG_Roll.length; z++) {
					DOMAIN_Roll[z] = (double)(temp - (AVG_Roll.length - z));
					AVG_Roll[z] /= counter;
					STATS_Roll.append(DOMAIN_Roll[z] + "\t" + AVG_Roll[z] + "\n");
				}
				tabbedPane_Scatterplot.add("Roll", CompositePlot.createCompositePlot(DOMAIN_Roll, AVG_Roll, NAME + " Roll"));
				STATS_Roll.setCaretPosition(0);
				JScrollPane Rollpane = new JScrollPane(STATS_Roll, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
				tabbedPane_Statistics.add("Roll", Rollpane);
			}
			firePropertyChange("fa",x, x + 1);	
		}
	}
		
    
    private void openOutputFiles(int index) {
		String NAME = FASTA.get(index).getName().split("\\.")[0];
    	//Open Output File
		if(OUTPUTPATH != null) {
			try {
				if(OUTPUT_TYPE[0]) { OUT_M = new PrintStream(new File(OUTPUTPATH.getCanonicalPath() + File.separator + NAME + "_MGW.cdt")); }
				if(OUTPUT_TYPE[1]) { OUT_P = new PrintStream(new File(OUTPUTPATH.getCanonicalPath() + File.separator + NAME + "_PTwist.cdt")); }
				if(OUTPUT_TYPE[2]) { OUT_H = new PrintStream(new File(OUTPUTPATH.getCanonicalPath() + File.separator + NAME + "_HTwist.cdt")); }
				if(OUTPUT_TYPE[3]) { OUT_R = new PrintStream(new File(OUTPUTPATH.getCanonicalPath() + File.separator + NAME + "_Roll.cdt")); }
			} catch (FileNotFoundException e) { e.printStackTrace(); }
			catch (IOException e) {	e.printStackTrace(); }
		} else {
			try {
				if(OUTPUT_TYPE[0]) { OUT_M = new PrintStream(new File(NAME + "_MGW.cdt")); }
				if(OUTPUT_TYPE[1]) { OUT_P = new PrintStream(new File(NAME + "_PropT.cdt")); }
				if(OUTPUT_TYPE[2]) { OUT_H = new PrintStream(new File(NAME + "_HelT.cdt")); }
				if(OUTPUT_TYPE[3]) { OUT_R = new PrintStream(new File(NAME + "_Roll.cdt")); }
			} catch (FileNotFoundException e) { e.printStackTrace(); }
		}
    }
    
	private static String getTimeStamp() {
		Date date= new Date();
		String time = new Timestamp(date.getTime()).toString();
		return time;
	}       
}