package org.lnr.t2s.converter;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable after construction; safe to share between chapter loads. */
public final class TraditionalToSimplified {
    private static final class Node {
        final Map<Integer, Node> children = new HashMap<>();
        String replacement;
    }

    private final Node root = new Node();

    /** Readers remain owned by the caller. Phrase entries take precedence. */
    public TraditionalToSimplified(Reader characters, Reader phrases) throws IOException {
        load(characters);
        load(phrases);
    }

    private void load(Reader reader) throws IOException {
        BufferedReader lines = new BufferedReader(reader);
        String line;
        int lineNumber = 0;
        while ((line = lines.readLine()) != null) {
            lineNumber++;
            if (line.isEmpty() || line.startsWith("#")) continue;
            int tab = line.indexOf('\t');
            if (tab <= 0 || tab == line.length() - 1) {
                throw new IOException("Invalid dictionary entry at line " + lineNumber);
            }
            String source = line.substring(0, tab);
            // OpenCC orders ambiguous target candidates by preference.
            String targets = line.substring(tab + 1);
            int space = targets.indexOf(' ');
            String target = space < 0 ? targets : targets.substring(0, space);
            if (target.isEmpty()) throw new IOException("Empty dictionary target");
            Node node = root;
            for (int offset = 0; offset < source.length();) {
                int point = source.codePointAt(offset);
                Node next = node.children.get(point);
                if (next == null) {
                    next = new Node();
                    node.children.put(point, next);
                }
                node = next;
                offset += Character.charCount(point);
            }
            node.replacement = target;
        }
    }

    /** Forward longest-match conversion over Unicode code points, in one pass. */
    public String convert(String text) {
        Objects.requireNonNull(text, "text");
        StringBuilder output = new StringBuilder(text.length());
        for (int offset = 0; offset < text.length();) {
            Node node = root;
            String replacement = null;
            int matchedEnd = offset;
            for (int cursor = offset; cursor < text.length();) {
                int point = text.codePointAt(cursor);
                node = node.children.get(point);
                if (node == null) break;
                cursor += Character.charCount(point);
                if (node.replacement != null) {
                    replacement = node.replacement;
                    matchedEnd = cursor;
                }
            }
            if (replacement == null) {
                int point = text.codePointAt(offset);
                output.appendCodePoint(point);
                offset += Character.charCount(point);
            } else {
                output.append(replacement);
                offset = matchedEnd;
            }
        }
        return output.toString();
    }
}
