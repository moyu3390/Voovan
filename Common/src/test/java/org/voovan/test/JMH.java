package org.voovan.test;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.voovan.test.tools.json.TestObject;
import org.voovan.tools.compiler.function.DynamicFunction;
import org.voovan.tools.reflect.TReflect;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.Vector;
import java.util.concurrent.TimeUnit;

/**
 * 类文字命名
 *
 * @author: helyho
 * Voovan Framework.
 * WebSite: https://github.com/helyho/Voovan
 * Licence: Apache v2 License
 */
@BenchmarkMode(Mode.Throughput)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Threads(3)
@Fork(1)
//@OutputTimeUnit(TimeUnit.)
public class JMH {
    static String m = new String("abcdabcdefghijkabcdefghijkabcdefghijkabcdefghijkabcdefghijkabcdefghijkabcdefghijkefghijk");
    static TestObject testObject = new TestObject();
    static Vector ml = new Vector();
    static Method method = TReflect.findMethod(TestObject.class, "getData", 2)[0];
    static Constructor constructor = TReflect.findConstructor(TestObject.class, 0)[0];
    static DynamicFunction methodFunction = TReflect.genMethodInvoker(TestObject.class, method);
    static DynamicFunction constructorFunction = TReflect.genConstructorInvoker(TestObject.class, constructor);

    static {
        testObject.setBint(11);
    }

    @Benchmark
    public static void nativeCall() throws Exception {
        for(int i=0;i<10000;i++) {
//            constructorFunction.call(new Object[]{new Object[0]});
            methodFunction.call(testObject, new Object[]{"123123", 111});
        }
    }

    @Benchmark
    public void reflectCall() throws Exception {
        for(int i=0;i<10000;i++) {
//            TReflect.newInstance(TestObject.class, new Object[0]);
            TReflect.invokeMethod(testObject, "getData", "123123", 111);
        }
    }

    @Benchmark
    public void directCall() {
        for(int i=0;i<10000;i++) {
//            new TestObject();
            testObject.getData("123123", 111);
        }
    }

    public static void main(String[] args) throws Exception {
        TReflect.register(TestObject.class);

//        TReflect.invokeMethod(testObject, "getData", "123123", 111);
        constructorFunction.call(new Object[]{new Object[0]});

        Options options = new OptionsBuilder()
                .include(JMH.class.getSimpleName())
                .build();
        new Runner(options).run();
    }
}
