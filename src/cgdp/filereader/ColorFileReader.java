package cgdp.filereader;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;

/**
 * 配色ファイルリーダー。
 */
public class ColorFileReader {
	/**
	 * 領域の色。
	 */
	@Getter
	@Setter(AccessLevel.PROTECTED)
	private Map<String, String> colorMap = null;

	/**
	 * 特徴領域のファイルから色を取得する。
	 * @param line ファイルの行。
	 * @return 色の文字列。
	 */
	protected String[] getColor(final String line) {
		String[] ret = null;
		Pattern p = Pattern.compile("^###\\s*color\\s*:\\s*([^=]+)\\s*=\\s*(#[0-9A-Fa-f]{6})\\s*$");
		Matcher m = p.matcher(line);
		if (m.find()) {
			ret = new String[2];
			ret[0] = m.group(1).trim();
			ret[1] = m.group(2).trim();
		}
		return ret;
	}
}
