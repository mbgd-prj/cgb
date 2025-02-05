package cgdp.corealign;

import java.awt.Graphics2D;

public interface Drawer {
/*
	void setupDrawParameters(Graphics2D g, int width, int height);
*/
	int setParametersHeight(int height);
	void setGraphics(Graphics2D g);
	void setParametersByPaperSize(int width, int height);
	void drawData();
}

