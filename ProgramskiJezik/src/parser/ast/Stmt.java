package parser.ast;

import lexer.token.Token;

import java.util.List;

public abstract class Stmt {

    public interface Visitor<R>
    {
        R visitVarDecl(VarDecl s);
        R visitReturn(Return s);
        R visitAssign(Assign s);
        R visitCallStmt(CallStmt s);
        R visitBeginIf(BeginIf s);
        R visitBeginFor(BeginFor s);
        R visitBeginWhile(BeginWhile s);
        R visitBeginForClassic(BeginForClassic s);

    }

    public abstract <R> R accept(Visitor<R> v);

    public static final class VarDecl extends Stmt {
        public final List<Expr> dims;
        public final List<Token> names;
        public VarDecl(List<Expr> dims, List<Token> names) {
            this.dims = dims; this.names = names;
        }
        @Override public <R> R accept(Visitor<R> v) {
            return v.visitVarDecl(this);
        }
    }

    public static final class Return extends Stmt {
        public final Expr expr;
        public Return(Expr expr) {
            this.expr = expr;
        }
        @Override public <R> R accept(Visitor<R> v) {
            return v.visitReturn(this);
        }
    }

    public static final class Assign extends Stmt {
        public final Expr left;
        public final LValue lvalue;
        public Assign(Expr left, LValue lvalue) {
            this.left = left; this.lvalue = lvalue;
        }
        @Override public <R> R accept(Visitor<R> v) {
            return v.visitAssign(this);
        }
    }

    public static final class CallStmt extends Stmt {
        public final Expr.Call call;
        public CallStmt(Expr.Call call) {
            this.call = call;
        }
        @Override public <R> R accept(Visitor<R> v) {
            return v.visitCallStmt(this);
        }
    }

    public static final class BeginIf extends Stmt {
        public static final class Arm {
            public final Expr cond;
            public final List<Stmt> block;
            public Arm(Expr cond, List<Stmt> block) {
                this.cond = cond; this.block = block;
            }
        }
        public final Arm ifArm;
        public final List<Arm> orIfArms;
        public final List<Stmt> elseBlock;
        public BeginIf(Arm ifArm, List<Arm> orIfArms, List<Stmt> elseBlock) {
            this.ifArm = ifArm; this.orIfArms = orIfArms; this.elseBlock = elseBlock;
        }
        @Override public <R> R accept(Visitor<R> v) {
            return v.visitBeginIf(this);
        }
    }

    public static final class BeginFor extends Stmt {
        public final Token var;
        public final Expr from;
        public final Expr to;
        public final List<Stmt> body;
        public BeginFor(Token var, Expr from, Expr to, List<Stmt> body) {
            this.var = var; this.from = from; this.to = to; this.body = body;
        }
        @Override public <R> R accept(Visitor<R> v) { return v.visitBeginFor(this); }
    }

    public static final class BeginForClassic extends Stmt
    {
        public final Stmt.VarDecl init;
        public final Expr cond;
        public final Stmt.Assign step;
        public final List<Stmt> body;

        public BeginForClassic(Stmt.VarDecl init, Expr cond, Stmt.Assign step, List<Stmt> body)
        {
            this.init = init;
            this.cond = cond;
            this.step = step;
            this.body = body;
        }

        @Override
        public <R> R accept(Visitor<R> v)
        {
            return v.visitBeginForClassic(this);
        }
    }

    public static final class BeginWhile extends Stmt
    {
        public final Expr cond;
        public final List<Stmt> body;
        public BeginWhile(Expr cond, List<Stmt> body)
        {
            this.cond = cond;
            this.body = body;
        }
        @Override public <R> R accept(Visitor<R> v)
        {
            return v.visitBeginWhile(this);
        }
    }

    public static final class LValue {
        public final Token name;
        public final List<Expr> indices;
        public LValue(Token name, List<Expr> indices) {
            this.name = name; this.indices = indices;
        }
    }
}
