package au.com.gaiaresources.bdrs.service.python;

import au.com.gaiaresources.bdrs.json.JSONObject;
import org.apache.commons.lang.StringUtils;

import javax.persistence.Table;
import java.util.List;

/**
 * A Java facade over a serialised Django representation of a hibernate entity.
 */
public class DjangoModelFacade {

    private Class<?> beanClass;
    private JSONObject djangoModels;

    private String dbTableName;

    /**
     * Creates a new instance.
     * @param beanClass the hibernate entity to be represented.
     * @param initHandler handles the communications with django to retrieve the django representation of the entity.
     */
    public DjangoModelFacade(Class<?> beanClass, DjangoModelFacadeInitHandler initHandler) throws Exception {
        this.beanClass = beanClass;
        this.dbTableName = getTableName();
        this.djangoModels = initHandler.getModelData(getDjangoModelName(this.dbTableName));
    }

    /**
     * Returns the django side listing of column names.
     * @return
     */
    public List<String> getColumnNames() {
        return djangoModels.getJSONArray("columns");
    }

    /**
     * @param pk the primary key of the instance to be retrieved.
     * @return the django side representation of the instance with the specified primary key.
     */
    public JSONObject getObject(String pk) {
        return this.djangoModels.getJSONObject("objects").optJSONObject(pk);
    }

    private String getDjangoModelName(String tableName) {
        String[] split = tableName.toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();
        for (String part : split) {
            builder.append(StringUtils.capitalize(part));
        }
        return builder.toString();
    }

    private String getTableName() {
        if (this.beanClass == null) {
            return null;
        }

        Table tableAnnotation = this.beanClass.getAnnotation(Table.class);
        if (tableAnnotation == null) {
            return null;
        }

        return tableAnnotation.name();
    }

    public String toString() {
        return this.djangoModels.toJSONString();
    }
}
