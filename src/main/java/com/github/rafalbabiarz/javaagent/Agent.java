package com.github.rafalbabiarz.javaagent;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtMethod;
import javassist.LoaderClassPath;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

public class Agent {
    public static void premain(String args, Instrumentation inst) {

        inst.addTransformer(new ClassFileTransformer() {
            @Override
            public byte[] transform(ClassLoader classLoader, String s, Class<?> aClass, ProtectionDomain protectionDomain, byte[] bytes) throws IllegalClassFormatException {

                if ("com/github/rafalbabiarz/javaagent/testapp/HelloController".equals(s)) {
                    try {
                        ClassPool cp = ClassPool.getDefault();
                        cp.insertClassPath(new LoaderClassPath(Thread.currentThread().getContextClassLoader()));
                        CtClass cc = cp.get("com.github.rafalbabiarz.javaagent.testapp.HelloController");
                        CtMethod m = cc.getDeclaredMethod("index");
                        m.setBody("return \"modified response\";");
                        byte[] byteCode = cc.toBytecode();
                        cc.detach();
                        return byteCode;
                    } catch (Exception ex) {
                        System.out.println(ex);
                    }
                }

                return null;
            }
        });
    }

}
