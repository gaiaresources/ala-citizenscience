package au.com.gaiaresources.bdrs.model.report.impl;

import java.util.List;

import au.com.gaiaresources.bdrs.model.report.ReportCapability;
import org.hibernate.Query;
import org.hibernate.classic.Session;
import org.springframework.stereotype.Repository;

import au.com.gaiaresources.bdrs.db.impl.AbstractDAOImpl;
import au.com.gaiaresources.bdrs.model.report.Report;
import au.com.gaiaresources.bdrs.model.report.ReportDAO;

import javax.persistence.NonUniqueResultException;

@Repository
public class ReportDAOImpl extends AbstractDAOImpl implements ReportDAO {

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.report.ReportDAO#delete(au.com.gaiaresources.bdrs.model.report.Report)
     */
    @Override
    public void delete(Report report) {
        getSession().delete(report);
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.report.ReportDAO#getReport(int)
     */
    @Override
    public Report getReport(int reportId) {
        return super.getByID(Report.class, reportId);
    }

    /**
     * (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.report.ReportDAO#getReport(String)
     */
    @Override
    public Report getReport(String reportName) throws NonUniqueResultException {
        String queryString = "select r from Report r where r.name = :name";
        Session session = getSessionFactory().getCurrentSession();
        Query query = session.createQuery(queryString);
        query.setParameter("name", reportName);

        List<Report> result = query.list();
        if (result.size() > 1) {
            throw new NonUniqueResultException("There is more than one report with the name " + reportName);
        } else {
            return result.isEmpty() ? null : result.get(0);
        }
    }

    /* (non-Javadoc)
     * @see au.com.gaiaresources.bdrs.model.report.ReportDAO#getReports()
     */
    @Override
    public List<Report> getReports() {
        return super.find("from Report order by name");
    }

    @Override
    public List<Report> getReports(ReportCapability capability, ReportView view) {
        StringBuilder q = new StringBuilder();
        q.append("select distinct r");
        q.append(" from Report r");
        q.append(" left join fetch r.views view");
        q.append(" left join fetch r.capabilities capability");
        q.append(" where view = :view");
        if(capability != null) {
            q.append(" and capability = :capability");
        }
        q.append(" order by r.name");
        
        Session session = getSessionFactory().getCurrentSession();
        Query query = session.createQuery(q.toString());
        query.setParameter("view", view);

        if(capability != null) {
            query.setParameter("capability", capability);
        }

        return query.list();
    }

    /* (non-Javadoc)
    * @see au.com.gaiaresources.bdrs.model.report.ReportDAO#save(au.com.gaiaresources.bdrs.model.report.Report)
    */
    @Override
    public Report save(Report report) {
        return super.save(report);
    }
}