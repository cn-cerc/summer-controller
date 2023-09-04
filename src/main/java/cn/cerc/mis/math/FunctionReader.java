package cn.cerc.mis.math;

import java.util.function.Consumer;

public class FunctionReader {
    private Consumer<String> onFunction;
    private Consumer<String> onText;
    private boolean letterOption;

    public FunctionReader() {
        this.letterOption = false;
    }

    public FunctionReader(boolean letterOption) {
        this.letterOption = letterOption;
    }

    public int parse(String text) {
        return parseExpression(text, this.onFunction, this.onText, this.letterOption);
    }

    public void onText(Consumer<String> onText) {
        this.onText = onText;
    }

    public void onFunction(Consumer<String> onFunction) {
        this.onFunction = onFunction;
    }

    public static int parseExpression(String value, Consumer<String> onFunction, Consumer<String> onText,
            boolean letterOption) {
        int result = 0;
        if (value == null)
            return result;
        StringBuffer text = new StringBuffer();
        StringBuffer func = new StringBuffer();
        int count = 0;
        for (var i = 0; i < value.length(); i++) {
            var ch = value.charAt(i);
            if (Character.isLetter(ch) || ch == '_') {
                if (func.length() == 0) {
                    if (text.length() > 0) {
                        if (onText != null)
                            onText.accept(text.toString());
                        text.delete(0, text.length());
                    }
                    count = 0;
                }
                func.append(ch);
            } else if (Character.isDigit(ch)) {
                if (func.length() > 0)
                    func.append(ch);
                else
                    text.append(ch);
            } else if (ch == '(') {
                count++;
                if (func.length() > 0)
                    func.append(ch);
                else
                    text.append(ch);
            } else if (ch == ')') {
                count--;
                if (func.length() > 0)
                    func.append(ch);
                else
                    text.append(ch);
                if (count == 0) {
                    if (func.length() > 0) {
                        if (onFunction != null)
                            onFunction.accept(func.toString());
                        func.delete(0, func.length());
                        result++;
                    } else if (text.length() > 0) {
                        if (onText != null)
                            onText.accept(text.toString());
                        text.delete(0, text.length());
                    }
                }
            } else if (count > 0 && func.length() > 0)
                func.append(ch);
            else {
                if (func.length() > 0) {
                    // 若单词独立化
                    if (letterOption) {
                        if (onText != null)
                            onText.accept(func.toString());
                    } else {
                        text.append(func.toString());
                    }
                    func.delete(0, func.length());
                }
                text.append(ch);
            }
        }
        //
        if (func.length() > 0) {
            text.append(func.toString());
            func.delete(0, func.length());
        }
        if (text.length() > 0) {
            if (onText != null)
                onText.accept(text.toString());
            text.delete(0, text.length());
        }
        return result;
    }

    public boolean isLetterOption() {
        return letterOption;
    }

    public void setLetterOption(boolean letterOption) {
        this.letterOption = letterOption;
    }

    public static void main(String[] args) {
        var fr = new FunctionReader();
        fr.onFunction(text -> System.out.println("function: " + text));
        fr.onText(text -> System.out.println("text: " + text));
        fr.parse("a+b");
    }

}
