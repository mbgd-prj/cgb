package cgdp.filereader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * 特徴領域のユーティリティクラス。
 */
public class SegmentFileReader extends ColorFileReader {
	/**
	 * 特徴領域。
	 */
	@Data
	public static class Segment {
		/**
		 * 生物種。
		 */
		private String species;
		/**
		 * 染色体。
		 */
		private String chromosome;
		/**
		 * 染色体の連番。
		 */
		private int seqNo;
		/**
		 * 開始位置。
		 */
		private int start;
		/**
		 * 終了位置。
		 */
		private int end;
		/**
		 * 方向。
		 */
		private int dir;
		/**
		 * 領域名。
		 */
		private String name;
		/**
		 * 塩基配列のパターン。
		 */
		private String pattern;

		/**
		 * 領域の色コード。
		 */
		private String colorCode;

		/**
		 * 特徴領域の開始位置を表す文字列を取得する。
		 * @return 特徴領域の開始位置を表す文字列。
		 */
		public String getFrom() {
			return this.species + ":" + this.seqNo + ":" + this.start;
		}

		/**
		 * 特徴領域の終了位置を表す文字列を取得する。
		 * @return 特徴領域の終了位置を表す文字列。
		 */
		public String getTo() {
			return this.species + ":" + this.seqNo + ":" + this.end;
		}

	}

	/**
	 * 特徴領域のマップ。
	 */
	public static class SegmentMap extends HashMap<String, List<Segment>> {
		private static final long serialVersionUID = 1L;

	}

	/**
	 * ファイルから特徴領域を読み込む。
	 * @param filePath ファイルパス。
	 * @return 読み込んだ特徴領域のリスト。
	 * @throws Exception 例外。
	 *
	 */
	public List<Segment> readSegmentFile(final String filePath) throws Exception {
		Map<String, String> colorMap = new HashMap<>();
		List<Segment> segmentList = new java.util.ArrayList<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#")) {
					String[] color = this.getColor(line);
					if (color != null) {
						colorMap.put(color[0], color[1]);
					}
					continue;
				}
				String[] token = line.split("[\t:]");
				if (token.length >= 7) {
					Segment segment = new Segment();
					segment.setSpecies(token[0]);
					segment.setChromosome(token[1]);
					segment.setStart(Integer.parseInt(token[2]));
					segment.setEnd(Integer.parseInt(token[3]));
					segment.setDir(Integer.parseInt(token[4]));
					segment.setName(token[5]);
					segment.setPattern(token[6]);
					segmentList.add(segment);
				}
			}
		}
		for (Segment segment : segmentList) {
			String colorCode = colorMap.get(segment.getName());
			if (colorCode != null) {
				segment.setColorCode(colorCode);
			} else {
				segment.setColorCode(null);
				colorMap.put(segment.getName(), null);
			}
		}
		this.setColorMap(colorMap); // 色のマップをセット
		return segmentList; // 読み込んだ特徴領域のリストを返す
	}
}
