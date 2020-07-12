/*
 * Copyright 2020 Google LLC
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
package com.google.swarm.tokenization.common;

import com.google.privacy.dlp.v2.Table;
import com.google.privacy.dlp.v2.Value;
import java.util.Iterator;
import java.util.Objects;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.commons.csv.CSVRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("serial")
class MapStringToDlpRow extends DoFn<KV<String, CSVRecord>, KV<String, Table.Row>> {

  public static final Logger LOG = LoggerFactory.getLogger(DLPTransform.class);

  private final String delimiter;

  public MapStringToDlpRow(String delimiter) {
    this.delimiter = delimiter;
  }

  @ProcessElement
  public void processElement(ProcessContext context) {
    Table.Row.Builder rowBuilder = Table.Row.newBuilder();
    CSVRecord line = Objects.requireNonNull(context.element().getValue());
    Iterator<String> valueIterator = line.iterator();
    while (valueIterator.hasNext()) {
      String value = valueIterator.next();
      if (value != null) {
        rowBuilder.addValues(Value.newBuilder().setStringValue(value.toString()).build());
      } else {
        rowBuilder.addValues(Value.newBuilder().setStringValue("").build());
      }
    }

    context.output(KV.of(context.element().getKey(), rowBuilder.build()));
  }
}