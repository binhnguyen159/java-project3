package com.udacity.webcrawler;

import com.udacity.webcrawler.parser.PageParser;
import com.udacity.webcrawler.parser.PageParserFactory;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.RecursiveTask;
import java.util.regex.Pattern;

public final class InternalCrawlerTask extends RecursiveTask<Boolean> {
    private final String url;
    private final Instant dueTime;
    private final int maxDepth;
    private final ConcurrentMap<String, Integer> counts;
    private final ConcurrentSkipListSet<String> urlsVisited;
    private final Clock clock;
    private final PageParserFactory parserFactory;
    private final List<Pattern> ignoredUrls;

    public InternalCrawlerTask(
            String url,
            Instant dueTime,
            int maxDepth,
            ConcurrentMap<String, Integer> counts,
            ConcurrentSkipListSet<String> urlsVisited,
            Clock clock,
            PageParserFactory parserFactory,
            List<Pattern> ignoredUrls
    ){
        this.url = url;
        this.dueTime = dueTime;
        this.maxDepth = maxDepth;
        this.counts = counts;
        this.urlsVisited = urlsVisited;
        this.clock = clock;
        this.parserFactory = parserFactory;
        this.ignoredUrls = ignoredUrls;
    }

    @Override
    protected synchronized Boolean compute(){
        if(maxDepth == 0 || clock.instant().isAfter((dueTime))){
            return false;
        }
        for(Pattern pattern: ignoredUrls){
            if(pattern.matcher(url).matches()){
                return false;
            }
        }
        if(urlsVisited.contains(url)){
               return false;
        }
        urlsVisited.add(url);

        PageParser.Result result = parserFactory.get(url).parse();
        for(ConcurrentMap.Entry<String, Integer> e: result.getWordCounts().entrySet()){
            counts.compute(e.getKey(),(k,v) -> (v == null) ? e.getValue() : e.getValue() +v);
        }

        List<InternalCrawlerTask> subtasks = new ArrayList<>();
        for(String link: result.getLinks()){
            subtasks.add(new InternalCrawlerTask(link, dueTime, maxDepth - 1, counts, urlsVisited, clock, parserFactory, ignoredUrls));
        }
        invokeAll(subtasks);
        return true;
    }

}
