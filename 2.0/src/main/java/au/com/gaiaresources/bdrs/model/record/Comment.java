package au.com.gaiaresources.bdrs.model.record;

import au.com.gaiaresources.bdrs.db.impl.PortalPersistentImpl;
import org.hibernate.annotations.Cascade;

import javax.persistence.AttributeOverride;
import javax.persistence.AttributeOverrides;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OrderBy;
import javax.persistence.Table;
import javax.persistence.Transient;
import java.util.ArrayList;
import java.util.List;

/**
 * A Comment captures a user comment about a Record.  A Comment can apply directly to a Record or be a reply to
 * another Comment.
 */
@Entity
@Table(name = "RECORD_COMMENT")
@AttributeOverrides({
  @AttributeOverride(name = "id", column = @Column(name = "COMMENT_ID"))
})
public class Comment extends PortalPersistentImpl {

    /** The text of this Comment */
    private String commentText;

    /** A List of replies made to this Comment */
    private List<Comment> replies = new ArrayList<Comment>();

    /** The Comment this Comment is a reply to */
    private Comment parent;

    /** Whether this Comment has been marked as deleted by an administrator */
    private boolean deleted;

    /** The Record this Comment applies to */
    private Record record;

    /**
     * @return the Comment this Comment replies to, or null if this is a top level comment.
     */
    @ManyToOne
    @JoinColumn(name="PARENT_COMMENT")
    public Comment getParent() {
        return parent;
    }

    /**
     * The Comment this Comment replies to.  Only nested Comments have a non null value for this property.
     * @param parent the Comment this Comment applies to, or null if this is a top level Comment.
     */
    public void setParent(Comment parent) {
        this.parent = parent;
    }

    /**
     * @return the text of this Comment.
     */
    @Column(name="COMMENT_TEXT", length = 1024)
    public String getCommentText() {
        return commentText;
    }

    /**
     * Sets the text of this Comment.
     * @param commentText the text of the Comment.
     */
    public void setCommentText(String commentText) {
        this.commentText = commentText;
    }

    /**
     * The Record this Comment applies to.  Only top level Comments have a non null value for this property.
     * @return the Record this Comment applies to, or null if this is a nested Comment.
     */
    @ManyToOne
    @JoinColumn(name="RECORD_ID")
    public Record getRecord() {
        return record;
    }

    /**
     * Returns the Comments that have been made in reply to this Comment.
     * @return a List of Comments that have been made in reply to this Comment.
     */
    @OneToMany(cascade={CascadeType.ALL}, fetch= FetchType.EAGER, mappedBy="parent")
    @Cascade(org.hibernate.annotations.CascadeType.DELETE_ORPHAN)
    @OrderBy("createdAt desc")
    public List<Comment> getReplies() {
        return replies;
    }

    /**
     * Sets the List of replies to this Comment.  This is a framework method, clients should use "reply" instead.
     * @param replies the List of Comments that are replies to this Comment.
     */
    void setReplies(List<Comment> replies) {
        this.replies = replies;
    }

    /**
     * Used by Hibernate & the Record class to maintain the relationship correctly,
     * Generally, Comments should be added to records, not the other way around.
     * @param record the Record that this Comment relates to.
     */
    void setRecord(Record record) {
        this.record = record;
    }

    /**
     *
     * @return true if this Comment has been deleted by an administrator.
     */
    @Column(name="DELETED")
    public boolean isDeleted() {
        return deleted;
    }

    /**
     * Sets the deleted property of this Comment.  This is a framework method -
     * clients should use the softDelete method instead.
     * @param deleted true if the Comment should be deleted.
     */
    void setDeleted(boolean deleted) {
        this.deleted = deleted;
    }

    /**
     * Performs a soft delete on this Comment.  Currently just sets the deleted flag, however it could optionally
     * replace the comment text.
     */
    @Transient
    public void softDelete() {
        setDeleted(true);
    }


    /**
     * Adds a child Comment (a reply) to this Comment using the supplied text.
     * @param commentText the text of the reply.
     * @return the newly added Comment.
     */
    @Transient
    public Comment reply(String commentText) {
        Comment comment = new Comment();
        comment.setCommentText(commentText);
        comment.setParent(this);

        // Insert the comment into the start of the list for consistency - when retrieved from the
        // database comments are returned in descending date created order.
        replies.add(0, comment);
        
        return comment;
    }


    /**
     * Searches recursively through the replies made on this comment for one with the supplied ID.
     * If no such comment is found, this method returns null.
     * @param commentId the ID of the comment to find.
     * @return a Comment with the supplied id if one exists, otherwise null.
     */
    @Transient
    public Comment getCommentById(int commentId) {
        Comment comment = null;
        for (Comment tmpComment : replies) {
            if (tmpComment.getId() == commentId) {
                comment = tmpComment;
                break;
            }

            comment = tmpComment.getCommentById(commentId);
            if (comment != null) {
                break;
            }
        }
        
        return comment;
    }
}
