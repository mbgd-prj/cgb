package cgdp.filereader;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * アノテーションファイルを読み込むクラス。
 */
public class AnnotationFileReader {

	/**
	 * Logger。
	 */
	private static Logger logger = LogManager.getLogger(AnnotationFileReader.class);

	/**
	 * アノテーションファイルを読み込みます。。
	 * @param fname アノテーションファイル名。
	 * @return アノテーションのマップ。
	 * @throws Exception 例外。
	 */
	public Map<String, String> readAnnotationFile(String fname) throws Exception {
		logger.debug("readAnnotationFile: fname=" + fname);
		Map<String, String> anntationMap = new java.util.HashMap<>();
		try (BufferedReader reader = new BufferedReader(new FileReader(fname))) {
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				if (line.length() == 0 || line.startsWith("#")) {
					continue;
				}
				String[] token = line.split("\t");
				if (token.length >= 2) {
					anntationMap.put(token[0], token[1]);
				}
			}
		}
		return anntationMap;
	}

}
