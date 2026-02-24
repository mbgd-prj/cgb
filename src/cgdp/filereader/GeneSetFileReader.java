package cgdp.filereader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Data;

/**
 * 遺伝子セットファイルリーダー。
 */
public class GeneSetFileReader extends ColorFileReader {
	/**
	 * GenSet。
	 */
	@Data
	public static class GeneSet {
		// 生物種。
		private String species;
		// ローカスタグ。
		private String locus;
		// グループ名。
		private String groupName;
	}

	/**
	 * ファイルから遺伝子情報を読み込む。
	 * @param filePath ファイルパス。
	 * @return 遺伝子情報のリスト。
	 * @throws Exception 例外。
	 *
	 */
	public List<GeneSet> readGeneSetFile(final String filePath) throws Exception {
		Map<String, String> colorMap = new HashMap<>();
		List<GeneSet> geneSetList = new java.util.ArrayList<>();
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
				if (token.length >= 3) {
					GeneSet geneSet = new GeneSet();
					geneSet.setSpecies(token[0]);
					geneSet.setLocus(token[1]);
					geneSet.setGroupName(token[2]);
					geneSetList.add(geneSet);
				}
			}
		}
		return geneSetList; // 読み込んだ特徴領域のリストを返す
	}

}
