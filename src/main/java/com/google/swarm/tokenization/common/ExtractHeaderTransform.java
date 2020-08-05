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

import java.util.List;

import com.google.auto.value.AutoValue;
import com.google.swarm.tokenization.avro.AvroHeaderDoFn;
import com.google.swarm.tokenization.common.Util.FileType;
import org.apache.beam.sdk.io.FileIO;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.View;
import org.apache.beam.sdk.transforms.windowing.AfterProcessingTime;
import org.apache.beam.sdk.transforms.windowing.GlobalWindows;
import org.apache.beam.sdk.transforms.windowing.Repeatedly;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;


@AutoValue
public abstract class ExtractHeaderTransform extends PTransform<PCollection<KV<String, FileIO.ReadableFile>>, PCollectionView<List<String>>> {

    public abstract FileType fileType();

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract ExtractHeaderTransform.Builder setFileType(FileType fileType);

        public abstract ExtractHeaderTransform build();
    }

    public static ExtractHeaderTransform.Builder newBuilder() {
        return new AutoValue_ExtractHeaderTransform.Builder();
    }

    @Override
    public PCollectionView<List<String>> expand(PCollection<KV<String, FileIO.ReadableFile>> input) {
        PCollection<KV<String, FileIO.ReadableFile>> globalWindow = input
            .apply(
                "GlobalWindow",
                Window.<KV<String, FileIO.ReadableFile>>into(new GlobalWindows())
                    .triggering(
                        Repeatedly.forever(AfterProcessingTime.pastFirstElementInPane()))
                    .discardingFiredPanes());
        PCollection<String> readHeader;
        switch (fileType()) {
            case AVRO:
                readHeader = globalWindow
                    .apply("ReadHeader", ParDo.of(new AvroHeaderDoFn()));
                break;
            case CSV:
                readHeader = globalWindow
                    .apply("ReadHeader", ParDo.of(new CSVFileHeaderDoFn()));
                break;
            default:
                throw new IllegalStateException("Unexpected value: " + fileType());
        }
        return readHeader.apply("ViewAsList", View.asList());
    }
}
