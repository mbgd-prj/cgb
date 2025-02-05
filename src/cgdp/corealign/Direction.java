package cgdp.corealign;
import java.lang.*;

class Direction {
	static final int LEFT = 0, RIGHT = 1;
	static final int L_L = 0, L_R = 1, R_L = 2, R_R = 3;
	static int getRelDir(int side, int dir) {
		if (dir > 0) {  // same direction, opsosite side: => => / <= <=
			if (side == LEFT) {
				return (L_R);
			} else {
				return (R_L);
			}
		} else {   // opposite direction, same side: => <= / <= =>
			if (side == LEFT) {
				return (L_L);
			} else {
				return (R_R);
			}
		}
	}
	static int reldir2dir(int reldir, int pos) {
		if (pos == 1) {
			return(reldir / 2);
		} else if (pos == 2) {
			return(reldir % 2);
		}
		return(-1);
	}
	public static void main(String[] args) {
		System.out.println(reldir2dir(L_L, 1)==LEFT);
		System.out.println(reldir2dir(L_L, 2)==LEFT);
		System.out.println(reldir2dir(L_R, 1)==LEFT);
		System.out.println(reldir2dir(L_R, 2)==RIGHT);
	}
}
