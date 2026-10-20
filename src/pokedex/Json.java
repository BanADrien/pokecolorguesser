package pokedex;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Petit lecteur JSON maison (le JDK n'en fournit pas).
 * Objets -> Map, tableaux -> List, nombres -> Long ou Double,
 * chaînes -> String, true/false -> Boolean, null -> null.
 */
final class Json {

    private final String text;
    private int pos;

    private Json(String text) {
        this.text = text;
    }

    static Object parse(String text) {
        Json parser = new Json(text);
        Object value = parser.readValue();
        parser.skipWhitespace();
        if (parser.pos != text.length()) {
            throw parser.error("contenu inattendu après la fin du JSON");
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    static Map<String, Object> asObject(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    static List<Object> asArray(Object value) {
        return (List<Object>) value;
    }

    /** Suit un chemin de clés : get(o, "type", "name") équivaut à o.type.name en JavaScript. */
    static Object get(Object value, String... keys) {
        for (String key : keys) {
            value = asObject(value).get(key);
        }
        return value;
    }

    private Object readValue() {
        skipWhitespace();
        if (pos >= text.length()) {
            throw error("fin du texte inattendue");
        }
        return switch (text.charAt(pos)) {
            case '{' -> readObject();
            case '[' -> readArray();
            case '"' -> readString();
            case 't' -> readLiteral("true", Boolean.TRUE);
            case 'f' -> readLiteral("false", Boolean.FALSE);
            case 'n' -> readLiteral("null", null);
            default -> readNumber();
        };
    }

    private Map<String, Object> readObject() {
        Map<String, Object> object = new LinkedHashMap<>();
        expect('{');
        skipWhitespace();
        if (peek() == '}') {
            pos++;
            return object;
        }
        while (true) {
            skipWhitespace();
            String key = readString();
            skipWhitespace();
            expect(':');
            object.put(key, readValue());
            skipWhitespace();
            char c = next();
            if (c == '}') {
                return object;
            }
            if (c != ',') {
                throw error("',' ou '}' attendu");
            }
        }
    }

    private List<Object> readArray() {
        List<Object> array = new ArrayList<>();
        expect('[');
        skipWhitespace();
        if (peek() == ']') {
            pos++;
            return array;
        }
        while (true) {
            array.add(readValue());
            skipWhitespace();
            char c = next();
            if (c == ']') {
                return array;
            }
            if (c != ',') {
                throw error("',' ou ']' attendu");
            }
        }
    }

    private String readString() {
        expect('"');
        StringBuilder sb = new StringBuilder();
        while (true) {
            char c = next();
            if (c == '"') {
                return sb.toString();
            }
            if (c != '\\') {
                sb.append(c);
                continue;
            }
            char escaped = next();
            switch (escaped) {
                case '"', '\\', '/' -> sb.append(escaped);
                case 'b' -> sb.append('\b');
                case 'f' -> sb.append('\f');
                case 'n' -> sb.append('\n');
                case 'r' -> sb.append('\r');
                case 't' -> sb.append('\t');
                case 'u' -> {
                    if (pos + 4 > text.length()) {
                        throw error("séquence \\u incomplète");
                    }
                    sb.append((char) Integer.parseInt(text.substring(pos, pos + 4), 16));
                    pos += 4;
                }
                default -> throw error("échappement inconnu \\" + escaped);
            }
        }
    }

    private Object readNumber() {
        int start = pos;
        while (pos < text.length() && "+-0123456789.eE".indexOf(text.charAt(pos)) >= 0) {
            pos++;
        }
        String number = text.substring(start, pos);
        if (number.isEmpty()) {
            throw error("valeur JSON attendue");
        }
        if (number.contains(".") || number.contains("e") || number.contains("E")) {
            return Double.parseDouble(number);
        }
        return Long.parseLong(number);
    }

    private Object readLiteral(String word, Object value) {
        if (!text.startsWith(word, pos)) {
            throw error("'" + word + "' attendu");
        }
        pos += word.length();
        return value;
    }

    private void skipWhitespace() {
        while (pos < text.length() && Character.isWhitespace(text.charAt(pos))) {
            pos++;
        }
    }

    private char peek() {
        if (pos >= text.length()) {
            throw error("fin du texte inattendue");
        }
        return text.charAt(pos);
    }

    private char next() {
        char c = peek();
        pos++;
        return c;
    }

    private void expect(char expected) {
        if (next() != expected) {
            throw error("'" + expected + "' attendu");
        }
    }

    private IllegalArgumentException error(String message) {
        return new IllegalArgumentException("JSON invalide (position " + pos + ") : " + message);
    }
}
