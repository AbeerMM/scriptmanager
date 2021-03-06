package util;

import java.awt.Color;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintStream;

public class JTVOutput {

	public static void outputJTV(String filename, String type) throws FileNotFoundException {
		String[] name = filename.split("\\.");
		PrintStream OUT = new PrintStream(new File(name[0] + ".jtv"));	
		if(type.equals("red")) {
			OUT.println("<DocumentConfig><UrlExtractor/><ArrayUrlExtractor/><Views><View type=\"Dendrogram\" dock=\"1\"><ColorExtractor><ColorSet zero=\"#FFFFFF\" up=\"#FF0000\"/></ColorExtractor><ArrayDrawer/><GlobalXMap current=\"Fill\"><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></GlobalXMap><GlobalYMap current=\"Fill\"><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></GlobalYMap><ZoomXMap><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></ZoomXMap><ZoomYMap><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></ZoomYMap><TextView><TextView><GeneSummary/></TextView><TextView><GeneSummary/></TextView></TextView><ArrayNameView><ArraySummary included=\"0\"/></ArrayNameView><AtrSummary/><GtrSummary/></View></Views></DocumentConfig>");
		} else if(type.equals("orange")) {
			OUT.println("<DocumentConfig><UrlExtractor/><ArrayUrlExtractor/><Views><View type=\"Dendrogram\" dock=\"1\"><ColorExtractor><ColorSet zero=\"#FFFFFF\" up=\"#FFA500\"/></ColorExtractor><ArrayDrawer/><GlobalXMap current=\"Fill\"><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></GlobalXMap><GlobalYMap current=\"Fill\"><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></GlobalYMap><ZoomXMap><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></ZoomXMap><ZoomYMap><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></ZoomYMap><TextView><TextView><GeneSummary/></TextView><TextView><GeneSummary/></TextView></TextView><ArrayNameView><ArraySummary included=\"0\"/></ArrayNameView><AtrSummary/><GtrSummary/></View></Views></DocumentConfig>");
		}  else if(type.equals("yellow")) {
			OUT.println("<DocumentConfig><UrlExtractor/><ArrayUrlExtractor/><Views><View type=\"Dendrogram\" dock=\"1\"><ColorExtractor><ColorSet zero=\"#FFFFFF\" up=\"#FFD700\"/></ColorExtractor><ArrayDrawer/><GlobalXMap current=\"Fill\"><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></GlobalXMap><GlobalYMap current=\"Fill\"><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></GlobalYMap><ZoomXMap><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></ZoomXMap><ZoomYMap><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></ZoomYMap><TextView><TextView><GeneSummary/></TextView><TextView><GeneSummary/></TextView></TextView><ArrayNameView><ArraySummary included=\"0\"/></ArrayNameView><AtrSummary/><GtrSummary/></View></Views></DocumentConfig>");
		}  else if(type.equals("green")) {
			OUT.println("<DocumentConfig><UrlExtractor/><ArrayUrlExtractor/><Views><View type=\"Dendrogram\" dock=\"1\"><ColorExtractor><ColorSet zero=\"#FFFFFF\" up=\"#228B22\"/></ColorExtractor><ArrayDrawer/><GlobalXMap current=\"Fill\"><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></GlobalXMap><GlobalYMap current=\"Fill\"><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></GlobalYMap><ZoomXMap><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></ZoomXMap><ZoomYMap><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></ZoomYMap><TextView><TextView><GeneSummary/></TextView><TextView><GeneSummary/></TextView></TextView><ArrayNameView><ArraySummary included=\"0\"/></ArrayNameView><AtrSummary/><GtrSummary/></View></Views></DocumentConfig>");
		}  else if(type.equals("blue")) {
			OUT.println("<DocumentConfig><UrlExtractor/><ArrayUrlExtractor/><Views><View type=\"Dendrogram\" dock=\"1\"><ColorExtractor><ColorSet zero=\"#FFFFFF\" up=\"#0000FF\"/></ColorExtractor><ArrayDrawer/><GlobalXMap current=\"Fill\"><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></GlobalXMap><GlobalYMap current=\"Fill\"><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></GlobalYMap><ZoomXMap><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></ZoomXMap><ZoomYMap><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></ZoomYMap><TextView><TextView><GeneSummary/></TextView><TextView><GeneSummary/></TextView></TextView><ArrayNameView><ArraySummary included=\"0\"/></ArrayNameView><AtrSummary/><GtrSummary/></View></Views></DocumentConfig>");
		}  else if(type.equals("purple")) {
			OUT.println("<DocumentConfig><UrlExtractor/><ArrayUrlExtractor/><Views><View type=\"Dendrogram\" dock=\"1\"><ColorExtractor><ColorSet zero=\"#FFFFFF\" up=\"#A020F0\"/></ColorExtractor><ArrayDrawer/><GlobalXMap current=\"Fill\"><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></GlobalXMap><GlobalYMap current=\"Fill\"><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></GlobalYMap><ZoomXMap><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></ZoomXMap><ZoomYMap><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></ZoomYMap><TextView><TextView><GeneSummary/></TextView><TextView><GeneSummary/></TextView></TextView><ArrayNameView><ArraySummary included=\"0\"/></ArrayNameView><AtrSummary/><GtrSummary/></View></Views></DocumentConfig>");
		}  else if(type.equals("black")) {
			OUT.println("<DocumentConfig><UrlExtractor/><ArrayUrlExtractor/><Views><View type=\"Dendrogram\" dock=\"1\"><ColorExtractor><ColorSet zero=\"#FFFFFF\" up=\"#000000\"/></ColorExtractor><ArrayDrawer/><GlobalXMap current=\"Fill\"><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></GlobalXMap><GlobalYMap current=\"Fill\"><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></GlobalYMap><ZoomXMap><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></ZoomXMap><ZoomYMap><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></ZoomYMap><TextView><TextView><GeneSummary/></TextView><TextView><GeneSummary/></TextView></TextView><ArrayNameView><ArraySummary included=\"0\"/></ArrayNameView><AtrSummary/><GtrSummary/></View></Views></DocumentConfig");
		}
		OUT.close();
	}
	
	public static void outputJTV(String filename, Color type) throws FileNotFoundException {
		String[] name = filename.split("\\.");
		String NEWNAME = "";
		for(int x = 0; x < name.length - 1; x++) {
			NEWNAME += (name[x] + ".");
		}
		PrintStream OUT = new PrintStream(new File(NEWNAME + "jtv"));
		OUT.println("<DocumentConfig><UrlExtractor/><ArrayUrlExtractor/><Views><View type=\"Dendrogram\" dock=\"1\"><ColorExtractor><ColorSet zero=\"#FFFFFF\" up=\"" + convertColortoHex(type) + "\"/></ColorExtractor><ArrayDrawer/><GlobalXMap current=\"Fill\"><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></GlobalXMap><GlobalYMap current=\"Fill\"><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></GlobalYMap><ZoomXMap><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></ZoomXMap><ZoomYMap><FixedMap type=\"Fixed\"/><FillMap type=\"Fill\"/><NullMap type=\"Null\"/></ZoomYMap><TextView><TextView><GeneSummary/></TextView><TextView><GeneSummary/></TextView></TextView><ArrayNameView><ArraySummary included=\"0\"/></ArrayNameView><AtrSummary/><GtrSummary/></View></Views></DocumentConfig>");
		OUT.close();
	}
	
	public static String convertColortoHex(Color RGB) {
		String hex = Integer.toHexString(RGB.getRGB() & 0xffffff);
		if (hex.length() < 6) {
		    hex = "0" + hex;
		}
		hex = "#" + hex;
		return hex;
	}
}
