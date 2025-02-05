package cgdp.corealign;

import java.io.*;
import java.lang.*;
import java.util.*;

class NbrClustData extends WeightedData {
	String clustid1, clustid2;
	int count, maxdir, maxcnt;
	int dir, side1, side2;
	/* double weight :  inherited from WeightedData */

	NbrClustData(String _clustid1, String _clustid2, int _count, int _maxdir, int _maxcnt, double _weight) {
		clustid1 = _clustid1; clustid2 = _clustid2;
		count = _count; maxdir = _maxdir;
		maxcnt = _maxcnt; weight = _weight;
		int side1 = Direction.reldir2dir(maxdir, 1);
		int side2 = Direction.reldir2dir(maxdir, 2);
		dir = (side1 == side2) ? -1 : 1; // => <= or <= => then dir=-1
	}
/*
	public String toString() {
		return("<"+count+" "+maxdir+" "+maxcnt+" "+weight+">");
	}
*/
	public String toString() {
		return(clustid1+":"+clustid2);
	}
}
