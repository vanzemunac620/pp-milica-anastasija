package application;

import lexer.Lexer;
import lexer.token.Token;
import lexer.token.TokenFormatter;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

public class Application
{

    /*
    Options (pored run i debug) -> Configuration Edit -> Working directory svoj resources folder
    Ime fajla kao arg komandne linije
     */

//    public static void main(String[] args) {
//        if (args.length != 1) {
//            System.err.println("Usage: java main.Application <source-file>");
//            System.exit(64);
//        }
//
//        try {
//            String code = Files.readString(Path.of(args[0]));
//            Lexer lexer = new Lexer(code);
//            List<Token> tokens = lexer.scanTokens();
//
//            System.out.println(TokenFormatter.formatList(tokens));
//        } catch (Exception e) {
//            System.err.println("Error: " + e.getMessage());
//            System.exit(1);
//        }
//    }

    /*
        if (args.length < 1) {
            System.err.println("Greska: Nije uneta putanja do fajla");
            System.exit(1);
        }

        String filePath = args[0];
        String sourceCode = "";

        try
        {
            sourceCode = new String(Files.readAllBytes(Paths.get(filePath)));
        }
        catch (IOException e)
        {
            System.err.println("Greska pri citanju fajla: " + e.getMessage());
            System.exit(1);
        }

        System.out.println("=== SOURCE CODE ===");
        System.out.println(sourceCode);
        System.out.println("===================\n");

        // Lexer lexer = new Lexer(sourceCode);
        // List<Token> tokens = lexer.scanTokens();
        // System.out.println(TokenFormatter.formatList(tokens));

 */


    public static void main(String[] args)
    {
        if (args.length != 1) {
            System.err.println("Usage: java application.Application <source-file>");
            System.exit(64);
        }

        String code;
        try
        {
            code = Files.readString(Path.of(args[0]));
        }
        catch (Exception e)
        {
            System.err.println("Error reading file: " + e.getMessage());
            System.exit(1);
            return;
        }

        try
        {
            Lexer lexer = new Lexer(code);
            List<Token> tokens = lexer.scanTokens();
            System.out.println(TokenFormatter.formatList(tokens));
        }
        catch (RuntimeException e)
        {
            System.err.println("Lexer error: " + e.getMessage());
            System.exit(1);
        }
    }

}
