package cgat.seq;
import java.util.*;

public class SeqRegion implements Comparator {
	int _from, _to, _dir;
	public SeqRegion() {
	}
	public SeqRegion(int from, int to) {
		_from = from; _to = to;
	}
	public SeqRegion(int from, int to, int dir) {
		_from = from; _to = to; _dir = dir;
	}
	public int getFrom() {
		return _from;
	}
	public int getTo() {
		return _to;
	}
	public int getDir() {
		return _dir;
	}
	public boolean isReverse() {
		return (_dir < 0);
	}
	public void setFrom(int from) {
		_from = from;
	}
	public void setTo(int to) {
		_to = to;
	}
	public void setDir(int dir) {
		_dir = dir;
	}
	public boolean overlap(SeqRegion reg) {
		return (this.getFrom() <= reg.getTo()
				&& reg.getFrom() <= this.getTo());
	}
	public String toString() {
		return "" + getFrom() +" "+ getTo();
	}
	public int compare(Object o1, Object o2) {
		return ((SeqRegion) o1).getFrom() - ((SeqRegion) o2).getFrom();
	}
}
