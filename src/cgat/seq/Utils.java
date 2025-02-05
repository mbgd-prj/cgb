package cgat.seq;
import java.lang.*;
import java.util.*;

public class Utils {
	public static String [] split(String str, String sep) {
		return split(str, sep, 0);
	}
	public static String [] split(String str, String sep, int limit) {
		List result = new ArrayList();
		StringTokenizer st = new StringTokenizer(str, sep);
		int numField = 0;
		while (st.hasMoreTokens()) {
			result.add(st.nextToken());
			numField++;
			if (limit > 0 && numField >= limit) {
				break;
			}
		}
		String [] retstr = new String[numField];
		for (int i = 0; i < numField; i++) {
			retstr[i] = (String) result.get(i);
		}
		return retstr;
	}
	public static String setStringWidth(String str, int width) {
		int addw = width - str.length();
		StringBuffer retstr = new StringBuffer(str);
		for (int i =0; i < addw; i++) {
			retstr.append(' ');
		}
		return retstr.toString();
	}
	public static void main(String [] args) {
		String [] a = Utils.split("a b cd  d  \t e\ta\n", " \t\n");
		for (int i = 0; i < a.length; i++) {
			System.out.println(">"+a[i]+"<");
		}
	}
}
