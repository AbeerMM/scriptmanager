package scripts.Data_Analysis;

import htsjdk.samtools.SAMException;
import htsjdk.samtools.reference.IndexedFastaSequenceFile;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import objects.BEDCoord;
import util.LineReader;

/*
 * Adapted from:
 * https://github.com/mdshw5/pyfaidx/blob/master/pyfaidx/__init__.py
 * pyfaidx python program for manipulating fasta files efficiently
 */

@SuppressWarnings("serial")
public class DNAShapePrediction extends JFrame {
	private File GENOME = null;
	private File OUTPUTPATH = null;
	private boolean[] OUTPUT_TYPE = null;
	private ArrayList<File> BED = null;

	private boolean STRAND = true;
	private boolean INDEX = true;
	
	private PrintStream OUT_M = null;
	private PrintStream OUT_P = null;
	private PrintStream OUT_H = null;
	private PrintStream OUT_R = null;
	
	static Map<String, List<Double>> STRUCTURE = new HashMap<String, List<Double>>();
	
	private JTextArea textArea;
	
	public DNAShapePrediction(File gen, ArrayList<File> b, File out, boolean[] type, boolean str) {
		setTitle("FASTA Extraction Progress");
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setBounds(150, 150, 600, 800);
		
		JScrollPane scrollPane = new JScrollPane();
		getContentPane().add(scrollPane, BorderLayout.CENTER);
		
		textArea = new JTextArea();
		textArea.setEditable(false);
		scrollPane.setViewportView(textArea);
		
		GENOME = gen;
		BED = b;
		OUTPUTPATH = out;
		OUTPUT_TYPE = type;
		STRAND = str;
	}
	
	public void run() throws IOException, InterruptedException {
		File FAI = new File(GENOME + ".fai");
		//Check if FAI index file exists
		if(!FAI.exists() || FAI.isDirectory()) {
			textArea.append("FASTA Index file not found.\nGenerating new one...\n");
			INDEX = buildFASTAIndex(GENOME);
		}		
		if(INDEX) {
			try{
				IndexedFastaSequenceFile QUERY = new IndexedFastaSequenceFile(GENOME);
				InitializeStructure();
				
				for(int x = 0; x < BED.size(); x++) {
					textArea.append("Proccessing File: " + BED.get(x).getName() + "\n");
					//Open Output File
					String NAME = BED.get(x).getName().split("\\.")[0];
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
					ArrayList<BEDCoord> BED_Coord = loadCoord(BED.get(x));
					
					for(int y = 0; y < BED_Coord.size(); y++) {
						try {
							String seq = new String(QUERY.getSubsequenceAt(BED_Coord.get(y).getChrom(), BED_Coord.get(y).getStart() + 1, BED_Coord.get(y).getStop()).getBases());
							if(STRAND && BED_Coord.get(y).getDir().equals("-")) {
								seq = RevComplement(seq);
							}
							
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
								if(y == 0) {
									OUT_M.print("YORF\tNAME");
									for(int z = 0; z < MGW.size(); z++) { OUT_M.print("\t" + z); }
									OUT_M.println();
								}
								OUT_M.print(BED_Coord.get(y).getName() + "\t" + BED_Coord.get(y).getName());
								for(int z = 0; z < MGW.size(); z++) {
									OUT_M.print("\t" + MGW.get(z));							
								}
								OUT_M.println();
							}
							if(OUTPUT_TYPE[1]) {
								if(y == 0) {
									OUT_P.print("YORF\tNAME");
									for(int z = 0; z < PropT.size(); z++) { OUT_P.print("\t" + z); }
									OUT_P.println();
								}
								OUT_P.print(BED_Coord.get(y).getName() + "\t" + BED_Coord.get(y).getName());
								for(int z = 0; z < PropT.size(); z++) {
									OUT_P.print("\t" + PropT.get(z));
								}
								OUT_P.println();
							}
							if(OUTPUT_TYPE[2]) {
								if(y == 0) {
									OUT_H.print("YORF\tNAME");
									for(int z = 0; z < HelT.size(); z++) { OUT_H.print("\t" + z); }
									OUT_H.println();
								}
								OUT_H.print(BED_Coord.get(y).getName() + "\t" + BED_Coord.get(y).getName());
								for(int z = 0; z < HelT.size(); z++) {
									OUT_H.print("\t" + HelT.get(z));							
								}
								OUT_H.println();
							}
							if(OUTPUT_TYPE[3]) {
								if(y == 0) {
									OUT_R.print("YORF\tNAME");
									for(int z = 0; z < Roll.size(); z++) { OUT_R.print("\t" + z); }
									OUT_R.println();
								}
								OUT_R.print(BED_Coord.get(y).getName() + "\t" + BED_Coord.get(y).getName());
								for(int z = 0; z < Roll.size(); z++) {
									OUT_R.print("\t" + Roll.get(z));
								}
								OUT_R.println();
							}
										
						} catch (SAMException e) {
							textArea.append("INVALID COORDINATE: " + BED_Coord.get(y).toString() + "\n");
						}
					}
					if(OUT_M != null) { OUT_M.close(); }
					if(OUT_P != null) { OUT_P.close(); }
					if(OUT_H != null) { OUT_H.close(); }
					if(OUT_R != null) { OUT_R.close(); }
			        firePropertyChange("fa",x, x + 1);	
				}
				QUERY.close();
				textArea.append("Extraction Complete\n");
			} catch(IllegalArgumentException e) {
				textArea.append(e.getMessage());
			} catch(FileNotFoundException e) {
				textArea.append(e.getMessage());
			} catch(SAMException e) {
				textArea.append(e.getMessage());
			}
		} else {
			textArea.append("Genome FASTA file contains invalid lines!!!\n");
		}
	}
		
	public String RevComplement (String SEQ) {
		SEQ = SEQ.toUpperCase();
		String RC = "";
		for (int x = 0; x < SEQ.length(); x++){
			if(SEQ.charAt(x) == 'A') { RC = 'T' + RC; }
			else if(SEQ.charAt(x) == 'T') { RC = 'A' + RC; }
			else if(SEQ.charAt(x) == 'G') { RC = 'C' + RC; }
			else if(SEQ.charAt(x) == 'C') { RC = 'G' + RC; }
			else { RC = 'N' + RC; }
		}
		return RC;
	}
    public ArrayList<BEDCoord> loadCoord(File INPUT) throws FileNotFoundException {
		Scanner scan = new Scanner(INPUT);
		ArrayList<BEDCoord> COORD = new ArrayList<BEDCoord>();
		while (scan.hasNextLine()) {
			String[] temp = scan.nextLine().split("\t");
			if(temp.length > 2) { 
				if(!temp[0].contains("track") && !temp[0].contains("#")) {
					String name = "";
					if(temp.length > 3) { name = temp[3]; }
					else { name = temp[0] + "_" + temp[1] + "_" + temp[2]; }
					if(Integer.parseInt(temp[1]) >= 0) {
						if(temp[5].equals("+")) { COORD.add(new BEDCoord(temp[0], Integer.parseInt(temp[1]), Integer.parseInt(temp[2]), "+", name)); }
						else { COORD.add(new BEDCoord(temp[0], Integer.parseInt(temp[1]), Integer.parseInt(temp[2]), "-", name)); }
					} else {
						System.out.println("Invalid Coordinate in File!!!\n" + Arrays.toString(temp));
					}
				}
			}
		}
		scan.close();
		return COORD;
    }
    

    private static String getTimeStamp() {
		Date date= new Date();
		String time = new Timestamp(date.getTime()).toString();
		return time;
	}
    
    /*
     *	contig_name\tcontig_length\toffset_distance_from_last_contig\tcolumnlength\tcolumnlength_with_endline\n"
     *	chr1    230218  6       60      61 
     *	chr2    813184  234067  60      61 
     */
    public boolean buildFASTAIndex(File fasta) throws IOException {
    	textArea.append(getTimeStamp() + "\nBuilding Genome Index...\n");
    	
    	boolean properFASTA = true;
    	ArrayList<String> IMPROPER_FASTA = new ArrayList<String>();
    	int counter = 0;

    	String contig = "";
    	int binaryOffset = 0;
    	int currentOffset = 0;
    	int contigLength = 0;
    	int column_Length = 0;
    	int untrimmed_Column_Length = 0;
    	    	
    	BufferedReader b_read = new BufferedReader(new FileReader(fasta));
    	LineReader reader = new LineReader(b_read);
    	PrintStream FAI = new PrintStream(fasta.getName() + ".fai");
    	
    	String strLine = "";
    	while(!(strLine = reader.readLine()).equals("")) {
    		//Pull parameters line
    		int current_untrimmed_Column_Length = strLine.length();
			int current_column_Length = strLine.trim().length();

			if(strLine.contains(">")) {
				if(IMPROPER_FASTA.size() > 1) {
					textArea.append("Unequal column size FASTA Line at:\n");
					for(int z = 0; z < IMPROPER_FASTA.size(); z++) {	textArea.append(contig + "\t" + IMPROPER_FASTA.get(z) + "\n");	}
					properFASTA = false;
					break;
				}
				if(counter > 0) { FAI.println(contig + "\t" + contigLength + "\t" + currentOffset + "\t" + column_Length + "\t" + untrimmed_Column_Length);	}
				//Reset parameters for new contig
				untrimmed_Column_Length = 0;
				contigLength = 0;
				column_Length = 0;
				contig = strLine.trim().substring(1);
				binaryOffset += current_untrimmed_Column_Length;
				currentOffset = binaryOffset;
				IMPROPER_FASTA = new ArrayList<String>();
			} else {
				if(untrimmed_Column_Length == 0) { untrimmed_Column_Length = current_untrimmed_Column_Length; }
				if(column_Length == 0) { column_Length = current_column_Length;	}
				binaryOffset += current_untrimmed_Column_Length;
				contigLength += current_column_Length;
				
				//Check to make sure all the columns are equal. Index is invalid otherwise
				if(current_untrimmed_Column_Length != untrimmed_Column_Length || current_untrimmed_Column_Length == 0) { IMPROPER_FASTA.add(strLine.trim());	}
			}
			counter++;
    	}
		FAI.println(contig + "\t" + contigLength + "\t" + currentOffset + "\t" + column_Length + "\t" + untrimmed_Column_Length);
		b_read.close();
    	FAI.close();
    	
		if(properFASTA) textArea.append("Genome Index Built\n" + getTimeStamp() + "\n");
		else { new File(fasta.getName() + ".fai").delete(); }
		
		return properFASTA;
    }
    	
	/*	
	 *	http://rohslab.cmb.usc.edu/DNAshape/
	 *	Zhou,,T., Yang,L., Lu,Y., Dror,I., Dantas Machado,A.C., Ghane,T., Di Felice,R. and Rohs,R. (2013) DNAshape: a method for the high-throughput prediction of DNA structural features on a genomic scale. Nucleic Acids Res., 41, W56-W62.
	 */
	public static void InitializeStructure() {
		/*
		#Seq	MGW	PropT	HelT-1	HelT-2	Roll-1	Roll-2
		*/
		STRUCTURE.put("AAAAA", new ArrayList<Double>() {{add(3.38); add(-16.51); add(37.74); add(38.01); add(-5.05); add(-5.09); }} );
		STRUCTURE.put("AAAAT", new ArrayList<Double>() {{add(3.63); add(-14.89); add(36.93); add(37.68); add(-3.56); add(-5.12); }} );
		STRUCTURE.put("AAAAG", new ArrayList<Double>() {{add(3.68); add(-14.68); add(37.02); add(37.18); add(-4.23); add(-6.47); }} );
		STRUCTURE.put("AAAAC", new ArrayList<Double>() {{add(4.05); add(-14.47); add(37.13); add(36.95); add(-3.62); add(-4.8); }} );
		STRUCTURE.put("AAATA", new ArrayList<Double>() {{add(3.79); add(-12.63); add(37.22); add(33.85); add(-3.91); add(-6.42); }} );
		STRUCTURE.put("AAATT", new ArrayList<Double>() {{add(2.85); add(-14.95); add(37.52); add(35.11); add(-5.0); add(-8.57); }} );
		STRUCTURE.put("AAATG", new ArrayList<Double>() {{add(3.84); add(-11.76); add(36.86); add(33.42); add(-4.21); add(-7.0); }} );
		STRUCTURE.put("AAATC", new ArrayList<Double>() {{add(4.12); add(-12.27); add(36.55); add(33.42); add(-2.43); add(-5.36); }} );
		STRUCTURE.put("AAAGA", new ArrayList<Double>() {{add(4.02); add(-10.71); add(36.69); add(33.18); add(-4.76); add(-3.21); }} );
		STRUCTURE.put("AAAGT", new ArrayList<Double>() {{add(3.35); add(-11.68); add(36.93); add(33.88); add(-6.36); add(-4.78); }} );
		STRUCTURE.put("AAAGG", new ArrayList<Double>() {{add(4.05); add(-9.21); add(35.88); add(32.79); add(-4.8); add(-5.06); }} );
		STRUCTURE.put("AAAGC", new ArrayList<Double>() {{add(4.03); add(-10.58); add(36.56); add(32.81); add(-5.07); add(-3.95); }} );
		STRUCTURE.put("AAACA", new ArrayList<Double>() {{add(4.65); add(-13.05); add(36.59); add(35.22); add(-2.98); add(-2.89); }} );
		STRUCTURE.put("AAACT", new ArrayList<Double>() {{add(3.85); add(-12.78); add(35.86); add(36.86); add(-4.3); add(-5.49); }} );
		STRUCTURE.put("AAACG", new ArrayList<Double>() {{add(4.43); add(-13.15); add(36.57); add(35.48); add(-2.94); add(-3.27); }} );
		STRUCTURE.put("AAACC", new ArrayList<Double>() {{add(4.06); add(-12.41); add(36.34); add(36.2); add(-3.75); add(-4.65); }} );
		STRUCTURE.put("AATAA", new ArrayList<Double>() {{add(5.53); add(-9.65); add(32.44); add(34.83); add(-3.91); add(8.21); }} );
		STRUCTURE.put("AATAT", new ArrayList<Double>() {{add(4.8); add(-10.43); add(33.17); add(35.25); add(-5.48); add(6.33); }} );
		STRUCTURE.put("AATAG", new ArrayList<Double>() {{add(4.65); add(-11.61); add(33.54); add(35.89); add(-5.98); add(3.47); }} );
		STRUCTURE.put("AATAC", new ArrayList<Double>() {{add(5.3); add(-10.45); add(32.78); add(34.79); add(-4.27); add(6.24); }} );
		STRUCTURE.put("AATTA", new ArrayList<Double>() {{add(4.36); add(-12.2); add(33.31); add(35.92); add(-5.21); add(-2.38); }} );
		STRUCTURE.put("AATTT", new ArrayList<Double>() {{add(2.85); add(-14.95); add(35.11); add(37.52); add(-8.57); add(-5.0); }} );
		STRUCTURE.put("AATTG", new ArrayList<Double>() {{add(4.24); add(-11.93); add(33.37); add(35.62); add(-5.32); add(-2.97); }} );
		STRUCTURE.put("AATTC", new ArrayList<Double>() {{add(3.75); add(-11.18); add(33.75); add(36.17); add(-6.49); add(-3.59); }} );
		STRUCTURE.put("AATGA", new ArrayList<Double>() {{add(4.46); add(-9.02); add(32.87); add(34.98); add(-6.08); add(3.8); }} );
		STRUCTURE.put("AATGT", new ArrayList<Double>() {{add(4.27); add(-10.82); add(33.25); add(36.13); add(-6.17); add(4.78); }} );
		STRUCTURE.put("AATGG", new ArrayList<Double>() {{add(5.08); add(-8.98); add(32.12); add(34.87); add(-4.16); add(3.6); }} );
		STRUCTURE.put("AATGC", new ArrayList<Double>() {{add(4.89); add(-9.59); add(32.67); add(35.25); add(-5.22); add(3.59); }} );
		STRUCTURE.put("AATCA", new ArrayList<Double>() {{add(4.46); add(-9.91); add(33.01); add(36.14); add(-4.75); add(-1.08); }} );
		STRUCTURE.put("AATCT", new ArrayList<Double>() {{add(3.75); add(-10.31); add(33.3); add(37.53); add(-6.11); add(-2.71); }} );
		STRUCTURE.put("AATCG", new ArrayList<Double>() {{add(4.56); add(-9.51); add(32.89); add(36.19); add(-4.9); add(-0.8); }} );
		STRUCTURE.put("AATCC", new ArrayList<Double>() {{add(4.19); add(-9.67); add(33.31); add(36.57); add(-5.81); add(-0.9); }} );
		STRUCTURE.put("AAGAA", new ArrayList<Double>() {{add(4.8); add(-3.46); add(32.3); add(36.52); add(-1.79); add(-0.63); }} );
		STRUCTURE.put("AAGAT", new ArrayList<Double>() {{add(3.9); add(-3.55); add(32.88); add(37.56); add(-3.96); add(-3.29); }} );
		STRUCTURE.put("AAGAG", new ArrayList<Double>() {{add(4.68); add(-3.22); add(32.31); add(35.92); add(-1.76); add(-1.36); }} );
		STRUCTURE.put("AAGAC", new ArrayList<Double>() {{add(4.65); add(-3.82); add(32.56); add(36.4); add(-2.15); add(-2.37); }} );
		STRUCTURE.put("AAGTA", new ArrayList<Double>() {{add(4.03); add(-4.26); add(33.24); add(35.21); add(-3.52); add(-4.72); }} );
		STRUCTURE.put("AAGTT", new ArrayList<Double>() {{add(3.34); add(-6.26); add(33.58); add(36.3); add(-3.62); add(-5.19); }} );
		STRUCTURE.put("AAGTG", new ArrayList<Double>() {{add(4.14); add(-4.48); add(33.1); add(34.91); add(-2.79); add(-4.51); }} );
		STRUCTURE.put("AAGTC", new ArrayList<Double>() {{add(3.74); add(-4.07); add(33.16); add(35.44); add(-4.01); add(-5.04); }} );
		STRUCTURE.put("AAGGA", new ArrayList<Double>() {{add(4.31); add(-1.33); add(32.55); add(34.9); add(-3.38); add(-1.91); }} );
		STRUCTURE.put("AAGGT", new ArrayList<Double>() {{add(3.75); add(-1.48); add(32.53); add(35.68); add(-4.81); add(-3.27); }} );
		STRUCTURE.put("AAGGG", new ArrayList<Double>() {{add(4.42); add(-0.14); add(31.94); add(34.22); add(-3.37); add(-3.54); }} );
		STRUCTURE.put("AAGGC", new ArrayList<Double>() {{add(4.51); add(-1.5); add(32.34); add(34.57); add(-2.75); add(-2.62); }} );
		STRUCTURE.put("AAGCA", new ArrayList<Double>() {{add(4.61); add(-1.47); add(32.29); add(37.13); add(-3.05); add(-3.31); }} );
		STRUCTURE.put("AAGCT", new ArrayList<Double>() {{add(4.14); add(-1.56); add(32.14); add(38.05); add(-3.24); add(-4.35); }} );
		STRUCTURE.put("AAGCG", new ArrayList<Double>() {{add(4.63); add(-1.91); add(32.27); add(37.15); add(-2.38); add(-2.72); }} );
		STRUCTURE.put("AAGCC", new ArrayList<Double>() {{add(4.17); add(-1.55); add(32.49); add(37.74); add(-3.7); add(-4.34); }} );
		STRUCTURE.put("AACAA", new ArrayList<Double>() {{add(4.97); add(-10.17); add(34.6); add(35.64); add(-2.29); add(6.26); }} );
		STRUCTURE.put("AACAT", new ArrayList<Double>() {{add(4.3); add(-10.22); add(35.12); add(35.94); add(-3.23); add(5.76); }} );
		STRUCTURE.put("AACAG", new ArrayList<Double>() {{add(4.95); add(-9.26); add(34.88); add(34.91); add(-2.4); add(5.23); }} );
		STRUCTURE.put("AACAC", new ArrayList<Double>() {{add(5.05); add(-9.4); add(34.68); add(35.07); add(-2.42); add(4.19); }} );
		STRUCTURE.put("AACTA", new ArrayList<Double>() {{add(4.24); add(-6.07); add(35.56); add(32.1); add(-4.24); add(-3.9); }} );
		STRUCTURE.put("AACTT", new ArrayList<Double>() {{add(3.34); add(-6.26); add(36.3); add(33.58); add(-5.19); add(-3.62); }} );
		STRUCTURE.put("AACTG", new ArrayList<Double>() {{add(4.49); add(-5.42); add(35.45); add(31.73); add(-3.28); add(-1.85); }} );
		STRUCTURE.put("AACTC", new ArrayList<Double>() {{add(3.95); add(-5.23); add(36.03); add(32.41); add(-4.38); add(-3.01); }} );
		STRUCTURE.put("AACGA", new ArrayList<Double>() {{add(4.8); add(-9.62); add(34.71); add(33.66); add(-2.49); add(5.41); }} );
		STRUCTURE.put("AACGT", new ArrayList<Double>() {{add(4.21); add(-10.39); add(35.24); add(34.53); add(-3.43); add(5.64); }} );
		STRUCTURE.put("AACGG", new ArrayList<Double>() {{add(4.62); add(-10.04); add(35.14); add(33.45); add(-3.07); add(3.74); }} );
		STRUCTURE.put("AACGC", new ArrayList<Double>() {{add(4.64); add(-9.91); add(35.06); add(33.41); add(-2.93); add(4.64); }} );
		STRUCTURE.put("AACCA", new ArrayList<Double>() {{add(4.33); add(-8.86); add(35.26); add(34.89); add(-3.3); add(-0.59); }} );
		STRUCTURE.put("AACCT", new ArrayList<Double>() {{add(3.64); add(-8.15); add(35.92); add(35.61); add(-4.78); add(-2.23); }} );
		STRUCTURE.put("AACCG", new ArrayList<Double>() {{add(4.36); add(-7.97); add(35.23); add(34.48); add(-3.13); add(-0.65); }} );
		STRUCTURE.put("AACCC", new ArrayList<Double>() {{add(4.03); add(-7.68); add(35.8); add(34.89); add(-4.18); add(-1.38); }} );
		STRUCTURE.put("ATAAA", new ArrayList<Double>() {{add(5.66); add(-12.37); add(34.6); add(35.94); add(8.13); add(-0.95); }} );
		STRUCTURE.put("ATAAT", new ArrayList<Double>() {{add(5.28); add(-11.27); add(35.02); add(35.9); add(6.41); add(-2.76); }} );
		STRUCTURE.put("ATAAG", new ArrayList<Double>() {{add(5.48); add(-10.54); add(35.15); add(35.03); add(6.31); add(-3.47); }} );
		STRUCTURE.put("ATAAC", new ArrayList<Double>() {{add(5.62); add(-10.51); add(34.54); add(35.5); add(6.14); add(-3.5); }} );
		STRUCTURE.put("ATATA", new ArrayList<Double>() {{add(5.76); add(-8.95); add(34.46); add(32.12); add(8.32); add(-2.62); }} );
		STRUCTURE.put("ATATT", new ArrayList<Double>() {{add(4.8); add(-10.43); add(35.25); add(33.17); add(6.33); add(-5.48); }} );
		STRUCTURE.put("ATATG", new ArrayList<Double>() {{add(5.32); add(-8.33); add(35.37); add(32.04); add(5.85); add(-4.26); }} );
		STRUCTURE.put("ATATC", new ArrayList<Double>() {{add(5.4); add(-8.3); add(35.01); add(32.32); add(6.33); add(-3.98); }} );
		STRUCTURE.put("ATAGA", new ArrayList<Double>() {{add(5.69); add(-7.03); add(34.25); add(31.88); add(6.34); add(-1.51); }} );
		STRUCTURE.put("ATAGT", new ArrayList<Double>() {{add(4.86); add(-8.26); add(35.53); add(31.86); add(3.79); add(-3.25); }} );
		STRUCTURE.put("ATAGG", new ArrayList<Double>() {{add(5.6); add(-6.87); add(34.5); add(31.71); add(5.54); add(-2.45); }} );
		STRUCTURE.put("ATAGC", new ArrayList<Double>() {{add(5.37); add(-6.84); add(34.96); add(31.21); add(4.96); add(-2.59); }} );
		STRUCTURE.put("ATACA", new ArrayList<Double>() {{add(5.82); add(-8.93); add(34.63); add(34.26); add(5.89); add(-2.07); }} );
		STRUCTURE.put("ATACT", new ArrayList<Double>() {{add(5.37); add(-8.21); add(34.44); add(34.73); add(6.22); add(-2.71); }} );
		STRUCTURE.put("ATACG", new ArrayList<Double>() {{add(5.53); add(-9.05); add(34.59); add(34.16); add(5.89); add(-2.22); }} );
		STRUCTURE.put("ATACC", new ArrayList<Double>() {{add(5.46); add(-8.36); add(34.56); add(34.69); add(5.39); add(-2.59); }} );
		STRUCTURE.put("ATTAA", new ArrayList<Double>() {{add(5.58); add(-11.32); add(35.37); add(34.44); add(-2.12); add(8.64); }} );
		STRUCTURE.put("ATTAT", new ArrayList<Double>() {{add(5.28); add(-11.27); add(35.9); add(35.02); add(-2.76); add(6.41); }} );
		STRUCTURE.put("ATTAG", new ArrayList<Double>() {{add(5.57); add(-11.58); add(35.33); add(34.46); add(-1.62); add(7.71); }} );
		STRUCTURE.put("ATTAC", new ArrayList<Double>() {{add(5.44); add(-11.34); add(35.53); add(34.68); add(-2.51); add(5.03); }} );
		STRUCTURE.put("ATTTA", new ArrayList<Double>() {{add(4.75); add(-13.87); add(36.55); add(35.79); add(-2.36); add(-0.94); }} );
		STRUCTURE.put("ATTTT", new ArrayList<Double>() {{add(3.63); add(-14.89); add(37.68); add(36.93); add(-5.12); add(-3.56); }} );
		STRUCTURE.put("ATTTG", new ArrayList<Double>() {{add(4.12); add(-13.36); add(37.28); add(35.49); add(-4.68); add(-3.54); }} );
		STRUCTURE.put("ATTTC", new ArrayList<Double>() {{add(4.27); add(-12.09); add(36.7); add(35.77); add(-4.4); add(-3.13); }} );
		STRUCTURE.put("ATTGA", new ArrayList<Double>() {{add(5.46); add(-10.92); add(34.93); add(34.6); add(-2.1); add(7.05); }} );
		STRUCTURE.put("ATTGT", new ArrayList<Double>() {{add(5.03); add(-11.94); add(35.45); add(35.55); add(-2.43); add(6.33); }} );
		STRUCTURE.put("ATTGG", new ArrayList<Double>() {{add(5.3); add(-10.72); add(34.93); add(34.4); add(-2.07); add(5.06); }} );
		STRUCTURE.put("ATTGC", new ArrayList<Double>() {{add(5.15); add(-10.6); add(35.34); add(34.49); add(-2.92); add(3.9); }} );
		STRUCTURE.put("ATTCA", new ArrayList<Double>() {{add(5.22); add(-10.83); add(36.06); add(35.03); add(-2.45); add(0.21); }} );
		STRUCTURE.put("ATTCT", new ArrayList<Double>() {{add(4.43); add(-10.15); add(36.11); add(36.6); add(-3.86); add(-1.42); }} );
		STRUCTURE.put("ATTCG", new ArrayList<Double>() {{add(4.88); add(-10.56); add(35.86); add(35.52); add(-2.98); add(-0.24); }} );
		STRUCTURE.put("ATTCC", new ArrayList<Double>() {{add(4.69); add(-9.61); add(35.6); add(35.99); add(-3.73); add(-0.88); }} );
		STRUCTURE.put("ATGAA", new ArrayList<Double>() {{add(5.5); add(-6.07); add(35.03); add(35.39); add(5.73); add(0.04); }} );
		STRUCTURE.put("ATGAT", new ArrayList<Double>() {{add(4.79); add(-6.15); add(34.82); add(36.14); add(3.79); add(-1.32); }} );
		STRUCTURE.put("ATGAG", new ArrayList<Double>() {{add(5.23); add(-6.05); add(34.93); add(35.1); add(5.69); add(-1.52); }} );
		STRUCTURE.put("ATGAC", new ArrayList<Double>() {{add(5.45); add(-6.37); add(35.1); add(35.47); add(5.16); add(-2.06); }} );
		STRUCTURE.put("ATGTA", new ArrayList<Double>() {{add(5.27); add(-7.4); add(35.76); add(34.06); add(5.57); add(-2.14); }} );
		STRUCTURE.put("ATGTT", new ArrayList<Double>() {{add(4.3); add(-10.22); add(35.94); add(35.12); add(5.76); add(-3.23); }} );
		STRUCTURE.put("ATGTG", new ArrayList<Double>() {{add(5.15); add(-8.83); add(35.86); add(33.53); add(6.57); add(-2.22); }} );
		STRUCTURE.put("ATGTC", new ArrayList<Double>() {{add(5.02); add(-7.1); add(35.42); add(34.24); add(4.9); add(-2.83); }} );
		STRUCTURE.put("ATGGA", new ArrayList<Double>() {{add(5.34); add(-3.37); add(34.46); add(33.69); add(4.34); add(-0.26); }} );
		STRUCTURE.put("ATGGT", new ArrayList<Double>() {{add(4.97); add(-4.33); add(34.88); add(34.55); add(4.04); add(-1.22); }} );
		STRUCTURE.put("ATGGG", new ArrayList<Double>() {{add(5.19); add(-2.37); add(34.56); add(33.45); add(3.38); add(-2.51); }} );
		STRUCTURE.put("ATGGC", new ArrayList<Double>() {{add(5.23); add(-3.12); add(34.93); add(33.39); add(3.68); add(-2.18); }} );
		STRUCTURE.put("ATGCA", new ArrayList<Double>() {{add(5.55); add(-3.73); add(35.12); add(35.6); add(4.66); add(-1.2); }} );
		STRUCTURE.put("ATGCT", new ArrayList<Double>() {{add(5.23); add(-3.68); add(34.56); add(36.89); add(4.59); add(-2.2); }} );
		STRUCTURE.put("ATGCG", new ArrayList<Double>() {{add(5.6); add(-4.03); add(35.0); add(35.58); add(5.09); add(-0.76); }} );
		STRUCTURE.put("ATGCC", new ArrayList<Double>() {{add(5.33); add(-3.12); add(34.66); add(36.28); add(4.26); add(-1.93); }} );
		STRUCTURE.put("ATCAA", new ArrayList<Double>() {{add(5.52); add(-7.12); add(35.38); add(34.6); add(0.04); add(6.62); }} );
		STRUCTURE.put("ATCAT", new ArrayList<Double>() {{add(4.79); add(-6.15); add(36.14); add(34.82); add(-1.32); add(3.79); }} );
		STRUCTURE.put("ATCAG", new ArrayList<Double>() {{add(5.34); add(-6.55); add(35.8); add(34.24); add(-0.36); add(4.36); }} );
		STRUCTURE.put("ATCAC", new ArrayList<Double>() {{add(5.35); add(-6.53); add(35.64); add(34.03); add(-0.51); add(3.72); }} );
		STRUCTURE.put("ATCTA", new ArrayList<Double>() {{add(4.74); add(-3.2); add(36.79); add(31.29); add(-1.24); add(-2.21); }} );
		STRUCTURE.put("ATCTT", new ArrayList<Double>() {{add(3.9); add(-3.55); add(37.56); add(32.88); add(-3.29); add(-3.96); }} );
		STRUCTURE.put("ATCTG", new ArrayList<Double>() {{add(4.98); add(-2.7); add(36.37); add(31.12); add(-0.33); add(-2.27); }} );
		STRUCTURE.put("ATCTC", new ArrayList<Double>() {{add(4.74); add(-2.78); add(36.47); add(31.47); add(-1.46); add(-2.16); }} );
		STRUCTURE.put("ATCGA", new ArrayList<Double>() {{add(5.23); add(-7.08); add(35.76); add(32.77); add(-0.54); add(5.51); }} );
		STRUCTURE.put("ATCGT", new ArrayList<Double>() {{add(4.8); add(-6.85); add(36.22); add(33.64); add(-1.68); add(4.65); }} );
		STRUCTURE.put("ATCGG", new ArrayList<Double>() {{add(5.25); add(-7.15); add(35.6); add(32.57); add(-0.66); add(3.63); }} );
		STRUCTURE.put("ATCGC", new ArrayList<Double>() {{add(5.28); add(-6.96); add(35.82); add(32.49); add(-0.66); add(4.15); }} );
		STRUCTURE.put("ATCCA", new ArrayList<Double>() {{add(4.94); add(-5.7); add(36.24); add(33.69); add(-0.96); add(-0.54); }} );
		STRUCTURE.put("ATCCT", new ArrayList<Double>() {{add(4.51); add(-5.29); add(36.16); add(34.37); add(-1.25); add(-1.41); }} );
		STRUCTURE.put("ATCCG", new ArrayList<Double>() {{add(4.47); add(-4.49); add(36.59); add(32.93); add(-2.07); add(-2.34); }} );
		STRUCTURE.put("ATCCC", new ArrayList<Double>() {{add(4.81); add(-4.91); add(36.09); add(33.66); add(-1.49); add(-1.06); }} );
		STRUCTURE.put("AGAAA", new ArrayList<Double>() {{add(4.74); add(-10.55); add(36.43); add(36.02); add(-0.69); add(-2.61); }} );
		STRUCTURE.put("AGAAT", new ArrayList<Double>() {{add(4.43); add(-10.15); add(36.6); add(36.11); add(-1.42); add(-3.86); }} );
		STRUCTURE.put("AGAAG", new ArrayList<Double>() {{add(4.5); add(-10.3); add(36.79); add(35.47); add(-1.65); add(-4.1); }} );
		STRUCTURE.put("AGAAC", new ArrayList<Double>() {{add(4.85); add(-10.59); add(36.45); add(35.7); add(-0.56); add(-3.03); }} );
		STRUCTURE.put("AGATA", new ArrayList<Double>() {{add(4.72); add(-8.23); add(36.52); add(32.48); add(-1.03); add(-4.23); }} );
		STRUCTURE.put("AGATT", new ArrayList<Double>() {{add(3.75); add(-10.31); add(37.53); add(33.3); add(-2.71); add(-6.11); }} );
		STRUCTURE.put("AGATG", new ArrayList<Double>() {{add(4.66); add(-7.85); add(36.48); add(32.25); add(-1.27); add(-5.19); }} );
		STRUCTURE.put("AGATC", new ArrayList<Double>() {{add(4.36); add(-8.23); add(36.68); add(32.82); add(-1.69); add(-5.53); }} );
		STRUCTURE.put("AGAGA", new ArrayList<Double>() {{add(5.0); add(-6.62); add(35.8); add(31.7); add(-1.55); add(-1.87); }} );
		STRUCTURE.put("AGAGT", new ArrayList<Double>() {{add(4.86); add(-7.83); add(35.66); add(31.76); add(-0.84); add(-1.51); }} );
		STRUCTURE.put("AGAGG", new ArrayList<Double>() {{add(4.97); add(-6.38); add(35.39); add(31.44); add(-1.11); add(-2.6); }} );
		STRUCTURE.put("AGAGC", new ArrayList<Double>() {{add(5.04); add(-6.5); add(35.84); add(31.62); add(-1.41); add(-2.48); }} );
		STRUCTURE.put("AGACA", new ArrayList<Double>() {{add(4.99); add(-8.49); add(36.49); add(34.36); add(-1.88); add(-2.8); }} );
		STRUCTURE.put("AGACT", new ArrayList<Double>() {{add(4.57); add(-7.81); add(36.01); add(34.89); add(-2.0); add(-3.59); }} );
		STRUCTURE.put("AGACG", new ArrayList<Double>() {{add(4.83); add(-8.81); add(36.41); add(34.44); add(-2.02); add(-2.95); }} );
		STRUCTURE.put("AGACC", new ArrayList<Double>() {{add(4.73); add(-8.04); add(36.1); add(34.74); add(-1.9); add(-3.14); }} );
		STRUCTURE.put("AGTAA", new ArrayList<Double>() {{add(5.37); add(-7.55); add(34.75); add(34.55); add(-3.91); add(5.33); }} );
		STRUCTURE.put("AGTAT", new ArrayList<Double>() {{add(5.37); add(-8.21); add(34.73); add(34.44); add(-2.71); add(6.22); }} );
		STRUCTURE.put("AGTAG", new ArrayList<Double>() {{add(5.2); add(-8.16); add(35.18); add(34.13); add(-4.12); add(4.64); }} );
		STRUCTURE.put("AGTAC", new ArrayList<Double>() {{add(5.54); add(-8.16); add(34.74); add(33.82); add(-2.73); add(4.94); }} );
		STRUCTURE.put("AGTTA", new ArrayList<Double>() {{add(4.67); add(-10.74); add(35.6); add(34.85); add(-3.98); add(-3.3); }} );
		STRUCTURE.put("AGTTT", new ArrayList<Double>() {{add(3.85); add(-12.78); add(36.86); add(35.86); add(-5.49); add(-4.3); }} );
		STRUCTURE.put("AGTTG", new ArrayList<Double>() {{add(4.38); add(-11.29); add(36.24); add(34.76); add(-4.47); add(-3.88); }} );
		STRUCTURE.put("AGTTC", new ArrayList<Double>() {{add(4.25); add(-10.45); add(35.93); add(35.24); add(-4.54); add(-3.79); }} );
		STRUCTURE.put("AGTGA", new ArrayList<Double>() {{add(5.22); add(-7.1); add(34.23); add(33.87); add(-3.58); add(3.56); }} );
		STRUCTURE.put("AGTGT", new ArrayList<Double>() {{add(5.1); add(-8.48); add(34.67); add(34.61); add(-3.51); add(3.69); }} );
		STRUCTURE.put("AGTGG", new ArrayList<Double>() {{add(5.31); add(-7.4); add(34.38); add(34.11); add(-2.95); add(2.64); }} );
		STRUCTURE.put("AGTGC", new ArrayList<Double>() {{add(5.3); add(-7.68); add(34.58); add(33.99); add(-3.54); add(2.74); }} );
		STRUCTURE.put("AGTCA", new ArrayList<Double>() {{add(4.89); add(-8.11); add(35.06); add(35.14); add(-3.69); add(-1.77); }} );
		STRUCTURE.put("AGTCT", new ArrayList<Double>() {{add(4.57); add(-7.81); add(34.89); add(36.01); add(-3.59); add(-2.0); }} );
		STRUCTURE.put("AGTCG", new ArrayList<Double>() {{add(4.59); add(-8.15); add(35.27); add(35.38); add(-4.46); add(-2.31); }} );
		STRUCTURE.put("AGTCC", new ArrayList<Double>() {{add(4.51); add(-7.34); add(34.97); add(35.78); add(-4.26); add(-2.46); }} );
		STRUCTURE.put("AGGAA", new ArrayList<Double>() {{add(4.76); add(-4.88); add(34.39); add(36.26); add(-1.44); add(-1.3); }} );
		STRUCTURE.put("AGGAT", new ArrayList<Double>() {{add(4.51); add(-5.29); add(34.37); add(36.16); add(-1.41); add(-1.25); }} );
		STRUCTURE.put("AGGAG", new ArrayList<Double>() {{add(4.63); add(-4.81); add(34.13); add(36.23); add(-1.32); add(-2.62); }} );
		STRUCTURE.put("AGGAC", new ArrayList<Double>() {{add(4.81); add(-5.39); add(34.49); add(36.03); add(-0.96); add(-2.19); }} );
		STRUCTURE.put("AGGTA", new ArrayList<Double>() {{add(4.33); add(-5.43); add(35.24); add(34.69); add(-2.81); add(-4.49); }} );
		STRUCTURE.put("AGGTT", new ArrayList<Double>() {{add(3.64); add(-8.15); add(35.61); add(35.92); add(-2.23); add(-4.78); }} );
		STRUCTURE.put("AGGTG", new ArrayList<Double>() {{add(4.42); add(-6.64); add(35.25); add(34.31); add(-1.65); add(-3.26); }} );
		STRUCTURE.put("AGGTC", new ArrayList<Double>() {{add(4.18); add(-5.67); add(35.04); add(34.84); add(-2.46); add(-3.96); }} );
		STRUCTURE.put("AGGGA", new ArrayList<Double>() {{add(4.68); add(-1.76); add(33.86); add(33.6); add(-2.51); add(-1.61); }} );
		STRUCTURE.put("AGGGT", new ArrayList<Double>() {{add(4.29); add(-2.62); add(34.17); add(34.42); add(-3.17); add(-2.05); }} );
		STRUCTURE.put("AGGGG", new ArrayList<Double>() {{add(4.62); add(-1.32); add(33.78); add(33.61); add(-2.55); add(-2.41); }} );
		STRUCTURE.put("AGGGC", new ArrayList<Double>() {{add(4.73); add(-1.88); add(34.0); add(33.54); add(-2.27); add(-2.16); }} );
		STRUCTURE.put("AGGCA", new ArrayList<Double>() {{add(4.82); add(-2.38); add(34.03); add(36.26); add(-2.06); add(-2.41); }} );
		STRUCTURE.put("AGGCT", new ArrayList<Double>() {{add(4.33); add(-2.34); add(34.0); add(37.47); add(-2.71); add(-3.85); }} );
		STRUCTURE.put("AGGCG", new ArrayList<Double>() {{add(4.77); add(-2.82); add(34.34); add(36.31); add(-2.1); add(-2.16); }} );
		STRUCTURE.put("AGGCC", new ArrayList<Double>() {{add(4.72); add(-2.2); add(33.71); add(36.64); add(-1.89); add(-2.34); }} );
		STRUCTURE.put("AGCAA", new ArrayList<Double>() {{add(5.08); add(-3.36); add(36.94); add(34.41); add(-3.02); add(3.78); }} );
		STRUCTURE.put("AGCAT", new ArrayList<Double>() {{add(5.23); add(-3.68); add(36.89); add(34.56); add(-2.2); add(4.59); }} );
		STRUCTURE.put("AGCAG", new ArrayList<Double>() {{add(5.16); add(-3.6); add(36.97); add(33.86); add(-2.72); add(3.76); }} );
		STRUCTURE.put("AGCAC", new ArrayList<Double>() {{add(5.43); add(-3.79); add(36.67); add(34.03); add(-1.72); add(2.81); }} );
		STRUCTURE.put("AGCTA", new ArrayList<Double>() {{add(4.88); add(-1.3); add(37.56); add(31.13); add(-2.66); add(-2.32); }} );
		STRUCTURE.put("AGCTT", new ArrayList<Double>() {{add(4.14); add(-1.56); add(38.05); add(32.14); add(-4.35); add(-3.24); }} );
		STRUCTURE.put("AGCTG", new ArrayList<Double>() {{add(4.8); add(-1.38); add(37.48); add(31.01); add(-2.49); add(-2.54); }} );
		STRUCTURE.put("AGCTC", new ArrayList<Double>() {{add(4.63); add(-0.83); add(37.66); add(31.33); add(-3.25); add(-2.86); }} );
		STRUCTURE.put("AGCGA", new ArrayList<Double>() {{add(5.19); add(-3.56); add(36.83); add(32.44); add(-2.29); add(4.15); }} );
		STRUCTURE.put("AGCGT", new ArrayList<Double>() {{add(4.86); add(-3.93); add(37.02); add(33.0); add(-2.63); add(3.7); }} );
		STRUCTURE.put("AGCGG", new ArrayList<Double>() {{add(5.15); add(-3.78); add(36.72); add(32.34); add(-1.9); add(2.44); }} );
		STRUCTURE.put("AGCGC", new ArrayList<Double>() {{add(5.18); add(-3.87); add(36.76); add(32.08); add(-1.92); add(2.89); }} );
		STRUCTURE.put("AGCCA", new ArrayList<Double>() {{add(4.53); add(-2.76); add(37.62); add(33.35); add(-3.86); add(-2.44); }} );
		STRUCTURE.put("AGCCT", new ArrayList<Double>() {{add(4.33); add(-2.34); add(37.47); add(34.0); add(-3.85); add(-2.71); }} );
		STRUCTURE.put("AGCCG", new ArrayList<Double>() {{add(4.83); add(-3.04); add(37.23); add(33.27); add(-2.14); add(-1.44); }} );
		STRUCTURE.put("AGCCC", new ArrayList<Double>() {{add(4.61); add(-2.48); add(37.3); add(33.51); add(-2.95); add(-1.98); }} );
		STRUCTURE.put("ACAAA", new ArrayList<Double>() {{add(5.21); add(-12.03); add(35.41); add(35.77); add(6.09); add(-2.56); }} );
		STRUCTURE.put("ACAAT", new ArrayList<Double>() {{add(5.03); add(-11.94); add(35.55); add(35.45); add(6.33); add(-2.43); }} );
		STRUCTURE.put("ACAAG", new ArrayList<Double>() {{add(5.19); add(-10.72); add(35.43); add(35.05); add(5.65); add(-3.95); }} );
		STRUCTURE.put("ACAAC", new ArrayList<Double>() {{add(5.58); add(-11.7); add(35.22); add(35.25); add(6.2); add(-3.26); }} );
		STRUCTURE.put("ACATA", new ArrayList<Double>() {{add(5.14); add(-8.52); add(35.48); add(32.21); add(4.85); add(-4.73); }} );
		STRUCTURE.put("ACATT", new ArrayList<Double>() {{add(4.27); add(-10.82); add(36.13); add(33.25); add(4.78); add(-6.17); }} );
		STRUCTURE.put("ACATG", new ArrayList<Double>() {{add(4.99); add(-10.15); add(35.7); add(32.0); add(5.73); add(-4.67); }} );
		STRUCTURE.put("ACATC", new ArrayList<Double>() {{add(4.82); add(-8.65); add(35.37); add(32.37); add(5.15); add(-4.91); }} );
		STRUCTURE.put("ACAGA", new ArrayList<Double>() {{add(5.33); add(-7.0); add(35.0); add(31.6); add(3.72); add(-1.93); }} );
		STRUCTURE.put("ACAGT", new ArrayList<Double>() {{add(5.2); add(-8.12); add(34.72); add(31.75); add(4.57); add(-1.83); }} );
		STRUCTURE.put("ACAGG", new ArrayList<Double>() {{add(5.29); add(-6.78); add(34.61); add(31.34); add(4.68); add(-2.48); }} );
		STRUCTURE.put("ACAGC", new ArrayList<Double>() {{add(5.31); add(-7.34); add(35.03); add(31.35); add(5.07); add(-2.1); }} );
		STRUCTURE.put("ACACA", new ArrayList<Double>() {{add(5.5); add(-9.33); add(35.28); add(33.88); add(2.96); add(-2.65); }} );
		STRUCTURE.put("ACACT", new ArrayList<Double>() {{add(5.1); add(-8.48); add(34.61); add(34.67); add(3.69); add(-3.51); }} );
		STRUCTURE.put("ACACG", new ArrayList<Double>() {{add(5.47); add(-9.43); add(35.35); add(33.84); add(4.66); add(-2.25); }} );
		STRUCTURE.put("ACACC", new ArrayList<Double>() {{add(5.31); add(-8.57); add(34.83); add(34.36); add(3.95); add(-2.95); }} );
		STRUCTURE.put("ACTAA", new ArrayList<Double>() {{add(5.73); add(-8.1); add(31.57); add(34.45); add(-0.98); add(7.88); }} );
		STRUCTURE.put("ACTAT", new ArrayList<Double>() {{add(4.86); add(-8.26); add(31.86); add(35.53); add(-3.25); add(3.79); }} );
		STRUCTURE.put("ACTAG", new ArrayList<Double>() {{add(5.13); add(-7.44); add(32.22); add(35.0); add(-2.86); add(3.55); }} );
		STRUCTURE.put("ACTAC", new ArrayList<Double>() {{add(5.62); add(-8.16); add(31.68); add(34.24); add(-1.63); add(4.89); }} );
		STRUCTURE.put("ACTTA", new ArrayList<Double>() {{add(4.48); add(-9.96); add(33.06); add(35.16); add(-2.87); add(-3.58); }} );
		STRUCTURE.put("ACTTT", new ArrayList<Double>() {{add(3.35); add(-11.68); add(33.88); add(36.93); add(-4.78); add(-6.36); }} );
		STRUCTURE.put("ACTTG", new ArrayList<Double>() {{add(4.52); add(-10.21); add(33.13); add(34.94); add(-2.35); add(-3.59); }} );
		STRUCTURE.put("ACTTC", new ArrayList<Double>() {{add(4.31); add(-8.83); add(32.77); add(35.22); add(-3.34); add(-4.2); }} );
		STRUCTURE.put("ACTGA", new ArrayList<Double>() {{add(5.48); add(-7.1); add(31.46); add(34.41); add(-1.92); add(4.75); }} );
		STRUCTURE.put("ACTGT", new ArrayList<Double>() {{add(5.2); add(-8.12); add(31.75); add(34.72); add(-1.83); add(4.57); }} );
		STRUCTURE.put("ACTGG", new ArrayList<Double>() {{add(5.32); add(-7.24); add(31.51); add(34.39); add(-1.82); add(3.06); }} );
		STRUCTURE.put("ACTGC", new ArrayList<Double>() {{add(5.4); add(-7.79); add(31.49); add(33.95); add(-1.42); add(3.67); }} );
		STRUCTURE.put("ACTCA", new ArrayList<Double>() {{add(4.89); add(-7.5); add(32.14); add(35.29); add(-2.78); add(-1.75); }} );
		STRUCTURE.put("ACTCT", new ArrayList<Double>() {{add(4.86); add(-7.83); add(31.76); add(35.66); add(-1.51); add(-0.84); }} );
		STRUCTURE.put("ACTCG", new ArrayList<Double>() {{add(4.93); add(-7.78); add(31.96); add(35.21); add(-1.9); add(-1.04); }} );
		STRUCTURE.put("ACTCC", new ArrayList<Double>() {{add(4.73); add(-6.75); add(31.81); add(35.76); add(-2.84); add(-2.04); }} );
		STRUCTURE.put("ACGAA", new ArrayList<Double>() {{add(5.21); add(-6.66); add(33.73); add(36.0); add(5.06); add(-0.84); }} );
		STRUCTURE.put("ACGAT", new ArrayList<Double>() {{add(4.8); add(-6.85); add(33.64); add(36.22); add(4.65); add(-1.68); }} );
		STRUCTURE.put("ACGAG", new ArrayList<Double>() {{add(5.13); add(-6.5); add(33.69); add(35.94); add(4.47); add(-2.01); }} );
		STRUCTURE.put("ACGAC", new ArrayList<Double>() {{add(5.34); add(-6.61); add(33.73); add(35.7); add(5.01); add(-2.42); }} );
		STRUCTURE.put("ACGTA", new ArrayList<Double>() {{add(4.84); add(-7.92); add(34.34); add(34.3); add(5.09); add(-2.68); }} );
		STRUCTURE.put("ACGTT", new ArrayList<Double>() {{add(4.21); add(-10.39); add(34.53); add(35.24); add(5.64); add(-3.43); }} );
		STRUCTURE.put("ACGTG", new ArrayList<Double>() {{add(4.85); add(-8.78); add(34.48); add(34.05); add(5.64); add(-3.13); }} );
		STRUCTURE.put("ACGTC", new ArrayList<Double>() {{add(4.7); add(-7.92); add(34.3); add(34.58); add(5.05); add(-3.06); }} );
		STRUCTURE.put("ACGGA", new ArrayList<Double>() {{add(5.06); add(-3.36); add(33.01); add(34.6); add(2.56); add(-2.1); }} );
		STRUCTURE.put("ACGGT", new ArrayList<Double>() {{add(4.73); add(-4.79); add(33.29); add(34.47); add(3.68); add(-1.2); }} );
		STRUCTURE.put("ACGGG", new ArrayList<Double>() {{add(4.92); add(-2.81); add(33.23); add(33.75); add(2.76); add(-2.05); }} );
		STRUCTURE.put("ACGGC", new ArrayList<Double>() {{add(5.08); add(-3.97); add(33.55); add(33.79); add(3.91); add(-1.58); }} );
		STRUCTURE.put("ACGCA", new ArrayList<Double>() {{add(5.27); add(-4.25); add(33.71); add(35.83); add(4.21); add(-1.26); }} );
		STRUCTURE.put("ACGCT", new ArrayList<Double>() {{add(4.86); add(-3.93); add(33.0); add(37.02); add(3.7); add(-2.63); }} );
		STRUCTURE.put("ACGCG", new ArrayList<Double>() {{add(5.2); add(-4.73); add(33.2); add(35.9); add(4.51); add(-1.39); }} );
		STRUCTURE.put("ACGCC", new ArrayList<Double>() {{add(5.08); add(-3.63); add(33.32); add(36.67); add(3.67); add(-2.09); }} );
		STRUCTURE.put("ACCAA", new ArrayList<Double>() {{add(5.08); add(-4.69); add(34.7); add(34.6); add(-0.94); add(4.34); }} );
		STRUCTURE.put("ACCAT", new ArrayList<Double>() {{add(4.97); add(-4.33); add(34.55); add(34.88); add(-1.22); add(4.04); }} );
		STRUCTURE.put("ACCAG", new ArrayList<Double>() {{add(5.19); add(-4.44); add(34.39); add(34.21); add(-0.64); add(3.13); }} );
		STRUCTURE.put("ACCAC", new ArrayList<Double>() {{add(5.38); add(-5.0); add(34.37); add(34.18); add(-0.49); add(3.35); }} );
		STRUCTURE.put("ACCTA", new ArrayList<Double>() {{add(4.68); add(-1.54); add(34.99); add(31.46); add(-1.78); add(-2.96); }} );
		STRUCTURE.put("ACCTT", new ArrayList<Double>() {{add(3.75); add(-1.48); add(35.68); add(32.53); add(-3.27); add(-4.81); }} );
		STRUCTURE.put("ACCTG", new ArrayList<Double>() {{add(4.54); add(-1.68); add(35.08); add(31.38); add(-1.63); add(-2.97); }} );
		STRUCTURE.put("ACCTC", new ArrayList<Double>() {{add(4.37); add(-0.88); add(34.93); add(31.49); add(-2.35); add(-3.46); }} );
		STRUCTURE.put("ACCGA", new ArrayList<Double>() {{add(5.02); add(-4.29); add(34.18); add(32.64); add(-0.94); add(3.35); }} );
		STRUCTURE.put("ACCGT", new ArrayList<Double>() {{add(4.73); add(-4.79); add(34.47); add(33.29); add(-1.2); add(3.68); }} );
		STRUCTURE.put("ACCGG", new ArrayList<Double>() {{add(5.16); add(-5.03); add(34.12); add(32.44); add(-0.44); add(2.54); }} );
		STRUCTURE.put("ACCGC", new ArrayList<Double>() {{add(5.08); add(-4.55); add(34.31); add(32.31); add(-0.62); add(2.79); }} );
		STRUCTURE.put("ACCCA", new ArrayList<Double>() {{add(4.66); add(-3.38); add(34.63); add(33.53); add(-1.42); add(-2.0); }} );
		STRUCTURE.put("ACCCT", new ArrayList<Double>() {{add(4.29); add(-2.62); add(34.42); add(34.17); add(-2.05); add(-3.17); }} );
		STRUCTURE.put("ACCCG", new ArrayList<Double>() {{add(4.7); add(-3.53); add(34.56); add(33.51); add(-0.94); add(-1.76); }} );
		STRUCTURE.put("ACCCC", new ArrayList<Double>() {{add(4.58); add(-2.37); add(34.28); add(33.44); add(-1.8); add(-2.4); }} );
		STRUCTURE.put("TAAAA", new ArrayList<Double>() {{add(4.89); add(-13.29); add(35.6); add(36.72); add(-2.46); add(-2.75); }} );
		STRUCTURE.put("TAAAT", new ArrayList<Double>() {{add(4.75); add(-13.87); add(35.79); add(36.55); add(-0.94); add(-2.36); }} );
		STRUCTURE.put("TAAAG", new ArrayList<Double>() {{add(4.7); add(-12.85); add(35.7); add(36.11); add(-1.56); add(-3.87); }} );
		STRUCTURE.put("TAAAC", new ArrayList<Double>() {{add(5.1); add(-13.54); add(35.72); add(36.4); add(-1.29); add(-2.84); }} );
		STRUCTURE.put("TAATA", new ArrayList<Double>() {{add(5.11); add(-9.98); add(35.37); add(32.48); add(-2.19); add(-3.92); }} );
		STRUCTURE.put("TAATT", new ArrayList<Double>() {{add(4.36); add(-12.2); add(35.92); add(33.31); add(-2.38); add(-5.21); }} );
		STRUCTURE.put("TAATG", new ArrayList<Double>() {{add(4.9); add(-10.52); add(35.43); add(32.4); add(-2.15); add(-4.86); }} );
		STRUCTURE.put("TAATC", new ArrayList<Double>() {{add(4.81); add(-10.25); add(35.28); add(32.72); add(-2.32); add(-4.51); }} );
		STRUCTURE.put("TAAGA", new ArrayList<Double>() {{add(4.93); add(-8.51); add(34.89); add(32.43); add(-3.37); add(-2.51); }} );
		STRUCTURE.put("TAAGT", new ArrayList<Double>() {{add(4.48); add(-9.96); add(35.16); add(33.06); add(-3.58); add(-2.87); }} );
		STRUCTURE.put("TAAGG", new ArrayList<Double>() {{add(4.81); add(-8.13); add(34.66); add(32.34); add(-3.71); add(-4.02); }} );
		STRUCTURE.put("TAAGC", new ArrayList<Double>() {{add(5.01); add(-8.92); add(34.85); add(32.17); add(-2.87); add(-2.56); }} );
		STRUCTURE.put("TAACA", new ArrayList<Double>() {{add(5.17); add(-11.35); add(35.32); add(34.78); add(-2.39); add(-2.47); }} );
		STRUCTURE.put("TAACT", new ArrayList<Double>() {{add(4.67); add(-10.74); add(34.85); add(35.6); add(-3.3); add(-3.98); }} );
		STRUCTURE.put("TAACG", new ArrayList<Double>() {{add(5.15); add(-11.31); add(35.27); add(34.89); add(-2.63); add(-2.83); }} );
		STRUCTURE.put("TAACC", new ArrayList<Double>() {{add(4.87); add(-10.83); add(35.08); add(35.3); add(-3.03); add(-3.26); }} );
		STRUCTURE.put("TATAA", new ArrayList<Double>() {{add(6.07); add(-7.14); add(31.78); add(34.57); add(-3.01); add(7.31); }} );
		STRUCTURE.put("TATAT", new ArrayList<Double>() {{add(5.76); add(-8.95); add(32.12); add(34.46); add(-2.62); add(8.32); }} );
		STRUCTURE.put("TATAG", new ArrayList<Double>() {{add(5.79); add(-7.69); add(31.96); add(34.63); add(-3.1); add(5.66); }} );
		STRUCTURE.put("TATAC", new ArrayList<Double>() {{add(6.01); add(-7.88); add(31.78); add(34.63); add(-2.8); add(5.6); }} );
		STRUCTURE.put("TATTA", new ArrayList<Double>() {{add(5.11); add(-9.98); add(32.48); add(35.37); add(-3.92); add(-2.19); }} );
		STRUCTURE.put("TATTT", new ArrayList<Double>() {{add(3.79); add(-12.63); add(33.85); add(37.22); add(-6.42); add(-3.91); }} );
		STRUCTURE.put("TATTG", new ArrayList<Double>() {{add(5.02); add(-9.7); add(32.4); add(35.12); add(-3.9); add(-2.87); }} );
		STRUCTURE.put("TATTC", new ArrayList<Double>() {{add(4.51); add(-10.01); add(32.92); add(35.93); add(-4.85); add(-3.1); }} );
		STRUCTURE.put("TATGA", new ArrayList<Double>() {{add(5.84); add(-7.0); add(31.46); add(35.1); add(-3.24); add(4.95); }} );
		STRUCTURE.put("TATGT", new ArrayList<Double>() {{add(5.14); add(-8.52); add(32.21); add(35.48); add(-4.73); add(4.85); }} );
		STRUCTURE.put("TATGG", new ArrayList<Double>() {{add(5.88); add(-6.69); add(31.26); add(34.97); add(-2.84); add(3.24); }} );
		STRUCTURE.put("TATGC", new ArrayList<Double>() {{add(5.76); add(-8.11); add(31.62); add(34.78); add(-2.97); add(5.2); }} );
		STRUCTURE.put("TATCA", new ArrayList<Double>() {{add(5.38); add(-7.32); add(31.94); add(35.75); add(-3.4); add(-1.13); }} );
		STRUCTURE.put("TATCT", new ArrayList<Double>() {{add(4.72); add(-8.23); add(32.48); add(36.52); add(-4.23); add(-1.03); }} );
		STRUCTURE.put("TATCG", new ArrayList<Double>() {{add(5.32); add(-7.63); add(31.89); add(35.68); add(-3.35); add(-0.47); }} );
		STRUCTURE.put("TATCC", new ArrayList<Double>() {{add(4.98); add(-7.47); add(32.31); add(36.04); add(-4.17); add(-1.31); }} );
		STRUCTURE.put("TAGAA", new ArrayList<Double>() {{add(5.33); add(-3.73); add(31.57); add(36.55); add(-1.4); add(-0.49); }} );
		STRUCTURE.put("TAGAT", new ArrayList<Double>() {{add(4.74); add(-3.2); add(31.29); add(36.79); add(-2.21); add(-1.24); }} );
		STRUCTURE.put("TAGAG", new ArrayList<Double>() {{add(5.41); add(-3.22); add(31.4); add(35.31); add(-1.35); add(-0.44); }} );
		STRUCTURE.put("TAGAC", new ArrayList<Double>() {{add(5.32); add(-3.62); add(31.64); add(36.16); add(-1.45); add(-1.45); }} );
		STRUCTURE.put("TAGTA", new ArrayList<Double>() {{add(5.33); add(-3.97); add(31.63); add(34.42); add(-1.21); add(-2.12); }} );
		STRUCTURE.put("TAGTT", new ArrayList<Double>() {{add(4.24); add(-6.07); add(32.1); add(35.56); add(-3.9); add(-4.24); }} );
		STRUCTURE.put("TAGTG", new ArrayList<Double>() {{add(5.11); add(-4.24); add(31.7); add(34.28); add(-1.38); add(-2.73); }} );
		STRUCTURE.put("TAGTC", new ArrayList<Double>() {{add(5.08); add(-3.9); add(31.53); add(34.57); add(-1.0); add(-2.07); }} );
		STRUCTURE.put("TAGGA", new ArrayList<Double>() {{add(4.98); add(-1.39); add(31.66); add(34.64); add(-2.51); add(-0.97); }} );
		STRUCTURE.put("TAGGT", new ArrayList<Double>() {{add(4.68); add(-1.54); add(31.46); add(34.99); add(-2.96); add(-1.78); }} );
		STRUCTURE.put("TAGGG", new ArrayList<Double>() {{add(4.99); add(-0.56); add(31.16); add(34.12); add(-2.67); add(-2.27); }} );
		STRUCTURE.put("TAGGC", new ArrayList<Double>() {{add(4.97); add(-1.23); add(31.51); add(34.04); add(-2.47); add(-1.96); }} );
		STRUCTURE.put("TAGCA", new ArrayList<Double>() {{add(5.36); add(-1.6); add(31.39); add(36.48); add(-2.09); add(-1.5); }} );
		STRUCTURE.put("TAGCT", new ArrayList<Double>() {{add(4.88); add(-1.3); add(31.13); add(37.56); add(-2.32); add(-2.66); }} );
		STRUCTURE.put("TAGCG", new ArrayList<Double>() {{add(5.22); add(-1.63); add(31.38); add(36.72); add(-2.2); add(-1.71); }} );
		STRUCTURE.put("TAGCC", new ArrayList<Double>() {{add(5.0); add(-1.22); add(31.35); add(37.31); add(-2.77); add(-2.39); }} );
		STRUCTURE.put("TACAA", new ArrayList<Double>() {{add(5.89); add(-7.17); add(33.4); add(35.27); add(-1.49); add(5.8); }} );
		STRUCTURE.put("TACAT", new ArrayList<Double>() {{add(5.27); add(-7.4); add(34.06); add(35.76); add(-2.14); add(5.57); }} );
		STRUCTURE.put("TACAG", new ArrayList<Double>() {{add(5.41); add(-6.49); add(33.82); add(34.82); add(-1.95); add(3.96); }} );
		STRUCTURE.put("TACAC", new ArrayList<Double>() {{add(5.74); add(-6.58); add(33.6); add(34.99); add(-2.01); add(3.4); }} );
		STRUCTURE.put("TACTA", new ArrayList<Double>() {{add(5.33); add(-3.97); add(34.42); add(31.63); add(-2.12); add(-1.21); }} );
		STRUCTURE.put("TACTT", new ArrayList<Double>() {{add(4.03); add(-4.26); add(35.21); add(33.24); add(-4.72); add(-3.52); }} );
		STRUCTURE.put("TACTG", new ArrayList<Double>() {{add(5.13); add(-4.2); add(34.47); add(31.52); add(-2.15); add(-1.74); }} );
		STRUCTURE.put("TACTC", new ArrayList<Double>() {{add(4.69); add(-3.05); add(34.56); add(31.76); add(-3.67); add(-2.76); }} );
		STRUCTURE.put("TACGA", new ArrayList<Double>() {{add(5.61); add(-6.48); add(33.57); add(33.73); add(-1.92); add(4.91); }} );
		STRUCTURE.put("TACGT", new ArrayList<Double>() {{add(4.84); add(-7.92); add(34.3); add(34.34); add(-2.68); add(5.09); }} );
		STRUCTURE.put("TACGG", new ArrayList<Double>() {{add(5.33); add(-6.7); add(33.73); add(33.27); add(-2.19); add(2.74); }} );
		STRUCTURE.put("TACGC", new ArrayList<Double>() {{add(5.32); add(-7.22); add(33.93); add(33.14); add(-2.07); add(4.31); }} );
		STRUCTURE.put("TACCA", new ArrayList<Double>() {{add(5.13); add(-5.96); add(34.38); add(34.3); add(-2.45); add(-0.68); }} );
		STRUCTURE.put("TACCT", new ArrayList<Double>() {{add(4.33); add(-5.43); add(34.69); add(35.24); add(-4.49); add(-2.81); }} );
		STRUCTURE.put("TACCG", new ArrayList<Double>() {{add(4.96); add(-5.99); add(34.43); add(34.27); add(-2.35); add(-0.83); }} );
		STRUCTURE.put("TACCC", new ArrayList<Double>() {{add(4.71); add(-5.01); add(34.53); add(34.33); add(-3.44); add(-1.79); }} );
		STRUCTURE.put("TTAAA", new ArrayList<Double>() {{add(5.73); add(-10.7); add(34.67); add(35.63); add(5.72); add(-3.0); }} );
		STRUCTURE.put("TTAAT", new ArrayList<Double>() {{add(5.58); add(-11.32); add(34.44); add(35.37); add(8.64); add(-2.12); }} );
		STRUCTURE.put("TTAAG", new ArrayList<Double>() {{add(5.58); add(-9.57); add(34.81); add(34.68); add(6.23); add(-3.71); }} );
		STRUCTURE.put("TTAAC", new ArrayList<Double>() {{add(5.85); add(-10.44); add(34.36); add(35.05); add(7.53); add(-2.86); }} );
		STRUCTURE.put("TTATA", new ArrayList<Double>() {{add(6.07); add(-7.14); add(34.57); add(31.78); add(7.31); add(-3.01); }} );
		STRUCTURE.put("TTATT", new ArrayList<Double>() {{add(5.53); add(-9.65); add(34.83); add(32.44); add(8.21); add(-3.91); }} );
		STRUCTURE.put("TTATG", new ArrayList<Double>() {{add(6.02); add(-8.06); add(34.54); add(31.48); add(7.92); add(-3.28); }} );
		STRUCTURE.put("TTATC", new ArrayList<Double>() {{add(5.89); add(-7.66); add(34.59); add(31.94); add(7.32); add(-3.54); }} );
		STRUCTURE.put("TTAGA", new ArrayList<Double>() {{add(6.0); add(-6.61); add(34.4); add(31.27); add(6.26); add(-1.11); }} );
		STRUCTURE.put("TTAGT", new ArrayList<Double>() {{add(5.73); add(-8.1); add(34.45); add(31.57); add(7.88); add(-0.98); }} );
		STRUCTURE.put("TTAGG", new ArrayList<Double>() {{add(5.96); add(-5.77); add(34.35); add(31.45); add(5.87); add(-2.83); }} );
		STRUCTURE.put("TTAGC", new ArrayList<Double>() {{add(5.82); add(-6.12); add(34.68); add(31.43); add(5.63); add(-2.69); }} );
		STRUCTURE.put("TTACA", new ArrayList<Double>() {{add(5.99); add(-7.55); add(34.6); add(33.74); add(6.03); add(-2.1); }} );
		STRUCTURE.put("TTACT", new ArrayList<Double>() {{add(5.37); add(-7.55); add(34.55); add(34.75); add(5.33); add(-3.91); }} );
		STRUCTURE.put("TTACG", new ArrayList<Double>() {{add(6.11); add(-7.71); add(34.26); add(33.58); add(6.35); add(-1.95); }} );
		STRUCTURE.put("TTACC", new ArrayList<Double>() {{add(5.43); add(-7.21); add(34.64); add(34.56); add(5.01); add(-3.71); }} );
		STRUCTURE.put("TTTAA", new ArrayList<Double>() {{add(5.73); add(-10.7); add(35.63); add(34.67); add(-3.0); add(5.72); }} );
		STRUCTURE.put("TTTAT", new ArrayList<Double>() {{add(5.66); add(-12.37); add(35.94); add(34.6); add(-0.95); add(8.13); }} );
		STRUCTURE.put("TTTAG", new ArrayList<Double>() {{add(5.82); add(-11.18); add(35.72); add(34.35); add(-1.41); add(6.08); }} );
		STRUCTURE.put("TTTAC", new ArrayList<Double>() {{add(5.91); add(-11.89); add(35.53); add(34.25); add(-0.99); add(6.47); }} );
		STRUCTURE.put("TTTTA", new ArrayList<Double>() {{add(4.89); add(-13.29); add(36.72); add(35.6); add(-2.75); add(-2.46); }} );
		STRUCTURE.put("TTTTT", new ArrayList<Double>() {{add(3.38); add(-16.51); add(38.01); add(37.74); add(-5.09); add(-5.05); }} );
		STRUCTURE.put("TTTTG", new ArrayList<Double>() {{add(4.76); add(-13.79); add(36.92); add(35.51); add(-2.76); add(-2.19); }} );
		STRUCTURE.put("TTTTC", new ArrayList<Double>() {{add(4.35); add(-13.16); add(37.0); add(36.32); add(-4.32); add(-3.67); }} );
		STRUCTURE.put("TTTGA", new ArrayList<Double>() {{add(5.6); add(-10.28); add(35.3); add(34.64); add(-2.67); add(5.24); }} );
		STRUCTURE.put("TTTGT", new ArrayList<Double>() {{add(5.21); add(-12.03); add(35.77); add(35.41); add(-2.56); add(6.09); }} );
		STRUCTURE.put("TTTGG", new ArrayList<Double>() {{add(5.42); add(-9.32); add(35.2); add(34.57); add(-3.1); add(2.51); }} );
		STRUCTURE.put("TTTGC", new ArrayList<Double>() {{add(5.41); add(-10.88); add(35.29); add(34.35); add(-3.68); add(3.96); }} );
		STRUCTURE.put("TTTCA", new ArrayList<Double>() {{add(5.4); add(-10.81); add(35.9); add(34.89); add(-1.64); add(0.49); }} );
		STRUCTURE.put("TTTCT", new ArrayList<Double>() {{add(4.74); add(-10.55); add(36.02); add(36.43); add(-2.61); add(-0.69); }} );
		STRUCTURE.put("TTTCG", new ArrayList<Double>() {{add(4.98); add(-10.58); add(35.88); add(35.62); add(-2.61); add(-0.38); }} );
		STRUCTURE.put("TTTCC", new ArrayList<Double>() {{add(4.63); add(-10.09); add(35.97); add(36.46); add(-3.36); add(-1.41); }} );
		STRUCTURE.put("TTGAA", new ArrayList<Double>() {{add(6.0); add(-5.98); add(34.59); add(34.8); add(5.46); add(0.18); }} );
		STRUCTURE.put("TTGAT", new ArrayList<Double>() {{add(5.52); add(-7.12); add(34.6); add(35.38); add(6.62); add(0.04); }} );
		STRUCTURE.put("TTGAG", new ArrayList<Double>() {{add(5.9); add(-6.36); add(34.65); add(34.56); add(5.89); add(-0.68); }} );
		STRUCTURE.put("TTGAC", new ArrayList<Double>() {{add(5.91); add(-7.07); add(34.61); add(34.78); add(6.83); add(-0.77); }} );
		STRUCTURE.put("TTGTA", new ArrayList<Double>() {{add(5.89); add(-7.17); add(35.27); add(33.4); add(5.8); add(-1.49); }} );
		STRUCTURE.put("TTGTT", new ArrayList<Double>() {{add(4.97); add(-10.17); add(35.64); add(34.6); add(6.26); add(-2.29); }} );
		STRUCTURE.put("TTGTG", new ArrayList<Double>() {{add(5.2); add(-8.23); add(35.72); add(33.59); add(6.07); add(-2.4); }} );
		STRUCTURE.put("TTGTC", new ArrayList<Double>() {{add(5.57); add(-7.48); add(34.98); add(33.76); add(6.28); add(-1.95); }} );
		STRUCTURE.put("TTGGA", new ArrayList<Double>() {{add(5.42); add(-2.74); add(34.57); add(33.66); add(2.7); add(-1.03); }} );
		STRUCTURE.put("TTGGT", new ArrayList<Double>() {{add(5.08); add(-4.69); add(34.6); add(34.7); add(4.34); add(-0.94); }} );
		STRUCTURE.put("TTGGG", new ArrayList<Double>() {{add(5.44); add(-3.45); add(34.28); add(32.76); add(6.64); add(-0.64); }} );
		STRUCTURE.put("TTGGC", new ArrayList<Double>() {{add(5.56); add(-3.85); add(34.72); add(33.0); add(4.45); add(-1.23); }} );
		STRUCTURE.put("TTGCA", new ArrayList<Double>() {{add(5.78); add(-3.8); add(34.61); add(35.49); add(4.6); add(-1.25); }} );
		STRUCTURE.put("TTGCT", new ArrayList<Double>() {{add(5.08); add(-3.36); add(34.41); add(36.94); add(3.78); add(-3.02); }} );
		STRUCTURE.put("TTGCG", new ArrayList<Double>() {{add(5.81); add(-4.14); add(34.85); add(35.42); add(4.82); add(-0.77); }} );
		STRUCTURE.put("TTGCC", new ArrayList<Double>() {{add(5.3); add(-3.14); add(34.5); add(36.34); add(3.97); add(-2.26); }} );
		STRUCTURE.put("TTCAA", new ArrayList<Double>() {{add(6.0); add(-5.98); add(34.8); add(34.59); add(0.18); add(5.46); }} );
		STRUCTURE.put("TTCAT", new ArrayList<Double>() {{add(5.5); add(-6.07); add(35.39); add(35.03); add(0.04); add(5.73); }} );
		STRUCTURE.put("TTCAG", new ArrayList<Double>() {{add(5.93); add(-5.95); add(35.08); add(34.21); add(0.57); add(5.37); }} );
		STRUCTURE.put("TTCAC", new ArrayList<Double>() {{add(5.94); add(-6.21); add(34.8); add(34.78); add(0.69); add(4.07); }} );
		STRUCTURE.put("TTCTA", new ArrayList<Double>() {{add(5.33); add(-3.73); add(36.55); add(31.57); add(-0.49); add(-1.4); }} );
		STRUCTURE.put("TTCTT", new ArrayList<Double>() {{add(4.8); add(-3.46); add(36.52); add(32.3); add(-0.63); add(-1.79); }} );
		STRUCTURE.put("TTCTG", new ArrayList<Double>() {{add(5.14); add(-3.52); add(36.46); add(31.59); add(-0.54); add(-1.9); }} );
		STRUCTURE.put("TTCTC", new ArrayList<Double>() {{add(4.67); add(-2.4); add(36.61); add(31.51); add(-1.63); add(-2.79); }} );
		STRUCTURE.put("TTCGA", new ArrayList<Double>() {{add(5.75); add(-5.99); add(34.95); add(32.98); add(0.19); add(5.38); }} );
		STRUCTURE.put("TTCGT", new ArrayList<Double>() {{add(5.21); add(-6.66); add(36.0); add(33.73); add(-0.84); add(5.06); }} );
		STRUCTURE.put("TTCGG", new ArrayList<Double>() {{add(5.38); add(-6.08); add(35.61); add(32.67); add(-0.53); add(2.76); }} );
		STRUCTURE.put("TTCGC", new ArrayList<Double>() {{add(5.53); add(-6.08); add(35.53); add(32.71); add(-0.23); add(3.79); }} );
		STRUCTURE.put("TTCCA", new ArrayList<Double>() {{add(4.97); add(-5.2); add(36.04); add(33.8); add(-0.69); add(-0.87); }} );
		STRUCTURE.put("TTCCT", new ArrayList<Double>() {{add(4.76); add(-4.88); add(36.26); add(34.39); add(-1.3); add(-1.44); }} );
		STRUCTURE.put("TTCCG", new ArrayList<Double>() {{add(5.09); add(-5.36); add(35.82); add(34.24); add(-1.8); add(-1.46); }} );
		STRUCTURE.put("TTCCC", new ArrayList<Double>() {{add(4.73); add(-4.39); add(36.01); add(33.66); add(-0.97); add(-1.61); }} );
		STRUCTURE.put("TGAAA", new ArrayList<Double>() {{add(5.4); add(-10.81); add(34.89); add(35.9); add(0.49); add(-1.64); }} );
		STRUCTURE.put("TGAAT", new ArrayList<Double>() {{add(5.22); add(-10.83); add(35.03); add(36.06); add(0.21); add(-2.45); }} );
		STRUCTURE.put("TGAAG", new ArrayList<Double>() {{add(5.15); add(-10.24); add(35.1); add(35.14); add(0.22); add(-3.01); }} );
		STRUCTURE.put("TGAAC", new ArrayList<Double>() {{add(5.35); add(-10.38); add(35.12); add(35.57); add(0.09); add(-2.98); }} );
		STRUCTURE.put("TGATA", new ArrayList<Double>() {{add(5.38); add(-7.32); add(35.75); add(31.94); add(-1.13); add(-3.4); }} );
		STRUCTURE.put("TGATT", new ArrayList<Double>() {{add(4.46); add(-9.91); add(36.14); add(33.01); add(-1.08); add(-4.75); }} );
		STRUCTURE.put("TGATG", new ArrayList<Double>() {{add(5.22); add(-8.13); add(35.36); add(31.93); add(-0.46); add(-3.98); }} );
		STRUCTURE.put("TGATC", new ArrayList<Double>() {{add(4.77); add(-7.81); add(35.6); add(32.48); add(-1.37); add(-4.67); }} );
		STRUCTURE.put("TGAGA", new ArrayList<Double>() {{add(5.29); add(-6.62); add(35.17); add(31.77); add(-1.29); add(-2.02); }} );
		STRUCTURE.put("TGAGT", new ArrayList<Double>() {{add(4.89); add(-7.5); add(35.29); add(32.14); add(-1.75); add(-2.78); }} );
		STRUCTURE.put("TGAGG", new ArrayList<Double>() {{add(5.3); add(-6.36); add(34.61); add(31.54); add(-0.98); add(-2.49); }} );
		STRUCTURE.put("TGAGC", new ArrayList<Double>() {{add(5.4); add(-6.75); add(34.81); add(31.45); add(-0.9); add(-2.11); }} );
		STRUCTURE.put("TGACA", new ArrayList<Double>() {{add(5.43); add(-8.28); add(35.3); add(34.2); add(-1.53); add(-2.49); }} );
		STRUCTURE.put("TGACT", new ArrayList<Double>() {{add(4.89); add(-8.11); add(35.14); add(35.06); add(-1.77); add(-3.69); }} );
		STRUCTURE.put("TGACG", new ArrayList<Double>() {{add(5.39); add(-8.47); add(35.16); add(34.29); add(-1.16); add(-2.4); }} );
		STRUCTURE.put("TGACC", new ArrayList<Double>() {{add(4.94); add(-7.96); add(35.47); add(34.83); add(-2.01); add(-3.46); }} );
		STRUCTURE.put("TGTAA", new ArrayList<Double>() {{add(5.99); add(-7.55); add(33.74); add(34.6); add(-2.1); add(6.03); }} );
		STRUCTURE.put("TGTAT", new ArrayList<Double>() {{add(5.82); add(-8.93); add(34.26); add(34.63); add(-2.07); add(5.89); }} );
		STRUCTURE.put("TGTAG", new ArrayList<Double>() {{add(5.74); add(-8.6); add(33.91); add(35.06); add(-3.23); add(3.24); }} );
		STRUCTURE.put("TGTAC", new ArrayList<Double>() {{add(6.2); add(-8.0); add(33.68); add(34.52); add(-1.75); add(4.26); }} );
		STRUCTURE.put("TGTTA", new ArrayList<Double>() {{add(5.17); add(-11.35); add(34.78); add(35.32); add(-2.47); add(-2.39); }} );
		STRUCTURE.put("TGTTT", new ArrayList<Double>() {{add(4.65); add(-13.05); add(35.22); add(36.59); add(-2.89); add(-2.98); }} );
		STRUCTURE.put("TGTTG", new ArrayList<Double>() {{add(5.24); add(-11.69); add(34.7); add(35.19); add(-2.5); add(-2.78); }} );
		STRUCTURE.put("TGTTC", new ArrayList<Double>() {{add(4.85); add(-10.91); add(34.81); add(35.68); add(-2.74); add(-3.14); }} );
		STRUCTURE.put("TGTGA", new ArrayList<Double>() {{add(5.84); add(-7.82); add(33.59); add(34.48); add(-2.33); add(3.97); }} );
		STRUCTURE.put("TGTGT", new ArrayList<Double>() {{add(5.5); add(-9.33); add(33.88); add(35.28); add(-2.65); add(2.96); }} );
		STRUCTURE.put("TGTGG", new ArrayList<Double>() {{add(5.8); add(-7.33); add(33.07); add(34.79); add(-1.82); add(2.59); }} );
		STRUCTURE.put("TGTGC", new ArrayList<Double>() {{add(5.94); add(-8.3); add(33.55); add(35.01); add(-1.85); add(3.31); }} );
		STRUCTURE.put("TGTCA", new ArrayList<Double>() {{add(5.43); add(-8.28); add(34.2); add(35.3); add(-2.49); add(-1.53); }} );
		STRUCTURE.put("TGTCT", new ArrayList<Double>() {{add(4.99); add(-8.49); add(34.36); add(36.49); add(-2.8); add(-1.88); }} );
		STRUCTURE.put("TGTCG", new ArrayList<Double>() {{add(5.28); add(-8.25); add(34.23); add(35.73); add(-2.39); add(-1.7); }} );
		STRUCTURE.put("TGTCC", new ArrayList<Double>() {{add(5.02); add(-7.71); add(34.09); add(36.22); add(-2.88); add(-2.2); }} );
		STRUCTURE.put("TGGAA", new ArrayList<Double>() {{add(4.97); add(-5.2); add(33.8); add(36.04); add(-0.87); add(-0.69); }} );
		STRUCTURE.put("TGGAT", new ArrayList<Double>() {{add(4.94); add(-5.7); add(33.69); add(36.24); add(-0.54); add(-0.96); }} );
		STRUCTURE.put("TGGAG", new ArrayList<Double>() {{add(5.02); add(-5.45); add(33.85); add(35.8); add(-0.86); add(-1.81); }} );
		STRUCTURE.put("TGGAC", new ArrayList<Double>() {{add(5.19); add(-5.78); add(33.79); add(36.11); add(-0.37); add(-2.08); }} );
		STRUCTURE.put("TGGTA", new ArrayList<Double>() {{add(5.13); add(-5.96); add(34.3); add(34.38); add(-0.68); add(-2.45); }} );
		STRUCTURE.put("TGGTT", new ArrayList<Double>() {{add(4.33); add(-8.86); add(34.89); add(35.26); add(-0.59); add(-3.3); }} );
		STRUCTURE.put("TGGTG", new ArrayList<Double>() {{add(4.84); add(-7.0); add(34.81); add(34.27); add(-0.68); add(-2.83); }} );
		STRUCTURE.put("TGGTC", new ArrayList<Double>() {{add(4.84); add(-6.17); add(34.3); add(34.68); add(-0.69); add(-2.73); }} );
		STRUCTURE.put("TGGGA", new ArrayList<Double>() {{add(4.93); add(-2.42); add(33.23); add(33.69); add(-1.55); add(-0.93); }} );
		STRUCTURE.put("TGGGT", new ArrayList<Double>() {{add(4.66); add(-3.38); add(33.53); add(34.63); add(-2.0); add(-1.42); }} );
		STRUCTURE.put("TGGGG", new ArrayList<Double>() {{add(4.89); add(-2.02); add(33.26); add(33.49); add(-1.74); add(-2.1); }} );
		STRUCTURE.put("TGGGC", new ArrayList<Double>() {{add(4.96); add(-2.77); add(33.45); add(33.75); add(-2.07); add(-1.99); }} );
		STRUCTURE.put("TGGCA", new ArrayList<Double>() {{add(5.24); add(-2.98); add(33.34); add(36.44); add(-1.33); add(-1.81); }} );
		STRUCTURE.put("TGGCT", new ArrayList<Double>() {{add(4.53); add(-2.76); add(33.35); add(37.62); add(-2.44); add(-3.86); }} );
		STRUCTURE.put("TGGCG", new ArrayList<Double>() {{add(5.06); add(-2.98); add(33.36); add(36.34); add(-1.66); add(-1.9); }} );
		STRUCTURE.put("TGGCC", new ArrayList<Double>() {{add(4.93); add(-2.54); add(33.23); add(37.04); add(-1.76); add(-2.42); }} );
		STRUCTURE.put("TGCAA", new ArrayList<Double>() {{add(5.78); add(-3.8); add(35.49); add(34.61); add(-1.25); add(4.6); }} );
		STRUCTURE.put("TGCAT", new ArrayList<Double>() {{add(5.55); add(-3.73); add(35.6); add(35.12); add(-1.2); add(4.66); }} );
		STRUCTURE.put("TGCAG", new ArrayList<Double>() {{add(5.79); add(-3.68); add(35.64); add(34.5); add(-0.7); add(3.43); }} );
		STRUCTURE.put("TGCAC", new ArrayList<Double>() {{add(5.94); add(-3.74); add(35.46); add(34.64); add(-0.76); add(2.85); }} );
		STRUCTURE.put("TGCTA", new ArrayList<Double>() {{add(5.36); add(-1.6); add(36.48); add(31.39); add(-1.5); add(-2.09); }} );
		STRUCTURE.put("TGCTT", new ArrayList<Double>() {{add(4.61); add(-1.47); add(37.13); add(32.29); add(-3.31); add(-3.05); }} );
		STRUCTURE.put("TGCTG", new ArrayList<Double>() {{add(5.27); add(-1.72); add(36.65); add(31.23); add(-1.51); add(-1.86); }} );
		STRUCTURE.put("TGCTC", new ArrayList<Double>() {{add(5.1); add(-0.94); add(36.61); add(31.46); add(-2.29); add(-2.44); }} );
		STRUCTURE.put("TGCGA", new ArrayList<Double>() {{add(5.68); add(-3.81); add(35.53); add(33.31); add(-0.91); add(4.16); }} );
		STRUCTURE.put("TGCGT", new ArrayList<Double>() {{add(5.27); add(-4.25); add(35.83); add(33.71); add(-1.26); add(4.21); }} );
		STRUCTURE.put("TGCGG", new ArrayList<Double>() {{add(5.59); add(-4.13); add(35.63); add(33.13); add(-0.88); add(2.75); }} );
		STRUCTURE.put("TGCGC", new ArrayList<Double>() {{add(5.71); add(-3.94); add(35.52); add(32.79); add(-0.67); add(3.54); }} );
		STRUCTURE.put("TGCCA", new ArrayList<Double>() {{add(5.24); add(-2.98); add(36.44); add(33.34); add(-1.81); add(-1.33); }} );
		STRUCTURE.put("TGCCT", new ArrayList<Double>() {{add(4.82); add(-2.38); add(36.26); add(34.03); add(-2.41); add(-2.06); }} );
		STRUCTURE.put("TGCCG", new ArrayList<Double>() {{add(5.17); add(-3.11); add(36.2); add(33.58); add(-1.38); add(-1.26); }} );
		STRUCTURE.put("TGCCC", new ArrayList<Double>() {{add(5.02); add(-2.52); add(36.27); add(33.82); add(-1.81); add(-1.78); }} );
		STRUCTURE.put("TCAAA", new ArrayList<Double>() {{add(5.6); add(-10.28); add(34.64); add(35.3); add(5.24); add(-2.67); }} );
		STRUCTURE.put("TCAAT", new ArrayList<Double>() {{add(5.46); add(-10.92); add(34.6); add(34.93); add(7.05); add(-2.1); }} );
		STRUCTURE.put("TCAAG", new ArrayList<Double>() {{add(5.4); add(-9.98); add(34.53); add(34.76); add(5.51); add(-3.44); }} );
		STRUCTURE.put("TCAAC", new ArrayList<Double>() {{add(5.65); add(-10.22); add(34.69); add(34.93); add(5.39); add(-3.34); }} );
		STRUCTURE.put("TCATA", new ArrayList<Double>() {{add(5.84); add(-7.0); add(35.1); add(31.46); add(4.95); add(-3.24); }} );
		STRUCTURE.put("TCATT", new ArrayList<Double>() {{add(4.46); add(-9.02); add(34.98); add(32.87); add(3.8); add(-6.08); }} );
		STRUCTURE.put("TCATG", new ArrayList<Double>() {{add(5.27); add(-9.26); add(35.0); add(31.75); add(5.91); add(-4.44); }} );
		STRUCTURE.put("TCATC", new ArrayList<Double>() {{add(5.08); add(-7.37); add(34.64); add(32.03); add(4.02); add(-4.68); }} );
		STRUCTURE.put("TCAGA", new ArrayList<Double>() {{add(5.47); add(-6.32); add(34.38); add(31.28); add(4.43); add(-1.76); }} );
		STRUCTURE.put("TCAGT", new ArrayList<Double>() {{add(5.48); add(-7.1); add(34.41); add(31.46); add(4.75); add(-1.92); }} );
		STRUCTURE.put("TCAGG", new ArrayList<Double>() {{add(5.47); add(-5.31); add(34.07); add(30.94); add(3.63); add(-3.12); }} );
		STRUCTURE.put("TCAGC", new ArrayList<Double>() {{add(5.74); add(-6.36); add(34.29); add(31.14); add(4.93); add(-2.0); }} );
		STRUCTURE.put("TCACA", new ArrayList<Double>() {{add(5.84); add(-7.82); add(34.48); add(33.59); add(3.97); add(-2.33); }} );
		STRUCTURE.put("TCACT", new ArrayList<Double>() {{add(5.22); add(-7.1); add(33.87); add(34.23); add(3.56); add(-3.58); }} );
		STRUCTURE.put("TCACG", new ArrayList<Double>() {{add(5.71); add(-8.08); add(34.66); add(33.8); add(4.27); add(-2.49); }} );
		STRUCTURE.put("TCACC", new ArrayList<Double>() {{add(5.4); add(-7.04); add(34.15); add(34.04); add(3.82); add(-3.21); }} );
		STRUCTURE.put("TCTAA", new ArrayList<Double>() {{add(6.0); add(-6.61); add(31.27); add(34.4); add(-1.11); add(6.26); }} );
		STRUCTURE.put("TCTAT", new ArrayList<Double>() {{add(5.69); add(-7.03); add(31.88); add(34.25); add(-1.51); add(6.34); }} );
		STRUCTURE.put("TCTAG", new ArrayList<Double>() {{add(5.84); add(-6.98); add(31.57); add(34.35); add(-1.22); add(5.27); }} );
		STRUCTURE.put("TCTAC", new ArrayList<Double>() {{add(5.76); add(-6.38); add(30.99); add(34.42); add(-2.44); add(4.12); }} );
		STRUCTURE.put("TCTTA", new ArrayList<Double>() {{add(4.93); add(-8.51); add(32.43); add(34.89); add(-2.51); add(-3.37); }} );
		STRUCTURE.put("TCTTT", new ArrayList<Double>() {{add(4.02); add(-10.71); add(33.18); add(36.69); add(-3.21); add(-4.76); }} );
		STRUCTURE.put("TCTTG", new ArrayList<Double>() {{add(4.92); add(-9.12); add(32.47); add(34.77); add(-1.59); add(-3.37); }} );
		STRUCTURE.put("TCTTC", new ArrayList<Double>() {{add(4.52); add(-8.18); add(32.42); add(35.03); add(-2.28); add(-3.52); }} );
		STRUCTURE.put("TCTGA", new ArrayList<Double>() {{add(5.47); add(-6.32); add(31.28); add(34.38); add(-1.76); add(4.43); }} );
		STRUCTURE.put("TCTGT", new ArrayList<Double>() {{add(5.33); add(-7.0); add(31.6); add(35.0); add(-1.93); add(3.72); }} );
		STRUCTURE.put("TCTGG", new ArrayList<Double>() {{add(5.56); add(-6.09); add(31.02); add(33.91); add(-2.71); add(2.85); }} );
		STRUCTURE.put("TCTGC", new ArrayList<Double>() {{add(5.75); add(-6.69); add(31.17); add(33.98); add(-1.2); add(4.1); }} );
		STRUCTURE.put("TCTCA", new ArrayList<Double>() {{add(5.29); add(-6.62); add(31.77); add(35.17); add(-2.02); add(-1.29); }} );
		STRUCTURE.put("TCTCT", new ArrayList<Double>() {{add(5.0); add(-6.62); add(31.7); add(35.8); add(-1.87); add(-1.55); }} );
		STRUCTURE.put("TCTCG", new ArrayList<Double>() {{add(5.17); add(-6.8); add(31.51); add(35.18); add(-1.9); add(-1.21); }} );
		STRUCTURE.put("TCTCC", new ArrayList<Double>() {{add(4.92); add(-6.1); add(31.48); add(35.79); add(-2.23); add(-2.03); }} );
		STRUCTURE.put("TCGAA", new ArrayList<Double>() {{add(5.75); add(-5.99); add(32.98); add(34.95); add(5.38); add(0.19); }} );
		STRUCTURE.put("TCGAT", new ArrayList<Double>() {{add(5.23); add(-7.08); add(32.77); add(35.76); add(5.51); add(-0.54); }} );
		STRUCTURE.put("TCGAG", new ArrayList<Double>() {{add(5.42); add(-6.72); add(33.01); add(35.17); add(5.47); add(-1.2); }} );
		STRUCTURE.put("TCGAC", new ArrayList<Double>() {{add(5.64); add(-6.68); add(33.06); add(35.51); add(5.38); add(-1.63); }} );
		STRUCTURE.put("TCGTA", new ArrayList<Double>() {{add(5.61); add(-6.48); add(33.73); add(33.57); add(4.91); add(-1.92); }} );
		STRUCTURE.put("TCGTT", new ArrayList<Double>() {{add(4.8); add(-9.62); add(33.66); add(34.71); add(5.41); add(-2.49); }} );
		STRUCTURE.put("TCGTG", new ArrayList<Double>() {{add(5.29); add(-7.95); add(33.99); add(33.77); add(5.8); add(-2.18); }} );
		STRUCTURE.put("TCGTC", new ArrayList<Double>() {{add(5.16); add(-7.11); add(33.65); add(34.18); add(4.81); add(-2.39); }} );
		STRUCTURE.put("TCGGA", new ArrayList<Double>() {{add(5.36); add(-3.04); add(32.77); add(33.64); add(3.14); add(-0.81); }} );
		STRUCTURE.put("TCGGT", new ArrayList<Double>() {{add(5.02); add(-4.29); add(32.64); add(34.18); add(3.35); add(-0.94); }} );
		STRUCTURE.put("TCGGG", new ArrayList<Double>() {{add(5.22); add(-2.92); add(32.86); add(33.58); add(3.35); add(-1.75); }} );
		STRUCTURE.put("TCGGC", new ArrayList<Double>() {{add(5.43); add(-3.4); add(32.47); add(32.95); add(3.91); add(-0.99); }} );
		STRUCTURE.put("TCGCA", new ArrayList<Double>() {{add(5.68); add(-3.81); add(33.31); add(35.53); add(4.16); add(-0.91); }} );
		STRUCTURE.put("TCGCT", new ArrayList<Double>() {{add(5.19); add(-3.56); add(32.44); add(36.83); add(4.15); add(-2.29); }} );
		STRUCTURE.put("TCGCG", new ArrayList<Double>() {{add(5.42); add(-4.27); add(32.65); add(35.66); add(4.5); add(-1.33); }} );
		STRUCTURE.put("TCGCC", new ArrayList<Double>() {{add(5.4); add(-2.99); add(32.6); add(36.2); add(3.27); add(-1.65); }} );
		STRUCTURE.put("TCCAA", new ArrayList<Double>() {{add(5.42); add(-2.74); add(33.66); add(34.57); add(-1.03); add(2.7); }} );
		STRUCTURE.put("TCCAT", new ArrayList<Double>() {{add(5.34); add(-3.37); add(33.69); add(34.46); add(-0.26); add(4.34); }} );
		STRUCTURE.put("TCCAG", new ArrayList<Double>() {{add(5.31); add(-2.98); add(33.74); add(33.98); add(-0.76); add(2.8); }} );
		STRUCTURE.put("TCCAC", new ArrayList<Double>() {{add(5.58); add(-3.48); add(33.75); add(34.34); add(-0.49); add(2.75); }} );
		STRUCTURE.put("TCCTA", new ArrayList<Double>() {{add(4.98); add(-1.39); add(34.64); add(31.66); add(-0.97); add(-2.51); }} );
		STRUCTURE.put("TCCTT", new ArrayList<Double>() {{add(4.31); add(-1.33); add(34.9); add(32.55); add(-1.91); add(-3.38); }} );
		STRUCTURE.put("TCCTG", new ArrayList<Double>() {{add(4.92); add(-0.9); add(34.22); add(31.21); add(-1.05); add(-2.79); }} );
		STRUCTURE.put("TCCTC", new ArrayList<Double>() {{add(4.8); add(-0.44); add(34.17); add(31.53); add(-1.37); add(-2.86); }} );
		STRUCTURE.put("TCCGA", new ArrayList<Double>() {{add(5.36); add(-3.04); add(33.64); add(32.77); add(-0.81); add(3.14); }} );
		STRUCTURE.put("TCCGT", new ArrayList<Double>() {{add(5.06); add(-3.36); add(34.6); add(33.01); add(-2.1); add(2.56); }} );
		STRUCTURE.put("TCCGG", new ArrayList<Double>() {{add(5.39); add(-3.19); add(33.6); add(32.84); add(-0.62); add(1.31); }} );
		STRUCTURE.put("TCCGC", new ArrayList<Double>() {{add(5.27); add(-3.15); add(33.51); add(32.42); add(-0.89); add(2.4); }} );
		STRUCTURE.put("TCCCA", new ArrayList<Double>() {{add(4.93); add(-2.42); add(33.69); add(33.23); add(-0.93); add(-1.55); }} );
		STRUCTURE.put("TCCCT", new ArrayList<Double>() {{add(4.68); add(-1.76); add(33.6); add(33.86); add(-1.61); add(-2.51); }} );
		STRUCTURE.put("TCCCG", new ArrayList<Double>() {{add(4.95); add(-2.56); add(34.12); add(33.56); add(-0.91); add(-1.93); }} );
		STRUCTURE.put("TCCCC", new ArrayList<Double>() {{add(4.76); add(-1.44); add(33.65); add(33.55); add(-1.23); add(-2.33); }} );
		STRUCTURE.put("GAAAA", new ArrayList<Double>() {{add(4.35); add(-13.16); add(36.32); add(37.0); add(-3.67); add(-4.32); }} );
		STRUCTURE.put("GAAAT", new ArrayList<Double>() {{add(4.27); add(-12.09); add(35.77); add(36.7); add(-3.13); add(-4.4); }} );
		STRUCTURE.put("GAAAG", new ArrayList<Double>() {{add(4.36); add(-11.93); add(35.91); add(36.1); add(-3.0); add(-4.95); }} );
		STRUCTURE.put("GAAAC", new ArrayList<Double>() {{add(4.74); add(-12.76); add(36.01); add(36.17); add(-2.18); add(-3.35); }} );
		STRUCTURE.put("GAATA", new ArrayList<Double>() {{add(4.51); add(-10.01); add(35.93); add(32.92); add(-3.1); add(-4.85); }} );
		STRUCTURE.put("GAATT", new ArrayList<Double>() {{add(3.75); add(-11.18); add(36.17); add(33.75); add(-3.59); add(-6.49); }} );
		STRUCTURE.put("GAATG", new ArrayList<Double>() {{add(4.39); add(-9.43); add(35.63); add(32.69); add(-3.64); add(-5.91); }} );
		STRUCTURE.put("GAATC", new ArrayList<Double>() {{add(4.36); add(-9.46); add(35.53); add(32.93); add(-3.18); add(-5.55); }} );
		STRUCTURE.put("GAAGA", new ArrayList<Double>() {{add(4.52); add(-8.18); add(35.03); add(32.42); add(-3.52); add(-2.28); }} );
		STRUCTURE.put("GAAGT", new ArrayList<Double>() {{add(4.31); add(-8.83); add(35.22); add(32.77); add(-4.2); add(-3.34); }} );
		STRUCTURE.put("GAAGG", new ArrayList<Double>() {{add(4.63); add(-7.38); add(34.75); add(32.02); add(-3.82); add(-3.57); }} );
		STRUCTURE.put("GAAGC", new ArrayList<Double>() {{add(4.82); add(-8.01); add(34.96); add(32.06); add(-3.36); add(-2.79); }} );
		STRUCTURE.put("GAACA", new ArrayList<Double>() {{add(4.85); add(-10.91); add(35.68); add(34.81); add(-3.14); add(-2.74); }} );
		STRUCTURE.put("GAACT", new ArrayList<Double>() {{add(4.25); add(-10.45); add(35.24); add(35.93); add(-3.79); add(-4.54); }} );
		STRUCTURE.put("GAACG", new ArrayList<Double>() {{add(4.74); add(-11.06); add(35.71); add(34.9); add(-2.93); add(-2.93); }} );
		STRUCTURE.put("GAACC", new ArrayList<Double>() {{add(4.46); add(-10.08); add(35.45); add(35.19); add(-3.83); add(-3.59); }} );
		STRUCTURE.put("GATAA", new ArrayList<Double>() {{add(5.89); add(-7.66); add(31.94); add(34.59); add(-3.54); add(7.32); }} );
		STRUCTURE.put("GATAT", new ArrayList<Double>() {{add(5.4); add(-8.3); add(32.32); add(35.01); add(-3.98); add(6.33); }} );
		STRUCTURE.put("GATAG", new ArrayList<Double>() {{add(5.54); add(-8.88); add(32.43); add(33.89); add(-4.19); add(6.28); }} );
		STRUCTURE.put("GATAC", new ArrayList<Double>() {{add(5.65); add(-8.06); add(32.1); add(34.46); add(-3.62); add(5.72); }} );
		STRUCTURE.put("GATTA", new ArrayList<Double>() {{add(4.81); add(-10.25); add(32.72); add(35.28); add(-4.51); add(-2.32); }} );
		STRUCTURE.put("GATTT", new ArrayList<Double>() {{add(4.12); add(-12.27); add(33.42); add(36.55); add(-5.36); add(-2.43); }} );
		STRUCTURE.put("GATTG", new ArrayList<Double>() {{add(4.81); add(-10.33); add(32.75); add(34.92); add(-4.28); add(-2.12); }} );
		STRUCTURE.put("GATTC", new ArrayList<Double>() {{add(4.36); add(-9.46); add(32.93); add(35.53); add(-5.55); add(-3.18); }} );
		STRUCTURE.put("GATGA", new ArrayList<Double>() {{add(5.08); add(-7.37); add(32.03); add(34.64); add(-4.68); add(4.02); }} );
		STRUCTURE.put("GATGT", new ArrayList<Double>() {{add(4.82); add(-8.65); add(32.37); add(35.37); add(-4.91); add(5.15); }} );
		STRUCTURE.put("GATGG", new ArrayList<Double>() {{add(5.53); add(-7.48); add(31.66); add(34.54); add(-3.42); add(4.13); }} );
		STRUCTURE.put("GATGC", new ArrayList<Double>() {{add(5.38); add(-7.6); add(31.96); add(34.59); add(-4.24); add(3.67); }} );
		STRUCTURE.put("GATCA", new ArrayList<Double>() {{add(4.77); add(-7.81); add(32.48); add(35.6); add(-4.67); add(-1.37); }} );
		STRUCTURE.put("GATCT", new ArrayList<Double>() {{add(4.36); add(-8.23); add(32.82); add(36.68); add(-5.53); add(-1.69); }} );
		STRUCTURE.put("GATCG", new ArrayList<Double>() {{add(4.96); add(-7.79); add(32.29); add(35.54); add(-4.15); add(-0.57); }} );
		STRUCTURE.put("GATCC", new ArrayList<Double>() {{add(4.63); add(-7.56); add(32.41); add(36.29); add(-4.63); add(-1.67); }} );
		STRUCTURE.put("GAGAA", new ArrayList<Double>() {{add(4.67); add(-2.4); add(31.51); add(36.61); add(-2.79); add(-1.63); }} );
		STRUCTURE.put("GAGAT", new ArrayList<Double>() {{add(4.74); add(-2.78); add(31.47); add(36.47); add(-2.16); add(-1.46); }} );
		STRUCTURE.put("GAGAG", new ArrayList<Double>() {{add(4.93); add(-2.54); add(31.39); add(35.65); add(-1.93); add(-1.9); }} );
		STRUCTURE.put("GAGAC", new ArrayList<Double>() {{add(5.04); add(-3.46); add(31.76); add(36.17); add(-1.45); add(-1.85); }} );
		STRUCTURE.put("GAGTA", new ArrayList<Double>() {{add(4.69); add(-3.05); add(31.76); add(34.56); add(-2.76); add(-3.67); }} );
		STRUCTURE.put("GAGTT", new ArrayList<Double>() {{add(3.95); add(-5.23); add(32.41); add(36.03); add(-3.01); add(-4.38); }} );
		STRUCTURE.put("GAGTG", new ArrayList<Double>() {{add(4.78); add(-3.31); add(31.79); add(34.12); add(-1.98); add(-3.21); }} );
		STRUCTURE.put("GAGTC", new ArrayList<Double>() {{add(4.53); add(-3.11); add(31.83); add(34.87); add(-2.44); add(-3.57); }} );
		STRUCTURE.put("GAGGA", new ArrayList<Double>() {{add(4.8); add(-0.44); add(31.53); add(34.17); add(-2.86); add(-1.37); }} );
		STRUCTURE.put("GAGGT", new ArrayList<Double>() {{add(4.37); add(-0.88); add(31.49); add(34.93); add(-3.46); add(-2.35); }} );
		STRUCTURE.put("GAGGG", new ArrayList<Double>() {{add(4.78); add(-0.03); add(31.33); add(33.87); add(-2.73); add(-2.62); }} );
		STRUCTURE.put("GAGGC", new ArrayList<Double>() {{add(4.85); add(-0.7); add(31.38); add(33.92); add(-2.42); add(-2.18); }} );
		STRUCTURE.put("GAGCA", new ArrayList<Double>() {{add(5.1); add(-0.94); add(31.46); add(36.61); add(-2.44); add(-2.29); }} );
		STRUCTURE.put("GAGCT", new ArrayList<Double>() {{add(4.63); add(-0.83); add(31.33); add(37.66); add(-2.86); add(-3.25); }} );
		STRUCTURE.put("GAGCG", new ArrayList<Double>() {{add(5.02); add(-1.32); add(31.47); add(36.68); add(-1.89); add(-1.98); }} );
		STRUCTURE.put("GAGCC", new ArrayList<Double>() {{add(4.67); add(-0.53); add(31.39); add(37.09); add(-2.76); add(-3.07); }} );
		STRUCTURE.put("GACAA", new ArrayList<Double>() {{add(5.57); add(-7.48); add(33.76); add(34.98); add(-1.95); add(6.28); }} );
		STRUCTURE.put("GACAT", new ArrayList<Double>() {{add(5.02); add(-7.1); add(34.24); add(35.42); add(-2.83); add(4.9); }} );
		STRUCTURE.put("GACAG", new ArrayList<Double>() {{add(5.26); add(-6.88); add(34.25); add(34.79); add(-2.46); add(3.98); }} );
		STRUCTURE.put("GACAC", new ArrayList<Double>() {{add(5.49); add(-7.25); add(34.03); add(34.92); add(-2.01); add(4.23); }} );
		STRUCTURE.put("GACTA", new ArrayList<Double>() {{add(5.08); add(-3.9); add(34.57); add(31.53); add(-2.07); add(-1.0); }} );
		STRUCTURE.put("GACTT", new ArrayList<Double>() {{add(3.74); add(-4.07); add(35.44); add(33.16); add(-5.04); add(-4.01); }} );
		STRUCTURE.put("GACTG", new ArrayList<Double>() {{add(5.0); add(-3.96); add(34.65); add(31.46); add(-2.08); add(-1.4); }} );
		STRUCTURE.put("GACTC", new ArrayList<Double>() {{add(4.53); add(-3.11); add(34.87); add(31.83); add(-3.57); add(-2.44); }} );
		STRUCTURE.put("GACGA", new ArrayList<Double>() {{add(5.16); add(-7.11); add(34.18); add(33.65); add(-2.39); add(4.81); }} );
		STRUCTURE.put("GACGT", new ArrayList<Double>() {{add(4.7); add(-7.92); add(34.58); add(34.3); add(-3.06); add(5.05); }} );
		STRUCTURE.put("GACGG", new ArrayList<Double>() {{add(5.1); add(-7.15); add(34.1); add(33.08); add(-2.46); add(3.11); }} );
		STRUCTURE.put("GACGC", new ArrayList<Double>() {{add(5.09); add(-7.38); add(34.21); add(33.02); add(-2.45); add(4.23); }} );
		STRUCTURE.put("GACCA", new ArrayList<Double>() {{add(4.84); add(-6.17); add(34.68); add(34.3); add(-2.73); add(-0.69); }} );
		STRUCTURE.put("GACCT", new ArrayList<Double>() {{add(4.18); add(-5.67); add(34.84); add(35.04); add(-3.96); add(-2.46); }} );
		STRUCTURE.put("GACCG", new ArrayList<Double>() {{add(4.69); add(-5.99); add(34.65); add(34.17); add(-2.77); add(-0.84); }} );
		STRUCTURE.put("GACCC", new ArrayList<Double>() {{add(4.45); add(-5.33); add(34.81); add(34.36); add(-3.44); add(-1.78); }} );
		STRUCTURE.put("GTAAA", new ArrayList<Double>() {{add(5.91); add(-11.89); add(34.25); add(35.53); add(6.47); add(-0.99); }} );
		STRUCTURE.put("GTAAT", new ArrayList<Double>() {{add(5.44); add(-11.34); add(34.68); add(35.53); add(5.03); add(-2.51); }} );
		STRUCTURE.put("GTAAG", new ArrayList<Double>() {{add(5.62); add(-10.31); add(34.34); add(34.87); add(5.9); add(-3.24); }} );
		STRUCTURE.put("GTAAC", new ArrayList<Double>() {{add(5.86); add(-10.83); add(34.23); add(35.06); add(5.89); add(-2.71); }} );
		STRUCTURE.put("GTATA", new ArrayList<Double>() {{add(6.01); add(-7.88); add(34.63); add(31.78); add(5.6); add(-2.8); }} );
		STRUCTURE.put("GTATT", new ArrayList<Double>() {{add(5.3); add(-10.45); add(34.79); add(32.78); add(6.24); add(-4.27); }} );
		STRUCTURE.put("GTATG", new ArrayList<Double>() {{add(5.93); add(-8.42); add(34.49); add(31.46); add(6.01); add(-2.99); }} );
		STRUCTURE.put("GTATC", new ArrayList<Double>() {{add(5.65); add(-8.06); add(34.46); add(32.1); add(5.72); add(-3.62); }} );
		STRUCTURE.put("GTAGA", new ArrayList<Double>() {{add(5.76); add(-6.38); add(34.42); add(30.99); add(4.12); add(-2.44); }} );
		STRUCTURE.put("GTAGT", new ArrayList<Double>() {{add(5.62); add(-8.16); add(34.24); add(31.68); add(4.89); add(-1.63); }} );
		STRUCTURE.put("GTAGG", new ArrayList<Double>() {{add(5.76); add(-6.19); add(34.06); add(31.41); add(4.46); add(-2.71); }} );
		STRUCTURE.put("GTAGC", new ArrayList<Double>() {{add(5.88); add(-6.96); add(34.48); add(31.25); add(4.45); add(-2.28); }} );
		STRUCTURE.put("GTACA", new ArrayList<Double>() {{add(6.2); add(-8.0); add(34.52); add(33.68); add(4.26); add(-1.75); }} );
		STRUCTURE.put("GTACT", new ArrayList<Double>() {{add(5.54); add(-8.16); add(33.82); add(34.74); add(4.94); add(-2.73); }} );
		STRUCTURE.put("GTACG", new ArrayList<Double>() {{add(5.93); add(-8.84); add(34.2); add(33.97); add(4.68); add(-2.11); }} );
		STRUCTURE.put("GTACC", new ArrayList<Double>() {{add(5.84); add(-8.12); add(34.23); add(34.34); add(4.32); add(-2.37); }} );
		STRUCTURE.put("GTTAA", new ArrayList<Double>() {{add(5.85); add(-10.44); add(35.05); add(34.36); add(-2.86); add(7.53); }} );
		STRUCTURE.put("GTTAT", new ArrayList<Double>() {{add(5.62); add(-10.51); add(35.5); add(34.54); add(-3.5); add(6.14); }} );
		STRUCTURE.put("GTTAG", new ArrayList<Double>() {{add(5.8); add(-10.65); add(35.18); add(34.54); add(-2.89); add(5.73); }} );
		STRUCTURE.put("GTTAC", new ArrayList<Double>() {{add(5.86); add(-10.83); add(35.06); add(34.23); add(-2.71); add(5.89); }} );
		STRUCTURE.put("GTTTA", new ArrayList<Double>() {{add(5.1); add(-13.54); add(36.4); add(35.72); add(-2.84); add(-1.29); }} );
		STRUCTURE.put("GTTTT", new ArrayList<Double>() {{add(4.05); add(-14.47); add(36.95); add(37.13); add(-4.8); add(-3.62); }} );
		STRUCTURE.put("GTTTG", new ArrayList<Double>() {{add(4.95); add(-13.1); add(36.33); add(35.28); add(-3.27); add(-2.42); }} );
		STRUCTURE.put("GTTTC", new ArrayList<Double>() {{add(4.74); add(-12.76); add(36.17); add(36.01); add(-3.35); add(-2.18); }} );
		STRUCTURE.put("GTTGA", new ArrayList<Double>() {{add(5.65); add(-10.22); add(34.93); add(34.69); add(-3.34); add(5.39); }} );
		STRUCTURE.put("GTTGT", new ArrayList<Double>() {{add(5.58); add(-11.7); add(35.25); add(35.22); add(-3.26); add(6.2); }} );
		STRUCTURE.put("GTTGG", new ArrayList<Double>() {{add(5.42); add(-9.8); add(31.76); add(34.44); add(-1.94); add(6.57); }} );
		STRUCTURE.put("GTTGC", new ArrayList<Double>() {{add(5.7); add(-10.75); add(35.02); add(34.8); add(-2.98); add(4.66); }} );
		STRUCTURE.put("GTTCA", new ArrayList<Double>() {{add(5.35); add(-10.38); add(35.57); add(35.12); add(-2.98); add(0.09); }} );
		STRUCTURE.put("GTTCT", new ArrayList<Double>() {{add(4.85); add(-10.59); add(35.7); add(36.45); add(-3.03); add(-0.56); }} );
		STRUCTURE.put("GTTCG", new ArrayList<Double>() {{add(5.1); add(-10.48); add(35.58); add(35.61); add(-3.31); add(-0.26); }} );
		STRUCTURE.put("GTTCC", new ArrayList<Double>() {{add(4.84); add(-9.91); add(35.48); add(36.02); add(-3.78); add(-1.77); }} );
		STRUCTURE.put("GTGAA", new ArrayList<Double>() {{add(5.94); add(-6.21); add(34.78); add(34.8); add(4.07); add(0.69); }} );
		STRUCTURE.put("GTGAT", new ArrayList<Double>() {{add(5.35); add(-6.53); add(34.03); add(35.64); add(3.72); add(-0.51); }} );
		STRUCTURE.put("GTGAG", new ArrayList<Double>() {{add(5.8); add(-6.5); add(34.48); add(34.92); add(4.33); add(-0.77); }} );
		STRUCTURE.put("GTGAC", new ArrayList<Double>() {{add(6.01); add(-6.82); add(34.6); add(35.33); add(4.03); add(-1.42); }} );
		STRUCTURE.put("GTGTA", new ArrayList<Double>() {{add(5.74); add(-6.58); add(34.99); add(33.6); add(3.4); add(-2.01); }} );
		STRUCTURE.put("GTGTT", new ArrayList<Double>() {{add(5.05); add(-9.4); add(35.07); add(34.68); add(4.19); add(-2.42); }} );
		STRUCTURE.put("GTGTG", new ArrayList<Double>() {{add(5.73); add(-8.16); add(35.12); add(33.35); add(5.01); add(-1.37); }} );
		STRUCTURE.put("GTGTC", new ArrayList<Double>() {{add(5.49); add(-7.25); add(34.92); add(34.03); add(4.23); add(-2.01); }} );
		STRUCTURE.put("GTGGA", new ArrayList<Double>() {{add(5.58); add(-3.48); add(34.34); add(33.75); add(2.75); add(-0.49); }} );
		STRUCTURE.put("GTGGT", new ArrayList<Double>() {{add(5.38); add(-5.0); add(34.18); add(34.37); add(3.35); add(-0.49); }} );
		STRUCTURE.put("GTGGG", new ArrayList<Double>() {{add(5.41); add(-2.94); add(34.09); add(33.55); add(2.54); add(-1.85); }} );
		STRUCTURE.put("GTGGC", new ArrayList<Double>() {{add(5.46); add(-4.03); add(34.2); add(33.11); add(3.09); add(-1.52); }} );
		STRUCTURE.put("GTGCA", new ArrayList<Double>() {{add(5.94); add(-3.74); add(34.64); add(35.46); add(2.85); add(-0.76); }} );
		STRUCTURE.put("GTGCT", new ArrayList<Double>() {{add(5.43); add(-3.79); add(34.03); add(36.67); add(2.81); add(-1.72); }} );
		STRUCTURE.put("GTGCG", new ArrayList<Double>() {{add(5.88); add(-4.13); add(34.69); add(35.54); add(3.27); add(-0.64); }} );
		STRUCTURE.put("GTGCC", new ArrayList<Double>() {{add(5.66); add(-3.41); add(34.22); add(36.31); add(3.05); add(-1.35); }} );
		STRUCTURE.put("GTCAA", new ArrayList<Double>() {{add(5.91); add(-7.07); add(34.78); add(34.61); add(-0.77); add(6.83); }} );
		STRUCTURE.put("GTCAT", new ArrayList<Double>() {{add(5.45); add(-6.37); add(35.47); add(35.1); add(-2.06); add(5.16); }} );
		STRUCTURE.put("GTCAG", new ArrayList<Double>() {{add(5.67); add(-6.43); add(35.2); add(34.37); add(-1.29); add(4.57); }} );
		STRUCTURE.put("GTCAC", new ArrayList<Double>() {{add(6.01); add(-6.82); add(35.33); add(34.6); add(-1.42); add(4.03); }} );
		STRUCTURE.put("GTCTA", new ArrayList<Double>() {{add(5.32); add(-3.62); add(36.16); add(31.64); add(-1.45); add(-1.45); }} );
		STRUCTURE.put("GTCTT", new ArrayList<Double>() {{add(4.65); add(-3.82); add(36.4); add(32.56); add(-2.37); add(-2.15); }} );
		STRUCTURE.put("GTCTG", new ArrayList<Double>() {{add(5.22); add(-3.97); add(36.44); add(31.42); add(-1.52); add(-1.18); }} );
		STRUCTURE.put("GTCTC", new ArrayList<Double>() {{add(5.04); add(-3.46); add(36.17); add(31.76); add(-1.85); add(-1.45); }} );
		STRUCTURE.put("GTCGA", new ArrayList<Double>() {{add(5.64); add(-6.68); add(35.51); add(33.06); add(-1.63); add(5.38); }} );
		STRUCTURE.put("GTCGT", new ArrayList<Double>() {{add(5.34); add(-6.61); add(35.7); add(33.73); add(-2.42); add(5.01); }} );
		STRUCTURE.put("GTCGG", new ArrayList<Double>() {{add(5.54); add(-6.74); add(35.42); add(32.81); add(-1.52); add(3.41); }} );
		STRUCTURE.put("GTCGC", new ArrayList<Double>() {{add(5.56); add(-6.9); add(35.46); add(32.7); add(-1.39); add(4.64); }} );
		STRUCTURE.put("GTCCA", new ArrayList<Double>() {{add(5.19); add(-5.78); add(36.11); add(33.79); add(-2.08); add(-0.37); }} );
		STRUCTURE.put("GTCCT", new ArrayList<Double>() {{add(4.81); add(-5.39); add(36.03); add(34.49); add(-2.19); add(-0.96); }} );
		STRUCTURE.put("GTCCG", new ArrayList<Double>() {{add(5.08); add(-6.07); add(36.17); add(33.8); add(-1.73); add(-0.51); }} );
		STRUCTURE.put("GTCCC", new ArrayList<Double>() {{add(4.9); add(-5.28); add(35.97); add(33.99); add(-2.34); add(-0.93); }} );
		STRUCTURE.put("GGAAA", new ArrayList<Double>() {{add(4.63); add(-10.09); add(36.46); add(35.97); add(-1.41); add(-3.36); }} );
		STRUCTURE.put("GGAAT", new ArrayList<Double>() {{add(4.69); add(-9.61); add(35.99); add(35.6); add(-0.88); add(-3.73); }} );
		STRUCTURE.put("GGAAG", new ArrayList<Double>() {{add(4.63); add(-9.44); add(35.39); add(35.02); add(-0.61); add(-3.81); }} );
		STRUCTURE.put("GGAAC", new ArrayList<Double>() {{add(4.84); add(-9.91); add(36.02); add(35.48); add(-1.77); add(-3.78); }} );
		STRUCTURE.put("GGATA", new ArrayList<Double>() {{add(4.98); add(-7.47); add(36.04); add(32.31); add(-1.31); add(-4.17); }} );
		STRUCTURE.put("GGATT", new ArrayList<Double>() {{add(4.19); add(-9.67); add(36.57); add(33.31); add(-0.9); add(-5.81); }} );
		STRUCTURE.put("GGATG", new ArrayList<Double>() {{add(4.84); add(-7.65); add(36.16); add(32.03); add(-1.14); add(-4.71); }} );
		STRUCTURE.put("GGATC", new ArrayList<Double>() {{add(4.63); add(-7.56); add(36.29); add(32.41); add(-1.67); add(-4.63); }} );
		STRUCTURE.put("GGAGA", new ArrayList<Double>() {{add(4.92); add(-6.1); add(35.79); add(31.48); add(-2.03); add(-2.23); }} );
		STRUCTURE.put("GGAGT", new ArrayList<Double>() {{add(4.73); add(-6.75); add(35.76); add(31.81); add(-2.04); add(-2.84); }} );
		STRUCTURE.put("GGAGG", new ArrayList<Double>() {{add(4.93); add(-5.75); add(35.57); add(31.42); add(-1.91); add(-3.27); }} );
		STRUCTURE.put("GGAGC", new ArrayList<Double>() {{add(5.05); add(-6.07); add(35.75); add(31.38); add(-1.59); add(-2.75); }} );
		STRUCTURE.put("GGACA", new ArrayList<Double>() {{add(5.02); add(-7.71); add(36.22); add(34.09); add(-2.2); add(-2.88); }} );
		STRUCTURE.put("GGACT", new ArrayList<Double>() {{add(4.51); add(-7.34); add(35.78); add(34.97); add(-2.46); add(-4.26); }} );
		STRUCTURE.put("GGACG", new ArrayList<Double>() {{add(5.02); add(-8.11); add(36.13); add(34.01); add(-1.8); add(-2.49); }} );
		STRUCTURE.put("GGACC", new ArrayList<Double>() {{add(4.8); add(-7.55); add(35.8); add(34.71); add(-2.12); add(-3.32); }} );
		STRUCTURE.put("GGTAA", new ArrayList<Double>() {{add(5.43); add(-7.21); add(34.56); add(34.64); add(-3.71); add(5.01); }} );
		STRUCTURE.put("GGTAT", new ArrayList<Double>() {{add(5.46); add(-8.36); add(34.69); add(34.56); add(-2.59); add(5.39); }} );
		STRUCTURE.put("GGTAG", new ArrayList<Double>() {{add(5.55); add(-8.07); add(34.52); add(34.3); add(-2.63); add(4.24); }} );
		STRUCTURE.put("GGTAC", new ArrayList<Double>() {{add(5.84); add(-8.12); add(34.34); add(34.23); add(-2.37); add(4.32); }} );
		STRUCTURE.put("GGTTA", new ArrayList<Double>() {{add(4.87); add(-10.83); add(35.3); add(35.08); add(-3.26); add(-3.03); }} );
		STRUCTURE.put("GGTTT", new ArrayList<Double>() {{add(4.06); add(-12.41); add(36.2); add(36.34); add(-4.65); add(-3.75); }} );
		STRUCTURE.put("GGTTG", new ArrayList<Double>() {{add(4.82); add(-4.14); add(35.52); add(31.89); add(-3.18); add(-2.06); }} );
		STRUCTURE.put("GGTTC", new ArrayList<Double>() {{add(4.46); add(-10.08); add(35.19); add(35.45); add(-3.59); add(-3.83); }} );
		STRUCTURE.put("GGTGA", new ArrayList<Double>() {{add(5.4); add(-7.04); add(34.04); add(34.15); add(-3.21); add(3.82); }} );
		STRUCTURE.put("GGTGT", new ArrayList<Double>() {{add(5.31); add(-8.57); add(34.36); add(34.83); add(-2.95); add(3.95); }} );
		STRUCTURE.put("GGTGG", new ArrayList<Double>() {{add(5.56); add(-7.11); add(33.92); add(34.27); add(-2.55); add(2.46); }} );
		STRUCTURE.put("GGTGC", new ArrayList<Double>() {{add(5.56); add(-7.89); add(34.16); add(34.26); add(-2.69); add(2.97); }} );
		STRUCTURE.put("GGTCA", new ArrayList<Double>() {{add(4.94); add(-7.96); add(34.83); add(35.47); add(-3.46); add(-2.01); }} );
		STRUCTURE.put("GGTCT", new ArrayList<Double>() {{add(4.73); add(-8.04); add(34.74); add(36.1); add(-3.14); add(-1.9); }} );
		STRUCTURE.put("GGTCG", new ArrayList<Double>() {{add(4.82); add(-8.09); add(34.74); add(35.73); add(-3.5); add(-2.17); }} );
		STRUCTURE.put("GGTCC", new ArrayList<Double>() {{add(4.8); add(-7.55); add(34.71); add(35.8); add(-3.32); add(-2.12); }} );
		STRUCTURE.put("GGGAA", new ArrayList<Double>() {{add(4.73); add(-4.39); add(33.66); add(36.01); add(-1.61); add(-0.97); }} );
		STRUCTURE.put("GGGAT", new ArrayList<Double>() {{add(4.81); add(-4.91); add(33.66); add(36.09); add(-1.06); add(-1.49); }} );
		STRUCTURE.put("GGGAG", new ArrayList<Double>() {{add(4.85); add(-4.6); add(33.49); add(35.58); add(-1.0); add(-1.87); }} );
		STRUCTURE.put("GGGAC", new ArrayList<Double>() {{add(4.9); add(-5.28); add(33.99); add(35.97); add(-0.93); add(-2.34); }} );
		STRUCTURE.put("GGGTA", new ArrayList<Double>() {{add(4.71); add(-5.01); add(34.33); add(34.53); add(-1.79); add(-3.44); }} );
		STRUCTURE.put("GGGTT", new ArrayList<Double>() {{add(4.03); add(-7.68); add(34.89); add(35.8); add(-1.38); add(-4.18); }} );
		STRUCTURE.put("GGGTG", new ArrayList<Double>() {{add(4.77); add(-5.89); add(34.41); add(34.14); add(-1.08); add(-2.99); }} );
		STRUCTURE.put("GGGTC", new ArrayList<Double>() {{add(4.45); add(-5.33); add(34.36); add(34.81); add(-1.78); add(-3.44); }} );
		STRUCTURE.put("GGGGA", new ArrayList<Double>() {{add(4.76); add(-1.44); add(33.55); add(33.65); add(-2.33); add(-1.23); }} );
		STRUCTURE.put("GGGGT", new ArrayList<Double>() {{add(4.58); add(-2.37); add(33.44); add(34.28); add(-2.4); add(-1.8); }} );
		STRUCTURE.put("GGGGG", new ArrayList<Double>() {{add(4.75); add(-1.03); add(33.25); add(33.41); add(-2.24); add(-2.41); }} );
		STRUCTURE.put("GGGGC", new ArrayList<Double>() {{add(4.82); add(-2.13); add(33.56); add(33.68); add(-2.02); add(-1.96); }} );
		STRUCTURE.put("GGGCA", new ArrayList<Double>() {{add(5.02); add(-2.52); add(33.82); add(36.27); add(-1.78); add(-1.81); }} );
		STRUCTURE.put("GGGCT", new ArrayList<Double>() {{add(4.61); add(-2.48); add(33.51); add(37.3); add(-1.98); add(-2.95); }} );
		STRUCTURE.put("GGGCG", new ArrayList<Double>() {{add(4.96); add(-2.89); add(33.85); add(36.18); add(-1.63); add(-1.66); }} );
		STRUCTURE.put("GGGCC", new ArrayList<Double>() {{add(4.77); add(-1.93); add(33.57); add(36.61); add(-2.36); add(-2.4); }} );
		STRUCTURE.put("GGCAA", new ArrayList<Double>() {{add(5.3); add(-3.14); add(36.34); add(34.5); add(-2.26); add(3.97); }} );
		STRUCTURE.put("GGCAT", new ArrayList<Double>() {{add(5.33); add(-3.12); add(36.28); add(34.66); add(-1.93); add(4.26); }} );
		STRUCTURE.put("GGCAG", new ArrayList<Double>() {{add(5.41); add(-3.2); add(36.22); add(33.75); add(-1.34); add(3.38); }} );
		STRUCTURE.put("GGCAC", new ArrayList<Double>() {{add(5.66); add(-3.41); add(36.31); add(34.22); add(-1.35); add(3.05); }} );
		STRUCTURE.put("GGCTA", new ArrayList<Double>() {{add(5.0); add(-1.22); add(37.31); add(31.35); add(-2.39); add(-2.77); }} );
		STRUCTURE.put("GGCTT", new ArrayList<Double>() {{add(4.17); add(-1.55); add(37.74); add(32.49); add(-4.34); add(-3.7); }} );
		STRUCTURE.put("GGCTG", new ArrayList<Double>() {{add(4.95); add(-1.2); add(37.17); add(31.16); add(-2.29); add(-2.64); }} );
		STRUCTURE.put("GGCTC", new ArrayList<Double>() {{add(4.67); add(-0.53); add(37.09); add(31.39); add(-3.07); add(-2.76); }} );
		STRUCTURE.put("GGCGA", new ArrayList<Double>() {{add(5.4); add(-2.99); add(36.2); add(32.6); add(-1.65); add(3.27); }} );
		STRUCTURE.put("GGCGT", new ArrayList<Double>() {{add(5.08); add(-3.63); add(36.67); add(33.32); add(-2.09); add(3.67); }} );
		STRUCTURE.put("GGCGG", new ArrayList<Double>() {{add(5.29); add(-3.13); add(36.31); add(32.31); add(-2.02); add(1.79); }} );
		STRUCTURE.put("GGCGC", new ArrayList<Double>() {{add(5.41); add(-3.39); add(36.24); add(32.32); add(-1.71); add(2.85); }} );
		STRUCTURE.put("GGCCA", new ArrayList<Double>() {{add(4.93); add(-2.54); add(37.04); add(33.23); add(-2.42); add(-1.76); }} );
		STRUCTURE.put("GGCCT", new ArrayList<Double>() {{add(4.72); add(-2.2); add(36.64); add(33.71); add(-2.34); add(-1.89); }} );
		STRUCTURE.put("GGCCG", new ArrayList<Double>() {{add(4.96); add(-2.72); add(36.85); add(33.23); add(-2.0); add(-1.3); }} );
		STRUCTURE.put("GGCCC", new ArrayList<Double>() {{add(4.77); add(-1.93); add(36.61); add(33.57); add(-2.4); add(-2.36); }} );
		STRUCTURE.put("GCAAA", new ArrayList<Double>() {{add(5.41); add(-10.88); add(34.35); add(35.29); add(3.96); add(-3.68); }} );
		STRUCTURE.put("GCAAT", new ArrayList<Double>() {{add(5.15); add(-10.6); add(34.49); add(35.34); add(3.9); add(-2.92); }} );
		STRUCTURE.put("GCAAG", new ArrayList<Double>() {{add(5.45); add(-9.9); add(34.37); add(34.89); add(4.47); add(-3.39); }} );
		STRUCTURE.put("GCAAC", new ArrayList<Double>() {{add(5.7); add(-10.75); add(34.8); add(35.02); add(4.66); add(-2.98); }} );
		STRUCTURE.put("GCATA", new ArrayList<Double>() {{add(5.76); add(-8.11); add(34.78); add(31.62); add(5.2); add(-2.97); }} );
		STRUCTURE.put("GCATT", new ArrayList<Double>() {{add(4.89); add(-9.59); add(35.25); add(32.67); add(3.59); add(-5.22); }} );
		STRUCTURE.put("GCATG", new ArrayList<Double>() {{add(5.5); add(-9.43); add(34.82); add(31.54); add(4.69); add(-3.87); }} );
		STRUCTURE.put("GCATC", new ArrayList<Double>() {{add(5.38); add(-7.6); add(34.59); add(31.96); add(3.67); add(-4.24); }} );
		STRUCTURE.put("GCAGA", new ArrayList<Double>() {{add(5.75); add(-6.69); add(33.98); add(31.17); add(4.1); add(-1.2); }} );
		STRUCTURE.put("GCAGT", new ArrayList<Double>() {{add(5.4); add(-7.79); add(33.95); add(31.49); add(3.67); add(-1.42); }} );
		STRUCTURE.put("GCAGG", new ArrayList<Double>() {{add(5.52); add(-5.92); add(33.94); add(31.18); add(2.96); add(-3.15); }} );
		STRUCTURE.put("GCAGC", new ArrayList<Double>() {{add(5.67); add(-6.76); add(34.49); add(31.29); add(3.55); add(-2.09); }} );
		STRUCTURE.put("GCACA", new ArrayList<Double>() {{add(5.94); add(-8.3); add(35.01); add(33.55); add(3.31); add(-1.85); }} );
		STRUCTURE.put("GCACT", new ArrayList<Double>() {{add(5.3); add(-7.68); add(33.99); add(34.58); add(2.74); add(-3.54); }} );
		STRUCTURE.put("GCACG", new ArrayList<Double>() {{add(5.74); add(-8.4); add(34.75); add(33.78); add(3.22); add(-2.27); }} );
		STRUCTURE.put("GCACC", new ArrayList<Double>() {{add(5.56); add(-7.89); add(34.26); add(34.16); add(2.97); add(-2.69); }} );
		STRUCTURE.put("GCTAA", new ArrayList<Double>() {{add(5.82); add(-6.12); add(31.43); add(34.68); add(-2.69); add(5.63); }} );
		STRUCTURE.put("GCTAT", new ArrayList<Double>() {{add(5.37); add(-6.84); add(31.21); add(34.96); add(-2.59); add(4.96); }} );
		STRUCTURE.put("GCTAG", new ArrayList<Double>() {{add(5.67); add(-6.54); add(31.36); add(34.66); add(-2.24); add(4.03); }} );
		STRUCTURE.put("GCTAC", new ArrayList<Double>() {{add(5.88); add(-6.96); add(31.25); add(34.48); add(-2.28); add(4.45); }} );
		STRUCTURE.put("GCTTA", new ArrayList<Double>() {{add(5.01); add(-8.92); add(32.17); add(34.85); add(-2.56); add(-2.87); }} );
		STRUCTURE.put("GCTTT", new ArrayList<Double>() {{add(4.03); add(-10.58); add(32.81); add(36.56); add(-3.95); add(-5.07); }} );
		STRUCTURE.put("GCTTG", new ArrayList<Double>() {{add(4.94); add(-8.63); add(32.11); add(34.57); add(-2.39); add(-3.44); }} );
		STRUCTURE.put("GCTTC", new ArrayList<Double>() {{add(4.82); add(-8.01); add(32.06); add(34.96); add(-2.79); add(-3.36); }} );
		STRUCTURE.put("GCTGA", new ArrayList<Double>() {{add(5.74); add(-6.36); add(31.14); add(34.29); add(-2.0); add(4.93); }} );
		STRUCTURE.put("GCTGT", new ArrayList<Double>() {{add(5.31); add(-7.34); add(31.35); add(35.03); add(-2.1); add(5.07); }} );
		STRUCTURE.put("GCTGG", new ArrayList<Double>() {{add(5.53); add(-5.87); add(30.94); add(34.09); add(-2.45); add(2.75); }} );
		STRUCTURE.put("GCTGC", new ArrayList<Double>() {{add(5.67); add(-6.76); add(31.29); add(34.49); add(-2.09); add(3.55); }} );
		STRUCTURE.put("GCTCA", new ArrayList<Double>() {{add(5.4); add(-6.75); add(31.45); add(34.81); add(-2.11); add(-0.9); }} );
		STRUCTURE.put("GCTCT", new ArrayList<Double>() {{add(5.04); add(-6.5); add(31.62); add(35.84); add(-2.48); add(-1.41); }} );
		STRUCTURE.put("GCTCG", new ArrayList<Double>() {{add(5.2); add(-6.78); add(31.41); add(35.31); add(-2.22); add(-1.22); }} );
		STRUCTURE.put("GCTCC", new ArrayList<Double>() {{add(5.05); add(-6.07); add(31.38); add(35.75); add(-2.75); add(-1.59); }} );
		STRUCTURE.put("GCGAA", new ArrayList<Double>() {{add(5.53); add(-6.08); add(32.71); add(35.53); add(3.79); add(-0.23); }} );
		STRUCTURE.put("GCGAT", new ArrayList<Double>() {{add(5.28); add(-6.96); add(32.49); add(35.82); add(4.15); add(-0.66); }} );
		STRUCTURE.put("GCGAG", new ArrayList<Double>() {{add(5.46); add(-6.31); add(32.59); add(35.13); add(4.38); add(-1.17); }} );
		STRUCTURE.put("GCGAC", new ArrayList<Double>() {{add(5.56); add(-6.9); add(32.7); add(35.46); add(4.64); add(-1.39); }} );
		STRUCTURE.put("GCGTA", new ArrayList<Double>() {{add(5.32); add(-7.22); add(33.14); add(33.93); add(4.31); add(-2.07); }} );
		STRUCTURE.put("GCGTT", new ArrayList<Double>() {{add(4.64); add(-9.91); add(33.41); add(35.06); add(4.64); add(-2.93); }} );
		STRUCTURE.put("GCGTG", new ArrayList<Double>() {{add(5.25); add(-7.98); add(33.34); add(33.73); add(4.71); add(-2.23); }} );
		STRUCTURE.put("GCGTC", new ArrayList<Double>() {{add(5.09); add(-7.38); add(33.02); add(34.21); add(4.23); add(-2.45); }} );
		STRUCTURE.put("GCGGA", new ArrayList<Double>() {{add(5.27); add(-3.15); add(32.42); add(33.51); add(2.4); add(-0.89); }} );
		STRUCTURE.put("GCGGT", new ArrayList<Double>() {{add(5.08); add(-4.55); add(32.31); add(34.31); add(2.79); add(-0.62); }} );
		STRUCTURE.put("GCGGG", new ArrayList<Double>() {{add(5.3); add(-2.75); add(32.3); add(33.41); add(2.34); add(-1.73); }} );
		STRUCTURE.put("GCGGC", new ArrayList<Double>() {{add(5.37); add(-3.73); add(32.46); add(33.44); add(2.78); add(-1.31); }} );
		STRUCTURE.put("GCGCA", new ArrayList<Double>() {{add(5.71); add(-3.94); add(32.79); add(35.52); add(3.54); add(-0.67); }} );
		STRUCTURE.put("GCGCT", new ArrayList<Double>() {{add(5.18); add(-3.87); add(32.08); add(36.76); add(2.89); add(-1.92); }} );
		STRUCTURE.put("GCGCG", new ArrayList<Double>() {{add(5.54); add(-4.22); add(32.44); add(35.7); add(3.12); add(-1.15); }} );
		STRUCTURE.put("GCGCC", new ArrayList<Double>() {{add(5.41); add(-3.39); add(32.32); add(36.24); add(2.85); add(-1.71); }} );
		STRUCTURE.put("GCCAA", new ArrayList<Double>() {{add(5.56); add(-3.85); add(33.0); add(34.72); add(-1.23); add(4.45); }} );
		STRUCTURE.put("GCCAT", new ArrayList<Double>() {{add(5.23); add(-3.12); add(33.39); add(34.93); add(-2.18); add(3.68); }} );
		STRUCTURE.put("GCCAG", new ArrayList<Double>() {{add(5.43); add(-3.34); add(33.53); add(34.28); add(-1.52); add(2.79); }} );
		STRUCTURE.put("GCCAC", new ArrayList<Double>() {{add(5.46); add(-4.03); add(33.11); add(34.2); add(-1.52); add(3.09); }} );
		STRUCTURE.put("GCCTA", new ArrayList<Double>() {{add(4.97); add(-1.23); add(34.04); add(31.51); add(-1.96); add(-2.47); }} );
		STRUCTURE.put("GCCTT", new ArrayList<Double>() {{add(4.51); add(-1.5); add(34.57); add(32.34); add(-2.62); add(-2.75); }} );
		STRUCTURE.put("GCCTG", new ArrayList<Double>() {{add(4.94); add(-1.28); add(33.99); add(31.14); add(-1.61); add(-2.26); }} );
		STRUCTURE.put("GCCTC", new ArrayList<Double>() {{add(4.85); add(-0.7); add(33.92); add(31.38); add(-2.18); add(-2.42); }} );
		STRUCTURE.put("GCCGA", new ArrayList<Double>() {{add(5.43); add(-3.4); add(32.95); add(32.47); add(-0.99); add(3.91); }} );
		STRUCTURE.put("GCCGT", new ArrayList<Double>() {{add(5.08); add(-3.97); add(33.79); add(33.55); add(-1.58); add(3.91); }} );
		STRUCTURE.put("GCCGG", new ArrayList<Double>() {{add(5.36); add(-3.58); add(33.41); add(32.61); add(-1.33); add(2.08); }} );
		STRUCTURE.put("GCCGC", new ArrayList<Double>() {{add(5.37); add(-3.73); add(33.44); add(32.46); add(-1.31); add(2.78); }} );
		STRUCTURE.put("GCCCA", new ArrayList<Double>() {{add(4.96); add(-2.77); add(33.75); add(33.45); add(-1.99); add(-2.07); }} );
		STRUCTURE.put("GCCCT", new ArrayList<Double>() {{add(4.73); add(-1.88); add(33.54); add(34.0); add(-2.16); add(-2.27); }} );
		STRUCTURE.put("GCCCG", new ArrayList<Double>() {{add(4.95); add(-2.73); add(33.78); add(33.44); add(-1.71); add(-1.7); }} );
		STRUCTURE.put("GCCCC", new ArrayList<Double>() {{add(4.82); add(-2.13); add(33.68); add(33.56); add(-1.96); add(-2.02); }} );
		STRUCTURE.put("CAAAA", new ArrayList<Double>() {{add(4.76); add(-13.79); add(35.51); add(36.92); add(-2.19); add(-2.76); }} );
		STRUCTURE.put("CAAAT", new ArrayList<Double>() {{add(4.12); add(-13.36); add(35.49); add(37.28); add(-3.54); add(-4.68); }} );
		STRUCTURE.put("CAAAG", new ArrayList<Double>() {{add(4.52); add(-12.85); add(35.41); add(36.3); add(-2.8); add(-4.84); }} );
		STRUCTURE.put("CAAAC", new ArrayList<Double>() {{add(4.95); add(-13.1); add(35.28); add(36.33); add(-2.42); add(-3.27); }} );
		STRUCTURE.put("CAATA", new ArrayList<Double>() {{add(5.02); add(-9.7); add(35.12); add(32.4); add(-2.87); add(-3.9); }} );
		STRUCTURE.put("CAATT", new ArrayList<Double>() {{add(4.24); add(-11.93); add(35.62); add(33.37); add(-2.97); add(-5.32); }} );
		STRUCTURE.put("CAATG", new ArrayList<Double>() {{add(4.69); add(-10.42); add(35.08); add(32.41); add(-2.63); add(-4.37); }} );
		STRUCTURE.put("CAATC", new ArrayList<Double>() {{add(4.81); add(-10.33); add(34.92); add(32.75); add(-2.12); add(-4.28); }} );
		STRUCTURE.put("CAAGA", new ArrayList<Double>() {{add(4.92); add(-9.12); add(34.77); add(32.47); add(-3.37); add(-1.59); }} );
		STRUCTURE.put("CAAGT", new ArrayList<Double>() {{add(4.52); add(-10.21); add(34.94); add(33.13); add(-3.59); add(-2.35); }} );
		STRUCTURE.put("CAAGG", new ArrayList<Double>() {{add(4.65); add(-9.16); add(34.98); add(32.51); add(-3.66); add(-2.93); }} );
		STRUCTURE.put("CAAGC", new ArrayList<Double>() {{add(4.94); add(-8.63); add(34.57); add(32.11); add(-3.44); add(-2.39); }} );
		STRUCTURE.put("CAACA", new ArrayList<Double>() {{add(5.24); add(-11.69); add(35.19); add(34.7); add(-2.78); add(-2.5); }} );
		STRUCTURE.put("CAACT", new ArrayList<Double>() {{add(4.38); add(-11.29); add(34.76); add(36.24); add(-3.88); add(-4.47); }} );
		STRUCTURE.put("CAACG", new ArrayList<Double>() {{add(4.98); add(-11.71); add(35.01); add(35.1); add(-2.97); add(-2.83); }} );
		STRUCTURE.put("CAACC", new ArrayList<Double>() {{add(4.82); add(-4.14); add(31.89); add(35.52); add(-2.06); add(-3.18); }} );
		STRUCTURE.put("CATAA", new ArrayList<Double>() {{add(6.02); add(-8.06); add(31.48); add(34.54); add(-3.28); add(7.92); }} );
		STRUCTURE.put("CATAT", new ArrayList<Double>() {{add(5.32); add(-8.33); add(32.04); add(35.37); add(-4.26); add(5.85); }} );
		STRUCTURE.put("CATAG", new ArrayList<Double>() {{add(5.53); add(-8.2); add(31.84); add(35.03); add(-3.65); add(5.41); }} );
		STRUCTURE.put("CATAC", new ArrayList<Double>() {{add(5.93); add(-8.42); add(31.46); add(34.49); add(-2.99); add(6.01); }} );
		STRUCTURE.put("CATTA", new ArrayList<Double>() {{add(4.9); add(-10.52); add(32.4); add(35.43); add(-4.86); add(-2.15); }} );
		STRUCTURE.put("CATTT", new ArrayList<Double>() {{add(3.84); add(-11.76); add(33.42); add(36.86); add(-7.0); add(-4.21); }} );
		STRUCTURE.put("CATTG", new ArrayList<Double>() {{add(4.69); add(-10.42); add(32.41); add(35.08); add(-4.37); add(-2.63); }} );
		STRUCTURE.put("CATTC", new ArrayList<Double>() {{add(4.39); add(-9.43); add(32.69); add(35.63); add(-5.91); add(-3.64); }} );
		STRUCTURE.put("CATGA", new ArrayList<Double>() {{add(5.27); add(-9.26); add(31.75); add(35.0); add(-4.44); add(5.91); }} );
		STRUCTURE.put("CATGT", new ArrayList<Double>() {{add(4.99); add(-10.15); add(32.0); add(35.7); add(-4.67); add(5.73); }} );
		STRUCTURE.put("CATGG", new ArrayList<Double>() {{add(5.34); add(-9.01); add(31.61); add(34.66); add(-4.03); add(3.95); }} );
		STRUCTURE.put("CATGC", new ArrayList<Double>() {{add(5.5); add(-9.43); add(31.54); add(34.82); add(-3.87); add(4.69); }} );
		STRUCTURE.put("CATCA", new ArrayList<Double>() {{add(5.22); add(-8.13); add(31.93); add(35.36); add(-3.98); add(-0.46); }} );
		STRUCTURE.put("CATCT", new ArrayList<Double>() {{add(4.66); add(-7.85); add(32.25); add(36.48); add(-5.19); add(-1.27); }} );
		STRUCTURE.put("CATCG", new ArrayList<Double>() {{add(4.94); add(-8.32); add(32.14); add(35.83); add(-4.47); add(-0.95); }} );
		STRUCTURE.put("CATCC", new ArrayList<Double>() {{add(4.84); add(-7.65); add(32.03); add(36.16); add(-4.71); add(-1.14); }} );
		STRUCTURE.put("CAGAA", new ArrayList<Double>() {{add(5.14); add(-3.52); add(31.59); add(36.46); add(-1.9); add(-0.54); }} );
		STRUCTURE.put("CAGAT", new ArrayList<Double>() {{add(4.98); add(-2.7); add(31.12); add(36.37); add(-2.27); add(-0.33); }} );
		STRUCTURE.put("CAGAG", new ArrayList<Double>() {{add(5.14); add(-3.06); add(31.18); add(35.6); add(-1.81); add(-1.0); }} );
		STRUCTURE.put("CAGAC", new ArrayList<Double>() {{add(5.22); add(-3.97); add(31.42); add(36.44); add(-1.18); add(-1.52); }} );
		STRUCTURE.put("CAGTA", new ArrayList<Double>() {{add(5.13); add(-4.2); add(31.52); add(34.47); add(-1.74); add(-2.15); }} );
		STRUCTURE.put("CAGTT", new ArrayList<Double>() {{add(4.49); add(-5.42); add(31.73); add(35.45); add(-1.85); add(-3.28); }} );
		STRUCTURE.put("CAGTG", new ArrayList<Double>() {{add(4.98); add(-4.52); add(31.61); add(34.3); add(-1.18); add(-2.39); }} );
		STRUCTURE.put("CAGTC", new ArrayList<Double>() {{add(5.0); add(-3.96); add(31.46); add(34.65); add(-1.4); add(-2.08); }} );
		STRUCTURE.put("CAGGA", new ArrayList<Double>() {{add(4.92); add(-0.9); add(31.21); add(34.22); add(-2.79); add(-1.05); }} );
		STRUCTURE.put("CAGGT", new ArrayList<Double>() {{add(4.54); add(-1.68); add(31.38); add(35.08); add(-2.97); add(-1.63); }} );
		STRUCTURE.put("CAGGG", new ArrayList<Double>() {{add(4.85); add(-0.14); add(31.11); add(34.07); add(-3.39); add(-2.88); }} );
		STRUCTURE.put("CAGGC", new ArrayList<Double>() {{add(4.94); add(-1.28); add(31.14); add(33.99); add(-2.26); add(-1.61); }} );
		STRUCTURE.put("CAGCA", new ArrayList<Double>() {{add(5.27); add(-1.72); add(31.23); add(36.65); add(-1.86); add(-1.51); }} );
		STRUCTURE.put("CAGCT", new ArrayList<Double>() {{add(4.8); add(-1.38); add(31.01); add(37.48); add(-2.54); add(-2.49); }} );
		STRUCTURE.put("CAGCG", new ArrayList<Double>() {{add(5.13); add(-1.72); add(31.2); add(36.67); add(-1.93); add(-1.69); }} );
		STRUCTURE.put("CAGCC", new ArrayList<Double>() {{add(4.95); add(-1.2); add(31.16); add(37.17); add(-2.64); add(-2.29); }} );
		STRUCTURE.put("CACAA", new ArrayList<Double>() {{add(5.2); add(-8.23); add(33.59); add(35.72); add(-2.4); add(6.07); }} );
		STRUCTURE.put("CACAT", new ArrayList<Double>() {{add(5.15); add(-8.83); add(33.53); add(35.86); add(-2.22); add(6.57); }} );
		STRUCTURE.put("CACAG", new ArrayList<Double>() {{add(5.32); add(-8.12); add(33.74); add(35.2); add(-2.15); add(4.99); }} );
		STRUCTURE.put("CACAC", new ArrayList<Double>() {{add(5.73); add(-8.16); add(33.35); add(35.12); add(-1.37); add(5.01); }} );
		STRUCTURE.put("CACTA", new ArrayList<Double>() {{add(5.11); add(-4.24); add(34.28); add(31.7); add(-2.73); add(-1.38); }} );
		STRUCTURE.put("CACTT", new ArrayList<Double>() {{add(4.14); add(-4.48); add(34.91); add(33.1); add(-4.51); add(-2.79); }} );
		STRUCTURE.put("CACTG", new ArrayList<Double>() {{add(4.98); add(-4.52); add(34.3); add(31.61); add(-2.39); add(-1.18); }} );
		STRUCTURE.put("CACTC", new ArrayList<Double>() {{add(4.78); add(-3.31); add(34.12); add(31.79); add(-3.21); add(-1.98); }} );
		STRUCTURE.put("CACGA", new ArrayList<Double>() {{add(5.29); add(-7.95); add(33.77); add(33.99); add(-2.18); add(5.8); }} );
		STRUCTURE.put("CACGT", new ArrayList<Double>() {{add(4.85); add(-8.78); add(34.05); add(34.48); add(-3.13); add(5.64); }} );
		STRUCTURE.put("CACGG", new ArrayList<Double>() {{add(5.1); add(-8.46); add(33.81); add(33.7); add(-2.43); add(4.06); }} );
		STRUCTURE.put("CACGC", new ArrayList<Double>() {{add(5.25); add(-7.98); add(33.73); add(33.34); add(-2.23); add(4.71); }} );
		STRUCTURE.put("CACCA", new ArrayList<Double>() {{add(4.84); add(-7.0); add(34.27); add(34.81); add(-2.83); add(-0.68); }} );
		STRUCTURE.put("CACCT", new ArrayList<Double>() {{add(4.42); add(-6.64); add(34.31); add(35.25); add(-3.26); add(-1.65); }} );
		STRUCTURE.put("CACCG", new ArrayList<Double>() {{add(4.92); add(-6.56); add(34.05); add(34.36); add(-2.48); add(-0.28); }} );
		STRUCTURE.put("CACCC", new ArrayList<Double>() {{add(4.77); add(-5.89); add(34.14); add(34.41); add(-2.99); add(-1.08); }} );
		STRUCTURE.put("CTAAA", new ArrayList<Double>() {{add(5.82); add(-11.18); add(34.35); add(35.72); add(6.08); add(-1.41); }} );
		STRUCTURE.put("CTAAT", new ArrayList<Double>() {{add(5.57); add(-11.58); add(34.46); add(35.33); add(7.71); add(-1.62); }} );
		STRUCTURE.put("CTAAG", new ArrayList<Double>() {{add(5.49); add(-10.31); add(34.71); add(35.04); add(6.02); add(-3.25); }} );
		STRUCTURE.put("CTAAC", new ArrayList<Double>() {{add(5.8); add(-10.65); add(34.54); add(35.18); add(5.73); add(-2.89); }} );
		STRUCTURE.put("CTATA", new ArrayList<Double>() {{add(5.79); add(-7.69); add(34.63); add(31.96); add(5.66); add(-3.1); }} );
		STRUCTURE.put("CTATT", new ArrayList<Double>() {{add(4.65); add(-11.61); add(35.89); add(33.54); add(3.47); add(-5.98); }} );
		STRUCTURE.put("CTATG", new ArrayList<Double>() {{add(5.53); add(-8.2); add(35.03); add(31.84); add(5.41); add(-3.65); }} );
		STRUCTURE.put("CTATC", new ArrayList<Double>() {{add(5.54); add(-8.88); add(33.89); add(32.43); add(6.28); add(-4.19); }} );
		STRUCTURE.put("CTAGA", new ArrayList<Double>() {{add(5.84); add(-6.98); add(34.35); add(31.57); add(5.27); add(-1.22); }} );
		STRUCTURE.put("CTAGT", new ArrayList<Double>() {{add(5.13); add(-7.44); add(35.0); add(32.22); add(3.55); add(-2.86); }} );
		STRUCTURE.put("CTAGG", new ArrayList<Double>() {{add(5.69); add(-6.51); add(34.19); add(31.47); add(4.92); add(-2.23); }} );
		STRUCTURE.put("CTAGC", new ArrayList<Double>() {{add(5.67); add(-6.54); add(34.66); add(31.36); add(4.03); add(-2.24); }} );
		STRUCTURE.put("CTACA", new ArrayList<Double>() {{add(5.74); add(-8.6); add(35.06); add(33.91); add(3.24); add(-3.23); }} );
		STRUCTURE.put("CTACT", new ArrayList<Double>() {{add(5.2); add(-8.16); add(34.13); add(35.18); add(4.64); add(-4.12); }} );
		STRUCTURE.put("CTACG", new ArrayList<Double>() {{add(5.77); add(-8.43); add(34.25); add(34.02); add(4.93); add(-2.16); }} );
		STRUCTURE.put("CTACC", new ArrayList<Double>() {{add(5.55); add(-8.07); add(34.3); add(34.52); add(4.24); add(-2.63); }} );
		STRUCTURE.put("CTTAA", new ArrayList<Double>() {{add(5.58); add(-9.57); add(34.68); add(34.81); add(-3.71); add(6.23); }} );
		STRUCTURE.put("CTTAT", new ArrayList<Double>() {{add(5.48); add(-10.54); add(35.03); add(35.15); add(-3.47); add(6.31); }} );
		STRUCTURE.put("CTTAG", new ArrayList<Double>() {{add(5.49); add(-10.31); add(35.04); add(34.71); add(-3.25); add(6.02); }} );
		STRUCTURE.put("CTTAC", new ArrayList<Double>() {{add(5.62); add(-10.31); add(34.87); add(34.34); add(-3.24); add(5.9); }} );
		STRUCTURE.put("CTTTA", new ArrayList<Double>() {{add(4.7); add(-12.85); add(36.11); add(35.7); add(-3.87); add(-1.56); }} );
		STRUCTURE.put("CTTTT", new ArrayList<Double>() {{add(3.68); add(-14.68); add(37.18); add(37.02); add(-6.47); add(-4.23); }} );
		STRUCTURE.put("CTTTG", new ArrayList<Double>() {{add(4.52); add(-12.85); add(36.3); add(35.41); add(-4.84); add(-2.8); }} );
		STRUCTURE.put("CTTTC", new ArrayList<Double>() {{add(4.36); add(-11.93); add(36.1); add(35.91); add(-4.95); add(-3.0); }} );
		STRUCTURE.put("CTTGA", new ArrayList<Double>() {{add(5.4); add(-9.98); add(34.76); add(34.53); add(-3.44); add(5.51); }} );
		STRUCTURE.put("CTTGT", new ArrayList<Double>() {{add(5.19); add(-10.72); add(35.05); add(35.43); add(-3.95); add(5.65); }} );
		STRUCTURE.put("CTTGG", new ArrayList<Double>() {{add(5.31); add(-9.71); add(34.56); add(34.61); add(-3.4); add(4.48); }} );
		STRUCTURE.put("CTTGC", new ArrayList<Double>() {{add(5.45); add(-9.9); add(34.89); add(34.37); add(-3.39); add(4.47); }} );
		STRUCTURE.put("CTTCA", new ArrayList<Double>() {{add(5.15); add(-10.24); add(35.14); add(35.1); add(-3.01); add(0.22); }} );
		STRUCTURE.put("CTTCT", new ArrayList<Double>() {{add(4.5); add(-10.3); add(35.47); add(36.79); add(-4.1); add(-1.65); }} );
		STRUCTURE.put("CTTCG", new ArrayList<Double>() {{add(4.92); add(-9.8); add(35.11); add(35.23); add(-3.7); add(-0.16); }} );
		STRUCTURE.put("CTTCC", new ArrayList<Double>() {{add(4.63); add(-9.44); add(35.02); add(35.39); add(-3.81); add(-0.61); }} );
		STRUCTURE.put("CTGAA", new ArrayList<Double>() {{add(5.93); add(-5.95); add(34.21); add(35.08); add(5.37); add(0.57); }} );
		STRUCTURE.put("CTGAT", new ArrayList<Double>() {{add(5.34); add(-6.55); add(34.24); add(35.8); add(4.36); add(-0.36); }} );
		STRUCTURE.put("CTGAG", new ArrayList<Double>() {{add(5.56); add(-6.08); add(34.47); add(34.84); add(4.27); add(-0.89); }} );
		STRUCTURE.put("CTGAC", new ArrayList<Double>() {{add(5.67); add(-6.43); add(34.37); add(35.2); add(4.57); add(-1.29); }} );
		STRUCTURE.put("CTGTA", new ArrayList<Double>() {{add(5.41); add(-6.49); add(34.82); add(33.82); add(3.96); add(-1.95); }} );
		STRUCTURE.put("CTGTT", new ArrayList<Double>() {{add(4.95); add(-9.26); add(34.91); add(34.88); add(5.23); add(-2.4); }} );
		STRUCTURE.put("CTGTG", new ArrayList<Double>() {{add(5.32); add(-8.12); add(35.2); add(33.74); add(4.99); add(-2.15); }} );
		STRUCTURE.put("CTGTC", new ArrayList<Double>() {{add(5.26); add(-6.88); add(34.79); add(34.25); add(3.98); add(-2.46); }} );
		STRUCTURE.put("CTGGA", new ArrayList<Double>() {{add(5.31); add(-2.98); add(33.98); add(33.74); add(2.8); add(-0.76); }} );
		STRUCTURE.put("CTGGT", new ArrayList<Double>() {{add(5.19); add(-4.44); add(34.21); add(34.39); add(3.13); add(-0.64); }} );
		STRUCTURE.put("CTGGG", new ArrayList<Double>() {{add(5.39); add(-2.47); add(33.88); add(33.54); add(2.42); add(-1.97); }} );
		STRUCTURE.put("CTGGC", new ArrayList<Double>() {{add(5.43); add(-3.34); add(34.28); add(33.53); add(2.79); add(-1.52); }} );
		STRUCTURE.put("CTGCA", new ArrayList<Double>() {{add(5.79); add(-3.68); add(34.5); add(35.64); add(3.43); add(-0.7); }} );
		STRUCTURE.put("CTGCT", new ArrayList<Double>() {{add(5.16); add(-3.6); add(33.86); add(36.97); add(3.76); add(-2.72); }} );
		STRUCTURE.put("CTGCG", new ArrayList<Double>() {{add(5.67); add(-3.85); add(34.24); add(35.64); add(3.64); add(-0.89); }} );
		STRUCTURE.put("CTGCC", new ArrayList<Double>() {{add(5.41); add(-3.2); add(33.75); add(36.22); add(3.38); add(-1.34); }} );
		STRUCTURE.put("CTCAA", new ArrayList<Double>() {{add(5.9); add(-6.36); add(34.56); add(34.65); add(-0.68); add(5.89); }} );
		STRUCTURE.put("CTCAT", new ArrayList<Double>() {{add(5.23); add(-6.05); add(35.1); add(34.93); add(-1.52); add(5.69); }} );
		STRUCTURE.put("CTCAG", new ArrayList<Double>() {{add(5.56); add(-6.08); add(34.84); add(34.47); add(-0.89); add(4.27); }} );
		STRUCTURE.put("CTCAC", new ArrayList<Double>() {{add(5.8); add(-6.5); add(34.92); add(34.48); add(-0.77); add(4.33); }} );
		STRUCTURE.put("CTCTA", new ArrayList<Double>() {{add(5.41); add(-3.22); add(35.31); add(31.4); add(-0.44); add(-1.35); }} );
		STRUCTURE.put("CTCTT", new ArrayList<Double>() {{add(4.68); add(-3.22); add(35.92); add(32.31); add(-1.36); add(-1.76); }} );
		STRUCTURE.put("CTCTG", new ArrayList<Double>() {{add(5.14); add(-3.06); add(35.6); add(31.18); add(-1.0); add(-1.81); }} );
		STRUCTURE.put("CTCTC", new ArrayList<Double>() {{add(4.93); add(-2.54); add(35.65); add(31.39); add(-1.9); add(-1.93); }} );
		STRUCTURE.put("CTCGA", new ArrayList<Double>() {{add(5.42); add(-6.72); add(35.17); add(33.01); add(-1.2); add(5.47); }} );
		STRUCTURE.put("CTCGT", new ArrayList<Double>() {{add(5.13); add(-6.5); add(35.94); add(33.69); add(-2.01); add(4.47); }} );
		STRUCTURE.put("CTCGG", new ArrayList<Double>() {{add(5.65); add(-6.12); add(34.73); add(32.31); add(-0.49); add(4.6); }} );
		STRUCTURE.put("CTCGC", new ArrayList<Double>() {{add(5.46); add(-6.31); add(35.13); add(32.59); add(-1.17); add(4.38); }} );
		STRUCTURE.put("CTCCA", new ArrayList<Double>() {{add(5.02); add(-5.45); add(35.8); add(33.85); add(-1.81); add(-0.86); }} );
		STRUCTURE.put("CTCCT", new ArrayList<Double>() {{add(4.63); add(-4.81); add(36.23); add(34.13); add(-2.62); add(-1.32); }} );
		STRUCTURE.put("CTCCG", new ArrayList<Double>() {{add(5.01); add(-5.07); add(35.79); add(33.51); add(-1.83); add(-0.61); }} );
		STRUCTURE.put("CTCCC", new ArrayList<Double>() {{add(4.85); add(-4.6); add(35.58); add(33.49); add(-1.87); add(-1.0); }} );
		STRUCTURE.put("CGAAA", new ArrayList<Double>() {{add(4.98); add(-10.58); add(35.62); add(35.88); add(-0.38); add(-2.61); }} );
		STRUCTURE.put("CGAAT", new ArrayList<Double>() {{add(4.88); add(-10.56); add(35.52); add(35.86); add(-0.24); add(-2.98); }} );
		STRUCTURE.put("CGAAG", new ArrayList<Double>() {{add(4.92); add(-9.8); add(35.23); add(35.11); add(-0.16); add(-3.7); }} );
		STRUCTURE.put("CGAAC", new ArrayList<Double>() {{add(5.1); add(-10.48); add(35.61); add(35.58); add(-0.26); add(-3.31); }} );
		STRUCTURE.put("CGATA", new ArrayList<Double>() {{add(5.32); add(-7.63); add(35.68); add(31.89); add(-0.47); add(-3.35); }} );
		STRUCTURE.put("CGATT", new ArrayList<Double>() {{add(4.56); add(-9.51); add(36.19); add(32.89); add(-0.8); add(-4.9); }} );
		STRUCTURE.put("CGATG", new ArrayList<Double>() {{add(4.94); add(-8.32); add(35.83); add(32.14); add(-0.95); add(-4.47); }} );
		STRUCTURE.put("CGATC", new ArrayList<Double>() {{add(4.96); add(-7.79); add(35.54); add(32.29); add(-0.57); add(-4.15); }} );
		STRUCTURE.put("CGAGA", new ArrayList<Double>() {{add(5.17); add(-6.8); add(35.18); add(31.51); add(-1.21); add(-1.9); }} );
		STRUCTURE.put("CGAGT", new ArrayList<Double>() {{add(4.93); add(-7.78); add(35.21); add(31.96); add(-1.04); add(-1.9); }} );
		STRUCTURE.put("CGAGG", new ArrayList<Double>() {{add(5.18); add(-6.32); add(34.97); add(31.5); add(-1.26); add(-2.8); }} );
		STRUCTURE.put("CGAGC", new ArrayList<Double>() {{add(5.2); add(-6.78); add(35.31); add(31.41); add(-1.22); add(-2.22); }} );
		STRUCTURE.put("CGACA", new ArrayList<Double>() {{add(5.28); add(-8.25); add(35.73); add(34.23); add(-1.7); add(-2.39); }} );
		STRUCTURE.put("CGACT", new ArrayList<Double>() {{add(4.59); add(-8.15); add(35.38); add(35.27); add(-2.31); add(-4.46); }} );
		STRUCTURE.put("CGACG", new ArrayList<Double>() {{add(5.19); add(-8.49); add(35.57); add(34.26); add(-1.47); add(-2.51); }} );
		STRUCTURE.put("CGACC", new ArrayList<Double>() {{add(4.82); add(-8.09); add(35.73); add(34.74); add(-2.17); add(-3.5); }} );
		STRUCTURE.put("CGTAA", new ArrayList<Double>() {{add(6.11); add(-7.71); add(33.58); add(34.26); add(-1.95); add(6.35); }} );
		STRUCTURE.put("CGTAT", new ArrayList<Double>() {{add(5.53); add(-9.05); add(34.16); add(34.59); add(-2.22); add(5.89); }} );
		STRUCTURE.put("CGTAG", new ArrayList<Double>() {{add(5.77); add(-8.43); add(34.02); add(34.25); add(-2.16); add(4.93); }} );
		STRUCTURE.put("CGTAC", new ArrayList<Double>() {{add(5.93); add(-8.84); add(33.97); add(34.2); add(-2.11); add(4.68); }} );
		STRUCTURE.put("CGTTA", new ArrayList<Double>() {{add(5.15); add(-11.31); add(34.89); add(35.27); add(-2.83); add(-2.63); }} );
		STRUCTURE.put("CGTTT", new ArrayList<Double>() {{add(4.43); add(-13.15); add(35.48); add(36.57); add(-3.27); add(-2.94); }} );
		STRUCTURE.put("CGTTG", new ArrayList<Double>() {{add(4.98); add(-11.71); add(35.1); add(35.01); add(-2.83); add(-2.97); }} );
		STRUCTURE.put("CGTTC", new ArrayList<Double>() {{add(4.74); add(-11.06); add(34.9); add(35.71); add(-2.93); add(-2.93); }} );
		STRUCTURE.put("CGTGA", new ArrayList<Double>() {{add(5.71); add(-8.08); add(33.8); add(34.66); add(-2.49); add(4.27); }} );
		STRUCTURE.put("CGTGT", new ArrayList<Double>() {{add(5.47); add(-9.43); add(33.84); add(35.35); add(-2.25); add(4.66); }} );
		STRUCTURE.put("CGTGG", new ArrayList<Double>() {{add(5.64); add(-7.87); add(33.53); add(34.18); add(-2.34); add(3.23); }} );
		STRUCTURE.put("CGTGC", new ArrayList<Double>() {{add(5.74); add(-8.4); add(33.78); add(34.75); add(-2.27); add(3.22); }} );
		STRUCTURE.put("CGTCA", new ArrayList<Double>() {{add(5.39); add(-8.47); add(34.29); add(35.16); add(-2.4); add(-1.16); }} );
		STRUCTURE.put("CGTCT", new ArrayList<Double>() {{add(4.83); add(-8.81); add(34.44); add(36.41); add(-2.95); add(-2.02); }} );
		STRUCTURE.put("CGTCG", new ArrayList<Double>() {{add(5.19); add(-8.49); add(34.26); add(35.57); add(-2.51); add(-1.47); }} );
		STRUCTURE.put("CGTCC", new ArrayList<Double>() {{add(5.02); add(-8.11); add(34.01); add(36.13); add(-2.49); add(-1.8); }} );
		STRUCTURE.put("CGGAA", new ArrayList<Double>() {{add(5.09); add(-5.36); add(34.24); add(35.82); add(-1.46); add(-1.8); }} );
		STRUCTURE.put("CGGAT", new ArrayList<Double>() {{add(4.47); add(-4.49); add(32.93); add(36.59); add(-2.34); add(-2.07); }} );
		STRUCTURE.put("CGGAG", new ArrayList<Double>() {{add(5.01); add(-5.07); add(33.51); add(35.79); add(-0.61); add(-1.83); }} );
		STRUCTURE.put("CGGAC", new ArrayList<Double>() {{add(5.08); add(-6.07); add(33.8); add(36.17); add(-0.51); add(-1.73); }} );
		STRUCTURE.put("CGGTA", new ArrayList<Double>() {{add(4.96); add(-5.99); add(34.27); add(34.43); add(-0.83); add(-2.35); }} );
		STRUCTURE.put("CGGTT", new ArrayList<Double>() {{add(4.36); add(-7.97); add(34.48); add(35.23); add(-0.65); add(-3.13); }} );
		STRUCTURE.put("CGGTG", new ArrayList<Double>() {{add(4.92); add(-6.56); add(34.36); add(34.05); add(-0.28); add(-2.48); }} );
		STRUCTURE.put("CGGTC", new ArrayList<Double>() {{add(4.69); add(-5.99); add(34.17); add(34.65); add(-0.84); add(-2.77); }} );
		STRUCTURE.put("CGGGA", new ArrayList<Double>() {{add(4.95); add(-2.56); add(33.56); add(34.12); add(-1.93); add(-0.91); }} );
		STRUCTURE.put("CGGGT", new ArrayList<Double>() {{add(4.7); add(-3.53); add(33.51); add(34.56); add(-1.76); add(-0.94); }} );
		STRUCTURE.put("CGGGG", new ArrayList<Double>() {{add(4.94); add(-1.95); add(33.22); add(33.62); add(-1.77); add(-1.76); }} );
		STRUCTURE.put("CGGGC", new ArrayList<Double>() {{add(4.95); add(-2.73); add(33.44); add(33.78); add(-1.7); add(-1.71); }} );
		STRUCTURE.put("CGGCA", new ArrayList<Double>() {{add(5.17); add(-3.11); add(33.58); add(36.2); add(-1.26); add(-1.38); }} );
		STRUCTURE.put("CGGCT", new ArrayList<Double>() {{add(4.83); add(-3.04); add(33.27); add(37.23); add(-1.44); add(-2.14); }} );
		STRUCTURE.put("CGGCG", new ArrayList<Double>() {{add(5.09); add(-3.17); add(33.59); add(36.24); add(-1.33); add(-1.46); }} );
		STRUCTURE.put("CGGCC", new ArrayList<Double>() {{add(4.96); add(-2.72); add(33.23); add(36.85); add(-1.3); add(-2.0); }} );
		STRUCTURE.put("CGCAA", new ArrayList<Double>() {{add(5.81); add(-4.14); add(35.42); add(34.85); add(-0.77); add(4.82); }} );
		STRUCTURE.put("CGCAT", new ArrayList<Double>() {{add(5.6); add(-4.03); add(35.58); add(35.0); add(-0.76); add(5.09); }} );
		STRUCTURE.put("CGCAG", new ArrayList<Double>() {{add(5.67); add(-3.85); add(35.64); add(34.24); add(-0.89); add(3.64); }} );
		STRUCTURE.put("CGCAC", new ArrayList<Double>() {{add(5.88); add(-4.13); add(35.54); add(34.69); add(-0.64); add(3.27); }} );
		STRUCTURE.put("CGCTA", new ArrayList<Double>() {{add(5.22); add(-1.63); add(36.72); add(31.38); add(-1.71); add(-2.2); }} );
		STRUCTURE.put("CGCTT", new ArrayList<Double>() {{add(4.63); add(-1.91); add(37.15); add(32.27); add(-2.72); add(-2.38); }} );
		STRUCTURE.put("CGCTG", new ArrayList<Double>() {{add(5.13); add(-1.72); add(36.67); add(31.2); add(-1.69); add(-1.93); }} );
		STRUCTURE.put("CGCTC", new ArrayList<Double>() {{add(5.02); add(-1.32); add(36.68); add(31.47); add(-1.98); add(-1.89); }} );
		STRUCTURE.put("CGCGA", new ArrayList<Double>() {{add(5.42); add(-4.27); add(35.66); add(32.65); add(-1.33); add(4.5); }} );
		STRUCTURE.put("CGCGT", new ArrayList<Double>() {{add(5.2); add(-4.73); add(35.9); add(33.2); add(-1.39); add(4.51); }} );
		STRUCTURE.put("CGCGG", new ArrayList<Double>() {{add(5.51); add(-4.25); add(35.54); add(32.35); add(-1.05); add(2.68); }} );
		STRUCTURE.put("CGCGC", new ArrayList<Double>() {{add(5.54); add(-4.22); add(35.7); add(32.44); add(-1.15); add(3.12); }} );
		STRUCTURE.put("CGCCA", new ArrayList<Double>() {{add(5.06); add(-2.98); add(36.34); add(33.36); add(-1.9); add(-1.66); }} );
		STRUCTURE.put("CGCCT", new ArrayList<Double>() {{add(4.77); add(-2.82); add(36.31); add(34.34); add(-2.16); add(-2.1); }} );
		STRUCTURE.put("CGCCG", new ArrayList<Double>() {{add(5.09); add(-3.17); add(36.24); add(33.59); add(-1.46); add(-1.33); }} );
		STRUCTURE.put("CGCCC", new ArrayList<Double>() {{add(4.96); add(-2.89); add(36.18); add(33.85); add(-1.66); add(-1.63); }} );
		STRUCTURE.put("CCAAA", new ArrayList<Double>() {{add(5.42); add(-9.32); add(34.57); add(35.2); add(2.51); add(-3.1); }} );
		STRUCTURE.put("CCAAT", new ArrayList<Double>() {{add(5.3); add(-10.72); add(34.4); add(34.93); add(5.06); add(-2.07); }} );
		STRUCTURE.put("CCAAG", new ArrayList<Double>() {{add(5.31); add(-9.71); add(34.61); add(34.56); add(4.48); add(-3.4); }} );
		STRUCTURE.put("CCAAC", new ArrayList<Double>() {{add(5.42); add(-9.8); add(34.44); add(31.76); add(6.57); add(-1.94); }} );
		STRUCTURE.put("CCATA", new ArrayList<Double>() {{add(5.88); add(-6.69); add(34.97); add(31.26); add(3.24); add(-2.84); }} );
		STRUCTURE.put("CCATT", new ArrayList<Double>() {{add(5.08); add(-8.98); add(34.87); add(32.12); add(3.6); add(-4.16); }} );
		STRUCTURE.put("CCATG", new ArrayList<Double>() {{add(5.34); add(-9.01); add(34.66); add(31.61); add(3.95); add(-4.03); }} );
		STRUCTURE.put("CCATC", new ArrayList<Double>() {{add(5.53); add(-7.48); add(34.54); add(31.66); add(4.13); add(-3.42); }} );
		STRUCTURE.put("CCAGA", new ArrayList<Double>() {{add(5.56); add(-6.09); add(33.91); add(31.02); add(2.85); add(-2.71); }} );
		STRUCTURE.put("CCAGT", new ArrayList<Double>() {{add(5.32); add(-7.24); add(34.39); add(31.51); add(3.06); add(-1.82); }} );
		STRUCTURE.put("CCAGG", new ArrayList<Double>() {{add(5.4); add(-5.54); add(34.05); add(31.17); add(2.39); add(-2.87); }} );
		STRUCTURE.put("CCAGC", new ArrayList<Double>() {{add(5.53); add(-5.87); add(34.09); add(30.94); add(2.75); add(-2.45); }} );
		STRUCTURE.put("CCACA", new ArrayList<Double>() {{add(5.8); add(-7.33); add(34.79); add(33.07); add(2.59); add(-1.82); }} );
		STRUCTURE.put("CCACT", new ArrayList<Double>() {{add(5.31); add(-7.4); add(34.11); add(34.38); add(2.64); add(-2.95); }} );
		STRUCTURE.put("CCACG", new ArrayList<Double>() {{add(5.64); add(-7.87); add(34.18); add(33.53); add(3.23); add(-2.34); }} );
		STRUCTURE.put("CCACC", new ArrayList<Double>() {{add(5.56); add(-7.11); add(34.27); add(33.92); add(2.46); add(-2.55); }} );
		STRUCTURE.put("CCTAA", new ArrayList<Double>() {{add(5.96); add(-5.77); add(31.45); add(34.35); add(-2.83); add(5.87); }} );
		STRUCTURE.put("CCTAT", new ArrayList<Double>() {{add(5.6); add(-6.87); add(31.71); add(34.5); add(-2.45); add(5.54); }} );
		STRUCTURE.put("CCTAG", new ArrayList<Double>() {{add(5.69); add(-6.51); add(31.47); add(34.19); add(-2.23); add(4.92); }} );
		STRUCTURE.put("CCTAC", new ArrayList<Double>() {{add(5.76); add(-6.19); add(31.41); add(34.06); add(-2.71); add(4.46); }} );
		STRUCTURE.put("CCTTA", new ArrayList<Double>() {{add(4.81); add(-8.13); add(32.34); add(34.66); add(-4.02); add(-3.71); }} );
		STRUCTURE.put("CCTTT", new ArrayList<Double>() {{add(4.05); add(-9.21); add(32.79); add(35.88); add(-5.06); add(-4.8); }} );
		STRUCTURE.put("CCTTG", new ArrayList<Double>() {{add(4.65); add(-9.16); add(32.51); add(34.98); add(-2.93); add(-3.66); }} );
		STRUCTURE.put("CCTTC", new ArrayList<Double>() {{add(4.63); add(-7.38); add(32.02); add(34.75); add(-3.57); add(-3.82); }} );
		STRUCTURE.put("CCTGA", new ArrayList<Double>() {{add(5.47); add(-5.31); add(30.94); add(34.07); add(-3.12); add(3.63); }} );
		STRUCTURE.put("CCTGT", new ArrayList<Double>() {{add(5.29); add(-6.78); add(31.34); add(34.61); add(-2.48); add(4.68); }} );
		STRUCTURE.put("CCTGG", new ArrayList<Double>() {{add(5.4); add(-5.54); add(31.17); add(34.05); add(-2.87); add(2.39); }} );
		STRUCTURE.put("CCTGC", new ArrayList<Double>() {{add(5.52); add(-5.92); add(31.18); add(33.94); add(-3.15); add(2.96); }} );
		STRUCTURE.put("CCTCA", new ArrayList<Double>() {{add(5.3); add(-6.36); add(31.54); add(34.61); add(-2.49); add(-0.98); }} );
		STRUCTURE.put("CCTCT", new ArrayList<Double>() {{add(4.97); add(-6.38); add(31.44); add(35.39); add(-2.6); add(-1.11); }} );
		STRUCTURE.put("CCTCG", new ArrayList<Double>() {{add(5.18); add(-6.32); add(31.5); add(34.97); add(-2.8); add(-1.26); }} );
		STRUCTURE.put("CCTCC", new ArrayList<Double>() {{add(4.93); add(-5.75); add(31.42); add(35.57); add(-3.27); add(-1.91); }} );
		STRUCTURE.put("CCGAA", new ArrayList<Double>() {{add(5.38); add(-6.08); add(32.67); add(35.61); add(2.76); add(-0.53); }} );
		STRUCTURE.put("CCGAT", new ArrayList<Double>() {{add(5.25); add(-7.15); add(32.57); add(35.6); add(3.63); add(-0.66); }} );
		STRUCTURE.put("CCGAG", new ArrayList<Double>() {{add(5.65); add(-6.12); add(32.31); add(34.73); add(4.6); add(-0.49); }} );
		STRUCTURE.put("CCGAC", new ArrayList<Double>() {{add(5.54); add(-6.74); add(32.81); add(35.42); add(3.41); add(-1.52); }} );
		STRUCTURE.put("CCGTA", new ArrayList<Double>() {{add(5.33); add(-6.7); add(33.27); add(33.73); add(2.74); add(-2.19); }} );
		STRUCTURE.put("CCGTT", new ArrayList<Double>() {{add(4.62); add(-10.04); add(33.45); add(35.14); add(3.74); add(-3.07); }} );
		STRUCTURE.put("CCGTG", new ArrayList<Double>() {{add(5.1); add(-8.46); add(33.7); add(33.81); add(4.06); add(-2.43); }} );
		STRUCTURE.put("CCGTC", new ArrayList<Double>() {{add(5.1); add(-7.15); add(33.08); add(34.1); add(3.11); add(-2.46); }} );
		STRUCTURE.put("CCGGA", new ArrayList<Double>() {{add(5.39); add(-3.19); add(32.84); add(33.6); add(1.31); add(-0.62); }} );
		STRUCTURE.put("CCGGT", new ArrayList<Double>() {{add(5.16); add(-5.03); add(32.44); add(34.12); add(2.54); add(-0.44); }} );
		STRUCTURE.put("CCGGG", new ArrayList<Double>() {{add(5.19); add(-3.26); add(32.53); add(33.24); add(1.93); add(-1.59); }} );
		STRUCTURE.put("CCGGC", new ArrayList<Double>() {{add(5.36); add(-3.58); add(32.61); add(33.41); add(2.08); add(-1.33); }} );
		STRUCTURE.put("CCGCA", new ArrayList<Double>() {{add(5.59); add(-4.13); add(33.13); add(35.63); add(2.75); add(-0.88); }} );
		STRUCTURE.put("CCGCT", new ArrayList<Double>() {{add(5.15); add(-3.78); add(32.34); add(36.72); add(2.44); add(-1.9); }} );
		STRUCTURE.put("CCGCG", new ArrayList<Double>() {{add(5.51); add(-4.25); add(32.35); add(35.54); add(2.68); add(-1.05); }} );
		STRUCTURE.put("CCGCC", new ArrayList<Double>() {{add(5.29); add(-3.13); add(32.31); add(36.31); add(1.79); add(-2.02); }} );
		STRUCTURE.put("CCCAA", new ArrayList<Double>() {{add(5.44); add(-3.45); add(32.76); add(34.28); add(-0.64); add(6.64); }} );
		STRUCTURE.put("CCCAT", new ArrayList<Double>() {{add(5.19); add(-2.37); add(33.45); add(34.56); add(-2.51); add(3.38); }} );
		STRUCTURE.put("CCCAG", new ArrayList<Double>() {{add(5.39); add(-2.47); add(33.54); add(33.88); add(-1.97); add(2.42); }} );
		STRUCTURE.put("CCCAC", new ArrayList<Double>() {{add(5.41); add(-2.94); add(33.55); add(34.09); add(-1.85); add(2.54); }} );
		STRUCTURE.put("CCCTA", new ArrayList<Double>() {{add(4.99); add(-0.56); add(34.12); add(31.16); add(-2.27); add(-2.67); }} );
		STRUCTURE.put("CCCTT", new ArrayList<Double>() {{add(4.42); add(-0.14); add(34.22); add(31.94); add(-3.54); add(-3.37); }} );
		STRUCTURE.put("CCCTG", new ArrayList<Double>() {{add(4.85); add(-0.14); add(34.07); add(31.11); add(-2.88); add(-3.39); }} );
		STRUCTURE.put("CCCTC", new ArrayList<Double>() {{add(4.78); add(-0.03); add(33.87); add(31.33); add(-2.62); add(-2.73); }} );
		STRUCTURE.put("CCCGA", new ArrayList<Double>() {{add(5.22); add(-2.92); add(33.58); add(32.86); add(-1.75); add(3.35); }} );
		STRUCTURE.put("CCCGT", new ArrayList<Double>() {{add(4.92); add(-2.81); add(33.75); add(33.23); add(-2.05); add(2.76); }} );
		STRUCTURE.put("CCCGG", new ArrayList<Double>() {{add(5.19); add(-3.26); add(33.24); add(32.53); add(-1.59); add(1.93); }} );
		STRUCTURE.put("CCCGC", new ArrayList<Double>() {{add(5.3); add(-2.75); add(33.41); add(32.3); add(-1.73); add(2.34); }} );
		STRUCTURE.put("CCCCA", new ArrayList<Double>() {{add(4.89); add(-2.02); add(33.49); add(33.26); add(-2.1); add(-1.74); }} );
		STRUCTURE.put("CCCCT", new ArrayList<Double>() {{add(4.62); add(-1.32); add(33.61); add(33.78); add(-2.41); add(-2.55); }} );
		STRUCTURE.put("CCCCG", new ArrayList<Double>() {{add(4.94); add(-1.95); add(33.62); add(33.22); add(-1.76); add(-1.77); }} );
		STRUCTURE.put("CCCCC", new ArrayList<Double>() {{add(4.75); add(-1.03); add(33.41); add(33.25); add(-2.41); add(-2.24); }} );
	}
}