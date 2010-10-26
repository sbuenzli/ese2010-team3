package models;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;

/**
 * A user with a name. Can contain {@link Item}s i.e. {@link Question}s,
 * {@link Answer}s, {@link Comment}s and {@link Vote}s. When deleted, the
 * <code>User</code> requests all his {@link Item}s to delete themselves.
 * 
 * @author Simon Marti
 * @author Mirco Kocher
 * 
 */
public class User {

	private final String name;
	private final String password;
	private String email;
	private final HashSet<Item> items;
	private String fullname;
	protected Date dateOfBirth;
	private String website;
	private String profession;
	private String employer;
	private String biography;

	private Date timestamp;
	
	private ArrayList<Question> recentQuestions = new ArrayList<Question>();
	private ArrayList<Answer> recentAnswers = new ArrayList<Answer>();
	private ArrayList<Comment> recentComments = new ArrayList<Comment>();
	
	public static final String DATE_FORMAT_CH = "dd.MM.yyyy";
	public static final String DATE_FORMAT_US = "MM/dd/yyyy";
	public static final String DATE_FORMAT_ISO = "yyyy-MM-dd";


	/**
	 * Creates a <code>User</code> with a given name.
	 * 
	 * @param name
	 *            the name of the <code>User</code>
	 */
	public User(String name, String password) {
		this.name = name;
		this.password = encrypt(password);
		this.items = new HashSet<Item>();
	}

	/**
	 * Returns the name of the <code>User</code>.
	 * 
	 * @return name of the <code>User</code>
	 */
	public String name() {
		return this.name;
	}


	/**
	 * Encrypt the password with MD5
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
	 * Encrypt the password and check if it is the same as the stored one.
	 * 
	 * @param passwort
	 * @return true if the password is right
	 */
	public boolean checkPW(String password) {
		return this.password.equals(encrypt(password));
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

	/**
	 * Registers an {@link Item} which should be deleted in case the
	 * <code>User</code> gets deleted.
	 * 
	 * @param item
	 *            the {@link Item} to register
	 */
	public void registerItem(Item item) {
		this.items.add(item);
	}

	/*
	 * Causes the <code>User</code> to delete all his {@link Item}s.
	 */
	public void delete() {
		// operate on a clone to prevent a ConcurrentModificationException
		HashSet<Item> clone = (HashSet<Item>) this.items.clone();
		for (Item item : clone)
			item.unregister();
		this.items.clear();
		users.remove(this.name);
	}

	/**
	 * Unregisters an {@link Item} which has been deleted.
	 * 
	 * @param item
	 *            the {@link Item} to unregister
	 */
	public void unregister(Item item) {
		this.items.remove(item);
	}

	/**
	 * Checks if an {@link Item} is registered and therefore owned by a
	 * <code>User</code>.
	 * 
	 * @param item
	 *            the {@link Item}to check
	 * @return true if the {@link Item} is registered
	 */
	public boolean hasItem(Item item) {
		return this.items.contains(item);
	}

	/**
	 * The amount of Comments, Answers and Questions the <code>User</code> has
	 * posted in the last 60 Minutes
	 * 
	 * @return The amount of Comments, Answers and Questions for this
	 *         <code>User</code> in this Hour.
	 */
	public int howManyItemsPerHour() {
		Date now = SystemInformation.get().now();
		int i = 0;
		for (Item item : this.items) {
			if ((now.getTime() - item.timestamp().getTime()) <= 60 * 60 * 1000) {
				i++;
			}
		}
		return i;
	}

	/**
	 * The <code>User</code> is a Cheater if over 50% of his votes is for the
	 * same <code>User</code>
	 * 
	 * @return True if the <code>User</code> is supporting somebody.
	 */
	public boolean isMaybeCheater() {
		HashMap<User, Integer> votesForUser = new HashMap<User, Integer>();
		for (Item item : this.items) {
			if (item instanceof Vote && ((Vote) item).up()) {
				Integer count = votesForUser.get(item.owner());
				if (count == null)
					count = 0;
				votesForUser.put(item.owner(), count + 1);
			}
		}

	    if (votesForUser.isEmpty())
			return false;

	    Integer maxCount = (Integer) Collections.max(votesForUser.values());
		return maxCount > 3 && maxCount / votesForUser.size() > 0.5;
	}

	/**
	 * Anonymizes all questions, answers and comments by this user.
	 * 
	 * @param doAnswers
	 *            - whether to anonymize this user's answers as well
	 * @param doComments
	 *            - whether to anonymize this user's comments as well
	 */
	public void anonymize(boolean doAnswers, boolean doComments) {
		// operate on a clone to prevent a ConcurrentModificationException
		HashSet<Item> clone = (HashSet<Item>) this.items.clone();
		for (Item item : clone) {
			if (item instanceof Question || doAnswers && item instanceof Answer
					|| doComments && item instanceof Comment) {
				((Entry) item).anonymize();
				this.items.remove(item);
			}
		}
	}

	/**
	 * The <code>User</code> is a Spammer if he posts more than 30 comments,
	 * answers or questions in the last hour.
	 * 
	 * @return True if the <code>User</code> is a Spammer.
	 */
	public boolean isSpammer() {
		int number = this.howManyItemsPerHour();
		if (number >= 60) {
			return true;
		}
		return false;
	}

	/**
	 * Set the <code>User</code> as a Cheater if he spams the Site or supports
	 * somebody.
	 * 
	 */
	public boolean isCheating() {
		return (isSpammer() || isMaybeCheater());
	}

	/**
	 * Calculates the age of the <code>User</code> in years
	 * 
	 * @return age of the <code>User</code>
	 */
	private int age() {
		Date now = SystemInformation.get().now();
		if (dateOfBirth != null) {
			long age = now.getTime() - dateOfBirth.getTime();
			return (int) (age / ((long) 1000 * 3600 * 24 * 365));
		} else
			return (0);
	}

	/**
	 * Turns the Date object d into a String using the format given in the
	 * constant DATE_FORMAT.
	 */
	private String dateToString(Date d) {
		if (d != null) {
			SimpleDateFormat fmt = new SimpleDateFormat(DATE_FORMAT_CH);
			return fmt.format(d);
		} else
			return null;
	}

	/**
	 * Turns the String object s into a Date assuming the format given in the
	 * constant DATE_FORMAT
	 * 
	 * @throws ParseException
	 */
	private Date stringToDate(String s) throws ParseException {
		if (Pattern.matches("\\d{1,2}\\.\\d{1,2}\\.\\d{4}", s)) {
			SimpleDateFormat fmt = new SimpleDateFormat(DATE_FORMAT_CH);
			return fmt.parse(s);
		} else if (Pattern.matches("\\d{1,2}/\\d{1,2}/\\d{4}", s)) {
			SimpleDateFormat fmt = new SimpleDateFormat(DATE_FORMAT_US);
			return fmt.parse(s);
		} else if (Pattern.matches("\\d{4}-\\d{1,2}-\\d{1,2}", s)) {
			SimpleDateFormat fmt = new SimpleDateFormat(DATE_FORMAT_ISO);
			return fmt.parse(s);
		} else
			return (null);
	}

	/* Getter and Setter for profile data */

	public void setEmail(String email) {
		this.email = email;
	}

	public String getEmail() {
		return this.email;
	}

	public void setFullname(String fullname) {
		this.fullname = fullname;
	}

	public String getFullname() {
		return this.fullname;
	}

	public void setDateOfBirth(String birthday) throws ParseException {
		this.dateOfBirth = stringToDate(birthday);
	}

	public String getDateOfBirth() {
		return this.dateToString(dateOfBirth);
	}

	public int getAge() {
		return this.age();
	}

	public void setWebsite(String website) {
		this.website = website;
	}

	public String getWebsite() {
		return this.website;
	}

	public void setProfession(String profession) {
		this.profession = profession;
	}

	public String getProfession() {
		return this.profession;
	}

	public void setEmployer(String employer) {
		this.employer = employer;
	}

	public String getEmployer() {
		return this.employer;
	}

	public void setBiography(String biography) {
		this.biography = biography;
	}

	public String getBiography() {
		return this.biography;
	}

	public String getSHA1Password() {
		return this.password;
	}

	/*
	 * Static interface to access questions from controller (not part of unit
	 * testing)
	 */

	private static HashMap<String, User> users = new HashMap();

	/**
	 * Validate if the <code>User</code> is already in our database.
	 * 
	 * @param username
	 * @return True if this <code>User</code> has not yet Signed Up
	 */
	public static boolean needSignUp(String username) {
		return (users.get(username) == null);
	}

	public static User register(String username, String password) {
		User user = new User(username, password);
		users.put(username, user);
		return user;
	}

	/**
	 * Get the <code>User</code> with the given name.
	 * 
	 * @param name
	 * @return a <code>User</code> or null if the given name doesn't exist.
	 */
	public static User get(String name) {
		return users.get(name);
	}

	public ArrayList<Question> getRecentQuestions() {
		return this.recentQuestions;
	}

	public ArrayList<Answer> getRecentAnswers() {
		return this.recentAnswers;
	}

	public ArrayList<Comment> getRecentComments() {
		return this.recentComments;
	}
	
	public void addRecentQuestions(Question question) {
		if (recentQuestions.size() > 2) {
			this.recentQuestions.remove(2);
		}
		this.recentQuestions.add(0, question);
	}

	public void addRecentAnswers(Answer answer) {
		if (recentAnswers.size() > 2) {
			this.recentAnswers.remove(2);
		}
		this.recentAnswers.add(0, answer);
	}

	public void addRecentComments(Comment comment) {
		if (recentComments.size() > 2) {
			this.recentComments.remove(2);
		}
		this.recentComments.add(0, comment);
	}

	/*
	 * Interface to gather statistical data
	 */

	public static int getUserCount() {
		return User.users.size();
	}

	/**
	 * Get an ArrayList of all questions of this user
	 * 
	 * @return ArrayList<Question> All questions of this user
	 */
	public ArrayList<Question> getQuestions() {
		ArrayList<Question> questions = new ArrayList<Question>();
		for (Item i : this.items) {
			if (i instanceof Question) {
				questions.add((Question) i);
			}
		}
		return questions;
	}

	/**
	 * Get an ArrayList of all answers of this user
	 * 
	 * @return ArrayList<Answer> All answers of this user
	 */
	public ArrayList<Answer> getAnswers() {
		ArrayList<Answer> answers = new ArrayList<Answer>();
		for (Item i : this.items) {
			if (i instanceof Answer) {
				answers.add((Answer) i);
			}
		}
		return answers;
	}

	/**
	 * Get an ArrayList of all best rated answers
	 * 
	 * @return ArrayList<Answer> All best rated answers
	 */
	public ArrayList<Answer> bestAnswers() {
		ArrayList<Answer> answers = new ArrayList<Answer>();
		for (Answer a : this.getAnswers()) {
			if (a.isBestAnswer()) {
				answers.add(a);
			}
		}
		return answers;
	}

	/**
	 * Get an ArrayList of all highRated answers
	 * 
	 * @return ArrayList<Answer> All high rated answers
	 */
	public ArrayList<Answer> highRatedAnswers() {
		ArrayList<Answer> answers = new ArrayList<Answer>();
		for (Answer a : this.getAnswers()) {
			if (a.isHighRated()) {
				answers.add(a);
			}
		}
		return answers;

	}
}
