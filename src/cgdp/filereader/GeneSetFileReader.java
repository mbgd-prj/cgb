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
		// グループ名リスト。
		private List<String> nameList;
		// 色リスト。
		private List<String> colodList;

		/**
		 * コンストラクタ。
		 */
		public GeneSet() {
			this.nameList = new java.util.ArrayList<>();
			this.colodList = new java.util.ArrayList<>();
		}

		/**
		 * 指定のグループ名に該当するかどうかをチェックする。
		 * @param name グループ名。
		 * @return 該当する場合はtrue、そうでない場合はfalse。
		 */
		public boolean checkName(String name) {
			for(String n : this.nameList) {
				if (n.equals(name)) {
					return true;
				}
			}
			return false;
		}
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
					geneSet.setSpecies(token[0].trim());
					geneSet.setLocus(token[1].trim());
					for(int i = 2; i < token.length; i++) {
						String name = token[i].trim();
						geneSet.getNameList().add(name);
					}
					geneSetList.add(geneSet);
				}
			}
		}
		for (GeneSet geneSet : geneSetList) {
			for(String name : geneSet.getNameList()) {
				String color = colorMap.get(name);
				if (color != null) {
					geneSet.getColodList().add(color);
				} else {
					geneSet.getColodList().add(null);
					colorMap.put(name, null);
				}
			}
		}
		this.setColorMap(colorMap); // 色のマップをセット
		return geneSetList; // 読み込んだ特徴領域のリストを返す
	}

}
