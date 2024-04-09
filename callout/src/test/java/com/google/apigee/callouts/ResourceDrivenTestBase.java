// Copyright (c) 2016 Apigee Corp, 2017-2024 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//

package com.google.apigee.callouts;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.function.Function;
import org.testng.annotations.DataProvider;

public abstract class ResourceDrivenTestBase extends TestBase {

  abstract String testDataDir();

  @DataProvider(name = "batch1")
  public Object[][] getDataForBatch1() throws IOException, IllegalStateException {

    // @DataProvider requires the output to be a Object[][]. The inner
    // Object[] is the set of params that get passed to the test method.
    // So, if you want to pass just one param to the constructor, then
    // each inner Object[] must have length 1.

    // Path currentRelativePath = Paths.get("");
    // String s = currentRelativePath.toAbsolutePath().toString();
    // System.out.println("Current relative path is: " + s);

    // read in all the subdirectories in the test-data directory

    File dataDir = new File(testDataDir());
    if (!dataDir.exists()) {
      throw new IllegalStateException("no test data directory.");
    }

    File[] dirs = dataDir.listFiles(File::isDirectory);
    if (dirs.length == 0) {
      throw new IllegalStateException("no tests found.");
    }
    Arrays.sort(dirs);
    Function<File, Object[]> toTestCase =
        (dir) -> {
          try {
            String name = dir.getName();
            return new Object[] {new TestCase(name, Paths.get(testDataDir(), name))};
          } catch (java.lang.Exception exc1) {
            exc1.printStackTrace();
            throw new RuntimeException("uncaught exception", exc1);
          }
        };

    return Arrays.stream(dirs).map(toTestCase).toArray(Object[][]::new);
  }
}
