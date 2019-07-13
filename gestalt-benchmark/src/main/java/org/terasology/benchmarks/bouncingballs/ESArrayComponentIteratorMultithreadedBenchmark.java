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

import org.terasology.benchmarks.bouncingballs.arraystore.ArrayComponentStore;
import org.terasology.benchmarks.bouncingballs.common.ComponentIterator;
import org.terasology.benchmarks.bouncingballs.common.ComponentStore;
import org.terasology.benchmarks.bouncingballs.common.Location;
import org.terasology.benchmarks.bouncingballs.common.Physics;
import org.terasology.benchmarks.bouncingballs.entity.EntityManager;
import org.terasology.benchmarks.bouncingballs.entity.EntityManagerImpl;
import org.terasology.entitysystem.component.ComponentManager;
import org.terasology.entitysystem.component.LambdaComponentTypeFactory;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;

class ESArrayComponentIteratorMultithreadedBenchmark implements Benchmark {
    EntityManager entityManager;
    long bounces;

    //ExecutorService executorService = E

    @Override
    public String getName() {
        return "Entity Manager Iterator w/ Array Multithreaded (4) benchmark";
    }

    @Override
    public void setup() {
        ComponentManager componentManager = new ComponentManager(new LambdaComponentTypeFactory());
        entityManager = new EntityManagerImpl();
        ComponentStore<Location> locationStore = entityManager.addComponentStore(new ArrayComponentStore<>(componentManager.getType(Location.class)));
        ComponentStore<Physics> physicsStore = entityManager.addComponentStore(new ArrayComponentStore<>(componentManager.getType(Physics.class)));


        Random random = new Random();

        for (int i = 0; i < 10000; ++i) {
            int id = entityManager.getNewId();
            Location location = new Location();
            location.setX(400 * random.nextFloat() - 200);
            location.setY(random.nextFloat() * 100);
            location.setZ(400 * random.nextFloat() - 200);
            Physics physics = new Physics();
            physics.setVelocityY(random.nextFloat() * 10 - 5);
            locationStore.set(id, location);
            physicsStore.set(id, physics);
        }
    }

    @Override
    public void cleanup() {
        System.out.println("Bounces: " + bounces);
        entityManager = null;
    }

    @Override
    public void doFrame(float delta) {
        ComponentStore<Location> locationStore = entityManager.getComponentStore(Location.class);
        ComponentStore<Physics> physicsStore = entityManager.getComponentStore(Physics.class);
        Physics physics = new Physics();
        Location location = new Location();
        EntityManagerImpl.ComponentsIterator entityIterator = entityManager.iterate(physics, location);
        while (entityIterator.next()) {


            int id = entityIterator.getEntityId();

            physics.setVelocityY(physics.getVelocityY() - 10.0f * delta);
            float y = location.getY();
            y = y + physics.getVelocityY() * delta;
            if (y < 0) {
                y *= -1;
                physics.setVelocityY(physics.getVelocityY() * -1);

                bounces++;
            }
            location.setY(y);
            physicsStore.set(id, physics);
            locationStore.set(id, location);
        }
    }

}
