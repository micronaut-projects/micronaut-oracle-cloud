package io.micronaut.oraclecloud.httpclient.netty.visitor

import io.micronaut.annotation.processing.test.AbstractTypeElementSpec
import io.micronaut.core.beans.BeanIntrospection
import io.micronaut.core.naming.NameUtils
import io.micronaut.core.type.Argument

class OCISdkModelSerdeVisitorSpec extends AbstractTypeElementSpec {
    final static String ANN_SERDEABLE = "io.micronaut.serde.annotation.Serdeable"

    void "test oci sdk model is serdeable"() {
        given:
        def introspection = buildBeanIntrospection('test.introspection.Test','''
package test;

import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel;

public class Test extends ExplicitlySetBmcModel {
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

    void "test oci sdk inner model is serdeable"() {
        given:
        def classLoader = buildClassLoader('test.introspection.Test','''
package test;

import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class Test extends ExplicitlySetBmcModel {

    @JsonDeserialize
    public static class TestA extends ExplicitlySetBmcModel {
        String a;

        TestA(String a) {
            this.a = a;
        }
    }
}
''')

        expect:
        var introspectionName = 'test.introspection.$Test$TestA$Introspection'
        var introspection = classLoader.loadClass(introspectionName).newInstance(new Object[0]) as BeanIntrospection
        introspection != null
        introspection.hasStereotype(ANN_SERDEABLE)
    }

    void "test oci sdk child model is serdeable"() {
        given:
        def introspection = buildBeanIntrospection('test.introspection.TestChild','''
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

public class TestChild extends TestModel {
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
        def introspection = buildBeanIntrospection('test.introspection.TestModel','''
package test;

import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel;

public class TestModel extends ExplicitlySetBmcModel {
    private String a;
    private Integer b;

    public TestModel(String a, Integer b) {
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
        def introspection = buildBeanIntrospection('test.introspection.ChildModel','''
package test;

import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel;

class TestModel extends ExplicitlySetBmcModel {
    private String a;

    public TestModel(String a) {
        this.a = a;
    }

    public String getA() {
        return a;
    }
}

public class ChildModel extends TestModel {
    private Integer b;

    public ChildModel(String a, Integer b) {
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
        def introspection = buildBeanIntrospection('test.introspection.TestEnum','''
package test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.oracle.bmc.http.internal.BmcEnum;

public enum TestEnum implements BmcEnum {
    STOPPED,
    RUNNING,
    DEFAULT;

    public String getValue() {
        return null;
    }
}
''')
        expect:
        introspection != null
        introspection.hasStereotype(ANN_SERDEABLE)
    }

    void "test oci enum creator is nullable"() {
        given:
        def introspection = buildBeanIntrospection('test.introspection.TestEnum','''
package test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.oracle.bmc.http.internal.BmcEnum;

public enum TestEnum implements BmcEnum {
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

    void "test oci inner enum is serdeable"() {
        given:
        def classLoader = buildClassLoader('test.introspection.TestClass','''
package test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.oracle.bmc.http.client.internal.ExplicitlySetBmcModel;
import com.oracle.bmc.http.internal.BmcEnum;

public class TestClass extends ExplicitlySetBmcModel {
    @JsonDeserialize
    public enum TestEnum implements BmcEnum {
        STOPPED,
        RUNNING,
        DEFAULT;

        public String getValue() {
            return null;
        }
    }
}
''')
        expect:
        var introspectionName = 'test.introspection.$TestClass$TestEnum$Introspection'
        var introspection = classLoader.loadClass(introspectionName).newInstance(new Object[0]) as BeanIntrospection
        introspection != null
        introspection.hasStereotype(ANN_SERDEABLE)
    }

    @Override
    protected BeanIntrospection buildBeanIntrospection(String className, String cls) {
        className = NameUtils.getPackageName(className) + "." + NameUtils.getSimpleName(className)
        return super.buildBeanIntrospection(className, cls)
    }
}
