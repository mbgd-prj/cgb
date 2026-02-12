package cgdp.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import lombok.Data;
import net.arnx.jsonic.JSON;

/**
 * WebDB設定ファイルクラス。
 */
public class WebDBConfUtil {

	/**
	 * ロガー。
	 */
	private static Logger logger = LogManager.getLogger(WebDBConfUtil.class);

	/**
	 * WebDB設定ファイル名。
	 */
	private static final String WEBDB_CONF_FILE = "webdb.json";

	/**
	 * デフォルトWebDBリスト。
	 */
	private static final String[][] DEFAULT_WEBDB_LIST = {
		{"MBGD", "https://mbgd.nibb.ac.jp/htbin/MBGD_gene_info_frame.pl?name=${sp}:${name}"},
//		{"KEGG", "https://www.kegg.jp/dbget-bin/www_bget?${sp}:${name}"},
//		{"NCBI Gene", "https://www.ncbi.nlm.nih.gov/gene/?term=${sp}[Organism]+AND+${name}[Gene+Name]"},
//		{"UniProt", "https://www.uniprot.org/uniprot/?query=${sp}+${name}&sort=score"},
	};


	/**
	 * WebDB設定ファイルパスを取得する。
	 * @return WebDB設定ファイルパス。
	 */
	private static String getConfFilePath() {
		String confPath = System.getProperty("user.home") + "/.corealign/" + WEBDB_CONF_FILE;
		confPath = confPath.replaceAll("\\\\", "/");
		return confPath;
	}

	/**
	 * デフォルトWebDBリストを取得する。
	 * @return	デフォルトWebDBリスト。
	 */
	private static List<WebDB> getDefaultWebDBList() {
		List<WebDB> list = new ArrayList<>();
		for (String[] entry : DEFAULT_WEBDB_LIST) {
			list.add(new WebDB(entry[0], entry[1]));
		}
		return list;
	}


	/**
	 * 静的初期化子。
	 * <pre>
	 * WebDB設定ファイルが存在しない場合、デフォルトWebDBリストを作成する。
	 * </pre>
	 */
	 public static void createDefaultWebDBConfFile() {
		// WebDB設定ファイルが存在しない場合、デフォルトWebDBリストを保存する。
		String confFile = WebDBConfUtil.getConfFilePath();
		File f = new File(confFile);
		logger.info("WebDBConfUtil: confFile=" + confFile + ", exists=" + f.exists());
		if (!f.exists()) {
			List<WebDB> defaultList = WebDBConfUtil.getDefaultWebDBList();
			String json = JSON.encode(defaultList, true);
			logger.debug("WebDBConfUtil: create default webdb conf file. json=" + json);
			if (!f.getParentFile().exists()) {
				f.getParentFile().mkdir();
			}
			try (FileWriter r = new FileWriter(f)) {
				try (BufferedWriter w = new BufferedWriter(r)) {
					w.write(json);
				}
			} catch (Exception e) {
				logger.error(e.getMessage(), e);
			}
		}
	}


	/**
	 * コンストラクタ。
	 */
	private WebDBConfUtil() {
	}

	/**
	 * WebDB情報クラス。
	 */
	@Data
	public static class WebDB {
		// 名前。
		public String name;
		// URL。
		public String url;

		/**
		 * コンストラクタ。
		 * @param name	DB名。
		 * @param url	DBのURL。
		 */
		public WebDB(final String name, final String url) {
			this.name = name;
			this.url = url;
		}
	}




	/**
	 * WebDBリストを取得する。
	 * @return WebDBリスト。
	 */
	public static List<WebDB> getWebDBList() {
		WebDBConfUtil.createDefaultWebDBConfFile();
		String confFile = WebDBConfUtil.getConfFilePath();
		File f = new File(confFile);
		List<WebDB> list = new ArrayList<>();
		try (FileInputStream is = new FileInputStream(f)) {
			@SuppressWarnings("unchecked")
			List<Map<String, String>> mlist = (List<Map<String, String>>) JSON.decode(is, ArrayList.class);
			for (Map<String, String> entry : mlist) {
				list.add(new WebDB(entry.get("name"), entry.get("url")));
			}
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		}
		return list;
	}
}
