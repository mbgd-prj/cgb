package cgat.seq;
import java.util.*;

public class SubSequenceList
{
	Sequence _seq;
	ArrayList regionList = null;
	public SubSequenceList(Sequence seq) {
		_seq = seq;
		regionList = new ArrayList();
	}
	public void add(SeqRegion reg) {
		regionList.add(reg);
	}
	public int size() {
		return regionList.size();
	}
	public void sort() {
		Collections.sort(regionList);
	}
	public SubSequence get(int idx) {
		SeqRegion reg = (SeqRegion) regionList.get(idx);
		return (new SubSequence(_seq, reg));
	}
	public SubSequenceList getOverlappingSubSequences(SeqRegion reg) {
		SubSequenceList slist = new SubSequenceList(_seq);
		addOverlappingSubSequences(reg, slist);
		return slist;
	}
	public void addOverlappingSubSequences(
				SeqRegion reg, SubSequenceList slist) {
		int i;
		SeqRegion r;
		for (i = 0; i < regionList.size(); i++) {
			r = (SeqRegion) regionList.get(i);
			if (reg.overlap(r)) {
				slist.add(r);
			}
		}
	}
}
