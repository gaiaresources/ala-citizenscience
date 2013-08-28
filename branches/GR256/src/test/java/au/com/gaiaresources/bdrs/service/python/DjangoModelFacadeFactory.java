package au.com.gaiaresources.bdrs.service.python;

import org.hibernate.Session;
import org.hibernate.metadata.ClassMetadata;

import java.util.HashMap;
import java.util.Map;

/**
 * A repository that maps hibernate entities to django representations of those entities.
 */
public class DjangoModelFacadeFactory {

    private Map<Class<?>, DjangoModelFacade> facadeMap = new HashMap<Class<?>, DjangoModelFacade>();

    /**
     * Creates a new instance.
     * @param sesh a session created with the factory to be used in retrieving all hibernate entities.
     * @param handler a handler to be invoked when retrieving django representations of the entities.
     */
    public DjangoModelFacadeFactory(Session sesh, DjangoModelFacadeInitHandler handler) throws Exception {
        Map<String, ClassMetadata> classMetadataMap = sesh.getSessionFactory().getAllClassMetadata();

        for (Map.Entry<String, ClassMetadata> entry : classMetadataMap.entrySet()) {
            ClassMetadata md = entry.getValue();
            Class<?> entityClass = md.getMappedClass(sesh.getEntityMode());
            facadeMap.put(entityClass, new DjangoModelFacade(entityClass, handler));
        }
    }

    /**
     * Returns a django representation of the specified hibernate entity.
     * @param entityClass the hibernate entity whose django counterpart is to be retrieved.
     * @return the django representation of the specified hibernate entity.
     */
    public DjangoModelFacade getDjangoModelFacade(Class<?> entityClass) {
        return this.facadeMap.get(entityClass);
    }
}
