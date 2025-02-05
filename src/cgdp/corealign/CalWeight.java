package cgdp.corealign;
import java.lang.*;

class CalWeight {
	static double pow = 1.5;
	static double calc(double dist) {
		if (dist == 0) {
			dist = 0.5;
		}
		// weight = 1/dist^pow
		dist = Math.pow(dist, pow);
		return 1 / dist;
	}
}
