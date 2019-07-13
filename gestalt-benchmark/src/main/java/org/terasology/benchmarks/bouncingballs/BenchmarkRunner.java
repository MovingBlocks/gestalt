/*
 * Copyright 2019 MovingBlocks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.terasology.benchmarks.bouncingballs;

import com.google.common.base.Stopwatch;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class BenchmarkRunner {

    private List<Benchmark> benchmarks = Lists.newArrayList(
            new NonESBallsBenchmark(),
            new DirectSingleComponentStoresBenchmark(),
            new DirectSingleIteratorComponentStoresBenchmark()
    );

    public static void main(String... args) {
        BenchmarkRunner runner = new BenchmarkRunner();
        runner.run();
    }

    public void run() {

        for (Benchmark benchmark : benchmarks) {
            System.out.println("Benchmark: " + benchmark.getName());
            System.out.println("Setting up...");
            benchmark.setup();
            System.out.println("Warming up...");
            for (int i = 0; i < 1000; ++i) {
                benchmark.doFrame(0.001f);
            }
            System.out.println("Running...");
            Stopwatch stopwatch = Stopwatch.createStarted();
            float delta = 0.01f;
            for (int i = 0; i < 100000; ++i) {
                benchmark.doFrame(delta);
                delta = Math.max(0.000001f, Math.max(0.001f, stopwatch.elapsed(TimeUnit.MILLISECONDS) * 1000f / (i + 1)));
            }
            stopwatch.stop();
            long millis = stopwatch.elapsed(TimeUnit.MILLISECONDS);
            double fps = 100000000 / millis;
            System.out.println(fps + " fps");
            System.out.println("Cleaning up...");
            benchmark.cleanup();
            System.out.println();
        }
    }
}
