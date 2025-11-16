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

    //parseDECL: (type -> identif -> = )return bool
    //parseGANG gang -> parseDECL -> for({) -> ;
    //parseGENERAL: parseDECL -> parseAssign

    public Ast.Program parseProgram() {
        while (match(NEWLINE)) {}
        Ast.Program p = null;
        if(check(BEGIN) || check(FUNCTION)){
            p = parseImplicitProgram();
        }

        consume(EOF, "expected EOF");
        return p;
    }

    /*private Ast.Program parseExplicitProgram() {
        consume(BEGIN, "expected BEGIN");
        consume(PROGRAM, "expected PROGRAM");
        requireNL1();
        List<Ast.TopItem> items = parseProgramBody();
        consume(END, "expected END");
        consume(PROGRAM, "expected PROGRAM");
        while (match(NEWLINE)) {}
        return new Ast.Program(true, items);
    }*/

    // implicit_program = program_body ;
    private Ast.Program parseImplicitProgram() {
        List<Ast.TopItem> items = parseProgramBody();
        return new Ast.Program(false, items);
    }

    // program_body = [ top_item { NL1 top_item } [ NL1 ] ] ;
    private List<Ast.TopItem> parseProgramBody() {
        List<Ast.TopItem> items = new ArrayList<>();
        if (check(FUNCTION)) {
            items.add(parseTopItem());
            while (check(NEWLINE)) {    //while je nepotreban zato sto su izbaceni svi NEWLINE
                if (!startsTopItem()) break;
                items.add(parseTopItem());
            }
        }
        return items;
    }

    // top_item = top_begin | top_simple ;
    private Ast.TopItem parseTopItem() {
        if (check(FUNCTION)) {
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

    // top_begin = BEGIN ( func_tail | if_tail | for_tail ) ; FUNCKIJA NAMA NE TREBA (NA TAJ NACIN)
    private Ast.TopItem parseTopBegin() {
        //consume(BEGIN, "expected jam");

        if (match(FUNCTION)) {
            return parseFuncTailAfterFUNCTION();
        } else if (match(IF)) {
            Stmt.BeginIf s = parseIfTailAfterIF();
            return new Ast.TopStmt(s);
        } else if (match(FOR)) {
            Stmt.BeginFor s = parseForTailAfterFOR();
            return new Ast.TopStmt(s);
        } else {
            throw error(peek(), "expected FUNCTION or annieAreYouOkay or justBeatIt after jam");
        }
    }

    // func_tail = FUNCTION IDENT "(" [ params ] ")" ":" type {code body} END FUNCTION ;
    private Ast.FuncDef parseFuncTailAfterFUNCTION() {
        Ast.Type ret = parseType();
        if(ret == null) {
            ret = new Ast.Type(Ast.Type.Kind.VOID, null, 0);
        }

        Token name = consume(IDENT, "expected function name");
        consume(LPAREN, "expected '('");
        List<Ast.Param> params = new ArrayList<>();
        if (!check(RPAREN)) params = parseParams();
        consume(RPAREN, "expected ')'");
        while(match(NEWLINE)){}
        consume(LBRACE, "expected '{'");
        List<Stmt> body = parseBlock();

        consume(RBRACE, "expected '}' after function declaration");
        //consume(FUNCTION, "expected smoothCriminal");
        return new Ast.FuncDef(name, params, ret, body);
    }

    // params = param { "," param } ;
    private List<Ast.Param> parseParams() {
        List<Ast.Param> ps = new ArrayList<>();
        ps.add(parseParam());
        while (match(SEPARATOR_COMMA)) ps.add(parseParam());
        return ps;
    }

    // param = type IDENT ;
    private Ast.Param parseParam() {
        Ast.Type t = parseType();
        Token name = consume(IDENT, "expected parameter name");
        //consume(TYPE_COLON, "expected ':'");
        return new Ast.Param(name, t);
    }

    // type = INT { "[]" } ;
    //gang type IDENT = {params};
    private Ast.Type parseType() {
        Token base;
        Ast.Type type = null;
        int rank = 0;
        if(check(INT)){ base = consume(INT, "expected heehee"); type = new Ast.Type(Ast.Type.Kind.INT, base, rank);}
        else if(check(FLOAT)){ base = consume(FLOAT, "expected ow"); type = new Ast.Type(Ast.Type.Kind.FLOAT, base, rank);}
        else if(check(CHAR)){ base = consume(CHAR, "expected shooCa"); type = new Ast.Type(Ast.Type.Kind.CHAR, base, rank);}
        else if(check(STRING)){ base = consume(STRING, "expected shooCaChooCa"); type = new Ast.Type(Ast.Type.Kind.STRING, base, rank);}
        else if(check(BOOL)){ base = consume(BOOL, "expected blackOrWhite"); type = new Ast.Type(Ast.Type.Kind.BOOL, base, rank);}

        /*while (match(LBRACKET)) {
            consume(RBRACKET, "expected ']'");
            rank++;
        }*/
        return type;
    }

    // var_decl = INT ( array_dims ident_list | ident_list ) ;
    //var_decl : type IDENT = {array_elements};
    private Stmt.VarDecl parseVarDecl() {
        boolean array = false;
        if(check(ARRAY)) {advance(); array = true;}
        Ast.Type type = parseType();
        consume(IDENT, "expected identifier");
        consume(EQ, "expected '=' in declaration");
        List<Expr> dims = new ArrayList<>();

        if (array && match(LBRACE)) {
            dims.add(parseExpr());
            consume(RBRACE, "expected '}' in array");
            while (match(LBRACE)) {
                dims.add(parseExpr());
                consume(RBRACE, "expected '}' in multidimensional array");
            }
        }
        else if(!array) dims.add(parseExpr());
        else consume(LPAREN, "expected '{'");

        List<Token> names = parseIdentList();
        return new Stmt.VarDecl(dims, names);
    }

    private List<Token> parseIdentList() {
        List<Token> ids = new ArrayList<>();
        ids.add(consume(IDENT, "expected identifier"));
        while (match(SEPARATOR_COMMA)) ids.add(consume(IDENT, "expected identifier"));
        return ids;
    }

    // block = { stmt ; } ;
    private List<Stmt> parseBlock() {
        List<Stmt> out = new ArrayList<>();
        while (startsStmtInBlock()) {
            out.add(parseStmt());
            consume(SEPARATOR_SEMICOLON, "expected ';");
        }
        return out;
    }

    // stmt = var_decl | return_stmt | call_and_maybe_assign | assign_stmt | begin_group ;
    private Stmt parseStmt() {
        if (check(INT) || check(FLOAT) || check(CHAR) || check(STRING) || check(BOOL) || check((ARRAY))) return parseVarDecl();
        if (check(RETURN)) return parseReturnStmt();
        if (check(CALL)) return parseCallAndMaybeAssign();
        if (check(BEGIN)) return parseBeginGroup();
        if(match(IF)) return parseIfTailAfterIF();
        if(match(FOR)) return parseForTailAfterFOR();
        return parseAssignStmt();
    }

    // begin_group = BEGIN ( if_tail | for_tail ) ;
    private Stmt parseBeginGroup() {
        //consume(BEGIN, "expected jam");
        if (match(IF)) {
            return parseIfTailAfterIF();
        } else if (match(FOR)) {
            return parseForTailAfterFOR();
        } else {
            throw error(peek(), "expected annieAreYouOkay or justBeatIt after jam");
        }
    }

    // if_tail = IF "(" cond ")" NL1 block { OR IF "(" cond ")" NL1 block } [ ELSE NL1 block ] END IF ;
    private Stmt.BeginIf parseIfTailAfterIF() {
        consume(LPAREN, "expected '('");
        Expr cond = parseCond();
        consume(RPAREN, "expected ')'");
        consume(LBRACE, "expected '{' at start of if");
        List<Stmt> first = parseBlock();
        Stmt.BeginIf.Arm ifArm = new Stmt.BeginIf.Arm(cond, first);

        List<Stmt.BeginIf.Arm> orArms = new ArrayList<>();
        while (match(OR)) {
            consume(IF, "expected annieAreYouOkay");
            consume(LPAREN, "expected '(' after if");
            Expr c = parseCond();
            consume(RPAREN, "expected ')' after if");

            List<Stmt> b = parseBlock();
            orArms.add(new Stmt.BeginIf.Arm(c, b));
        }
        consume(RBRACE, "expected '}' after if");
        List<Stmt> elseBlock = null;
        if (match(ELSE)) {
            consume(LBRACE, "expected '{' at start of else");
            elseBlock = parseBlock();
            consume(RBRACE, "expected '}' after else");
        }


        consume(IF, "expected annieAreYouOkay");
        return new Stmt.BeginIf(ifArm, orArms, elseBlock);
    }

    // for_tail = FOR "(" IDENT GOES FROM aexpr TO aexpr ")" NL1 block END FOR ;
    private Stmt.BeginFor parseForTailAfterFOR() {
        consume(LPAREN, "expected '('");
        Token var = consume(IDENT, "expected loop variable");
        consume(GOES, "expected moves");
        consume(FROM, "expected from");
        Expr from = parseAExpr();
        consume(TO, "expected glideTo");
        Expr to = parseAExpr();
        consume(RPAREN, "expected ')'");
        consume(LBRACE, "expected '{");
        List<Stmt> body = parseBlock();
        consume(RBRACE, "expected }");
        consume(FOR, "expected justBeatIt");
        return new Stmt.BeginFor(var, from, to, body);
    }

    private Stmt parseReturnStmt() {
        consume(RETURN, "expected moonWalk");
        Expr e = parseExpr();
        return new Stmt.Return(e);
    }

    // call_expr = CALL IDENT "(" [ args ] ")" ;
    private Expr.Call parseCallExpr() {
        Token callTok = consume(CALL, "expected shamona");
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
    // type IDENT = expr_no_call;
    private Stmt parseAssignStmt() {

        Expr left = parseExprNoCall();
        consume(ASSIGN, "expected '='");
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
        if (match(ADD, SUBTRACT, MULTIPLY, DIVIDE, PERCENT /* swap to PERCENT if needed */)) {
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
        if (match(ADD, SUBTRACT, MULTIPLY, DIVIDE, PERCENT)) {
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


    private boolean startsTopItem() {
        return check(BEGIN) || check(INT) || check(CALL) || check(INT_LIT) || check(IDENT) || check(LPAREN) || check(FUNCTION);
    }

    //ne moze funkcija u funkciji (za sada)
    private boolean startsStmtInBlock() {
        return check(IF) || check(ELSE) || check(FOR) || check(BEGIN) || check(INT) || check(CALL) || check(INT_LIT) || check(IDENT) || check(LPAREN) || check(RETURN);
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
        if(peek().type == NEWLINE) advance();
        if (isAtEnd()) return type == EOF;
        return peek().type == type;
    }

    private boolean checkNext(TokenType type) {
        if (current + 1 >= tokens.size()) return false;
        return tokens.get(current + 1).type == type;
    }

    private Token advance() {
        if (!isAtEnd()){
            while(peek().type == EOF) current++;
            current++;
        }
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