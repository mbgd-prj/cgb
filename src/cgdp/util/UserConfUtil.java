package cgdp.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.Map;

import net.arnx.jsonic.JSON;

/**
 * ユーザ設定ファイルクラス。
 */
public class UserConfUtil {
	/**
	 * データパス。
	 */
	public static final String DATA_PATH = "dataPath";

	/**
	 * コンストラクタ。
	 */
	private UserConfUtil() {
	}

	/**
	 * 設定ファイル名を取得する。
	 * @return 設定ファイル名。
	 */
	private static String getConfFileName() {
		String userHome = System.getProperty("user.home");
		String confPath = userHome + File.separator + ".corealign" + File.separator + "conf.json";
		return confPath;
	}

	/**
	 * 設定マップを取得する。
	 * @return 設定マップ。
	 * @throws Exception 例外。
	 */
	private static Map<String, String> loadConfigMap() throws Exception {
		String confFile = UserConfUtil.getConfFileName();
		File f = new File(confFile);
		if (f.exists()) {
			try (FileInputStream is = new FileInputStream(f)) {
				@SuppressWarnings("unchecked")
				HashMap<String, String> map = (HashMap<String, String>) JSON.decode(is, HashMap.class);
				return map;
			}
		} else {
			Map<String, String> map = new HashMap<String, String>();
			return map;
		}
	}

	/**
	 * 設定マップを保存しする。
	 * @param map 設定マップ。
	 * @throws Exception 例外。
	 */
	private static void saveConfigMap(final Map<String, String> map) throws Exception {
		String json = JSON.encode(map, true);
		String confFile = UserConfUtil.getConfFileName();
		File f = new File(confFile);
		if (!f.getParentFile().exists()) {
			f.getParentFile().mkdir();
		}
		try (FileWriter r = new FileWriter(f)) {
			try (BufferedWriter w = new BufferedWriter(r)) {
				w.write(json);
			}
		}
	}

	/**
	 * 設定値を保存する。
	 * @param key 設定キー。
	 * @param value 値。
	 * @throws Exception 例外。
	 */
	public static void set(final String key, final String value) throws Exception {
		Map<String, String> map = loadConfigMap();
		map.put(key, value);
		saveConfigMap(map);
	}


	/**
	 * 設定値を取得する。
	 * @param key 設定キー。
	 * @return 値。
	 * @throws Exception 例外。
	 */
	public static String get(final String key) throws Exception {
		Map<String, String> map = loadConfigMap();
		return map.get(key);
	}

}
