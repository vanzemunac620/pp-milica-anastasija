package parser.ast;

import lexer.token.Token;
import lexer.token.TokenType;

import java.util.ArrayList;
import java.util.List;

import static lexer.token.TokenType.*;

public final class ParserAst {
    private final List<Token> tokens;
    private int current = 0;

    public ParserAst(List<Token> tokens) { this.tokens = tokens; }

    // program = { NEWLINE } ( explicit_program | implicit_program ) EOF ;
    public Ast.Program parseProgram() {
        while (match(NEWLINE)) {}

        Ast.Program p;
        if (check(BEGIN) && checkNext(PROGRAM)) {
            p = parseExplicitProgram();
        } else {
            p = parseImplicitProgram();
        }

        consume(EOF, "expected EOF");
        return p;
    }

    // explicit_program = BEGIN PROGRAM NL1 program_body END PROGRAM { NEWLINE } ;
    private Ast.Program parseExplicitProgram() {
        consume(BEGIN, "expected BEGIN");
        consume(PROGRAM, "expected PROGRAM");
        requireNL1();
        List<Ast.TopItem> items = parseProgramBody();
        consume(END, "expected END");
        consume(PROGRAM, "expected PROGRAM");
        while (match(NEWLINE)) {}
        return new Ast.Program(true, items);
    }

    // implicit_program = program_body ;
    private Ast.Program parseImplicitProgram() {
        List<Ast.TopItem> items = parseProgramBody();
        return new Ast.Program(false, items);
    }

    // program_body = [ top_item { NL1 top_item } [ NL1 ] ] ;
    private List<Ast.TopItem> parseProgramBody() {
        List<Ast.TopItem> items = new ArrayList<>();
        if (startsTopItem()) {
            items.add(parseTopItem());
            while (check(NEWLINE)) {
                requireNL1();
                if (!startsTopItem()) break;
                items.add(parseTopItem());
            }
            if (check(NEWLINE)) requireNL1();
        }
        return items;
    }

    // top_item = top_begin | top_simple ;
    private Ast.TopItem parseTopItem() {
        if (check(BEGIN)) {
            return parseTopBegin();
        } else {
            return parseTopSimple();
        }
    }

    // top_simple = var_decl | call_and_maybe_assign | assign_stmt ;
    private Ast.TopItem parseTopSimple() {
        if (check(INT)) {
            return new Ast.TopVarDecl(parseVarDecl());
        } else if (check(CALL)) {
            Stmt s = parseCallAndMaybeAssign();
            return new Ast.TopStmt(s);
        } else {
            Stmt s = parseAssignStmt();
            return new Ast.TopStmt(s);
        }
    }

    // top_begin = BEGIN ( func_tail | if_tail | for_tail ) ;
    private Ast.TopItem parseTopBegin() {
        consume(BEGIN, "expected BEGIN");

        if (match(FUNCTION)) {
            Ast.FuncDef f = parseFuncTailAfterFUNCTION();
            return f;
        } else if (match(IF)) {
            Stmt.BeginIf s = parseIfTailAfterIF();
            return new Ast.TopStmt(s);
        } else if (match(FOR)) {
            Stmt.BeginFor s = parseForTailAfterFOR();
            return new Ast.TopStmt(s);
        } else {
            throw error(peek(), "expected FUNCTION or IF or FOR after BEGIN");
        }
    }

    // func_tail = FUNCTION IDENT "(" [ params ] ")" ":" type NL1 block END FUNCTION ;
    private Ast.FuncDef parseFuncTailAfterFUNCTION() {
        Token name = consume(IDENT, "expected function name");
        consume(LPAREN, "expected '('");
        List<Ast.Param> params = new ArrayList<>();
        if (!check(RPAREN)) params = parseParams();
        consume(RPAREN, "expected ')'");

        Ast.Type ret = match(TYPE_COLON) ? parseType() : new Ast.Type(Ast.Type.Kind.VOID, null, 0);
        // implicit void has no Token

        requireNL1();
        List<Stmt> body = parseBlock();
        consume(END, "expected END");
        consume(FUNCTION, "expected FUNCTION");
        return new Ast.FuncDef(name, params, ret, body);
    }

    // params = param { "," param } ;
    private List<Ast.Param> parseParams() {
        List<Ast.Param> ps = new ArrayList<>();
        ps.add(parseParam());
        while (match(SEPARATOR_COMMA)) ps.add(parseParam());
        return ps;
    }

    // param = IDENT ":" type ;
    private Ast.Param parseParam() {
        Token name = consume(IDENT, "expected parameter name");
        consume(TYPE_COLON, "expected ':'");
        Ast.Type t = parseType();
        return new Ast.Param(name, t);
    }

    // type = INT { "[]" } ;
    private Ast.Type parseType() {
        Token base = consume(INT, "expected INT");
        int rank = 0;
        while (match(LBRACKET)) {
            consume(RBRACKET, "expected ']'");
            rank++;
        }
        return new Ast.Type(Ast.Type.Kind.INT, base, rank);
    }

    // var_decl = INT ( array_dims ident_list | ident_list ) ;
    private Stmt.VarDecl parseVarDecl() {
        consume(INT, "expected INT");
        List<Expr> dims = new ArrayList<>();
        if (match(LBRACKET)) {
            dims.add(parseExpr());
            consume(RBRACKET, "expected ']'");
            while (match(LBRACKET)) {
                dims.add(parseExpr());
                consume(RBRACKET, "expected ']'");
            }
        }
        List<Token> names = parseIdentList();
        return new Stmt.VarDecl(dims, names);
    }

    private List<Token> parseIdentList() {
        List<Token> ids = new ArrayList<>();
        ids.add(consume(IDENT, "expected identifier"));
        while (match(SEPARATOR_COMMA)) ids.add(consume(IDENT, "expected identifier"));
        return ids;
    }

    // block = { stmt NL1 } ;
    private List<Stmt> parseBlock() {
        List<Stmt> out = new ArrayList<>();
        while (startsStmtInBlock()) {
            out.add(parseStmt());
            requireNL1();
        }
        return out;
    }

    // stmt = var_decl | return_stmt | call_and_maybe_assign | assign_stmt | begin_group ;
    private Stmt parseStmt() {
        if (check(INT)) return parseVarDecl();
        if (check(RETURN)) return parseReturnStmt();
        if (check(CALL)) return parseCallAndMaybeAssign();
        if (check(BEGIN)) return parseBeginGroup();
        return parseAssignStmt();
    }

    // begin_group = BEGIN ( if_tail | for_tail ) ;
    private Stmt parseBeginGroup() {
        consume(BEGIN, "expected BEGIN");
        if (match(IF)) {
            return parseIfTailAfterIF();
        } else if (match(FOR)) {
            return parseForTailAfterFOR();
        } else {
            throw error(peek(), "expected IF or FOR after BEGIN");
        }
    }

    // if_tail = IF "(" cond ")" NL1 block { OR IF "(" cond ")" NL1 block } [ ELSE NL1 block ] END IF ;
    private Stmt.BeginIf parseIfTailAfterIF() {
        consume(LPAREN, "expected '('");
        Expr cond = parseCond();
        consume(RPAREN, "expected ')'");
        requireNL1();
        List<Stmt> first = parseBlock();
        Stmt.BeginIf.Arm ifArm = new Stmt.BeginIf.Arm(cond, first);

        List<Stmt.BeginIf.Arm> orArms = new ArrayList<>();
        while (match(OR)) {
            consume(IF, "expected IF");
            consume(LPAREN, "expected '('");
            Expr c = parseCond();
            consume(RPAREN, "expected ')'");
            requireNL1();
            List<Stmt> b = parseBlock();
            orArms.add(new Stmt.BeginIf.Arm(c, b));
        }

        List<Stmt> elseBlock = null;
        if (match(ELSE)) {
            requireNL1();
            elseBlock = parseBlock();
        }

        consume(END, "expected END");
        consume(IF, "expected IF");
        return new Stmt.BeginIf(ifArm, orArms, elseBlock);
    }

    // for_tail = FOR "(" IDENT GOES FROM aexpr TO aexpr ")" NL1 block END FOR ;
    private Stmt.BeginFor parseForTailAfterFOR() {
        consume(LPAREN, "expected '('");
        Token var = consume(IDENT, "expected loop variable");
        consume(GOES, "expected MOVES");
        consume(FROM, "expected FROM");
        Expr from = parseAExpr();
        consume(TO, "expected TO");
        Expr to = parseAExpr();
        consume(RPAREN, "expected ')'");
        requireNL1();
        List<Stmt> body = parseBlock();
        consume(END, "expected END");
        consume(FOR, "expected FOR");
        return new Stmt.BeginFor(var, from, to, body);
    }

    private Stmt parseReturnStmt() {
        consume(RETURN, "expected moonWalk");
        Expr e = parseExpr();
        return new Stmt.Return(e);
    }

    // call_expr = CALL IDENT "(" [ args ] ")" ;
    private Expr.Call parseCallExpr() {
        Token callTok = consume(CALL, "expected CALL");
        Token name = consume(IDENT, "expected function name");
        consume(LPAREN, "expected '('");
        List<Expr> args = new ArrayList<>();
        if (!check(RPAREN)) {
            args.add(parseExpr());
            while (match(SEPARATOR_COMMA)) args.add(parseExpr());
        }
        consume(RPAREN, "expected ')'");
        return new Expr.Call(callTok, name, args);
    }

    // call_and_maybe_assign = call_expr [ ASSIGN lvalue ] ;
    private Stmt parseCallAndMaybeAssign() {
        Expr.Call call = parseCallExpr();
        if (match(ASSIGN)) {
            Stmt.LValue lv = parseLValue();
            return new Stmt.Assign(call, lv);
        }
        return new Stmt.CallStmt(call);
    }

    // assign_stmt = expr_no_call ASSIGN lvalue ;
    private Stmt parseAssignStmt() {
        Expr left = parseExprNoCall();
        consume(ASSIGN, "expected '->'");
        Stmt.LValue lv = parseLValue();
        return new Stmt.Assign(left, lv);
    }

    // lvalue = IDENT { "[" expr "]" } ;
    private Stmt.LValue parseLValue() {
        Token id = consume(IDENT, "expected identifier");
        List<Expr> idx = new ArrayList<>();
        while (match(LBRACKET)) {
            idx.add(parseExpr());
            consume(RBRACKET, "expected ']'");
        }
        return new Stmt.LValue(id, idx);
    }

    // ===== expressions =====
    // expr = aexpr ;
    private Expr parseExpr() { return parseAExpr(); }

    // aexpr = call_expr | ( atom [ arith_op atom ] ) ;
    private Expr parseAExpr() {
        if (check(CALL)) return parseCallExpr();
        Expr left = parseAtom();
        if (match(ADD, SUBTRACT, MULTIPLY, DIVIDE, REMINDER /* swap to PERCENT if needed */)) {
            Token op = previous();
            Expr right = parseAtom();
            return new Expr.Binary(left, op, right);
        }
        return left;
    }

    // expr_no_call = aexpr_no_call ;  aexpr_no_call = atom [ arith_op atom ] ;
    private Expr parseExprNoCall() { return parseAExprNoCall(); }

    private Expr parseAExprNoCall() {
        Expr left = parseAtom();
        if (match(ADD, SUBTRACT, MULTIPLY, DIVIDE, REMINDER /* or PERCENT */)) {
            Token op = previous();
            Expr right = parseAtom();
            return new Expr.Binary(left, op, right);
        }
        return left;
    }

    // atom = INT_LIT | IDENT { "[" expr "]" } | "(" expr ")"
    private Expr parseAtom() {
        if (match(INT_LIT)) {
            Token t = previous();
            int val = t.literal;
            return new Expr.Literal(t, val);
        }
        if (match(IDENT)) {
            Token id = previous();
            if (match(LBRACKET)) {
                List<Expr> idx = new ArrayList<>();
                idx.add(parseExpr());
                consume(RBRACKET, "expected ']'");
                while (match(LBRACKET)) {
                    idx.add(parseExpr());
                    consume(RBRACKET, "expected ']'");
                }
                return new Expr.Index(id, idx);
            }
            return new Expr.Ident(id);
        }
        if (match(LPAREN)) {
            Expr inner = parseExpr();
            consume(RPAREN, "expected ')'");
            return new Expr.Grouping(inner);
        }
        throw error(peek(), "expected expression");
    }

    // cond = aexpr rel_op aexpr ;
    private Expr parseCond() {
        Expr left = parseAExpr();
        Token op = consumeOneOf("expected relational operator",
                LT, LE, GT, GE, EQ, NEQ);
        Expr right = parseAExpr();
        return new Expr.Binary(left, op, right);
    }

    // ===== utilities =====

    // NL1 = NEWLINE { NEWLINE }
    private void requireNL1() {
        consume(NEWLINE, "expected newline");
        while (match(NEWLINE)) {}
    }

    private boolean startsTopItem() {
        return check(BEGIN) || check(INT) || check(CALL) || check(INT_LIT) || check(IDENT) || check(LPAREN);
    }

    private boolean startsStmtInBlock() {
        return check(INT) || check(RETURN) || check(CALL) || check(BEGIN) || check(INT_LIT) || check(IDENT) || check(LPAREN);
    }

    private boolean match(TokenType... types) {
        for (TokenType t : types) {
            if (check(t)) { advance(); return true; }
        }
        return false;
    }

    private Token consume(TokenType type, String message) {
        if (check(type)) return advance();
        throw error(peek(), message);
    }

    private Token consumeOneOf(String message, TokenType... types) {
        for (TokenType t : types) if (check(t)) return advance();
        throw error(peek(), message);
    }

    private boolean check(TokenType type) {
        if (isAtEnd()) return type == EOF;
        return peek().type == type;
    }

    private boolean checkNext(TokenType type) {
        if (current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).type == type;
    }

    private Token advance() {
        if (!isAtEnd()) current++;
        return previous();
    }

    private boolean isAtEnd() { return peek().type == EOF; }

    private Token peek() { return tokens.get(current); }

    private Token previous() { return tokens.get(current - 1); }

    private ParseError error(Token token, String message) {
        String where = token.type == EOF ? " at end" : " at '" + token.lexeme + "'";
        return new ParseError("Parse error" + where + ": " + message +
                " (line: " + token.line + ", col: " + token.colStart + ")");
    }

    private static final class ParseError extends RuntimeException {
        ParseError(String s) { super(s); }
        ParseError() { super("parser error"); }
    }
}
