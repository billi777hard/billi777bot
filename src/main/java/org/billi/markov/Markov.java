package org.billi.markov;

import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
public class Markov {
    private final static Logger logger = LoggerFactory.getLogger(Markov.class);

    public static final Pattern GARBAGE = Pattern.compile("[^\\w,.:!?-]+", Pattern.UNICODE_CHARACTER_CLASS);
    public static final Pattern URL = Pattern.compile("(http|https)://[^\\s]+", Pattern.UNICODE_CHARACTER_CLASS);
    public static final Pattern WORD = Pattern.compile("(<:\\w+:\\d+>)|((\\w+-?\\w*)(\\W?))", Pattern.UNICODE_CHARACTER_CLASS);

    @Data
    private static class Info {
        private Set<String> follow;
        private String word;
        private Set<String> stops;
        private boolean stop;
        private boolean start;

        Info() {
            follow = new HashSet<>();
            stops = new HashSet<>();
        }

        Info(String word) {
            this();
            this.word = word;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("> ").append(word).append("\n");
            sb.append("start: ").append(start).append(", ")
                    .append("stop : ").append(stop)
                    .append("\n");
            sb.append("follow:\n\t")
                    .append(String.join("\n\t", follow))
                    .append("\n");
            sb.append("stops:\n\t")
                    .append(String.join("\n\t", stops))
                    .append("\n");
            return sb.toString();
        }
    }

    private Map<String, Info> data = new HashMap<>();
    private List<String> startWords = new ArrayList<>();
    private Random random = new Random();

    public void add(String message) {
        String t = message;
        if (t == null || t.isEmpty())
            return;
        String text1 = URL.matcher(t).replaceAll(" ");
//        String text = GARBAGE.matcher(text1).replaceAll(" ");
        String text = text1;
        Matcher wordMatcher = WORD.matcher(text);

        String prevKey = "";
        while (wordMatcher.find()) {
            String word = "";
            boolean isEmoji = StringUtils.isNotBlank(wordMatcher.group(1));
            if (isEmoji) {
                word = wordMatcher.group(1);
            } else {
                word = wordMatcher.group(3);
            }
            String stop = wordMatcher.group(4);
            String key = word.toLowerCase();
            Info info;
            if (this.data.get(key) != null) {
                info = this.data.get(key);
            } else {
                info = new Info(word);
            }
            if (StringUtils.isNotBlank(stop)) {
                info.setStop(true);
                info.getStops().add(stop);
            }
            this.data.put(key, info);
            if (prevKey.isEmpty()) {
                info.setStart(true);
                this.startWords.add(word);
            } else {
                Info prevInfo = this.data.get(prevKey);
                prevInfo.getFollow().add(key);
            }
            logger.debug(info.toString());
            prevKey = key;
        }
        if (!prevKey.isEmpty()) {
            this.data.get(prevKey).setStop(true);
            data.get(prevKey).getStops().add(".");
        }
    }

    public String generate() {
        int i = random.nextInt(startWords.size());
        String w = startWords.get(i).toLowerCase();
        StringBuilder sb = new StringBuilder(startWords.get(i));
        Info info = data.get(w);
        logger.debug(info.toString());
        boolean finished = false;
        int size = 1;
        while (info.getFollow().size() > 0 && !finished && size < 30) {
            if (info.getStops().size() > 0) {
                if (random.nextDouble() < 0.4) {
                    ArrayList<String> stops = new ArrayList<>(info.getStops());
                    sb.append(stops.get(random.nextInt(stops.size())));
                } else {
                    if (info.isStop() && random.nextDouble() < 0.2) {
                        sb.append(".");
                        finished = true;
                    }
                }
            }
            ArrayList<String> follow = new ArrayList<>(info.getFollow());
            w = follow.get(random.nextInt(follow.size()));
            info = data.get(w);
            sb.append(" ").append(info.getWord());
            size++;
        }
        return sb.toString();
    }

    public String debug() {
        StringBuilder sb = new StringBuilder("```\n");
        for (Info value: data.values()) {
            sb.append(value.toString());
        }
        sb.append("```");
        return sb.toString();
    }
}
