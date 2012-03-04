package au.com.gaiaresources.bdrs.util;

import org.hibernate.AssertionFailure;
import org.hibernate.Session;
import org.hibernate.Transaction;

/**
 * The reason for the existence of this class is:
 * If the org.hibernate.Transaction.commit() or rollback() methods throw any non-SQL exception (for example if a
 * postInsert listener throws a RuntimeException) Hibernate (3.3.2 GA) closes the Session but does not
 * disassociate it from the context to which it's bound (in the current BRDS implementation it is a ThreadLocal).
 * Subsequent requests (even on a different Thread) will receive the closed Session from getCurrentSession()
 * and throw an Exception when attempting to start a transaction.  This manifests to the user as a redirect loop
 * to the 500 error page.
 *
 * This class will detect that situation (which manifests as an AssertionFailure exception) and manually close the
 * session.  The close below forces this disassociation but will throw an Exception because the Session is
 * already closed, which is caught and the original AssertionFailure Exception is re-thrown.
 */
public class TransactionHelper {

    /**
     * Commits the Transaction bound to the supplied Session.
     * @param session the Session containing the transaction to commit.
     */
    public static void commit(Session session) {
        try {
            session.getTransaction().commit();
        }

        catch (AssertionFailure e) {
            try {
                // The reason for this is it forces the Hibernate dynamic proxy around the Session to disassociate
                // the session from the current session context.  The close method on the real Session object will
                // then throw an Exception, which we catch and ignore.
                session.close();
            }
            catch (Exception ex) {
                // This is expected because the session is already closed.
            }
            throw e;
        }    
    }

    /**
     * Commits the supplied Transaction.
     * @param tx the Transaction to commit.
     * @param session the Session containing the transaction to commit.
     */
    public static void commit(Transaction tx, Session session) {
        try {
            tx.commit();
        }

        catch (AssertionFailure e) {
            try {
                // The reason for this is it forces the Hibernate dynamic proxy around the Session to disassociate
                // the session from the current session context.  The close method on the real Session object will
                // then throw an Exception, which we catch and ignore.
                session.close();
            }
            catch (Exception ex) {
                // This is expected because the session is already closed.
            }
            throw e;
        }
    }
    

    /**
     * Rolls back the Transaction bound to the supplied Session.
     * @param session the Session containing the transaction to rollback.
     */
    public static void rollback(Session session) {
        try {
            session.getTransaction().rollback();
        }
        catch (AssertionFailure e) {
            try {
                // The reason for this is it forces the Hibernate dynamic proxy around the Session to disassociate
                // the session from the current session context.  The close method on the real Session object will
                // then throw an Exception, which we catch and ignore.
                session.close();
            }
            catch (Exception ex) {
                // This is expected because the session is already closed.
            }
            throw e;
        }
    }
    
}
