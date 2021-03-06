/*
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to You under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.geode.internal.process;

import static org.junit.Assert.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.junit.Test;
import org.junit.experimental.categories.Category;

import org.apache.geode.test.dunit.Host;
import org.apache.geode.test.dunit.SerializableRunnable;
import org.apache.geode.test.dunit.internal.JUnit4DistributedTestCase;
import org.apache.geode.test.junit.categories.DistributedTest;

/**
 * Multi-process tests for ProcessLauncher.
 * 
 * @since GemFire 7.0
 */
@Category(DistributedTest.class)
@SuppressWarnings("serial")
public class LocalProcessLauncherDUnitTest extends JUnit4DistributedTestCase {

  public LocalProcessLauncherDUnitTest() {
    super();
  }

  @Override
  public final void postSetUp() throws Exception {
    new File(getClass().getSimpleName()).mkdir();
  }

  @Test
  public void testExistingPidFileThrows() throws Exception {
    final File pidFile =
        new File(getClass().getSimpleName() + File.separator + "testExistingPidFileThrows.pid");
    final String absolutePath = pidFile.getAbsolutePath();

    assertFalse(pidFile.exists());
    new LocalProcessLauncher(pidFile, false);
    assertTrue(pidFile.exists());

    Host.getHost(0).getVM(0).invoke(
        new SerializableRunnable("LocalProcessLauncherDUnitTest#testExistingPidFileThrows") {
          @Override
          public void run() {
            try {
              new LocalProcessLauncher(new File(absolutePath), false);
              fail("Two processes both succeeded in creating " + pidFile);
            } catch (FileAlreadyExistsException e) {
              // passed
            } catch (IllegalStateException e) {
              throw new AssertionError(e);
            } catch (IOException e) {
              throw new AssertionError(e);
            } catch (PidUnavailableException e) {
              throw new AssertionError(e);
            }
          }
        });
  }

  @Test
  public void testForceReplacesExistingPidFile() throws Exception {
    final File pidFile = new File(
        getClass().getSimpleName() + File.separator + "testForceReplacesExistingPidFile.pid");
    final String absolutePath = pidFile.getAbsolutePath();

    assertFalse(pidFile.exists());
    final LocalProcessLauncher launcher = new LocalProcessLauncher(pidFile, false);
    assertTrue(pidFile.exists());
    assertNotNull(launcher);
    final int pid = launcher.getPid();

    Host.getHost(0).getVM(0).invoke(
        new SerializableRunnable("LocalProcessLauncherDUnitTest#testForceReplacesExistingPidFile") {
          @Override
          public void run() {
            try {
              final LocalProcessLauncher launcher =
                  new LocalProcessLauncher(new File(absolutePath), true);
              assertNotSame(pid, launcher.getPid());
              assertFalse(pid == launcher.getPid());

              final FileReader fr = new FileReader(absolutePath);
              final BufferedReader br = new BufferedReader(fr);
              final int pidFromFile = Integer.parseInt(br.readLine());
              br.close();

              assertNotSame(pid, pidFromFile);
              assertFalse(pid == pidFromFile);
              assertEquals(launcher.getPid(), pidFromFile);
            } catch (IllegalStateException e) {
              throw new AssertionError(e);
            } catch (IOException e) {
              throw new AssertionError(e);
            } catch (FileAlreadyExistsException e) {
              throw new AssertionError(e);
            } catch (PidUnavailableException e) {
              throw new AssertionError(e);
            }
          }
        });
  }

  @Test
  public void testPidFileReadByOtherProcess() throws Exception {
    final File pidFile =
        new File(getClass().getSimpleName() + File.separator + "testPidFileReadByOtherProcess.pid");
    final String absolutePath = pidFile.getAbsolutePath();

    assertFalse(pidFile.exists());
    final LocalProcessLauncher launcher = new LocalProcessLauncher(pidFile, false);
    assertTrue(pidFile.exists());
    assertNotNull(launcher);
    final int pid = launcher.getPid();

    Host.getHost(0).getVM(0).invoke(
        new SerializableRunnable("LocalProcessLauncherDUnitTest#testPidFileReadByOtherProcess") {
          @Override
          public void run() {
            try {
              final FileReader fr = new FileReader(absolutePath);
              final BufferedReader br = new BufferedReader(fr);
              final int pidFromFile = Integer.parseInt(br.readLine());
              br.close();
              assertEquals(pid, pidFromFile);
            } catch (FileNotFoundException e) {
              throw new Error(e);
            } catch (IOException e) {
              throw new Error(e);
            }
          }
        });
  }
}
