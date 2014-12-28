 package com.llmofang.hinge.agent.compile.visitor;
 


//import org.objectweb.asm.ClassAdapter;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;

import java.util.ArrayList;
import java.util.Arrays;

 public class TraceClassDecorator
 {
   private ClassVisitor adapter;
 
   public TraceClassDecorator(ClassVisitor adapter)
   {
     this.adapter = adapter;
   }
 
   public void addTraceField() {
     this.adapter.visitField(1, "_nr_trace", "Lcom/newrelic/agent/android/tracing/Trace;", null, null);
   }
 
   public static String[] addInterface(String[] interfaces) {
     ArrayList newInterfaces = new ArrayList(Arrays.asList(interfaces));
     newInterfaces.add("com/newrelic/agent/android/api/v2/TraceFieldInterface");
 
     return (String[])newInterfaces.toArray(new String[newInterfaces.size()]);
   }
 
   public void addTraceInterface(final Type ownerType) {
     MethodVisitor mv = this.adapter.visitMethod(1, "_nr_setTrace", "(Lcom/newrelic/agent/android/tracing/Trace;)V", null, null);
 
     Method method = new Method("_nr_setTrace", "(Lcom/newrelic/agent/android/tracing/Trace;)V");
     mv = new GeneratorAdapter(1, method, mv)
     {
       public void visitCode() {
         Label tryStart = new Label();
         Label tryEnd = new Label();
         Label tryHandler = new Label();
 
         super.visitCode();
 
         visitLabel(tryStart);
         loadThis();
         loadArgs();
         putField(ownerType, "_nr_trace", Type.getType("Lcom/newrelic/agent/android/tracing/Trace;"));
         goTo(tryEnd);
 
         visitLabel(tryHandler);
 
         pop();
         visitLabel(tryEnd);
         visitTryCatchBlock(tryStart, tryEnd, tryHandler, "java/lang/Exception");
         visitInsn(177);
       }
     };
     mv.visitCode();
     mv.visitMaxs(0, 0);
     mv.visitEnd();
   }
 }

/* Location:           /home/think/Downloads/newrelic-android-4.120.0/lib/class.rewriter.jar
 * Qualified Name:     com.newrelic.agent.compile.visitor.TraceClassDecorator
 * JD-Core Version:    0.6.2
 */