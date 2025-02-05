package cgdp.util;

import java.awt.Color;

/**
 * 色関連ユーティリティ。
 *
 */
public final class ColorUtil {
	/**
	 * コンストラクタ。
	 */
	private ColorUtil() {

	}

	/**
	 * カラーコードに変換する。
	 * @param color Color。
	 * @return カラーコード"#rrggbb"形式。
	 */
	public static String getColorCode(final Color color) {
		int r = color.getRed();
		int g = color.getGreen();
		int b = color.getBlue();
		String rgb = "#" + String.format("%02x", r) + String.format("%02x", g) + String.format("%02x", b);
//		logger.debug("rgb=" + rgb);
		return rgb;

	}

	/**
	 * カラーコードからカラーに変換する。
	 * @param colorCode カラーコード。
	 * @return Colorのインスタンス。
	 */
	public static Color getColor(final String colorCode) {
		int r = Integer.parseInt(colorCode.substring(1, 3), 16);
		int g = Integer.parseInt(colorCode.substring(3, 5), 16);
		int b = Integer.parseInt(colorCode.substring(5, 7), 16);
		Color color = new Color(r, g, b);
		return color;
	}


}
