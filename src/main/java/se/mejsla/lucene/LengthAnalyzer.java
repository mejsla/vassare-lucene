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

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.miscellaneous.LengthFilter;

public final class LengthAnalyzer extends Analyzer {

    private final int min;

    public LengthAnalyzer(final int min) {
        this.min = min;
    }

    @Override
    protected TokenStreamComponents createComponents(final String fieldName) {
        final Tokenizer source = new WhitespaceTokenizer();
        final TokenStream result = new LengthFilter(source, min, Integer.MAX_VALUE);
        return new TokenStreamComponents(source, result);
    }
}
