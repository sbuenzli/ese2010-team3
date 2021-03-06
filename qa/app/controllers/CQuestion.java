package controllers;

import models.Comment;
import models.Question;
import models.User;
import play.cache.Cache;
import play.data.validation.Required;
import play.data.validation.Validation;
import play.mvc.Router.ActionDefinition;
import play.mvc.With;

/**
 * The controller for all routes that concern the {@link Question}'s.
 * 
 * @author Group3
 * 
 */
@With(Secure.class)
public class CQuestion extends BaseController {

	/**
	 * Add a new {@link Question}. It is required that the content is not empty.
	 * In this case an error message will be displayed to the {@link User}.
	 * 
	 * @param content
	 *            the content of the {@link Question}.
	 * @param tags
	 *            the tags belonging to this {@link Question}.
	 */
	public static void newQuestion(@Required String content, String tags) {
		if (!Validation.hasErrors()) {
			User user = Session.user();
			if (user.canPost()) {
				Cache.delete("index.questions");
				Question question = Database.questions()
						.add(user, content);
				question.setTagString(tags);
				flash.success("secure.newquestionflash");
				Application.question(question.id());
			}
		} else {
			flash.error("secure.emptyquestionerror");
			Application.index(0);
		}
	}

	/**
	 * Update the tags of a {@link Question}.
	 * 
	 * @param id
	 *            the id of the {@link Question} to which the tags belong.
	 * @param tags
	 *            the tags to be updated
	 */
	public static void updateTags(int id, String tags) {
		Question question = Database.questions().get(id);
		User user = Session.user();
		if (question != null && user.canEdit(question)) {
			flash.success("secure.updatetagsflash");
			question.setTagString(tags);
		}
		Application.question(id);
	}

	/**
	 * Add a new {@link Comment} to an {@link Questions}.
	 * 
	 * @param questionId
	 *            the id of the {@link Question}.
	 * @param content
	 *            the content of the {@link Comment}. This field is required to
	 *            be filled out.
	 */
	public static void newCommentQuestion(int questionId,
			@Required String content) {
		Question question = Database.questions().get(questionId);
		User user = Session.user();
		if (!Validation.hasErrors() && question != null && !question.isLocked()
				&& user.canPost()) {
			Comment comment = question.comment(user, content);
			flash.success("secure.newcommentquestionflash");
			ActionDefinition action = reverse();
			Application.question(questionId);
			redirect(action.addRef("comment-" + comment.id()).toString());
		}
	}

	/**
	 * Adds the liker of a {@link Comment} to an {@link Question}.
	 * 
	 * @param commentId
	 *            the id of the {@link Comment}.
	 * @param questionId
	 *            the id of the {@link Question}.
	 */
	public static void addLikerQuestionComment(int commentId, int questionId) {
		Comment comment = getComment(questionId, -1, commentId);
		if (comment != null) {
			comment.addLiker(Session.user());
			flash.success("secure.likecommentflash");
		}
		Application.question(questionId);
	}

	/**
	 * Removes the liker of a {@link Comment} to an {@link Question}.
	 * 
	 * @param commentId
	 *            the id of the {@link Comment}.
	 * @param questionId
	 *            the id of the {@link Question}.
	 */
	public static void removeLikerQuestionComment(int commentId, int questionId) {
		Comment comment = getComment(questionId, -1, commentId);
		if (comment != null) {
			comment.removeLiker(Session.user());
			flash.success("secure.dislikecommentflash");
		}
		Application.question(questionId);
	}

	/**
	 * Vote {@link Question} up.
	 * 
	 * @param question
	 *            the id of the {@link Question}.
	 */
	public static void voteQuestionUp(int id) {
		Question question = Database.questions().get(id);
		if (question != null) {
			question.voteUp(Session.user());
			flash.success("secure.upvoteflash");
			if (!redirectToCallingPage()) {
				Application.question(id);
			}
		} else {
			Application.index(0);
		}
	}

	/**
	 * Vote {@link Question} down.
	 * 
	 * @param question
	 *            the id of the {@link Question}.
	 */
	public static void voteQuestionDown(int id) {
		Question question = Database.questions().get(id);
		if (question != null) {
			question.voteDown(Session.user());
			flash.success("secure.downvoteflash");
			if (!redirectToCallingPage()) {
				Application.question(id);
			}
		} else {
			Application.index(0);
		}
	}

	/**
	 * Cancel the own vote to an {@link Question}.
	 * 
	 * @param question
	 *            the id of the {@link Question}.
	 */
	public static void voteQuestionCancel(int id) {
		Question question = Database.questions().get(id);
		if (question != null) {
			question.voteCancel(Session.user());
			flash.success("Your vote has been forgotten.");
			if (!redirectToCallingPage()) {
				Application.question(id);
			}
		} else {
			Application.index(0);
		}
	}

	/**
	 * Delete the {@link Question}.
	 * 
	 * @param id
	 *            the id of the {@link Question} to be deleted.
	 */
	public static void deleteQuestion(int id) {
		Question question = Database.questions().get(id);
		if (question != null) {
			flash.success("secure.questiondeletedflash");
			question.delete();
		}
		Application.index(0);
	}

	/**
	 * Delete the comment to {@link Question}.
	 * 
	 * @param questionId
	 *            the id of the {@link Question}.
	 * 
	 * @param commentId
	 *            the id of the {@link Comment}.
	 */
	public static void deleteCommentQuestion(int questionId, int commentId) {
		Comment comment = getComment(questionId, -1, commentId);
		if (comment != null) {
			comment.delete();
			flash.success("secure.commentdeletedflash");
		}
		Application.question(questionId);
	}

	/**
	 * Watch the {@link Question}. A success message will be displayed.
	 * 
	 * @param id
	 *            the id of the {@link Question} to be watched.
	 */
	public static void watchQuestion(int id) {
		Question question = Database.questions().get(id);
		if (question != null) {
			Session.user().startObserving(question);
			flash.success("secure.startwatchquestionflash");
		}
		Application.question(id);
	}

	/**
	 * Stop to watch a {@link Question}.
	 * 
	 * @param id
	 *            the id of the {@link Question} to be unwatched.
	 */
	public static void unwatchQuestion(int id) {
		Question question = Database.questions().get(id);
		if (question != null) {
			Session.user().stopObserving(question);
			flash.success("secure.stopwatchquestionflash");
		}
		Application.question(id);
	}

	/**
	 * Stop to watch a {@link Question} chosen from the list.
	 * 
	 * @param id
	 *            the id of the {@link Question} to be unwatched.
	 */
	public static void unwatchQuestionFromList(int id) {
		Question question = Database.questions().get(id);
		if (question != null) {
			Session.user().stopObserving(question);
			flash.success("secure.stopwatchquestionflash", id, question
					.summary());
		}
		Application.notifications(1);
	}

	/**
	 * Unlock a {@link Question}.
	 * 
	 * @param id
	 *            the id of the {@link Question} to be unlocked.
	 */
	public static void unlockQuestion(int id) {
		User user = Session.user();
		if (user.isModerator()) {
			Question question = Database.questions().get(id);
			question.unlock();
			flash.success("secure.unlockquestionflash");
			Application.question(id);
		}
	}

	/**
	 * Lock {@link Question}.
	 * 
	 * @param id
	 *            the id of the {@link Question} to be locked.
	 */
	public static void lockQuestion(int id) {
		User user = Session.user();
		if (user.isModerator()) {
			Question question = Database.questions().get(id);
			question.lock();
			flash.success("secure.lockquestionflash");
			Application.question(id);
		}
	}

	/**
	 * Informs the moderators that this post is spam or deletes it, if the User
	 * is a moderator.
	 */
	public static void markSpam(int id) {
		markSpam(Database.questions().get(id));
		Application.index(0);
	}

	/**
	 * Informs the moderators that this comment is spam.
	 */
	public static void markSpamComment(int questionId, int commentId) {
		markSpam(getComment(questionId, -1, commentId));
		Application.question(questionId);
	}
}
