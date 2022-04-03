/*
 * Copyright 2022 Johan Dykström
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.mejsla.lucene;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queries.mlt.MoreLikeThis;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.queryparser.flexible.standard.config.PointsConfig;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import static org.apache.lucene.search.BooleanClause.Occur.SHOULD;

@SuppressWarnings({"java:S106", "java:S1192", "java:S125"})
public class QueryBooks {

    private static final Path INDEX_PATH = Path.of("data", "index");
    private static final Path BOOK_PATH = Path.of("data", "book");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Analyzer ANALYZER = new StandardAnalyzer();
    //private static final Analyzer ANALYZER = new SwedishAnalyzer();
    //private static final Analyzer ANALYZER = new LengthAnalyzer(5);
    private static final StandardQueryParser QUERY_PARSER = new StandardQueryParser(ANALYZER);

    static {
        Map<String, PointsConfig> pointsConfigMap = Map.of("pages", new PointsConfig(new DecimalFormat(), Integer.class));
        QUERY_PARSER.setPointsConfigMap(pointsConfigMap);
    }

    public static void main(String[] args) throws Exception {
        final var query = new TermQuery(new Term("isbn", "978-9113084909"));

        //final var query = new FuzzyQuery(new Term("author", "tolkie"));

        //final var query = IntPoint.newRangeQuery("pages", 300, 1000);

        //final var query = createPhraseQuery(5, "den", "bägaren");

        //final var query = new WildcardQuery(new Term("title", "d*"));

        //final var query = createBooleanQuery("ring", "skola");

        //final var query = QUERY_PARSER.parse("author:weir", "description");
        //final var query = QUERY_PARSER.parse("author:Tolkien", "description");
        //final var query = QUERY_PARSER.parse("\"den bägaren\"~10", "description");
        //final var query = QUERY_PARSER.parse("pages:[400 TO 500]", "description");
        //final var query = QUERY_PARSER.parse("/magi.*/", "description");
        //final var query = QUERY_PARSER.parse("author:(rowling OR tolkien) AND description:alla", "description");

        printBooks(executeBookQuery(query));

        //executeSimilarityQuery("978-9129723915.json");
        //executeSimilarityQuery("978-9113084909.json");
    }

    private static Query createBooleanQuery(final String... terms) {
        final var builder = new BooleanQuery.Builder();
        Arrays.stream(terms)
              .map(term -> new TermQuery(new Term("description", term)))
              .forEach(query -> builder.add(query, SHOULD));
        return builder.build();
    }

    public static Query createPhraseQuery(final int slop, final String... terms) {
        final var builder = new PhraseQuery.Builder();
        builder.setSlop(slop);
        for (int i = 0; i < terms.length; i++) {
            builder.add(new Term("description", terms[i].toLowerCase()), i);
        }
        return builder.build();
    }

    public static List<Book> executeBookQuery(final Query query) throws IOException {
        try (final Directory directory = FSDirectory.open(INDEX_PATH);
             final IndexReader reader = DirectoryReader.open(directory)) {
            final var searcher = new IndexSearcher(reader);
            final List<Book> books = new ArrayList<>();

            ScoreDoc[] hits = searcher.search(query, 10).scoreDocs;
            while (hits.length > 0) {
                for (final ScoreDoc hit : hits) {
                    final var document = searcher.doc(hit.doc);
                    final var path = Path.of(document.get("path"));
                    books.add(OBJECT_MAPPER.readValue(path.toFile(), Book.class));
                }
                hits = searcher.searchAfter(hits[hits.length - 1], query, 10).scoreDocs;
            }

            return books;
        }
    }

    public static void executeSimilarityQuery(final String filename) throws IOException {
        Book source = OBJECT_MAPPER.readValue(BOOK_PATH.resolve(filename).toFile(), Book.class);

        try (final Directory directory = FSDirectory.open(INDEX_PATH);
             final IndexReader reader = DirectoryReader.open(directory)) {
            final var searcher = new IndexSearcher(reader);

            MoreLikeThis mlt = new MoreLikeThis(reader);
            mlt.setAnalyzer(ANALYZER);
            mlt.setFieldNames(null);
            mlt.setMinTermFreq(0);
            mlt.setMinDocFreq(0);
            final var query = mlt.like("description", new StringReader(source.description()));

            ScoreDoc[] hits = searcher.search(query, 10).scoreDocs;
            while (hits.length > 0) {
                for (final ScoreDoc hit : hits) {
                    final var document = searcher.doc(hit.doc);
                    System.out.printf("%5.2f   %s%n", hit.score, document.get("title"));
                }
                hits = searcher.searchAfter(hits[hits.length - 1], query, 10).scoreDocs;
            }
        }
    }

    private static void printBooks(final List<Book> books) {
        for (var book : books) {
            final var description = book.description().replace("\n", " | ");
            System.out.printf("%-15s   %-20s   %-40s   %3d   %s%n",
                    book.isbn(), book.author(), book.title(), book.pages(), description);
        }
    }
}
