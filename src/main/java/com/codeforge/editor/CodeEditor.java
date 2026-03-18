package com.codeforge.editor;

import javafx.geometry.Insets;
import javafx.scene.layout.BorderPane;

import org.fxmisc.richtext.CodeArea;
import org.fxmisc.richtext.LineNumberFactory;
import org.fxmisc.richtext.model.StyleSpans;
import org.fxmisc.richtext.model.StyleSpansBuilder;

import java.time.Duration;
import java.util.Collection;
import java.util.Collections;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CodeEditor {

    private static final String[] KEYWORDS = {
        "print", "if", "elif", "else", "while", "def", "return", "True", "False", "None"
    };

    private static final String KEYWORD_PATTERN = "\\b(" + String.join("|", KEYWORDS) + ")\\b";
    private static final String PAREN_PATTERN = "\\(|\\)";
    private static final String BRACE_PATTERN = "\\{|\\}";
    private static final String BRACKET_PATTERN = "\\[|\\]";
    private static final String SEMICOLON_PATTERN = "\\;";
    private static final String STRING_PATTERN = "\"([^\"\\\\]|\\\\.)*\"|'([^'\\\\]|\\\\.)*'";
    private static final String COMMENT_PATTERN = "#[^\\n]*";
    private static final String NUMBER_PATTERN = "\\b\\d+(\\.\\d+)?\\b";
    private static final String OPERATOR_PATTERN = "==|!=|<=|>=|=|\\+|\\-|\\*|/|%|:|,|<|>";
    private static final Pattern PATTERN = Pattern.compile(
        "(?<KEYWORD>" + KEYWORD_PATTERN + ")"
            + "|(?<PAREN>" + PAREN_PATTERN + ")"
            + "|(?<BRACE>" + BRACE_PATTERN + ")"
            + "|(?<BRACKET>" + BRACKET_PATTERN + ")"
            + "|(?<SEMICOLON>" + SEMICOLON_PATTERN + ")"
            + "|(?<STRING>" + STRING_PATTERN + ")"
            + "|(?<COMMENT>" + COMMENT_PATTERN + ")"
            + "|(?<NUMBER>" + NUMBER_PATTERN + ")"
            + "|(?<OPERATOR>" + OPERATOR_PATTERN + ")"
    );

    private final CodeArea codeArea;
    private final BorderPane root;
    private boolean dirty;

    public CodeEditor() {
        codeArea = new CodeArea();
        codeArea.getStyleClass().add("code-editor");
        codeArea.setParagraphGraphicFactory(LineNumberFactory.get(codeArea));
        codeArea.setWrapText(false);
        codeArea.setPadding(new Insets(12));

        codeArea.multiPlainChanges()
            .successionEnds(Duration.ofMillis(80))
            .subscribe(ignore -> codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText())));

        codeArea.textProperty().addListener((obs, oldText, newText) -> dirty = true);

        root = new BorderPane(codeArea);
        root.getStyleClass().add("editor-shell");
    }

    public BorderPane getView() {
        return root;
    }

    public String getCode() {
        return codeArea.getText();
    }

    public void setCode(String code) {
        codeArea.replaceText(code == null ? "" : code);
        codeArea.setStyleSpans(0, computeHighlighting(codeArea.getText()));
        dirty = false;
    }

    public boolean isDirty() {
        return dirty;
    }

    public void markSaved() {
        dirty = false;
    }

    private StyleSpans<Collection<String>> computeHighlighting(String text) {
        Matcher matcher = PATTERN.matcher(text);
        int lastMatchEnd = 0;
        StyleSpansBuilder<Collection<String>> spansBuilder = new StyleSpansBuilder<>();

        while (matcher.find()) {
            String styleClass =
                matcher.group("KEYWORD") != null ? "token-keyword" :
                matcher.group("PAREN") != null ? "token-paren" :
                matcher.group("BRACE") != null ? "token-brace" :
                matcher.group("BRACKET") != null ? "token-bracket" :
                matcher.group("SEMICOLON") != null ? "token-semicolon" :
                matcher.group("STRING") != null ? "token-string" :
                matcher.group("COMMENT") != null ? "token-comment" :
                matcher.group("NUMBER") != null ? "token-number" :
                matcher.group("OPERATOR") != null ? "token-operator" :
                null;

            if (matcher.start() > lastMatchEnd) {
                spansBuilder.add(Collections.singleton("plain-text"), matcher.start() - lastMatchEnd);
            }
            spansBuilder.add(Collections.singleton(styleClass), matcher.end() - matcher.start());
            lastMatchEnd = matcher.end();
        }

        if (text.length() > lastMatchEnd) {
            spansBuilder.add(Collections.singleton("plain-text"), text.length() - lastMatchEnd);
        }
        return spansBuilder.create();
    }
}
