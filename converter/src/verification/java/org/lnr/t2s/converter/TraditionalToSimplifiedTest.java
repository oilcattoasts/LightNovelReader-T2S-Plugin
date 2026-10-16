package org.lnr.t2s.converter;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.List;

/** Dependency-free regression suite, runnable with JDK 17 or Gradle check. */
public final class TraditionalToSimplifiedTest {
    private static int assertions;

    private static void equal(String expected, String actual) {
        assertions++;
        if (!expected.equals(actual)) {
            throw new AssertionError("Expected <" + expected + ">, got <" + actual + ">");
        }
    }

    public static void main(String[] args) throws Exception {
        Path dictionary = Path.of(args[0]);
        TraditionalToSimplified converter;
        try (Reader characters = Files.newBufferedReader(dictionary.resolve("TSCharacters.txt"), StandardCharsets.UTF_8);
             Reader phrases = Files.newBufferedReader(dictionary.resolve("TSPhrases.txt"), StandardCharsets.UTF_8)) {
            converter = new TraditionalToSimplified(characters, phrases);
        }
        equal("", converter.convert(""));
        equal("这是一部轻小说，阅读体验很好。", converter.convert("這是一部輕小說，閱讀體驗很好。"));
        equal("乾隆年间，天空很干净。", converter.convert("乾隆年間，天空很乾淨。"));
        equal("他回复了我的信，反复确认计划。", converter.convert("他回覆了我的信，反覆確認計畫。"));
        equal("乾坤大挪移", converter.convert("乾坤大挪移"));
        equal("瞭望台，一目了然。", converter.convert("瞭望臺，一目瞭然。"));
        equal("ABC 日本语 あいう 😀\n\t123！", converter.convert("ABC 日本語 あいう 😀\n\t123！"));
        equal("已经是简体，保留标点。\r\n", converter.convert("已经是简体，保留标点。\r\n"));
        equal("𫝈𠉂😀", converter.convert("㑮㒓😀"));
        equal("滑鼠和软体", converter.convert("滑鼠和軟體"));
        equal("情有独钟", converter.convert("情有獨鍾"));

        // All pinned dictionary entries, including identity phrases, are honored.
        for (String name : List.of("TSCharacters.txt", "TSPhrases.txt")) {
            for (String line : Files.readAllLines(dictionary.resolve(name), StandardCharsets.UTF_8)) {
                if (line.isBlank() || line.startsWith("#")) continue;
                String[] columns = line.split("\t", 2);
                equal(columns[1].split(" ", 2)[0], converter.convert(columns[0]));
            }
        }
        TraditionalToSimplified synthetic = new TraditionalToSimplified(
            new StringReader("甲\t乙\n乙\t丙\n𠮷\t吉\n"),
            new StringReader("甲乙\t長詞\n甲乙丙\t最長詞\n")
        );
        equal("最長詞長詞乙丁丙吉", synthetic.convert("甲乙丙甲乙甲丁乙𠮷"));
        equal("乙", synthetic.convert("甲")); // Never cascade a replacement through the dictionary.
        equal("吉😀", synthetic.convert("𠮷😀"));
        try {
            new TraditionalToSimplified(new StringReader("bad line"), new StringReader(""));
            throw new AssertionError("Malformed dictionary accepted");
        } catch (IOException expected) { assertions++; }

        String chapter = "乾隆年間，他回覆了這封信。😀\n".repeat(10000);
        String expectedChapter = "乾隆年间，他回复了这封信。😀\n".repeat(10000);
        long start = System.nanoTime();
        equal(expectedChapter, converter.convert(chapter));
        System.out.printf("Long chapter: %,d UTF-16 units, %.1f ms%n", chapter.length(), (System.nanoTime() - start) / 1e6);
        var pool = Executors.newFixedThreadPool(4);
        try {
            List<Future<String>> jobs = new ArrayList<>();
            for (int i = 0; i < 32; i++) jobs.add(pool.submit(() -> converter.convert("乾隆年間，回覆。😀")));
            for (Future<String> job : jobs) equal("乾隆年间，回复。😀", job.get());
        } finally { pool.shutdownNow(); }
        System.out.println("PASS: " + assertions + " assertions");
    }
}
