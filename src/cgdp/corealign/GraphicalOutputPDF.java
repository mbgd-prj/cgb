package cgdp.corealign;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.util.regex.*;

import com.itextpdf.awt.PdfGraphics2D;
import com.itextpdf.text.Document;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfContentByte;

class GraphicalOutputPDF {
	CoreGenome coreGenome;
	GenomeData genomeData;
	String pdfout;
	Rectangle pagesize = null;
	Drawer drawer;

	public GraphicalOutputPDF(CoreGenome _coreGenome, GenomeData _genomeData, Drawer _drawer) {
		coreGenome = _coreGenome;
		genomeData = _genomeData;
		drawer = _drawer;
	}
	public void setPageSize(String papersize, boolean landscape) {
		// papersize should be "A4", "A3", "LETTER" etc.
		pagesize = PageSize.getRectangle(papersize);
		if (landscape) {
			pagesize = pagesize.rotate();
		}
	}
	// Landscape can be specified as like "A4L"
	public void setPageSize(String papersize) {
		boolean landscape = false;
		Pattern p = Pattern.compile("([AB][0-9]+)L");
		Matcher m = p.matcher(papersize);
		if (m.matches()) {
			papersize = m.group(1);
			landscape = true;
		}
		setPageSize(papersize, landscape);
	}
	public void setPageSize(float width, float height) {
		pagesize = new Rectangle(width, height);
	}
	public void createPDF(String _pdfout) {
		pdfout=_pdfout;
		if (pagesize == null) {
			// default page size
			setPageSize("A4",true);
		}
		Document doc = new Document(pagesize);
		PdfWriter pdfwriter = null;
		try {
			FileOutputStream fos = new FileOutputStream(pdfout);
			pdfwriter = PdfWriter.getInstance(doc,fos);
		} catch (Exception e ) {
//			throw(e);
		}
		
		doc.open();
		PdfContentByte canvas = pdfwriter.getDirectContent();
//		PdfTemplate template = canvas.createTemplate(width, height);
//		PdfGraphics2D g2 = new PdfGraphics2D(template, width, height);

		// com.itextpdf.text.Rectangle class
		Rectangle rect = doc.getPageSize();


		PdfGraphics2D g2 = new PdfGraphics2D(canvas, rect.getWidth(), rect.getHeight());

		drawer.setGraphics(g2);
		drawer.setParametersByPaperSize((int) rect.getWidth(), (int) rect.getHeight());

		drawer.drawData();
		g2.dispose();
		doc.close();
		pdfwriter.close();
	}
}
