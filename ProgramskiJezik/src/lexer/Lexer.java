package lexer;

import lexer.token.Token;
import lexer.token.TokenType;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class Lexer
{
    private final ScannerCore sc;
    private final String source;
    private final List<Token> tokens = new ArrayList<>();

    public Lexer(ScannerCore sc, String source)
    {
        this.sc = sc;
        this.source = source;
    }

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
            Map.entry("heehee", TokenType.INT),
            Map.entry("ow", TokenType.FLOAT),
            Map.entry("blackOrWhite", TokenType.BOOL),
            Map.entry("shooCaChooCa", TokenType.STRING),
            Map.entry("shooCa", TokenType.CHAR),
            Map.entry("jam", TokenType.BEGIN),
            Map.entry("jamOut", TokenType.END),
            Map.entry("smoothCriminal", TokenType.FUNCTION),
            Map.entry("annieAreYouOkay", TokenType.IF),
            Map.entry("eitherWay", TokenType.OR),
            Map.entry("fightForYourLife", TokenType.ELSE),
            Map.entry("justBeatIt", TokenType.FOR),
            Map.entry("stayGroovy", TokenType.WHILE),
            Map.entry("moves", TokenType.GOES),
            Map.entry("from", TokenType.FROM),
            Map.entry("glideTo", TokenType.TO),
            Map.entry("shamona", TokenType.CALL),
            Map.entry("moonWalk", TokenType.RETURN),
            Map.entry("and", TokenType.AND),
            Map.entry("lieBecomesTheTruth", TokenType.NOT),
            Map.entry("array", TokenType.ARRAY),
            Map.entry("freeze", TokenType.BREAK),
            Map.entry("white", TokenType.TRUE),
            Map.entry("black", TokenType.FALSE)
    );

/*    private static final Map<TokenType, String> TOKEN_PATTERNS = Map.ofEntries(
            Map.entry(TokenType.INT_LIT, "^[0-9]+"),
            Map.entry(TokenType.IDENT, "^[a-zA-Z_][a-zA-Z0-9_]*"),
            Map.entry(TokenType.ADD, "^\\+"),
            Map.entry(TokenType.SUBTRACT, "^-"),
            Map.entry(TokenType.MULTIPLY, "^\\*"),
            Map.entry(TokenType.DIVIDE, "^/"),
            Map.entry(TokenType.LPAREN, "^\\("),
            Map.entry(TokenType.RPAREN, "^\\)"),
            Map.entry(TokenType.LBRACKET, "^\\["),
            Map.entry(TokenType.RBRACKET, "^\\]"),
            Map.entry(TokenType.TYPE_COLON, "^:"),
            Map.entry(TokenType.SEPARATOR_COMMA, "^,"),
            Map.entry(TokenType.SEPARATOR_SEMICOLON, "^;"),
            Map.entry(TokenType.EQ, "^="),
            Map.entry(TokenType.NEQ, "^!="),
            Map.entry(TokenType.LT, "^<"),
            Map.entry(TokenType.LE, "^<="),
            Map.entry(TokenType.GT, "^>"),
            Map.entry(TokenType.GE, "^>=")
    );
 */

    private void scanToken()
    {
        char c = sc.advance();

        switch (c)
        {
            case '(' -> add(TokenType.LPAREN);
            case ')' -> add(TokenType.RPAREN);
            case '[' -> add(TokenType.LBRACKET);
            case ']' -> add(TokenType.RBRACKET);
            case '{' -> add(TokenType.LBRACE);
            case '}' -> add(TokenType.RBRACE);
            case ',' -> add(TokenType.SEPARATOR_COMMA);
            case ':' -> add(TokenType.TYPE_COLON);
            case ';' -> add(TokenType.SEPARATOR_SEMICOLON);
            case '"' -> add(TokenType.QUOTE);
            case '+' -> {
                if (sc.match('+')) add(TokenType.INC);
                else add(TokenType.ADD);
            }
            case '-' -> add(TokenType.SUBTRACT);
            case '*' -> add(TokenType.MULTIPLY);
            case '/' -> add(TokenType.DIVIDE);
            case '%' -> add(TokenType.PERCENT);
            case '<' -> add(sc.match('=') ? TokenType.LE : TokenType.LT);
            case '>' -> add(sc.match('=') ? TokenType.GE : TokenType.GT);
            case '=' -> {
                if (sc.match('=')) add(TokenType.EQ);
                else add(TokenType.ASSIGN);
            }
            case '!' -> {
                if (sc.match('=')) add(TokenType.NEQ); //nejednakost
                else add(TokenType.NOT); // not
            }
            case '&' -> {
                if (sc.match('&')) add(TokenType.AND);
                else throw error("BadBeat: Lone '&' found");
            }
            case '\n' -> tokens.add(new Token(
                    TokenType.NEWLINE, "\n", null, sc.getStartLine(), sc.getStartCol(), sc.getStartCol()
            ));
            case ' ', '\r', '\t' -> {}
            default -> {
                if (Character.isDigit(c)) number();
                else if (isIdentStart(c)) identifier();
                else throw error("JamCrash: Unrecognized move");
            }
        }
    }

    public Lexer(String source)
    {
        this.source = source;
        this.sc = new ScannerCore(source);
    }

    public List<Token> scanTokens()
    {
        while (!sc.isAtEnd())
        {
            sc.beginToken();
            scanToken();
        }
        tokens.add(new Token(TokenType.EOF, "\0", null, sc.getLine(), sc.getCol(), sc.getCol()));
        tokens.removeIf(t -> t.type == TokenType.NEWLINE);
        return tokens;
    }

    private void number()
    {
        while (Character.isDigit(sc.peek())) sc.advance();
        String text = source.substring(sc.getStartIdx(), sc.getCur());
        char nextChar = sc.peek();
        if (Character.isAlphabetic(nextChar))
        {
            //throw error("Error: Character in int literal");
            throw error("BadBeat: Letter in MoonSteps literal");
        }
        addLiteralInt(text);
    }

    private void identifier()
    {
        while (isIdentPart(sc.peek())) sc.advance();
        String text = source.substring(sc.getStartIdx(), sc.getCur());
        TokenType type = KEYWORDS.getOrDefault(text, TokenType.IDENT);
        add(type, text);
    }

    private boolean isIdentStart(char c)
    {
        return Character.isLetter(c) || c == '_';
    }

    private boolean isIdentPart(char c)
    {
        return isIdentStart(c) || Character.isDigit(c);
    }

    private void add(TokenType type)
    {
        String lex = source.substring(sc.getStartIdx(), sc.getCur());
        tokens.add(new Token(type, lex, null, sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private void add(TokenType type, String text)
    {
        tokens.add(new Token(type, text, null, sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private void addLiteralInt(String literal)
    {
        tokens.add(new Token(TokenType.INT_LIT, literal, Integer.valueOf(literal), sc.getStartLine(), sc.getStartCol(), sc.getCol() - 1));
    }

    private RuntimeException error(String msg)
    {
        String near = source.substring(sc.getStartIdx(), Math.min(sc.getCur(), source.length()));
        //return new RuntimeException("LEXER > " + msg + " at " + sc.getStartLine() + ":" + sc.getStartCol() + " near '" + near + "'");

        return new RuntimeException("JAMCRASH > " + msg + " at " + sc.getStartLine() + ":" + sc.getStartCol() + " near '" + near + "'");
    }
}
