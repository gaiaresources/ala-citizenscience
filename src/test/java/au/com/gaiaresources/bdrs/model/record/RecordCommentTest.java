package au.com.gaiaresources.bdrs.model.record;

import au.com.gaiaresources.bdrs.controller.AbstractGridControllerTest;
import au.com.gaiaresources.bdrs.servlet.RequestContextHolder;
import junit.framework.Assert;
import org.hibernate.Session;
import org.junit.Test;

import java.util.List;

/**
 * The purpose of these tests is to test the Record/Comment/Child Comments associations, in particular the
 * persistence aspects of the relationship.
 */
public class RecordCommentTest extends AbstractGridControllerTest {

    /**
     * Tests that a comment can be added to a Record and is saved to the database correctly.
     */
    @Test
    public void testAddComment() throws Exception {


        String commentText = "This is my first comment!";
        
        Record rec = (Record)sesh.get(Record.class, r1.getId());
        
        Comment comment = new Comment();
        comment.setCommentText(commentText);
        rec.addComment(comment);
        Record freshR1 = reload(rec);


        Assert.assertEquals(1, freshR1.getComments().size());
        Assert.assertEquals(commentText, freshR1.getComments().get(0).getCommentText());

    }

    /**
     * Syncs the comment with the database, evicts it from the session then reloads it from the database.
     * @param record the Record to reload.
     * @return a new instance of the Record, reloaded from the database.
     */
    private Record reload(Record record) {

        sesh.flush();
        sesh.evict(record);

        return (Record)sesh.load(Record.class, r1.getId());
    }

    /**
     * Tests that a comment can be edited and is saved to the database correctly.
     * Note that is is not currently possible to do using the user interface - comments are not editable.
     */
    @Test
    public void testEditComment() throws Exception {

        Record rec = (Record)sesh.get(Record.class, r1.getId());
        String commentText = "This is my first comment!";

        rec.addComment(commentText);

        // Force the new comment to be synced with the database then remove it to make sure the comments are
        // queried correctly.
        Record freshR1 = reload(rec);
        Comment comment = freshR1.getComments().get(0);
        comment.setCommentText("This is my edited comment!");

        freshR1 = reload(freshR1);
        comment = freshR1.getComments().get(0);

        Assert.assertEquals("This is my edited comment!", comment.getCommentText());

    }

    /**
     * This tests that the order of comments is preserved during round trips to the database.
     */
    @Test
    public void testCommentOrder() {

        Record rec = (Record)sesh.get(Record.class, r1.getId());
        int numComments=10;
        String commentPrefix = "Comment ";
        for (int i=0; i<numComments; i++) {
            rec.addComment(commentPrefix+i);
            rec = reload(rec);
        }
        
        rec = reload(rec);
        
        Assert.assertEquals(numComments, rec.getComments().size());
        // Comments are supposed to be returned in descending date order.
        for (int i=0; i<numComments; i++) {
            Assert.assertEquals(commentPrefix+(numComments-i-1), rec.getComments().get(i).getCommentText());
        }

    }

    /**
     * Tests that the persistence logic is correct when nested comments are created.
     */
    @Test
    public void testNestedComments() {
        Record rec = (Record)sesh.get(Record.class, r1.getId());
        rec.addComment("Comment 1");

        rec = reload(rec);
        
        rec.addComment("Comment 2");

        String replyText = "This is a reply";
        rec.getComments().get(1).reply(replyText);
        
        rec = reload(rec);

        Assert.assertEquals(2, rec.getComments().size());

        // Note that the comments are returned in reverse date order so the first added comment is now
        // last in the list.
        Comment comment = rec.getComments().get(1);
        Assert.assertEquals(1, comment.getReplies().size());
        Assert.assertEquals(replyText, comment.getReplies().get(0).getCommentText());

    }


    /**
     * Tests that comments are deleted correctly when the Record they apply to is deleted using session.delete(record).
     */
    @Test
    public void testCommentsAreDeletedWithRecordUsingSession() throws Exception {
        deleteRecordAndAssert(new Deleter() {
            @Override
            public void delete(Record record) {
                Session session = RequestContextHolder.getContext().getHibernate();
                session.delete(record);
            }
        });
    }

    /**
     * Tests that comments are deleted correctly when the Record they apply to is deleted using RecordDAO.delete(record).
     */
    @Test
    public void testCommentsAreDeletedWithRecordUsingRecordDAO() throws Exception {
        deleteRecordAndAssert(new Deleter() {
            @Override
            public void delete(Record record) {
                recordDAO.delete(record);
            }
        });
    }

    /**
     * Tests that comments are deleted correctly when the Record they apply to is deleted.
     * @param deleter implements the delete operation.
     */
    private void deleteRecordAndAssert(Deleter deleter) {

        Record rec = (Record)sesh.get(Record.class, r1.getId());
        Comment c1 = rec.addComment("Comment 1");
        rec.addComment("Comment 2");
        c1.reply("Comment 3");

        Session session = RequestContextHolder.getContext().getHibernate();
        // Flush so we get handles on all of the ids.
        session.flush();

        // This tests the cascade handling.  The RecordDAO does a delete by query which means that the normal
        // cascade rules are not applied.
        deleter.delete(r1);

        // Flush the delete.
        session.flush();

        // Make sure the comments have been deleted.
        List<Comment> allComments = session.createQuery("from Comment c").list();
        Assert.assertEquals(0, allComments.size());
    }

    /** Implementations delete Records using different approaches */
    interface Deleter {
        public void delete(Record record);
    }
    
}
