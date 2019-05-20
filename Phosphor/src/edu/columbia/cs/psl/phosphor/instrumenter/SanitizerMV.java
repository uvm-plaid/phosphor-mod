package edu.columbia.cs.psl.phosphor.instrumenter;

import edu.columbia.cs.psl.phosphor.Configuration;
import edu.columbia.cs.psl.phosphor.runtime.TaintChecker;
import edu.columbia.cs.psl.phosphor.runtime.TaintSourceWrapper;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class SanitizerMV extends MethodVisitor implements Opcodes {

    private String owner;
    private String name;
    private String desc;
    private boolean isStatic;

    public SanitizerMV(MethodVisitor mv, int access, String owner, String name, String desc) {
        super(Configuration.ASM_VERSION, mv);
        this.owner = owner;
        this.name = name;
        this.desc = desc;
        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
    }

    private void callAutoSanitize(int argIndex, String internalName) {
        super.visitFieldInsn(GETSTATIC, Type.getInternalName(Configuration.class), "autoTainter", Type.getDescriptor(TaintSourceWrapper.class));
        super.visitInsn(SWAP);
        super.visitIntInsn(BIPUSH, argIndex);
        super.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(TaintSourceWrapper.class), "sanitize", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
        super.visitTypeInsn(CHECKCAST, internalName);
    }

    private void autoSanitizeArguments() {
        Type[] args = Type.getArgumentTypes(desc);
        int idx = isStatic ? 0 : 1; // skip over the "this" argument for non-static methods
        for (int i = 0; i < args.length; i++) {
            if(Configuration.MULTI_TAINTING) {
                if(args[i].getSort() == Type.OBJECT || args[i].getSort() == Type.ARRAY && args[i].getElementType().getSort() == Type.OBJECT) {
                    super.visitVarInsn(ALOAD, idx); // load the argument onto the stack
                    callAutoSanitize(i, args[i].getInternalName());
                    super.visitVarInsn(ASTORE, idx); // replace the argument with the return of autoTaint
                }
            }
            idx += args[i].getSize();
        }
    }

    @Override
    public void visitCode() {
        super.visitCode();
        System.out.println("code visited");
        //autoSanitizeArguments();

        Type[] args = Type.getArgumentTypes(desc);
        int idx = 0;
        if (!isStatic)
            idx++;
        boolean skipNextArray = false;
        for (int i = 0; i < args.length; i++) {
            if (args[i].getSort() == Type.OBJECT && !args[i].getDescriptor().equals(Configuration.TAINT_TAG_DESC) || args[i].getSort() == Type.ARRAY) {
                super.visitVarInsn(ALOAD, idx);
                super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintSourceWrapper.class), "sanitize", "(Ljava/lang/Object;)V", false);
            } else if (!skipNextArray && args[i].getSort() == Type.ARRAY
                    && (args[i].getElementType().getSort() != Type.OBJECT || args[i].getDescriptor().equals(Configuration.TAINT_TAG_ARRAYDESC)) && args[i].getDimensions() == 1) {
                skipNextArray = true;
                super.visitVarInsn(ALOAD, idx);
                super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintSourceWrapper.class), "sanitize", "(Ljava/lang/Object;)V", false);
            } else if (skipNextArray)
                skipNextArray = false;
            idx += args[i].getSize();

        }
    }

    @Override
    public void visitInsn(int opcode) {
        if (opcode == ARETURN) {
            Type returnType = Type.getReturnType(desc);
            if (returnType.getSort() != Type.VOID && Configuration.MULTI_TAINTING) {
                super.visitInsn(DUP);
                super.visitMethodInsn(INVOKESTATIC, Type.getInternalName(TaintSourceWrapper.class), "sanitize", "(Ljava/lang/Object;)V", false);
            }
        }
        super.visitInsn(opcode);
    }
}
