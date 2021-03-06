/*
 * JBoss, Home of Professional Open Source
 * Copyright 2009 Red Hat Inc. and/or its affiliates and other
 * contributors as indicated by the @author tags. All rights reserved.
 * See the copyright.txt in the distribution for a full listing of
 * individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.infinispan.distribution.rehash;

import org.infinispan.Cache;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.test.TestingUtil;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static java.util.concurrent.TimeUnit.SECONDS;

@Test(groups = "functional", testName = "distribution.rehash.ConcurrentJoinTest")
public class ConcurrentJoinTest extends RehashTestBase {

   List<EmbeddedCacheManager> joinerManagers;
   List<Cache<Object, String>> joiners;

   final int numJoiners = 4;

   void performRehashEvent(boolean offline) {
      Runnable runnable = new Runnable() {
         public void run() {

            joinerManagers = new ArrayList<EmbeddedCacheManager>(numJoiners);
            joiners = new ArrayList<Cache<Object, String>>(numJoiners);
            for (int i = 0; i < numJoiners; i++) {
               EmbeddedCacheManager joinerManager = addClusterEnabledCacheManager();
               joinerManager.defineConfiguration(cacheName, configuration);
               Cache<Object, String> joiner = joinerManager.getCache(cacheName);
               joinerManagers.add(joinerManager);
               joiners.add(joiner);
            }
         }
      };

      if (offline) {
         new Thread(runnable).start();
      } else {
         runnable.run();
      }
   }

   @SuppressWarnings("unchecked")
   void waitForRehashCompletion() {
      waitForJoinTasksToComplete(SECONDS.toMillis(480), joiners.toArray(new Cache[numJoiners]));
      TestingUtil.sleepThread(SECONDS.toMillis(2));
      int[] joinersPos = new int[numJoiners];
      for (int i = 0; i < numJoiners; i++) joinersPos[i] = locateJoiner(joinerManagers.get(i).getAddress());

      log.info("***>>> Joiners are in positions " + Arrays.toString(joinersPos));
      for (int i = 0; i < numJoiners; i++) {
         if (joinersPos[i] > caches.size())
            caches.add(joiners.get(i));
         else
            caches.add(joinersPos[i], joiners.get(i));
      }
   }
}
