

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.StandardLocation;
import javax.tools.ToolProvider;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskListener;
import com.sun.source.util.Trees;
import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.SymbolMetadata;
import com.sun.tools.javac.code.Attribute;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.code.Kinds;
import com.sun.tools.javac.code.Printer;
import com.sun.tools.javac.code.Scope;
import com.sun.tools.javac.code.Scope.CompoundScope;
import com.sun.tools.javac.code.Symbol;
import com.sun.tools.javac.code.Symbol.*;
import com.sun.tools.javac.code.Type;
import com.sun.tools.javac.code.Type.*;
import com.sun.tools.javac.code.TypeTag;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.JCTree.*;
import com.sun.tools.javac.tree.Pretty;
import com.sun.tools.javac.tree.TreeInfo;
import com.sun.tools.javac.tree.TreeScanner;
import com.sun.tools.javac.util.Assert;
import com.sun.tools.javac.util.Context;
import com.sun.tools.javac.util.Log;




public class DPrinter {
    protected final PrintWriter out;
    protected final Trees trees;
    protected Printer printer;
    protected boolean showEmptyItems = true;
    protected boolean showNulls = true;
    protected boolean showPositions = false;
    protected boolean showSrc;
    protected boolean showTreeSymbols;
    protected boolean showTreeTypes;
    protected int maxSrcLength = 32;
    protected Locale locale = Locale.getDefault();
    protected static final String NULL = "#null";

    

    public static DPrinter instance(Context context) {
        DPrinter dp = context.get(DPrinter.class);
        if (dp == null) {
            dp = new DPrinter(context);
        }
        return dp;

    }

    protected DPrinter(Context context) {
        context.put(DPrinter.class, this);
        out = context.get(Log.outKey);
        trees = JavacTrees.instance(context);
    }

    public DPrinter(PrintWriter out, Trees trees) {
        this.out = out;
        this.trees = trees;
    }

    public DPrinter emptyItems(boolean showEmptyItems) {
        this.showEmptyItems = showEmptyItems;
        return this;
    }

    public DPrinter nulls(boolean showNulls) {
        this.showNulls = showNulls;
        return this;
    }

    public DPrinter positions(boolean showPositions) {
        this.showPositions = showPositions;
        return this;
    }

    public DPrinter source(boolean showSrc) {
        this.showSrc = showSrc;
        return this;
    }

    public DPrinter source(int maxSrcLength) {
        this.showSrc = true;
        this.maxSrcLength = maxSrcLength;
        return this;
    }

    public DPrinter treeSymbols(boolean showTreeSymbols) {
        this.showTreeSymbols = showTreeSymbols;
        return this;
    }

    public DPrinter treeTypes(boolean showTreeTypes) {
        this.showTreeTypes = showTreeTypes;
        return this;
    }

    public DPrinter typeSymbolPrinter(Printer p) {
        printer = p;
        return this;
    }

    

    

    protected enum Details {
        
        SUMMARY,
        
        FULL
    };

    public void printAnnotations(String label, SymbolMetadata annotations) {
        printAnnotations(label, annotations, Details.FULL);
    }

    protected void printAnnotations(String label, SymbolMetadata annotations, Details details) {
        if (annotations == null) {
            printNull(label);
        } else {
            

            
            Object DECL_NOT_STARTED = getField(null, SymbolMetadata.class, "DECL_NOT_STARTED");
            Object DECL_IN_PROGRESS = getField(null, SymbolMetadata.class, "DECL_IN_PROGRESS");
            Object attributes = getField(annotations, SymbolMetadata.class, "attributes");
            Object type_attributes = getField(annotations, SymbolMetadata.class, "type_attributes");

            if (!showEmptyItems) {
                if (attributes instanceof List && ((List) attributes).isEmpty()
                        && attributes != DECL_NOT_STARTED
                        && attributes != DECL_IN_PROGRESS
                        && type_attributes instanceof List && ((List) type_attributes).isEmpty())
                    return;
            }

            printString(label, hashString(annotations));

            indent(+1);
            if (attributes == DECL_NOT_STARTED)
                printString("attributes", "DECL_NOT_STARTED");
            else if (attributes == DECL_IN_PROGRESS)
                printString("attributes", "DECL_IN_PROGRESS");
            else if (attributes instanceof List)
                printList("attributes", (List) attributes);
            else
                printObject("attributes", attributes, Details.SUMMARY);

            if (attributes instanceof List)
                printList("type_attributes", (List) type_attributes);
            else
                printObject("type_attributes", type_attributes, Details.SUMMARY);
            indent(-1);
        }
    }

    public void printAttribute(String label, Attribute attr) {
        if (attr == null) {
            printNull(label);
        } else {
            printString(label, attr.getClass().getSimpleName());

            indent(+1);
            attr.accept(attrVisitor);
            indent(-1);
        }
    }

    public void printFileObject(String label, FileObject fo) {
        if (fo == null) {
            printNull(label);
        } else {
            printString(label, fo.getName());
        }
    }

    protected <T> void printImplClass(T item, Class<? extends T> stdImplClass) {
        if (item.getClass() != stdImplClass)
            printString("impl", item.getClass().getName());
    }

    public void printInt(String label, int i) {
        printString(label, String.valueOf(i));
    }

    public void printList(String label, List<?> list) {
        if (list == null) {
             printNull(label);
        } else if (!list.isEmpty() || showEmptyItems) {
            printString(label, "[" + list.size() + "]");

            indent(+1);
            int i = 0;
            for (Object item: list) {
                printObject(String.valueOf(i++), item, Details.FULL);
            }
            indent(-1);
        }
    }

    public void printName(String label, Name name) {
        if (name == null) {
            printNull(label);
        } else {
            printString(label, name.toString());
        }
    }

    public void printNull(String label) {
        if (showNulls)
            printString(label, NULL);
    }

    protected void printObject(String label, Object item, Details details) {
        if (item == null) {
            printNull(label);
        } else if (item instanceof Attribute) {
            printAttribute(label, (Attribute) item);
        } else if (item instanceof Symbol) {
            printSymbol(label, (Symbol) item, details);
        } else if (item instanceof Type) {
            printType(label, (Type) item, details);
        } else if (item instanceof JCTree) {
            printTree(label, (JCTree) item);
        } else if (item instanceof List) {
            printList(label, (List) item);
        } else if (item instanceof Name) {
            printName(label, (Name) item);
        } else {
            printString(label, String.valueOf(item));
        }
    }

    public void printScope(String label, Scope scope) {
        printScope(label, scope, Details.FULL);
    }

    public void printScope(String label, Scope scope, Details details) {
        if (scope == null) {
            printNull(label);
        } else {
            switch (details) {
                case SUMMARY: {
                    indent();
                    out.print(label);
                    out.print(": [");
                    String sep = "";
                    for (Symbol sym: scope.getElements()) {
                        out.print(sep);
                        out.print(sym.name);
                        sep = ",";
                    }
                    out.println("]");
                    break;
                }

                case FULL: {
                    indent();
                    out.println(label);

                    indent(+1);
                    printImplClass(scope, Scope.class);
                    printSymbol("owner", scope.owner, Details.SUMMARY);
                    printScope("next", scope.next, Details.SUMMARY);
                    printObject("shared", getField(scope, Scope.class, "shared"), Details.SUMMARY);
                    if (scope instanceof CompoundScope) {
                        printObject("subScopes",
                                getField(scope, CompoundScope.class, "subScopes"),
                                Details.FULL);
                    } else {
                        for (Symbol sym : scope.getElements()) {
                            printSymbol(sym.name.toString(), sym, Details.SUMMARY);
                        }
                    }
                    indent(-1);
                    break;
                }
            }
        }
    }

    public void printSource(String label, JCTree tree) {
        printString(label, Pretty.toSimpleString(tree, maxSrcLength));
    }

    public void printString(String label, String text) {
        indent();
        out.print(label);
        out.print(": ");
        out.print(text);
        out.println();
    }

    public void printSymbol(String label, Symbol symbol) {
        printSymbol(label, symbol, Details.FULL);
    }

    protected void printSymbol(String label, Symbol sym, Details details) {
        if (sym == null) {
            printNull(label);
        } else {
            switch (details) {
            case SUMMARY:
                printString(label, toString(sym));
                break;

            case FULL:
                indent();
                out.print(label);
                out.println(": " +
                        info(sym.getClass(),
                            String.format("0x%x--%s", sym.kind, Kinds.kindName(sym)),
                            sym.getKind())
                        + " " + sym.name
                        + " " + hashString(sym));

                indent(+1);
                if (showSrc) {
                    JCTree tree = (JCTree) trees.getTree(sym);
                    if (tree != null)
                        printSource("src", tree);
                }
                printString("flags", String.format("0x%x--%s",
                        sym.flags_field, Flags.toString(sym.flags_field)));
                printObject("completer", sym.completer, Details.SUMMARY); 
                printSymbol("owner", sym.owner, Details.SUMMARY);
                printType("type", sym.type, Details.SUMMARY);
                printType("erasure", sym.erasure_field, Details.SUMMARY);
                sym.accept(symVisitor, null);
                printAnnotations("annotations", sym.getAnnotations(), Details.SUMMARY);
                indent(-1);
            }
        }
    }

    protected String toString(Symbol sym) {
        return (printer != null) ? printer.visit(sym, locale) : String.valueOf(sym);
    }

    protected void printTree(String label, JCTree tree) {
        if (tree == null) {
            printNull(label);
        } else {
            indent();
            String ext;
            try {
                ext = tree.getKind().name();
            } catch (Throwable t) {
                ext = "n/a";
            }
            out.print(label + ": " + info(tree.getClass(), tree.getTag(), ext));
            if (showPositions) {
                
                
                out.print(" pos:" + tree.pos);
            }
            if (showTreeTypes && tree.type != null)
                out.print(" type:" + toString(tree.type));
            Symbol sym;
            if (showTreeSymbols && (sym = TreeInfo.symbolFor(tree)) != null)
                out.print(" sym:" + toString(sym));
            out.println();

            indent(+1);
            if (showSrc) {
                indent();
                out.println("src: " + Pretty.toSimpleString(tree, maxSrcLength));
            }
            tree.accept(treeVisitor);
            indent(-1);
        }
    }

    public void printType(String label, Type type) {
        printType(label, type, Details.FULL);
    }

    protected void printType(String label, Type type, Details details) {
        if (type == null)
            printNull(label);
        else {
            switch (details) {
                case SUMMARY:
                    printString(label, toString(type));
                    break;

                case FULL:
                    indent();
                    out.print(label);
                    out.println(": " + info(type.getClass(), type.getTag(), type.getKind())
                            + " " + hashString(type));

                    indent(+1);
                    printSymbol("tsym", type.tsym, Details.SUMMARY);
                    printObject("constValue", type.constValue(), Details.SUMMARY);
                    printObject("annotations", type.getAnnotationMirrors(), Details.SUMMARY);
                    type.accept(typeVisitor, null);
                    indent(-1);
            }
        }
    }

    protected String toString(Type type) {
        return (printer != null) ? printer.visit(type, locale) : String.valueOf(type);
    }

    protected String hashString(Object obj) {
        return String.format("#%x", obj.hashCode());
    }

    protected String info(Class<?> clazz, Object internal, Object external) {
        return String.format("%s,%s,%s", clazz.getSimpleName(), internal, external);
    }

    private int indent = 0;

    protected void indent() {
        for (int i = 0; i < indent; i++) {
            out.print("  ");
        }
    }

    protected void indent(int n) {
        indent += n;
    }

    protected Object getField(Object o, Class<?> clazz, String name) {
        try {
            Field f = clazz.getDeclaredField(name);
            boolean prev = f.isAccessible();
            f.setAccessible(true);
            try {
                return f.get(o);
            } finally {
                f.setAccessible(prev);
            }
        } catch (ReflectiveOperationException e) {
            return e;
        } catch (SecurityException e) {
            return e;
        }
    }

    

    

    protected JCTree.Visitor treeVisitor = new TreeVisitor();

    
    public class TreeVisitor extends JCTree.Visitor {
        @Override
        public void visitTopLevel(JCCompilationUnit tree) {
            printList("packageAnnotations", tree.packageAnnotations);
            printTree("pid", tree.pid);
            printList("defs", tree.defs);
        }

        @Override
        public void visitImport(JCImport tree) {
            printTree("qualid", tree.qualid);
        }

        @Override
        public void visitClassDef(JCClassDecl tree) {
            printName("name", tree.name);
            printTree("mods", tree.mods);
            printList("typarams", tree.typarams);
            printTree("extending", tree.extending);
            printList("implementing", tree.implementing);
            printList("defs", tree.defs);
        }

        @Override
        public void visitMethodDef(JCMethodDecl tree) {
            printName("name", tree.name);
            printTree("mods", tree.mods);
            printTree("restype", tree.restype);
            printList("typarams", tree.typarams);
            printTree("recvparam", tree.recvparam);
            printList("params", tree.params);
            printList("thrown", tree.thrown);
            printTree("defaultValue", tree.defaultValue);
            printTree("body", tree.body);
        }

        @Override
        public void visitVarDef(JCVariableDecl tree) {
            printName("name", tree.name);
            printTree("mods", tree.mods);
            printTree("vartype", tree.vartype);
            printTree("init", tree.init);
        }

        @Override
        public void visitSkip(JCSkip tree) {
        }

        @Override
        public void visitBlock(JCBlock tree) {
            printList("stats", tree.stats);
        }

        @Override
        public void visitDoLoop(JCDoWhileLoop tree) {
            printTree("body", tree.body);
            printTree("cond", tree.cond);
        }

        @Override
        public void visitWhileLoop(JCWhileLoop tree) {
            printTree("cond", tree.cond);
            printTree("body", tree.body);
        }

        @Override
        public void visitForLoop(JCForLoop tree) {
            printList("init", tree.init);
            printTree("cond", tree.cond);
            printList("step", tree.step);
            printTree("body", tree.body);
        }

        @Override
        public void visitForeachLoop(JCEnhancedForLoop tree) {
            printTree("var", tree.var);
            printTree("expr", tree.expr);
            printTree("body", tree.body);
        }

        @Override
        public void visitLabelled(JCLabeledStatement tree) {
            printTree("body", tree.body);
        }

        @Override
        public void visitSwitch(JCSwitch tree) {
            printTree("selector", tree.selector);
            printList("cases", tree.cases);
        }

        @Override
        public void visitCase(JCCase tree) {
            printTree("pat", tree.pat);
            printList("stats", tree.stats);
        }

        @Override
        public void visitSynchronized(JCSynchronized tree) {
            printTree("lock", tree.lock);
            printTree("body", tree.body);
        }

        @Override
        public void visitTry(JCTry tree) {
            printList("resources", tree.resources);
            printTree("body", tree.body);
            printList("catchers", tree.catchers);
            printTree("finalizer", tree.finalizer);
        }

        @Override
        public void visitCatch(JCCatch tree) {
            printTree("param", tree.param);
            printTree("body", tree.body);
        }

        @Override
        public void visitConditional(JCConditional tree) {
            printTree("cond", tree.cond);
            printTree("truepart", tree.truepart);
            printTree("falsepart", tree.falsepart);
        }

        @Override
        public void visitIf(JCIf tree) {
            printTree("cond", tree.cond);
            printTree("thenpart", tree.thenpart);
            printTree("elsepart", tree.elsepart);
        }

        @Override
        public void visitExec(JCExpressionStatement tree) {
            printTree("expr", tree.expr);
        }

        @Override
        public void visitBreak(JCBreak tree) {
            printName("label", tree.label);
        }

        @Override
        public void visitContinue(JCContinue tree) {
            printName("label", tree.label);
        }

        @Override
        public void visitReturn(JCReturn tree) {
            printTree("expr", tree.expr);
        }

        @Override
        public void visitThrow(JCThrow tree) {
            printTree("expr", tree.expr);
        }

        @Override
        public void visitAssert(JCAssert tree) {
            printTree("cond", tree.cond);
            printTree("detail", tree.detail);
        }

        @Override
        public void visitApply(JCMethodInvocation tree) {
            printList("typeargs", tree.typeargs);
            printTree("meth", tree.meth);
            printList("args", tree.args);
        }

        @Override
        public void visitNewClass(JCNewClass tree) {
            printTree("encl", tree.encl);
            printList("typeargs", tree.typeargs);
            printTree("clazz", tree.clazz);
            printList("args", tree.args);
            printTree("def", tree.def);
        }

        @Override
        public void visitNewArray(JCNewArray tree) {
            printList("annotations", tree.annotations);
            printTree("elemtype", tree.elemtype);
            printList("dims", tree.dims);
            printList("dimAnnotations", tree.dimAnnotations);
            printList("elems", tree.elems);
        }

        @Override
        public void visitLambda(JCLambda tree) {
            printTree("body", tree.body);
            printList("params", tree.params);
        }

        @Override
        public void visitParens(JCParens tree) {
            printTree("expr", tree.expr);
        }

        @Override
        public void visitAssign(JCAssign tree) {
            printTree("lhs", tree.lhs);
            printTree("rhs", tree.rhs);
        }

        @Override
        public void visitAssignop(JCAssignOp tree) {
            printTree("lhs", tree.lhs);
            printTree("rhs", tree.rhs);
        }

        @Override
        public void visitUnary(JCUnary tree) {
            printTree("arg", tree.arg);
        }

        @Override
        public void visitBinary(JCBinary tree) {
            printTree("lhs", tree.lhs);
            printTree("rhs", tree.rhs);
        }

        @Override
        public void visitTypeCast(JCTypeCast tree) {
            printTree("clazz", tree.clazz);
            printTree("expr", tree.expr);
        }

        @Override
        public void visitTypeTest(JCInstanceOf tree) {
            printTree("expr", tree.expr);
            printTree("clazz", tree.clazz);
        }

        @Override
        public void visitIndexed(JCArrayAccess tree) {
            printTree("indexed", tree.indexed);
            printTree("index", tree.index);
        }

        @Override
        public void visitSelect(JCFieldAccess tree) {
            printTree("selected", tree.selected);
        }

        @Override
        public void visitReference(JCMemberReference tree) {
            printTree("expr", tree.expr);
            printList("typeargs", tree.typeargs);
        }

        @Override
        public void visitIdent(JCIdent tree) {
            printName("name", tree.name);
        }

        @Override
        public void visitLiteral(JCLiteral tree) {
            printString("value", Pretty.toSimpleString(tree, 32));
        }

        @Override
        public void visitTypeIdent(JCPrimitiveTypeTree tree) {
            printString("typetag", tree.typetag.name());
        }

        @Override
        public void visitTypeArray(JCArrayTypeTree tree) {
            printTree("elemtype", tree.elemtype);
        }

        @Override
        public void visitTypeApply(JCTypeApply tree) {
            printTree("clazz", tree.clazz);
            printList("arguments", tree.arguments);
        }

        @Override
        public void visitTypeUnion(JCTypeUnion tree) {
            printList("alternatives", tree.alternatives);
        }

        @Override
        public void visitTypeIntersection(JCTypeIntersection tree) {
            printList("bounds", tree.bounds);
        }

        @Override
        public void visitTypeParameter(JCTypeParameter tree) {
            printName("name", tree.name);
            printList("annotations", tree.annotations);
            printList("bounds", tree.bounds);
        }

        @Override
        public void visitWildcard(JCWildcard tree) {
            printTree("kind", tree.kind);
            printTree("inner", tree.inner);
        }

        @Override
        public void visitTypeBoundKind(TypeBoundKind tree) {
            printString("kind", tree.kind.name());
        }

        @Override
        public void visitModifiers(JCModifiers tree) {
            printList("annotations", tree.annotations);
            printString("flags", String.valueOf(Flags.asFlagSet(tree.flags)));
        }

        @Override
        public void visitAnnotation(JCAnnotation tree) {
            printTree("annotationType", tree.annotationType);
            printList("args", tree.args);
        }

        @Override
        public void visitAnnotatedType(JCAnnotatedType tree) {
            printList("annotations", tree.annotations);
            printTree("underlyingType", tree.underlyingType);
        }

        @Override
        public void visitErroneous(JCErroneous tree) {
            printList("errs", tree.errs);
        }

        @Override
        public void visitLetExpr(LetExpr tree) {
            printList("defs", tree.defs);
            printTree("expr", tree.expr);
        }

        @Override
        public void visitTree(JCTree tree) {
            Assert.error();
        }
    }

    

    

    protected Symbol.Visitor<Void,Void> symVisitor = new SymbolVisitor();

    
    class SymbolVisitor implements Symbol.Visitor<Void,Void> {
        @Override
        public Void visitClassSymbol(ClassSymbol sym, Void ignore) {
            printName("fullname", sym.fullname);
            printName("flatname", sym.flatname);
            printScope("members", sym.members_field);
            printFileObject("sourcefile", sym.sourcefile);
            printFileObject("classfile", sym.classfile);
            
            
            return visitTypeSymbol(sym, null);
        }

        @Override
        public Void visitMethodSymbol(MethodSymbol sym, Void ignore) {
            
            printList("params", sym.params);
            printList("savedParameterNames", sym.savedParameterNames);
            return visitSymbol(sym, null);
        }

        @Override
        public Void visitPackageSymbol(PackageSymbol sym, Void ignore) {
            printName("fullname", sym.fullname);
            printScope("members", sym.members_field);
            printSymbol("package-info", sym.package_info, Details.SUMMARY);
            return visitTypeSymbol(sym, null);
        }

        @Override
        public Void visitOperatorSymbol(OperatorSymbol sym, Void ignore) {
            printInt("opcode", sym.opcode);
            return visitMethodSymbol(sym, null);
        }

        @Override
        public Void visitVarSymbol(VarSymbol sym, Void ignore) {
            printInt("pos", sym.pos);
            printInt("adm", sym.adr);
            
            
            
            printObject("data", getField(sym, VarSymbol.class, "data"), Details.SUMMARY);
            return visitSymbol(sym, null);
        }

        @Override
        public Void visitTypeSymbol(TypeSymbol sym, Void ignore) {
            return visitSymbol(sym, null);
        }

        @Override
        public Void visitSymbol(Symbol sym, Void ignore) {
            return null;
        }
    }

    

    

    protected Type.Visitor<Void,Void> typeVisitor = new TypeVisitor();

    
    public class TypeVisitor implements Type.Visitor<Void,Void> {
        public Void visitAnnotatedType(AnnotatedType type, Void ignore) {
            printList("typeAnnotations", type.getAnnotationMirrors());
            printType("underlyingType", type.unannotatedType(), Details.FULL);
            return visitType(type, null);
        }

        public Void visitArrayType(ArrayType type, Void ignore) {
            printType("elemType", type.elemtype, Details.FULL);
            return visitType(type, null);
        }

        public Void visitCapturedType(CapturedType type, Void ignore) {
            printType("wildcard", type.wildcard, Details.FULL);
            return visitTypeVar(type, null);
        }

        public Void visitClassType(ClassType type, Void ignore) {
            printType("outer", type.getEnclosingType(), Details.SUMMARY);
            printList("typarams", type.typarams_field);
            printList("allparams", type.allparams_field);
            printType("supertype", type.supertype_field, Details.SUMMARY);
            printList("interfaces", type.interfaces_field);
            printList("allinterfaces", type.all_interfaces_field);
            return visitType(type, null);
        }

        public Void visitErrorType(ErrorType type, Void ignore) {
            printType("originalType", type.getOriginalType(), Details.FULL);
            return visitClassType(type, null);
        }

        public Void visitForAll(ForAll type, Void ignore) {
            printList("tvars", type.tvars);
            return visitDelegatedType(type);
        }

        public Void visitMethodType(MethodType type, Void ignore) {
            printList("argtypes", type.argtypes);
            printType("restype", type.restype, Details.FULL);
            printList("thrown", type.thrown);
            return visitType(type, null);
        }

        public Void visitPackageType(PackageType type, Void ignore) {
            return visitType(type, null);
        }

        public Void visitTypeVar(TypeVar type, Void ignore) {
            
            
            
            if (!type.hasTag(TypeTag.TYPEVAR)
                    || !(type.bound == null || type.bound.hasTag(TypeTag.BOT))) {
                printType("bound", type.bound, Details.FULL);
            }
            printType("lower", type.lower, Details.FULL);
            return visitType(type, null);
        }

        public Void visitUndetVar(UndetVar type, Void ignore) {
            for (UndetVar.InferenceBound ib: UndetVar.InferenceBound.values())
                printList("bounds." + ib, type.getBounds(ib));
            printInt("declaredCount", type.declaredCount);
            printType("inst", type.inst, Details.SUMMARY);
            return visitDelegatedType(type);
        }

        public Void visitWildcardType(WildcardType type, Void ignore) {
            printType("type", type.type, Details.SUMMARY);
            printString("kind", type.kind.name());
            printType("bound", type.bound, Details.SUMMARY);
            return visitType(type, null);
        }

        protected Void visitDelegatedType(DelegatedType type) {
            printType("qtype", type.qtype, Details.FULL);
            return visitType(type, null);
        }

        public Void visitType(Type type, Void ignore) {
            return null;
        }
    }

    

    

    protected Attribute.Visitor attrVisitor = new AttributeVisitor();

    
    public class AttributeVisitor implements Attribute.Visitor {

        public void visitConstant(Attribute.Constant a) {
            printObject("value", a.value, Details.SUMMARY);
            visitAttribute(a);
        }

        public void visitClass(Attribute.Class a) {
            printObject("classType", a.classType, Details.SUMMARY);
            visitAttribute(a);
        }

        public void visitCompound(Attribute.Compound a) {
            if (a instanceof Attribute.TypeCompound) {
                Attribute.TypeCompound ta = (Attribute.TypeCompound) a;
                
                printObject("position", ta.position, Details.SUMMARY);
            }
            printObject("synthesized", a.isSynthesized(), Details.SUMMARY);
            printList("values", a.values);
            visitAttribute(a);
        }

        public void visitArray(Attribute.Array a) {
            printList("values", Arrays.asList(a.values));
            visitAttribute(a);
        }

        public void visitEnum(Attribute.Enum a) {
            printSymbol("value", a.value, Details.SUMMARY);
            visitAttribute(a);
        }

        public void visitError(Attribute.Error a) {
            visitAttribute(a);
        }

        public void visitAttribute(Attribute a) {
            printType("type", a.type, Details.SUMMARY);
        }

    }
    

    

    
    static class Main {
        public static void main(String... args) throws IOException {
            Main m = new Main();
            PrintWriter out = new PrintWriter(System.out);
            try {
                if (args.length == 0)
                    m.usage(out);
                else
                    m.run(out, args);
            } finally {
                out.flush();
            }
        }

        void usage(PrintWriter out) {
            out.println("Usage:");
            out.println("  java " + Main.class.getName() + " mode [options] [javac-options]");
            out.print("where mode is one of: ");
            String sep = "";
            for (Handler h: getHandlers().values()) {
                out.print(sep);
                out.print(h.name);
                sep = ", ";
            }
            out.println();
            out.println("and where options include:");
            out.println("  -before PARSE|ENTER|ANALYZE|GENERATE|ANNOTATION_PROCESSING|ANNOTATION_PROCESSING_ROUND");
            out.println("  -after PARSE|ENTER|ANALYZE|GENERATE|ANNOTATION_PROCESSING|ANNOTATION_PROCESSING_ROUND");
            out.println("  -showPositions");
            out.println("  -showSource");
            out.println("  -showTreeSymbols");
            out.println("  -showTreeTypes");
            out.println("  -hideEmptyItems");
            out.println("  -hideNulls");
        }

        void run(PrintWriter out, String... args) throws IOException {
            JavaCompiler c = ToolProvider.getSystemJavaCompiler();
            StandardJavaFileManager fm = c.getStandardFileManager(null, null, null);

            
            final Set<TaskEvent.Kind> before = EnumSet.noneOf(TaskEvent.Kind.class);
            final Set<TaskEvent.Kind> after = EnumSet.noneOf(TaskEvent.Kind.class);
            boolean showPositions = false;
            boolean showSource = false;
            boolean showTreeSymbols = false;
            boolean showTreeTypes = false;
            boolean showEmptyItems = true;
            boolean showNulls = true;

            
            Collection<String> options = new ArrayList<String>();
            Collection<File> files = new ArrayList<File>();
            String classpath = null;
            String classoutdir = null;

            final Handler h = getHandlers().get(args[0]);
            if (h == null)
                throw new IllegalArgumentException(args[0]);

            for (int i = 1; i < args.length; i++) {
                String arg = args[i];
                if (arg.equals("-before") && i + 1 < args.length) {
                    before.add(getKind(args[++i]));
                } else if (arg.equals("-after") && i + 1 < args.length) {
                    after.add(getKind(args[++i]));
                } else if (arg.equals("-showPositions")) {
                    showPositions = true;
                } else if (arg.equals("-showSource")) {
                    showSource = true;
                } else if (arg.equals("-showTreeSymbols")) {
                    showTreeSymbols = true;
                } else if (arg.equals("-showTreeTypes")) {
                    showTreeTypes = true;
                } else if (arg.equals("-hideEmptyLists")) {
                    showEmptyItems = false;
                } else if (arg.equals("-hideNulls")) {
                    showNulls = false;
                } else if (arg.equals("-classpath") && i + 1 < args.length) {
                    classpath = args[++i];
                } else if (arg.equals("-d") && i + 1 < args.length) {
                    classoutdir = args[++i];
                } else if (arg.startsWith("-")) {
                    int n = c.isSupportedOption(arg);
                    if (n < 0) throw new IllegalArgumentException(arg);
                    options.add(arg);
                    while (n > 0) options.add(args[++i]);
                } else if (arg.endsWith(".java")) {
                    files.add(new File(arg));
                }
            }

            if (classoutdir != null) {
                fm.setLocation(StandardLocation.CLASS_OUTPUT, Arrays.asList(new File(classoutdir)));
            }

            if (classpath != null) {
                Collection<File> path = new ArrayList<File>();
                for (String p: classpath.split(File.pathSeparator)) {
                    if (p.isEmpty()) continue;
                    File f = new File(p);
                    if (f.exists()) path.add(f);
                }
                fm.setLocation(StandardLocation.CLASS_PATH, path);
            }
            Iterable<? extends JavaFileObject> fos = fm.getJavaFileObjectsFromFiles(files);

            JavacTask task = (JavacTask) c.getTask(out, fm, null, options, null, fos);
            final Trees trees = Trees.instance(task);

            final DPrinter dprinter = new DPrinter(out, trees);
            dprinter.source(showSource)
                    .emptyItems(showEmptyItems)
                    .nulls(showNulls)
                    .positions(showPositions)
                    .treeSymbols(showTreeSymbols)
                    .treeTypes(showTreeTypes);

            if (before.isEmpty() && after.isEmpty()) {
                if (h.name.equals("trees") && !showTreeSymbols && !showTreeTypes)
                    after.add(TaskEvent.Kind.PARSE);
                else
                    after.add(TaskEvent.Kind.ANALYZE);
            }

            task.addTaskListener(new TaskListener() {
                public void started(TaskEvent e) {
                    if (before.contains(e.getKind()))
                        handle(e);
                }

                public void finished(TaskEvent e) {
                    if (after.contains(e.getKind()))
                        handle(e);
                }

                private void handle(TaskEvent e) {
                     switch (e.getKind()) {
                         case PARSE:
                         case ENTER:
                             h.handle(e.getSourceFile().getName(),
                                     (JCTree) e.getCompilationUnit(),
                                     dprinter);
                             break;

                         default:
                             TypeElement elem = e.getTypeElement();
                             h.handle(elem.toString(),
                                     (JCTree) trees.getTree(elem),
                                     dprinter);
                             break;
                     }
                }
            });

            task.call();
        }

        TaskEvent.Kind getKind(String s) {
            return TaskEvent.Kind.valueOf(s.toUpperCase());
        }

        static protected abstract class Handler {
            final String name;
            Handler(String name) {
                this.name = name;
            }
            abstract void handle(String label, JCTree tree, DPrinter dprinter);
        }

        Map<String,Handler> getHandlers() {
            Map<String,Handler> map = new HashMap<String, Handler>();
            for (Handler h: defaultHandlers) {
                map.put(h.name, h);
            }
            return map;
        }

        protected final Handler[] defaultHandlers = {
            new Handler("trees") {
                @Override
                void handle(String name, JCTree tree, DPrinter dprinter) {
                    dprinter.printTree(name, tree);
                    dprinter.out.println();
                }
            },

            new Handler("symbols") {
                @Override
                void handle(String name, JCTree tree, final DPrinter dprinter) {
                    TreeScanner ds = new TreeScanner() {
                        @Override
                        public void visitClassDef(JCClassDecl tree) {
                            visitDecl(tree, tree.sym);
                            super.visitClassDef(tree);
                        }

                        @Override
                        public void visitMethodDef(JCMethodDecl tree) {
                            visitDecl(tree, tree.sym);
                            super.visitMethodDef(tree);
                        }

                        @Override
                        public void visitVarDef(JCVariableDecl tree) {
                            visitDecl(tree, tree.sym);
                            super.visitVarDef(tree);
                        }

                        void visitDecl(JCTree tree, Symbol sym) {
                            dprinter.printSymbol(sym.name.toString(), sym);
                            dprinter.out.println();
                        }
                    };
                    ds.scan(tree);
                }
            },

            new Handler("types") {
                @Override
                void handle(String name, JCTree tree, final DPrinter dprinter) {
                    TreeScanner ts = new TreeScanner() {
                        @Override
                        public void scan(JCTree tree) {
                            if (tree == null) {
                                return;
                            }
                            if (tree.type != null) {
                                String label = Pretty.toSimpleString(tree);
                                dprinter.printType(label, tree.type);
                                dprinter.out.println();
                            }
                            super.scan(tree);
                        }
                    };
                    ts.scan(tree);
                }
            }
        };
    }

    

}
