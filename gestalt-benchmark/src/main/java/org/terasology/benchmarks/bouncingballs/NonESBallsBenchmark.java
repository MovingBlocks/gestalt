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

import com.google.common.collect.Lists;

import org.terasology.benchmarks.bouncingballs.common.Location;
import org.terasology.benchmarks.bouncingballs.common.Physics;

import java.util.List;
import java.util.Random;

class NonESBallsBenchmark implements Benchmark
{
    List<Ball> balls = Lists.newArrayListWithCapacity(10000);

    long bounces;

    @Override
    public String getName() {
        return "Non-entity system baseline benchmark";
    }

    @Override
    public void setup() {
        Random random = new Random();

        for (int i = 0; i < 10000; ++i) {
            Ball ball = new Ball();
            ball.location.setX(400 * random.nextFloat() - 200);
            ball.location.setY(random.nextFloat() * 100);
            ball.location.setZ(400 * random.nextFloat() - 200);
            ball.physics.setVelocityY(random.nextFloat() * 10 - 5);
            balls.add(ball);
        }
    }

    @Override
    public void cleanup() {
        System.out.println("Bounces: " + bounces);
        balls.clear();
    }

    @Override
    public void doFrame(float delta) {
        for (Ball ball : balls) {
            Physics physics = ball.physics;
            physics.setVelocityY(physics.getVelocityY() - 10.0f * delta);
            Location location = ball.location;
            float y = location.getY();
            y = y + physics.getVelocityY() * delta;
            if (y < 0) {
                y *= -1;
                physics.setVelocityY(physics.getVelocityY() * -1);
                bounces++;
            }
            location.setY(y);
        }
    }

    private static class Ball {
        Location location = new Location();
        Physics physics = new Physics();
    }
}
