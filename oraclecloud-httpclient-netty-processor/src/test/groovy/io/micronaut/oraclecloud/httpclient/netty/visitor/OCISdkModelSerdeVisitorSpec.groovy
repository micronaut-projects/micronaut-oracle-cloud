package io.micronaut.oraclecloud.httpclient.netty.visitor

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.micronaut.core.type.Argument

class OCISdkModelSerdeVisitorSpec extends AbstractTypeElementSpec {
    final static String ANN_SERDEABLE = "io.micronaut.serde.annotation.Serdeable"

    void "test oci sdk model is serdeable"() {
        given:
        def introspection = buildBeanIntrospection('test.Test','''
package test;

import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel;

class Test extends ExplicitlySetBmcModel {
    String a;

    Test(String a) {
        this.a = a;
    }
}
''')

        expect:
        introspection != null
        introspection.getAnnotationNames().contains(ANN_SERDEABLE)
    }

    void "test oci sdk child model is serdeable"() {
        given:
        def introspection = buildBeanIntrospection('test.TestChild','''
package test;

import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel;

class TestModel extends ExplicitlySetBmcModel {
    private String a;

    TestModel(String a) {
        this.a = a;
    }

    public String getA() {
        return a;
    }
}

class TestChild extends TestModel {
    private String b;

    TestChild(String a, String b) {
        super(a);
        this.b = b;
    }

    public String getB() {
        return b;
    }
}
''')

        expect:
        introspection != null
        introspection.getAnnotationNames().contains(ANN_SERDEABLE)
    }

    void "test oci model elements are nullable by default"() {
        given:
        def introspection = buildBeanIntrospection('test.TestModel','''
package test;

import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel;

class TestModel extends ExplicitlySetBmcModel {
    private String a;
    private Integer b;

    TestModel(String a, Integer b) {
        this.a = a;
        this.b = b;
    }

    public String getA() {
        return a;
    }

    public Integer getB() {
        return b;
    }
}
''')

        expect:
        introspection != null

        introspection.getConstructorArguments().length == 2
        introspection.getConstructorArguments()[0].isNullable()
        introspection.getConstructorArguments()[1].isNullable()

        when:
        Object[] arguments = [null, null]
        introspection.instantiate(arguments)

        then:
        notThrown(InstantiationException)

        when:
        arguments = [null, 2]
        introspection.instantiate(arguments)

        then:
        notThrown(InstantiationException)
    }

    void "test oci model elements are nullable by default for child model"() {
        given:
        def introspection = buildBeanIntrospection('test.ChildModel','''
package test;

import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel;

class TestModel extends ExplicitlySetBmcModel {
    private String a;

    TestModel(String a) {
        this.a = a;
    }

    public String getA() {
        return a;
    }
}

class ChildModel extends TestModel {
    private Integer b;

    ChildModel(String a, Integer b) {
        super(a);
        this.b = b;
    }

    public Integer getB() {
        return b;
    }
}
''')

        expect:
        introspection != null

        introspection.getConstructorArguments().length == 2
        introspection.getConstructorArguments()[0].isNullable()
        introspection.getConstructorArguments()[1].isNullable()

        when:
        Object[] arguments = [null, null]
        introspection.instantiate(arguments)

        then:
        notThrown(InstantiationException)
    }

    void "test oci enum is serdeable"() {
        given:
        def introspection = buildBeanIntrospection('test.TestEnum','''
package test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.oracle.bmc.http.internal.BmcEnum;

enum TestEnum implements BmcEnum {
    STOPPED,
    RUNNING,
    DEFAULT
}
''')
        expect:
        introspection != null
        introspection.hasStereotype(ANN_SERDEABLE)
    }

    void "test oci enum creator is nullable"() {
        given:
        def introspection = buildBeanIntrospection('test.TestEnum','''
package test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.oracle.bmc.http.internal.BmcEnum;

enum TestEnum implements BmcEnum {
    STOPPED("stopped"),
    RUNNING("running"),
    DEFAULT(null);

    final String value;

    TestEnum(String value) {
        this.value = value;
    }

    @JsonCreator
    public static TestEnum create(String value) {
        switch(value) {
            case "stopped": return STOPPED;
            case "running": return RUNNING;
            default: return DEFAULT;
        }
    }

    @JsonValue
    public String getValue() {
        return null;
    }
}
''')

        expect:
        introspection != null
        introspection.hasStereotype(ANN_SERDEABLE)

        when:
        Argument<?> argument = introspection.getConstructorArguments().first()

        then:
        argument.isNullable()
    }
}
