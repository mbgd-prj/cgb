package cgdp.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Confファイルユーティリティ。
 */
public class ConfFileUtil {
	/**
	 * Logger.
	 */
	private static Logger logger = LogManager.getLogger(ConfFileUtil.class);

	/**
	 * CoreFileのキー。
	 */
	public static final String COREFILE = "corefile";

	/**
	 * GaneFileのキー。
	 */
	public static final String GENEFILE = "genefile";

	/**
	 * IslandFileのキー。
	 */
	public static final String ISLFILE = "islfile";

	/**
	 * OtherFileのキー。
	 */
	public static final String OTHERFILE = "otherfile";


	/**
	 * DNA sequence fileのキー。、
	 */
	public static final String SEQFILE = "seqfile";

	/**
	 * Orderファイル。
	 */
	public static final String ORDERFILE = "orderfile";

	/**
	 * ALT ファイル。
	 */
	public static final String ALTNAMEFILE = "altnamefile";

	/**
	 * CHROM_GAP_LEN_RATIO。
	 */
	public static final String CHROM_GAP_LEN_RATIO = "chromGapLenRatio";

	/**
	 * Gene setのキー。
	 */
	public static final String GENESETFILE = "genesetfile";

	/**
	 * SegmentFileのキー。
	 */
	public static final String SEGMENTFILE = "senmentfile";

	/**
	 * AnnotationFileのキー。
	 */
	public static final String ANNOTATIONFILE = "annotationfile";


	/**
	 * コンストラクタ。
	 */
	private ConfFileUtil() {

	}

	/**
	 * 行の文字列を解析し値を取得する。
	 * @param key 取得するキー。
	 * @param line 行。
	 * @param map 値を保存するMap。
	 */
	private static void getValue(final String key, final String line, final Map<String, Object> map) {
		String pat = "^\\s*" + key + "\\s*=\\s*(.*)$";
		Pattern p = Pattern.compile(pat);
		Matcher m = p.matcher(line);
		if (m.find()) {
			String value = m.group(1).trim();
			if (value.length() > 0) {
				logger.debug("conf=" + key + ", " + value);
				map.put(key, value);
			}
		}
	}

	/**
	 * 行の文字列を解析し値を取得する。
	 * @param key 取得するキー。
	 * @param line 行。
	 * @param map 値を保存するMap。
	 */
	private static void getList(final String key, final String line, final Map<String, Object> map) {
		String pat = "^\\s*" + key + "\\s*=\\s*(.*)$";
		Pattern p = Pattern.compile(pat);
		Matcher m = p.matcher(line);
		if (m.find()) {
			String value = m.group(1).trim();
			if (value.length() > 0) {
				@SuppressWarnings("unchecked")
				List<String> list = (List<String>) map.get(key);
				if (list == null) {
					list = new ArrayList<String>();
				}
				list.add(value);
				map.put(key, list);
			}
		}
	}


	/**
	 * conffileを取得する。
	 * @param conffile 選択したパス。
	 * @return 各種ファイル名のマップ。
	 * @throws Exception 例外。
	 */
	public static Map<String, Object> readConfFile(final String conffile) throws Exception {
		Map<String, Object> ret = new HashMap<String, Object>();
		try (BufferedReader br = new BufferedReader(new FileReader(conffile))) {
			String line = null;
			while ((line = br.readLine()) != null) {
				line = line.trim();
				ConfFileUtil.getValue(COREFILE, line, ret);
				ConfFileUtil.getValue(GENEFILE, line, ret);
				ConfFileUtil.getValue(ISLFILE, line, ret);
				ConfFileUtil.getList(OTHERFILE, line, ret);
				ConfFileUtil.getValue(SEQFILE, line, ret);
				ConfFileUtil.getValue(ORDERFILE, line, ret);
				ConfFileUtil.getValue(ALTNAMEFILE, line, ret);
				ConfFileUtil.getValue(CHROM_GAP_LEN_RATIO, line, ret);

				ConfFileUtil.getList(GENESETFILE, line, ret);
				ConfFileUtil.getList(SEGMENTFILE, line, ret);
				ConfFileUtil.getList(ANNOTATIONFILE, line, ret);
			}
		}
		return ret;
	}


	/**
	 * データディレクトリに存在する各種データファイルを取得する。
	 * @param path データディレクトリのパス。
	 * @return 各種ファイル名のマップ。
	 * @throws Exception 例外。
	 */
	public static Map<String, Object> readDataDirectory(final String path) throws Exception {
		Map<String, Object> ret = new HashMap<String, Object>();
		File dir = new File(path);
		File[] list = dir.listFiles();
		for (File f: list) {
			String name = f.getName();
			logger.debug("name=" + name);
			if (Pattern.matches(".*coaln$", name)) {
				ret.put(COREFILE, name);
			} else if (Pattern.matches(".*genetab$", name)) {
				ret.put(GENEFILE, name);
			} else if (Pattern.matches(".*isl$", name)) {
				ret.put(ISLFILE, name);
			} else if (Pattern.matches(".*other$", name)) {
				@SuppressWarnings("unchecked")
				List<String> otherList = (List<String>) ret.get(OTHERFILE);
				if (otherList == null) {
					otherList = new ArrayList<String>();
				}
				otherList.add(name);
				ret.put(OTHERFILE, otherList);
			} else if (Pattern.matches(".*dnaseq$", name)) {
				ret.put(SEQFILE, name);
			} else if (Pattern.matches(".*fas$", name)) {
				ret.put(SEQFILE, name);
			} else if (Pattern.matches(".*order$", name)) {
				ret.put(ORDERFILE, name);
			} else if (Pattern.matches(".*altnames$", name)) {
				ret.put(ALTNAMEFILE, name);
			} else if (Pattern.matches(".*gset$", name)) {
				@SuppressWarnings("unchecked")
				List<String> flist = (List<String>) ret.get(GENESETFILE);
				if (flist == null) {
					flist = new ArrayList<String>();
				}
				flist.add(name);
				ret.put(GENESETFILE, flist);
			} else if (Pattern.matches(".*seg$", name)) {
				@SuppressWarnings("unchecked")
				List<String> flist = (List<String>) ret.get(SEGMENTFILE);
				if (flist == null) {
					flist = new ArrayList<String>();
				}
				flist.add(name);
				ret.put(SEGMENTFILE, flist);
			} else if (Pattern.matches(".*ann$", name)) {
				@SuppressWarnings("unchecked")
				List<String> flist = (List<String>) ret.get(ANNOTATIONFILE);
				if (flist == null) {
					flist = new ArrayList<String>();
				}
				flist.add(name);
				ret.put(ANNOTATIONFILE, flist);
			}
		}
		return ret;
	}
}
