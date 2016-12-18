//> Parsing Expressions 99
package com.craftinginterpreters.lox;

//> Statements and State 99
import java.util.ArrayList;
import java.util.Arrays;
//< Statements and State 99
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static com.craftinginterpreters.lox.TokenType.*;

class Parser {
  private static final Set<TokenType> synchronizing = new HashSet<>();

  static {
//> Statements and State 99
    synchronizing.add(LEFT_BRACE);
    synchronizing.add(RIGHT_BRACE);
//< Statements and State 99
    synchronizing.add(RIGHT_PAREN);
//> Statements and State 99
    synchronizing.add(EQUAL);
    synchronizing.add(SEMICOLON);
//< Statements and State 99
  }

  private final List<Token> tokens;
  private int currentIndex = 0;
  private final ErrorReporter errorReporter;

  Parser(List<Token> tokens, ErrorReporter errorReporter) {
    this.tokens = tokens;
    this.errorReporter = errorReporter;
  }
//> Statements and State 99

  List<Stmt> parseProgram() {
    List<Stmt> statements = new ArrayList<>();
    while (!isAtEnd()) {
      statements.add(declaration());
    }

    return statements;
  }
//< Statements and State 99

  Expr parseExpression() {
/* Parsing Expressions 99 < Statements and State 99
    return equality();
*/
//> Statements and State 99
    return assignment();
//< Statements and State 99
  }
//> Statements and State 99

  private Stmt declaration() {
//> Classes 99
    if (match(CLASS)) return classDeclaration();
//< Classes 99
//> Functions 99
    if (match(FUN)) return function("function");
//< Functions 99
    if (match(VAR)) return varDeclaration();

    return statement();
  }
//> Classes 99

  private Stmt classDeclaration() {
    Token name = consume(IDENTIFIER, "Expect class name.");
//> Inheritance 99

    Expr superclass = null;
    if (match(LESS)) {
      consume(IDENTIFIER, "Expect superclass name.");
      superclass = new Expr.Variable(previous());
    }
//< Inheritance 99

    List<Stmt.Function> methods = new ArrayList<>();
    consume(LEFT_BRACE, "Expect '{' before class body.");

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      methods.add(function("method"));
    }

    consume(RIGHT_BRACE, "Expect '}' after class body.");

/* Classes 99 < Inheritance 99
    return new Stmt.Class(name, methods);
*/
//> Inheritance 99
    return new Stmt.Class(name, superclass, methods);
//< Inheritance 99
  }
//< Classes 99

  private Stmt statement() {
//> Control Flow 99
    if (match(FOR)) return forStatement();
    if (match(IF)) return ifStatement();
//< Control Flow 99
    if (match(PRINT)) return printStatement();
//> Functions 99
    if (match(RETURN)) return returnStatement();
//< Functions 99
//> Control Flow 99
    if (match(WHILE)) return whileStatement();
//< Control Flow 99
    if (check(LEFT_BRACE)) return new Stmt.Block(block());

    return expressionStatement();
  }
//> Control Flow 99

  private Stmt forStatement() {
    // Parse it.
    consume(LEFT_PAREN, "Expect '(' after 'for'.");

    Stmt initializer;
    if (match(SEMICOLON)) {
      initializer = null;
    } else if (match(VAR)) {
      initializer = varDeclaration();
    } else {
      initializer = expressionStatement();
    }

    Expr condition = null;
    if (!check(SEMICOLON)) {
      condition = parseExpression();
    }
    consume(SEMICOLON, "Expect ';' after loop condition.");

    Stmt increment = null;
    if (!check(RIGHT_PAREN)) {
      increment = new Stmt.Expression(parseExpression());
    }
    consume(RIGHT_PAREN, "Expect ')' after for clauses.");

    Stmt body = statement();

    // Desugar to a while loop.
    if (increment != null) {
      body = new Stmt.Block(Arrays.asList(body, increment));
    }

    if (condition == null) condition = new Expr.Literal(true);
    body = new Stmt.While(condition, body);

    if (initializer != null) {
      body = new Stmt.Block(Arrays.asList(initializer, body));
    }

    return body;
  }

  private Stmt ifStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'if'.");
    Expr condition = parseExpression();
    consume(RIGHT_PAREN, "Expect ')' after if condition.");

    Stmt thenBranch = statement();
    Stmt elseBranch = null;
    if (match(ELSE)) {
      elseBranch = statement();
    }

    return new Stmt.If(condition, thenBranch, elseBranch);
  }
//< Control Flow 99

  private Stmt printStatement() {
    Expr value = parseExpression();
    consume(SEMICOLON, "Expect ';' after value.");
    return new Stmt.Print(value);
  }
//> Functions 99

  private Stmt returnStatement() {
    Token keyword = previous();
    Expr value = null;
    if (!check(SEMICOLON)) {
      value = parseExpression();
    }

    consume(SEMICOLON, "Expect ';' after return value.");
    return new Stmt.Return(keyword, value);
  }
//< Functions 99

  private Stmt varDeclaration() {
    Token name = consume(IDENTIFIER, "Expect variable name.");

    Expr initializer = null;
    if (match(EQUAL)) {
      initializer = parseExpression();
    }

    consume(SEMICOLON, "Expect ';' after variable declaration.");

    return new Stmt.Var(name, initializer);
  }
//> Control Flow 99

  private Stmt whileStatement() {
    consume(LEFT_PAREN, "Expect '(' after 'while'.");
    Expr condition = parseExpression();
    consume(RIGHT_PAREN, "Expect '(' after condition.");
    Stmt body = statement();

    return new Stmt.While(condition, body);
  }
//< Control Flow 99

  private Stmt expressionStatement() {
    Expr expr = parseExpression();
    consume(SEMICOLON, "Expect ';' after expression.");
    return new Stmt.Expression(expr);
  }
//> Functions 99

  private Stmt.Function function(String kind) {
    Token name = consume(IDENTIFIER, "Expect " + kind + " name.");
    consume(LEFT_PAREN, "Expect '(' after " + kind + " name.");
    List<Token> parameters = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (parameters.size() >= 8) {
          error("Cannot have more than 8 parameters.");
        }

        parameters.add(consume(IDENTIFIER, "Expect parameter name."));
      } while (match(COMMA));
    }
    consume(RIGHT_PAREN, "Expect ')' after parameters.");

    List<Stmt> body = block();
    return new Stmt.Function(name, parameters, body);
  }
//< Functions 99

  private List<Stmt> block() {
    consume(LEFT_BRACE, "Expect '{' before block.");
    List<Stmt> statements = new ArrayList<>();

    while (!check(RIGHT_BRACE) && !isAtEnd()) {
      statements.add(declaration());
    }

    consume(RIGHT_BRACE, "Expect '}' after block.");

    return statements;
  }

  private Expr assignment() {
/* Statements and State 99 < Control Flow 99
    Expr expr = equality();
*/
//> Control Flow 99
    Expr expr = or();
//< Control Flow 99

    if (match(EQUAL)) {
      Token equals = previous();
      Expr value = assignment();

      if (expr instanceof Expr.Variable) {
        Token name = ((Expr.Variable)expr).name;
        return new Expr.Assign(name, value);
//> Classes 99
      } else if (expr instanceof Expr.Get) {
        Expr.Get get = (Expr.Get)expr;
        return new Expr.Set(get.object, get.name, value);
//< Classes 99
      }

      error("Invalid assignment target.", equals);
    }

    return expr;
  }
//< Statements and State 99
//> Control Flow 99

  private Expr or() {
    Expr expr = and();

    while (match(OR)) {
      Token operator = previous();
      Expr right = and();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }

  private Expr and() {
    Expr expr = equality();

    while (match(AND)) {
      Token operator = previous();
      Expr right = equality();
      expr = new Expr.Logical(expr, operator, right);
    }

    return expr;
  }
//< Control Flow 99

  private Expr equality() {
    Expr expr = comparison();

    while (match(BANG_EQUAL, EQUAL_EQUAL)) {
      Token operator = previous();
      Expr right = comparison();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr comparison() {
    Expr expr = term();

    while (match(GREATER, GREATER_EQUAL, LESS, LESS_EQUAL)) {
      Token operator = previous();
      Expr right = term();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr term() {
    Expr expr = factor();

    while (match(MINUS, PLUS)) {
      Token operator = previous();
      Expr right = factor();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr factor() {
    Expr expr = unary();

    while (match(SLASH, STAR)) {
      Token operator = previous();
      Expr right = unary();
      expr = new Expr.Binary(expr, operator, right);
    }

    return expr;
  }

  private Expr unary() {
    if (match(BANG, MINUS)) {
      Token operator = previous();
      Expr right = unary();
      return new Expr.Unary(operator, right);
    }

/* Parsing Expressions 99 < Functions 99
    return primary();
*/
//> Functions 99
    return call();
//< Functions 99
  }
//> Functions 99

  private Expr finishCall(Expr callee) {
    List<Expr> arguments = new ArrayList<>();
    if (!check(RIGHT_PAREN)) {
      do {
        if (arguments.size() >= 8) {
          error("Cannot have more than 8 arguments.");
        }

        arguments.add(parseExpression());
      } while (match(COMMA));
    }

    Token paren = consume(RIGHT_PAREN,
        "Expect ')' after arguments.");

    return new Expr.Call(callee, paren, arguments);
  }

  private Expr call() {
    Expr expr = primary();

    while (true) {
      if (match(LEFT_PAREN)) {
        expr = finishCall(expr);
//> Classes 99
      } else if (match(DOT)) {
        Token name = consume(IDENTIFIER,
            "Expect property name after '.'.");
        expr = new Expr.Get(expr, name);
//< Classes 99
      } else {
        break;
      }
    }

    return expr;
  }
//< Functions 99

  private Expr primary() {
    if (match(FALSE)) return new Expr.Literal(false);
    if (match(TRUE)) return new Expr.Literal(true);
    if (match(NIL)) return new Expr.Literal(null);

    if (match(NUMBER, STRING)) {
      return new Expr.Literal(previous().value);
    }
//> Inheritance 99

    if (match(SUPER)) {
      Token keyword = previous();
      consume(DOT, "Expect '.' after 'super'.");
      Token method = consume(IDENTIFIER,
          "Expect superclass method name.");
      return new Expr.Super(keyword, method);
    }
//< Inheritance 99
//> Classes 99

    if (match(THIS)) return new Expr.This(previous());
//< Classes 99
//> Statements and State 99

    if (match(IDENTIFIER)) {
      return new Expr.Variable(previous());
    }
//< Statements and State 99

    if (match(LEFT_PAREN)) {
      Expr expr = parseExpression();
      consume(RIGHT_PAREN, "Expect ')' after expression.");
      return new Expr.Grouping(expr);
    }

    error("Expect expression.");

    // Discard the token so we can make progress.
    advance();
    return null;
  }

  private boolean match(TokenType... types) {
    for (TokenType type : types) {
      if (check(type)) {
        advance();
        return true;
      }
    }

    return false;
  }

  private Token consume(TokenType type, String message) {
    if (check(type)) return advance();
    error(message);

    if (!synchronizing.contains(type)) return null;

    while (!check(type) && !isAtEnd()) {
      advance();
    }

    return advance();
  }

  private Token advance() {
    if (!isAtEnd()) currentIndex++;
    return previous();
  }

  // Returns true if the current token is of tokenType, but
  // does not consume it.
  private boolean check(TokenType tokenType) {
    if (isAtEnd()) return false;
    return current().type == tokenType;
  }

  private boolean isAtEnd() {
    return current().type == EOF;
  }

  private Token current() {
    return tokens.get(currentIndex);
  }

  private Token previous() {
    return tokens.get(currentIndex - 1);
  }

  private void error(String message) {
    error(message, current());
  }

  private void error(String message, Token token) {
    errorReporter.error(token, message);
  }
}
