package io.micronaut.oraclecloud.serde


import com.oracle.bmc.http.client.HttpProvider
import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel
import io.micronaut.core.beans.BeanIntrospection
import spock.lang.Specification

class SerdeSpecBase extends Specification {

    String serialize(Object requestBody) throws Exception {
        return HttpProvider.getDefault().serializer.writeValueAsString(requestBody)
    }

    <T> T deserialize(String value, Class<T> type) throws Exception {
        return HttpProvider.getDefault().serializer.readValue(value, type)
    }

    ExplicitlySetBmcModel copyExplicitlySet(ExplicitlySetBmcModel from, ExplicitlySetBmcModel to) {
        return ModelUtils.copyExplicitlySet(from, to)
    }

    boolean equalsIgnoreExplicitlySet(ExplicitlySetBmcModel expected, ExplicitlySetBmcModel model) {
        return expected == copyExplicitlySet(expected, model)
    }

    // Class that has access to ExplicitlySetBmcModel protected methods
    static abstract class ModelUtils extends ExplicitlySetBmcModel {
        static ExplicitlySetBmcModel copyExplicitlySet(ExplicitlySetBmcModel from, ExplicitlySetBmcModel to) {
            BeanIntrospection.getIntrospection(((Object) from).getClass()).beanProperties.forEach(p -> {
                if (from.wasPropertyExplicitlySet(p.getName())) {
                    to.markPropertyAsExplicitlySet(p.getName())
                }
            })
            return from
        }
    }
}
