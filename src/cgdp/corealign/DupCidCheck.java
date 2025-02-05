package cgdp.corealign;

import java.io.*;
import java.lang.*;
import java.util.*;
import java.util.regex.*;

class DupCidCheck {
	Pattern newCidPat;	// regex
	HashMap foundCID;
//	String sep_subgrp = "#";
	String sep_subgrp = "_";
	static DupCidCheck instance;
	private DupCidCheck() {
		newCidPat = Pattern.compile(sep_subgrp + "[0-9]+");
		foundCID = new HashMap();
	}
	static DupCidCheck getInstance() {
		if (instance == null) {
			instance = new DupCidCheck();
		}
		return instance;
	}
	boolean isNewCID(String cid) {
		Matcher m = newCidPat.matcher(cid);
		return (m.find());
	}
	String getOrigID(String cid) {
		Matcher m = newCidPat.matcher(cid);
		String origid = m.replaceAll("");
		return(origid);
	}

	// class for dupcheck
	class CountData {
		int count = 0;
		CountData(int i) {
			count = i;
		}
		void incr() {
			count++;
		}
	}
	String checkDupCluster(String cid, int mode) {
		String newcid;
		if (mode == 1) {
			newcid = dupcheck(cid);
		} else {
			newcid = dupcheck2(cid);
		}
		return newcid;
	}
	String dupcheck(String cid) {
		CountData cidCount = (CountData) foundCID.get(cid);
		if (cidCount == null) {
			cidCount = new CountData(1);
			foundCID.put(cid, cidCount);
			return cid;
		} else {
			cidCount.incr();
			int cidNum = cidCount.count;
			return cid + sep_subgrp + cidNum;
		}
		
	}
	String dupcheck2(String cid) {
		CountData cidCount = (CountData) foundCID.get(cid);
		if (cidCount == null) {
			cidCount = new CountData(1);
			foundCID.put(cid, cidCount);
			return cid;
		} else {
			int cidNum = cidCount.count;
			if (cidNum > 1) {
				return cid + sep_subgrp + "1";
			} else {
				return cid;
			}
		}
		
	}
}
