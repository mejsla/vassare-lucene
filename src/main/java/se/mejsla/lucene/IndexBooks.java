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

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.IntPoint;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import static java.util.Objects.requireNonNull;

@SuppressWarnings({"java:S106", "java:S1192", "java:S125"})
public class IndexBooks {

    private static final Path INDEX_PATH = Path.of("data", "index");
    private static final Path BOOK_PATH = Path.of("data", "book");

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final Analyzer ANALYZER = new StandardAnalyzer();
    //private static final Analyzer ANALYZER = new StandardAnalyzer(new CharArraySet(Set.of("i", "om", "nu"), true));
    //private static final Analyzer ANALYZER = new SwedishAnalyzer();
    //private static final Analyzer ANALYZER = new LengthAnalyzer(5);

    public static void main(String[] args) throws IOException {
        final IndexWriterConfig config = new IndexWriterConfig(ANALYZER);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        try (final Directory directory = FSDirectory.open(INDEX_PATH);
             final IndexWriter writer = new IndexWriter(directory, config)) {
            for (File file : requireNonNull(BOOK_PATH.toFile().listFiles())) {
                Book book = OBJECT_MAPPER.readValue(file, Book.class);
                writer.addDocument(createDocument(file.toPath(), book));
            }
        }
    }

    public static Document createDocument(final Path path, final Book book) {
        Document document = new Document();
        document.add(new StringField("path", path.toString(), Field.Store.YES));
        document.add(new StringField("isbn", book.isbn(), Field.Store.YES));
        document.add(new TextField("title", book.title(), Field.Store.YES));
        document.add(new TextField("author", book.author(), Field.Store.YES));
        document.add(new IntPoint("pages", book.pages()));
        document.add(new TextField("description", book.description(), Field.Store.NO));
        return document;
    }
}
