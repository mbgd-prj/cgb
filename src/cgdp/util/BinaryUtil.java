package cgdp.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

/**
 * Object <-> binanry変換ユーティリティ。
 *
 */
public final class BinaryUtil {

	/**
	 * オブジェクトをバイナリに変換します。
	 * @param obj オブジェクト。
	 * @return バイナリ。
	 * @throws Exception 例外。
	 */
	public static byte[] convertToBinary(final Object obj) throws Exception {
		byte[] ret = null;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			try (ObjectOutputStream oos = new ObjectOutputStream(baos)) {
				oos.writeObject(obj);
			}
			ret = baos.toByteArray();
		}
		return ret;
	}

	/**
	 * バイナリをオブジェクトに変換します。
	 * @param bin バイナリ。
	 * @return オブジェクト。
	 * @throws Exception 例外。
	 */
	public static Object convertToObject(final byte[] bin) throws Exception {
		Object ret = null;
		try (ByteArrayInputStream bais = new ByteArrayInputStream(bin)) {
			try (ObjectInputStream ois = new ObjectInputStream(bais)) {
				ret = ois.readObject();
			}
		}
		return ret;
	}
}
