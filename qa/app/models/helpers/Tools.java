package models.helpers;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

public class Tools {

	/**
	 * Encrypt the password with SHA-1.
	 * 
	 * @param password
	 * @return the encrypted password
	 */
	public static String encrypt(String password) {
		try {
			MessageDigest m = MessageDigest.getInstance("SHA-1");
			return new BigInteger(1, m.digest(password.getBytes()))
					.toString(16);
		} catch (NoSuchAlgorithmException e) {
			return password;
		}
	}

	/**
	 * Check an email-address to be valid.
	 * 
	 * @param email
	 * @return true if the email is valid.
	 */
	public static boolean checkEmail(String email) {
		return email.matches("\\S+@(?:[A-Za-z0-9-]+\\.)+\\w{2,4}");
	}

	public static final String DATE_FORMAT_CH = "dd.MM.yyyy";
	public static final String DATE_FORMAT_US = "MM/dd/yyyy";
	public static final String DATE_FORMAT_ISO = "yyyy-MM-dd";

	/**
	 * Turns the Date object d into a String using the format given in the
	 * constant DATE_FORMAT.
	 */
	public static String dateToString(Date d) {
		if (d != null)
			return new SimpleDateFormat(DATE_FORMAT_CH).format(d);
		return null;
	}

	/**
	 * Turns the String object s into a Date assuming the format given in the
	 * constant DATE_FORMAT.
	 * 
	 * @throws ParseException
	 */
	public static Date stringToDate(String s) throws ParseException {
		if (s.matches("\\d{1,2}\\.\\d{1,2}\\.\\d{4}"))
			return new SimpleDateFormat(DATE_FORMAT_CH).parse(s);
		if (s.matches("\\d{1,2}/\\d{1,2}/\\d{4}"))
			return new SimpleDateFormat(DATE_FORMAT_US).parse(s);
		if (s.matches("\\d{4}-\\d{1,2}-\\d{1,2}"))
			return new SimpleDateFormat(DATE_FORMAT_ISO).parse(s);
		return null;
	}

	/**
	 * Takes a String of words with at least 4 characters and counts the
	 * occurrence. Words that occur more than 3 times are treated as important
	 * words.
	 * 
	 * @param input
	 *            with all the words that contain more than 3 characters
	 * @return keywords with words that occur more than 3 times
	 */
	public static String extractImportantWords(String input) {
		input = input.trim();
		HashMap<String, Integer> keywords = new HashMap();
		keywords.put("", 1);
		while (input.contains(" ") && input.length() > 3) {
			String word = input.substring(0, input.indexOf(" ")).trim();
			if (word.length() > 3) {
				int occurrence = (input.length() - (input.replaceAll(word, ""))
						.length()) / word.length();
				if (occurrence > 3) {
					if (keywords.size() < 5) {
						keywords.put(word, occurrence);
					} else {
						for (String stri : keywords.keySet()) {
							if (keywords.get(stri).intValue() < occurrence) {
								keywords.put(word, occurrence);
								keywords.remove(stri);
								break;
							}
						}
					}
				}
			}
			input = input.replaceAll(word + " ", "").trim();
		}
		return keywords.keySet().toString().replaceAll("[,\\[\\]]", "")
				.replaceAll("  ", " ").trim();
	}
}