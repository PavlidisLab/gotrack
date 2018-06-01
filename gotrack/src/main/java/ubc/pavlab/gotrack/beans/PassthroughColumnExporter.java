package ubc.pavlab.gotrack.beans;

import org.apache.commons.lang3.StringUtils;
import org.primefaces.component.api.UIColumn;

import javax.enterprise.context.ApplicationScoped;
import javax.faces.component.UIComponent;
import javax.inject.Named;

/**
 * Created by mjacobson on 30/05/18.
 */
@Named
@ApplicationScoped
public class PassthroughColumnExporter {

    public String export(final UIColumn column) {
        String value = StringUtils.EMPTY;
        for (final UIComponent child : column.getChildren()) {
            final Object exportValue = child.getAttributes().get("data-export");
            if (exportValue != null) {
                value = String.valueOf(exportValue);
                break;
            }
        }

        return value;
    }
}