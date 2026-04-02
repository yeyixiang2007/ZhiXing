package com.zhixing.navigation.infrastructure.persistence;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class SimpleJson {
    private SimpleJson() {
    }

    static List<Map<String, Object>> parseArrayOfObjects(String json) {
        Object root = new Parser(json).parseValue();
        if (!(root instanceof List)) {
            throw new IllegalArgumentException("JSON root must be an array");
        }
        List<?> rawList = (List<?>) root;
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        for (Object item : rawList) {
            if (!(item instanceof Map)) {
                throw new IllegalArgumentException("Array item must be an object");
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> obj = (Map<String, Object>) item;
            result.add(obj);
        }
        return result;
    }

    static String toJsonArray(List<Map<String, Object>> objects) {
        StringBuilder builder = new StringBuilder();
        builder.append("[\n");
        for (int i = 0; i < objects.size(); i++) {
            builder.append("  ");
            appendObject(builder, objects.get(i));
            if (i < objects.size() - 1) {
                builder.append(",");
            }
            builder.append("\n");
        }
        builder.append("]\n");
        return builder.toString();
    }

    private static void appendObject(StringBuilder builder, Map<String, Object> object) {
        builder.append("{");
        int index = 0;
        for (Map.Entry<String, Object> entry : object.entrySet()) {
            if (index > 0) {
                builder.append(", ");
            }
            builder.append("\"").append(escape(entry.getKey())).append("\": ");
            appendValue(builder, entry.getValue());
            index++;
        }
        builder.append("}");
    }

    @SuppressWarnings("unchecked")
    private static void appendValue(StringBuilder builder, Object value) {
        if (value == null) {
            builder.append("null");
            return;
        }
        if (value instanceof String) {
            builder.append("\"").append(escape((String) value)).append("\"");
            return;
        }
        if (value instanceof Number || value instanceof Boolean) {
            builder.append(value.toString());
            return;
        }
        if (value instanceof Map) {
            appendObject(builder, (Map<String, Object>) value);
            return;
        }
        if (value instanceof List) {
            List<?> list = (List<?>) value;
            builder.append("[");
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                appendValue(builder, list.get(i));
            }
            builder.append("]");
            return;
        }
        throw new IllegalArgumentException("Unsupported JSON value type: " + value.getClass().getName());
    }

    private static String escape(String value) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            switch (ch) {
                case '\\':
                    builder.append("\\\\");
                    break;
                case '"':
                    builder.append("\\\"");
                    break;
                case '\n':
                    builder.append("\\n");
                    break;
                case '\r':
                    builder.append("\\r");
                    break;
                case '\t':
                    builder.append("\\t");
                    break;
                default:
                    builder.append(ch);
            }
        }
        return builder.toString();
    }

    private static final class Parser {
        private final String source;
        private int pos;

        private Parser(String source) {
            this.source = source == null ? "" : source;
            this.pos = 0;
        }

        private Object parseValue() {
            skipWhitespace();
            if (isEnd()) {
                throw new IllegalArgumentException("Unexpected end of JSON");
            }
            char ch = peek();
            if (ch == '{') {
                return parseObject();
            }
            if (ch == '[') {
                return parseArray();
            }
            if (ch == '"') {
                return parseString();
            }
            if (ch == 't' || ch == 'f') {
                return parseBoolean();
            }
            if (ch == 'n') {
                parseNull();
                return null;
            }
            if (ch == '-' || isDigit(ch)) {
                return parseNumber();
            }
            throw new IllegalArgumentException("Unexpected token at position " + pos + ": " + ch);
        }

        private Map<String, Object> parseObject() {
            expect('{');
            skipWhitespace();
            Map<String, Object> object = new LinkedHashMap<String, Object>();
            if (peekIf('}')) {
                pos++;
                return object;
            }
            while (true) {
                skipWhitespace();
                String key = parseString();
                skipWhitespace();
                expect(':');
                Object value = parseValue();
                object.put(key, value);
                skipWhitespace();
                if (peekIf('}')) {
                    pos++;
                    break;
                }
                expect(',');
            }
            return object;
        }

        private List<Object> parseArray() {
            expect('[');
            skipWhitespace();
            List<Object> list = new ArrayList<Object>();
            if (peekIf(']')) {
                pos++;
                return list;
            }
            while (true) {
                Object value = parseValue();
                list.add(value);
                skipWhitespace();
                if (peekIf(']')) {
                    pos++;
                    break;
                }
                expect(',');
            }
            return list;
        }

        private String parseString() {
            expect('"');
            StringBuilder builder = new StringBuilder();
            while (!isEnd()) {
                char ch = source.charAt(pos++);
                if (ch == '"') {
                    return builder.toString();
                }
                if (ch == '\\') {
                    if (isEnd()) {
                        throw new IllegalArgumentException("Invalid escape at end of JSON");
                    }
                    char esc = source.charAt(pos++);
                    switch (esc) {
                        case '"':
                            builder.append('"');
                            break;
                        case '\\':
                            builder.append('\\');
                            break;
                        case '/':
                            builder.append('/');
                            break;
                        case 'b':
                            builder.append('\b');
                            break;
                        case 'f':
                            builder.append('\f');
                            break;
                        case 'n':
                            builder.append('\n');
                            break;
                        case 'r':
                            builder.append('\r');
                            break;
                        case 't':
                            builder.append('\t');
                            break;
                        case 'u':
                            builder.append(parseUnicodeEscape());
                            break;
                        default:
                            throw new IllegalArgumentException("Invalid escape sequence: \\" + esc);
                    }
                } else {
                    builder.append(ch);
                }
            }
            throw new IllegalArgumentException("Unterminated string");
        }

        private char parseUnicodeEscape() {
            if (pos + 4 > source.length()) {
                throw new IllegalArgumentException("Invalid unicode escape");
            }
            String hex = source.substring(pos, pos + 4);
            pos += 4;
            try {
                return (char) Integer.parseInt(hex, 16);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid unicode escape: " + hex);
            }
        }

        private Boolean parseBoolean() {
            if (source.startsWith("true", pos)) {
                pos += 4;
                return Boolean.TRUE;
            }
            if (source.startsWith("false", pos)) {
                pos += 5;
                return Boolean.FALSE;
            }
            throw new IllegalArgumentException("Invalid boolean value at position " + pos);
        }

        private void parseNull() {
            if (!source.startsWith("null", pos)) {
                throw new IllegalArgumentException("Invalid null value at position " + pos);
            }
            pos += 4;
        }

        private Double parseNumber() {
            int start = pos;
            if (peekIf('-')) {
                pos++;
            }
            parseDigits();
            if (peekIf('.')) {
                pos++;
                parseDigits();
            }
            if (peekIf('e') || peekIf('E')) {
                pos++;
                if (peekIf('+') || peekIf('-')) {
                    pos++;
                }
                parseDigits();
            }
            String token = source.substring(start, pos);
            try {
                return Double.valueOf(token);
            } catch (NumberFormatException ex) {
                throw new IllegalArgumentException("Invalid number: " + token);
            }
        }

        private void parseDigits() {
            if (isEnd() || !isDigit(peek())) {
                throw new IllegalArgumentException("Expected digits at position " + pos);
            }
            while (!isEnd() && isDigit(peek())) {
                pos++;
            }
        }

        private void expect(char expected) {
            skipWhitespace();
            if (isEnd() || source.charAt(pos) != expected) {
                throw new IllegalArgumentException("Expected '" + expected + "' at position " + pos);
            }
            pos++;
        }

        private boolean peekIf(char expected) {
            return !isEnd() && source.charAt(pos) == expected;
        }

        private char peek() {
            return source.charAt(pos);
        }

        private void skipWhitespace() {
            while (!isEnd()) {
                char ch = source.charAt(pos);
                if (ch == ' ' || ch == '\n' || ch == '\r' || ch == '\t') {
                    pos++;
                } else {
                    break;
                }
            }
        }

        private boolean isEnd() {
            return pos >= source.length();
        }

        private static boolean isDigit(char ch) {
            return ch >= '0' && ch <= '9';
        }
    }
}

